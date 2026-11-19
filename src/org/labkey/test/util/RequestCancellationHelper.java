/*
 * Copyright (c) 2026 LabKey Corporation
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
package org.labkey.test.util;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.ExecuteSqlCommand;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.util.core.admin.ServerUsageUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Helpers for tests that abandon slow requests and check that the server cancels them */
public class RequestCancellationHelper
{
    /** The usage report gathers every module's metrics, which can take over a minute on a large database */
    public static final int METRICS_TIMEOUT = 180_000;
    public static final String WS_MESSAGE = "wsMessage";
    public static final String WS_CLOSE = "wsClose";

    private final WebDriverWrapper _test;

    public RequestCancellationHelper(WebDriverWrapper test)
    {
        _test = test;
    }

    public void waitForWebSocket()
    {
        // The server ignores any message type but "cancel"
        WebDriverWrapper.waitFor(() -> (Boolean) _test.executeScript("return LABKEY.WebSocket.send({ type: 'ping' });"),
            "The notification WebSocket didn't open", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    /** Server-wide count of requests canceled from one source (WS_MESSAGE, WS_CLOSE), from Core's usage metrics */
    @SuppressWarnings("unchecked")
    public long getCancelCount(String metric)
    {
        try
        {
            Map<String, Object> core = ServerUsageUtils.getModuleMetrics(_test.createDefaultConnection().setTimeout(METRICS_TIMEOUT), "Core");
            Map<String, Object> counts = (Map<String, Object>) core.getOrDefault("simpleMetricCounts", Map.of());
            Map<String, Object> requestCancel = (Map<String, Object>) counts.getOrDefault("requestCancel", Map.of());
            return ((Number) requestCancel.getOrDefault(metric, 0)).longValue();
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    /** At least, since the counter is server-wide and other cancels can land meanwhile */
    public void waitForCancelCount(String metric, long minimum)
    {
        WebDriverWrapper.waitFor(() -> getCancelCount(metric) >= minimum, "Requests canceled (" + metric + ") never reached " + minimum, METRICS_TIMEOUT);
    }

    /** Names of the server's modules, sorted; the basis for slow cross joins whose size doesn't depend on the module count */
    public List<String> getModuleNames(String containerPath) throws IOException, CommandException
    {
        return new ExecuteSqlCommand("core", "SELECT Name FROM core.Modules ORDER BY Name")
            .execute(_test.createDefaultConnection(), containerPath)
            .getRows().stream().map(row -> (String) row.get("Name")).toList();
    }

    /** "core.Modules m0, core.Modules m1, ..." with {@code joins} tables */
    public static String crossJoinModules(int joins)
    {
        return IntStream.range(0, joins).mapToObj(i -> "core.Modules m" + i).collect(Collectors.joining(", "));
    }
}
