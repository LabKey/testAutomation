package org.labkey.test.stress;

import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.Connection;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientDataScenario extends AbstractScenario<Simulation.RequestResult>
{
    public ClientDataScenario(List<Simulation.Definition> simulationDefinitions)
    {
        super(simulationDefinitions);
    }

    @Override
    protected Simulation.ResultCollector<Simulation.RequestResult> getResultsCollector(Connection connection)
    {
        return new ClientSideResultsCollector(connection);
    }

    static class ClientSideResultsCollector implements Simulation.ResultCollector<Simulation.RequestResult>
    {
        private final List<Simulation.RequestResult> results = new CopyOnWriteArrayList<>();

        public ClientSideResultsCollector(Connection ignored) { }

        @Override
        public void postRequest(Simulation.RequestResult requestResult) throws InterruptedException
        {
            results.add(requestResult);
        }

        @Override
        public @NotNull Collection<Simulation.RequestResult> getResults()
        {
            return Collections.unmodifiableList(results);
        }
    }
}
