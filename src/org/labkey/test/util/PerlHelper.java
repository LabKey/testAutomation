/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
import org.labkey.test.pages.ConfigureReportsAndScriptsPage;

import java.io.File;
import java.io.FileFilter;

import static org.junit.Assert.fail;

public class PerlHelper
{
    protected BaseWebDriverTest _test;

    public PerlHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    @LogMethod
    public void ensurePerlConfig()
    {
        _test.goToAdminConsole().clickViewsAndScripting();
        _test.log("Check if Perl already is configured");

        ConfigureReportsAndScriptsPage scripts = new ConfigureReportsAndScriptsPage(_test);

        if (scripts.isEnginePresentForLanguage("Perl"))
            return;

        _test.log("Try configuring Perl");
        String perlHome = System.getProperty("perl.home", System.getenv("PERL_HOME"));
        if (perlHome != null)
        {
            _test.log("PERL_HOME is set to: " + perlHome + " searching for the Perl application");
            File perlHomeDir = new File(perlHome);
            FileFilter perlFilenameFilter = new FileFilter()
            {
                public boolean accept(File file)
                {
                    return ("perl.exe".equalsIgnoreCase(file.getName()) || "perl".equalsIgnoreCase(file.getName())) && file.canExecute();
                }
            };
            File[] files = perlHomeDir.listFiles(perlFilenameFilter);

            if (files == null || files.length == 0)
            {
                files = new File(perlHome, "bin").listFiles(perlFilenameFilter);
            }

            if (files.length == 1)
            {
                ConfigureReportsAndScriptsPage.EngineConfig config = new ConfigureReportsAndScriptsPage.EngineConfig(files[0]);
                scripts.addEngine(ConfigureReportsAndScriptsPage.EngineType.PERL, config);
                return;
            }
            else if (files.length > 1)
            {
                _test.log("Found too many Perl executables:");
                for (File file : files)
                {
                    _test.log("\t" + file.getAbsolutePath());
                }
            }
        }

        _test.log("Environment info: " + System.getenv());

        if (null == perlHome)
        {
            _test.log("");   // Blank line helps make the following message more readable
            _test.log("PERL_HOME environment variable is not set.  Set PERL_HOME to your Perl bin directory to enable automatic configuration.");
        }
        fail("Perl is not configured on this system.");
    }
}
