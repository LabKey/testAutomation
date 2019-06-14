/*
 * Copyright (c) 2015-2019 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.ChartLayoutDialog;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.components.LookAndFeelTimeChart;
import org.labkey.test.components.SaveChartDialog;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

// TODO: Refactor class. Much of it is applicable to more than time charts
public class TimeChartWizard extends LabKeyPage
{
    public TimeChartWizard(WebDriverWrapper test)
    {
        super(test);
    }

    public TimeChartWizard waitForReportRender()
    {
        _ext4Helper.waitForMaskToDisappear();
        waitFor(() -> isElementPresent(Locator.css("svg")) ||
                        isAnyTextPresent("Please select at least one"/* group/participant */,
                                "No data found",
                                "Error rendering chart:",
                                "The source dataset, list, or query may have been deleted."),
                "Report view did not load", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        return this;
    }

    public TimeChartWizard doAndWaitForUpdate(Runnable runnable)
    {
        return doAndWaitForUpdate(runnable, longWait());
    }

    public TimeChartWizard doAndWaitForUpdate(Runnable runnable, WebDriverWait wait)
    {
        WebElement svg = Locator.tag("svg").findElementOrNull(getDriver());
        runnable.run();
        if (svg != null)
            wait.until(ExpectedConditions.stalenessOf(svg));
        waitForReportRender();
        svg = Locator.tag("svg").findElementOrNull(getDriver());
        if (svg != null)
            wait.until(LabKeyExpectedConditions.animationIsDone(svg));
        _ext4Helper.waitForMaskToDisappear();
        clearCache();
        return this;
    }

    public ChartTypeDialog clickChartTypeButton()
    {
        waitForElement(Ext4Helper.Locators.ext4Button("Chart Type").enabled()).click();
        return new ChartTypeDialog(getDriver());
    }

    public LookAndFeelTimeChart clickChartLayoutButton()
    {
        return clickChartLayoutButton(LookAndFeelTimeChart.class);
    }

    public <LayoutDialog extends ChartLayoutDialog> LayoutDialog clickChartLayoutButton(Class<LayoutDialog> clazz)
    {
        waitForElement(Ext4Helper.Locators.ext4Button("Chart Layout").enabled()).click();

        LayoutDialog dialog;
        try
        {
            Constructor<LayoutDialog> constructor = clazz.getConstructor(WebDriver.class);
            dialog = constructor.newInstance(getDriver());
        }
        catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e)
        {
            throw new RuntimeException("Unable to instantiate page class: " + clazz.getName(), e);
        }
        return dialog;
    }

    public void waitForWarningMessage(String message)
    {
        waitForWarningMessage(message, false);
    }

    public void waitForWarningMessage(String message, boolean partialTextMatch)
    {
        if (partialTextMatch)
            isTextPresent(message);
        else
            waitForElement(Locator.tagWithText("div", message));
    }

    public void verifySvgChart(int expectedNumLines, @Nullable String[] legendItems)
    {
        waitForElements(Locator.css("div.x4-container svg path.line"), expectedNumLines, WebDriverWrapper.WAIT_FOR_JAVASCRIPT * 6);

        if (legendItems != null)
        {
            for (String legendItem : legendItems)
            {
                assertElementPresent(Locator.css("div.x4-container svg g.legend-item text").withText(legendItem));
            }
        }
    }

    public void goToNextParticipantsPage()
    {
        click(Locator.tagWithAttribute("a", "data-qtip", "Next Page"));
    }

    public void checkFilterGridRow(String label)
    {
        List<WebElement> svgs = Locator.css("div.x4-container svg").findElements(getDriver());
        waitForElement(Ext4Helper.Locators.getGridRow(label));
        _ext4Helper.checkGridRowCheckbox(label);
        if (svgs.size() > 0)
        {
            shortWait().until(ExpectedConditions.stalenessOf(svgs.get(0)));
        }
        _ext4Helper.waitForMaskToDisappear();
    }

    public void uncheckFilterGridRow(String label)
    {
        List<WebElement> svgs = Locator.css("svg").findElements(getDriver());
        waitForElement(Ext4Helper.Locators.getGridRow(label));
        _ext4Helper.uncheckGridRowCheckbox(label);
        if (svgs.size() > 0)
        {
            shortWait().until(ExpectedConditions.stalenessOf(svgs.get(0)));
        }
        _ext4Helper.waitForMaskToDisappear();
    }

    public void clickSwitchToGroupButton(boolean clickYes)
    {
        click(Locator.tagWithId("a", "switchToGroups" + (clickYes ? "Yes" : "No")));
        if (clickYes)
        {
            waitForElement(Locator.linkWithText("Manage Groups"));
        }
    }

    public TimeChartWizard reSaveReport()
    {
        return saveReport(null, null);
    }

    public TimeChartWizard saveReport(String name)
    {
        return saveReport(name, null);
    }

    public TimeChartWizard saveReport(@Nullable String name, @Nullable String description)
    {
        SaveChartDialog saveChartDialog = clickSave();

        if (name != null)
            saveChartDialog.setReportName(name);
        if (description != null)
            saveChartDialog.setReportDescription(description);

        return saveChartDialog.clickSave();
    }

    public TimeChartWizard saveReportAs(@NotNull String name, @Nullable String description)
    {
        SaveChartDialog saveChartDialog = clickSaveAs();

        saveChartDialog.setReportName(name);
        if (description != null)
            saveChartDialog.setReportDescription(description);

        return saveChartDialog.clickSave();
    }

    public SaveChartDialog clickSave()
    {
        Ext4Helper.Locators.ext4Button("Save").findElement(getDriver()).click();
        return new SaveChartDialog(this);
    }

    public SaveChartDialog clickSaveAs()
    {
        Ext4Helper.Locators.ext4Button("Save As").findElement(getDriver()).click();
        return new SaveChartDialog(this);
    }

    public TimeChartWizard clickEdit()
    {
        WebElement editButton = Ext4Helper.Locators.ext4Button("Edit").findElement(getDriver());
        doAndWaitForUpdate(() -> clickAndWait(editButton));
        shortWait().until(ExpectedConditions.stalenessOf(editButton));
        return this;
    }

    public Window clickEditExpectingError()
    {
        click(Ext4Helper.Locators.ext4Button("Edit"));
        return new Window.WindowFinder(getDriver()).withTitle("Error").waitFor();
    }

    public DataRegionTable clickViewData()
    {
        WebElement viewDataButton = Ext4Helper.Locators.ext4Button("View Data").findElementOrNull(getDriver());
        click(viewDataButton);
        shortWait().until(wd -> !viewDataButton.getText().contains("Data"));
        return new DataRegionTable.DataRegionFinder(getDriver()).waitFor();
    }

    public TimeChartWizard clickViewChart()
    {
        WebElement viewChartButton = Locator.findAnyElement("View chart button", getDriver(),
                Ext4Helper.Locators.ext4Button("View Chart"), Ext4Helper.Locators.ext4Button("View Chart(s)"));
        click(viewChartButton);
        shortWait().until(wd -> !viewChartButton.getText().contains("Chart"));
        return waitForReportRender();
    }
}
