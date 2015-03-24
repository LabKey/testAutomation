/*
 * Copyright (c) 2015 LabKey Corporation
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
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/**
 * Created by cnathe on 2/4/2015.
 */
public class TimeChartWizard
{
    private BaseWebDriverTest _test;

    public TimeChartWizard(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void createNewChart()
    {
        _test.goToManageViews();
        _test._extHelper.clickExtMenuButton(true, Locator.linkContainingText("Add Report"), "Time Chart");
        _test.waitForElement(Locator.tagWithText("div", "To get started, choose a Measure:"));
    }

    public void chooseInitialMeasure(String queryName, String measure)
    {
        _test.waitForElement(_test.getButtonLocator("Choose a Measure"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.clickButton("Choose a Measure", 0);
        _test._extHelper.waitForExtDialog("Add Measure...");
        _test._extHelper.waitForLoadingMaskToDisappear(5 * BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test._ext4Helper.clickGridRowText(measure, 0);
        _test.clickButton("Select", 0);
        _test.waitForElement(Locator.css("svg text").withText(queryName));
    }

    public void waitForWarningMessage(String message)
    {
        _test.waitForElement(Locator.tagWithText("td", message));
    }

    public void changeXAxisToVisitBased(String axisLabel, String newLabel)
    {
        _test.goToSvgAxisTab(axisLabel);
        _test._ext4Helper.selectRadioButton("Chart Type:", "Visit Based Chart");
        _test.assertElementPresent(Locator.xpath("//table[//label[text() = 'Draw x-axis as:'] and contains(@class, 'x4-item-disabled')]"));
        _test.assertElementPresent(Locator.xpath("//table[//label[text() = 'Calculate time interval(s) relative to:'] and contains(@class, 'x4-item-disabled')]"));
        _test.assertElementPresent(Locator.xpath("//table[//label[text() = 'Range:'] and contains(@class, 'x4-item-disabled')]"));
        _test.setFormElement(Locator.name("x-axis-label-textfield"), newLabel);
        _test.waitForElementToDisappear(Locator.css(".x4-btn-disabled.revertxAxisLabel"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        applyChanges();
        _test.waitForElementToDisappear(Locator.css("svg text").containing(axisLabel));
        _test.waitForElement(Locator.css("svg text").withText(newLabel));
    }

    public void verifySvgChart(int expectedNumLines, @Nullable String[] legendItems)
    {
        _test.waitForElements(Locator.css("div.x4-container svg path.line"), expectedNumLines);

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
        List<WebElement> svgs = Locator.css("svg").findElements(_test.getDriver());
        _test.waitForElement(Ext4Helper.Locators.getGridRow(label, 0));
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
        _test.waitForElement(Ext4Helper.Locators.getGridRow(label, 0));
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

    public void showGroupIndividualLines()
    {
        openGroupingPanel();
        _test._ext4Helper.checkCheckbox("Show Individual Lines");
        applyChanges();
    }

    public void saveReport(String name, String description, boolean expectReload)
    {
        openSaveMenu();
        _test.setFormElement(Locator.name("reportName"), name);
        _test.setFormElement(Locator.name("reportDescription"), description);

        _test.clickAndWait(_test.getButtonLocator("Save", 1), expectReload ? BaseWebDriverTest.WAIT_FOR_PAGE : 0);
        if (!expectReload)
        {
            _test._extHelper.waitForExtDialog("Success");
            _test._extHelper.waitForExt3MaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return _test.isTextPresent("Please select at least one") || // group/participant
                        _test.isTextPresent("No data found") ||
                        _test.isElementPresent(Locator.css("svg"));
            }
        }, "Time chart failed to appear after saving", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    private void openSaveMenu()
    {
        _test.clickButtonByIndex("Save", 0, 0);
        _test.waitForText("Viewable By");
    }

    private void applyChanges()
    {
        _test.waitAndClick(_test.getButtonLocator("OK"));
        _test._ext4Helper.waitForMaskToDisappear();
    }

    private void openGroupingPanel()
    {
        _test.clickButton("Grouping", 0);
        _test.waitForElement(_test.getButtonLocator("Cancel"));
    }


}
