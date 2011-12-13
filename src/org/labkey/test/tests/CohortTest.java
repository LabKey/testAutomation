/*
 * Copyright (c) 2010-2011 LabKey Corporation
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
    private static final String XPATH_SPECIMEN_REPORT_TABLE_NEGATIVE = "//td[@id='bodypanel']/div[2]/table[1]";
    private static final String XPATH_SPECIMEN_REPORT_TABLE_POSITIVE = "//td[@id='bodypanel']/div[2]/table[2]";
    private static final String XPATH_SPECIMEN_REPORT_TABLE_UNASSIGNED = "//td[@id='bodypanel']/div[2]/table[3]";
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

    @Override
    protected void doTestSteps() throws Exception
    {
        cohortTest();
    }

    private void cohortTest()
    {
        log("Check advanced cohort features.");
        createProject(PROJECT_NAME, "Study");
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

