package org.labkey.test.tests.study;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.DatasetPropertiesPage;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.tests.MissingValueIndicatorsTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.test.util.DataRegionTable.DataRegion;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class StudyMissingValuesTest extends MissingValueIndicatorsTest
{
    @BeforeClass
    public static void beforeTestClass()
    {
        StudyMissingValuesTest init = (StudyMissingValuesTest)getCurrentTest();

        init.setupProject();
    }

    @LogMethod
    private void setupProject()
    {
        _containerHelper.createProject(getProjectName(), "Study");
        new PortalHelper(this).addWebPart("Assay List");
        clickButton("Create Study");
        selectOptionByValue(Locator.name("securityString"), "BASIC_WRITE");
        clickButton("Create Study");
        setupMVIndicators();
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testDatasetMV()
    {
        final String datasetName = "MV Dataset";
        final File DATASET_SCHEMA_FILE = TestFileUtils.getSampleData("mvIndicators/dataset_schema.tsv");
        final String TEST_DATA_SINGLE_COLUMN_DATASET =
                "participantid\tSequenceNum\tAge with space\tSex\n" +
                        "Ted\t1\tN\tmale\n" +
                        "Alice\t1\t17\tfemale\n" +
                        "Bob\t1\tQ\tN";
        final String TEST_DATA_TWO_COLUMN_DATASET =
                "participantid\tSequenceNum\tAge with space\tAge with spaceMVIndicator\tSex\tSexMVIndicator\n" +
                        "Franny\t1\t\tN\tmale\t\n" +
                        "Zoe\t1\t25\tQ\tfemale\t\n" +
                        "J.D.\t1\t50\t\tmale\tQ";
        final String TEST_DATA_SINGLE_COLUMN_DATASET_BAD =
                "participantid\tSequenceNum\tAge with space\tSex\n" +
                        "Ted\t1\t.N\tmale\n" +
                        "Alice\t1\t17\tfemale\n" +
                        "Bob\t1\tQ\tN";
        final String TEST_DATA_TWO_COLUMN_DATASET_BAD =
                "participantid\tSequenceNum\tAge with space\tAge with spaceMVIndicator\tSex\tSexMVIndicator\n" +
                        "Franny\t1\t\tN\tmale\t\n" +
                        "Zoe\t1\t25\tQ\tfemale\t\n" +
                        "J.D.\t1\t50\t\tmale\t.Q";

        // Dummy visit map data (probably non-sensical), but enough to get a placeholder created for dataset #1:
        _studyHelper.goToManageVisits().goToImportVisitMap();
        setFormElement(Locator.name("content"), "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<visitMap xmlns=\"http://labkey.org/study/xml\">\n" +
                "  <visit label=\"Only Visit\" typeCode=\"S\" sequenceNum=\"20.0\" visitDateDatasetId=\"1\" sequenceNumHandling=\"normal\">\n" +
                "    <datasets>\n" +
                "      <dataset id=\"1\" type=\"REQUIRED\"/>\n" +
                "    </datasets>\n" +
                "  </visit>\n" +
                "</visitMap>");
        clickButton("Import");
        clickAndWait(Locator.linkWithText("Manage Study"));
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText("Define Dataset Schemas"));
        clickAndWait(Locator.linkWithText("Bulk Import Schemas"));
        setFormElement(Locator.name("typeNameColumn"), "datasetName");
        setFormElement(Locator.name("labelColumn"), "datasetLabel");
        setFormElement(Locator.name("typeIdColumn"), "datasetId");
        setFormElementJS(Locator.name("tsv"), TestFileUtils.getFileContents(DATASET_SCHEMA_FILE));
        clickButton("Submit", 180000);
        assertNoLabKeyErrors();
        assertTextPresent(datasetName);

        log("Import dataset data");
        clickAndWait(Locator.linkWithText(datasetName));
        ImportDataPage importDataPage = new DatasetPropertiesPage(getDriver())
                .clickViewData()
                .getDataRegion()
                .clickImportBulkData();

        importDataPage.setText(TEST_DATA_SINGLE_COLUMN_DATASET_BAD)
                .submitExpectingError();

        importDataPage.setText(TEST_DATA_SINGLE_COLUMN_DATASET)
                .submit();

        assertNoLabKeyErrors();

        DataRegionTable dataRegion = new DataRegionTable("Dataset", this);

        Map<String, List<String>> expectedData = new HashMap<>();
        expectedData.put("Participant ID", List.of("Alice", "Bob", "Ted"));
        expectedData.put("Age with space", List.of("17", "Q", "N"));
        expectedData.put("Sex", List.of("female", "N", "male"));

        Map<String, List<Integer>> expectedMVIndicators = new HashMap<>();
        expectedMVIndicators.put("Age with space", List.of(1, 2));
        expectedMVIndicators.put("Sex", List.of(1));

        checkDataregionData(dataRegion, expectedData);
        checkMvIndicatorPresent(dataRegion, expectedMVIndicators);

        testMvFiltering(List.of("Age with space", "Sex"));

        deleteDatasetData(3);

        log("Test inserting a single row");
        DataRegion(getDriver()).find().clickInsertNewRow();
        setFormElement(Locator.name("quf_ParticipantId"), "Sid");
        setFormElement(Locator.name("quf_SequenceNum"), "1");
        selectOptionByValue(Locator.name("quf_Age with spaceMVIndicator"), "Z");
        setFormElement(Locator.name("quf_Sex"), "male");
        clickButton("Submit");
        assertNoLabKeyErrors();
        assertTextPresent("Sid", "male", "N");

        deleteDatasetData(1);

        log("Import dataset data with two mv columns");
        importDataPage = DataRegion(getDriver()).find().clickImportBulkData();

        importDataPage.setText(TEST_DATA_TWO_COLUMN_DATASET_BAD);
        importDataPage.submitExpectingErrorContaining("Value is not a valid missing value indicator: .Q");

        importDataPage.setText(TEST_DATA_TWO_COLUMN_DATASET);
        importDataPage.submit();
        assertNoLabKeyErrors();

        dataRegion = new DataRegionTable("Dataset", this);

        expectedData = new HashMap<>();
        expectedData.put("Participant ID", List.of("Franny", "J.D.", "Zoe"));
        expectedData.put("Age with space", List.of("N", "50", "Q"));
        expectedData.put("Sex", List.of("male", "Q", "female"));

        expectedMVIndicators = new HashMap<>();
        expectedMVIndicators.put("Age with space", List.of(0, 2));
        expectedMVIndicators.put("Sex", List.of(1));

        checkDataregionData(dataRegion, expectedData);
        checkMvIndicatorPresent(dataRegion, expectedMVIndicators);
        checkOriginalValuePopup(dataRegion, "Age with space", 2, "25");

        testMvFiltering(List.of("Age with space", "Sex"));

        log("19874: Regression test for reshow of missing value indicators when submitting default forms with errors");
        DataRegion(getDriver()).find().clickInsertNewRow();
        Locator mvSeletor = Locator.name("quf_Age with spaceMVIndicator");
        Assert.assertEquals("There should not be a devault missing value indicator selection", "", getSelectedOptionText(mvSeletor));
        String mvSelection = "Z";
        selectOptionByValue(mvSeletor, mvSelection);
        clickButton("Submit");
        Assert.assertEquals("Form should remember MVI selection after error", mvSelection, getSelectedOptionText(mvSeletor));
    }

    @Test
    public void testAssayLinkToStudyMV()
    {
        final String ASSAY_NAME = "MVAssay";
        final String ASSAY_RUN_SINGLE_COLUMN = "MVAssayRunSingleColumn";
        final String TEST_DATA_SINGLE_COLUMN_ASSAY =
                "SpecimenID\tParticipantID\tVisitID\tDate\tage\tsex\n" +
                        "1\tTed\t1\t01-Jan-09\tN\tmale\n" +
                        "2\tAlice\t1\t01-Jan-09\t17\tfemale\n" +
                        "3\tBob\t1\t01-Jan-09\tQ\tN";

        defineAssay(ASSAY_NAME);

        log("Import single column MV data");
        waitAndClickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickButton("Import Data");
        String targetStudyValue = "/" + getProjectName() + " (" + getProjectName() + " Study)";
        selectOptionByText(Locator.xpath("//select[@name='targetStudy']"), targetStudyValue);

        clickButton("Next");
        setFormElement(Locator.name("name"), ASSAY_RUN_SINGLE_COLUMN);
        click(Locator.xpath("//input[@value='textAreaDataProvider']"));
        setFormElement(Locator.name("TextAreaDataCollector.textArea"), TEST_DATA_SINGLE_COLUMN_ASSAY);
        clickButton("Save and Finish");
        assertNoLabKeyErrors();

        log("Link to study");
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickAndWait(Locator.linkWithText(ASSAY_RUN_SINGLE_COLUMN));
        checkCheckbox(Locator.checkboxByName(".toggle"));
        clickButton("Link to Study");

        clickButton("Next");

        clickButton("Link to Study");
        assertNoLabKeyErrors();

        DataRegionTable dataRegion = new DataRegionTable("Dataset", this);

        Map<String, List<String>> expectedData = new HashMap<>();
        expectedData.put("Participant ID", List.of("Alice", "Bob", "Ted"));
        expectedData.put("Age", List.of("17", "Q", "N"));
        expectedData.put("Sex", List.of("female", "N", "male"));
        expectedData.put("Assay ID", Collections.nCopies(3, ASSAY_RUN_SINGLE_COLUMN));

        Map<String, List<Integer>> expectedMVIndicators = new HashMap<>();
        expectedMVIndicators.put("Age", List.of(1, 2));
        expectedMVIndicators.put("Sex", List.of(1));

        checkDataregionData(dataRegion, expectedData);
        checkMvIndicatorPresent(dataRegion, expectedMVIndicators);

    }

    private void deleteDatasetData(int rowCount)
    {
        checkCheckbox(Locator.checkboxByName(".toggle"));
        doAndWaitForPageToLoad(() ->
        {
            new DataRegionTable("Dataset", getDriver()).clickHeaderButton("Delete");
            assertAlert("Delete selected row" + (1 == rowCount ? "" : "s") + " from this dataset?");
        });
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment", "study");
    }
}
