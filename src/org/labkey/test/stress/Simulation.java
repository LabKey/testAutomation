package org.labkey.test.stress;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.xmlbeans.XmlException;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.labkey.query.xml.ApiTestsDocument;
import org.labkey.query.xml.TestCaseType;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.miniprofiler.RequestInfo;
import org.labkey.remoteapi.miniprofiler.RequestsResponse;
import org.labkey.remoteapi.miniprofiler.SessionRequestsCommand;
import org.labkey.remoteapi.security.WhoAmICommand;
import org.labkey.test.credentials.Login;
import org.labkey.test.credentials.Server;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.Timer;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
        // Random sleep to stagger simulations
        Thread.sleep((long) (Math.random() * _delayBetweenActivities));

        while (!_stopped.get() && !Thread.interrupted())
        {
            for (Activity activity : _activities)
            {
                try
                {
                    runActivity(activity, _activityExecutor);

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

    private void runActivity(Activity activity, ExecutorService activityExecutor) throws ExecutionException, InterruptedException
    {
        List<Future<?>> futures = new ArrayList<>();
        for (Activity.RequestParams request : activity.getRequests())
        {
            futures.add(activityExecutor.submit(() -> {
                makeRequest(request);
                return null;
            }));
        }

        for (Future<?> future : futures)
        {
            if (_stopped.get())
            {
                return;
            }
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
        private static final int MAX_TRACKED_REQUESTS = 500;

        private final Connection _connection;
        private final String sessionId;
        private final long initialRequestId;
        private final AtomicBoolean lock = new AtomicBoolean(false);
        private final Map<Long, RequestInfo> requestInfos = new ConcurrentHashMap<>();
        private final AtomicLong maxRequestId = new AtomicLong(0);
        private final AtomicInteger requestCount = new AtomicInteger(0);

        public SessionResultsCollector(Connection connection)
        {
            _connection = connection;
            // Get initial request Id
            RequestsResponse sessionRequests = getSessionRequests();
            sessionId = sessionRequests.getSessionId();
            initialRequestId = sessionRequests.getRequestInfos().stream().map(RequestInfo::getId).max(Comparator.naturalOrder()).orElse(0L);
            maxRequestId.set(initialRequestId);
            requestInfos.clear();
        }

        @Override
        public void postRequest(RequestResult requestResult)
        {
            requestCount.incrementAndGet();

            // Only allow one thread to collect session info
            if (!lock.getAndSet(true))
            {
                try
                {
                    collectSessionRequestInfos();
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
            collectSessionRequestInfos();
            try
            {
                Timer timer = new Timer(Duration.ofSeconds(5));
                while (!timer.isTimedOut() && requestCount.get() > requestInfos.size())
                {
                    // Wait to get lingering request info
                    Thread.sleep(500);
                    collectSessionRequestInfos();
                }
            }
            catch (InterruptedException ignore) { }
            int missingInfos = requestCount.get() - requestInfos.size();
            if (missingInfos != 0)
            {
                TestLogger.warn("Didn't find request info for %d requests".formatted(missingInfos));
            }
            return requestInfos.values();
        }

        private void collectSessionRequestInfos()
        {
            RequestsResponse response = getSessionRequests();
            Assert.assertEquals("Session ID has changed during simulation", sessionId, response.getSessionId());
            List<RequestInfo> responseRequestInfos = response.getRequestInfos();
            if (!responseRequestInfos.isEmpty())
            {
                responseRequestInfos.forEach(ri -> {
                    requestInfos.putIfAbsent(ri.getId(), ri);
                    maxRequestId.getAndUpdate(old -> Math.max(ri.getId(), old));
                });
            }
        }

        private RequestsResponse getSessionRequests()
        {
            try
            {
                long requestIdForCommand = Math.max(initialRequestId, maxRequestId.get() - MAX_TRACKED_REQUESTS);
                SessionRequestsCommand command = new SessionRequestsCommand(requestIdForCommand);
                return command.execute(_connection, null);
            }
            catch (IOException | CommandException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public interface ResultCollector<T>
    {
        void postRequest(RequestResult requestResult) throws InterruptedException;

        Collection<T> getResults();
    }

    public static final ResultCollector<Void> RESULTS_NOOP = new ResultCollector<>(){
        @Override
        public void postRequest(RequestResult requestResult) { }
        @Override
        public Collection<Void> getResults() { return Collections.emptyList(); }
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
