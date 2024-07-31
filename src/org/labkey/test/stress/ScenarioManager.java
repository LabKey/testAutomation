package org.labkey.test.stress;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.miniprofiler.RequestInfo;
import org.labkey.test.util.TestLogger;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class ScenarioManager
{
    private final Map<URI, RequestGate> _requestGates = new HashMap<>();
    private final List<Simulation.Definition> simulationDefinitions;
    private final List<Simulation> _simulations = new ArrayList<>();
    private final List<RequestInfo> _requestInfos = new ArrayList<>();
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

    private RequestGate getGateForConnection(Connection connection)
    {
        return _requestGates.computeIfAbsent(connection.getBaseURI(), uri -> new RequestGate());
    }

    public final void startSimulationsAndCollectBaselinePerf() throws InterruptedException
    {
        TestLogger.log("Starting background simulations to collect baseline performance data");
        startBackgroundSimulations();
        Thread.sleep(baselineDataCollectionDuration);
        for (Simulation simulation : _simulations)
        {
            if (simulation.isStopped())
            {
                // Something probably went wrong. This should throw an error.
                simulation.collectResults();
            }
        }
    }

    private void startBackgroundSimulations()
    {
        try
        {
            for (Simulation.Definition definition : simulationDefinitions)
            {
                _simulations.add(definition.startSimulation(this::getGateForConnection));
            }
        }
        catch (IOException | CommandException e)
        {
            stopBackgroundSimulations();
            throw new RuntimeException(e);
        }
    }

    public final void collectBaselinePerfAndStopSimulations() throws InterruptedException
    {
        if (!_simulations.isEmpty())
        {
            TestLogger.log("Allow background simulations to collect baseline performance data before terminating");
            Thread.sleep(baselineDataCollectionDuration);
            TestLogger.log("Stop background simulations");
            stopBackgroundSimulations();
        }
    }

    private void stopBackgroundSimulations()
    {
        for (Simulation simulation : _simulations)
        {
            _requestInfos.addAll(simulation.collectResults());
        }
    }

    public static class RequestGate
    {
        public static final RequestGate noop = new RequestGate(){
            @Override
            public void preRequest() { }
        };

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

        public synchronized void preRequest() throws InterruptedException
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