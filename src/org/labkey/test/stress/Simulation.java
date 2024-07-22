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
    private final int _maximumActivityThreads;
    private final ExecutorService _simulationExecutor = Executors.newSingleThreadExecutor();
    private final Future<Object> _runningSimulation;
    private final AtomicBoolean _stopped = new AtomicBoolean(false);

    Simulation(Connection connection, List<Activity> activities, int delayBetweenActivities, int maximumActivityThreads)
    {
        _connection = connection;
        _activities = activities;
        _delayBetweenActivities = delayBetweenActivities;
        _maximumActivityThreads = maximumActivityThreads;
        _runningSimulation = _simulationExecutor.submit(this::startSimulation);
    }

    public Object collectResults() throws ExecutionException, InterruptedException, TimeoutException
    {
        _simulationExecutor.shutdown();
        _stopped.set(true);
        return _runningSimulation.get(60, TimeUnit.SECONDS);
    }

    private Object startSimulation()// throws Exception
    {
        ExecutorService activityExecutor = Executors.newFixedThreadPool(_maximumActivityThreads);
        MultiValuedMap<String, Map<String, Integer>> results = new ArrayListValuedHashMap<>();
        while (!_stopped.get())
        {
            for (Activity activity : _activities)
            {
                Map<String, Integer> activityResult = runActivity(activity, activityExecutor);
                results.put(activity.getName(), activityResult);

                try
                {
                    Thread.sleep(_delayBetweenActivities);
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
        return results;
    }

    private Map<String, Integer> runActivity(Activity activity, ExecutorService activityExecutor)
    {
        Map<String, Future<Integer>> futures = new ArrayListMap<>();
        for (TestCaseType request : activity.getRequests())
        {
            futures.put(request.getName(), activityExecutor.submit(() -> makeRequest(request)));
        }

        Map<String, Integer> results = new ArrayListMap<>();
        for (Map.Entry<String, Future<Integer>> entry : futures.entrySet())
        {
            Integer result;
            try
            {
                result = entry.getValue().get();
            }
            catch (InterruptedException | ExecutionException e)
            {
                result = -2;
            }
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


    public static class Builder
    {
        private final Supplier<Connection> _connectionSupplier;

        private int maxActivityThreads = 6;
        private int delayBetweenActivities = 5_000;
        private List<Activity> activityDefinitions = Collections.emptyList();

        public Builder(Supplier<Connection> connectionSupplier)
        {
            _connectionSupplier = connectionSupplier;
        }

        public Builder(String baseUrl, String username, String password)
        {
            this(() -> new Connection(baseUrl, username, password));
        }

        public Builder(String baseUrl, Login login)
        {
            this(baseUrl, login.getUsername(), login.getPassword());
        }

        public Builder(Server server)
        {
            this(server.getHost(), server.getLogins().get(0));
        }

        public Builder setMaxActivityThreads(int maxActivityThreads)
        {
            this.maxActivityThreads = maxActivityThreads;
            return this;
        }

        public Builder setDelayBetweenActivities(int delayBetweenActivities)
        {
            this.delayBetweenActivities = delayBetweenActivities;
            return this;
        }

        public Builder setActivityFiles(File... activityFiles)
        {
            activityDefinitions = Arrays.stream(activityFiles).map(f -> new Activity(f.getName(), Builder.parseTests(f))).toList();
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
