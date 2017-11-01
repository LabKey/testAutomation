/*
 * Copyright (c) 2011-2017 LabKey Corporation
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

import org.junit.Test;
import org.labkey.test.BaseWebDriverTest;

import java.util.List;

import static org.labkey.test.WebTestHelper.logToServer;

public class JUnitHeader extends BaseWebDriverTest
{
    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Test
    public void beforeJUnit() throws Exception
    {
        log("** This should precede JUnitTest.");
        log("** It will enable the dumbster and clean up any errors caused by the previous test");

        RReportHelper reportHelper = new RReportHelper(this);
        reportHelper.ensureRConfig(); // reportTest.js (via RhinoService) executes an R script

        PipelineToolsHelper pipelineToolsHelper = new PipelineToolsHelper(this);
        pipelineToolsHelper.setToolsDirToTestDefault(); // Point to extra tools if present (currently only sequeneanalysis tools)

        try{
            _containerHelper.deleteFolder("Shared", "_junit");
        }catch(Throwable e){/*ignore*/}

        startSystemMaintenance();

        logToServer("=== Starting Server-side JUnit Tests ===");
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
