/*
 * Copyright (c) 2008-2010 LabKey Corporation
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
package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ListHelper;

import java.io.File;

/*
* User: Jess Garms
* Date: Jan 16, 2009
*/
public class MissingValueIndicatorsTest extends BaseSeleniumWebTest
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
        return getLabKeyRoot() + "/sampledata/mvIndicators/";
    }

    protected void doTestSteps() throws Exception
    {
        log("Create MV project");
        createProject(PROJECT_NAME, "Study");
        clickNavButton("Create Study");
        selectOptionByValue("securityString", "BASIC_WRITE");
        clickNavButton("Create Study");
        clickLinkWithText(PROJECT_NAME + " Study");
        clickLinkWithText("Manage Files");
        clickNavButton("Setup");
        setFormElement("path", getSampleRoot());
        submit();

        setupIndicators();

        checkList();
        checkDataset();
        checkAssay();
    }

    private void setupIndicators() throws InterruptedException
    {
        log("Setting MV indicators");
        
        clickLinkWithText("Folder Settings");
        clickLinkWithText("Missing Value Indicators");
        clickCheckboxById("inherit");

        // Delete all site-level settings
        while(isElementPresent(Locator.tagWithAttribute("img", "alt", "delete")))
        {
            click(Locator.tagWithAttribute("img", "alt", "delete"));
            Thread.sleep(500);
        }

        click(getButtonLocator("Add"));
        click(getButtonLocator("Add"));
        click(getButtonLocator("Add"));
        Thread.sleep(500);

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
                selenium.type(xpathString, mvIndicator);
            }
            index++;
        }

        clickNavButton("Save");

        log("Set MV indicators.");
    }

    private void checkList() throws Exception
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

        ListHelper.createList(this, PROJECT_NAME, LIST_NAME, ListHelper.ListColumnType.AutoInteger, "Key", columns);

        log("Test upload list data with a combined data and MVI column");
        ListHelper.clickImportData(this);
        setFormElement("ff_data", TEST_DATA_SINGLE_COLUMN_LIST_BAD);
        submit();
        assertLabkeyErrorPresent();

        setFormElement("ff_data", TEST_DATA_SINGLE_COLUMN_LIST);
        submit();
        validateSingleColumnData();

        deleteListData();        

        log("Test inserting a single new row");
        clickNavButton("Insert New");
        setFormElement("quf_name", "Sid");
        setFormElement("quf_sex", "male");
        selectOptionByValue("quf_ageMVIndicator", "Z");
        submit();
        assertNoLabkeyErrors();
        assertTextPresent("Sid");
        assertTextPresent("male");
        assertTextPresent("N");

        deleteListData();

        log("Test separate MVIndicator column");
        clickNavButton("Import Data");
        setFormElement("ff_data", TEST_DATA_TWO_COLUMN_LIST_BAD);
        submit();
        assertLabkeyErrorPresent();

        setFormElement("ff_data", TEST_DATA_TWO_COLUMN_LIST);
        submit();
        validateTwoColumnData("query", "name");
    }

    private void deleteListData()
    {
        checkCheckbox(".toggle");
        selenium.chooseOkOnNextConfirmation();
        clickButton("Delete", 0);
        assertEquals(selenium.getConfirmation(), "Are you sure you want to delete the selected rows?");
        waitForPageToLoad();
    }

    private void checkDataset() throws Exception
    {
        log("Create dataset");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Define Dataset Schemas");
        clickLinkWithText("Bulk Import Schemas");
        setFormElement("typeNameColumn", "datasetName");
        setFormElement("labelColumn", "datasetLabel");
        setFormElement("typeIdColumn", "datasetId");
        setLongTextField("tsv", getFileContents(DATASET_SCHEMA_FILE));
        clickNavButton("Submit", 180000);
        assertNoLabkeyErrors();
        assertTextPresent("MV Dataset");

        log("Import dataset data");
        clickLinkWithText("MV Dataset");
        clickNavButton("View Data");
        clickNavButton("Import Data");

        setFormElement("tsv", TEST_DATA_SINGLE_COLUMN_DATASET_BAD);
        submit();
        assertLabkeyErrorPresent();
        submit();

        setFormElement("tsv", TEST_DATA_SINGLE_COLUMN_DATASET);
        submit();
        validateSingleColumnData();

        deleteDatasetData();

        log("Test inserting a single row");
        clickNavButton("Insert New");
        setFormElement("quf_ParticipantId", "Sid");
        setFormElement("quf_SequenceNum", "1");
        selectOptionByValue("quf_AgeMVIndicator", "Z");
        setFormElement("quf_Sex", "male");
        submit();
        assertNoLabkeyErrors();
        assertTextPresent("Sid");
        assertTextPresent("male");
        assertTextPresent("N");

        deleteDatasetData();

        log("Import dataset data with two mv columns");
        clickNavButton("Import Data");

        setFormElement("tsv", TEST_DATA_TWO_COLUMN_DATASET_BAD);
        submit();
        assertLabkeyErrorPresent();

        setFormElement("tsv", TEST_DATA_TWO_COLUMN_DATASET);
        submit();
        validateTwoColumnData("Dataset", "ParticipantId");
    }

    private void validateSingleColumnData()
    {
        assertNoLabkeyErrors();
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
        assertNoLabkeyErrors();
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
        selenium.click("//img[@class='labkey-mv-indicator']/../../a");
        assertTextPresent("'25'");
    }

    private void checkAssay()
    {
        log("Create assay");
        defineAssay();

        log("Import single column MV data");
        clickLinkWithText(ASSAY_NAME);
        clickNavButton("Import Data");
        String targetStudyValue = "/" + PROJECT_NAME + " (" + PROJECT_NAME + " Study)";
        selenium.select("//select[@name='targetStudy']", targetStudyValue);

        clickNavButton("Next");
        selenium.type("name", ASSAY_RUN_SINGLE_COLUMN);
        selenium.click("//input[@value='textAreaDataProvider']");

        selenium.type("TextAreaDataCollector.textArea", TEST_DATA_SINGLE_COLUMN_ASSAY_BAD);
        clickNavButton("Save and Finish");
        assertLabkeyErrorPresent();

        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_DATA_SINGLE_COLUMN_ASSAY);
        clickNavButton("Save and Finish");
        assertNoLabkeyErrors();
        clickLinkWithText(ASSAY_RUN_SINGLE_COLUMN);
        validateSingleColumnData();

        log("Import two column MV data");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(ASSAY_NAME);
        clickNavButton("Import Data");
        selenium.select("//select[@name='targetStudy']", targetStudyValue);

        clickNavButton("Next");
        selenium.type("name", ASSAY_RUN_TWO_COLUMN);

        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_DATA_TWO_COLUMN_ASSAY_BAD);
        clickNavButton("Save and Finish");
        assertLabkeyErrorPresent();

        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_DATA_TWO_COLUMN_ASSAY);
        clickNavButton("Save and Finish");
        assertNoLabkeyErrors();
        clickLinkWithText(ASSAY_RUN_TWO_COLUMN);
        validateTwoColumnData("MVAssay Data", "Properties/ParticipantID");

        log("Copy to study");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(ASSAY_NAME);
        clickLinkWithText(ASSAY_RUN_SINGLE_COLUMN);
        validateSingleColumnData();
        checkCheckbox(".toggle");
        clickNavButton("Copy to Study");
        
        clickNavButton("Next");

        clickNavButton("Copy to Study");
        validateSingleColumnData();

        if (isFileUploadAvailable())
        {
            log("Import from Excel in single-column format");
            clickLinkWithText(PROJECT_NAME);
            clickLinkWithText(ASSAY_NAME);
            clickNavButton("Import Data");
            selenium.select("//select[@name='targetStudy']", targetStudyValue);

            clickNavButton("Next");
            selenium.type("name", ASSAY_EXCEL_RUN_SINGLE_COLUMN);
            checkRadioButton("dataCollectorName", "File upload");

            File file = new File(ASSAY_SINGLE_COLUMN_EXCEL_FILE_BAD);
            setFormElement("__primaryFile__", file);
            clickNavButton("Save and Finish");
            assertLabkeyErrorPresent();

            checkRadioButton("dataCollectorName", "File upload");
            file = new File(ASSAY_SINGLE_COLUMN_EXCEL_FILE);
            setFormElement("__primaryFile__", file);
            clickNavButton("Save and Finish");
            assertNoLabkeyErrors();
            clickLinkWithText(ASSAY_EXCEL_RUN_SINGLE_COLUMN);
            validateSingleColumnData();

            log("Import from Excel in two-column format");
            clickLinkWithText(PROJECT_NAME);
            clickLinkWithText(ASSAY_NAME);
            clickNavButton("Import Data");
            selenium.select("//select[@name='targetStudy']", targetStudyValue);

            clickNavButton("Next");
            selenium.type("name", ASSAY_EXCEL_RUN_TWO_COLUMN);
            checkRadioButton("dataCollectorName", "File upload");
            file = new File(ASSAY_TWO_COLUMN_EXCEL_FILE_BAD);
            setFormElement("__primaryFile__", file);
            clickNavButton("Save and Finish");
            assertLabkeyErrorPresent();

            checkRadioButton("dataCollectorName", "File upload");
            file = new File(ASSAY_TWO_COLUMN_EXCEL_FILE);
            setFormElement("__primaryFile__", file);
            clickNavButton("Save and Finish");
            assertNoLabkeyErrors();
            clickLinkWithText(ASSAY_EXCEL_RUN_TWO_COLUMN);
            validateTwoColumnData("MVAssay Data", "Properties/ParticipantID");
        }
    }

    private void assertMvIndicatorPresent()
    {
        // We'd better have some 
        assertElementPresent(Locator.xpath("//img[@class='labkey-mv-indicator']"));
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
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
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Assay List");

        //copied from old test
        clickNavButton("Manage Assays");
        clickNavButton("New Assay Design");
        checkRadioButton("providerName", "General");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        selenium.type("//input[@id='AssayDesignerName']", ASSAY_NAME);

        int index = AssayTest.TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT;
        addField("Data Fields", index++, "age", "Age", ListHelper.ListColumnType.Integer);
        addField("Data Fields", index++, "sex", "Sex", ListHelper.ListColumnType.String);
        sleep(1000);

        log("setting fields to enable missing values");
        ListHelper.clickRow(this, getPropertyXPath("Data Fields"), 4);
        ListHelper.clickMvEnabled(this, getPropertyXPath("Data Fields")); 

        ListHelper.clickRow(this, getPropertyXPath("Data Fields"), 5);
        ListHelper.clickMvEnabled(this, getPropertyXPath("Data Fields"));

        clickNavButton("Save & Close");
        assertNoLabkeyErrors();

    }

    private void deleteDatasetData()
    {
        checkCheckbox(".toggle");
        selenium.chooseOkOnNextConfirmation();
        clickButton("Delete", 0);
        assertEquals(selenium.getConfirmation(), "Delete selected rows of this dataset?");
        waitForPageToLoad();
    }

    protected void doCleanup() throws Exception
    {
        try
        {
            deleteProject(PROJECT_NAME);
        }
        catch (Throwable t)
        {
            //
        }

        deleteDir(new File(getSampleRoot(), "assaydata"));
    }

    public String getAssociatedModuleDirectory()
    {
        return "experiment";
    }
}
