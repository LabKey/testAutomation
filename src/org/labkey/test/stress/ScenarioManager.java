package org.labkey.test.stress;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.miniprofiler.RecentRequestsCommand;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class ScenarioManager extends AbstractScenarioManager<Void>
{
    private final Map<URI, RequestGate> requestGates = new ConcurrentHashMap<>();
    private final Map<URI, Connection> miniProfilerConnections = new ConcurrentHashMap<>();

    public ScenarioManager(List<Simulation.Definition> simulationDefinitions)
    {
        super(simulationDefinitions);
    }

    @Override
    public ScenarioManager setBaselineDataCollectionDuration(int baselineDataCollectionDuration)
    {
        super.setBaselineDataCollectionDuration(baselineDataCollectionDuration);
        return this;
    }

    public ScenarioManager setMiniProfilerConnections(Connection... miniProfilerConnections)
    {
        Arrays.stream(miniProfilerConnections).forEach(this::verifyAndAddMiniProfilerConnection);
        return this;
    }

    private void verifyAndAddMiniProfilerConnection(Connection connection)
    {
        if (!miniProfilerConnections.containsKey(connection.getBaseURI()))
        {
            try
            {
                new RecentRequestsCommand(Long.MAX_VALUE).execute(connection, null);
                miniProfilerConnections.put(connection.getBaseURI(), connection);
            }
            catch (IOException | CommandException e)
            {
                throw new RuntimeException("Unable to query mini-profiler with provided connection", e);
            }
        }
    }

    @Override
    protected RequestGate getResultsCollector(Connection connection)
    {
        verifyAndAddMiniProfilerConnection(connection);
        return requestGates.computeIfAbsent(connection.getBaseURI(), uri -> new RequestGate());
    }

    public static class RequestGate implements Simulation.ResultCollector<Void>
    {
        private final Semaphore semaphore = new Semaphore(1);
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private final Object waitingRequest = new Object();
        private final int maxUncollectedRequests;

        RequestGate(int maxUncollectedRequests)
        {
            this.maxUncollectedRequests = maxUncollectedRequests;
        }

        RequestGate()
        {
            this(400);
        }

        void reset()
        {
            requestCount.set(0);
            waitingRequest.notifyAll();
        }

        @Override
        public Void getResults()
        {
            return null;
        }

        @Override
        public synchronized void postRequest() throws InterruptedException
        {
            semaphore.acquire();
            if (requestCount.incrementAndGet() > maxUncollectedRequests)
            {
                semaphore.release();
                waitingRequest.wait();
            }
            else
            {
                semaphore.release();
            }
        }
    }
}