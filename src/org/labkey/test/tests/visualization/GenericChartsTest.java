/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.tests.visualization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.components.ChartLayoutDialog;
import org.labkey.test.pages.TimeChartWizard;
import org.labkey.test.tests.ReportTest;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class GenericChartsTest extends ReportTest
{
    protected static final String MICE_A = "Mice A";
    protected static final String MICE_B = "Mice B";
    protected static final String MICE_C = "Mice C";
    protected static final String MOUSE_GROUP_CATEGORY = "Cat Mice Let";
    protected static final String CHART_TITLE = "Test Title";

    private List<String> _plots = new ArrayList<>();
    private List<String> _plotDescriptions = new ArrayList<>();

    @LogMethod
    final protected void doCreateSteps()
    {
        // import study and wait; no specimens needed
        importStudy();
        waitForPipelineJobsToComplete(1, "study import", false);

        if (shouldCreateParticipantGroups())
        {
            // Create category with 3 groups
            _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), MICE_A, "Mouse", MOUSE_GROUP_CATEGORY, true, true, "999320016,999320518,999320529,999320557");
            _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), MICE_B, "Mouse", MOUSE_GROUP_CATEGORY, false, true, "999320565,999320576,999320582,999320609");
            _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), MICE_C, "Mouse", MOUSE_GROUP_CATEGORY, false, true, "999320624,999320646,999320652,999320671");
        }
    }

    protected boolean shouldCreateParticipantGroups()
    {
        return false;
    }

    @LogMethod
    protected void doVerifySteps()
    {
        testPlots();

        log("Verify saved plots");
        navigateToFolder(getProjectName(), getFolderName());
        clickTab("Clinical and Assay Data");
        for (int i = 0; i < _plots.size(); i++)
        {
            log("Verify " + _plots.get(i));
            WebElement plotLink = waitForElement(Locator.linkWithText(_plots.get(i)));
            mouseOver(plotLink);
            WebElement tipWindow = waitForElement(Locator.tag("div").withClass("data-views-tip-content").notHidden());
            String tipText = tipWindow.getText();
            String expectedDescription = _plotDescriptions.get(i);
            if (expectedDescription != null)
            {
                assertTrue("Plot tooltip. Expected: " + expectedDescription + "\n Actual: " + tipText,
                        tipText.contains(expectedDescription));
            }
            else
            {
                assertFalse("Expected no description in plot tooltip. Actual: " + tipText,
                        tipText.contains("Description"));
            }
            mouseOver(Locator.css("a")); // Just to dismiss the dialog
            shortWait().until(ExpectedConditions.invisibilityOf(tipWindow));
        }
    }

    protected abstract void testPlots();

    @LogMethod
    protected void savePlot(@NotNull String name, @Nullable String description)
    {
        savePlot(name, description, false);
    }

    @LogMethod
    protected void savePlot(@NotNull String name, @Nullable String description, boolean saveAs)
    {
        TimeChartWizard reportWizard = new TimeChartWizard(this);

        if (saveAs)
            reportWizard.saveReportAs(name, description);
        else
            reportWizard.saveReport(name, description);

        waitForText(name);
        _plots.add(name);
        _plotDescriptions.add(description);
    }

    protected abstract ChartLayoutDialog clickChartLayoutButton();

    protected <LayoutDialog extends ChartLayoutDialog> LayoutDialog clickChartLayoutButton(Class<LayoutDialog> clazz)
    {
        return new TimeChartWizard(this).clickChartLayoutButton(clazz);
    }

    protected TimeChartWizard openSavedPlotInEditMode(String savedPlotName)
    {
        click(Locator.linkContainingText("Clinical and Assay Data"));
        clickReportGridLink(savedPlotName);
        TimeChartWizard chartWizard = new TimeChartWizard(this);
        chartWizard.waitForReportRender();

        // verify that we originally are in view mode and can switch to edit mode
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Save"));
        chartWizard.clickEdit();
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Edit"));
        return chartWizard;
    }

    protected Map<String, File> export(String type, String xAxis, String yAxis)
    {
        Map<String, File> exported = new HashMap<>();

        waitForElement(Locator.css("svg"));

        log("Export as PDF");
        File pdf = clickExportPDFIcon("chart-render-div", 0);
        exported.put("pdf", pdf);

        log("Export as PNG");
        File png = clickExportPNGIcon("chart-render-div", 0);
        exported.put("png", png);

        log("Export to script.");
        Assert.assertEquals("Unexpected number of export script icons", 1, getExportScriptIconCount("chart-render-div"));
        clickExportScriptIcon("chart-render-div", 0);
        String exportScript = _extHelper.getCodeMirrorValue("export-script-textarea");

        log("Validate that the script is as expected.");
        assertTrue("Script did not contain expected text: '" + type + "' ", exportScript.toLowerCase().contains(type.toLowerCase()));
        assertTrue("Script did not contain expected text: '" + xAxis + "' ", exportScript.toLowerCase().contains(xAxis.toLowerCase()));
        if (yAxis != null)
            assertTrue("Script did not contain expected text: '" + yAxis + "' ", exportScript.toLowerCase().contains(yAxis.toLowerCase()));
        waitAndClick(Ext4Helper.Locators.ext4Button("Close"));

        return exported;
    }
}
