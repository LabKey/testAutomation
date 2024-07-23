package org.labkey.test.stress;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.xmlbeans.XmlException;
import org.labkey.query.xml.ApiTestsDocument;
import org.labkey.query.xml.TestCaseType;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.WhoAmICommand;
import org.labkey.serverapi.collections.ArrayListMap;
import org.labkey.test.credentials.Login;
import org.labkey.test.credentials.Server;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class Simulation
{
    private final Connection _connection;
    private final List<Activity> _activities;
    private final int _delayBetweenActivities;
    private final ExecutorService _simulationExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService _activityExecutor;
    private final Future<Object> _runningSimulation;
    private final AtomicBoolean _stopped = new AtomicBoolean(false);

    Simulation(Connection connection, List<Activity> activities, int delayBetweenActivities, int maximumActivityThreads)
    {
        _connection = connection;
        _activities = activities;
        _delayBetweenActivities = delayBetweenActivities;
        _activityExecutor = Executors.newFixedThreadPool(maximumActivityThreads);
        _runningSimulation = _simulationExecutor.submit(this::startSimulation);
    }

    public Object collectResults()
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

    private Object startSimulation()
    {
        MultiValuedMap<String, Map<String, Integer>> results = new ArrayListValuedHashMap<>();
        while (!_stopped.get() && !Thread.interrupted())
        {
            for (Activity activity : _activities)
            {
                try
                {
                    Map<String, Integer> activityResult = runActivity(activity, _activityExecutor);
                    results.put(activity.getName(), activityResult);

                    Thread.sleep(_delayBetweenActivities);
                }
                catch (ExecutionException | InterruptedException e)
                {
                    shutdownNow();
                    return results;
                }
            }
        }
        return results;
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

    private int makeRequest(TestCaseType testCase)
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
    }


    public static class Definition
    {
        private final Supplier<Connection> _connectionSupplier;

        private int maxActivityThreads = 6;
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
            activityDefinitions = Arrays.stream(activityFiles).map(f -> new Activity(f.getName(), Definition.parseTests(f))).toList();
            return this;
        }

        public Simulation startSimulation() throws IOException, CommandException
        {
            Connection connection = _connectionSupplier.get();
            new WhoAmICommand().execute(connection, null);
            return new Simulation(connection, activityDefinitions, delayBetweenActivities, maxActivityThreads);
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

    }
}
