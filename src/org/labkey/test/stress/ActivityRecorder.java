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
import org.labkey.remoteapi.miniprofiler.RequestInfo;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.TestLogger;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

public class ActivityRecorder
{
    private final RecentRequestsCollector recentRequestsCollector;

    public ActivityRecorder(Connection connection) throws IOException, CommandException
    {
        recentRequestsCollector = new RecentRequestsCollector(connection);
    }

    @LogMethod
    public <R> R recordActivity(@LoggedParam String description, Supplier<R> activity) throws IOException, CommandException
    {
        R result = activity.get();

        List<RequestInfo> recentRequests = recentRequestsCollector.getRecentRequests();
        TestLogger.log("%s triggered %s requests".formatted(description, recentRequests.size()));

        return result;
    }

    public void recordActivity(String description, Runnable activity) throws IOException, CommandException
    {
        recordActivity(description, () -> {
            activity.run();
            return null;
        });
    }

    public List<RequestInfo> skipRecentRequests() throws IOException, CommandException
    {
        List<RequestInfo> recentRequests = recentRequestsCollector.getRecentRequests();
        TestLogger.log("Skipping %s requests".formatted(recentRequests.size()));

        return recentRequests;
    }
}
