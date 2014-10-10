/*
 * Copyright (c) 2008-2014 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.ListHelper;

import java.io.File;
import java.util.Arrays;

@Category({DailyB.class, Assays.class})
public class MissingValueIndicatorsTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "MVIVerifyProject";
    private static final String LIST_NAME = "MVList";
    private static final String ASSAY_NAME = "MVAssay";
    private static final String ASSAY_RUN_SINGLE_COLUMN = "MVAssayRunSingleColumn";
    private static final String ASSAY_RUN_TWO_COLUMN = "MVAssayRunTwoColumn";
    private static final String ASSAY_EXCEL_RUN_SINGLE_COLUMN = "MVAssayExcelRunSingleColumn";
    private static final String ASSAY_EXCEL_RUN_TWO_COLUMN = "MVAssayExcelRunTwoColumn";

    private static final String TEST_DATA_SINGLE_COLUMN_LIST =
            "Name" + "\t" + "Age" + "\t"  + "Sex" + "\n" +
            "Ted" + "\t" + "N" + "\t" + "male" + "\n" +
            "Alice" + "\t" + "17" + "\t" + "female" + "\n" +
            "Bob" + "\t" + "Q" + "\t" + "N" + "\n";

    private static final String TEST_DATA_TWO_COLUMN_LIST =
            "Name" +    "\t" + "Age" +  "\t" + "AgeMVIndicator" +   "\t" + "Sex" +  "\t" + "SexMVIndicator" + "\n" +
            "Franny" +  "\t" + "" +     "\t" + "N" +               "\t" + "male" + "\t" +  "" + "\n" +
            "Zoe" +     "\t" + "25" +   "\t" + "Q" +               "\t" + "female" +     "\t" +  "" + "\n" +
            "J.D." +    "\t" + "50" +   "\t" + "" +                 "\t" + "male" + "\t" +  "Q" + "\n";

    private static final String TEST_DATA_SINGLE_COLUMN_LIST_BAD =
            "Name" + "\t" + "Age" + "\t"  + "Sex" + "\n" +
            "Ted" + "\t" + ".N" + "\t" + "male" + "\n" +
            "Alice" + "\t" + "17" + "\t" + "female" + "\n" +
            "Bob" + "\t" + "Q" + "\t" + "N" + "\n";

    private static final String TEST_DATA_TWO_COLUMN_LIST_BAD =
            "Name" +    "\t" + "Age" +  "\t" + "AgeMVIndicator" +   "\t" + "Sex" +  "\t" + "SexMVIndicator" + "\n" +
            "Franny" +  "\t" + "" +     "\t" + "N" +               "\t" + "male" + "\t" +  "" + "\n" +
            "Zoe" +     "\t" + "25" +   "\t" + "Q" +               "\t" + "female" +     "\t" +  "" + "\n" +
            "J.D." +    "\t" + "50" +   "\t" + "" +                 "\t" + "male" + "\t" +  ".Q" + "\n";

    private static final String TEST_DATA_SINGLE_COLUMN_DATASET =
            "participantid\tSequenceNum\tAge\tSex\n" +
            "Ted\t1\tN\tmale\n" +
            "Alice\t1\t17\tfemale\n" +
            "Bob\t1\tQ\tN";

    private static final String TEST_DATA_TWO_COLUMN_DATASET =
            "participantid\tSequenceNum\tAge\tAgeMVIndicator\tSex\tSexMVIndicator\n" +
            "Franny\t1\t\tN\tmale\t\n" +
            "Zoe\t1\t25\tQ\tfemale\t\n" +
            "J.D.\t1\t50\t\tmale\tQ";

    private static final String TEST_DATA_SINGLE_COLUMN_DATASET_BAD =
            "participantid\tSequenceNum\tAge\tSex\n" +
            "Ted\t1\t.N\tmale\n" +
            "Alice\t1\t17\tfemale\n" +
            "Bob\t1\tQ\tN";

    private static final String TEST_DATA_TWO_COLUMN_DATASET_BAD =
            "participantid\tSequenceNum\tAge\tAgeMVIndicator\tSex\tSexMVIndicator\n" +
            "Franny\t1\t\tN\tmale\t\n" +
            "Zoe\t1\t25\tQ\tfemale\t\n" +
            "J.D.\t1\t50\t\tmale\t.Q";

    private static final String DATASET_SCHEMA_FILE = "/sampledata/mvIndicators/dataset_schema.tsv";

    private static final String TEST_DATA_SINGLE_COLUMN_ASSAY =
            "SpecimenID\tParticipantID\tVisitID\tDate\tage\tsex\n" +
                    "1\tTed\t1\t01-Jan-09\tN\tmale\n" +
                    "2\tAlice\t1\t01-Jan-09\t17\tfemale\n" +
                    "3\tBob\t1\t01-Jan-09\tQ\tN";

    private static final String TEST_DATA_TWO_COLUMN_ASSAY =
            "SpecimenID\tParticipantID\tVisitID\tDate\tage\tageMVIndicator\tsex\tsexMVIndicator\n" +
                    "1\tFranny\t1\t01-Jan-09\t\tN\tmale\t\n" +
                    "2\tZoe\t1\t01-Jan-09\t25\tQ\tfemale\t\n" +
                    "3\tJ.D.\t1\t01-Jan-09\t50\t\tmale\tQ";

    private static final String TEST_DATA_SINGLE_COLUMN_ASSAY_BAD =
            "SpecimenID\tParticipantID\tVisitID\tDate\tage\tsex\n" +
                    "1\tTed\t1\t01-Jan-09\t.N\tmale\n" +
                    "2\tAlice\t1\t01-Jan-09\t17\tfemale\n" +
                    "3\tBob\t1\t01-Jan-09\tQ\tN";

    private static final String TEST_DATA_TWO_COLUMN_ASSAY_BAD =
            "SpecimenID\tParticipantID\tVisitID\tDate\tage\tageMVIndicator\tsex\tsexMVIndicator\n" +
                    "1\tFranny\t1\t01-Jan-09\t\tN\tmale\t\n" +
                    "2\tZoe\t1\t01-Jan-09\t25\tQ\tfemale\t\n" +
                    "3\tJ.D.\t1\t01-Jan-09\t50\t\tmale\t.Q";

    private final String ASSAY_SINGLE_COLUMN_EXCEL_FILE = getSampleRoot() + "assay_single_column.xls";
    private final String ASSAY_TWO_COLUMN_EXCEL_FILE = getSampleRoot() + "assay_two_column.xls";
    private final String ASSAY_SINGLE_COLUMN_EXCEL_FILE_BAD = getSampleRoot() + "assay_single_column_bad.xls";
    private final String ASSAY_TWO_COLUMN_EXCEL_FILE_BAD = getSampleRoot() + "assay_two_column_bad.xls";

    private String getSampleRoot()
    {
        return TestFileUtils.getLabKeyRoot() + "/sampledata/mvIndicators/";
    }

    @Test
    public void testSteps()
    {
        log("Create MV project");
        _containerHelper.createProject(PROJECT_NAME, "Study");
        clickButton("Create Study");
        selectOptionByValue(Locator.name("securityString"), "BASIC_WRITE");
        clickButton("Create Study");
        clickAndWait(Locator.linkWithText(PROJECT_NAME + " Study"));
        setPipelineRoot(getSampleRoot());
        clickAndWait(Locator.linkWithText(PROJECT_NAME + " Study"));

        setupIndicators();

        checkList();
        checkDataset();
        checkAssay();
    }

    private void setupIndicators()
    {
        log("Setting MV indicators");
        
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Missing Values"));
        click(Locator.checkboxById("inherit"));

        // Delete all site-level settings
        while(isElementPresent(Locator.tagWithAttribute("img", "alt", "delete")))
        {
            click(Locator.tagWithAttribute("img", "alt", "delete"));
            sleep(500);
        }

        click(getButtonLocator("Add"));
        click(getButtonLocator("Add"));
        click(getButtonLocator("Add"));
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

        log("Set MV indicators.");
    }

    private void checkList()
    {
        log("Create list");

        ListHelper.ListColumn[] columns = new ListHelper.ListColumn[3];

        ListHelper.ListColumn listColumn = new ListHelper.ListColumn("name", "Name", ListHelper.ListColumnType.String, "");
        columns[0] = listColumn;

        listColumn = new ListHelper.ListColumn("age", "Age", ListHelper.ListColumnType.Integer, "");
        listColumn.setMvEnabled(true);
        columns[1] = listColumn;

        listColumn = new ListHelper.ListColumn("sex", "Sex", ListHelper.ListColumnType.String, "");
        listColumn.setMvEnabled(true);
        columns[2] = listColumn;

        _listHelper.createList(PROJECT_NAME, LIST_NAME, ListHelper.ListColumnType.AutoInteger, "Key", columns);

        log("Test upload list data with a combined data and MVI column");
        _listHelper.clickImportData();
        setFormElement(Locator.id("tsv3"), TEST_DATA_SINGLE_COLUMN_LIST_BAD);
        _listHelper.submitImportTsv_error(null);
        assertLabKeyErrorPresent();

        setFormElement(Locator.id("tsv3"), TEST_DATA_SINGLE_COLUMN_LIST);
        _listHelper.submitImportTsv_success();
        validateSingleColumnData();

        deleteListData(3);

        log("Test inserting a single new row");
        clickButton("Insert New");
        setFormElement(Locator.name("quf_name"), "Sid");
        setFormElement(Locator.name("quf_sex"), "male");
        selectOptionByValue(Locator.name("quf_ageMVIndicator"), "Z");
        clickButton("Submit");
        assertNoLabKeyErrors();
        assertTextPresent("Sid");
        assertTextPresent("male");
        assertTextPresent("N");

        deleteListData(1);

        log("Test separate MVIndicator column");
        clickButton("Import Data");
        setFormElement(Locator.id("tsv3"), TEST_DATA_TWO_COLUMN_LIST_BAD);
        _listHelper.submitImportTsv_error(null);
        assertLabKeyErrorPresent();

        setFormElement(Locator.id("tsv3"), TEST_DATA_TWO_COLUMN_LIST);
        _listHelper.submitImportTsv_success();
        validateTwoColumnData("query", "name");
    }

    private void deleteListData(int rowCount)
    {
        checkCheckbox(Locator.checkboxByName(".toggle"));
        prepForPageLoad();
        clickButton("Delete", 0);
        assertAlert("Are you sure you want to delete the selected row" + (rowCount == 1 ? "?" : "s?"));
        waitForPageToLoad();
    }

    private void checkDataset()
    {
        log("Create dataset");
        clickProject(PROJECT_NAME);
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Visits"));
        clickAndWait(Locator.linkWithText("Import Visit Map"));
        // Dummy visit map data (probably non-sensical), but enough to get a placeholder created for dataset #1:
        setFormElement(Locator.name("content"), "20|S|Only Visit|1|1|1|1|1|1|1");
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
        assertTextPresent("MV Dataset");

        log("Import dataset data");
        clickAndWait(Locator.linkWithText("MV Dataset"));
        clickButton("View Data");
        clickButton("Import Data");

        setFormElement(Locator.id("tsv3"), TEST_DATA_SINGLE_COLUMN_DATASET_BAD);
        _listHelper.submitImportTsv_error(null);

        setFormElement(Locator.id("tsv3"), TEST_DATA_SINGLE_COLUMN_DATASET);
        _listHelper.submitImportTsv_success();
        validateSingleColumnData();

        deleteDatasetData(3);

        log("Test inserting a single row");
        clickButton("Insert New");
        setFormElement(Locator.name("quf_ParticipantId"), "Sid");
        setFormElement(Locator.name("quf_SequenceNum"), "1");
        selectOptionByValue(Locator.name("quf_AgeMVIndicator"), "Z");
        setFormElement(Locator.name("quf_Sex"), "male");
        clickButton("Submit");
        assertNoLabKeyErrors();
        assertTextPresent("Sid");
        assertTextPresent("male");
        assertTextPresent("N");

        deleteDatasetData(1);

        log("Import dataset data with two mv columns");
        clickButton("Import Data");

        setFormElement(Locator.id("tsv3"), TEST_DATA_TWO_COLUMN_DATASET_BAD);
        _listHelper.submitImportTsv_error(null);

        _listHelper.submitTsvData(TEST_DATA_TWO_COLUMN_DATASET);
        validateTwoColumnData("Dataset", "ParticipantId");
    }

    private void validateSingleColumnData()
    {
        assertNoLabKeyErrors();
        assertMvIndicatorPresent();
        assertTextPresent("Ted");
        assertTextPresent("Alice");
        assertTextPresent("Bob");
        assertTextPresent("Q");
        assertTextPresent("N");
        assertTextPresent("male");
        assertTextPresent("female");
        assertTextPresent("17");
    }

    private void validateTwoColumnData(String dataRegionName, String columnName)
    {
        assertNoLabKeyErrors();
        assertMvIndicatorPresent();
        assertTextPresent("Franny");
        assertTextPresent("Zoe");
        assertTextPresent("J.D.");
        assertTextPresent("Q");
        assertTextPresent("N");
        assertTextPresent("male");
        assertTextPresent("female");
        assertTextPresent("50");
        assertTextNotPresent("'25'");
        setFilter(dataRegionName, columnName, "Equals", "Zoe");
        assertTextNotPresent("'25'");
        assertTextPresent("Zoe");
        assertTextPresent("female");
        assertMvIndicatorPresent();
        click(Locator.xpath("//img[@class='labkey-mv-indicator']/../../a"));
        assertTextPresent("'25'");
    }

    private void checkAssay()
    {
        log("Create assay");
        defineAssay();

        log("Import single column MV data");
        waitAndClickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickButton("Import Data");
        String targetStudyValue = "/" + PROJECT_NAME + " (" + PROJECT_NAME + " Study)";
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
        clickProject(PROJECT_NAME);
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
        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickAndWait(Locator.linkWithText(ASSAY_RUN_SINGLE_COLUMN));
        validateSingleColumnData();
        checkCheckbox(Locator.checkboxByName(".toggle"));
        clickButton("Copy to Study");
        
        clickButton("Next");

        clickButton("Copy to Study");
        validateSingleColumnData();

        log("Import from Excel in single-column format");
        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickButton("Import Data");
        selectOptionByText(Locator.xpath("//select[@name='targetStudy']"), targetStudyValue);

        clickButton("Next");
        setFormElement(Locator.name("name"), ASSAY_EXCEL_RUN_SINGLE_COLUMN);
        checkCheckbox(Locator.radioButtonByNameAndValue("dataCollectorName", "File upload"));

        File file = new File(ASSAY_SINGLE_COLUMN_EXCEL_FILE_BAD);
        setFormElement(Locator.name("__primaryFile__"), file);
        clickButton("Save and Finish");
        assertLabKeyErrorPresent();

        checkCheckbox(Locator.radioButtonByNameAndValue("dataCollectorName", "File upload"));
        file = new File(ASSAY_SINGLE_COLUMN_EXCEL_FILE);
        setFormElement(Locator.name("__primaryFile__"), file);
        clickButton("Save and Finish");
        assertNoLabKeyErrors();
        clickAndWait(Locator.linkWithText(ASSAY_EXCEL_RUN_SINGLE_COLUMN));
        validateSingleColumnData();

        log("Import from Excel in two-column format");
        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickButton("Import Data");
        selectOptionByText(Locator.xpath("//select[@name='targetStudy']"), targetStudyValue);

        clickButton("Next");
        setFormElement(Locator.name("name"), ASSAY_EXCEL_RUN_TWO_COLUMN);
        checkCheckbox(Locator.radioButtonByNameAndValue("dataCollectorName", "File upload"));
        file = new File(ASSAY_TWO_COLUMN_EXCEL_FILE_BAD);
        setFormElement(Locator.name("__primaryFile__"), file);
        clickButton("Save and Finish");
        assertLabKeyErrorPresent();

        checkCheckbox(Locator.radioButtonByNameAndValue("dataCollectorName", "File upload"));
        file = new File(ASSAY_TWO_COLUMN_EXCEL_FILE);
        setFormElement(Locator.name("__primaryFile__"), file);
        clickButton("Save and Finish");
        assertNoLabKeyErrors();
        clickAndWait(Locator.linkWithText(ASSAY_EXCEL_RUN_TWO_COLUMN));
        validateTwoColumnData("Data", "ParticipantID");
    }

    private void assertMvIndicatorPresent()
    {
        // We'd better have some 
        assertElementPresent(Locator.xpath("//img[@class='labkey-mv-indicator']"));
    }

    /**
     * Defines an test assay at the project level for the security-related tests
     */
    @SuppressWarnings({"UnusedAssignment"})
    private void defineAssay()
    {
        log("Defining a test assay at the project level");
        //define a new assay at the project level
        //the pipeline must already be setup
        clickProject(PROJECT_NAME);
        addWebPart("Assay List");

        //copied from old test
        clickButton("Manage Assays");
        clickButton("New Assay Design");
        checkCheckbox(Locator.radioButtonByNameAndValue("providerName", "General"));
        clickButton("Next");

        waitForElement(Locator.id("AssayDesignerName"), WAIT_FOR_JAVASCRIPT);

        setFormElement(Locator.id("AssayDesignerName"), ASSAY_NAME);

        int index = AssayTest.TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT;
        _listHelper.addField("Data Fields", index++, "age", "Age", ListHelper.ListColumnType.Integer);
        _listHelper.addField("Data Fields", index++, "sex", "Sex", ListHelper.ListColumnType.String);
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
        prepForPageLoad();
        clickButton("Delete", 0);
        assertAlert("Delete selected row" + (1 == rowCount ? "" : "s") + " from this dataset?");
        waitForPageToLoad();
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteDir(new File(getSampleRoot(), "assaydata"));
        deleteProject(getProjectName(), afterTest);
    }

    public java.util.List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }
}
