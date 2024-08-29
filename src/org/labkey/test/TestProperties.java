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

import org.apache.commons.lang3.StringUtils;
import org.labkey.serverapi.reader.Readers;
import org.labkey.test.util.TestLogger;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class TestProperties
{

    static
    {
        if (System.getenv("FIREFOX128") != null)
        {
            System.setProperty("selenium.firefox.binary", System.getenv("FIREFOX128"));
        }

        final File propFile = new File(TestFileUtils.getTestRoot(), "test.properties");
        final File propFileTemplate = new File(TestFileUtils.getTestRoot(), "test.properties.template");
        if (!propFile.exists())
        {
            TestLogger.log(String.format("'%s' does not exist. Creating default from '%s'", propFile.getName(), propFileTemplate.getName()));
            try (Stream<String> propStream = Files.lines(propFileTemplate.toPath()))
            {
                final Iterator<String> iterator = propStream.filter(line -> !line.startsWith("#!!")).iterator();
                Files.write(propFile.toPath(), (Iterable<String>) () -> iterator, StandardOpenOption.CREATE_NEW);
            }
            catch (IOException e)
            {
                TestLogger.error(e.getMessage());
            }
        }
        try (Reader propReader = Readers.getReader(propFile))
        {
            TestLogger.log("Loading properties from " + propFile.getName());
            Properties properties = new Properties();
            properties.load(propReader);
            properties.putAll(System.getProperties());
            System.setProperties(properties);
        }
        catch (IOException ioe)
        {
            TestLogger.error("Failed to load " + propFile.getName() + " file. Running with hard-coded defaults");
            ioe.printStackTrace(System.err);
        }

    }

    public static void load()
    {
        /* Force static block to run */
    }

    public static boolean isTestCleanupSkipped()
    {
        return !getBooleanProperty("clean", false);
    }

    public static boolean isLinkCheckEnabled()
    {
        return getBooleanProperty("linkCheck", false) || isInjectionCheckEnabled();
    }

    public static boolean isInjectionCheckEnabled()
    {
        return getBooleanProperty("injectCheck", false);
    }

    public static boolean isScriptCheckEnabled()
    {
        return getBooleanProperty("scriptCheck", true);
    }

    public static boolean isDevModeEnabled()
    {
        return getBooleanProperty("devMode", true);
    }

    public static boolean isTestRunningOnTeamCity()
    {
        String buildTypeProperty = "teamcity.buildType.id";
        String undefinedBuildTypeProperty = "${" + buildTypeProperty + "}";
        return !undefinedBuildTypeProperty.equals(System.getProperty(buildTypeProperty, undefinedBuildTypeProperty));
    }

    public static boolean isServerRemote()
    {
        return getBooleanProperty("webtest.server.remote", false);
    }

    public static boolean isLeakCheckSkipped()
    {
        return !getBooleanProperty("memCheck", true);
    }

    public static boolean isQueryCheckSkipped()
    {
        return !getBooleanProperty("queryCheck", true);
    }

    public static boolean isCspCheckSkipped()
    {
        return !getBooleanProperty("webtest.cspCheck", false);
    }

    public static boolean isNewWebDriverForEachTest()
    {
        return !getBooleanProperty("selenium.reuseWebDriver", false);
    }

    public static boolean isViewCheckSkipped()
    {
        return !getBooleanProperty("viewCheck", true);
    }

    public static boolean isSystemMaintenanceDisabled()
    {
        return "never".equals(System.getProperty("systemMaintenance"));
    }

    public static boolean isHeapDumpCollectionEnabled()
    {
        return getBooleanProperty("webtest.enable.heap.dump", false);
    }

    public static boolean isDiagnosticsExportEnabled()
    {
        return getBooleanProperty("webtest.enable.export.diagnostics", false);
    }

    public static boolean isRunWebDriverHeadless()
    {
        return getBooleanProperty("webtest.webdriver.headless", false);
    }

    public static boolean isDumpBrowserConsole()
    {
        return getBooleanProperty("webtest.dump.browser.console", false);
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
        return getBooleanProperty("use.cloud.pipeline", false);
    }

    public static String getCloudPipelineBucketName()
    {
        return System.getProperty("cloud.pipeline.bucket");
    }

    public static boolean isWebDriverLoggingEnabled()
    {
        return getBooleanProperty("webtest.webdriver.logging", true);
    }

    public static boolean isTroubleshootingStacktracesEnabled()
    {
        return getBooleanProperty("webtest.troubleshooting.stacktraces", false);
    }

    public static boolean isDebugLoggingEnabled()
    {
        return getBooleanProperty("webtest.logging.debug", false);
    }

    public static boolean isPrimaryUserAppAdmin()
    {
        return getBooleanProperty("webtest.primary.app.admin", false);
    }

    public static boolean isWithoutTestModules()
    {
        return getBooleanProperty("webtest.without.test.modules", false);
    }

    public static boolean isTrialServer()
    {
        return getBooleanProperty("webtest.server.trial", false);
    }

    public static boolean isEmbeddedTomcat()
    {
        return getBooleanProperty("useEmbeddedTomcat", false) || new File(TestFileUtils.getDefaultDeployDir(), "embedded").isDirectory();
    }

    public static boolean isCheckerFatal()
    {
        return "true".equals(System.getProperty("webtest.checker.fatal"));
    }

    public static boolean isAssayProductFeatureAvailable()
    {
        return isProductFeatureAvailable("assay");
    }

    public static boolean isProductFeatureAvailable(String feature)
    {
        return "true".equals(System.getProperty("webtest.productFeature." + feature.toLowerCase(), "true"));
    }

    public static boolean ignoreDatabaseNotSupportedException()
    {
        return "true".equals(System.getProperty("webtest.ignoreDatabaseNotSupportedException"));
    }

    /**
     * Parses system property 'webtest.server.startup.timeout' to determine maximum allowed server startup time.
     * If property is not defined or is not an integer, it defaults to 60 seconds.
     * @return Maximum number of seconds to wait for server startup
     */
    public static int getServerStartupTimeout()
    {
        String property = System.getProperty("webtest.server.startup.timeout");
        try
        {
            return Integer.parseInt(property);
        }
        catch (NumberFormatException nfe)
        {
            return 120;
        }
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

    /**
     * Interpret system property as boolean. If property is blank or unset, return the specified default value.
     * Otherwise, parse property with {@link Boolean#parseBoolean(String)}
     * @param key System property name
     * @param def Default value
     * @return value of the specified property
     */
    private static boolean getBooleanProperty(String key, boolean def)
    {
        String prop = System.getProperty(key);
        if (!StringUtils.isBlank(prop))
        {
            return Boolean.parseBoolean(prop);
        }
        else
        {
            return def;
        }
    }
}
