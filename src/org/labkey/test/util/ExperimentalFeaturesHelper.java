/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.test.BaseWebDriverTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ExperimentalFeaturesHelper
{
    public static void enableExperimentalFeature(Connection cn, String feature)
    {
        setExperimentalFeature(cn, feature, true);
    }

    public static void disableExperimentalFeature(Connection cn, String feature)
    {
        setExperimentalFeature(cn, feature, false);
    }

    public static void setExperimentalFeature(Connection cn, String feature, boolean enable)
    {
        TestLogger.log((enable ? "Enabling" : "Disabling") + " experimental feature " + feature);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("feature", feature);
        parameters.put("enabled", enable);

        PostCommand command = new PostCommand("admin", "experimentalFeature");
        command.setParameters(parameters);
        try
        {
            CommandResponse rt = command.execute(cn, null);
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
}
