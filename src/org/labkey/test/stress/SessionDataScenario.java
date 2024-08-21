package org.labkey.test.stress;

import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.GetCommand;
import org.labkey.remoteapi.miniprofiler.RequestInfo;
import org.labkey.remoteapi.miniprofiler.RequestsResponse;
import org.labkey.remoteapi.miniprofiler.SessionRequestsCommand;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This scenario uses the 'mini-profiler-sessionRequest' API to collect request info for multiple simulations.<br>
 * Each simulation will have a separate results collector instance that will collect request info using that
 * simulation's {@link Connection}, thus sharing its session ID.<br>
 */
public class SessionDataScenario extends AbstractScenario<RequestInfo>
{
    private TsvResultsWriter<RequestInfo> resultsWriter;

    /**
     * @see AbstractScenario#AbstractScenario(List, String, File)
     */
    public SessionDataScenario(List<Simulation.Definition> simulationDefinitions, String scenarioName, File resultsDir)
    {
        super(simulationDefinitions, scenarioName, resultsDir);
    }

    public SessionDataScenario(List<Simulation.Definition> simulationDefinitions)
    {
        super(simulationDefinitions);
    }

    public synchronized TsvResultsWriter<RequestInfo> getResultsWriter()
    {
        if (resultsWriter == null && getResultsFile() != null)
        {
            try
            {
                resultsWriter = new RequestInfoTsvWriter(getResultsFile());
            }
            catch (FileNotFoundException e)
            {
                throw new RuntimeException(e);
            }
        }
        return resultsWriter;
    }

    @Override
    protected void afterComplete()
    {
        Optional.ofNullable(getResultsWriter()).ifPresent(TsvResultsWriter::close);
    }

    @Override
    protected Simulation.ResultCollector<RequestInfo> getResultsCollectorForSimulation(Connection connection)
    {
        return new SessionResultsCollector(connection, getResultsWriter(), getScenarioMetadata());
    }

    public static class SessionResultsCollector extends MiniProfilerResultsCollector
    {
        private final TsvResultsWriter<RequestInfo> _resultsWriter;
        private final Map<String, String> _metadata;

        public SessionResultsCollector(Connection connection, TsvResultsWriter<RequestInfo> resultsWriter, Map<String, String> scenarioProperties)
        {
            super(connection);
            _resultsWriter = resultsWriter;
            Map<String, String> temp = new HashMap<>(scenarioProperties);
            temp.put(Simulation.SERVER_URI, getConnection().getBaseURI().toString());
            _metadata = Collections.unmodifiableMap(temp);
        }

        @Override
        protected @NotNull GetCommand<RequestsResponse> getRequestsCommand(long requestIdForCommand)
        {
            return new SessionRequestsCommand(requestIdForCommand);
        }

        @Override
        protected RequestInfo processNewRequest(RequestInfo requestInfo)
        {
            if (_resultsWriter != null)
            {
                _resultsWriter.writeRow(requestInfo, _metadata);
                return RequestInfo.BLANK; // Don't store requestInfos in memory
            }
            else
            {
                return super.processNewRequest(requestInfo);
            }
        }
    }
}
