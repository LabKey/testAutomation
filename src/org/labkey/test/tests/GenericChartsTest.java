/*
 * Copyright (c) 2013 LabKey Corporation
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
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.LogMethod;

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

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
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

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void doVerifySteps()
    {
        testPlots();

        log("Verify saved plots");
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickTab("Clinical and Assay Data");
        for(int i = 0; i < _plots.size(); i++)
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

    protected void clickDialogButtonAndWaitForMaskToDisappear(String dialogTitle, String btnTxt)
    {
        _ext4Helper.clickWindowButton(dialogTitle, btnTxt, 0, 0);
        _extHelper.waitForExtDialogToDisappear(dialogTitle);
        sleep(500);
        _ext4Helper.waitForMaskToDisappear();
    }

    @LogMethod
    protected void savePlot(String name, String description)
    {
        boolean saveAs = getButtonLocator("Save As") != null;

        clickButton(saveAs ? "Save As" : "Save", 0);
        _extHelper.waitForExtDialog(saveAs ? "Save As" : "Save");
        setFormElement("reportName", name);
        setFormElement("reportDescription", description);
        clickDialogButtonAndWaitForMaskToDisappear(saveAs ? "Save As" : "Save", "Save");
        _extHelper.waitForExtDialogToDisappear("Saved");
        waitForText(name);
        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                return !(Boolean)executeScript("var p = Ext4.getCmp('generic-report-panel-1'); " +
                        "if (p) return p.isDirty(); " +
                        "else return false;");
            }
        }, "Page still dirty", WAIT_FOR_JAVASCRIPT);

        _plots.add(name);
        _plotDescriptions.add(description);
    }

    @LogMethod
    protected void savePlot()
    {
        clickButton("Save", 0);
        _extHelper.waitForExtDialog("Save");
        clickDialogButtonAndWaitForMaskToDisappear("Save", "Save");
        _extHelper.waitForExtDialogToDisappear("Saved");
    }

    @LogMethod
    protected void createQuickChart(String regionName, String columnName)
    {
        Locator header = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":header"));
        Locator quickChart = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":quick-chart"));

        click(header);
        waitAndClickAndWait(quickChart);
    }

    protected void clickOptionButtonAndWaitForDialog(String btnTxt, String dialogTitle)
    {
        click(Locator.ext4Button(btnTxt));
        _extHelper.waitForExtDialog(dialogTitle);
    }
}
