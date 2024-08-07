package org.labkey.test.stress;

import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.GetCommand;
import org.labkey.remoteapi.miniprofiler.RequestInfo;
import org.labkey.remoteapi.miniprofiler.RequestsResponse;
import org.labkey.remoteapi.miniprofiler.SessionRequestsCommand;

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
        return new SessionResultsCollector(connection);
    }

    public static class SessionResultsCollector extends MiniProfilerResultsCollector
    {
        public SessionResultsCollector(Connection connection)
        {
            super(connection);
        }

        @Override
        protected @NotNull GetCommand<RequestsResponse> getRequestsCommand(long requestIdForCommand)
        {
            return new SessionRequestsCommand(requestIdForCommand);
        }
    }
}
