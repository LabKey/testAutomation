package org.labkey.test.stress;

import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.GetCommand;
import org.labkey.remoteapi.miniprofiler.RecentRequestsCommand;
import org.labkey.remoteapi.miniprofiler.RequestInfo;
import org.labkey.remoteapi.miniprofiler.RequestsResponse;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RecentRequestsScenario extends AbstractScenario<RequestInfo>
{
    private final Map<URI, RecentRequestsResultsCollector> requestGates = new ConcurrentHashMap<>();
    private final Map<URI, Connection> miniProfilerConnections = new ConcurrentHashMap<>();

    public RecentRequestsScenario(List<Simulation.Definition> simulationDefinitions)
    {
        super(simulationDefinitions);
    }

    @Override
    public RecentRequestsScenario setBaselineDataCollectionDuration(Duration baselineDataCollectionDuration)
    {
        super.setBaselineDataCollectionDuration(baselineDataCollectionDuration);
        return this;
    }

    public RecentRequestsScenario setMiniProfilerConnections(Connection... miniProfilerConnections)
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

    @Override
    protected Simulation.ResultCollector<RequestInfo> getResultsCollector(Connection connection)
    {
        verifyAndAddMiniProfilerConnection(connection);
        return requestGates.computeIfAbsent(connection.getBaseURI(), uri -> new RecentRequestsResultsCollector(miniProfilerConnections.get(connection.getBaseURI())));
    }

    public static class RecentRequestsResultsCollector extends MiniProfilerResultsCollector
    {
        public RecentRequestsResultsCollector(Connection connection)
        {
            super(connection);
        }

        @Override
        protected @NotNull GetCommand<RequestsResponse> getRequestsCommand(long requestIdForCommand)
        {
            return new RecentRequestsCommand(requestIdForCommand);
        }

        @Override
        public @NotNull Collection<RequestInfo> getResults()
        {
            return super.getResults().stream().filter(ri -> !ri.getUrl().contains("whoami.api")).toList();
        }
    }
}