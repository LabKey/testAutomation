package org.labkey.test.stress;

import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.miniprofiler.RequestInfo;
import org.labkey.test.BaseWebDriverTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Category({})
public abstract class BaseBackgroundLoadTest extends BaseWebDriverTest
{
    private final List<Simulation<RequestInfo>> _simulations = new ArrayList<>();
    private final List<RequestInfo> _requestInfos = new ArrayList<>();

    protected abstract List<Simulation.Definition> getSimulationDefinitions();
    protected int getBaselineDataCollectionDuration()
    {
        return 10_000;
    }

    @Before
    public final void startSimulationsAndCollectBaselinePerf() throws InterruptedException
    {
        log("Starting background simulations to collect baseline performance data");
        startBackgroundSimulations();
        Thread.sleep(getBaselineDataCollectionDuration());
        for (Simulation<?> simulation : _simulations)
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
            for (Simulation.Definition definition : getSimulationDefinitions())
            {
                _simulations.add(definition.startSimulation());
            }
        }
        catch (IOException | CommandException e)
        {
            stopBackgroundSimulations();
            throw new RuntimeException(e);
        }
    }

    @After
    public final void collectBaselinePerfAndStopSimulations() throws InterruptedException
    {
        if (!_simulations.isEmpty())
        {
            log("Allow background simulations to collect baseline performance data before terminating");
            Thread.sleep(getBaselineDataCollectionDuration());
            log("Stop background simulations");
            stopBackgroundSimulations();
        }
    }

    private void stopBackgroundSimulations()
    {
        for (Simulation<RequestInfo> simulation : _simulations)
        {
            _requestInfos.addAll(simulation.collectResults());
        }
    }
}
