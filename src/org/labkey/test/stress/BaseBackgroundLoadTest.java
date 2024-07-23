package org.labkey.test.stress;

import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Category({})
public abstract class BaseBackgroundLoadTest extends BaseWebDriverTest
{
    private final List<Simulation> _simulations = new ArrayList<>();

    protected abstract List<Simulation.Definition> getSimulationDefinitions();
    protected int getBaselineDataCollectionDuration()
    {
        return 30_000;
    }

    @Before
    public final void startSimulationsAndCollectBaselinePerf() throws InterruptedException
    {
        startBackgroundSimulations();
        Thread.sleep(getBaselineDataCollectionDuration());
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
        Thread.sleep(getBaselineDataCollectionDuration());
        stopBackgroundSimulations();
    }

    private void stopBackgroundSimulations()
    {
        for (Simulation simulation : _simulations)
        {
            simulation.collectResults();
        }
    }

}
