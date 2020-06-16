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
import org.labkey.remoteapi.PostCommand;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.TestProperties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ExperimentalFeaturesHelper
{
    private static Map<String, Boolean> _originalFeatureFlags = new HashMap<>();

    public static void enableExperimentalFeature(Connection cn, String feature)
    {
        setFeature(cn, feature, true);
    }

    public static void disableExperimentalFeature(Connection cn, String feature)
    {
        setFeature(cn, feature, false);
    }

    public static void setExperimentalFeature(Connection cn, String feature, boolean enable)
    {
        setFeature(cn, feature, enable);
    }

    public static void setFeatures(LabKeySiteWrapper test, Map<String, Boolean> flags)
    {
        if (flags == null || flags.isEmpty())
            return;

        TestLogger.log("Setting experimental flags for duration of the test:");

        Connection cn = test.createDefaultConnection(false);
        for (Map.Entry<String, Boolean> flag : flags.entrySet())
        {
            setFeature(cn, flag.getKey(), flag.getValue());
        }
    }

    public static void resetFeatures(LabKeySiteWrapper test)
    {
        if (_originalFeatureFlags.isEmpty())
            return;

        TestLogger.log("Resetting experimental flags to their original value:");

        Connection cn = test.createDefaultConnection(false);
        for (Map.Entry<String, Boolean> features : _originalFeatureFlags.entrySet())
        {
            setExperimentalFeature(cn, features.getKey(), features.getValue());
        }

        _originalFeatureFlags = new HashMap<>();
    }

    private static void setFeature(Connection cn, String feature, boolean enable)
    {
        if (TestProperties.isPrimaryUserAppAdmin())
            return; // App admin can't enable/disable experimental features

        TestLogger.log((enable ? "Enabling" : "Disabling") + " experimental feature " + feature);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("feature", feature);
        parameters.put("enabled", enable);

        PostCommand command = new PostCommand("admin", "experimentalFeature");
        command.setParameters(parameters);
        try
        {
            CommandResponse r = command.execute(cn, null);
            Map<String, Object> response = r.getParsedData();

            // When setting a feature flag the first time, remember the previous setting
            if (!_originalFeatureFlags.containsKey(feature) && response.containsKey("previouslyEnabled"))
            {
                Boolean previouslyEnabled = (Boolean)response.get("previouslyEnabled");
                _originalFeatureFlags.put(feature, previouslyEnabled.booleanValue());
            }
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
