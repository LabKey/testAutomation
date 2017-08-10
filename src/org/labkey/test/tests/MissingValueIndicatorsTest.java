/*
 * Copyright (c) 2008-2017 LabKey Corporation
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({DailyB.class, Assays.class})
public class MissingValueIndicatorsTest extends BaseWebDriverTest
{
    {setIsBootstrapWhitelisted(true);}
    @BeforeClass
    public static void beforeTestClass()
    {
        MissingValueIndicatorsTest init = (MissingValueIndicatorsTest)getCurrentTest();

        init.setupProject();
        init.setupMVIndicators();
    }

    @LogMethod
    private void setupProject()
    {
        _containerHelper.createProject(getProjectName(), "Study");
        clickButton("Create Study");
        selectOptionByValue(Locator.name("securityString"), "BASIC_WRITE");
        clickButton("Create Study");
    }

    @LogMethod
    private void setupMVIndicators()
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Missing Values"));
        uncheckCheckbox(Locator.checkboxById("inherit"));

        // Delete all site-level settings
        while(isElementPresent(Locator.tagWithAttribute("img", "alt", "delete")))
        {
            click(Locator.tagWithAttribute("img", "alt", "delete"));
            sleep(500);
        }

        clickButton("Add", 0);
        clickButton("Add", 0);
        clickButton("Add", 0);
        sleep(500);

        // This is disgusting. For some reason a simple XPath doesn't seem to work: we have to get the id right,
        // and unfortunately the id is dependent on how many inherited indicators we had, which can vary by server.
        // So we have to try all possible ids.
        int completedCount = 0;
        String[] mvIndicators = new String[] {"Q", "N", "Z"};
        int index = 1; // xpath is 1-based
        while (completedCount < 3 && index < 1000)
        {
            String xpathString = "//div[@id='mvIndicatorsDiv']//input[@name='mvIndicators' and @id='mvIndicators" + index + "']";
            if (isElementPresent(Locator.xpath(xpathString)))
            {
                String mvIndicator = mvIndicators[completedCount++];
                setFormElement(Locator.xpath(xpathString), mvIndicator);
            }
            index++;
        }

        clickButton("Save");
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testListMV()
    {
        final String LIST_NAME = "MVList";
        final String TEST_DATA_SINGLE_COLUMN_LIST =
                "Name\tAge\tSex\n" +
                "Ted\tN\tmale\n" +
                "Alice\t17\tfemale\n" +
                "Bob\tQ\tN";
        final String TEST_DATA_TWO_COLUMN_LIST =
                "Name\tAge\tAgeMVIndicator\tSex\tSexMVIndicator\n" +
                "Franny\t\tN\tmale\t\n" +
                "Zoe\t25\tQ\tfemale\t\n" +
                "J.D.\t50\t\tmale\tQ";
        final String TEST_DATA_SINGLE_COLUMN_LIST_BAD =
                "Name\tAge\tSex\n" +
                "Ted\t.N\tmale\n" +
                "Alice\t17\tfemale\n" +
                "Bob\tQ\tN";
        final String TEST_DATA_TWO_COLUMN_LIST_BAD =
                "Name\tAge\tAgeMVIndicator\tSex\tSexMVIndicator\n" +
                "Franny\t\tN\tmale\t\n" +
                "Zoe\t25\tQ\tfemale\t\n" +
                "J.D.\t50\t\tmale\t.Q";


        ListHelper.ListColumn[] columns = new ListHelper.ListColumn[3];

        ListHelper.ListColumn listColumn = new ListHelper.ListColumn("name", "Name", ListHelper.ListColumnType.String, "");
        columns[0] = listColumn;

        listColumn = new ListHelper.ListColumn("age", "Age", ListHelper.ListColumnType.Integer, "");
        listColumn.setMvEnabled(true);
        columns[1] = listColumn;

        listColumn = new ListHelper.ListColumn("sex", "Sex", ListHelper.ListColumnType.String, "");
        listColumn.setMvEnabled(true);
        columns[2] = listColumn;

        _listHelper.createList(getProjectName(), LIST_NAME, ListHelper.ListColumnType.AutoInteger, "Key", columns);

        log("Test upload list data with a combined data and MVI column");
        _listHelper.clickImportData();
        setFormElementJS(Locator.id("tsv3"), TEST_DATA_SINGLE_COLUMN_LIST_BAD);
        _listHelper.submitImportTsv_error(null);
        assertLabKeyErrorPresent();

        setFormElementJS(Locator.id("tsv3"), TEST_DATA_SINGLE_COLUMN_LIST);
        _listHelper.submitImportTsv_success();
        validateSingleColumnData();

        deleteListData(3);

        log("Test inserting a single new row");
        DataRegionTable.findDataRegion(this).clickInsertNewRowDropdown();        setFormElement(Locator.name("quf_name"), "Sid");
        setFormElement(Locator.name("quf_sex"), "male");
        selectOptionByValue(Locator.name("quf_ageMVIndicator"), "Z");
        clickButton("Submit");
        assertNoLabKeyErrors();
        assertTextPresent("Sid", "male", "N");

        deleteListData(1);

        log("Test separate MVIndicator column");
        DataRegionTable.findDataRegion(this).clickImportBulkData();
        setFormElementJS(Locator.id("tsv3"), TEST_DATA_TWO_COLUMN_LIST_BAD);
        _listHelper.submitImportTsv_error(null);
        assertLabKeyErrorPresent();

        setFormElementJS(Locator.id("tsv3"), TEST_DATA_TWO_COLUMN_LIST);
        _listHelper.submitImportTsv_success();
        validateTwoColumnData("query", "name");
    }

    private void deleteListData(int rowCount)
    {
        DataRegionTable dt = new DataRegionTable("query", getDriver());
        checkCheckbox(Locator.checkboxByName(".toggle"));
        doAndWaitForPageToLoad(() ->
        {
            dt.clickHeaderButton("Delete");
            assertAlert("Are you sure you want to delete the selected row" + (rowCount == 1 ? "?" : "s?"));
        });
    }

    @Test
    public void testDatasetMV()
    {
        final String datasetName = "MV Dataset";
        final String DATASET_SCHEMA_FILE = "/sampledata/mvIndicators/dataset_schema.tsv";
        final String TEST_DATA_SINGLE_COLUMN_DATASET =
                "participantid\tSequenceNum\tAge\tSex\n" +
                        "Ted\t1\tN\tmale\n" +
                        "Alice\t1\t17\tfemale\n" +
                        "Bob\t1\tQ\tN";
        final String TEST_DATA_TWO_COLUMN_DATASET =
                "participantid\tSequenceNum\tAge\tAgeMVIndicator\tSex\tSexMVIndicator\n" +
                        "Franny\t1\t\tN\tmale\t\n" +
                        "Zoe\t1\t25\tQ\tfemale\t\n" +
                        "J.D.\t1\t50\t\tmale\tQ";
        final String TEST_DATA_SINGLE_COLUMN_DATASET_BAD =
                "participantid\tSequenceNum\tAge\tSex\n" +
                        "Ted\t1\t.N\tmale\n" +
                        "Alice\t1\t17\tfemale\n" +
                        "Bob\t1\tQ\tN";
        final String TEST_DATA_TWO_COLUMN_DATASET_BAD =
                "participantid\tSequenceNum\tAge\tAgeMVIndicator\tSex\tSexMVIndicator\n" +
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
        clickButton("View Data");
        DataRegionTable.findDataRegion(this).clickImportBulkData();

        setFormElementJS(Locator.id("tsv3"), TEST_DATA_SINGLE_COLUMN_DATASET_BAD);
        _listHelper.submitImportTsv_error(null);

        setFormElementJS(Locator.id("tsv3"), TEST_DATA_SINGLE_COLUMN_DATASET);
        _listHelper.submitImportTsv_success();
        validateSingleColumnData();

        deleteDatasetData(3);

        log("Test inserting a single row");
        DataRegionTable.findDataRegion(this).clickInsertNewRow();
        setFormElement(Locator.name("quf_ParticipantId"), "Sid");
        setFormElement(Locator.name("quf_SequenceNum"), "1");
        selectOptionByValue(Locator.name("quf_AgeMVIndicator"), "Z");
        setFormElement(Locator.name("quf_Sex"), "male");
        clickButton("Submit");
        assertNoLabKeyErrors();
        assertTextPresent("Sid", "male", "N");

        deleteDatasetData(1);

        log("Import dataset data with two mv columns");
        DataRegionTable.findDataRegion(this).clickImportBulkData();

        setFormElementJS(Locator.id("tsv3"), TEST_DATA_TWO_COLUMN_DATASET_BAD);
        _listHelper.submitImportTsv_error("Value is not a valid missing value indicator: .Q");

        _listHelper.submitTsvData(TEST_DATA_TWO_COLUMN_DATASET);
        validateTwoColumnData("Dataset", "ParticipantId");

        log("19874: Regression test for reshow of missing value indicators when submitting default forms with errors");
        DataRegionTable.findDataRegion(this).clickInsertNewRow();
        Locator mvSeletor = Locator.name("quf_AgeMVIndicator");
        Assert.assertEquals("There should not be a devault missing value indicator selection", "", getSelectedOptionText(mvSeletor));
        String mvSelection = "Z";
        selectOptionByValue(mvSeletor, mvSelection);
        clickButton("Submit");
        Assert.assertEquals("Form should remember MVI selection after error", mvSelection, getSelectedOptionText(mvSeletor));
    }

    private void validateSingleColumnData()
    {
        assertNoLabKeyErrors();
        assertMvIndicatorPresent();
        assertTextPresent("Ted", "Alice", "Bob", "Q", "N", "male", "female", "17");
    }

    private void validateTwoColumnData(String dataRegionName, String columnName)
    {
        assertNoLabKeyErrors();
        assertMvIndicatorPresent();
        assertTextPresent("Franny", "Zoe", "J.D.", "Q", "N", "male", "female", "50");
        assertTextNotPresent("'25'");
        DataRegionTable dataRegion = new DataRegionTable(dataRegionName, this);
        dataRegion.setFilter(columnName, "Equals", "Zoe");
        assertTextNotPresent("'25'");
        assertTextPresent("Zoe", "female");
        assertMvIndicatorPresent();
        click(Locator.xpath("//img[@class='labkey-mv-indicator']/../../a"));
        assertTextPresent("'25'");
        dataRegion.clearAllFilters(columnName);
    }

    @Test
    public void testAssayMV()
    {
        final String ASSAY_NAME = "MVAssay";
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

        defineAssay(ASSAY_NAME);

        log("Import single column MV data");
        waitAndClickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickButton("Import Data");
        String targetStudyValue = "/" + getProjectName() + " (" + getProjectName() + " Study)";
        selectOptionByText(Locator.xpath("//select[@name='targetStudy']"), targetStudyValue);

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
        selectOptionByText(Locator.xpath("//select[@name='targetStudy']"), targetStudyValue);

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

        log("Copy to study");
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickAndWait(Locator.linkWithText(ASSAY_RUN_SINGLE_COLUMN));
        validateSingleColumnData();
        checkCheckbox(Locator.checkboxByName(".toggle"));
        clickButton("Copy to Study");
        
        clickButton("Next");

        clickButton("Copy to Study");
        validateSingleColumnData();

        log("Import from Excel in single-column format");
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickButton("Import Data");
        selectOptionByText(Locator.xpath("//select[@name='targetStudy']"), targetStudyValue);

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
        selectOptionByText(Locator.xpath("//select[@name='targetStudy']"), targetStudyValue);

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
    }

    private void assertMvIndicatorPresent()
    {
        // We'd better have some
        Locator loc = Locator.xpath("//img[@class='labkey-mv-indicator']");
        waitForElement(loc);
        assertElementPresent(Locator.xpath("//img[@class='labkey-mv-indicator']"));
    }

    @LogMethod
    private void defineAssay(String assayName)
    {
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Assay List");

        //copied from old test
        clickButton("Manage Assays");
        clickButton("New Assay Design");
        checkCheckbox(Locator.radioButtonByNameAndValue("providerName", "General"));
        clickButton("Next");

        waitForElement(Locator.id("AssayDesignerName"), WAIT_FOR_JAVASCRIPT);

        setFormElement(Locator.id("AssayDesignerName"), assayName);

        _listHelper.addField("Data Fields", "age", "Age", ListHelper.ListColumnType.Integer);
        _listHelper.addField("Data Fields", "sex", "Sex", ListHelper.ListColumnType.String);
        sleep(1000);

        log("setting fields to enable missing values");
        _listHelper.clickRow(getPropertyXPath("Data Fields"), 4);
        _listHelper.clickMvEnabled(getPropertyXPath("Data Fields"));

        _listHelper.clickRow(getPropertyXPath("Data Fields"), 5);
        _listHelper.clickMvEnabled(getPropertyXPath("Data Fields"));

        clickButton("Save & Close");
        assertNoLabKeyErrors();
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
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }
}
