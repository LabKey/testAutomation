package org.labkey.test.stress;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.test.util.TestDateUtils;
import org.labkey.test.util.TestLogger;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Scenario: A set of coordinated simulations across multiple servers, simulating a particular usage event (e.g. one server with a heavy workload of importing large amounts of data while other servers perform simple read-only activities).
 * This will manage a group of simulations. {@link #startScenario()} will begin the scenario by running all provided simulations in parallel. The method will block briefly to allow simulations to collect some baseline performance data. Then the caller should perform other operations that are expected to impact the performance of the background simulations.<br>
 * {@link #finishScenario()} will end the scenario. It will sleep briefly to allow background simulations to collect additional baseline performance data. Then it will stop all simulations. If not writing results to a file, {@link #finishScenario()} will return the simulations' combined results.
 * <pre>{@code
 * @Test
 * public void testPerformace()
 * {
 *   SessionDataScenario scenario = new SessionDataScenario(getSimulationDefinitions(), "BackgroundDashboardLoad", TestFileUtils.ensureTestTempDir());
 *   scenario.startScenario();
 *   // perform foreground simulations via browser or API
 *   scenario.finishScenario();
 * }
 * }</pre>
 * @param <T> Result type returned by simulations
 * @see Simulation
 */
public abstract class AbstractScenario<T>
{
    public static final String SCENARIO_UUID = "scenarioUuid";
    public static final String SCENARIO_NAME = "scenarioName";

    private final String _scenarioUuid = UUID.randomUUID().toString();
    private final String _scenarioName;
    private final List<Simulation<T>> _simulations = new CopyOnWriteArrayList<>();
    private final List<Simulation.Definition> _simulationDefinitions;
    private final File _resultsFile;

    private Duration baselineDataCollectionDuration = Duration.ofSeconds(10);
    private ScenarioState state = ScenarioState.READY;

    /**
     * @param simulationDefinitions Must not be empty
     * @param scenarioName name will be included in the scenario's metadata
     * @param resultsDir If {@code null}, results will be returned by {@link #finishScenario()}. Otherwise, results will be written to a tsv within the directory
     */
    public AbstractScenario(@NotNull List<Simulation.Definition> simulationDefinitions, String scenarioName, @Nullable File resultsDir)
    {
        if (simulationDefinitions.isEmpty())
        {
            throw new IllegalArgumentException("Must supply simulation definitions to run scenario");
        }
        _simulationDefinitions = simulationDefinitions;
        _scenarioName = scenarioName;
        _resultsFile = resultsDir == null ? null : new File(resultsDir, scenarioName + "-" + TestDateUtils.dateTimeFileName() + ".tsv");
    }

    /**
     * Basic scenario that stores results in memory
     * @param simulationDefinitions must not be empty
     */
    public AbstractScenario(@NotNull List<Simulation.Definition> simulationDefinitions)
    {
        this(simulationDefinitions, "Unnamed", null);
    }

    /**
     * Time to let simulations run in the background before allowing foreground stress simulations to begin. The
     * scenario will also let the background simulations run after foreground simulations are complete.
     * @param duration time to allow background simulations to run
     * @return {@code this}
     */
    public AbstractScenario<T> setBaselineDataCollectionDuration(Duration duration)
    {
        this.baselineDataCollectionDuration = duration;
        return this;
    }

    /**
     * If a directory was specified to write results, returns the actual file with the generated name.
     * @return results file or {@code null} if no results file location was specified
     */
    public File getResultsFile()
    {
        return _resultsFile;
    }

    /**
     * Will be called to generate an appropriate results collector for each simulation as they are started
     * @param connection The connection used by the simulation
     * @return a {@link org.labkey.test.stress.Simulation.ResultCollector} to be used by a simulation
     */
    protected abstract Simulation.ResultCollector<T> getResultsCollectorForSimulation(Connection connection);

    /**
     * Starts all background simulations and waits for them to collect some baseline performance data
     * @throws InterruptedException thrown by {@link Thread#sleep(long)}
     * @see #setBaselineDataCollectionDuration(Duration)
     */
    public final void startScenario() throws InterruptedException
    {
        if (state != ScenarioState.READY)
        {
            throw new IllegalStateException("Unable to start scenario, already " + state);
        }
        state = ScenarioState.STARTING;
        TestLogger.log("Starting %d background simulations".formatted(_simulationDefinitions.size()));
        try
        {
            _simulationDefinitions.parallelStream().forEach(definition -> {
                try
                {
                    _simulations.add(definition.startSimulation(this::getResultsCollectorForSimulation));
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
        for (Simulation<?> simulation : _simulations)
        {
            if (simulation.isStopped())
            {
                // Something probably went wrong. This should throw an error.
                stopBackgroundSimulations();
                throw new IllegalStateException("Background simulation(s) stopped prematurely");
            }
        }

        state = ScenarioState.RUNNING;
    }

    /**
     * Get scenario metadata for use in scenario results
     * @return Map of metadata
     */
    public @NotNull Map<String, String> getScenarioMetadata()
    {
        return Map.of(SCENARIO_UUID, _scenarioUuid, SCENARIO_NAME, _scenarioName);
    }

    /**
     * Stop background simulations gracefully after allowing them to collect more baseline performance data
     * @return Combined results of all simulations
     * @throws InterruptedException thrown by {@link Thread#sleep(long)}
     */
    public final Set<T> finishScenario() throws InterruptedException
    {
        state = ScenarioState.FINISHING;
        if (!_simulations.isEmpty())
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

    /**
     * Stop all background simulations gracefully
     * @return Combined results of all simulations
     */
    private Set<T> stopBackgroundSimulations()
    {
        state = ScenarioState.FINISHING;
        try
        {
            return _simulations.parallelStream().flatMap(simulation -> simulation.collectResults().stream()).collect(Collectors.toSet());
        }
        finally
        {
            state = ScenarioState.DONE;
            afterDone();
        }
    }

    /**
     * Force background simulations to stop immediately
     */
    private void shutdownNow()
    {
        state = ScenarioState.FINISHING;
        try
        {
            _simulations.parallelStream().forEach(Simulation::shutdownNow);
        }
        finally
        {
            state = ScenarioState.DONE;
            afterDone();
        }
    }

    /**
     * Called after stopping background simulations (gracefully or not).<br>
     * Allows cleanup such as closing output streams.
     */
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
