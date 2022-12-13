/*
 * Copyright (c) 2016-2018 LabKey Corporation
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

import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.test.TestProperties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ExperimentalFeaturesHelper
{
    public static Boolean enableExperimentalFeature(Connection cn, String feature)
    {
        return setExperimentalFeature(cn, feature, true);
    }

    public static Boolean disableExperimentalFeature(Connection cn, String feature)
    {
        return setExperimentalFeature(cn, feature, false);
    }

    public static Boolean setExperimentalFeature(Connection cn, String feature, boolean enable)
    {
        if (TestProperties.isPrimaryUserAppAdmin())
            return null; // App admin can't enable/disable experimental features

        TestLogger.log((enable ? "Enabling" : "Disabling") + " experimental feature " + feature);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("feature", feature);
        parameters.put("enabled", enable);

        PostCommand<CommandResponse> command = new PostCommand<>("admin", "experimentalFeature");
        command.setParameters(parameters);
        try
        {
            CommandResponse r = command.execute(cn, null);
            Map<String, Object> response = r.getParsedData();

            return (Boolean)response.get("previouslyEnabled");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error setting experimental feature '" + feature + "'.", e);
        }
        catch (CommandException e)
        {
            throw new RuntimeException("Error setting experimental feature '" + feature + "': " + e.getStatusCode(), e);
        }
    }

    public static boolean isExperimentalFeatureEnabled(Connection cn, String feature)
    {
        Command<CommandResponse> command = new Command<>("admin", "experimentalFeature");
        command.setParameters(new HashMap<>(Map.of("feature", feature)));
        try
        {
            CommandResponse r = command.execute(cn, null);
            Map<String, Object> response = r.getParsedData();

            return (Boolean)response.get("enabled");
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error retrieving experimental feature '" + feature + "'.", e);
        }
    }
}
