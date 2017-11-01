/*
 * Copyright (c) 2009-2017 LabKey Corporation
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

package org.labkey.test.tests;

import org.labkey.test.Locator;
import org.labkey.test.components.ChartQueryDialog;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.util.LogMethod;

public abstract class ReportTest extends StudyBaseTest
{
    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @LogMethod
    protected void deleteReport(String reportName)
    {
        goToManageViews().deleteReport(reportName);
    }

    @LogMethod
    protected Locator getReportGridLink(String reportName)
    {
        return getReportGridLink(reportName, true);
    }

    @LogMethod
    protected Locator getReportGridLink(String reportName, boolean isAdmin)
    {
        if (isAdmin)
        {
            goToManageViews();
        }
        waitForElement(Locator.linkWithText(reportName), WAIT_FOR_JAVASCRIPT);
        return Locator.linkWithText(reportName);
    }

    protected void clickReportGridLink(String reportName, boolean isAdmin)
    {
        Locator link = getReportGridLink(reportName, isAdmin);
        clickAndWait(link, WAIT_FOR_JAVASCRIPT);
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
    }

    protected void clickReportGridLink(String reportName)
    {
        clickReportGridLink(reportName, true);
    }

    protected void clickReportDetailsLink(String reportName)
    {
        Locator link = Locator.xpath("//tr").withClass("x4-grid-row").containing(reportName).append("//a[contains(@data-qtip, 'Click to navigate to the Detail View')]");

        waitForElement(link);
        scrollIntoView(link);
        clickAndWait(link);
    }

    protected void clickReportPermissionsLink(String reportName)
    {
        Locator link = Locator.xpath("//tr").withClass("x4-grid-row").withPredicate(Locator.linkWithText(reportName)).append("//a[contains(@data-qtip, 'Click to customize the permissions')]");

        waitAndClickAndWait(link);
    }

    @Override
    protected String getProjectName()
    {
        return "ReportVerifyProject";  // don't want this test to stomp on StudyVerifyProject
    }

    @LogMethod
    protected void cleanPipelineItem(String item)
    {
        navigateToFolder(getProjectName(), getFolderName());
        deletePipelineJob(item, false);
    }

    protected ChartTypeDialog clickAddChart(String schemaName, String queryName)
    {
        goToManageViews();
        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Add Chart"));
        ChartQueryDialog queryDialog = new ChartQueryDialog(getDriver());
        queryDialog.selectSchema(schemaName).selectQuery(queryName);
        return queryDialog.clickOk();
    }
}
