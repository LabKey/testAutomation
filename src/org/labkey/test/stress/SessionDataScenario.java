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

public class SessionDataScenario extends AbstractScenario<RequestInfo>
{
    private TsvResultsWriter<RequestInfo> resultsWriter;

    public SessionDataScenario(List<Simulation.Definition> simulationDefinitions, String scenarioName, File resultsFile)
    {
        super(simulationDefinitions, scenarioName, resultsFile);
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
    protected void afterDone()
    {
        Optional.ofNullable(getResultsWriter()).ifPresent(TsvResultsWriter::close);
    }

    @Override
    protected Simulation.ResultCollector<RequestInfo> getResultsCollector(Connection connection)
    {
        return new SessionResultsCollector(connection, getResultsWriter(), getScenarioProperties());
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
                return RequestInfo.BLANK;
            }
            else
            {
                return super.processNewRequest(requestInfo);
            }
        }
    }
}
