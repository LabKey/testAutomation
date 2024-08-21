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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This scenario uses the 'mini-profiler-recentRequests' API to collect request info for multiple simulations.<br>
 * All simulations targeting a particular server will share a single data collector.<br>
 * It is not fully implemented yet; {@link SessionDataScenario} should be used until such time as we discover a
 * scenario that it doesn't work for.
 */
public class RecentRequestsScenario extends AbstractScenario<RequestInfo>
{
    private final Map<URI, RecentRequestsResultsCollector> resultsCollectors = new ConcurrentHashMap<>();
    private final Map<URI, Connection> miniProfilerConnections = new ConcurrentHashMap<>();

    /**
     * @param simulationDefinitions Background simulation definitions
     * @param miniProfilerConnections Sets {@link Connection}s to be used to collect {@link RequestInfo}s from server.
     *                               There should be one per server. Any duplicates will be ignored.<br>
     *                               When the scenario is started, if Simulations are found to target servers without a
     *                               {@code Connection}, the {@code Connection} used by that simulation will be used to
     *                               collect {@code RequestInfo}.
     */
    public RecentRequestsScenario(List<Simulation.Definition> simulationDefinitions, Connection... miniProfilerConnections)
    {
        super(simulationDefinitions);
        Arrays.stream(miniProfilerConnections).forEach(this::verifyAndAddMiniProfilerConnection);
    }

    private void verifyAndAddMiniProfilerConnection(Connection connection)
    {
        if (!miniProfilerConnections.containsKey(connection.getBaseURI()))
        {
            try
            {
                // This action should make sure the connection can access recent requests from other users
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
    protected Simulation.ResultCollector<RequestInfo> getResultsCollectorForSimulation(Connection connection)
    {
        verifyAndAddMiniProfilerConnection(connection);
        return resultsCollectors.computeIfAbsent(connection.getBaseURI(), uri -> new RecentRequestsResultsCollector(miniProfilerConnections.get(connection.getBaseURI())));
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