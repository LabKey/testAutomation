/*
 * Copyright (c) 2013-2015 LabKey Corporation
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

import java.io.File;

public abstract class TestProperties
{
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

    public static boolean isSystemMaintenanceDisabled()
    {
        return "never".equals(System.getProperty("systemMaintenance"));
    }

    public static String ensureChromedriverExeProperty()
    {
        final String key = "webdriver.chrome.driver";
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
            if (chromeExe != null)
            {
                File testBin = new File(TestFileUtils.getLabKeyRoot(), "server/test/bin");
                File chromePath = new File(testBin, chromeExe);
                System.setProperty(key, chromePath.getAbsolutePath());
            }
            else
                System.out.println("Unable to locate chromedriver executable - Using Firefox instead");
        }

        return System.getProperty(key);
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
