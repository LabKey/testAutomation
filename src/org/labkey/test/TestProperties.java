package org.labkey.test;

/**
 * User: tchadick
 * Date: 9/26/13
 */
public abstract class TestProperties
{
    public static boolean skipCleanup()
    {
        return "false".equals(System.getProperty("clean"));
    }

    public static boolean linkCheckEnabled()
    {
        return "true".equals(System.getProperty("linkCheck")) || injectCheckEnabled();
    }

    public static boolean injectCheckEnabled()
    {
        return "true".equals(System.getProperty("injectCheck"));
    }

    public static boolean scriptCheckEnabled()
    {
        return "true".equals(System.getProperty("scriptCheck"));
    }

    public static boolean devModeEnabled()
    {
        return "true".equals(System.getProperty("devMode"));
    }

    public static boolean firebugPanelsEnabled()
    {
        return "true".equals(System.getProperty("enableFirebugPanels"));
    }

    public static boolean firefoxExtensionsEnabled()
    {
        return "true".equals(System.getProperty("enableFirefoxExtensions"));
    }

    public static boolean onTeamCity()
    {
        return System.getProperty("teamcity.buildType.id") != null;
    }

    public static boolean skipLeakCheck()
    {
        return "false".equals(System.getProperty("memCheck"));
    }

    public static boolean skipQueryCheck()
    {
        return "false".equals(System.getProperty("queryCheck"));
    }

    public static boolean skipViewCheck()
    {
        return "false".equals(System.getProperty("viewCheck"));
    }

    public static boolean systemMaintenanceDisabled()
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

    public static boolean groupConcatSupported()
    {
        return  "pg".equals(getDatabaseType()) ||
                "mssql".equals(getDatabaseType()) && !"2005".equals(getDatabaseVersion());
    }
}
