/*
 * Copyright (c) 2010-2012 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: Trey Chadick
 * Date: Jan 13, 2010
 * Time: 4:50:24 PM
 */
public class CohortTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "Cohort Test Project";
    private static final String COHORT_STUDY_ZIP = "/sampledata/study/CohortStudy.zip";
    private static final String XPATH_SPECIMEN_REPORT_TABLE_NEGATIVE = "//td[@id='bodypanel']/div[2]/div[1]/table";
    private static final String XPATH_SPECIMEN_REPORT_TABLE_POSITIVE = "//td[@id='bodypanel']/div[2]/div[2]/table";
    private static final String XPATH_SPECIMEN_REPORT_TABLE_UNASSIGNED = "//td[@id='bodypanel']/div[2]/div[3]/table";
    private static final String TABLE_NEGATIVE = "tableNegative";
    private static final String TABLE_POSITIVE = "tablePositive";
    private static final String TABLE_UNASSIGNED = "tableUnassigned";
    private static final String INFECTED_1 = "Infected1";
    private static final String INFECTED_2 = "Infected2";
    private static final String INFECTED_3 = "Infected3";
    private static final String INFECTED_4 = "Infected4";
    private static final String UNASSIGNED_1 = "Unassigned1";
    private static final String XPATH_COHORT_ASSIGNMENT_TABLE = "//table[@id='participant-cohort-assignments']";
    private static final String COHORT_TABLE = "Cohort Table";
    private static final String COHORT_ALL = "All";
    private static final String COHORT_NEGATIVE = "Negative";
    private static final String COHORT_POSITIVE = "Positive";
    private static final String COHORT_NOCOHORT = "Not in any cohort";
    private static final String[] PTIDS_ALL = {INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4, UNASSIGNED_1};
    private static final String[] PTIDS_POSITIVE = {INFECTED_1, INFECTED_2, INFECTED_3};
    private static final String[] PTIDS_NEGATIVE = {INFECTED_4};
    private static final String[] PTIDS_NOCOHORT = {UNASSIGNED_1};
    private static final String[] PTIDS_POSITIVE_NOCOHORT = {INFECTED_1, INFECTED_2, INFECTED_3, UNASSIGNED_1};

    @Override
    protected void doTestSteps() throws Exception
    {
        cohortTest();
    }

    private void cohortTest()
    {
        log("Check advanced cohort features.");
        _containerHelper.createProject(PROJECT_NAME, "Study");
        importStudyFromZip(new File(getLabKeyRoot() + COHORT_STUDY_ZIP).getPath());
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Specimens");
        // Check all cohorts after initial import.

        waitAndClick(WAIT_FOR_JAVASCRIPT, Locator.linkWithText("Blood"), WAIT_FOR_PAGE);
        assertTextPresent("Count: 25"); // 5 participants x 5 visits
        assertTextPresent("Positive", 10);
        assertTextPresent("Negative", 10);

        clickMenuButton("Participant Groups", "Cohorts", "Negative", "Initial cohort");
        assertTextPresent("Count: 20"); // One participant has no cohorts.
        clickMenuButton("Participant Groups", "Cohorts", "Positive", "Initial cohort");
        assertTextPresent("Count: 0"); // All participants initially negative
        clickMenuButton("Participant Groups", "Cohorts", "Negative", "Current cohort");
        assertTextPresent("Count: 0"); // All participants are positive by the last visit
        clickMenuButton("Participant Groups", "Cohorts", "Positive", "Current cohort");
        assertTextPresent("Count: 20"); // All participants are positive by the last visit
        clickMenuButton("Participant Groups", "Cohorts", "Negative", "Cohort as of data collection");
        assertTextPresent("Count: 10");
        clickMenuButton("Participant Groups", "Cohorts", "Positive", "Cohort as of data collection");
        assertTextPresent("Count: 10");

        clickLinkWithText("Reports");
        clickNavButtonByIndex("View", 2); // Specimen Report: By Cohort
        assertTextPresent("Specimen Report: By Cohort");
        checkCheckbox("viewPtidList");
        clickNavButton("Refresh");
        selenium.assignId(Locator.xpath(XPATH_SPECIMEN_REPORT_TABLE_NEGATIVE).toString(), TABLE_NEGATIVE);
        selenium.assignId(Locator.xpath(XPATH_SPECIMEN_REPORT_TABLE_POSITIVE).toString(), TABLE_POSITIVE);
        selenium.assignId(Locator.xpath(XPATH_SPECIMEN_REPORT_TABLE_UNASSIGNED).toString(), TABLE_UNASSIGNED);
        assertTableCellContains(TABLE_NEGATIVE, 2, 2, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellContains(TABLE_NEGATIVE, 2, 3, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellContains(TABLE_NEGATIVE, 2, 4, INFECTED_3, INFECTED_4);
        assertTableCellContains(TABLE_NEGATIVE, 2, 5, INFECTED_4);
        assertTableCellNotContains(TABLE_NEGATIVE, 2, 2, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_NEGATIVE, 2, 3, INFECTED_1, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_NEGATIVE, 2, 4, INFECTED_1, INFECTED_2, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_NEGATIVE, 2, 5, INFECTED_1, INFECTED_2, INFECTED_3, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_NEGATIVE, 2, 6, INFECTED_1, INFECTED_2, INFECTED_3, UNASSIGNED_1, INFECTED_4);
        assertTableCellContains(TABLE_POSITIVE, 2, 3, INFECTED_1);
        assertTableCellContains(TABLE_POSITIVE, 2, 4, INFECTED_1, INFECTED_2);
        assertTableCellContains(TABLE_POSITIVE, 2, 5, INFECTED_1, INFECTED_2, INFECTED_3);
        assertTableCellContains(TABLE_POSITIVE, 2, 6, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_POSITIVE, 2, 2, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_POSITIVE, 2, 3, INFECTED_2, INFECTED_3, INFECTED_4, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_POSITIVE, 2, 4, INFECTED_3, INFECTED_4, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_POSITIVE, 2, 5, INFECTED_4, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_POSITIVE, 2, 6, UNASSIGNED_1);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 2, UNASSIGNED_1);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 3, UNASSIGNED_1);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 4, UNASSIGNED_1);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 5, UNASSIGNED_1);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 6, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 2, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 3, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 4, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 5, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 6, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);

        selectOptionByText("cohortFilterType", "Initial cohort");
        clickNavButton("Refresh");
        selenium.assignId(Locator.xpath(XPATH_SPECIMEN_REPORT_TABLE_NEGATIVE).toString(), TABLE_NEGATIVE);
        selenium.assignId(Locator.xpath(XPATH_SPECIMEN_REPORT_TABLE_POSITIVE).toString(), TABLE_POSITIVE);
        selenium.assignId(Locator.xpath(XPATH_SPECIMEN_REPORT_TABLE_UNASSIGNED).toString(), TABLE_UNASSIGNED);
        assertTableCellContains(TABLE_NEGATIVE, 2, 2, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellContains(TABLE_NEGATIVE, 2, 3, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellContains(TABLE_NEGATIVE, 2, 4, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellContains(TABLE_NEGATIVE, 2, 5, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellContains(TABLE_NEGATIVE, 2, 6, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_NEGATIVE, 2, 2, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_NEGATIVE, 2, 3, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_NEGATIVE, 2, 4, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_NEGATIVE, 2, 5, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_NEGATIVE, 2, 6, UNASSIGNED_1);
        assertTableCellContains(TABLE_POSITIVE, 2, 0, "No data to show.");
        assertTableCellContains(TABLE_UNASSIGNED, 2, 2, UNASSIGNED_1);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 3, UNASSIGNED_1);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 4, UNASSIGNED_1);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 5, UNASSIGNED_1);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 6, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 2, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 3, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 4, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 5, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 6, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);

        selectOptionByText("cohortFilterType", "Current cohort");
        clickNavButton("Refresh");
        selenium.assignId(Locator.xpath(XPATH_SPECIMEN_REPORT_TABLE_NEGATIVE).toString(), TABLE_NEGATIVE);
        selenium.assignId(Locator.xpath(XPATH_SPECIMEN_REPORT_TABLE_POSITIVE).toString(), TABLE_POSITIVE);
        selenium.assignId(Locator.xpath(XPATH_SPECIMEN_REPORT_TABLE_UNASSIGNED).toString(), TABLE_UNASSIGNED);
        assertTableCellContains(TABLE_NEGATIVE, 2, 0, "No data to show.");
        assertTableCellContains(TABLE_POSITIVE, 2, 2, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellContains(TABLE_POSITIVE, 2, 3, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellContains(TABLE_POSITIVE, 2, 4, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellContains(TABLE_POSITIVE, 2, 5, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellContains(TABLE_POSITIVE, 2, 6, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_POSITIVE, 2, 2, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_POSITIVE, 2, 3, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_POSITIVE, 2, 4, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_POSITIVE, 2, 5, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_POSITIVE, 2, 6, UNASSIGNED_1);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 2, UNASSIGNED_1);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 3, UNASSIGNED_1);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 4, UNASSIGNED_1);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 5, UNASSIGNED_1);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 6, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 2, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 3, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 4, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 5, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 6, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);

        // Check that cohort filters persist through participant view

        // Check that switching visit order changes cohort.
        clickLinkWithText(PROJECT_NAME);
        clickTab("Manage");
        clickLinkWithText("Manage Visits");
        clickLinkWithText("Change Visit Order");
        checkCheckbox("explicitChronologicalOrder");
        checkCheckbox("explicitDisplayOrder");
        selectOptionByText("displayOrderItems", "Visit 3");
        clickNavButtonByIndex("Move Up", 0, 0);
        clickNavButtonByIndex("Move Up", 0, 0);
        selectOptionByText("chronologicalOrderItems", "Visit 3");
        clickNavButtonByIndex("Move Up", 1, 0);
        clickNavButtonByIndex("Move Up", 1, 0);
        clickNavButton("Save");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("View Available Reports");
        clickNavButtonByIndex("View", 2);
        checkCheckbox("viewPtidList");
        clickNavButton("Refresh");
        selenium.assignId(Locator.xpath(XPATH_SPECIMEN_REPORT_TABLE_NEGATIVE).toString(), TABLE_NEGATIVE);
        selenium.assignId(Locator.xpath(XPATH_SPECIMEN_REPORT_TABLE_POSITIVE).toString(), TABLE_POSITIVE);
        assertTableCellContains(TABLE_NEGATIVE, 2, 3, INFECTED_1, INFECTED_2, INFECTED_4);
        assertTableCellContains(TABLE_NEGATIVE, 2, 5, INFECTED_4);
        assertTableCellNotContains(TABLE_NEGATIVE, 2, 3, INFECTED_3, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_NEGATIVE, 2, 5, INFECTED_1, INFECTED_2, INFECTED_3, UNASSIGNED_1);
        assertTableCellContains(TABLE_POSITIVE, 2, 3, INFECTED_3);
        assertTableCellContains(TABLE_POSITIVE, 2, 5, INFECTED_1, INFECTED_2, INFECTED_3);
        assertTableCellNotContains(TABLE_POSITIVE, 2, 3, INFECTED_1, INFECTED_2, INFECTED_4, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_POSITIVE, 2, 5, INFECTED_4, UNASSIGNED_1);

        // Check that deleting a vistit changes the cohort.
        clickLinkWithText(PROJECT_NAME);
        clickTab("Manage");
        clickLinkWithText("Manage Visits");
        clickLinkWithText("edit", 4); // Visit 4
        clickNavButton("Delete visit");
        clickNavButton("Delete");
        clickTab("Manage");
        clickLinkWithText("Manage Cohorts");
        selenium.assignId(Locator.xpath(XPATH_COHORT_ASSIGNMENT_TABLE).toString(), COHORT_TABLE);
        assertTableCellTextEquals(COHORT_TABLE, 4, 1, "Negative"); // Infected4

        // Check all cohorts after manipulation.
        clickLinkWithText(PROJECT_NAME);
        waitAndClick(WAIT_FOR_JAVASCRIPT, Locator.linkWithText("Blood"), WAIT_FOR_PAGE);
        assertTextPresent("Count: 20"); // 5 participants x 4 visits (was five visits, but one was just deleted)

        clickMenuButton("Participant Groups", "Cohorts", "Negative", "Initial cohort");
        assertTextPresent("Count: 16"); // One participant has no cohorts.
        clickMenuButton("Participant Groups", "Cohorts", "Positive", "Initial cohort");
        assertTextPresent("Count: 0"); // All participants initially negative
        clickMenuButton("Participant Groups", "Cohorts", "Negative", "Current cohort");
        assertTextPresent("Count: 4"); // Final visit (where Infected4 joins Positive cohort) has been deleted.
        clickMenuButton("Participant Groups", "Cohorts", "Positive", "Current cohort");
        assertTextPresent("Count: 12");
        clickMenuButton("Participant Groups", "Cohorts", "Negative", "Cohort as of data collection");
        assertTextPresent("Count: 10");
        clickMenuButton("Participant Groups", "Cohorts", "Positive", "Cohort as of data collection");
        assertTextPresent("Count: 6"); // Visit4 samples no longer have a cohort, and are thus not shown.

        // Check that participant view respects filter.
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("2 datasets");
        clickLinkWithText("Test Results");
        clickMenuButton("Participant Groups", "Cohorts", "Positive", "Cohort as of data collection");
        clickLinkWithText("Infected1");
        assertLinkNotPresentWithText("Previous Participant");
        clickLinkWithText("Next Participant");
        assertTextPresent("Infected2");
        assertLinkPresentWithText("Previous Participant");
        clickLinkWithText("Next Participant");
        assertTextPresent("Infected3");
        assertLinkPresentWithText("Previous Participant");
        assertLinkNotPresentWithText("Next Participant"); // Participant 4 should be filtered out
           
        // Check basic cohorts
        log("Check basic cohort features.");
        clickLinkWithText(PROJECT_NAME);
        clickTab("Manage");
        clickLinkWithText("Manage Cohorts");
        clickRadioButtonById("simpleCohorts");
        selenium.getConfirmation();
        waitForPageToLoad();

        clickLinkWithText(PROJECT_NAME);
        waitAndClick(Locator.linkWithText("Blood"));

        waitForText("Positive", 12, WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Negative", 4);
        clickLinkWithText("Reports");

        clickNavButtonByIndex("View", 2); // Specimen Report: By Cohort
        assertTextPresent("Specimen Report: By Cohort");
        checkCheckbox("viewPtidList");
        clickNavButton("Refresh");

        // Basic cohorts should be determined only by the most recent cohort assignment.
        selenium.assignId(Locator.xpath(XPATH_SPECIMEN_REPORT_TABLE_NEGATIVE).toString(), TABLE_NEGATIVE);
        selenium.assignId(Locator.xpath(XPATH_SPECIMEN_REPORT_TABLE_POSITIVE).toString(), TABLE_POSITIVE);
        selenium.assignId(Locator.xpath(XPATH_SPECIMEN_REPORT_TABLE_UNASSIGNED).toString(), TABLE_UNASSIGNED);
        assertTableCellContains(TABLE_NEGATIVE, 2, 2, INFECTED_4);
        assertTableCellContains(TABLE_NEGATIVE, 2, 3, INFECTED_4);
        assertTableCellContains(TABLE_NEGATIVE, 2, 4, INFECTED_4);
        assertTableCellContains(TABLE_NEGATIVE, 2, 5, INFECTED_4);
        assertTableCellNotContains(TABLE_NEGATIVE, 2, 3, INFECTED_1, INFECTED_2, INFECTED_3, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_NEGATIVE, 2, 2, INFECTED_1, INFECTED_2, INFECTED_3, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_NEGATIVE, 2, 4, INFECTED_1, INFECTED_2, INFECTED_3, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_NEGATIVE, 2, 5, INFECTED_1, INFECTED_2, INFECTED_3, UNASSIGNED_1);
        assertTableCellContains(TABLE_POSITIVE, 2, 2, INFECTED_1, INFECTED_2, INFECTED_3);
        assertTableCellContains(TABLE_POSITIVE, 2, 3, INFECTED_1, INFECTED_2, INFECTED_3);
        assertTableCellContains(TABLE_POSITIVE, 2, 4, INFECTED_1, INFECTED_2, INFECTED_3);
        assertTableCellContains(TABLE_POSITIVE, 2, 5, INFECTED_1, INFECTED_2, INFECTED_3);
        assertTableCellNotContains(TABLE_POSITIVE, 2, 2, UNASSIGNED_1, INFECTED_4);
        assertTableCellNotContains(TABLE_POSITIVE, 2, 3, UNASSIGNED_1, INFECTED_4);
        assertTableCellNotContains(TABLE_POSITIVE, 2, 4, UNASSIGNED_1, INFECTED_4);
        assertTableCellNotContains(TABLE_POSITIVE, 2, 5, UNASSIGNED_1, INFECTED_4);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 2, UNASSIGNED_1);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 3, UNASSIGNED_1);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 4, UNASSIGNED_1);
        assertTableCellContains(TABLE_UNASSIGNED, 2, 5, UNASSIGNED_1);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 2, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 3, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 4, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        assertTableCellNotContains(TABLE_UNASSIGNED, 2, 5, INFECTED_1, INFECTED_2, INFECTED_3, INFECTED_4);
        // The enrolledCohortTest assumes the following state:
        // Negative cohort {Infected4}
        // Positive cohort {Infected1, Infected2, Infected3}
        // Not in any cohort {Unassigned1}
        enrolledCohortTest();
    }

    //
    // test enrolled and unenrolled cohort functionality for cohorts
    //
    private void enrolledCohortTest()
    {
        log("Check enrolled/unenrolled cohort features.");
        DataRegionTable table = getCohortDataRegionTable(PROJECT_NAME);

        // verify that we have an 'enrolled' column and both cohorts are
        // true by default
        log("Check that cohorts are enrolled by default.");
        verifyCohortStatus(table, "positive", true);
        verifyCohortStatus(table, "negative", true);

        // verify we can roundtrip enrolled status
        // unenroll the "postiive" cohort and check
        log("Check that enrolled bit is roundtripped successfully.");
        changeCohortStatus(table, "positive", false);
        verifyCohortStatus(table, "positive", false);

        // start with everyone enrolled again
        changeCohortStatus(table, "positive", true);
        refreshParticipantList();

        // the rules for when we display the "enrolled" keyword are as follows:
        // 1.  if there are no unenrolled cohorts, then never show enrolled text
        // 2.  if unenrolled cohorts exist and all selected cohorts are enrolled then use the enrolled text to describe them
        // 3.  if unenrolled cohorts exist and all selected cohorts are unenrolled, do not use enrolled text
        // 4.  if unenrolled cohorts exist and selected cohorts include both enrolled and unenrolled cohorts, then do not show the enrolled text
        // note:  participants that don't belong to any cohort are enrolled

        log("verify enrolled text: all cohorts are enrolled");
        // rule #1:  no unenrolled cohorts exist so do not expect the enrolled text.
        verifyParticipantList(PTIDS_ALL, false);

        // unenroll all cohorts
        table = getCohortDataRegionTable(PROJECT_NAME);
        changeCohortStatus(table, "positive", false);
        changeCohortStatus(table, "negative", false);
        refreshParticipantList();

        log("verify enrolled text: all cohorts are unenrolled");
        // rule #2:  we expect the "enrolled" text since we've selected the 'not in any cohort' group by default
        verifyParticipantList(PTIDS_NOCOHORT, true);
        // rule #3:  since "all" includes both enrolled and unenrolled cohorts, don't show the enrolled text
        verifyCohortSelection(null, COHORT_ALL, PTIDS_ALL, false, "Found 5 participants of 5.");

        // rule #3: Negative cohort is not enrolled, so don't show enrolled text
        verifyCohortSelection(COHORT_ALL, COHORT_NEGATIVE, PTIDS_NEGATIVE, false, "Found 1 participant of 5.");

        // rule #3: Positive cohort is not enrolled, so don't show enrolled text
        verifyCohortSelection(COHORT_NEGATIVE, COHORT_POSITIVE, PTIDS_POSITIVE, false, "Found 3 participants of 5.");

        // rule #2: "not in any cohort" is enrolled so show text
        verifyCohortSelection(COHORT_POSITIVE, COHORT_NOCOHORT, PTIDS_NOCOHORT, true, "Found 1 enrolled participant of 5.");

        // test both enrolled and unenrolled cohorts
        table = getCohortDataRegionTable(PROJECT_NAME);
        changeCohortStatus(table, "positive", true);
        changeCohortStatus(table, "negative", false);
        refreshParticipantList();

        log("verify enrolled text: Positive cohort enrolled; negative cohort unenrolled");
        // rule #2, showing enrolled cohorts (positive and not in any cohort)
        verifyParticipantList(PTIDS_POSITIVE_NOCOHORT, true);

        // rule #4, don't show enrolled text since we have a mix of enrolled and unenrolled
        verifyCohortSelection(null, COHORT_ALL, PTIDS_ALL, false, "Found 5 participants of 5.");

        // rule #3, don't show enrolled text since we only have unenrolled cohorots
        verifyCohortSelection(COHORT_ALL, COHORT_NEGATIVE, PTIDS_NEGATIVE, false, "Found 1 participant of 5.");

        // rule #2, only showing enrolled cohorts
        verifyCohortSelection(COHORT_NEGATIVE, COHORT_POSITIVE, PTIDS_POSITIVE, true, "Found 3 enrolled participants of 5.");

        // rule #2, only showing enrolled cohorts
        verifyCohortSelection(COHORT_POSITIVE, COHORT_NOCOHORT, PTIDS_NOCOHORT, true, "Found 1 enrolled participant of 5.");
    }

    private void verifyCohortSelection(String previousCohort, String nextCohort, String[] expectedParticipants, boolean expectEnrolledText, String waitText)
    {
        if (previousCohort != null)
        {
            mouseDownGridCellCheckbox(previousCohort);
        }
        mouseDownGridCellCheckbox(nextCohort);
        waitForText(waitText);
        verifyParticipantList(expectedParticipants, expectEnrolledText);
    }

    private boolean isPartipantInGroup(String ptid, String[] ptidGroup)
    {
        for (String id : ptidGroup)
        {
            if ( 0 == ptid.compareToIgnoreCase(id))
            {
                return true;
            }
        }

        return false;
    }

    private void refreshParticipantList()
    {
        clickTab("Participants");
        waitForText("Filter"); // Wait for participant list to appear.
    }

    private void verifyParticipantList(String[] ptids, boolean expectEnrolledText)
    {
        // we should not see the "enrolled" text if no participants are unenrolled
        if (!expectEnrolledText)
        {
            assertTextNotPresent("enrolled");
        }
        else
        {
            assertTextPresent("enrolled");
        }

        // make sure everyone in the group is there
        for(String ptid : ptids)
        {
            assertTextPresent(ptid);
        }

        // make sure everyone not in the group is not there
        for (String ptid : PTIDS_ALL)
        {
            if (!isPartipantInGroup(ptid, ptids))
            {
                assertTextNotPresent(ptid);
            }
        }
    }

    @Override
    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME);} catch (Throwable T) {}
    }

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }
}

