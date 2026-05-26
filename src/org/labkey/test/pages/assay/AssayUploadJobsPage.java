/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
package org.labkey.test.pages.assay;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.pipeline.PipelineStatusDetailsPage;
import org.labkey.test.util.PipelineStatusTable;
import org.openqa.selenium.WebDriver;

import java.util.Map;

public class AssayUploadJobsPage extends LabKeyPage<AssayUploadJobsPage.ElementCache>
{
    public AssayUploadJobsPage(WebDriver driver)
    {
        super(driver);
    }

    public static AssayUploadJobsPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("assay", containerPath, "showUploadJobs"));
        return new AssayUploadJobsPage(webDriverWrapper.getDriver());
    }

    public static AssayUploadJobsPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath, int protocolId)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("assay", containerPath, "showUploadJobs", Map.of("rowId", protocolId)));
        return new AssayUploadJobsPage(webDriverWrapper.getDriver());
    }

    public PipelineStatusTable getDataTable()
    {
        return (PipelineStatusTable) PipelineStatusTable.finder(getDriver()).findWhenNeeded(getDriver());
    }

    public PipelineStatusDetailsPage clickJobStatus(String jobDescription)
    {
        return clickJobStatus(jobDescription, getDefaultWaitForPage());
    }

    public PipelineStatusDetailsPage clickJobStatus(String jobDescription, int waitTimeout)
    {
        int jobRow = getDataTable().getJobRow(jobDescription);
        return getDataTable().clickStatusLink(jobRow, waitTimeout);
    }

    public AssayDataPage clickViewResults()
    {
        clickAndWait(Locator.linkWithText("view results"));
        return new AssayDataPage(getDriver());
    }

    public AssayRunsPage clickViewRuns()
    {
        clickAndWait(Locator.linkWithText("view runs"));
        return new AssayRunsPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {

    }
}
