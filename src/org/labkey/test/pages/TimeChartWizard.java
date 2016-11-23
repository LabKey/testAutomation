/*
 * Copyright (c) 2015-2016 LabKey Corporation
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
package org.labkey.test.pages;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.components.LookAndFeelTimeChart;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class TimeChartWizard
{
    private BaseWebDriverTest _test;

    public TimeChartWizard(BaseWebDriverTest test)
    {
        _test = test;
    }

    public ChartTypeDialog clickChartTypeButton()
    {
        _test.clickButton("Chart Type", 0);
        return new ChartTypeDialog(_test.getDriver());
    }

    public LookAndFeelTimeChart clickChartLayoutButton()
    {
        _test.clickButton("Chart Layout", 0);
        return new LookAndFeelTimeChart(_test.getDriver());
    }

    public void waitForWarningMessage(String message)
    {
        waitForWarningMessage(message, false);
    }

    public void waitForWarningMessage(String message, boolean partialTextMatch)
    {
        if (partialTextMatch)
            _test.isTextPresent(message);
        else
            _test.waitForElement(Locator.tagWithText("div", message));
    }

    public void verifySvgChart(int expectedNumLines, @Nullable String[] legendItems)
    {
        _test.waitForElements(Locator.css("div.x4-container svg path.line"), expectedNumLines, WebDriverWrapper.WAIT_FOR_JAVASCRIPT * 6);

        if (legendItems != null)
        {
            for (String legendItem : legendItems)
            {
                _test.assertElementPresent(Locator.css("div.x4-container svg g.legend-item text").withText(legendItem));
            }
        }
    }

    public void goToNextParticipantsPage()
    {
        _test.click(Locator.tagWithAttribute("a", "data-qtip", "Next Page"));
    }

    public void checkFilterGridRow(String label)
    {
        List<WebElement> svgs = Locator.css("div.x4-container svg").findElements(_test.getDriver());
        _test.waitForElement(Ext4Helper.Locators.getGridRow(label));
        _test._ext4Helper.checkGridRowCheckbox(label);
        if (svgs.size() > 0)
        {
            _test.shortWait().until(ExpectedConditions.stalenessOf(svgs.get(0)));
        }
        _test._ext4Helper.waitForMaskToDisappear();
    }

    public void uncheckFilterGridRow(String label)
    {
        List<WebElement> svgs = Locator.css("svg").findElements(_test.getDriver());
        _test.waitForElement(Ext4Helper.Locators.getGridRow(label));
        _test._ext4Helper.uncheckGridRowCheckbox(label);
        if (svgs.size() > 0)
        {
            _test.shortWait().until(ExpectedConditions.stalenessOf(svgs.get(0)));
        }
        _test._ext4Helper.waitForMaskToDisappear();
    }

    public void clickSwitchToGroupButton(boolean clickYes)
    {
        _test.click(Locator.tagWithId("a", "switchToGroups" + (clickYes ? "Yes" : "No")));
        if (clickYes)
        {
            _test.waitForElement(Locator.linkWithText("Manage Groups"));
        }
    }

    public void reSaveReport()
    {
        saveReport(null, null, false);
    }

    public void saveReport(String name, String description, boolean expectReload)
    {
        openSaveMenu();

        if (name != null)
            _test.setFormElement(Locator.name("reportName"), name);
        if (description != null)
        _test.setFormElement(Locator.name("reportDescription"), description);

        _test.clickAndWait(_test.findButton("Save", 1), expectReload ? BaseWebDriverTest.WAIT_FOR_PAGE : 0);
        if (!expectReload)
        {
            _test.waitForElement(Locator.tagWithClass("div", "x4-window").containing("Report saved successfully."));
            _test._ext4Helper.waitForMaskToDisappear();
        }
        _test.waitFor(() -> _test.isTextPresent("Please select at least one") || // group/participant
                        _test.isTextPresent("No data found") ||
                        _test.isElementPresent(Locator.css("svg")),
                "Time chart failed to appear after saving", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    private void openSaveMenu()
    {
        _test.clickButtonByIndex("Save", 0, 0);
        _test.waitForText("Viewable By");
    }
}
