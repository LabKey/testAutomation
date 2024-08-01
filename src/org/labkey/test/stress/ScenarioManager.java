package org.labkey.test.stress;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.miniprofiler.RecentRequestsCommand;
import org.labkey.remoteapi.miniprofiler.RequestInfo;
import org.labkey.test.util.TestLogger;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class ScenarioManager
{
    private final Map<URI, RequestGate> requestGates = new ConcurrentHashMap<>();
    private final Map<URI, Connection> miniProfilerConnections = new ConcurrentHashMap<>();
    private final List<Simulation.Definition> simulationDefinitions;
    private final List<Simulation<?>> simulations = new ArrayList<>();
    private final List<RequestInfo> requestInfos = new ArrayList<>();

    private int baselineDataCollectionDuration = 10_000;

    public ScenarioManager(List<Simulation.Definition> simulationDefinitions)
    {
        this.simulationDefinitions = simulationDefinitions;
    }

    public ScenarioManager setBaselineDataCollectionDuration(int baselineDataCollectionDuration)
    {
        this.baselineDataCollectionDuration = baselineDataCollectionDuration;
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

    private RequestGate getGateForConnection(Connection connection)
    {
        verifyAndAddMiniProfilerConnection(connection);
        return requestGates.computeIfAbsent(connection.getBaseURI(), uri -> new RequestGate());
    }

    public final void startBackgroundSimulations() throws InterruptedException
    {
        TestLogger.log("Starting background simulations to collect baseline performance data");
        try
        {
            for (Simulation.Definition definition : simulationDefinitions)
            {
                simulations.add(definition.startSimulation(this::getGateForConnection));
            }
        }
        catch (Exception e)
        {
            stopBackgroundSimulations();
            throw new RuntimeException("Failed to start simulation", e);
        }

        Thread.sleep(baselineDataCollectionDuration);

        // Verify that simulations haven't already errored out
        for (Simulation<?> simulation : simulations)
        {
            if (simulation.isStopped())
            {
                // Something probably went wrong. This should throw an error.
                simulation.collectResults();
            }
        }
    }

    public final List<RequestInfo> collectBaselinePerfAndStopSimulations() throws InterruptedException
    {
        if (!simulations.isEmpty())
        {
            TestLogger.log("Allow background simulations to collect baseline performance data before terminating");
            Thread.sleep(baselineDataCollectionDuration);
            TestLogger.log("Stop background simulations");
            stopBackgroundSimulations();
        }
        return Collections.unmodifiableList(requestInfos);
    }

    private void stopBackgroundSimulations()
    {
        for (Simulation<?> simulation : simulations)
        {
            simulation.collectResults();
        }
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