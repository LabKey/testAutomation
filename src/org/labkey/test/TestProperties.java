/*
 * Copyright (c) 2013-2017 LabKey Corporation
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

import org.apache.commons.lang3.SystemUtils;
import org.labkey.api.reader.Readers;
import org.labkey.test.util.TestLogger;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import static org.openqa.selenium.chrome.ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY;
import static org.openqa.selenium.firefox.GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY;

public abstract class TestProperties
{
    static
    {
        try (Reader propReader = Readers.getReader(new File(TestFileUtils.getLabKeyRoot(), "server/test/test.properties")))
        {
            TestLogger.log("Loading properties from test.properties");
            Properties properties = new Properties();
            properties.load(propReader);
            properties.putAll(System.getProperties());
            System.setProperties(properties);
        }
        catch (IOException ignore)
        {
            TestLogger.log("Failed to load test.properties file. Running with hard-coded defaults");
            ignore.printStackTrace(System.out);
        }
    }

    public static void load()
    {
        /* Force static block to run */
    }

    public static boolean isTestCleanupSkipped()
    {
        return "false".equals(System.getProperty("clean", "false"));
    }

    public static boolean isLinkCheckEnabled()
    {
        return "true".equals(System.getProperty("linkCheck", "false")) || isInjectionCheckEnabled();
    }

    public static boolean isInjectionCheckEnabled()
    {
        return "true".equals(System.getProperty("injectCheck", "false"));
    }

    public static boolean isScriptCheckEnabled()
    {
        return "true".equals(System.getProperty("scriptCheck", "true"));
    }

    public static boolean isDevModeEnabled()
    {
        return "true".equals(System.getProperty("devMode", "true"));
    }

    public static boolean isTestRunningOnTeamCity()
    {
        String buildTypeProperty = "teamcity.buildType.id";
        String undefinedBuildTypeProperty = "${" + buildTypeProperty + "}";
        return !undefinedBuildTypeProperty.equals(System.getProperty(buildTypeProperty, undefinedBuildTypeProperty));
    }

    public static boolean isLeakCheckSkipped()
    {
        return "false".equals(System.getProperty("memCheck", "true"));
    }

    public static boolean isQueryCheckSkipped()
    {
        return "false".equals(System.getProperty("queryCheck", "true"));
    }

    public static boolean isNewWebDriverForEachTest()
    {
        return !"true".equals(System.getProperty("selenium.reuseWebDriver", "false"));
    }

    public static boolean isViewCheckSkipped()
    {
        return "false".equals(System.getProperty("viewCheck", "true"));
    }

    public static boolean isConfigureSecurity()
    {
        return "true".equals(System.getProperty("configureSecurity", "false"));
    }

    public static boolean isSystemMaintenanceDisabled()
    {
        return "never".equals(System.getProperty("systemMaintenance"));
    }

    public static boolean isHeapDumpCollectionEnabled()
    {
        return "true".equals(System.getProperty("enable.heap.dump"));
    }

    public static String ensureGeckodriverExeProperty()
    {
        final String key = GECKO_DRIVER_EXE_PROPERTY;
        String currentProperty = System.getProperty(key);
        if (currentProperty == null)
        {
            String executable = null;
            if(SystemUtils.IS_OS_MAC)
            {
                executable = "mac/geckodriver";
            }
            else if (SystemUtils.IS_OS_WINDOWS)
            {
                executable = "windows/geckodriver.exe";
            }
            else if (SystemUtils.IS_OS_LINUX)
            {
                switch (SystemUtils.OS_ARCH)
                {
                    case "amd64":
                        executable = "linux/amd64/geckodriver";
                        break;
                    case "i386":
                        executable = "linux/i386/geckodriver";
                        break;
                }
            }

            File testBin = new File(TestFileUtils.getLabKeyRoot(), "server/test/bin");
            File driverPath = new File(testBin, executable);
            System.setProperty(key, driverPath.getAbsolutePath());
        }

        return System.getProperty(key);
    }

    public static String ensureChromedriverExeProperty()
    {
        final String key = CHROME_DRIVER_EXE_PROPERTY;
        String currentProperty = System.getProperty(key);
        if (currentProperty == null)
        {
            String chromeExe = null;
            if(SystemUtils.IS_OS_MAC)
            {
                chromeExe = "mac/chromedriver";
            }
            else if (SystemUtils.IS_OS_WINDOWS)
            {
                chromeExe = "windows/chromedriver.exe";
            }
            else if (SystemUtils.IS_OS_LINUX)
            {
                switch (SystemUtils.OS_ARCH)
                {
                    case "amd64":
                        chromeExe = "linux/amd64/chromedriver";
                        break;
                    case "i386":
                        chromeExe = "linux/i386/chromedriver";
                        break;
                }
            }

            File testBin = new File(TestFileUtils.getLabKeyRoot(), "server/test/bin");
            File chromePath = new File(testBin, chromeExe);
            System.setProperty(key, chromePath.getAbsolutePath());
        }

        return System.getProperty(key);
    }

    public static File getTomcatHome()
    {
        String tomcatHome = System.getProperty("tomcat.home", System.getenv("CATALINA_HOME"));
        if (tomcatHome != null && !tomcatHome.isEmpty())
            return new File(tomcatHome);
        else
            return null;
    }

    public static String getAdditionalPipelineTools()
    {
        return System.getProperty("additional.pipeline.tools");
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
