/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.test.pages.reports;

import org.labkey.test.Locator;
import org.labkey.test.components.ChartQueryDialog;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebDriver;

import static org.labkey.test.components.ext4.Window.Window;

public class ManageViewsPage extends LabKeyPage
{
    public ManageViewsPage(WebDriver driver)
    {
        super(driver);
    }

    public void clickAddReport(String reportType)
    {
        BootstrapMenu.find(getDriver(),"Add Report").clickSubMenu(true,reportType);
    }

    public ChartQueryDialog clickAddChart()
    {
        clickAndWait(Locator.linkContainingText("Add Chart"));
        return new ChartQueryDialog(getDriver());
    }

    @LogMethod
    public void deleteReport(String reportName)
    {
        // td[3] is the chart type column, shouldn't be a link or anything
        final Locator report = Locator.tag("tr").withClass("x4-grid-row").containing(reportName).childTag("td").position(3);

        // select the report and click the delete button
        waitForElement(report, 10000);
        click(report);

        click(Locator.linkWithText("Delete Selected"));

        Window(getDriver()).withTitle("Delete")
                .waitFor()
                .clickButton("OK", true);

        // make sure the report is deleted
        waitFor(() -> !isElementPresent(report),
                "Failed to delete report: " + reportName, WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod
    public void editReport(String reportName)
    {
        //td[1] is the pencil
        final Locator report = Locator.tag("tr").withClass("x4-grid-row").containing(reportName).childTag("td").position(1);
        click(report);
    }

    @LogMethod
    public void selectReport(String reportName)
    {
        final Locator report = Locator.tag("tr").withClass("x4-grid-row").containing(reportName).childTag("td").position(3);
        waitForElement(report, 10000);
        click(report);
    }
}
