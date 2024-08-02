package org.labkey.test.stress;

import org.labkey.remoteapi.Connection;
import org.labkey.test.util.TestLogger;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractScenarioManager<T>
{
    private final List<Simulation.Definition> simulationDefinitions;
    private final List<Simulation<T>> simulations = new ArrayList<>();

    private int baselineDataCollectionDuration = 10_000;

    public AbstractScenarioManager(List<Simulation.Definition> simulationDefinitions)
    {
        this.simulationDefinitions = simulationDefinitions;
    }

    public AbstractScenarioManager<T> setBaselineDataCollectionDuration(int baselineDataCollectionDuration)
    {
        this.baselineDataCollectionDuration = baselineDataCollectionDuration;
        return this;
    }

    protected abstract Simulation.ResultCollector<T> getResultsCollector(Connection connection);

    public final void startBackgroundSimulations() throws InterruptedException
    {
        TestLogger.log("Starting background simulations to collect baseline performance data");
        try
        {
            for (Simulation.Definition definition : simulationDefinitions)
            {
                simulations.add(definition.startSimulation(this::getResultsCollector));
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
                stopBackgroundSimulations();
            }
        }
    }

    public final List<T> collectBaselinePerfAndStopSimulations() throws InterruptedException
    {
        if (!simulations.isEmpty())
        {
            TestLogger.log("Allow background simulations to collect baseline performance data before terminating");
            Thread.sleep(baselineDataCollectionDuration);
            TestLogger.log("Stop background simulations");
        }
        return stopBackgroundSimulations();
    }

    private List<T> stopBackgroundSimulations()
    {
        return simulations.stream().map(Simulation::collectResults).toList();
    }

}
