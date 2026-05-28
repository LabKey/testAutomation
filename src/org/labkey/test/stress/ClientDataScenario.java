/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.stress;

import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.Connection;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Collects perf data solely from client-side information
 * @see AbstractScenario
 */
public class ClientDataScenario extends AbstractScenario<Simulation.RequestResult>
{
    public ClientDataScenario(List<Simulation.Definition> simulationDefinitions)
    {
        super(simulationDefinitions);
    }

    @Override
    protected Simulation.ResultCollector<Simulation.RequestResult> getResultsCollectorForSimulation(Connection connection)
    {
        return new ClientSideResultsCollector(connection);
    }

    static class ClientSideResultsCollector implements Simulation.ResultCollector<Simulation.RequestResult>
    {
        private final List<Simulation.RequestResult> results = new CopyOnWriteArrayList<>();

        public ClientSideResultsCollector(Connection ignored) { }

        @Override
        public void submitResult(Simulation.RequestResult requestResult) throws InterruptedException
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
