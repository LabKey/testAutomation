/*
 * Copyright (c) 2011-2018 LabKey Corporation
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.labkey.test.BaseWebDriverTest;

import java.util.List;

import static org.labkey.test.WebTestHelper.logToServer;

@BaseWebDriverTest.ClassTimeout(minutes = 3)
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

    @BeforeClass
    public static void beforeClass()
    {
        TestLogger.log("Preparing server to execute server-side tests");
    }

    @Test
    public void configureR()
    {
        RReportHelper reportHelper = new RReportHelper(this);
        reportHelper.ensureRConfig(); // reportTest.js (via RhinoService) executes an R script
    }

    @Test
    public void configurePipeline()
    {
        PipelineToolsHelper pipelineToolsHelper = new PipelineToolsHelper(this);
        pipelineToolsHelper.setToolsDirToTestDefault(); // Point to extra tools if present (currently only sequeneanalysis tools)
    }

    @Test
    public void cleanTestContainer()
    {
        if (_containerHelper.doesContainerExist("Shared/_junit"))
            _containerHelper.deleteFolder("Shared", "_junit");
    }

    @Test
    public void startSystemMaintenance()
    {
        super.startSystemMaintenance();
    }

    @AfterClass
    public static void logStart()
    {
        logToServer("=== Starting Server-side JUnit Tests ===");
    }

    @Override
    protected void checkLinks()
    {
        // skip
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
