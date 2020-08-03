/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
import org.labkey.serverapi.reader.Readers;
import org.labkey.test.util.TestLogger;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.openqa.selenium.chrome.ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY;
import static org.openqa.selenium.firefox.GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY;

public abstract class TestProperties
{
    static
    {
        try (Reader propReader = Readers.getReader(new File(TestFileUtils.getTestRoot(), "test.properties")))
        {
            TestLogger.log("Loading properties from test.properties");
            Properties properties = new Properties();
            properties.load(propReader);
            properties.putAll(System.getProperties());
            System.setProperties(properties);
        }
        catch (IOException ioe)
        {
            TestLogger.log("Failed to load test.properties file. Running with hard-coded defaults");
            ioe.printStackTrace(System.out);
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

    public static boolean isServerRemote()
    {
        return "true".equals(System.getProperty("labkey.isRemote", "false"));
    }

    public static boolean isIgnoreMissingModules()
    {
        return "true".equals(System.getProperty("webtest.ignoreMissingModules", "false"));
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

    public static boolean isSystemMaintenanceDisabled()
    {
        return "never".equals(System.getProperty("systemMaintenance"));
    }

    public static boolean isHeapDumpCollectionEnabled()
    {
        return "true".equals(System.getProperty("enable.heap.dump"));
    }

    public static boolean isRunWebDriverHeadless()
    {
        return "true".equals(System.getProperty("webtest.webdriver.headless"));
    }

    public static double getTimeoutMultiplier()
    {
        try
        {
            return Math.max(0, Double.parseDouble(System.getProperty("webtest.timeout.multiplier", "")));
        }
        catch (NumberFormatException badProp)
        {
            return 1.0;
        }
    }

    public static Duration getCrawlerTimeout()
    {
        try
        {
            return Duration.ofSeconds(Integer.parseInt(System.getProperty("crawlerTimeout")));
        }
        catch (NumberFormatException ignore)
        {
            return Duration.ofSeconds(90);
        }
    }

    public static boolean isCloudPipelineEnabled()
    {
        return "true".equals(System.getProperty("use.cloud.pipeline"));
    }

    public static String getCloudPipelineBucketName()
    {
        return System.getProperty("cloud.pipeline.bucket");
    }

    public static boolean isWebDriverLoggingEnabled()
    {
        return "true".equals(System.getProperty("webtest.webdriver.logging"));
    }

    public static boolean isDebugLoggingEnabled()
    {
        return "true".equals(System.getProperty("webtest.logging.debug"));
    }

    public static boolean isPrimaryUserAppAdmin()
    {
        return "true".equals(System.getProperty("webtest.primary.app.admin"));
    }

    public static boolean isWithoutTestModules()
    {
        return "true".equals(System.getProperty("webtest.without.test.modules"));
    }

    public static boolean isServerSsoOnly()
    {
        return "true".equals(System.getProperty("webtest.server.sso.only"));
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

            File testBin = new File(TestFileUtils.getTestRoot(), "bin");
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

            File testBin = new File(TestFileUtils.getTestRoot(), "bin");
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

    public static Map<String, Boolean> getExperimentalFeatures()
    {
        Map<String, Boolean> features = new HashMap<>();

        Properties props = System.getProperties();
        for (Map.Entry<Object, Object> entry : props.entrySet())
        {
            String key = String.valueOf(entry.getKey());
            Boolean value = (entry.getValue() instanceof Boolean)
                    ? (Boolean)entry.getValue()
                    : Boolean.valueOf(String.valueOf(entry.getValue()));

            String prefix = "webtest.experimental.";
            if (key.startsWith(prefix))
            {
                String feature = key.substring(prefix.length());
                features.put(feature, value);
            }
        }

        return features;
    }

    public static List<String> getDebugLoggingPackages()
    {
        String prop = System.getProperty("webtest.debug.server.packages", "");
        String[] packages = prop.split("\\s*,\\s*");
        return Arrays.stream(packages).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    private static File dumpDir = null;
    public static File getDumpDir()
    {
        if (dumpDir == null)
        {
            String outputDir = System.getProperty("failure.output.dir");
            if (outputDir != null)
            {
                dumpDir = new File(outputDir);
                if (!dumpDir.exists())
                    dumpDir = new File(TestFileUtils.getLabKeyRoot(), outputDir);
            }
            if (dumpDir == null || !dumpDir.exists())
                dumpDir = new File(System.getProperty("java.io.tmpdir"));
            if (!dumpDir.exists())
            {
                dumpDir = null;
                throw new RuntimeException("Couldn't determine directory for placement of output files. " +
                        "Tried system properties failure.output.dir and java.io.tmpdir");
            }

            TestLogger.log("Using " + dumpDir + " to store test output");
        }
        return dumpDir;
    }
}
