package org.labkey.test.tests.assay;

import org.intellij.lang.annotations.Language;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.admin.ExportFolderPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.assay.GeneralAssayDesign;
import org.labkey.test.tests.MissingValueIndicatorsTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category({Daily.class})
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
        assertNoLabKeyErrors();

        DataRegionTable dataRegion = new DataRegionTable("Data", this);

        Map<String, List<String>> singleExpectedData = new HashMap<>();
        singleExpectedData.put("Participant ID", List.of("Ted", "Alice", "Bob"));
        singleExpectedData.put("Age", List.of("N", "17", "Q"));
        singleExpectedData.put("Sex", List.of("male", "female", "N"));

        Map<String, List<Integer>> singleExpectedMVIndicators = new HashMap<>();
        singleExpectedMVIndicators.put("Age", List.of(0, 2));
        singleExpectedMVIndicators.put("Sex", List.of(2));

        checkDataregionData(dataRegion, singleExpectedData);
        checkMvIndicatorPresent(dataRegion, singleExpectedMVIndicators);

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
        assertNoLabKeyErrors();

        dataRegion = new DataRegionTable("Data", this);

        Map<String, List<String>> multiExpectedData = new HashMap<>();
        multiExpectedData.put("Participant ID", List.of("Franny", "Zoe", "J.D."));
        multiExpectedData.put("Age", List.of("N", "Q", "50"));
        multiExpectedData.put("Sex", List.of("male", "female", "Q"));

        Map<String, List<Integer>> multiExpectedMVIndicators = new HashMap<>();
        multiExpectedMVIndicators.put("Age", List.of(0, 1));
        multiExpectedMVIndicators.put("Sex", List.of(2));

        checkDataregionData(dataRegion, multiExpectedData);
        checkMvIndicatorPresent(dataRegion, multiExpectedMVIndicators);
        checkOriginalValuePopup(dataRegion, "Age", 0, " ");

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
        assertNoLabKeyErrors();

        dataRegion = new DataRegionTable("Data", this);

        // Expected data is the same as before.
        checkDataregionData(dataRegion, singleExpectedData);
        checkMvIndicatorPresent(dataRegion, singleExpectedMVIndicators);

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
        assertNoLabKeyErrors();

        dataRegion = new DataRegionTable("Data", this);

        // Expected data is the same as before.
        checkDataregionData(dataRegion, multiExpectedData);
        checkMvIndicatorPresent(dataRegion, multiExpectedMVIndicators);
        checkOriginalValuePopup(dataRegion, "Age", 0, " ");

        log("Export folder");
        File folderAsZip = ExportFolderPage.beginAt(this).includeExperimentsAndRuns(true).exportToBrowserAsZipFile();

        log("Import exported folder into subfolder");
        _containerHelper.createProject(IMPORT_PROJECT, "Assay");
        importFolderFromZip(folderAsZip);

        log("Verify MV indicators are imported correctly");
        clickProject(IMPORT_PROJECT);
        waitAndClickAndWait(Locator.linkWithText("MVAssay"));
        clickAndWait(Locator.linkWithText("view results"));
        multiExpectedMVIndicators = new HashMap<>();
        multiExpectedMVIndicators.put("Age", List.of(0, 2, 3, 4, 6, 8, 9, 10));
        multiExpectedMVIndicators.put("Sex", List.of(2, 5, 8, 11));
        checkMvIndicatorPresent(dataRegion, multiExpectedMVIndicators);
        testMvFiltering(List.of("age", "sex"));
    }

    /**
     * provides regression coverage for Issue 39496
     * @throws Exception there was an exception creating the assay for this test
     */
    @Test
    public void testSaveBatchAPIMissingValues() throws Exception
    {
        // create the assay
        String assayName = "missingValueSaveBatchAPIAssay";
        List<PropertyDescriptor> dataFields = List.of(
                new FieldDefinition("ParticipantId", FieldDefinition.ColumnType.String),
                new FieldDefinition("VisitId", FieldDefinition.ColumnType.Integer),
                new FieldDefinition("Count", FieldDefinition.ColumnType.Integer).setMvEnabled(true));

        var serverProtocol = new GeneralAssayDesign(assayName)
                .setBatchFields(List.of(new FieldDefinition("batchData", FieldDefinition.ColumnType.String)), false)
                .setDataFields(dataFields, false)
                .createAssay(getProjectName(), createDefaultConnection());

        @Language("JavaScript") String saveBatch = """
                LABKEY.Experiment.saveBatch({
                    assayId: %protocolId%,
                    batch: {
                        runs: [{
                            name: 'js api',
                            dataRows : [
                                {participantId : 'p1', visitId : 1, count : 4.0},
                                {participantId : 'p2', visitId : 1, count : 'N'},
                                {participantId : 'p3', visitId : 1, count : 5, countMVIndicator : 'Q'}
                            ]
                        }]
                    }
                });
                """.replace("%protocolId%", serverProtocol.getProtocolId().toString());
        executeScript(saveBatch);

        // navigate to the results view
        goToProjectHome();
        clickAndWait(Locator.linkWithText(assayName));
        clickAndWait(Locator.linkWithText("view results"));
        var dataRegion = DataRegionTable.DataRegion(getDriver()).waitFor();

        // expect 3 rows in this assay, p2 and p3 should get mv indicators in the count column
        Map<String, List<String>> expectedData = new HashMap<>();
        expectedData.put("Participant ID", List.of("p1", "p2", "p3"));
        expectedData.put("Visit ID", List.of("1", "1", "1"));
        expectedData.put("Count", List.of("4", "N", "Q"));
        checkDataregionData(dataRegion, expectedData);

        Map<String, List<Integer>> expectedMVIndicators = new HashMap<>();
        expectedMVIndicators.put("Count", List.of(1, 2));
        checkMvIndicatorPresent(dataRegion, expectedMVIndicators);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment", "assay");
    }
}
