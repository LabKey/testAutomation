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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Simulation: A series of activities that represents some user workflow (e.g. loading the dashboard, navigating to the sample finder, and performing a search). For simplicity, API simulations in the initial proof of concept will consist of a single activity.
 * @param <T> Data type returned when collecting results
 */
public class Simulation<T>
{
    private final Connection _connection;
    private final List<Activity> _activities;
    private final int _delayBetweenActivities;
    private final ExecutorService _simulationExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService _activityExecutor;
    private final Future<Collection<T>> _runningSimulation;
    private final AtomicBoolean _stopped = new AtomicBoolean(false);
    private final ResultCollector<T> _resultCollector;

    Simulation(Connection connection, List<Activity> activities, int delayBetweenActivities, int maximumActivityThreads, ResultCollector<T> resultCollector)
    {
        _connection = connection;
        _activities = activities;
        _delayBetweenActivities = delayBetweenActivities;
        _activityExecutor = Executors.newFixedThreadPool(maximumActivityThreads);
        _runningSimulation = _simulationExecutor.submit(this::startSimulation);
        _resultCollector = resultCollector;
    }

    public boolean isStopped()
    {
        return _stopped.get();
    }

    public Collection<T> collectResults()
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

    private Collection<T> startSimulation() throws ExecutionException, InterruptedException
    {
        while (!_stopped.get() && !Thread.interrupted())
        {
            for (Activity activity : _activities)
            {
                try
                {
                    runActivity(activity, _activityExecutor);
                    _resultCollector.postRequest();

                    if (!_stopped.get())
                    {
                        Thread.sleep(_delayBetweenActivities);
                    }
                }
                catch (Exception e)
                {
                    shutdownNow();
                    throw e;
                }
            }
        }
        return _resultCollector.getResults();
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

    public static Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();
    public static AtomicInteger requestCount = new AtomicInteger(0);
    public static AtomicInteger badRequestCount = new AtomicInteger(0);
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
            _resultCollector.postRequest();
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

        public <T> Simulation<T> startSimulation(Function<Connection, ResultCollector<T>> resultCollectorSupplier) throws IOException, CommandException
        {
            Connection connection = _connectionSupplier.get();
            new WhoAmICommand().execute(connection, null);
            return new Simulation<>(connection, activityDefinitions, delayBetweenActivities, maxActivityThreads, resultCollectorSupplier.apply(connection));
        }

        public Simulation<RequestInfo> startSimulation() throws IOException, CommandException
        {
            return startSimulation(SessionResultsCollector::new);
        }

        public Simulation<Void> startSimulationWithoutMiniProfiler() throws IOException, CommandException
        {
            return startSimulation(connection -> RESULTS_NOOP);
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

    public static class SessionResultsCollector implements ResultCollector<RequestInfo>
    {
        private final Connection _connection;
        private final AtomicBoolean lock = new AtomicBoolean(false);
        private final Map<Long, RequestInfo> requestInfos = new ConcurrentHashMap<>();
        private final AtomicLong requestId = new AtomicLong(0);

        public SessionResultsCollector(Connection connection)
        {
            _connection = connection;
            // Get initial request Id
            collectRequestInfos();
            requestInfos.clear();
        }

        @Override
        public void postRequest()
        {
            if (!lock.getAndSet(true))
            {
                try
                {
                    collectRequestInfos();
                }
                finally
                {
                    lock.set(false);
                }
            }
        }

        @Override
        public Collection<RequestInfo> getResults()
        {
            collectRequestInfos();
            return requestInfos.values();
        }

        private void collectRequestInfos()
        {
            try
            {
                SessionRequestsCommand command = new SessionRequestsCommand(requestId.get());
                RequestsResponse response = command.execute(_connection, null);
                List<RequestInfo> requestInfos = response.getRequestInfos();
                if (!requestInfos.isEmpty())
                {
                    synchronized (requestId)
                    {
                        requestInfos.forEach(ri -> {
                            this.requestInfos.put(ri.getId(), ri);
                            if (ri.getId() > requestId.get())
                            {
                                requestId.set(ri.getId());
                            }
                        });
                    }
                }
            }
            catch (IOException | CommandException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public interface ResultCollector<T>
    {
        void postRequest() throws InterruptedException;

        Collection<T> getResults();
    }

    public static final ResultCollector<Void> RESULTS_NOOP = new ResultCollector<>(){
        @Override
        public void postRequest() { }
        @Override
        public Collection<Void> getResults() { return Collections.emptyList(); }
    };
}
