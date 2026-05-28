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

import org.labkey.remoteapi.miniprofiler.RequestInfo;
import org.labkey.serverapi.writer.PrintWriters;
import org.labkey.test.util.data.TestDataUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
    public static final String REQUEST_ID = "requestId";
    public static final String REQUEST_URL = "requestUrl";
    public static final String START_TIME = "startTime";
    public static final String DURATION = "duration";
    public static final String QUERY_COUNT = "queryCount";

    // scenarioUuid (pk) | scenarioName | serverUri (pk) | simulationId | requestId (pk) | requestUrl | startTime | duration | queryCount
    private static final List<String> FIELDS = List.of(
            SCENARIO_UUID,
            SCENARIO_NAME,
            SERVER_URI,
            SIMULATION_ID,
            REQUEST_ID,
            REQUEST_URL,
            START_TIME,
            DURATION,
            QUERY_COUNT);

    private static final Map<String, Function<RequestInfo, Object>> REQUEST_INFO_MAPPER = Map.of(
            SIMULATION_ID, RequestInfo::getSessionId,
            REQUEST_ID, RequestInfo::getId,
            REQUEST_URL, RequestInfo::getUrl,
            START_TIME, RequestInfo::getDate,
            DURATION, RequestInfo::getDuration,
            QUERY_COUNT, ri -> ri.getRoot() != null ? ri.getRoot().getCustomTimings().getOrDefault("sql", Collections.emptyList()).size() : null
    );

    private final PrintWriter printWriter;
    private final TestDataUtils.TsvQuoter _tsvQuoter = new TestDataUtils.TsvQuoter();

    public RequestInfoTsvWriter(File file) throws FileNotFoundException
    {
        printWriter = PrintWriters.getPrintWriter(new FileOutputStream(file));
        writeHeader();
    }

    @Override
    public void writeRow(RequestInfo requestInfo, Map<String, ?> resultMetadata)
    {
        printWriter.println(formatRow(requestInfo, resultMetadata));
    }

    @Override
    public void close()
    {
        printWriter.close();
    }

    private void writeHeader()
    {
        printWriter.println(String.join("\t", FIELDS));
    }

    private String formatRow(RequestInfo ri, Map<String, ?> resultMetadata)
    {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < FIELDS.size(); i++)
        {
            if (i > 0)
                row.append("\t");

            String field = FIELDS.get(i);
            Object value = null;
            if (resultMetadata.containsKey(field))
            {
                value = resultMetadata.get(field);
            }
            else if (REQUEST_INFO_MAPPER.containsKey(field))
            {
                value = REQUEST_INFO_MAPPER.get(field).apply(ri);
            }

            row.append(_tsvQuoter.quoteValue(value));
        }
        return row.toString();
    }
}
