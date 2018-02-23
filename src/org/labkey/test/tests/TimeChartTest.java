/*
 * Copyright (c) 2011-2016 LabKey Corporation
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

    protected static final String GROUP1_NAME = "Some Participants";
    protected static final String GROUP2_NAME = "Other Participants";
    protected static final String GROUP3_NAME = "Yet More Participants";

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
        waitForText("Import Study");
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

    protected void verifyAxisValueChanges(@Nullable String[] textPresent, @Nullable String[] textNotPresent)
    {
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
            waitForElement(Locator.tagWithClass("div", "x4-window").containing("Report saved successfully."));
            _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        }
        waitFor(() -> isTextPresent("Please select at least one") || // group/participant
                       isTextPresent("No data found") ||
                       isElementPresent(Locator.css("svg")),
                "Time chart failed to appear after saving", WAIT_FOR_JAVASCRIPT);
    }

    protected void waitForCharts(int count)
    {
        waitForElementToDisappear(Locator.css("div:not(.thumbnail) > svg").index(count), WAIT_FOR_JAVASCRIPT);
        if (count > 0)
            waitForElement(Locator.css("div:not(.thumbnail) > svg").index(count - 1));
    }
}
