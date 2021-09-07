/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.studydesigner.AssayScheduleWebpart;
import org.labkey.test.components.studydesigner.ImmunizationScheduleWebpart;
import org.labkey.test.components.studydesigner.VaccineDesignWebpart;
import org.labkey.test.pages.StartImportPage;
import org.labkey.test.pages.study.ManageDatasetQCStatesPage;
import org.labkey.test.pages.study.QCStateTableRow;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PermissionsHelper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category(Daily.class)
@BaseWebDriverTest.ClassTimeout(minutes = 9)
public class AdvancedImportOptionsTest extends BaseWebDriverTest
{
    private static final String LIMITED_USER = "limited@advancedimport.test";

    protected static final File IMPORT_STUDY_FILE = TestFileUtils.getSampleData("AdvancedImportOptions/AdvancedImportStudyProject01.folder.zip");
    private static final String IMPORT_PROJECT_FILE01 = "Advanced Import By File";
    private static final String IMPORT_PROJECT_FILE02 = "Advanced Import By File With Filters";
    private static final String IMPORT_PROJECT_FILE03 = "Advanced Import By Pipeline With Filters";

    protected static final String IMPORT_PROJECT_MULTI = "Advanced Import to Multiple Folders";
    private static final String IMPORT_FOLDER_MULTI01 = "Advance Import Folder 01";
    private static final String IMPORT_FOLDER_MULTI02 = "Advance Import Folder 02";
    private static final String IMPORT_FOLDER_MULTI03 = "Advance Import Folder 03";
    private static final String IMPORTED_SUB_FOLDER_NAME = "Advanced Import Subfolder";

    private static final int IMPORT_WAIT_TIME = 60 * 1000;  // This should be a limit of 1 minute.
    private static final boolean EXPECTED_IMPORT_ERRORS = false;
    private static final int EXPECTED_COMPLETED_IMPORT_JOBS = 1;
    private static final int EXPECTED_COMPLETED_MULTI_FOLDER_JOBS = 2;

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("study");
    }

    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        // Don't care about afterTest, always send false because I don't care (want to fail) if the project is not there.
        _containerHelper.deleteProject(IMPORT_PROJECT_FILE01, false);
        _containerHelper.deleteProject(IMPORT_PROJECT_FILE02, false);
        _containerHelper.deleteProject(IMPORT_PROJECT_FILE03, false);
        _containerHelper.deleteProject(IMPORT_PROJECT_MULTI, false);

        _userHelper.deleteUser(LIMITED_USER);
    }

    // This test class has no @Before or @BeforeClass. Each of the test cases, creates its own project to be used for importing.

    @Test
    public void testBasicImportFromFile()
    {
        File zipFile = IMPORT_STUDY_FILE;

        log("Create a new project to import the existing data.");
        _containerHelper.createProject(IMPORT_PROJECT_FILE01, "Study");

        log("Get to the import page and validate that is looks as expected.");
        StartImportPage importPage = StartImportPage.startImportFromFile(this, zipFile, true);
        importPage.setSelectSpecificImportOptions(true);
        assertTrue("The 'Select specific objects to import' is not visible, and it should be in this case.", importPage.isSelectSpecificImportOptionsVisible());

        log("Start the import");
        importPage.clickStartImport();

        waitForText("Data Pipeline");
        waitForPipelineJobsToComplete(EXPECTED_COMPLETED_IMPORT_JOBS, "Folder import", EXPECTED_IMPORT_ERRORS, IMPORT_WAIT_TIME);

        goToProjectHome(IMPORT_PROJECT_FILE01);
        validateFileImportResults();
    }

    private void validateFileImportResults()
    {
        log("Validate that the expected data has been imported.");
        log("Validate assay schedule.");
        clickTab("Assays");
        AssayScheduleWebpart assayScheduleWebpart = new AssayScheduleWebpart(getDriver());
        assertEquals(1, assayScheduleWebpart.getAssayRowCount());
        assertEquals("Name is Assay01", assayScheduleWebpart.getAssayCellDisplayValue("AssayName", 0));
        assertEquals("Assay01 Configuration", assayScheduleWebpart.getAssayCellDisplayValue("Description", 0));
        assertEquals("This is text in the assay plan (schedule).", assayScheduleWebpart.getAssayPlan());

        log("Validate Immunizations.");
        clickTab("Immunizations");
        ImmunizationScheduleWebpart immunizationScheduleWebpart = new ImmunizationScheduleWebpart(getDriver());
        assertEquals(1, immunizationScheduleWebpart.getCohortRowCount());
        assertEquals("Cohort01", immunizationScheduleWebpart.getCohortCellDisplayValue("Label", 0));
        assertEquals("5", immunizationScheduleWebpart.getCohortCellDisplayValue("SubjectCount", 0));
        assertEquals("Treatment01 ?", immunizationScheduleWebpart.getCohortCellDisplayValue("TimePoint01", 0));

        log("Validate Vaccine Design.");
        clickTab("Vaccine Design");
        VaccineDesignWebpart vaccineDesignWebpart = new VaccineDesignWebpart(getDriver());
        assertEquals(3, vaccineDesignWebpart.getImmunogenRowCount());
        assertEquals("Imm001", vaccineDesignWebpart.getImmunogenCellDisplayValue("Label", 0));
        assertEquals("immType02", vaccineDesignWebpart.getImmunogenCellDisplayValue("Type", 0));
        assertEquals("Imm003", vaccineDesignWebpart.getImmunogenCellDisplayValue("Label", 1));
        assertEquals("immType01", vaccineDesignWebpart.getImmunogenCellDisplayValue("Type", 1));
        assertEquals("Imm002", vaccineDesignWebpart.getImmunogenCellDisplayValue("Label", 2));
        assertEquals("immType02", vaccineDesignWebpart.getImmunogenCellDisplayValue("Type", 2));
        assertEquals(1, vaccineDesignWebpart.getAdjuvantRowCount());
        assertEquals("AdjLabel01", vaccineDesignWebpart.getAdjuvantCellDisplayValue("Label", 0));
    }

    @Test
    public void testFilteredImportFromFile()
    {
        File zipFile = IMPORT_STUDY_FILE;

        log("Create a new project to import the existing data.");
        _containerHelper.createProject(IMPORT_PROJECT_FILE02);

        log("Get to the import page and validate that is looks as expected.");
        StartImportPage importPage = StartImportPage.startImportFromFile(this, zipFile, true);
        importPage.setSelectSpecificImportOptions(true);
        assertTrue("The 'Select specific objects to import' is not visible, and it should be in this case.", importPage.isSelectSpecificImportOptionsVisible());

        boolean chkSet = false;
        Map<StartImportPage.AdvancedOptionsCheckBoxes, Boolean> myList = new HashMap<>();
        myList.put(StartImportPage.AdvancedOptionsCheckBoxes.AssaySchedule, chkSet);
        myList.put(StartImportPage.AdvancedOptionsCheckBoxes.CohortSettings, chkSet);
        myList.put(StartImportPage.AdvancedOptionsCheckBoxes.TreatmentData, chkSet);
        myList.put(StartImportPage.AdvancedOptionsCheckBoxes.WikisAndTheirAttachments, chkSet);

        log("Uncheck a few of the options.");
        importPage.setAdvancedOptionCheckBoxes(myList);

        log("Start the import");
        importPage.clickStartImport();

        waitForText("Data Pipeline");
        waitForPipelineJobsToComplete(EXPECTED_COMPLETED_IMPORT_JOBS, "Folder import", EXPECTED_IMPORT_ERRORS, IMPORT_WAIT_TIME);

        goToProjectHome(IMPORT_PROJECT_FILE02);

        log("Validate that the expected data has been imported.");

        log("Validate assay schedule.");
        clickTab("Assays");
        AssayScheduleWebpart assayScheduleWebpart = new AssayScheduleWebpart(getDriver());
        assertTrue(assayScheduleWebpart.isEmpty());

        log("Validate Immunizations.");
        clickTab("Immunizations");
        ImmunizationScheduleWebpart immunizationScheduleWebpart = new ImmunizationScheduleWebpart(getDriver());
        assertTrue(immunizationScheduleWebpart.isEmpty());

        log("Validate Vaccine Design.");
        clickTab("Vaccine Design");
        VaccineDesignWebpart vaccineDesignWebpart = new VaccineDesignWebpart(getDriver());
        assertTrue(vaccineDesignWebpart.isEmpty());

        log("Validate that Locations have been imported unchanged.");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Locations"));
        assertTextPresent("Jimmy Neutron Lab", "Dexter's Lab");

        log("Validate QC State.");
        ManageDatasetQCStatesPage manageDatasetQCStatesPage = goToManageStudy().manageDatasetQCStates();
        List<String> expectedStates = Arrays.asList("[none]", "QC State Name 01");
        List<String> states = manageDatasetQCStatesPage.getStateRows().stream().map(QCStateTableRow::getState).collect(Collectors.toList());
        assertEquals("Wrong QC states imported", expectedStates, states);

        log("Validate wiki content are not imported");
        goToProjectHome(IMPORT_PROJECT_FILE02);
        assertTextPresent("WikiPage01", "This folder does not currently contain any wiki pages to display.");
        assertElementNotPresent(Locator.xpath("//div[@class='labkey-wiki']//p[text() = 'This is a very basic wiki page.']"));

    }

    @Test
    public void testFilteredImportFromPipeline()
    {
        File zipFile = IMPORT_STUDY_FILE;

        log("Create a new project to import the existing data.");
        _containerHelper.createProject(IMPORT_PROJECT_FILE03);

        log("Get to the import page and validate that is looks as expected.");
        StartImportPage importPage = StartImportPage.startImportFromPipeline(this, zipFile, true, true);
        assertTrue("The 'Select specific objects to import' is not visible, and it should be in this case.", importPage.isSelectSpecificImportOptionsVisible());

        boolean chkSet = false;
        Map<StartImportPage.AdvancedOptionsCheckBoxes, Boolean> myList = new HashMap<>();
        myList.put(StartImportPage.AdvancedOptionsCheckBoxes.AssaySchedule, chkSet);
        myList.put(StartImportPage.AdvancedOptionsCheckBoxes.CohortSettings, chkSet);
        myList.put(StartImportPage.AdvancedOptionsCheckBoxes.TreatmentData, chkSet);
        myList.put(StartImportPage.AdvancedOptionsCheckBoxes.WikisAndTheirAttachments, chkSet);

        log("Uncheck a few of the options.");
        importPage.setAdvancedOptionCheckBoxes(myList);

        log("Start the import");
        importPage.clickStartImport();

        waitForText("Data Pipeline");
        waitForPipelineJobsToComplete(EXPECTED_COMPLETED_IMPORT_JOBS, "Folder import", EXPECTED_IMPORT_ERRORS, IMPORT_WAIT_TIME);

        goToProjectHome(IMPORT_PROJECT_FILE03);

        log("Validate that the expected data has been imported.");

        log("Validate assay schedule.");
        clickTab("Assays");
        AssayScheduleWebpart assayScheduleWebpart = new AssayScheduleWebpart(getDriver());
        assertTrue(assayScheduleWebpart.isEmpty());

        log("Validate Immunizations.");
        clickTab("Immunizations");
        ImmunizationScheduleWebpart immunizationScheduleWebpart = new ImmunizationScheduleWebpart(getDriver());
        assertTrue(immunizationScheduleWebpart.isEmpty());

        log("Validate Vaccine Design.");
        clickTab("Vaccine Design");
        VaccineDesignWebpart vaccineDesignWebpart = new VaccineDesignWebpart(getDriver());
        assertTrue(vaccineDesignWebpart.isEmpty());

        log("Validate that Locations have been imported unchanged.");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Locations"), WAIT_FOR_PAGE);
        assertTextPresent("Jimmy Neutron Lab", "Dexter's Lab");

        log("Validate QC State.");
        ManageDatasetQCStatesPage manageDatasetQCStatesPage = goToManageStudy().manageDatasetQCStates();
        List<String> expectedStates = Arrays.asList("[none]", "QC State Name 01");
        List<String> states = manageDatasetQCStatesPage.getStateRows().stream().map(QCStateTableRow::getState).collect(Collectors.toList());
        assertEquals("Wrong QC states imported", expectedStates, states);

        log("Validate wiki content are not imported");
        goToProjectHome(IMPORT_PROJECT_FILE03);
        assertTextPresent("WikiPage01", "This folder does not currently contain any wiki pages to display.");
        assertElementNotPresent(Locator.xpath("//div[@class='labkey-wiki']//p[text() = 'This is a very basic wiki page.']"));

    }

    @Test
    public void testImportToMultipleFolders()
    {
        Assume.assumeTrue("Issue 37413: Server becomes unresponsive after importing folder archive to multiple folders",
                WebTestHelper.getDatabaseType() != WebTestHelper.DatabaseType.MicrosoftSQLServer);

        File zipFile = IMPORT_STUDY_FILE;
        _userHelper.createUser(LIMITED_USER);

        log("Create a new project to import the existing data into multiple folders.");
        _containerHelper.createProject(IMPORT_PROJECT_MULTI);
        _containerHelper.enableModule(IMPORT_PROJECT_MULTI, "Specimen");

        log("Create subfolders and setup permissions.");
        _containerHelper.createSubfolder(IMPORT_PROJECT_MULTI, IMPORT_FOLDER_MULTI01);
        _containerHelper.createSubfolder(IMPORT_PROJECT_MULTI, IMPORT_FOLDER_MULTI02);
        _containerHelper.createSubfolder(IMPORT_PROJECT_MULTI, IMPORT_FOLDER_MULTI03);

        ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);

        log("Setting up permissions for a limited user");
        clickFolder(IMPORT_FOLDER_MULTI01);
        permissionsHelper.addMemberToRole(LIMITED_USER, "Folder Administrator", PermissionsHelper.MemberType.user);
        clickFolder(IMPORT_FOLDER_MULTI02);
        permissionsHelper.addMemberToRole(LIMITED_USER, "Reader", PermissionsHelper.MemberType.user);
        clickFolder(IMPORT_FOLDER_MULTI03);
        permissionsHelper.addMemberToRole(LIMITED_USER, "Folder Administrator", PermissionsHelper.MemberType.user);

        pushLocation();
        impersonate(LIMITED_USER);
        clickFolder(IMPORT_FOLDER_MULTI01);
        log("Get to the import page and validate that is looks as expected.");
        StartImportPage importPage = StartImportPage.startImportFromFile(this, zipFile, false);
        importPage.setSelectSpecificImportOptions(true);
        importPage.setApplyToMultipleFoldersCheckBox(true);

        assertTrue("The 'Select specific objects to import' is not visible, and it should be in this case.", importPage.isSelectSpecificImportOptionsVisible());

        log("Verify user can import only into folders they have admin access to");
        waitForElement(Locator.tagWithClass("span", "x4-tree-node-text").withText(IMPORT_FOLDER_MULTI01).notHidden(), 5000);
        assertElementNotPresent(Locator.tagWithClass("span", "x4-tree-node-text").withText(IMPORT_PROJECT_MULTI));
        assertElementNotPresent(Locator.tagWithClass("span", "x4-tree-node-text").withText(IMPORT_FOLDER_MULTI02));

        Locator.tagWithClass("span", "x4-tree-node-text").withText(IMPORT_FOLDER_MULTI01).waitForElement(new WebDriverWait(getDriver(), 5)).click();
        Locator.tagWithClass("span", "x4-tree-node-text").withText(IMPORT_FOLDER_MULTI03).waitForElement(new WebDriverWait(getDriver(), 5)).click();

        stopImpersonating();
        popLocation();

        clickFolder(IMPORT_FOLDER_MULTI01);
        log("Import into multiple folders from the same template");
        importPage = StartImportPage.startImportFromPipeline(this, zipFile, true, true);
        importPage.setSelectSpecificImportOptions(true);
        importPage.setAdvancedOptionCheckBoxes(Map.of(
                StartImportPage.AdvancedOptionsCheckBoxes.DatasetData, false,
                StartImportPage.AdvancedOptionsCheckBoxes.DatasetDefinitions, false,
                StartImportPage.AdvancedOptionsCheckBoxes.Specimens, false,
                StartImportPage.AdvancedOptionsCheckBoxes.SpecimenSettings, false));
        importPage.setApplyToMultipleFoldersCheckBox(true);

        assertTrue("The 'Select specific objects to import' is not visible, and it should be in this case.", importPage.isSelectSpecificImportOptionsVisible());
        Locator.tagWithClass("span", "x4-tree-node-text").notHidden().withText(IMPORT_FOLDER_MULTI01).waitForElement(shortWait());

        log("Select sub folders to import into");
        /* new UI behavior: clicking on the text doesn't toggle the box, but clicking the checkbox does.
         * Also, using a checkbox to wrap this has issues; checkBox.get() always returns false, so
         * set(true) unchecks the box.  Todo: salvage this logic into a component someplace.
          * Todo: wonder if being already-selected here is a problem.*/
        WebElement checkbox01 = Locator.tagWithClass("span", "x4-tree-node-text").withText(IMPORT_FOLDER_MULTI01)
                .precedingSibling("input").waitForElement(new WebDriverWait(getDriver(), 5));
        if (checkbox01.getAttribute("aria-checked") == null || !checkbox01.getAttribute("aria-checked").equals("true"))
            checkbox01.click();
        sleep(250);
        WebElement checkBox03 = Locator.tagWithClass("span", "x4-tree-node-text").withText(IMPORT_FOLDER_MULTI03)
                .precedingSibling("input").waitForElement(new WebDriverWait(getDriver(), 5));
        if (checkBox03.getAttribute("aria-checked") == null || !checkBox03.getAttribute("aria-checked").equals("true"))
            checkBox03.click();
        sleep(250);

        log("Start the import and verify the confirmation dialog");
        importPage.clickStartImport("The import archive will be applied to 2 selected target folders. A separate pipeline import job will be created for each. This action cannot be undone.\n\nWould you like to proceed?");

        waitForText("Data Pipeline");
        log("Verify the container filter has been set to see multiple import jobs");

        // if the container filter has been set correctly we should see all 2 pipeline jobs
        waitForPipelineJobsToComplete(EXPECTED_COMPLETED_MULTI_FOLDER_JOBS, "Folder import", EXPECTED_IMPORT_ERRORS, IMPORT_WAIT_TIME * EXPECTED_COMPLETED_IMPORT_JOBS);

        log("Validate that the expected data has been imported.");
        clickFolder(IMPORT_FOLDER_MULTI01);
        validateMultiFolderImportResults(IMPORT_FOLDER_MULTI01, false, false, false);

        clickFolder(IMPORT_FOLDER_MULTI03);
        validateMultiFolderImportResults(IMPORT_FOLDER_MULTI03, false, false, false);

        clickFolder(IMPORT_FOLDER_MULTI02);
        log("Import into a single folder");
        importPage = StartImportPage.startImportFromPipeline(this, zipFile, false, true);
        importPage.setSelectSpecificImportOptions(true);
        importPage.clickStartImport();
        waitForText("Data Pipeline");

        waitForPipelineJobsToComplete(EXPECTED_COMPLETED_IMPORT_JOBS, "Folder import", EXPECTED_IMPORT_ERRORS, IMPORT_WAIT_TIME);

        log("Validate that the expected data has been imported and the subfolder has been imported.");
        clickFolder(IMPORT_FOLDER_MULTI02);
        validateMultiFolderImportResults(IMPORT_FOLDER_MULTI02, true, true, true);
    }

    public void validateMultiFolderImportResults(String folderName, boolean hasDatasets, boolean hasSpecimens, boolean hasSubfolder)
    {
        validateFileImportResults();

        clickTab("Overview");
        if (hasDatasets)
        {
            log("Validate that datasets have been imported");
            waitForElement(Locator.tagWithAttribute("div", "data-qtip", "NAbTest"));
            waitForElement(Locator.tagWithAttribute("div", "data-qtip", "FlowTest"));
            waitForElement(Locator.tagWithAttribute("div", "data-qtip", "LuminexTest"));
            waitForElement(Locator.tagWithAttribute("div", "data-qtip", "ELISATest"));
        }
        else
        {
            log("Validate that no datasets have been imported");
            waitForElement(Locator.tagWithAttribute("div", "data-qtip", "5001"));
            waitForElement(Locator.tagWithAttribute("div", "data-qtip", "5002"));
            waitForElement(Locator.tagWithAttribute("div", "data-qtip", "5003"));
            waitForElement(Locator.tagWithAttribute("div", "data-qtip", "5004"));
        }

        log("Verify specimen import status");
        clickTab("Manage");
        waitForText(hasSpecimens ? "This study uses the advanced specimen repository" : "This study uses the standard specimen repository");

        log("Verify whether a subfolder should be present");
        WebElement folderTree = projectMenu().expandFolderFully(getCurrentProject(), folderName);
        if (hasSubfolder)
            assertTrue("project menu should have link for subfolder:" + IMPORTED_SUB_FOLDER_NAME, Locator.linkWithText(IMPORTED_SUB_FOLDER_NAME).existsIn(folderTree));
        else
            assertFalse("project menu should not have link for subfolder:" + IMPORTED_SUB_FOLDER_NAME, Locator.linkWithText(IMPORTED_SUB_FOLDER_NAME).existsIn(folderTree));
    }
}
