/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
import org.labkey.test.components.ChartLayoutDialog;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.components.SaveChartDialog;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

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

        // Create category with 3 groups
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), MICE_A, "Mouse", MOUSE_GROUP_CATEGORY, true, true, "999320016,999320518,999320529,999320557");
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), MICE_B, "Mouse", MOUSE_GROUP_CATEGORY, false, true, "999320565,999320576,999320582,999320609");
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), MICE_C, "Mouse", MOUSE_GROUP_CATEGORY, false, true, "999320624,999320646,999320652,999320671");
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
            Locator loc = Locator.linkWithText(_plots.get(i));
            waitForElement(loc);
            mouseOver(loc);
            Locator.XPathLocator tipWindow = Locator.tag("div").withClass("data-views-tip-content").notHidden().containing(_plotDescriptions.get(i));
            waitForElement(tipWindow);
            mouseOver(Locator.css("a")); // Just to dismiss the dialog
            waitForElementToDisappear(tipWindow);
        }
    }

    protected abstract void testPlots();

    @LogMethod
    protected void savePlot(String name, String description)
    {
        savePlot(name, description, false);
    }

    @LogMethod
    protected void savePlot(String name, String description, boolean saveAs)
    {
        WebElement saveButton = saveAs ? findButton("Save As") : findButton("Save");
        saveButton.click();

        SaveChartDialog saveChartDialog = new SaveChartDialog(this);
        saveChartDialog.waitForDialog(saveAs);
        saveChartDialog.setReportName(name);
        saveChartDialog.setReportDescription(description);
        saveChartDialog.clickSave(saveAs);
        sleep(2500); // sleep while the save success message shows
        waitForText(name);
        waitFor(() ->
                !(Boolean) executeScript("var p = Ext4.getCmp('generic-report-panel-1'); " +
                        "if (p) return p.isDirty(); " +
                        "else return false;"),
                "Page still dirty", WAIT_FOR_JAVASCRIPT);

        _plots.add(name);
        _plotDescriptions.add(description);
    }

    @LogMethod
    protected void savePlot()
    {
        clickButton("Save", 0);
        SaveChartDialog saveChartDialog = new SaveChartDialog(this);
        saveChartDialog.waitForDialog();
        saveChartDialog.clickSave();
        sleep(2500); // sleep while the save success message shows
    }

    protected ChartLayoutDialog clickChartLayoutButton()
    {
        waitForElement(Ext4Helper.Locators.ext4Button("Chart Layout").enabled()).click();
        return new ChartLayoutDialog(getDriver());
    }

    protected ChartTypeDialog clickChartTypeButton()
    {
        waitForElement(Ext4Helper.Locators.ext4Button("Chart Type").enabled()).click();
        return new ChartTypeDialog(getDriver());
    }

    protected void openSavedPlotInEditMode(String savedPlotName)
    {
        click(Locator.linkContainingText("Clinical and Assay Data"));
        clickReportGridLink(savedPlotName);
        _ext4Helper.waitForMaskToDisappear();

        // verify that we originally are in view mode and can switch to edit mode
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Save"));
        clickButton("Edit", WAIT_FOR_PAGE);
        _ext4Helper.waitForMaskToDisappear();
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Edit"));
    }
}
