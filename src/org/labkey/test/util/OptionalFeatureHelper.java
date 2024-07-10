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

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimpleGetCommand;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.TestProperties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class OptionalFeatureHelper
{
    public static Boolean enableOptionalFeature(Connection cn, String feature)
    {
        return setOptionalFeature(cn, feature, true);
    }

    public static Boolean disableOptionalFeature(Connection cn, String feature)
    {
        return setOptionalFeature(cn, feature, false);
    }

    public static Boolean setOptionalFeature(Connection cn, String feature, boolean enable)
    {
        if (TestProperties.isPrimaryUserAppAdmin())
            return null; // App admin can't enable/disable optional features

        TestLogger.log((enable ? "Enabling" : "Disabling") + " optional feature " + feature);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("feature", feature);
        parameters.put("enabled", enable);

        SimplePostCommand command = new SimplePostCommand("admin", "optionalFeature");
        command.setParameters(parameters);
        try
        {
            CommandResponse r = command.execute(cn, null);
            Map<String, Object> response = r.getParsedData();

            return (Boolean)response.get("previouslyEnabled");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error setting optional feature '" + feature + "'.", e);
        }
        catch (CommandException e)
        {
            throw new RuntimeException("Error setting optional feature '" + feature + "': " + e.getStatusCode(), e);
        }
    }

    public static boolean isOptionalFeatureEnabled(Connection cn, String feature)
    {
        SimpleGetCommand command = new SimpleGetCommand("admin", "optionalFeature");
        command.setParameters(Map.of("feature", feature));
        try
        {
            CommandResponse r = command.execute(cn, null);
            Map<String, Object> response = r.getParsedData();

            return (Boolean)response.get("enabled");
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error retrieving optional feature '" + feature + "'.", e);
        }
    }
}
