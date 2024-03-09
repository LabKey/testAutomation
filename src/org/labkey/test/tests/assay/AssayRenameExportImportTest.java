package org.labkey.test.tests.assay;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegion;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExcelHelper;
import org.labkey.test.util.core.webdav.WebDavUploadHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.labkey.test.util.AbstractDataRegionExportOrSignHelper.XarLsidOutputType.PARTIAL_FOLDER_RELATIVE;

/**
 * <p>
 *     These tests adds some coverage for code that was changed as part of the assay rename work.
 * </p>
 * <p>
 *     One tests will verify that an assay can be created after a folder import with an renamed assay.
 *     The other test will verify that an assay can be created successfully after importing from xar, with different names.
 *     This scenario is needed because the rename work changed how unique lsid is constructed. It will check that the
 *     DBsequence used for xar import doesn't collide with those created from project manually
 * </p>
 */
@Category({Assays.class, Daily.class})
public class AssayRenameExportImportTest extends BaseWebDriverTest
{

    private static final String ORIGINAL_PROJECT = "Assay_Rename_Folder_Import_Test";
    private static final String SECOND_PROJECT = "Assay_Rename_Folder_Import_Test_Copy";
    private static final String ASSAY_XAR_ORIGINAL_PROJECT = "Assay_Rename_XAR_Import_Test";
    private static final String ASSAY_XAR_SECOND_PROJECT = "Assay_Rename_XAR_Import_Test_Copy";

    private static final File FOLDER_EXPORT_RUN = TestFileUtils.getSampleData("GPAT/renameAssayTrialFolderExport.xls");
    private static final File ASSAY_EXPORT_RUN_01 = TestFileUtils.getSampleData("GPAT/renameAssayTrialAssayExport01.xls");
    private static final File ASSAY_EXPORT_RUN_02 = TestFileUtils.getSampleData("GPAT/renameAssayTrialAssayExport02.xls");

    private static final Date runDateValue = new Calendar.Builder()
            .setDate(2024, 1, 29)
            .setTimeOfDay(10, 45, 0)
            .build().getTime();

    private SimpleDateFormat _defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat _defaultTimeFormat = new SimpleDateFormat("HH:mm");
    private SimpleDateFormat _defaultDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @BeforeClass
    public static void doSetup()
    {
        AssayRenameExportImportTest init = (AssayRenameExportImportTest) getCurrentTest();

        init._containerHelper.createProject(ORIGINAL_PROJECT, "Assay");
        init._containerHelper.createProject(SECOND_PROJECT, "Assay");
        init._containerHelper.createProject(ASSAY_XAR_ORIGINAL_PROJECT, "Assay");
        init._containerHelper.createProject(ASSAY_XAR_SECOND_PROJECT, "Assay");

        init.goToProjectHome();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("assay");
    }

    @Override
    protected String getProjectName()
    {
        return ORIGINAL_PROJECT;
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(ORIGINAL_PROJECT, afterTest);
        _containerHelper.deleteProject(SECOND_PROJECT, afterTest);
        _containerHelper.deleteProject(ASSAY_XAR_ORIGINAL_PROJECT, afterTest);
        _containerHelper.deleteProject(ASSAY_XAR_SECOND_PROJECT, afterTest);
    }

    @Test
    public void testProjectImport() throws IOException
    {

        goToProjectHome(ORIGINAL_PROJECT);

        log("Create a gPat assay with some run data.");
        String originalAssayName = "This Assay Will Be Renamed";
        createGpatAssayAndRun(ORIGINAL_PROJECT, originalAssayName, FOLDER_EXPORT_RUN);

        String updatedAssayName = "A Brand New Name";
        log(String.format("Edit the assay design and rename it to '%s'.", updatedAssayName));

        ReactAssayDesignerPage assayDesignerPage = _assayHelper.clickEditAssayDesign(false);
        checker().fatal()
                .verifyTrue("The 'Name' field should be enabled and editable. Fatal error.",
                        assayDesignerPage.isNameEnabled());

        assayDesignerPage.setName(updatedAssayName);
        assayDesignerPage.clickFinish();

        log("Export the project folder.");
        File exportedProject = exportFolderAsZip(ORIGINAL_PROJECT, false, false, false, true);

        log("Import the folder archive into a new project.");
        goToProjectHome(SECOND_PROJECT);
        importFolderFromZip(exportedProject, false, 1);

        log("Validated that the renamed assay is as expected in the new project.");

        beginAt(WebTestHelper.buildURL("assay", SECOND_PROJECT, "begin"));

        DataRegionTable table;

        if(checker().verifyTrue(String.format("Link to updated assay design name '%s' not present in second project.", updatedAssayName),
                Locator.linkWithText(updatedAssayName).isDisplayed(getDriver())))
        {
            clickAndWait(Locator.linkWithText(updatedAssayName));

            table = new DataRegionTable("Runs", getDriver());

            checker().verifyEquals("Run Date is not as expected.",
                    _defaultDateFormat.format(runDateValue), table.getDataAsText(0, "Run Date"));

            checker().verifyEquals("Run Time is not as expected.",
                    _defaultTimeFormat.format(runDateValue), table.getDataAsText(0, "Run Time"));

            checker().verifyEquals("Run Date Time is not as expected.",
                    _defaultDateTimeFormat.format(runDateValue), table.getDataAsText(0, "Run Date Time"));

            checker().screenShotIfNewError("Run_Field_Error");

            if(checker().verifyTrue(String.format("Link to updated assay design name '%s' not present in second project.", updatedAssayName),
                    Locator.linkWithText(FOLDER_EXPORT_RUN.getName()).isDisplayed(getDriver())))
            {
                clickAndWait(Locator.linkWithText(FOLDER_EXPORT_RUN.getName()));
                checker().verifyTrue(String.format("Run data for assay '%s' not as expected in the second folder.", updatedAssayName),
                        validateRunDataLoaded());
            }

        }

        checker().screenShotIfNewError("Imported_Project_Folder_Assay_Error");

        log("Validate that a new assay can be created in the second folder.");
        String secondAssay = "Assay Created In Copied Project";
        createGpatAssayAndRun(SECOND_PROJECT, secondAssay, FOLDER_EXPORT_RUN);

        log("Export runs table to validate date and time fields.");

        goToProjectHome(ORIGINAL_PROJECT);

        clickAndWait(Locator.linkWithText(updatedAssayName));

        table = new DataRegionTable("Runs", getDriver());

        DataRegionExportHelper exportHelper = new DataRegionExportHelper(table);

        table.checkAllOnPage();

        File exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLSX);

        try(Workbook workbook = ExcelHelper.create(exportedFile))
        {
            Sheet sheet = workbook.getSheetAt(workbook.getActiveSheetIndex());

            Date excelDate = new Calendar.Builder()
                    .setDate(2024, 1, 29)
                    .setTimeOfDay(0, 0, 0)
                    .build().getTime();

            checker().verifyEquals("Run Date is not as expected.",
                    excelDate, sheet.getRow(1).getCell(6).getDateCellValue());

            Date excelTime = new Calendar.Builder()
                    .setDate(1970, 0, 1)
                    .setTimeOfDay(10, 45, 0)
                    .build().getTime();

            checker().verifyEquals("Run Time is not as expected.",
                    excelTime, sheet.getRow(1).getCell(7).getDateCellValue());

            checker().verifyEquals("Run Date Time is not as expected.",
                    runDateValue, sheet.getRow(1).getCell(8).getDateCellValue());

        }

    }

    @Test
    public void testAssayRunImport()
    {

        goToProjectHome(ASSAY_XAR_ORIGINAL_PROJECT);

        log("Create a gPat assay with some run data.");
        String originalAssayName = "Rename and Export Runs";

        createGpatAssayAndRun(ORIGINAL_PROJECT, originalAssayName, ASSAY_EXPORT_RUN_01);

        goToManageAssays();

        waitAndClickAndWait(Locator.linkWithText(originalAssayName));

        log("Update the name of the assay.");
        String updatedAssayName = "Rename and Export Runs Updated";

        ReactAssayDesignerPage assayDesignerPage = _assayHelper.clickEditAssayDesign(false);
        checker().fatal()
                .verifyTrue("The 'Name' field should be enabled and editable. Fatal error.",
                        assayDesignerPage.isNameEnabled());

        assayDesignerPage.setName(updatedAssayName);
        assayDesignerPage.clickFinish();

        log("Now export the run.");

        goToManageAssays();

        waitAndClickAndWait(Locator.linkWithText(updatedAssayName));
        DataRegionTable runsTable = DataRegionTable.DataRegion(getDriver()).find();
        runsTable.checkAllOnPage();
        DataRegionExportHelper exportPanel = runsTable.expandExportPanel();
        File partialRelativeXarFile = exportPanel.exportXar(PARTIAL_FOLDER_RELATIVE, "partialRelative.xar");

        log("Import runs (partial relative LSID) into a new project");
        goToProjectHome(ASSAY_XAR_SECOND_PROJECT);
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(partialRelativeXarFile);
        _fileBrowserHelper.importFile(partialRelativeXarFile.getName(), "Import Experiment");
        goToDataPipeline();
        waitForPipelineJobsToComplete(1, false);

        goToManageAssays();

        checker().verifyTrue(String.format("Link to updated assay design name '%s' not present after importing runs xar.", updatedAssayName),
                Locator.linkWithText(updatedAssayName).isDisplayed(getDriver()));

        log("Do a sanity check that an assay can be created after a xar import for a renamed assay.");
        String sanityAssayName = "Sanity Assay";

        createGpatAssayAndRun(ORIGINAL_PROJECT, sanityAssayName, ASSAY_EXPORT_RUN_02);

        goToManageAssays();

        checker().verifyTrue(String.format("Link to sanity assay design name '%s' not present.", sanityAssayName),
                Locator.linkWithText(sanityAssayName).isDisplayed(getDriver()));

    }

    private void createGpatAssayAndRun(String projectName, String assayName, File runFile)
    {
        new WebDavUploadHelper(projectName).uploadFile(runFile);
        beginAt(WebTestHelper.buildURL("pipeline", projectName, "browse"));
        _fileBrowserHelper.importFile(runFile.getName(), "Create New Standard Assay Design");

        ReactAssayDesignerPage assayDesignerPage = new ReactAssayDesignerPage(getDriver());

        assayDesignerPage.setName(assayName);

        DomainFormPanel domainFormPanel = assayDesignerPage.expandFieldsPanel("Results Fields");

        checker().fatal().verifyEquals(
                "Results fields count not as expected", "10 Fields Defined",
                domainFormPanel.getFieldCountMessage());

        domainFormPanel.getField("Date").setType(FieldDefinition.ColumnType.Date);
        domainFormPanel.getField("Time").setType(FieldDefinition.ColumnType.Time);

        domainFormPanel = assayDesignerPage.expandFieldsPanel("Run Fields");
        domainFormPanel.addField(new FieldDefinition("RunDate", FieldDefinition.ColumnType.Date));
        domainFormPanel.addField(new FieldDefinition("RunTime", FieldDefinition.ColumnType.Time));
        domainFormPanel.addField(new FieldDefinition("RunDateTime", FieldDefinition.ColumnType.DateAndTime));

        assayDesignerPage.clickFinish();

        waitAndClick(Locator.lkButton("Next"));

        WebElement runPropertiesPanel = Locator.tagWithAttributeContaining("form", "lk-region-form", "Runs")
                .findElement(getDriver());

        setFormElement(Locator.name("runDate").findElement(runPropertiesPanel),
                _defaultDateFormat.format(runDateValue));

        setFormElement(Locator.name("runTime").findElement(runPropertiesPanel),
                _defaultTimeFormat.format(runDateValue));

        setFormElement(Locator.name("runDateTime").findElement(runPropertiesPanel),
                _defaultDateTimeFormat.format(runDateValue));

        waitAndClick(Locator.lkButton("Save and Finish"));
        waitAndClick(Locator.linkWithText(runFile.getName()));

        checker().fatal().verifyTrue("Run data not loaded. Fatal error.",
                validateRunDataLoaded());

    }

    private boolean validateRunDataLoaded()
    {
        // Waiting for record count will validate that the import was successful.
        boolean result = waitFor(()->Locator.css(".labkey-pagination")
                        .containing("1 - 25 of 25")
                        .isDisplayed(getDriver()),
                5_000);

        // Filter the date-only and time-only fields to validate that the data is there.
        DataRegionTable table = new DataRegionTable("Data", getDriver());
        table.setFilter("Date", "Is Less Than", "2010-01-01");

        result = result && waitFor(()->Locator.css(".labkey-pagination")
                        .containing("1 - 19 of 19")
                        .isDisplayed(getDriver()),
                1_000);

        table.clearFilter("Date");

        table.setFilter("Time", "Is Greater Than", "16:00");

        result = result && waitFor(()->Locator.css(".labkey-pagination")
                        .containing("1 - 8 of 8")
                        .isDisplayed(getDriver()),
                1_000);

        table.clearFilter("Time");

        return result;
    }

}
