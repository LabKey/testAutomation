/*
 * Copyright (c) 2008-2009 LabKey Corporation
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
import org.labkey.test.util.ListHelper;

/*
* User: Jess Garms
* Date: Jan 16, 2009
*/
public class FieldLevelQcTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "FieldLevelQcVerifyProject";
    private static final String LIST_NAME = "QCList";

    private static final String TEST_DATA_SINGLE_COLUMN_QC_LIST =
            "Name" + "\t" + "Age" + "\t"  + "Sex" + "\n" +
            "Ted" + "\t" + ".N" + "\t" + "male" + "\n" +
            "Alice" + "\t" + "17" + "\t" + "female" + "\n" +
            "Bob" + "\t" + ".Q" + "\t" + ".N" + "\n";

    private static final String TEST_DATA_TWO_COLUMN_QC_LIST =
            "Name" +    "\t" + "Age" +  "\t" + "AgeQCIndicator" +   "\t" + "Sex" +  "\t" + "SexQCIndicator" + "\n" +
            "Franny" +  "\t" + "" +     "\t" + ".N" +               "\t" + "male" + "\t" +  "" + "\n" +
            "Zoe" +     "\t" + "25" +   "\t" + ".Q" +               "\t" + "female" +     "\t" +  "" + "\n" +
            "J.D." +    "\t" + "50" +   "\t" + "" +                 "\t" + "male" + "\t" +  ".Q" + "\n";

    private static final String TEST_DATA_SINGLE_COLUMN_QC_DATASET = 
            "participantid\tSequenceNum\tAge\tSex\n" +
            "Ted\t1\t.N\tmale\n" +
            "Alice\t1\t17\tfemale\n" +
            "Bob\t1\t.Q\t.N";

    private static final String TEST_DATA_TWO_COLUMN_QC_DATASET =
            "participantid\tSequenceNum\tAge\tAgeQCIndicator\tSex\tSexQCIndicator\n" +
            "Franny\t1\t\t.N\tmale\t\n" +
            "Zoe\t1\t25\t.Q\tfemale\t\n" +
            "J.D.\t1\t50\t\tmale\t.Q";

    private static final String DATASET_SCHEMA_FILE = "/sampledata/fieldLevelQC/dataset_schema.tsv";

    protected void doTestSteps() throws Exception
    {
        log("Create QC project");
        createProject(PROJECT_NAME, "Study");
        clickNavButton("Done");
        clickNavButton("Create Study");
        selectOptionByValue("securityString", "BASIC_WRITE");
        clickNavButton("Create Study");
        clickLinkWithText(PROJECT_NAME + " Study");
        clickLinkWithText("Data Pipeline");
        clickNavButton("Setup");
        setFormElement("path", getLabKeyRoot() + "/sampledata/fieldLevelQC");
        submit();

        checkListQc();
        checkDatasetQc();
    }

    private void checkListQc() throws Exception
    {
        log("Create list");

        ListHelper.ListColumn[] columns = new ListHelper.ListColumn[3];

        ListHelper.ListColumn listColumn = new ListHelper.ListColumn("name", "Name", ListHelper.ListColumnType.String, "");
        columns[0] = listColumn;

        listColumn = new ListHelper.ListColumn("age", "Age", ListHelper.ListColumnType.Integer, "");
        listColumn.setAllowsQc(true);
        columns[1] = listColumn;

        listColumn = new ListHelper.ListColumn("sex", "Sex", ListHelper.ListColumnType.String, "");
        listColumn.setAllowsQc(true);
        columns[2] = listColumn;

        ListHelper.createList(this, PROJECT_NAME, LIST_NAME, ListHelper.ListColumnType.AutoInteger, "Key", columns);

        log("Test upload list data with a combined data and QC column");
        clickLinkWithText("import data");
        setFormElement("ff_data", TEST_DATA_SINGLE_COLUMN_QC_LIST);
        submit();
        assertNoLabkeyErrors();
        assertTextPresent("Ted");
        assertTextPresent("Alice");
        assertTextPresent("Bob");
        assertTextPresent(".Q");
        assertTextPresent(".N");
        assertTextPresent("male");
        assertTextPresent("female");
        assertTextPresent("17");

        deleteListData();        

        log("Test inserting a single new row");
        clickNavButton("Insert New");
        setFormElement("quf_name", "Sid");
        setFormElement("quf_sex", "male");
        setFormElement("quf_age", ".N");
        submit();
        assertNoLabkeyErrors();
        assertTextPresent("Sid");
        assertTextPresent("male");
        assertTextPresent(".N");

        deleteListData();

        log("Test separate QCIndicator column");
        clickNavButton("Import Data");
        setFormElement("ff_data", TEST_DATA_TWO_COLUMN_QC_LIST);
        submit();
        assertNoLabkeyErrors();
        assertTextPresent("Franny");
        assertTextPresent("Zoe");
        assertTextPresent("J.D.");
        assertTextPresent(".Q");
        assertTextPresent(".N");
        assertTextPresent("male");
        assertTextPresent("female");
        assertTextPresent("50");
        assertTextPresent("25");
    }

    private void deleteListData()
    {
        checkCheckbox(".toggle");
        clickButton("Delete", defaultWaitForPage);
    }

    private void checkDatasetQc() throws Exception
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
        assertTextPresent("QC Dataset");

        log("Import dataset data");
        clickLinkWithText("QC Dataset");
        clickNavButton("Import Data");

        setFormElement("tsv", TEST_DATA_SINGLE_COLUMN_QC_DATASET);
        submit();
        assertNoLabkeyErrors();
        assertTextPresent("Ted");
        assertTextPresent("Alice");
        assertTextPresent("Bob");
        assertTextPresent(".Q");
        assertTextPresent(".N");
        assertTextPresent("male");
        assertTextPresent("female");
        assertTextPresent("17");

        deleteDatasetData();

        log("Test inserting a single row");
        clickNavButton("Insert New");
        setFormElement("quf_participantid", "Sid");
        setFormElement("quf_SequenceNum", "1");
        setFormElement("quf_Age", ".N");
        setFormElement("quf_Sex", "male");
        submit();
        assertNoLabkeyErrors();
        assertTextPresent("Sid");
        assertTextPresent("male");
        assertTextPresent(".N");

        deleteDatasetData();

        log("Import dataset data with two qc columns");
        clickNavButton("Import Data");
        setFormElement("tsv", TEST_DATA_TWO_COLUMN_QC_DATASET);
        submit();
        assertNoLabkeyErrors();
        assertTextPresent("Franny");
        assertTextPresent("Zoe");
        assertTextPresent("J.D.");
        assertTextPresent(".Q");
        assertTextPresent(".N");
        assertTextPresent("male");
        assertTextPresent("female");
        assertTextPresent("50");
        assertTextPresent("25");
    }

    private void deleteDatasetData()
    {
        clickButton("Delete All Rows", defaultWaitForPage);
        selenium.getConfirmation();
    }

    protected void doCleanup() throws Exception
    {
        try {
            deleteProject(PROJECT_NAME);
        }
        catch (Throwable t) {}
    }

    public String getAssociatedModuleDirectory()
    {
        return "experiment";
    }
}
