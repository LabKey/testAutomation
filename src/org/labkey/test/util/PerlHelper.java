package org.labkey.test.util;

/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

import java.io.File;
import java.io.FilenameFilter;

import static org.junit.Assert.*;

public class PerlHelper extends AbstractHelper
{
    public PerlHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    public boolean ensurePerlConfig()
    {

        _test.goToAdminConsole();
        _test.clickAndWait(Locator.linkWithText("views and scripting"));

        _test.log("Check if Perl already is configured");
        if (_test.isPerlEngineConfigured())
            return true;

        _test.log("Try configuring Perl");
        String perlHome = System.getenv("PERL_HOME");
        if (perlHome != null)
        {
            _test.log("PERL_HOME is set to: " + perlHome + " searching for the Perl application");
            File perlHomeDir = new File(perlHome);
            FilenameFilter perlFilenameFilter = new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    return "perl.exe".equalsIgnoreCase(name) || "perl".equalsIgnoreCase(name);
                }
            };
            File[] files = perlHomeDir.listFiles(perlFilenameFilter);

            if (files == null || files.length == 0)
            {
                files = new File(perlHome, "bin").listFiles(perlFilenameFilter);
            }

            if (files != null)
            {
                for (File file : files)
                {
                    // add a new Perl engine configuration
                    String id = _test._extHelper.getExtElementId("btn_addEngine");
                    _test.click(Locator.id(id));

                    id = _test._extHelper.getExtElementId("add_perlEngine");
                    _test.click(Locator.id(id));

                    id = _test._extHelper.getExtElementId("btn_submit");
                    _test.waitForElement(Locator.id(id), 10000);

                    id = _test._extHelper.getExtElementId("editEngine_exePath");
                    _test.setFormElement(Locator.id(id), file.getAbsolutePath());

                    id = _test._extHelper.getExtElementId("btn_submit");
                    _test.click(Locator.id(id));

                    // wait until the dialog has been dismissed
                    int cnt = 3;
                    while (_test.isElementPresent(Locator.id(id)) && cnt > 0)
                    {
                        _test.sleep(1000);
                        cnt--;
                    }

                    if (_test.isPerlEngineConfigured())
                        return true;

                    _test.refresh();
                }
            }
        }

        _test.log("Environment info: " + System.getenv());

        if (null == perlHome)
        {
            _test.log("");   // Blank line helps make the following message more readable
            _test.log("PERL_HOME environment variable is not set.  Set PERL_HOME to your R bin directory to enable automatic configuration.");
        }
        fail("Perl is not configured on this system.");
        return false;
    }
}
