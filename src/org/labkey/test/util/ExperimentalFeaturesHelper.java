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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.WebDriverWrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ExperimentalFeaturesHelper
{
    private static Map<String, Boolean> _originalFeatureFlags = new HashMap<>();

    public static void enableExperimentalFeature(Connection cn, String feature)
    {
        setExperimentalFeature(cn, feature, true, true);
    }

    public static void disableExperimentalFeature(Connection cn, String feature)
    {
        setExperimentalFeature(cn, feature, false, true);
    }

    public static void setExperimentalFeature(Connection cn, String feature, boolean enable)
    {
        setExperimentalFeature(cn, feature, enable, true);
    }

    public static void setFeatures(LabKeySiteWrapper test, Map<String, Boolean> flags)
    {
        if (flags == null || flags.isEmpty())
            return;

        TestLogger.log("Setting experimental flags for duration of the test:");

        Connection cn = test.createDefaultConnection(false);
        for (Map.Entry<String, Boolean> flag : flags.entrySet())
        {
            setExperimentalFeature(cn, flag.getKey(), flag.getValue(), true);
        }
    }

    public static void resetFeatures(LabKeySiteWrapper test)
    {
        if (_originalFeatureFlags.isEmpty())
            return;

        TestLogger.log("Resetting experimental flags to their original value:");

        Map<String, Boolean> flags = new HashMap<>(_originalFeatureFlags);
        _originalFeatureFlags = new HashMap<>();

        Connection cn = test.createDefaultConnection(false);
        for (Map.Entry<String, Boolean> features : flags.entrySet())
        {
            setExperimentalFeature(cn, features.getKey(), features.getValue(), false);
        }
    }

    private static void setExperimentalFeature(Connection cn, String feature, boolean enable, boolean savePrevious)
    {
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
            if (savePrevious && response.containsKey("previouslyEnabled"))
            {
                // Remember the previous setting for the feature flag if was different
                Boolean previouslyEnabled = (Boolean)response.get("previouslyEnabled");
                if (!Objects.equals(enable, previouslyEnabled.booleanValue()))
                {
                    TestLogger.log("Experimental feature will be reset back to " + (previouslyEnabled ? "enabled" : "disabled") + " after test is completed");
                    _originalFeatureFlags.put(feature, previouslyEnabled.booleanValue());
                }
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
