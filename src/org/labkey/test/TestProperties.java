/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
package org.labkey.test;

import java.io.File;

public abstract class TestProperties
{
    public static boolean isTestCleanupSkipped()
    {
        return "false".equals(System.getProperty("clean"));
    }

    public static boolean isLinkCheckEnabled()
    {
        return "true".equals(System.getProperty("linkCheck")) || isInjectCheckEnabled();
    }

    public static boolean isInjectCheckEnabled()
    {
        return "true".equals(System.getProperty("injectCheck"));
    }

    public static boolean isScriptCheckEnabled()
    {
        return "true".equals(System.getProperty("scriptCheck"));
    }

    public static boolean isDevModeEnabled()
    {
        return "true".equals(System.getProperty("devMode"));
    }

    public static boolean isFirebugPanelsEnabled()
    {
        return "true".equals(System.getProperty("enableFirebugPanels"));
    }

    public static boolean isFirefoxExtensionsEnabled()
    {
        return "true".equals(System.getProperty("enableFirefoxExtensions"));
    }

    public static boolean isTestRunningOnTeamCity()
    {
        return !System.getProperty("teamcity.buildType.id").equals("${teamcity.buildType.id}");
    }

    public static boolean isLeakCheckSkipped()
    {
        return "false".equals(System.getProperty("memCheck"));
    }

    public static boolean isQueryCheckSkipped()
    {
        return "false".equals(System.getProperty("queryCheck"));
    }

    public static boolean isViewCheckSkipped()
    {
        return "false".equals(System.getProperty("viewCheck"));
    }

    public static boolean isSystemMaintenanceDisabled()
    {
        return "never".equals(System.getProperty("systemMaintenance"));
    }

    public static File getDumpDir()
    {
        File dumpDir = null;
        String outputDir = System.getProperty("failure.output.dir");
        if (outputDir != null)
            dumpDir = new File(outputDir);
        if (dumpDir == null || !dumpDir.exists())
            dumpDir = new File(System.getProperty("java.io.tmpdir"));
        if (!dumpDir.exists())
        {
            throw new RuntimeException("Couldn't determine directory for placement of output files. " +
                    "Tried system properties failure.output.dir and java.io.tmpdir");
        }
        return dumpDir;
    }
}
