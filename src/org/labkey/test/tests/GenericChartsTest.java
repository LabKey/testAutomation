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

import org.junit.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.LogMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * User: tchadick
 * Date: 6/10/13
 */
public abstract class GenericChartsTest extends ReportTest
{
    protected static final String MICE_A = "Mice A";
    protected static final String MICE_B = "Mice B";
    protected static final String MICE_C = "Mice C";
    protected static final String MOUSE_GROUP_CATEGORY = "Cat Mice Let";
    protected static final String CHART_TITLE = "Test Title";

    private List<String> _plots = new ArrayList<>();
    private List<String> _plotDescriptions = new ArrayList<>();

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
            Locator loc = Locator.linkWithText(_plots.get(i));
            waitForElement(loc);
            mouseOver(loc);
            waitForText(_plotDescriptions.get(i));
            mouseOut(loc);
            waitForTextToDisappear(_plotDescriptions.get(i));
        }
    }

    protected abstract void testPlots();

    protected void assertSVG(final String expectedSvgText)
    {
        doesElementAppear(new BaseSeleniumWebTest.Checker()
        {
            @Override
            public boolean check()
            {
                return isElementPresent(Locator.css("svg")) &&
                        expectedSvgText.equals(getText(Locator.css("svg")));
            }
        }, WAIT_FOR_JAVASCRIPT);
        Assert.assertEquals("SVG did not look as expected", expectedSvgText, getText(Locator.css("svg")));
    }

    protected void clickDialogButtonAndWaitForMaskToDisappear(String dialogTitle, String btnTxt)
    {
        _extHelper.clickExtButton(dialogTitle, btnTxt, 0);
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
                return !Boolean.parseBoolean(selenium.getEval("var p = selenium.browserbot.getCurrentWindow().Ext4.getCmp('generic-report-panel-1'); " +
                        "if (p) p.isDirty(); " +
                        "else false;"));
            }
        }, "Page still dirty", WAIT_FOR_JAVASCRIPT);

        _plots.add(name);
        _plotDescriptions.add(description);
    }

    @LogMethod
    protected void createQuickChart(String regionName, String columnName)
    {
        Locator header = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":header"));
        Locator quickChart = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":quick-chart"));

        click(header);
        waitAndClick(quickChart);
        waitForPageToLoad();
    }

    protected void clickOptionButtonAndWaitForDialog(String btnTxt, String dialogTitle)
    {
        clickButton(btnTxt, 0);
        _extHelper.waitForExtDialog(dialogTitle);
    }
}
