/*
 * Copyright (c) 2014-2017 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage;

import java.io.File;

import static org.junit.Assert.fail;

public class QCAssayScriptHelper
{
    private static final String engineLanguage = "java";
    private static final String engineName = "Java";

    BaseWebDriverTest _test;

    public QCAssayScriptHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    @LogMethod
    public void ensureEngineConfig()
    {
        _test.goToAdminConsole().clickViewsAndScripting();
        _test.log("setup a java engine");

        ConfigureReportsAndScriptsPage scripts = new ConfigureReportsAndScriptsPage(_test);

        if (!scripts.isEnginePresentForLanguage(engineLanguage))
        {
            String javaHome = System.getProperty("java.home");
            File javaExe = new File(javaHome + "/bin/java.exe");
            if (!javaExe.exists())
            {
                javaExe = new File(javaHome + "/bin/java");
                if (!javaExe.exists())
                    fail("unable to setup the java engine");
            }

            ConfigureReportsAndScriptsPage.EngineConfig config = new ConfigureReportsAndScriptsPage.EngineConfig(javaExe);
            config.setName(engineName);
            config.setLanguage(engineLanguage);
            config.setExtensions("jar");
            config.setCommand("-jar \"${scriptFile}\" \"${runInfo}\" \"" + PasswordUtil.getUsername() + "\" \"" + PasswordUtil.getPassword() + "\" \"" + WebTestHelper.getBaseURL() + "\"");

            // add -Xdebug and -Xrunjdwp parameters to the engine command in order to attach a debugger to you transform script
            //config.setCommand("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006 -jar ${scriptFile} \"${runInfo}\" \"" + PasswordUtil.getUsername() + "\" \"" + PasswordUtil.getPassword() + "\" \"" + WebTestHelper.getBaseURL() + "\"");

            scripts.addEngine(ConfigureReportsAndScriptsPage.EngineType.EXTERNAL, config);
        }
    }

    @LogMethod
    public void deleteEngine()
    {
        _test.goToAdminConsole().clickViewsAndScripting();

        ConfigureReportsAndScriptsPage scripts = new ConfigureReportsAndScriptsPage(_test);

        if (scripts.isEnginePresentForLanguage(engineLanguage))
        {
            scripts.deleteEngine(engineName);
        }
    }
}
