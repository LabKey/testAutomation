package org.labkey.test.tests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.components.studydesigner.AssayScheduleWebpart;
import org.labkey.test.components.studydesigner.ImmunizationScheduleWebpart;
import org.labkey.test.components.studydesigner.VaccineDesignWebpart;
import org.labkey.test.pages.StartImportPage;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category(DailyB.class)
public class AdvancedImportOptionsTest extends BaseWebDriverTest
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

    // This test class has no @Before or @BeforeClass. Each of the test cases, creates its own project to be used for importing.

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
        AssayScheduleWebpart assayScheduleWebpart = new AssayScheduleWebpart(getDriver());
        Assert.assertEquals(1, assayScheduleWebpart.getAssayRowCount());
        Assert.assertEquals("Name is Assay01", assayScheduleWebpart.getAssayCellDisplayValue("AssayName", 0));
        Assert.assertEquals("Assay01 Configuration", assayScheduleWebpart.getAssayCellDisplayValue("Description", 0));
        Assert.assertEquals("This is text in the assay plan (schedule).", assayScheduleWebpart.getAssayPlan());

        log("Validate Immunizations.");
        clickTab("Immunizations");
        ImmunizationScheduleWebpart immunizationScheduleWebpart = new ImmunizationScheduleWebpart(getDriver());
        Assert.assertEquals(1, immunizationScheduleWebpart.getCohortRowCount());
        Assert.assertEquals("Cohort01", immunizationScheduleWebpart.getCohortCellDisplayValue("Label", 0));
        Assert.assertEquals("5", immunizationScheduleWebpart.getCohortCellDisplayValue("SubjectCount", 0));
        Assert.assertEquals("Treatment01 ?", immunizationScheduleWebpart.getCohortCellDisplayValue("TimePoint01", 0));

        log("Validate Vaccine Design.");
        clickTab("Vaccine Design");
        VaccineDesignWebpart vaccineDesignWebpart = new VaccineDesignWebpart(getDriver());
        Assert.assertEquals(3, vaccineDesignWebpart.getImmunogenRowCount());
        Assert.assertEquals("Imm001", vaccineDesignWebpart.getImmunogenCellDisplayValue("Label", 0));
        Assert.assertEquals("immType02", vaccineDesignWebpart.getImmunogenCellDisplayValue("Type", 0));
        Assert.assertEquals("Imm003", vaccineDesignWebpart.getImmunogenCellDisplayValue("Label", 1));
        Assert.assertEquals("immType01", vaccineDesignWebpart.getImmunogenCellDisplayValue("Type", 1));
        Assert.assertEquals("Imm002", vaccineDesignWebpart.getImmunogenCellDisplayValue("Label", 2));
        Assert.assertEquals("immType02", vaccineDesignWebpart.getImmunogenCellDisplayValue("Type", 2));
        Assert.assertEquals(1, vaccineDesignWebpart.getAdjuvantRowCount());
        Assert.assertEquals("AdjLabel01", vaccineDesignWebpart.getAdjuvantCellDisplayValue("Label", 0));

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
        AssayScheduleWebpart assayScheduleWebpart = new AssayScheduleWebpart(getDriver());
        Assert.assertTrue(assayScheduleWebpart.isEmpty());

        log("Validate Immunizations.");
        clickTab("Immunizations");
        ImmunizationScheduleWebpart immunizationScheduleWebpart = new ImmunizationScheduleWebpart(getDriver());
        Assert.assertTrue(immunizationScheduleWebpart.isEmpty());

        log("Validate Vaccine Design.");
        clickTab("Vaccine Design");
        VaccineDesignWebpart vaccineDesignWebpart = new VaccineDesignWebpart(getDriver());
        Assert.assertTrue(vaccineDesignWebpart.isEmpty());

        log("Validate that Locations have been imported unchanged.");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Locations"));
        assertTextPresent("Jimmy Neutron Lab", "Dexter's Lab");

        log("Validate QC State.");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Dataset QC States"));
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
        AssayScheduleWebpart assayScheduleWebpart = new AssayScheduleWebpart(getDriver());
        Assert.assertTrue(assayScheduleWebpart.isEmpty());

        log("Validate Immunizations.");
        clickTab("Immunizations");
        ImmunizationScheduleWebpart immunizationScheduleWebpart = new ImmunizationScheduleWebpart(getDriver());
        Assert.assertTrue(immunizationScheduleWebpart.isEmpty());

        log("Validate Vaccine Design.");
        clickTab("Vaccine Design");
        VaccineDesignWebpart vaccineDesignWebpart = new VaccineDesignWebpart(getDriver());
        Assert.assertTrue(vaccineDesignWebpart.isEmpty());

        log("Validate that Locations have been imported unchanged.");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Locations"), WAIT_FOR_PAGE);
        assertTextPresent("Jimmy Neutron Lab", "Dexter's Lab");

        log("Validate QC State.");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Dataset QC States"), WAIT_FOR_PAGE);
        assertFormElementEquals(Locator.css("td:nth-child(2) > input[type=\"text\"]:nth-child(2)"), "QC State Name 01");

        log("Validate wiki content are not imported");
        goToProjectHome(IMPORT_PROJECT_FILE03);
        assertTextPresent("WikiPage01", "This folder does not currently contain any wiki pages to display.");
        assertElementNotPresent(Locator.xpath("//div[@class='labkey-wiki']//p[text() = 'This is a very basic wiki page.']"));

        log("Cleanup and remove the project.");
        _containerHelper.deleteProject(IMPORT_PROJECT_FILE03);
    }
}
