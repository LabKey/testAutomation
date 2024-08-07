package org.labkey.test.stress;

import org.labkey.remoteapi.miniprofiler.RequestInfo;

import java.io.File;
import java.util.Collections;

public class RequestInfoTsvWriter
{
    // scenarioUuid (pk) | scenarioName | serverUri (pk) | simulationUuid | requestId (pk) | requestUrl | startTime | duration | queryCount
    private static final String HEADER = "scenarioUuid\tscenarioName\tserverUri\tsimulationUuid\trequestId\trequestUrl\tstartTime\tduration\tqueryCount";
    private final File _file;

    public RequestInfoTsvWriter(File file)
    {
        _file = file;
    }

    public void writeHeader()
    {

    }

    public void writeRow(RequestInfo requestInfo)
    {

    }

    private String formatRow(RequestInfo ri)
    {
        return "scenarioUuid\t" +
                "scenarioName\t" +
                "serverUri\t" +
                "simulationUuid\t" +
                ri.getId() + "\t" +
                ri.getUrl() + "\t" +
                ri.getDate() + "\t" +
                ri.getDuration() + "\t" +
                ri.getRoot().getCustomTimings().getOrDefault("sql", Collections.emptyList()).size();
    }
}
