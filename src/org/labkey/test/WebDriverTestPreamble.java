/*
 * Copyright (c) 2013-2014 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;

import static org.labkey.test.TestProperties.isSystemMaintenanceDisabled;
import static org.labkey.test.WebTestHelper.logToServer;

public class WebDriverTestPreamble extends BaseWebDriverTest
{
    public void preamble() throws Exception
    {
        log("\n\n=============== Starting " + Runner.getCurrentTestName() + Runner.getProgress() + " =================");

        _startTime = System.currentTimeMillis();

        logToServer("=== Starting " + Runner.getCurrentTestName() + Runner.getProgress() + " ===");
        signIn();
        enableEmailRecorder();
        resetErrors();

        if (isSystemMaintenanceDisabled())
        {
            // Disable scheduled system maintenance to prevent timeouts during nightly tests.
            disableMaintenance();
        }

        // Only do this as part of test startup if we haven't already checked. Since we do this as the last
        // step in the test, there's no reason to bother doing it again at the beginning of the next test
        if (!_checkedLeaksAndErrors && !"DRT".equals(System.getProperty("suite")))
        {
            checkLeaksAndErrors();
        }

        if (isPipelineToolsTest()) // Get DB back in a good state after failed pipeline tools test.
            fixPipelineToolsDirectory();
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }
}
