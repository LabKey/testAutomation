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
