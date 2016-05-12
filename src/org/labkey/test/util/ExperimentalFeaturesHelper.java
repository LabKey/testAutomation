package org.labkey.test.util;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class ExperimentalFeaturesHelper
{

    public static void setExperimentalFeature(Connection cn, String feature, boolean enable)
    {

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("feature", feature);
        parameters.put("enabled", enable);

        PostCommand command = new PostCommand("admin", "experimentalFeature");
        command.setParameters(parameters);
        try
        {
            org.labkey.remoteapi.CommandResponse rt = command.execute(cn, null);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Error setting experimental feature '" + feature + "'.", e);
        }

    }
}
