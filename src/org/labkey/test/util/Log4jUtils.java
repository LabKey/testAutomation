/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.TestProperties;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.core.admin.logger.ManagerPage;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class Log4jUtils
{
    // Only log the first failed attempt at setting log level. It isn't fatal and will tend add noise to logs.
    private static boolean loggedResetError = false;
    private static final Set<String> erroredPackages = new HashSet<>();

    @LogMethod(quiet = true)
    public static void setLogLevel(@LoggedParam String name, @LoggedParam ManagerPage.LoggingLevel level)
    {
        if (TestProperties.isPrimaryUserAppAdmin())
            return;

        Connection connection = WebTestHelper.getRemoteApiConnection();
        SimplePostCommand command = new SimplePostCommand("logger", "update");
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
            if (!erroredPackages.contains(name))
            {
                erroredPackages.add(name);
                TestLogger.warn("Failed to set log level for '" + name + "'. We will not log any subsequent failures.");
                e.printStackTrace();
            }
        }
    }

    @LogMethod(quiet = true)
    public static void resetAllLogLevels()
    {
        if (TestProperties.isPrimaryUserAppAdmin())
            return;

        Connection connection = WebTestHelper.getRemoteApiConnection();
        SimplePostCommand command = new SimplePostCommand("logger", "reset");
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
            if (!loggedResetError)
            {
                loggedResetError = true;
                TestLogger.warn("Failed to reset server logging levels. We will not log any subsequent failures.");
                e.printStackTrace();
            }
        }
    }

    @LogMethod(quiet = true)
    public static void resetLogMark() throws IOException, CommandException
    {
        Connection connection = WebTestHelper.getRemoteApiConnection();
        SimplePostCommand command = new SimplePostCommand("admin", "resetPrimaryLogMark");
        command.execute(connection, "/");
    }

    @LogMethod(quiet = true)
    public static void showLogSinceMark(WebDriverWrapper driver) throws IOException, CommandException
    {
        driver.beginAt(WebTestHelper.buildURL("admin", "showPrimaryLogSinceMark"));
    }
}
