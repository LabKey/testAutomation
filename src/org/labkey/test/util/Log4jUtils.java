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
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
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
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }
}
