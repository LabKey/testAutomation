package org.labkey.test.stress;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.test.util.TestLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Scenario: A set of coordinated simulations across multiple servers, simulating a particular usage event (e.g. one server with a heavy workload of importing large amounts of data while other servers perform simple read-only activities).
 * @param <T> Result type returned by simulations
 */
public abstract class AbstractScenario<T>
{
    private final UUID scenarioUuid = UUID.randomUUID();
    private final String _scenarioName;
    private final List<Simulation.Definition> _simulationDefinitions;
    private final List<Simulation<T>> simulations = new ArrayList<>();

    private int baselineDataCollectionDuration = 10_000;
    private ScenarioState state = ScenarioState.READY;

    public AbstractScenario(List<Simulation.Definition> simulationDefinitions, String scenarioName)
    {
        if (simulationDefinitions.isEmpty())
        {
            throw new IllegalArgumentException("Must supply simulation definitions to run scenario");
        }
        _simulationDefinitions = simulationDefinitions;
        _scenarioName = scenarioName;
    }

    public AbstractScenario(List<Simulation.Definition> simulationDefinitions)
    {
        this(simulationDefinitions, "Unnamed");
    }

    public AbstractScenario<T> setBaselineDataCollectionDuration(int baselineDataCollectionDuration)
    {
        this.baselineDataCollectionDuration = baselineDataCollectionDuration;
        return this;
    }

    protected abstract Simulation.ResultCollector<T> getResultsCollector(Connection connection);

    public final void startBackgroundSimulations() throws InterruptedException
    {
        if (state != ScenarioState.READY)
        {
            throw new IllegalStateException("Scenario not ready to start: " + state);
        }
        state = ScenarioState.STARTING;
        TestLogger.log("Starting background simulations");
        try
        {
            _simulationDefinitions.parallelStream().forEach(definition -> {
                try
                {
                    simulations.add(definition.startSimulation(this::getResultsCollector));
                }
                catch (IOException | CommandException e)
                {
                    throw new RuntimeException(e);
                }
            });
        }
        catch (Exception e)
        {
            stopBackgroundSimulations();
            throw new RuntimeException("Failed to start simulation", e);
        }

        TestLogger.log("Starting background simulations to collect baseline performance data");
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

        state = ScenarioState.RUNNING;
    }

    public final Set<T> collectBaselinePerfAndStopSimulations() throws InterruptedException
    {
        state = ScenarioState.FINISHING;
        if (!simulations.isEmpty())
        {
            TestLogger.log("Allow background simulations to collect baseline performance data before terminating");
            Thread.sleep(baselineDataCollectionDuration);
            TestLogger.log("Stop background simulations");
        }
        return stopBackgroundSimulations();
    }

    private Set<T> stopBackgroundSimulations()
    {
        state = ScenarioState.FINISHING;
        try
        {
            return simulations.parallelStream().flatMap(simulation -> simulation.collectResults().stream()).collect(Collectors.toSet());
        }
        finally
        {
            state = ScenarioState.DONE;
        }

    }

    private void shutdownNow()
    {
        state = ScenarioState.FINISHING;
        try
        {
            simulations.parallelStream().forEach(Simulation::shutdownNow);
        }
        finally
        {
            state = ScenarioState.DONE;
        }
    }

    public enum ScenarioState
    {
        READY(false),
        STARTING(true),
        RUNNING(true),
        FINISHING(true),
        DONE(false);

        private final boolean _active;

        ScenarioState(boolean active)
        {
            _active = active;
        }

        public boolean isActive()
        {
            return _active;
        }
    }
}
