package org.labkey.test.tests.assay;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.admin.ExportFolderPage;
import org.labkey.test.tests.MissingValueIndicatorsTest;
import org.labkey.test.util.LogMethod;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({DailyB.class, Assays.class})
@BaseWebDriverTest.ClassTimeout(minutes = 10)
public class AssayMissingValuesTest extends MissingValueIndicatorsTest
{
    private static final String ASSAY_NAME = "MVAssay";
    private static final String IMPORT_PROJECT = "MVAssay Import Project";

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        _containerHelper.deleteProject(IMPORT_PROJECT, false);
    }

    @BeforeClass
    public static void beforeTestClass()
    {
        AssayMissingValuesTest init = (AssayMissingValuesTest)getCurrentTest();

        init.setupProject();
    }

    @LogMethod
    protected void setupProject()
    {
        _containerHelper.createProject(getProjectName(), "Assay");
        defineAssay(ASSAY_NAME);
        setupMVIndicators();
    }

    @Test
    public void testAssayMV()
    {
        final String ASSAY_RUN_SINGLE_COLUMN = "MVAssayRunSingleColumn";
        final String ASSAY_RUN_TWO_COLUMN = "MVAssayRunTwoColumn";
        final String ASSAY_EXCEL_RUN_SINGLE_COLUMN = "MVAssayExcelRunSingleColumn";
        final String ASSAY_EXCEL_RUN_TWO_COLUMN = "MVAssayExcelRunTwoColumn";
        final String TEST_DATA_SINGLE_COLUMN_ASSAY =
                "SpecimenID\tParticipantID\tVisitID\tDate\tage\tsex\n" +
                        "1\tTed\t1\t01-Jan-09\tN\tmale\n" +
                        "2\tAlice\t1\t01-Jan-09\t17\tfemale\n" +
                        "3\tBob\t1\t01-Jan-09\tQ\tN";
        final String TEST_DATA_TWO_COLUMN_ASSAY =
                "SpecimenID\tParticipantID\tVisitID\tDate\tage\tageMVIndicator\tsex\tsexMVIndicator\n" +
                        "1\tFranny\t1\t01-Jan-09\t\tN\tmale\t\n" +
                        "2\tZoe\t1\t01-Jan-09\t25\tQ\tfemale\t\n" +
                        "3\tJ.D.\t1\t01-Jan-09\t50\t\tmale\tQ";
        final String TEST_DATA_SINGLE_COLUMN_ASSAY_BAD =
                "SpecimenID\tParticipantID\tVisitID\tDate\tage\tsex\n" +
                        "1\tTed\t1\t01-Jan-09\t.N\tmale\n" +
                        "2\tAlice\t1\t01-Jan-09\t17\tfemale\n" +
                        "3\tBob\t1\t01-Jan-09\tQ\tN";
        final String TEST_DATA_TWO_COLUMN_ASSAY_BAD =
                "SpecimenID\tParticipantID\tVisitID\tDate\tage\tageMVIndicator\tsex\tsexMVIndicator\n" +
                        "1\tFranny\t1\t01-Jan-09\t\tN\tmale\t\n" +
                        "2\tZoe\t1\t01-Jan-09\t25\tQ\tfemale\t\n" +
                        "3\tJ.D.\t1\t01-Jan-09\t50\t\tmale\t.Q";
        final File ASSAY_SINGLE_COLUMN_EXCEL_FILE = TestFileUtils.getSampleData("mvIndicators/assay_single_column.xls");
        final File ASSAY_TWO_COLUMN_EXCEL_FILE = TestFileUtils.getSampleData("mvIndicators/assay_two_column.xls");
        final File ASSAY_SINGLE_COLUMN_EXCEL_FILE_BAD = TestFileUtils.getSampleData("mvIndicators/assay_single_column_bad.xls");
        final File ASSAY_TWO_COLUMN_EXCEL_FILE_BAD = TestFileUtils.getSampleData("mvIndicators/assay_two_column_bad.xls");

        log("Import single column MV data");
        goToProjectHome();
        waitAndClickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.name("name"), ASSAY_RUN_SINGLE_COLUMN);
        click(Locator.xpath("//input[@value='textAreaDataProvider']"));

        setFormElement(Locator.name("TextAreaDataCollector.textArea"), TEST_DATA_SINGLE_COLUMN_ASSAY_BAD);
        clickButton("Save and Finish");
        assertLabKeyErrorPresent();

        click(Locator.xpath("//input[@value='textAreaDataProvider']"));
        setFormElement(Locator.name("TextAreaDataCollector.textArea"), TEST_DATA_SINGLE_COLUMN_ASSAY);
        clickButton("Save and Finish");
        assertNoLabKeyErrors();
        clickAndWait(Locator.linkWithText(ASSAY_RUN_SINGLE_COLUMN));
        validateSingleColumnData();

        log("Import two column MV data");
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.name("name"), ASSAY_RUN_TWO_COLUMN);

        click(Locator.xpath("//input[@value='textAreaDataProvider']"));
        setFormElement(Locator.name("TextAreaDataCollector.textArea"), TEST_DATA_TWO_COLUMN_ASSAY_BAD);
        clickButton("Save and Finish");
        assertLabKeyErrorPresent();

        click(Locator.xpath("//input[@value='textAreaDataProvider']"));
        setFormElement(Locator.name("TextAreaDataCollector.textArea"), TEST_DATA_TWO_COLUMN_ASSAY);
        clickButton("Save and Finish");
        assertNoLabKeyErrors();
        clickAndWait(Locator.linkWithText(ASSAY_RUN_TWO_COLUMN));
        validateTwoColumnData("Data", "ParticipantID");
        testMvFiltering(List.of("age", "sex"));

        log("Import from Excel in single-column format");
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.name("name"), ASSAY_EXCEL_RUN_SINGLE_COLUMN);
        checkCheckbox(Locator.radioButtonByNameAndValue("dataCollectorName", "File upload"));

        setFormElement(Locator.name("__primaryFile__"), ASSAY_SINGLE_COLUMN_EXCEL_FILE_BAD);
        clickButton("Save and Finish");
        assertLabKeyErrorPresent();

        checkCheckbox(Locator.radioButtonByNameAndValue("dataCollectorName", "File upload"));
        setFormElement(Locator.name("__primaryFile__"), ASSAY_SINGLE_COLUMN_EXCEL_FILE);
        clickButton("Save and Finish");
        assertNoLabKeyErrors();
        clickAndWait(Locator.linkWithText(ASSAY_EXCEL_RUN_SINGLE_COLUMN));
        validateSingleColumnData();

        log("Import from Excel in two-column format");
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.name("name"), ASSAY_EXCEL_RUN_TWO_COLUMN);
        checkCheckbox(Locator.radioButtonByNameAndValue("dataCollectorName", "File upload"));
        setFormElement(Locator.name("__primaryFile__"), ASSAY_TWO_COLUMN_EXCEL_FILE_BAD);
        clickButton("Save and Finish");
        assertLabKeyErrorPresent();

        checkCheckbox(Locator.radioButtonByNameAndValue("dataCollectorName", "File upload"));
        setFormElement(Locator.name("__primaryFile__"), ASSAY_TWO_COLUMN_EXCEL_FILE);
        clickButton("Save and Finish");
        assertNoLabKeyErrors();
        clickAndWait(Locator.linkWithText(ASSAY_EXCEL_RUN_TWO_COLUMN));
        validateTwoColumnData("Data", "ParticipantID");

        log("Export folder");
        File folderAsZip = ExportFolderPage.beginAt(this).includeExperimentsAndRuns(true).exportToBrowserAsZipFile();

        log("Import exported folder into subfolder");
        _containerHelper.createProject(IMPORT_PROJECT, "Assay");
        importFolderFromZip(folderAsZip);

        log("Verify MV indicators are imported correctly");
        clickProject(IMPORT_PROJECT);
        waitAndClickAndWait(Locator.linkWithText("MVAssay"));
        clickAndWait(Locator.linkWithText("view results"));
        assertMvIndicatorPresent();
        testMvFiltering(List.of("age", "sex"));
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment", "assay");
    }
}
