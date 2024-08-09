package org.labkey.test.stress;

import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.test.util.TestDateUtils;
import org.labkey.test.util.TestLogger;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Scenario: A set of coordinated simulations across multiple servers, simulating a particular usage event (e.g. one server with a heavy workload of importing large amounts of data while other servers perform simple read-only activities).
 * @param <T> Result type returned by simulations
 */
public abstract class AbstractScenario<T>
{
    public static final String SCENARIO_UUID = "scenarioUuid";
    public static final String SCENARIO_NAME = "scenarioName";

    private final String scenarioUuid = UUID.randomUUID().toString();
    private final List<Simulation<T>> simulations = new ArrayList<>();
    private final String _scenarioName;
    private final List<Simulation.Definition> _simulationDefinitions;
    private final File _resultsFile;

    private Duration baselineDataCollectionDuration = Duration.ofSeconds(10);
    private ScenarioState state = ScenarioState.READY;

    public AbstractScenario(List<Simulation.Definition> simulationDefinitions, String scenarioName, File resultsFile)
    {
        if (simulationDefinitions.isEmpty())
        {
            throw new IllegalArgumentException("Must supply simulation definitions to run scenario");
        }
        _simulationDefinitions = simulationDefinitions;
        _scenarioName = scenarioName;
        _resultsFile = resultsFile == null ? null : (resultsFile.isDirectory() ? new File(resultsFile, scenarioName + "-" + TestDateUtils.dateTimeFileName() + ".tsv") : resultsFile);
    }

    public AbstractScenario(List<Simulation.Definition> simulationDefinitions)
    {
        this(simulationDefinitions, "Unnamed", null);
    }

    public AbstractScenario<T> setBaselineDataCollectionDuration(Duration baselineDataCollectionDuration)
    {
        this.baselineDataCollectionDuration = baselineDataCollectionDuration;
        return this;
    }

    public File getResultsFile()
    {
        return _resultsFile;
    }

    protected abstract Simulation.ResultCollector<T> getResultsCollector(Connection connection);

    public final void startBackgroundSimulations() throws InterruptedException
    {
        if (state != ScenarioState.READY)
        {
            throw new IllegalStateException("Unable to start scenario, already " + state);
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

        if (baselineDataCollectionDuration.toMillis() > 0)
        {
            TestLogger.log("Letting background simulations run for %s to collect baseline performance data"
                    .formatted(TestDateUtils.durationString(baselineDataCollectionDuration)));
            Thread.sleep(baselineDataCollectionDuration.toMillis());
        }

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

    @NotNull
    public Map<String, String> getScenarioProperties()
    {
        return Map.of(SCENARIO_UUID, scenarioUuid, SCENARIO_NAME, _scenarioName);
    }

    public final Set<T> collectBaselinePerfAndStopSimulations() throws InterruptedException
    {
        state = ScenarioState.FINISHING;
        if (!simulations.isEmpty())
        {
            if (baselineDataCollectionDuration.toMillis() > 0)
            {
                TestLogger.log("Allow background simulations to collect baseline performance data before terminating");
                Thread.sleep(baselineDataCollectionDuration.toMillis());
            }
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
            afterDone();
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
            afterDone();
        }
    }

    protected void afterDone() { }

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

    public interface TsvResultsWriter<T>
    {
        void writeRow(T resultsObject, Map<String, ?> resultMetadata);
        void close();
    }
}
