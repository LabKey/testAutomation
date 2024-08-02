package org.labkey.test.stress;

import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.miniprofiler.RequestInfo;

import java.util.List;

public class SessionDataScenario extends AbstractScenario<RequestInfo>
{
    public SessionDataScenario(List<Simulation.Definition> simulationDefinitions)
    {
        super(simulationDefinitions);
    }

    @Override
    protected Simulation.ResultCollector<RequestInfo> getResultsCollector(Connection connection)
    {
        return new Simulation.SessionResultsCollector(connection);
    }
}
