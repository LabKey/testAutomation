package org.labkey.test.stress;

import org.labkey.remoteapi.miniprofiler.RequestInfo;
import org.labkey.serverapi.writer.PrintWriters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.labkey.test.stress.AbstractScenario.SCENARIO_NAME;
import static org.labkey.test.stress.AbstractScenario.SCENARIO_UUID;
import static org.labkey.test.stress.Simulation.SERVER_URI;
import static org.labkey.test.stress.Simulation.SIMULATION_ID;

public class RequestInfoTsvWriter implements AbstractScenario.TsvResultsWriter<RequestInfo>
{
    // scenarioUuid (pk) | scenarioName | serverUri (pk) | simulationId | requestId (pk) | requestUrl | startTime | duration | queryCount
    private static final List<String> FIELDS = List.of(
            SCENARIO_UUID,
            SCENARIO_NAME,
            SERVER_URI,
            SIMULATION_ID,
            "requestId",
            "requestUrl",
            "startTime",
            "duration",
            "queryCount");

    private static final Map<String, Function<RequestInfo, String>> REQUEST_INFO_MAPPER = Map.of(
            SIMULATION_ID, RequestInfo::getSessionId,
            "requestId", ri -> ri.getId().toString(),
            "requestUrl", RequestInfo::getUrl,
            "startTime", RequestInfo::getDate,
            "duration", ri -> ri.getDuration().toString(),
            "queryCount", ri -> String.valueOf(ri.getRoot().getCustomTimings().getOrDefault("sql", Collections.emptyList()).size())
    );

    private final File _file;
    private final PrintWriter printWriter;

    public RequestInfoTsvWriter(File file) throws FileNotFoundException
    {
        _file = file;
        printWriter = PrintWriters.getPrintWriter(_file);
        writeHeader();
    }

    @Override
    public void writeRow(RequestInfo requestInfo, Map<String, String> resultMetadata)
    {
        printWriter.println(formatRow(requestInfo, resultMetadata));
    }

    @Override
    public void close() throws IOException
    {
        printWriter.close();
    }

    private void writeHeader()
    {
        printWriter.println(String.join("\t", FIELDS));
    }

    private String formatRow(RequestInfo ri, Map<String, String> resultMetadata)
    {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < FIELDS.size(); i++)
        {
            if (i > 0)
                row.append("\t");

            String field = FIELDS.get(i);
            if (resultMetadata.containsKey(field))
            {
                row.append(resultMetadata.get(field));
            }
            else
            {
                row.append(REQUEST_INFO_MAPPER.get(field).apply(ri));
            }
        }
        return row.toString();
    }
}
