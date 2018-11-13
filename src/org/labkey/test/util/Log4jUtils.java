/*
 * Copyright (c) 2018 LabKey Corporation
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
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.core.admin.logger.ManagerPage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class Log4jUtils
{
    // Only log the first failed attempt at setting log level. It isn't fatal and will tend add noise to logs.
    private static boolean loggedError = false;

    @LogMethod(quiet = true)
    public static void setLogLevel(@LoggedParam String name, @LoggedParam ManagerPage.LoggingLevel level)
    {
        Connection connection = WebTestHelper.getRemoteApiConnection();
        PostCommand command = new PostCommand("logger", "update");
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("level", level.name());
        command.setParameters(params);
        try
        {
            command.execute(connection, "/");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        catch (CommandException e)
        {
            if (!loggedError)
            {
                loggedError = true;
                TestLogger.warn("Failed to set log level for '" + name + "'. We will not log any subsequent failures.");
                e.printStackTrace();
            }
        }
    }

    @LogMethod(quiet = true)
    public static void resetAllLogLevels()
    {
        Connection connection = WebTestHelper.getRemoteApiConnection();
        PostCommand command = new PostCommand("logger", "reset");
        try
        {
            command.execute(connection, "/");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        catch (CommandException e)
        {
            if (!loggedError)
            {
                loggedError = true;
                TestLogger.warn("Failed to reset server logging levels. We will not log any subsequent failures.");
                e.printStackTrace();
            }
        }
    }
}
