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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyC;
import org.labkey.test.categories.Reports;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({DailyC.class, Reports.class})
public class ParticipantReportTest extends ReportTest
{
    private static final String PARTICIPANT_REPORT_NAME = "Test Participant Report";
    private static final String PARTICIPANT_REPORT_DESCRIPTION = "Participant report created by ReportTest";
    private static final String PARTICIPANT_REPORT2_NAME = "Test Participant Report 2";
    private static final String PARTICIPANT_REPORT2_DESCRIPTION = "Another participant report created by ReportTest";
    private static final String ADD_MEASURE_TITLE = "Add Measure";
    private static final String PARTICIPANT_REPORT3_NAME = "Group Filter Report";
    private static final String PARTICIPANT_GROUP_ONE = "TEST_GROUP_1";
    private static final String PARTICIPANT_GROUP_TWO = "TEST_GROUP_2";
    private static final String MICE_A = "Mice A";
    private static final String MICE_B = "Mice B";
    private static final String MICE_C = "Mice C";
    private static final String MOUSE_GROUP_CATEGORY_A = "Cat Mice Let";
    private static final String MOUSE_GROUP_CATEGORY_B = "Cat Mice Foo";
    private static final String COHORT_1 = "Group 1";
    private static final String COHORT_2 = "Group 2";
    private static final String[] PTIDS_ONE = {"999320016", "999320485", "999320518", "999320529", "999320533", "999320541",
                                               "999320557", "999320565", "999320576", "999320582", "999320590", "999320609"};
    private static final String[] PTIDS_TWO = {"999320613", "999320624", "999320638", "999320646", "999320652", "999320660",
                                               "999320671", "999320687", "999320695", "999320703", "999320719", "999321029",
                                               "999321033"};
    private static final String PARTICIPANT_REPORT4_NAME = "Specimen Filter Report";
    private static final String SPECIMEN_GROUP_ONE = "SPEC GROUP 1";
    private static final String SPECIMEN_GROUP_TWO = "SPEC GROUP 2";
    private static final String[] SPEC_PTID_ONE = {"999320016"};
    private static final String[] SPEC_PTID_TWO = {"999320518"};
    private static final String PARTICIPANT_REPORT5_NAME = "Demographic Participant Report";

    @LogMethod
    protected void doCreateSteps()
    {
        enableEmailRecorder();

        // import study and wait; no specimens needed
        importStudy();
        startSpecimenImport(2);

        // wait for study and specimens to finish loading
        waitForSpecimenImport();

        //Need to create participant groups before we flip the demographics bit on DEM-1.
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), PARTICIPANT_GROUP_ONE, "Mouse", PTIDS_ONE);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), PARTICIPANT_GROUP_TWO, "Mouse", PTIDS_TWO);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), SPECIMEN_GROUP_ONE, "Mouse", SPEC_PTID_ONE);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), SPECIMEN_GROUP_TWO, "Mouse", SPEC_PTID_TWO);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), MICE_A, "Mouse", MOUSE_GROUP_CATEGORY_A, true, true, "999320016,999320518,999320529,999320557");
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), MICE_B, "Mouse", MOUSE_GROUP_CATEGORY_A, false, true, "999320565,999320576,999320582,999320609");
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), MICE_C, "Mouse", MOUSE_GROUP_CATEGORY_A, false, true, "999320613,999320671,999320687");

        // need this to turn off the demographic bit in the DEM-1 dataset
        clickFolder(getFolderName());
        setDemographicsBit("DEM-1: Demographics", false);
    }

    @LogMethod
    protected void doVerifySteps()
    {
        doParticipantGroupCategoriesTest();

        doParticipantReportTest();
        doParticipantFilterTests(); // Depends on successful doParticipantReportTest
    }

    @LogMethod
    private void doParticipantGroupCategoriesTest()
    {
        navigateToFolder(getProjectName(), getFolderName());

        // Check that groups have correct number of members
        clickAndWait(Locator.linkWithText("Mice"));
        waitForText("Cohorts"); // Wait for participant list to appear.
        waitForText("Found 25 enrolled mice of 138.");

        // no longer an all check box
        _ext4Helper.deselectAllParticipantFilter();
        waitForText("No matching enrolled Mice");

        _ext4Helper.checkGridRowCheckbox(MICE_C);
        waitForText("Found 3 enrolled mice of 138.");

        _ext4Helper.checkGridRowCheckbox(MICE_B);
        waitForText("Found 7 enrolled mice of 138.");

        // Test changing category and changing it back
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Mouse Groups"));
        _extHelper.waitForLoadingMaskToDisappear(10000);
        _studyHelper.editCustomParticipantGroup(MICE_C, "Mouse", MOUSE_GROUP_CATEGORY_B, true, true, false, false);
        waitForText(MOUSE_GROUP_CATEGORY_B);
        _studyHelper.editCustomParticipantGroup(MICE_C, "Mouse", MOUSE_GROUP_CATEGORY_A, false, true, false, false);
        waitForTextToDisappear(MOUSE_GROUP_CATEGORY_B);

        // Add more participants to a group
        _studyHelper.editCustomParticipantGroup(MICE_C, "Mouse", null, false, true, false, false, "999320703,999320719");

        // Check that group has correct number of participants
        clickAndWait(Locator.linkWithText("Mice"));
        waitForElement(Locator.css(".lk-filter-panel-label")); // Wait for participant list to appear.
        _ext4Helper.deselectAllParticipantFilter();
        waitForText("No matching enrolled Mice");
        _ext4Helper.checkGridRowCheckbox(MICE_C);
        waitForText("Found 5 enrolled mice of 138.");
    }

    @LogMethod
    private void doParticipantReportTest()
    {
        log("Testing Participant Report");

        navigateToFolder(getProjectName(), getFolderName());
        goToManageViews();
        BootstrapMenu.find(getDriver(), "Add Report")
                .clickSubMenu(true, "Mouse Report");
        // select some measures from a dataset
        clickButton("Choose Measures", 0);
        _extHelper.waitForExtDialog(ADD_MEASURE_TITLE);
        waitForElement(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE) + "//tr[contains(@class, 'x4-grid-row')][1]"));
        _extHelper.setExtFormElementByType(ADD_MEASURE_TITLE, "text", "cpf-1");
        pressEnter(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE) + "//input[contains(@class, 'x4-form-text') and @type='text']"));
        waitForElementToDisappear(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE) + "//tr[contains(@class, 'x4-grid-row')][18]"));
        assertEquals("Wrong number of measures visible after filtering.", 17, getElementCount(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE) + "//tr[contains(@class, 'x4-grid-row')]")));

        _ext4Helper.selectGridItem("label", "2a. Creatinine", -1, "measuresGridPanel", true);
        _ext4Helper.selectGridItem("label", "1a.ALT AE Severity Grade", -1, "measuresGridPanel", true);
        _ext4Helper.selectGridItem("label", "1a. ALT (SGPT)", -1, "measuresGridPanel", true);

        clickButton("Select", 0);

        waitForText("Visit Date", 8, WAIT_FOR_JAVASCRIPT);
        assertTextPresent("2a. Creatinine", 19); // 8 mice + 8 grid field tooltips + 1 Report Field list + 2 in hidden add field dialog
        assertTextPresent("1a.ALT AE Severity Grade", 18); // 8 mice + 8 grid field tooltips + 1 Report Field list + 1 in hidden add field dialog
        assertTextPresent("1a. ALT (SGPT)", 18); // 8 mice + 8 grid field tooltips + 1 Report Field list + 1 in hidden add field dialog

        // select additional measures from another dataset
        clickButton("Choose Measures", 0);
        _extHelper.waitForExtDialog(ADD_MEASURE_TITLE);
        waitForElement(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE) + "//tr[contains(@class, 'x4-grid-row')][1]"));
        _extHelper.setExtFormElementByType(ADD_MEASURE_TITLE, "text", "2a. Creatinine");
        pressEnter(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE) + "//input[contains(@class, 'x4-form-text') and @type='text']"));
        waitForElementToDisappear(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE) + "//tr[contains(@class, 'x4-grid-row')][5]"), WAIT_FOR_JAVASCRIPT);
        assertEquals("Wrong number of measures visible after filtering.", 4, getElementCount(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE) + "//tr[contains(@class, 'x4-grid-row')]")));
        _ext4Helper.selectGridItem("queryName", "CPS-1", -1, "measuresGridPanel", true);
        clickButton("Select", 0);

        // at this point the report should render some content
        waitForText("Creatinine", 37, WAIT_FOR_JAVASCRIPT); // 8 mice (x2 columns + tooltips) + 1 Report Field list + 2 in hidden add field dialog
        assertTextPresent("1a.ALT AE Severity Grade", 18); // 8 mice + 8 grid field tooltips + 1 Report Field list + 1 in hidden add field dialog
        assertTextPresent("1a. ALT (SGPT)", 18); // 8 mice + 8 grid field tooltips + 1 Report Field list + 1 in hidden add field dialog

        assertTextPresent("Showing partial results while in edit mode.");
        click(Locator.xpath("//a[./span[@title = 'Edit']]"));
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT); // Edit panel should be hidden
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 8 Results"));

        // verify form validation
        click(Locator.xpath("//a[./span[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        clickButton("Save", 0);
        _extHelper.waitForExtDialog("Error");
        clickButton("OK", 0);

        String transposeCheckPtid = "999320646";
        Locator.XPathLocator reportRow = Locator.tag("table").withClass("report").append("/tbody/tr").containing(transposeCheckPtid).append("/following-sibling::tr");
        log("assert text prsent in original form");
        String[] initialData = {
                "Visit Label Visit Date 2a. Creatinine 1a.ALT AE Severity Grade 1a. ALT (SGPT) 2a. Creatinine",
                "2 week Post-V#1   3.5   45  ",
                "Int. Vis. %{S.1.1} .%{S.2.1}   1.9      "};
        waitForElement(Locator.linkContainingText(transposeCheckPtid));
        for (int i = 0; i < initialData.length; i++)
        {
            WebElement row = reportRow.index(i).findElement(getDriver());
            scrollIntoView(row);
            assertEquals("Data not as expected for participant : " + transposeCheckPtid,
                    initialData[i],
                    row.getText());
        }

        clickButton("Transpose", 0);
        log("assert text tranposed");
        String[] transposedData = {
                "  2 week Post-V#1 Int. Vis. %{S.1.1} .%{S.2.1}",
                "Visit Date    ",
                "2a. Creatinine 3.5 1.9",
                "1a.ALT AE Severity Grade    ",
                "1a. ALT (SGPT) 45  ",
                "2a. Creatinine    "};
        for (int i = 0; i < transposedData.length; i++)
        {
            WebElement row = reportRow.index(i).findElement(getDriver());
            scrollIntoView(row);
            assertEquals("Data not transposed for participant : " + transposeCheckPtid,
                    transposedData[i],
                    row.getText());
        }

        // save the report for real
        _extHelper.setExtFormElementByLabel("Report Name", PARTICIPANT_REPORT_NAME);
        _extHelper.setExtFormElementByLabel("Report Description", PARTICIPANT_REPORT_DESCRIPTION);
        clickSaveParticipantReport();

        // verify visiting saved report
        goToManageViews();
        clickReportGridLink(PARTICIPANT_REPORT_NAME);

        waitForText("Creatinine", 32, WAIT_FOR_JAVASCRIPT); // 8 mice (x2 column headers) + 8 mice (x2 column tooltips)
        assertTextPresent(PARTICIPANT_REPORT_NAME);
        assertTextPresent("1a.ALT AE Severity Grade", 16); // 8 mice + 8 grid field tooltips
        assertTextPresent("1a. ALT (SGPT)", 16); // 8 mice + 8 grid field tooltips
        assertElementPresent(Locator.css("table.x4-toolbar-item").withText("Showing 8 Results"));
        assertElementPresent(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]")); // Edit panel should be hidden

        // Delete a column and save report
        click(Locator.xpath("//a[./span[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath("//img[@data-qtip = 'Delete']")); // Delete 'Creatinine' column.
        clickSaveParticipantReport();

        // Delete a column save a copy of the report (Save As)
        // Not testing column reorder. Ext4 and selenium don't play well together for drag & drop
        click(Locator.xpath("//a[./span[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath("//img[@data-qtip = 'Delete']")); // Delete 'Severity Grade' column.
        clickButton("Save As", 0);
        _extHelper.waitForExtDialog("Save As");
        _extHelper.setExtFormElementByLabel("Save As", "Report Name", PARTICIPANT_REPORT2_NAME);
        _extHelper.setExtFormElementByLabel("Save As", "Report Description", PARTICIPANT_REPORT2_DESCRIPTION);
        clickButtonByIndex("Save", 1, 0);
        _ext4Helper.waitForComponentNotDirty("participant-report-panel-1");
        waitForTextToDisappear("Severity Grade");

        // Verify saving with existing report name.
        click(Locator.xpath("//a[./span[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        clickButton("Save As", 0);
        _extHelper.waitForExtDialog("Save As");
        _extHelper.setExtFormElementByLabel("Save As", "Report Name", PARTICIPANT_REPORT_NAME);
        _extHelper.setExtFormElementByLabel("Save As", "Report Description", PARTICIPANT_REPORT2_DESCRIPTION);
        clickButtonByIndex("Save", 1, 0);
        _extHelper.waitForExtDialog("Failure");
        assertTextPresent("Another report with the same name already exists.");
        clickButton("OK", 0);
        clickButton("Cancel", 0); // Verify cancel button.
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT); // Edit panel should be hidden


        // verify modified, saved report
        goToManageViews();
        clickReportGridLink(PARTICIPANT_REPORT_NAME);

        waitForText("Creatinine", 16, WAIT_FOR_JAVASCRIPT); // 8 mice + 8 grid field tooltips
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 8 Results")); // There should only be 8 results, and it should state that.

        assertTextPresent(PARTICIPANT_REPORT_NAME);
        assertTextPresent("1a.ALT AE Severity Grade", 16); // 8 mice + 8 grid field tooltips
        assertTextPresent("1a. ALT (SGPT)", 16); // 8 mice + 8 grid field tooltips
        assertElementPresent(Locator.css("table.x4-toolbar-item").withText("Showing 8 Results"));
        assertElementPresent(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]")); // Edit panel should be hidden
        log("Verify report name and description.");
        click(Locator.xpath("//a[./span[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        assertEquals("Wrong report description", PARTICIPANT_REPORT_DESCRIPTION, _extHelper.getExtFormElementByLabel("Report Description"));


        // verify modified, saved-as report
        goToManageViews();
        clickReportGridLink(PARTICIPANT_REPORT2_NAME);

        waitForText("Creatinine", 16, WAIT_FOR_JAVASCRIPT); // 8 mice + 8 grid field tooltips
        assertTextPresent(PARTICIPANT_REPORT2_NAME);
        assertTextNotPresent("1a.ALT AE Severity Grade");
        assertTextPresent("1a. ALT (SGPT)", 16); // 8 mice + 8 grid field tooltips
        assertElementPresent(Locator.css("table.x4-toolbar-item").withText("Showing 8 Results"));
        assertElementPresent(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]")); // Edit panel should be hidden
        log("Verify report name and description.");
        click(Locator.xpath("//a[./span[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        assertEquals("Wrong report description", PARTICIPANT_REPORT2_DESCRIPTION, _extHelper.getExtFormElementByLabel("Report Description"));

        // Test group filtering
        goToManageViews();
        BootstrapMenu.find(getDriver(), "Add Report")
                .clickSubMenu(true, "Mouse Report");
        // select some measures from a dataset
        clickButton("Choose Measures", 0);
        _extHelper.waitForExtDialog(ADD_MEASURE_TITLE);
        waitForElement(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE) + "//tr[contains(@class, 'x4-grid-row')][1]"));

        _ext4Helper.selectGridItem("label", "17a. Preg. test result", -1, "measuresGridPanel", true);
        _ext4Helper.selectGridItem("label", "1.Adverse Experience (AE)", -1, "measuresGridPanel", true);

        click(Ext4Helper.Locators.ext4Button("Select"));

        //Deselect All
        Locator filterExpander = Locator.xpath("(//img[contains(@class, 'x4-tool-expand-right')])[1]");
        waitAndClick(filterExpander);
        waitForElement(Locator.css(".initSelectionComplete"));
        _ext4Helper.deselectAllParticipantFilter();
        click(Locator.xpath("//a[./span[@title = 'Edit']]"));
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT); // Edit panel should be hidden
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 0 Results"));

        //Mouse down on GROUP 1
        _ext4Helper.checkGridRowCheckbox(PARTICIPANT_GROUP_ONE, 0);
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 12 Results"));

        //Check if all PTIDs of GROUP 1 are visible.
        List<String> ptid_list2 = Arrays.asList(PTIDS_TWO);
        for (String ptid : PTIDS_ONE)
        {
            assertTextPresent(ptid);

            String base = "//td//a[text()='" + ptid + "']/../../..//td[contains(text(), 'Groups:')]/following-sibling::td[contains(normalize-space(), '";
            waitForElement(Locator.xpath(base + PARTICIPANT_GROUP_ONE + "')]"));

            if (ptid_list2.contains(ptid))
            {
                assertElementPresent(Locator.xpath(base + PARTICIPANT_GROUP_TWO + "')]"));
            }

        }

        _ext4Helper.checkGridRowCheckbox(PARTICIPANT_GROUP_TWO, 0);
        // groups are disjoint
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 0 Results"));

        _ext4Helper.uncheckGridRowCheckbox(PARTICIPANT_GROUP_ONE, 0);
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 13 Results"));

        //Check if all PTIDs of GROUP 2 are visible
        for (String ptid : PTIDS_TWO)
        {
            assertElementPresent(Locator.linkWithText(ptid));
        }
        //Make sure none from Group 1 are visible.
        for (String ptid : PTIDS_ONE)
        {
            assertTextNotPresent(ptid);
        }

        click(Locator.xpath("//a[./span[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        _extHelper.setExtFormElementByLabel("Report Name", PARTICIPANT_REPORT3_NAME);
        clickSaveParticipantReport();

        //Participant report with specimen fields.
        goToManageViews();
        BootstrapMenu.find(getDriver(), "Add Report").clickSubMenu(true, "Mouse Report");
        // select some measures from a dataset
        clickButton("Choose Measures", 0);
        _extHelper.waitForExtDialog(ADD_MEASURE_TITLE);
        waitForElement(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE) + "//tr[contains(@class, 'x4-grid-row')][1]"));
        _extHelper.setExtFormElementByType(ADD_MEASURE_TITLE, "text", "primary type vial counts blood");
        pressEnter(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE) + "//input[contains(@class, 'x4-form-text') and @type='text']"));

        _ext4Helper.selectGridItem("label", "Blood (Whole):VialCount", -1, "measuresGridPanel", true);
        _ext4Helper.selectGridItem("label", "Blood (Whole):AvailableCount", -1, "measuresGridPanel", true);

        clickButton("Select", 0);
        waitForElement(Locator.linkWithText(PTIDS_ONE[0]));

        click(Locator.xpath("//a[./span[@title = 'Edit']]"));
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT); // Edit panel should be hidden
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 116 Results"));

        //Deselect All
        click(filterExpander);
        waitForElement(Locator.css(".initSelectionComplete"));
        _ext4Helper.deselectAllParticipantFilter();
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 0 Results"));

        //Mouse down on SPEC GROUP 1
        _ext4Helper.checkGridRowCheckbox(SPECIMEN_GROUP_ONE, 0);
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 1 Results"));
        assertEquals(1, getElementCount(Locator.xpath("//td[text()='Screening']/..//td[3][text()='23']")));
        assertEquals(1, getElementCount(Locator.xpath("//td[text()='Screening']/..//td[4][text()='3']")));

        //Add SPEC GROUP 2
        _ext4Helper.checkGridRowCheckbox(SPECIMEN_GROUP_TWO, 0);
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 0 Results"));
        //Remove SPEC GROUP 1
        _ext4Helper.uncheckGridRowCheckbox(SPECIMEN_GROUP_ONE, 0);
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 1 Results"));
        assertEquals(1, getElementCount(Locator.xpath("//td[text()='Screening']/..//td[3][text()='15']")));
        assertEquals(1, getElementCount(Locator.xpath("//td[text()='Screening']/..//td[4][text()='1']")));

        click(Locator.xpath("//a[./span[@title = 'Edit']]"));
        waitForElementToDisappear(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT);
        _extHelper.setExtFormElementByLabel("Report Name", PARTICIPANT_REPORT4_NAME);
        clickSaveParticipantReport();

        //Participant report with multiple demographic fields
        _studyHelper.goToManageDatasets()
                .selectDatasetByName("DEM-1")
                .clickEditDefinition()
                .setIsDemographicData(true)
                .save();

        goToManageViews().clickAddReport("Mouse Report");
        // select some measures from the demographics
        clickButton("Choose Measures", 0);
        _extHelper.waitForExtDialog(ADD_MEASURE_TITLE);
        waitForElement(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE) + "//tr[contains(@class, 'x4-grid-row')][1]"));
        _extHelper.setExtFormElementByType(ADD_MEASURE_TITLE, "text", "demographic");
        pressEnter(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_TITLE) + "//input[contains(@class, 'x4-form-text') and @type='text']"));

        _ext4Helper.selectGridItem("label", "1.Date of Birth", -1, "measuresGridPanel", true);
        _ext4Helper.selectGridItem("label", "2.What is your sex?", -1, "measuresGridPanel", true);
        _ext4Helper.selectGridItem("label", "5. Sexual orientation", -1, "measuresGridPanel", true);
        clickButton("Select", 0);
        waitForText(WAIT_FOR_JAVASCRIPT, "Showing partial results while in edit mode.");

        // verify the data in the report
        waitForText("1.Date of Birth", 27, WAIT_FOR_JAVASCRIPT); // 24 mice + 1 Report Measures list + 2 in hidden add measure dialog
        waitForText("2.What is your sex?", 26, WAIT_FOR_JAVASCRIPT); // 24 mice + 1 Report Measures list + 1 in hidden add measure dialog
        waitForText("5. Sexual orientation", 26, WAIT_FOR_JAVASCRIPT); // 24 mice + 1 Report Measures list + 1 in hidden add measure dialog
        assertTextPresentInThisOrder("1965-03-06", "Female", "heterosexual");

        _extHelper.setExtFormElementByLabel("Report Name", PARTICIPANT_REPORT5_NAME);
        clickSaveParticipantReport();
    }

    private void clickSaveParticipantReport()
    {
        clickButton("Save", 0);
        shortWait().until(ExpectedConditions.invisibilityOfElementLocated(By.className("report-config-panel"))); // Edit panel should be hidden
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        _extHelper.waitForExtDialogToDisappear("Saved");
        _ext4Helper.waitForComponentNotDirty("participant-report-panel-1");
    }

    @LogMethod
    private void doParticipantFilterTests()
    {
        doParticipantReportFilterTest();
        doParticipantListFilterTest();
    }

    @LogMethod
    private void doParticipantReportFilterTest()
    {
        navigateToFolder(getProjectName(), getFolderName());
        clickTab("Clinical and Assay Data");
        waitAndClickAndWait(Locator.linkWithText(PARTICIPANT_REPORT5_NAME));

        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 24 Results"));
        waitForElement(Locator.css(".report-filter-window.x4-collapsed"));
        log("Verify report filter window");
        expandReportFilterWindow();
        collapseReportFilterWindow();
        expandReportFilterWindow();
        closeReportFilterWindow();
        openReportFilterWindow();

        _ext4Helper.deselectAllParticipantFilter();
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 0 Results"));

        _ext4Helper.selectAllParticipantFilter();
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 24 Results"));

        _ext4Helper.clickParticipantFilterGridRowText("Not in any cohort", 0);
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 0 Results"));

        _ext4Helper.checkGridRowCheckbox(COHORT_1);
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 10 Results"));

        _ext4Helper.clickParticipantFilterGridRowText(COHORT_2, 0);
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 14 Results"));

        // Selecting all or none of an entire category should not filter report
        _ext4Helper.clickParticipantFilterGridRowText(PARTICIPANT_GROUP_ONE, 0); // click group, not category with the same name
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 6 Results"));
        _ext4Helper.uncheckGridRowCheckbox(PARTICIPANT_GROUP_ONE, 0); // click group, not category with the same name
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 14 Results"));
        _ext4Helper.clickParticipantFilterGridRowText(PARTICIPANT_GROUP_ONE, 0); // click group, not category with the same name
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 6 Results"));
        _ext4Helper.clickParticipantFilterCategory(PARTICIPANT_GROUP_ONE); // click category
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 14 Results"));

        //Check intersection between cohorts and multiple categories
        _ext4Helper.clickParticipantFilterGridRowText(MICE_A, 0);
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 3 Results"));
        _ext4Helper.clickParticipantFilterGridRowText(SPECIMEN_GROUP_TWO, 0); // click group, not category with the same name
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 1 Results"));

        _ext4Helper.selectAllParticipantFilter();
        waitForElement(Locator.css("table.x4-toolbar-item").withText("Showing 24 Results"));

        click(Locator.xpath("//a[./span[@title = 'Edit']]"));
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div[" + Locator.NOT_HIDDEN + "]"), WAIT_FOR_JAVASCRIPT);
        clickSaveParticipantReport();

        //TODO: Test toggling participant/group modes
        //TODO: Blocked: 16110: Participant report filter panel loses state when switching between participant and group modes
    }

    @LogMethod
    private void doParticipantListFilterTest()
    {
        collapseReportFilterWindow(); // it is blocking the project menu
        navigateToFolder(getProjectName(), getFolderName());
        clickTab("Mice");
        waitForText("Found 25 enrolled mice of 138."); // Not in any cohort deselected initially

        _ext4Helper.deselectAllParticipantFilter();
        waitForText("No matching enrolled Mice.");

        _ext4Helper.selectAllParticipantFilter();
        waitForText("Found 138 mice of 138.");

        _ext4Helper.clickParticipantFilterGridRowText("Not in any cohort", 0);
        waitForText("Found 113 mice of 138.");

        _ext4Helper.checkGridRowCheckbox(COHORT_1);
        waitForText("Found 123 mice of 138.");

        _ext4Helper.clickParticipantFilterGridRowText(COHORT_2, 0);
        waitForText("Found 15 enrolled mice of 138.");

        // Selecting all or none of an entire category should not filter report
        _ext4Helper.clickParticipantFilterGridRowText(PARTICIPANT_GROUP_ONE, 0);
        waitForText("Found 7 enrolled mice of 138.");
        _ext4Helper.uncheckGridRowCheckbox(PARTICIPANT_GROUP_ONE, 0);
        waitForText("Found 15 enrolled mice of 138.");
        _ext4Helper.clickParticipantFilterGridRowText(PARTICIPANT_GROUP_ONE, 0);
        waitForText("Found 7 enrolled mice of 138.");
        _ext4Helper.clickParticipantFilterCategory(PARTICIPANT_GROUP_ONE);
        waitForText("Found 15 enrolled mice of 138.");

        //Check intersection between cohorts and multiple categories
        _ext4Helper.clickParticipantFilterGridRowText(MICE_A, 0);
        waitForText("Found 3 enrolled mice of 138.");
        _ext4Helper.clickParticipantFilterGridRowText(SPECIMEN_GROUP_TWO, 0);
        waitForText("Found 1 enrolled mouse of 138.");

        setFormElement(Locator.id("participantsDiv1.filter"), PTIDS_ONE[0]);
        waitForText("No mouse IDs contain \"" + PTIDS_ONE[0] + "\".");
        _ext4Helper.selectAllParticipantFilter();
        waitForText("Found 1 enrolled mouse of 138.");
    }

    private void expandReportFilterWindow()
    {
        assertElementPresent(Locator.css(".report-filter-window.x4-collapsed"));
        click(Locator.css(".report-filter-window .x4-tool-expand-right"));
        waitForElement(Locator.css(".report-filter-window .x4-tool-collapse-left"));
        assertElementNotPresent(Locator.css(".report-filter-window.x4-collapsed"));
    }

    private void collapseReportFilterWindow()
    {
        assertElementNotPresent(Locator.css(".report-filter-window.x4-collapsed"));
        assertElementNotPresent(Locator.css(".report-filter-window.x4-hide-offsets"));
        click(Locator.css(".report-filter-window .x4-tool-collapse-left"));
        waitForElement(Locator.css(".report-filter-window.x4-collapsed"));
    }

    private void closeReportFilterWindow()
    {
        assertElementPresent(Locator.css(".report-filter-window"));
        assertElementNotPresent(Locator.css(".report-filter-window.x4-hide-offsets"));
        click(Locator.css(".report-filter-window .x4-tool-close"));
        waitForElement(Locator.css(".report-filter-window.x4-hide-offsets"));
    }

    private void openReportFilterWindow()
    {
        assertElementPresent(Locator.css(".report-filter-window.x4-hide-offsets"));
        clickButton("Filter Report", 0);
        waitForElementToDisappear(Locator.css(".report-filter-window.x4-hide-offsets"), WAIT_FOR_JAVASCRIPT);
    }
}
