package org.labkey.test.stress;

import org.apache.commons.lang3.StringUtils;
import org.apache.xmlbeans.XmlException;
import org.assertj.core.api.Assertions;
import org.labkey.query.xml.ApiTestsDocument;
import org.labkey.query.xml.TestCaseType;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.miniprofiler.RequestInfo;
import org.labkey.remoteapi.miniprofiler.RequestsResponse;
import org.labkey.remoteapi.miniprofiler.SessionRequestsCommand;
import org.labkey.remoteapi.security.WhoAmICommand;
import org.labkey.serverapi.collections.ArrayListMap;
import org.labkey.test.credentials.Login;
import org.labkey.test.credentials.Server;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Simulation<T>
{
    private final Connection _connection;
    private final List<Activity> _activities;
    private final int _delayBetweenActivities;
    private final ExecutorService _simulationExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService _activityExecutor;
    private final Future<T> _runningSimulation;
    private final AtomicBoolean _stopped = new AtomicBoolean(false);
    private final ResultCollector<T> _gate;

    Simulation(Connection connection, List<Activity> activities, int delayBetweenActivities, int maximumActivityThreads, ResultCollector<T> gate)
    {
        _connection = connection;
        _activities = activities;
        _delayBetweenActivities = delayBetweenActivities;
        _activityExecutor = Executors.newFixedThreadPool(maximumActivityThreads);
        _runningSimulation = _simulationExecutor.submit(this::startSimulation);
        _gate = gate;
    }

    public boolean isStopped()
    {
        return _stopped.get();
    }

    public T collectResults()
    {
        _simulationExecutor.shutdown();
        _stopped.set(true);
        try
        {
            return _runningSimulation.get(60, TimeUnit.SECONDS);
        }
        catch (InterruptedException | ExecutionException | TimeoutException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void shutdownNow()
    {
        _stopped.set(true);
        _simulationExecutor.shutdownNow();
        _activityExecutor.shutdownNow();
    }

    private T startSimulation() throws ExecutionException, InterruptedException
    {
        while (!_stopped.get() && !Thread.interrupted())
        {
            for (Activity activity : _activities)
            {
                try
                {
                    runActivity(activity, _activityExecutor);

                    Thread.sleep(_delayBetweenActivities);
                }
                catch (Exception e)
                {
                    shutdownNow();
                    throw e;
                }
            }
        }
        return _gate.getResults();
    }

    private Map<String, Integer> runActivity(Activity activity, ExecutorService activityExecutor) throws ExecutionException, InterruptedException
    {
        Map<String, Future<Integer>> futures = new ArrayListMap<>();
        for (TestCaseType request : activity.getRequests())
        {
            futures.put(request.getName(), activityExecutor.submit(() -> makeRequest(request)));
        }

        Map<String, Integer> results = new ArrayListMap<>();
        for (Map.Entry<String, Future<Integer>> entry : futures.entrySet())
        {
            if (_stopped.get() || Thread.interrupted())
            {
                return results;
            }
            Integer result = entry.getValue().get();
            results.put(entry.getKey(), result);
        }
        return results;
    }

    private int makeRequest(TestCaseType testCase) throws InterruptedException
    {
        ApiTestCommand command = new ApiTestCommand(testCase);
        try
        {
            CommandResponse response = command.execute(_connection);
            return response.getStatusCode();
        }
        catch (CommandException e)
        {
            return e.getStatusCode();
        }
        catch (IOException e)
        {
            return -1;
        }
        finally
        {
            _gate.postRequest();
        }
    }

    public static class Definition
    {
        private final Supplier<Connection> _connectionSupplier;

        private int maxActivityThreads = 6; // This seems to be the number of parallel requests browsers handle
        private int delayBetweenActivities = 5_000;
        private List<Activity> activityDefinitions = Collections.emptyList();

        public Definition(Supplier<Connection> connectionSupplier)
        {
            _connectionSupplier = connectionSupplier;
        }

        public Definition(String baseUrl, String username, String password)
        {
            this(() -> new Connection(baseUrl, username, password));
        }

        public Definition(String baseUrl, Login login)
        {
            this(baseUrl, login.getUsername(), login.getPassword());
        }

        public Definition(Server server)
        {
            this(server.getHost(), server.getLogins().get(0));
        }

        public Definition setMaxActivityThreads(int maxActivityThreads)
        {
            this.maxActivityThreads = maxActivityThreads;
            return this;
        }

        public Definition setDelayBetweenActivities(int delayBetweenActivities)
        {
            this.delayBetweenActivities = delayBetweenActivities;
            return this;
        }

        public Definition setActivityFiles(File... activityFiles)
        {
            return setActivityFilesWithReplacements(
                    Arrays.stream(activityFiles).collect(Collectors.toMap(f -> f, f -> Collections.emptyMap())));
        }

        public Definition setActivityFilesWithReplacements(Map<File, Map<String, String>> activityFilesWithReplacements)
        {
            activityDefinitions = activityFilesWithReplacements.entrySet().stream()
                    .map(entry -> new Activity(entry.getKey().getName(), Definition.parseTests(entry.getKey(), entry.getValue())))
                    .toList();
            return this;
        }

        public Simulation<Collection<RequestInfo>> startSimulation() throws IOException, CommandException
        {
            return startSimulation(SessionResultsCollector::new);
        }

        public Simulation<Void> startSimulationIgnoringResults() throws IOException, CommandException
        {
            return startSimulation(connection -> RESULTS_NOOP);
        }

        public <T> Simulation<T> startSimulation(Function<Connection, ResultCollector<T>> gateSupplier) throws IOException, CommandException
        {
            Connection connection = _connectionSupplier.get();
            new WhoAmICommand().execute(connection, null);
            return new Simulation<>(connection, activityDefinitions, delayBetweenActivities, maxActivityThreads, gateSupplier.apply(connection));
        }

        private static List<TestCaseType> parseTests(File testFile)
        {
            try
            {
                ApiTestsDocument doc = ApiTestsDocument.Factory.parse(testFile);

                return List.of(doc.getApiTests().getTestArray());
            }
            catch (IOException | XmlException e)
            {
                throw new RuntimeException("Failed to parse test file " + testFile, e);
            }
        }

        private static List<TestCaseType> parseTests(File testFile, Map<String, String> replacements)
        {
            List<TestCaseType> testCases = parseTests(testFile);
            for (TestCaseType testCase : testCases)
            {
                applyReplacements(testCase, replacements);
            }
            return testCases;
        }

        private static void applyReplacements(TestCaseType testCase, Map<String, String> replacements)
        {
            testCase.setUrl(calculateReplacements(testCase.getUrl(), replacements));
            testCase.setFormData(calculateReplacements(testCase.getFormData(), replacements));
        }

        private static final Pattern replacementPattern = Pattern.compile("@@([a-zA-Z0-9_-]+)@@");
        private static String calculateReplacements(String value, Map<String, String> replacements)
        {
            if (StringUtils.isBlank(value))
                return value;

            Matcher matcher = replacementPattern.matcher(value);
            return matcher.replaceAll(r -> {
                String key = r.group(1);
                Assertions.assertThat(replacements).as("Missing replacement value").containsKey(key);
                return replacements.get(key);
            });
        }
    }

    private static class SessionResultsCollector implements ResultCollector<Collection<RequestInfo>>
    {
        private final Connection _connection;
        private final Object lock = new Object();
        private Map<Long, RequestInfo> _requestInfos = new ConcurrentHashMap<>();

        private Long _requestId = 0L;

        public SessionResultsCollector(Connection connection)
        {
            _connection = connection;
        }

        @Override
        public void postRequest() throws InterruptedException
        {

        }

        @Override
        public Collection<RequestInfo> getResults()
        {
            try
            {
                getRequestInfos();
            }
            catch (IOException | CommandException e)
            {
                throw new RuntimeException(e);
            }
            return _requestInfos.values();
        }

        private void getRequestInfos() throws IOException, CommandException
        {
            SessionRequestsCommand command = new SessionRequestsCommand(_requestId);
            RequestsResponse response = command.execute(_connection, null);
            List<RequestInfo> requestInfos = response.getRequestInfos();
            if (!requestInfos.isEmpty())
            {
                _requestId = requestInfos.get(requestInfos.size() - 1).getId();
                requestInfos.forEach(ri -> _requestInfos.put(ri.getId(), ri));
            }

        }
    }

    public interface ResultCollector<T>
    {
        void postRequest() throws InterruptedException;

        T getResults();
    }

    public static final ResultCollector<Void> RESULTS_NOOP = new ResultCollector<>(){
        @Override
        public void postRequest() { }
        @Override
        public Void getResults() { return null; }
    };
}
