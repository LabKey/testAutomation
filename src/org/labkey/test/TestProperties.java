/*
 * Copyright (c) 2013 LabKey Corporation
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

/**
 * User: tchadick
 * Date: 9/26/13
 */
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
        return System.getProperty("teamcity.buildType.id") != null;
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

    public static String getDatabaseType()
    {
        return System.getProperty("databaseType");
    }

    public static String getDatabaseVersion()
    {
        return System.getProperty("databaseVersion");
    }

    public static boolean isGroupConcatSupported()
    {
        return  "pg".equals(getDatabaseType()) ||
                "mssql".equals(getDatabaseType()) && !"2005".equals(getDatabaseVersion());
    }
}
