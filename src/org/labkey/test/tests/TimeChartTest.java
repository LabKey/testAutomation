/*
 * Copyright (c) 2011-2015 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.UIContainerHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public abstract class TimeChartTest extends ReportTest
{
    private static final String PROJECT_NAME =  "TimeChartTest Project" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    private static final String FOLDER_NAME =  "Demo Study";
    protected static final String VISIT_FOLDER_NAME =  "Demo Visit Study";
    private static final File STUDY_ZIP = TestFileUtils.getSampleData("studies/LabkeyDemoStudy.zip");
    public static final String TEST_DATA_API_PATH = "server/test/data/api";

    protected static final String ADD_MEASURE_DIALOG = "Add Measure...";
    protected static final String PARTICIPANTS = "Participants";
    protected static final String PARTICIPANTS_GROUPS = "Participant Groups";
    protected static final String GROUP1_NAME = "Some Participants";
    protected static final String GROUP2_NAME = "Other Participants";
    protected static final String GROUP3_NAME = "Yet More Participants";
    public static final String ONE_CHART_PER_PARTICIPANT = "One Chart Per Participant";
    public static final String ONE_CHART_PER_MEASURE = "One Chart Per Measure/Dimension";

    protected static final String[] GROUP1_PTIDS = {"249318596", "249320107"};
    protected static final String[] GROUP2_PTIDS = {"249320127", "249320489"};
    protected static final String[] GROUP3_PTIDS = {"249320489"/*Duplicate from group 2*/, "249320897", "249325717"};

    public static final String USER1 = "user1_timechart@timechart.test";
    public static final String USER2 = "user2_timechart@timechart.test";


    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("study");
    }

    public TimeChartTest()
    {
        setContainerHelper(new UIContainerHelper(this));
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected String getFolderName()
    {
        return FOLDER_NAME;
    }

    @LogMethod protected void configureStudy()
    {
        openProjectMenu();
        if (!isElementPresent(Locator.linkWithText(getProjectName())))
            _containerHelper.createProject(getProjectName(), null);

        _containerHelper.createSubfolder(getProjectName(), getProjectName(), getFolderName(), "Study", null);
        importStudyFromZip(STUDY_ZIP);
    }

    @LogMethod protected void configureVisitStudy()
    {
        openProjectMenu();
        if (!isElementPresent(Locator.linkWithText(getProjectName())))
            _containerHelper.createProject(getProjectName(), null);

        _containerHelper.createSubfolder(getProjectName(), getProjectName(), VISIT_FOLDER_NAME, "Study", null);
        initializePipeline();

        clickFolder(VISIT_FOLDER_NAME);
        clickButton("Process and Import Data");
        _fileBrowserHelper.importFile("study.xml", "Import Study");
        waitForText("Import Study from Pipeline");
        clickButton("Start Import");

        waitForPipelineJobsToComplete(1, "study import", false);
    }

    protected void goToNewTimeChart()
    {
        clickFolder(getFolderName());
        goToManageViews();
        clickAddReport("Time Chart");
        clickChooseInitialMeasure();
    }

    protected void clickChooseInitialMeasure()
    {
        // TODO: migrate usage to TimeChartWizard.chooseInitialMeasure
        clickButton("Choose a Measure", 0);
        _extHelper.waitForExtDialog(ADD_MEASURE_DIALOG);
        _extHelper.waitForLoadingMaskToDisappear(5*WAIT_FOR_JAVASCRIPT);
    }

    protected enum Axis
    {
        X("x"),
        LEFT("left"),
        RIGHT("right");

        String _axis;

        private Axis(String axis)
        {
            _axis = axis;
        }

        public String toString()
        {
            return _axis;
        }
    }

    /**
     * @param axis must be X, Left, or Right, case is important
     * @param textNotPresent intended to be used for numbers that should no longer be present in the axes.
     *                      ideally we'd calculate this automatically, but that's too complicated a problem for now
     *                      TODO:  calculate not-present number automatically
     *                      TODO: find a better way to determine if the range has changed approprietely (Something other than asserting text is or isnt present).
     */
    @LogMethod protected void setAxisValue(@LoggedParam Axis axis, @Nullable String rangeId, @Nullable String lowerBound, @Nullable String upperBound, @Nullable String label, @Nullable String scaleId, @Nullable String scale, @Nullable String[] textPresent, @Nullable String[] textNotPresent)
    {
        if(scaleId!=null && scale!=null)
        {
            _ext4Helper.selectComboBoxItemById(scaleId, scale);
        }

        if(label!=null)
        {
            setFormElement(Locator.name(axis + "-axis-label-textfield"), label);
            waitForElementToDisappear(Locator.css(".x4-btn-disabled.revert"+axis+"AxisLabel"), WAIT_FOR_JAVASCRIPT);
        }

        if (rangeId!=null)
        {
            _ext4Helper.selectRadioButtonById(rangeId + "-boxLabelEl");
            if (lowerBound!=null && upperBound!=null)
            {
                Locator minInput = Locator.name(axis + "axis_rangemin");
                setFormElement(minInput, lowerBound);
                assertEquals(lowerBound, getFormElement(minInput));
//                sleep(500);

                Locator maxInput = Locator.name(axis + "axis_rangemax");
                setFormElement(maxInput, upperBound);
                assertEquals(upperBound, getFormElement(maxInput));
            }
        }

        applyChanges();

        String svgText = null;

        if(textNotPresent!=null)
        {
            waitForElementToDisappear(Locator.css("svg").containing(textNotPresent[0]));
        }

        if(textPresent!=null)
        {
            waitForElement(Locator.css("svg").containing(textPresent[0]));
            svgText = getText(Locator.css("svg"));
            for (String text : textPresent)
                assertTrue("Expected text not found in SVG: " + text, svgText.contains(text));
        }

        if (textNotPresent != null)
        {
            if (svgText == null)
            {
                waitForElement(Locator.css("svg"));
                svgText = getText(Locator.css("svg"));
            }
            for (String text : textNotPresent)
                assertFalse("Unexpected text found in SVG: " + text, svgText.contains(text));
        }
    }

    @LogMethod protected void createParticipantGroups()
    {
        log("Create participant groups");
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), GROUP1_NAME, "Participant", true, GROUP1_PTIDS);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), GROUP2_NAME, "Participant", false, GROUP2_PTIDS);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), GROUP3_NAME, "Participant", false, GROUP3_PTIDS);
    }

    @LogMethod protected void modifyParticipantGroups()
    {
        log("Remove a participant from one group.");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Participant Groups"));
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        _studyHelper.editCustomParticipantGroup(GROUP1_NAME, "Participant", null, false, null, true, true, GROUP1_PTIDS[0]);

        log("Delete one group.");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Participant Groups"));
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        _studyHelper.deleteCustomParticipantGroup(GROUP3_NAME, "Participant");
    }

    protected void addMeasure()
    {
        clickButton("Add Measure", 0);
        _extHelper.waitForExtDialog(ADD_MEASURE_DIALOG);
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
    }

    protected void enterMeasuresPanel()
    {
        clickButton("Measures", 0);
        waitForText("Divide data into Series");
        waitForElement(Ext4Helper.Locators.ext4Button("Add Measure"));
    }

    protected void openSaveMenu()
    {
        clickButtonByIndex("Save", 0, 0);
        waitForText("Viewable By");
    }

    protected void saveReport(boolean expectReload)
    {
        clickAndWait(findButton("Save", 1), expectReload ? WAIT_FOR_PAGE : 0);
        if (!expectReload)
        {
            _extHelper.waitForExtDialog("Success");
            _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        }
        waitFor(() -> isTextPresent("Please select at least one") || // group/participant
                       isTextPresent("No data found") ||
                       isElementPresent(Locator.css("svg")),
                "Time chart failed to appear after saving", WAIT_FOR_JAVASCRIPT);
    }

    protected void setChartTitle(String title)
    {
        setFormElement(Locator.name("chart-title-textfield"), title);
    }

    protected void goToGroupingTab()
    {
        clickButton("Grouping", 0);
        waitForElement(Ext4Helper.Locators.ext4Button("Cancel"));
    }

    protected void goToDeveloperTab()
    {
        clickButton("Developer", 0);
        waitForElement(Ext4Helper.Locators.ext4Button("Cancel"));
    }

    protected void applyChanges()
    {
        clickButton("OK", 0);
        _ext4Helper.waitForMaskToDisappear();
    }

    protected void setParticipantSelection(String selection)
    {
        _ext4Helper.selectRadioButton("Participant Selection:", selection);
    }

    protected void setNumberOfCharts(String selection)
    {
        _ext4Helper.selectRadioButton("Number of Charts:", selection);
    }

    protected void waitForCharts(int count)
    {
        waitForElementToDisappear(Locator.css("div:not(.thumbnail) > svg").index(count), WAIT_FOR_JAVASCRIPT);
        if (count > 0)
            waitForElement(Locator.css("div:not(.thumbnail) > svg").index(count - 1));
    }
}
