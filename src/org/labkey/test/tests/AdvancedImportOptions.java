package org.labkey.test.tests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.StartImportPage;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category(DailyB.class)
public class AdvancedImportOptions extends BaseWebDriverTest
{

    private static final String IMPORT_STUDY_FILE = "/sampledata/AdvancedImportOptions/AdvancedImportStudyProject01.folder.zip";
    private static final String IMPORT_PROJECT_FILE01 = "Advanced Import By File";
    private static final String IMPORT_PROJECT_FILE02 = "Advanced Import By File With Filters";
    private static final String IMPORT_PROJECT_FILE03 = "Advanced Import By Pipeline With Filters";
    private static final int IMPORT_WAIT_TIME = 60 * 1000;  // This should be a limit of 1 minute.
    private static final boolean EXPECTED_IMPORT_ERRORS = false;
    private static final int EXPECTED_COMPLETED_IMPORT_JOBS = 1;

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("study");
    }

    @Override
    protected String getProjectName()
    {
        return "AdvancedImportOptions";
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
    }

    // This test class has no @Before or @BeforeClass. Each of the test cases, creates it's own project to be used for importing.

    @Test
    public void testBasicImportFromFile()
    {
        File zipFile  = new File(TestFileUtils.getLabKeyRoot() + IMPORT_STUDY_FILE);

        log("Create a new project to import the existing data.");
        _containerHelper.createProject(IMPORT_PROJECT_FILE01, "Study");

        log("Get to the import page and validate that is looks as expected.");
        StartImportPage importPage = StartImportPage.startImportFromFile(this, zipFile, true, true);
        Assert.assertTrue("The 'Advanced Import Panel' is not visible by default, and it should be in this case.", importPage.isAdvancedImportOptionsVisible());

        log("Start the import");
        importPage.clickStartImport();

        waitForText("Data Pipeline");
        waitForPipelineJobsToComplete(EXPECTED_COMPLETED_IMPORT_JOBS, "Folder import", EXPECTED_IMPORT_ERRORS, IMPORT_WAIT_TIME);

        goToProjectHome(IMPORT_PROJECT_FILE01);

        log("Validate that the expected data has been imported.");
        log("Validate assay schedule.");
        clickTab("Assays");
        assertTextPresent("To change the set of assays and edit the assay schedule, click the edit button below.");

        log("Validate Immunizations.");
        clickTab("Immunizations");
        assertTextPresent("Cohort01", "Treatment01");

        log("Validate Vaccine Design.");
        clickTab("Vaccine Design");
        assertTextPresent("Imm001", "immType02", "AdjLabel01");

        log("Cleanup and remove the project.");
        _containerHelper.deleteProject(IMPORT_PROJECT_FILE01);

    }

    @Test
    public void testFilteredImportFromFile()
    {
        File zipFile  = new File(TestFileUtils.getLabKeyRoot() + IMPORT_STUDY_FILE);

        log("Create a new project to import the existing data.");
        _containerHelper.createProject(IMPORT_PROJECT_FILE02, "Study");

        log("Get to the import page and validate that is looks as expected.");
        StartImportPage importPage = StartImportPage.startImportFromFile(this, zipFile, true, true);
        Assert.assertTrue("The 'Advanced Import Panel' is not visible by default, and it should be in this case.", importPage.isAdvancedImportOptionsVisible());

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
        assertTextPresent("No assays have been scheduled.");

        log("Validate Immunizations.");
        clickTab("Immunizations");
        assertTextPresent("No cohort/treatment/timepoint mappings have been defined.");

        log("Validate Vaccine Design.");
        clickTab("Vaccine Design");
        assertTextPresent("No immunogens have been defined.");

        log("Validate that Locations have been imported unchanged.");
        clickTab("Manage");
        click(Locator.linkWithText("Manage Locations"));
        assertTextPresent("Jimmy Neutron Lab", "Dexter's Lab");

        log("Validate QC State.");
        clickTab("Manage");
        click(Locator.linkWithText("Manage Dataset QC States"));
        assertFormElementEquals(Locator.css("td:nth-child(2) > input[type=\"text\"]:nth-child(2)"), "QC State Name 01");

        log("Validate wiki content are not imported");
        goToProjectHome(IMPORT_PROJECT_FILE02);
        assertTextPresent("WikiPage01", "This folder does not currently contain any wiki pages to display.");
        assertElementNotPresent(Locator.xpath("//div[@class='labkey-wiki']//p[text() = 'This is a very basic wiki page.']"));

        log("Cleanup and remove the project.");
        _containerHelper.deleteProject(IMPORT_PROJECT_FILE02);

    }

    @Test
    public void testFilteredImportFromPipeline()
    {
        File zipFile  = new File(TestFileUtils.getLabKeyRoot() + IMPORT_STUDY_FILE);

        log("Create a new project to import the existing data.");
        _containerHelper.createProject(IMPORT_PROJECT_FILE03, "Study");

        log("Get to the import page and validate that is looks as expected.");
        StartImportPage importPage = StartImportPage.startImportFromPipeline(this, zipFile, true, true);
        Assert.assertTrue("The 'Advanced Import Panel' is not visible by default, and it should be in this case.", importPage.isAdvancedImportOptionsVisible());

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
        assertTextPresent("No assays have been scheduled.");

        log("Validate Immunizations.");
        clickTab("Immunizations");
        assertTextPresent("No cohort/treatment/timepoint mappings have been defined.");

        log("Validate Vaccine Design.");
        clickTab("Vaccine Design");
        assertTextPresent("No immunogens have been defined.");

        log("Validate that Locations have been imported unchanged.");
        clickTab("Manage");
        click(Locator.linkWithText("Manage Locations"));
        assertTextPresent("Jimmy Neutron Lab", "Dexter's Lab");

        log("Validate QC State.");
        clickTab("Manage");
        click(Locator.linkWithText("Manage Dataset QC States"));
        assertFormElementEquals(Locator.css("td:nth-child(2) > input[type=\"text\"]:nth-child(2)"), "QC State Name 01");

        log("Validate wiki content are not imported");
        goToProjectHome(IMPORT_PROJECT_FILE03);
        assertTextPresent("WikiPage01", "This folder does not currently contain any wiki pages to display.");
        assertElementNotPresent(Locator.xpath("//div[@class='labkey-wiki']//p[text() = 'This is a very basic wiki page.']"));

        log("Cleanup and remove the project.");
        _containerHelper.deleteProject(IMPORT_PROJECT_FILE03);

    }

}
