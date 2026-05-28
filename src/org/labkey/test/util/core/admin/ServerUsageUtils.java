/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
package org.labkey.test.util.core.admin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimpleGetCommand;
import org.labkey.serverapi.util.UsageReportingLevel;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.data.JSONUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class ServerUsageUtils
{
    public static Map<String, Object> getUsageReportJson(Connection connection) throws IOException, CommandException
    {
        SimpleGetCommand command = new SimpleGetCommand("admin", "testMothershipReport");
        command.setParameters(getMothershipReportParams("CheckForUpdates", UsageReportingLevel.ON, false, null));
        CommandResponse response = command.execute(connection, "/");
        return response.getParsedData();
    }

    public static Map<String, Object> getUsageMetrics(Connection connection) throws IOException, CommandException
    {
        return JSONUtils.getProperty("jsonMetrics", getUsageReportJson(connection));
    }

    public static Map<String, Object> getModuleMetrics(Connection connection, String module) throws IOException, CommandException
    {
        Map<String, Object> modules = JSONUtils.getProperty("jsonMetrics.modules", getUsageReportJson(connection));
        if (modules.containsKey(module))
            return JSONUtils.getProperty(module, modules);
        else
            throw new NoSuchElementException("Server metrics for " + module + " module do not exist. Found: " + modules.keySet());
    }

    @NotNull
    public static String getTestMothershipReportUrl(String type, UsageReportingLevel level, boolean submit, @Nullable String forwardedFor)
    {
        Map<String, Object> params = getMothershipReportParams(type, level, submit, forwardedFor);
        return WebTestHelper.buildURL("admin", "testMothershipReport", params);
    }

    @NotNull
    private static Map<String, Object> getMothershipReportParams(String type, UsageReportingLevel level, boolean submit, @Nullable String forwardedFor)
    {
        Map<String, Object> params = new HashMap<>();
        params.put("type", type);
        params.put("level", level.toString());
        params.put("submit", submit);
        params.put("testMode", true);
        if (null != forwardedFor)
            params.put("forwardedFor", forwardedFor);
        return params;
    }

}
