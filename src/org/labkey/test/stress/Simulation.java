package org.labkey.test.stress;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.xmlbeans.XmlException;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.collections.ArrayListMap;
import org.labkey.query.xml.ApiTestsDocument;
import org.labkey.query.xml.TestCaseType;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.WhoAmICommand;
import org.labkey.test.credentials.Login;
import org.labkey.test.credentials.Server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

/**
 * Simulation: A series of activities that represents some user workflow (e.g. loading the dashboard, navigating to the sample finder, and performing a search). For simplicity, API simulations in the initial proof of concept will consist of a single activity.
 * @param <T> Data type returned when collecting results
 */
public class Simulation<T>
{
    private final ExecutorService simulationExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final Map<String, String> simulationProperties;
    private final Connection _connection;
    private final List<Activity> _activities;
    private final int _delayBetweenActivities;
    private final ExecutorService _activityExecutor;
    private final Future<Collection<T>> _runningSimulation;
    private final ResultCollector<T> _resultCollector;
    private final boolean _runOnce;

    Simulation(Connection connection, List<Activity> activities, int delayBetweenActivities, int maximumActivityThreads, ResultCollector<T> resultCollector, boolean runOnce, Map<String, String> scenarioProperties)
    {
        _connection = connection;
        _activities = activities;
        _delayBetweenActivities = delayBetweenActivities;
        _activityExecutor = Executors.newFixedThreadPool(maximumActivityThreads);
        _runningSimulation = simulationExecutor.submit(this::run);
        _resultCollector = resultCollector;
        _runOnce = runOnce;
        Map<String, String> temp = new ArrayListMap<>();
        temp.putAll(scenarioProperties);
        temp.put("simulationUuid", UUID.randomUUID().toString());
        simulationProperties = Collections.unmodifiableMap(temp);
    }

    public boolean isStopped()
    {
        return stopped.get();
    }

    public Collection<T> collectResults()
    {
        simulationExecutor.shutdown();
        stopped.set(true);
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
        stopped.set(true);
        simulationExecutor.shutdownNow();
        _activityExecutor.shutdownNow();
    }

    private Collection<T> run() throws ExecutionException, InterruptedException
    {
        // Random sleep to stagger simulations
        Thread.sleep((long) (Math.random() * _delayBetweenActivities));

        do
        {
            for (Activity activity : _activities)
            {
                try
                {
                    runActivity(activity);

                    if (stopped.get())
                    {
                        break;
                    }
                    else
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
        } while (!_runOnce && !stopped.get() && !Thread.interrupted());

        _activityExecutor.shutdown();
        return _resultCollector.getResults();
    }

    private void runActivity(Activity activity) throws ExecutionException, InterruptedException
    {
        List<Future<?>> futures = new ArrayList<>();
        for (Activity.RequestParams request : activity.getRequests())
        {
            if (!stopped.get())
            {
                futures.add(_activityExecutor.submit(() -> {
                    makeRequest(request);
                    return null;
                }));
            }
        }

        for (Future<?> future : futures)
        {
            future.get();
        }
    }

    private void makeRequest(Activity.RequestParams testCase) throws InterruptedException
    {
        ApiTestCommand command = new ApiTestCommand(testCase);
        StopWatch timer = StopWatch.createStarted();
        int statusCode = 0;
        try
        {
            CommandResponse response = command.execute(_connection);
            statusCode = response.getStatusCode();
        }
        catch (CommandException e)
        {
            statusCode = e.getStatusCode();
        }
        catch (IOException e)
        {
            statusCode = -1;
        }
        finally
        {
            timer.stop();
            _resultCollector.postRequest(new RequestResult(testCase, statusCode, timer));
        }
    }

    public static class Definition
    {
        private final Supplier<Connection> _connectionSupplier;

        private int maxActivityThreads = 6; // This seems to be the number of parallel requests browsers handle
        private int delayBetweenActivities = 5_000;
        private List<Activity> activityDefinitions = Collections.emptyList();
        private boolean runOnce = false;
        private Map<String, String> scenarioProperties = new LinkedHashMap<>();

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

        public Definition setScenarioProperties(Map<String, String> scenarioProperties)
        {
            this.scenarioProperties.putAll(scenarioProperties);
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

        public Definition setRunOnce(boolean runOnce)
        {
            this.runOnce = runOnce;
            return this;
        }

        public <T> Simulation<T> startSimulation(Function<Connection, ResultCollector<T>> resultCollectorSupplier) throws IOException, CommandException
        {
            Connection connection = _connectionSupplier.get();
            // Prime connection before starting simulation so that resultCollectorSupplier can know to ignore this request
            new WhoAmICommand().execute(connection, null);
            return new Simulation<>(connection, activityDefinitions, delayBetweenActivities, maxActivityThreads, resultCollectorSupplier.apply(connection), runOnce, scenarioProperties);
        }

        public Simulation<RequestResult> startSimulation() throws IOException, CommandException
        {
            return startSimulation(ClientDataScenario.ClientSideResultsCollector::new);
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

    public interface ResultCollector<T>
    {
        void postRequest(RequestResult requestResult) throws InterruptedException;

        @NotNull Collection<T> getResults();
    }

    public static final ResultCollector<Void> RESULTS_NOOP = new ResultCollector<>(){
        @Override
        public void postRequest(RequestResult requestResult) { }
        @Override
        public @NotNull Collection<Void> getResults() { return Collections.emptyList(); }
    };

    public static class RequestResult
    {
        private final Activity.RequestParams _requestParams;
        private final int _statusCode;
        private final StopWatch _timer;

        public RequestResult(Activity.RequestParams requestParams, int statusCode, StopWatch timer)
        {
            _requestParams = requestParams;
            _statusCode = statusCode;
            _timer = timer;
        }

        public Activity.RequestParams getRequestParams()
        {
            return _requestParams;
        }

        public int getStatusCode()
        {
            return _statusCode;
        }

        public StopWatch getTimer()
        {
            return _timer;
        }
    }
}
