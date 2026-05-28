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
package org.labkey.test.stress;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.miniprofiler.RecentRequestsCommand;
import org.labkey.remoteapi.miniprofiler.RequestInfo;
import org.labkey.test.util.Crawler;

import java.io.IOException;
import java.util.List;

public class RecentRequestsCollector
{
    private final Connection _connection;
    private long lastRequestId = 0L;

    public RecentRequestsCollector(Connection connection) throws IOException, CommandException
    {
        _connection = connection;
        getRecentRequests(); // Prime 'lastRequestId'
    }

    public List<RequestInfo> getRecentRequests() throws IOException, CommandException
    {
        List<RequestInfo> requestInfos = new RecentRequestsCommand(lastRequestId).execute(_connection, null).getRequestInfos();
        lastRequestId = requestInfos.getLast().getId();
        return requestInfos.stream().filter(requestInfo -> HarConverter.EXCLUDED_ACTIONS.contains(new Crawler.ControllerActionId(requestInfo.getUrl()))).toList();
    }
}
