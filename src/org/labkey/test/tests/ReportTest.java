/*
 * Copyright (c) 2009-2013 LabKey Corporation
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
import org.labkey.test.util.LogMethod;

/**
 * User: klum
 * Date: Jul 31, 2009
 */
public abstract class ReportTest extends StudyBaseTestWD
{
    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @LogMethod
    protected void deleteReport(String reportName)
    {
        clickAndWait(Locator.linkWithText("Manage Views"));
        final Locator report = Locator.tagContainingText("div", reportName);

        // select the report and click the delete button
        waitForElement(report, 10000);
        click(report);

        String id = _extHelper.getExtElementId("btn_deleteView");
        click(Locator.id(id));

        _extHelper.waitForExtDialog("Delete Views", WAIT_FOR_JAVASCRIPT);

        String btnId = (String)executeScript("return Ext.MessageBox.getDialog().buttons[1].getId();");
        click(Locator.id(btnId));

        // make sure the report is deleted
        waitFor(new Checker()
                {
                    public boolean check()
                    {
                        return !isElementPresent(report);
                    }
                }, "Failed to delete report: " + reportName, WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod
    protected Locator getReportGridLink(String reportName, String linkText)
    {
        return getReportGridLink(reportName, linkText, true);
    }

    @LogMethod
    protected Locator getReportGridLink(String reportName, String linkText, boolean isAdmin)
    {
        if (isAdmin)
        {
            goToManageViews();
        }
        final Locator report = Locator.tagContainingText("div", reportName);

        waitForElement(report, 10000);

        // click the row to expand it
        Locator expander = Locator.xpath("//div[@id='viewsGrid']//td//div[.='" + reportName + "']");
        click(expander);

        final Locator link = Locator.xpath("//div[@id='viewsGrid']//td//div[.='" + reportName + "']//..//..//..//td//a[contains(text(),'" + linkText + "')]");

        // make sure the row has expanded
        waitFor(new Checker() {
            public boolean check()
            {
                return isElementPresent(link);
            }
        }, "Unable to click the link: " + linkText + " for report: " + reportName, WAIT_FOR_JAVASCRIPT);

        return link;
    }

    protected void clickReportGridLink(String reportName, String linkText, boolean isAdmin)
    {
        Locator link = getReportGridLink(reportName, linkText, isAdmin);
        clickAndWait(link);
    }

    protected void clickReportGridLink(String reportName, String linkText)
    {
        clickReportGridLink(reportName, linkText, true);
    }

    protected void goToMainTitleTab(String mainTitle)
    {
        waitAndClick(Locator.css("svg text").containing(mainTitle));
        waitForElement(Locator.button("Cancel"));
    }

    protected void goToAxisTab(String axisLabel)
    {
        // Workaround: (Selenium 2.33) Unable to click axis labels reliably for some reason. Use javascript
        fireEvent(Locator.css("svg text").containing(axisLabel).waitForElmement(getDriver(), WAIT_FOR_JAVASCRIPT), SeleniumEvent.click);
        waitForElement(Locator.ext4Button("Cancel")); // Axis label windows always have a cancel button. It should be the only one on the page
    }

    @Override
    protected String getProjectName()
    {
        return "ReportVerifyProject";  // don't want this test to stomp on StudyVerifyProject
    }

    @LogMethod
    protected void cleanPipelineItem(String item)
    {
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("Manage Files"));
        if (isTextPresent(item))
        {
            checkCheckbox(Locator.xpath("//td/a[contains(text(), '" + item + "')]/../../td/input"));
            clickButton("Delete");
            assertTextNotPresent(item);
        }
    }
}
