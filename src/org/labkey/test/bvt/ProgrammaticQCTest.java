/*
 * Copyright (c) 2009-2010 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.ListHelper;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: Apr 6, 2009
 * Time: 4:39:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProgrammaticQCTest extends AbstractQCAssayTest
{
    protected final static String TEST_PROGRAMMATIC_QC_PRJ = "Programmatic QC Test";
    protected final static String QC_ASSAY = "QC Assay";
    protected final static String TRANSFORM_ASSAY = "Transform Assay";
    protected final static String TRANSFORM_QC_ASSAY = "Transform & QC Assay";

    private final ListHelper.ListColumn _listCol1 = new ListHelper.ListColumn("Date", "Date", ListHelper.ListColumnType.DateTime, "date");
    private final ListHelper.ListColumn _listCol2 = new ListHelper.ListColumn("Container", "Container", ListHelper.ListColumnType.String, "container path");
    private final ListHelper.ListColumn _listCol3 = new ListHelper.ListColumn("AssayId", "AssayId", ListHelper.ListColumnType.String, "assay id");
    private final ListHelper.ListColumn _listCol4 = new ListHelper.ListColumn("AssayName", "AssayName", ListHelper.ListColumnType.String, "assay name");
    private final ListHelper.ListColumn _listCol5 = new ListHelper.ListColumn("User", "User", ListHelper.ListColumnType.String, "user");
    private final ListHelper.ListColumn _listCol6 = new ListHelper.ListColumn("Comments", "Comments", ListHelper.ListColumnType.String, "run comments");

    protected static final String TEST_ASSAY_DATA_PROP_NAME = "testAssayDataProp";
    public static final int TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT = 4;
    protected static final ListHelper.ListColumnType[] TEST_ASSAY_DATA_PROP_TYPES = { ListHelper.ListColumnType.Boolean, ListHelper.ListColumnType.Integer, ListHelper.ListColumnType.DateTime };

    protected static final String TEST_RUN1_DATA1 = "specimenID\tparticipantID\tvisitID\t" + TEST_ASSAY_DATA_PROP_NAME + "20\t" + TEST_ASSAY_DATA_PROP_NAME + "5\t" + TEST_ASSAY_DATA_PROP_NAME + "6\n" +
            "s1\ta\t1\ttrue\t20\t2000-01-01\n" +
            "s2\tb\t2\ttrue\t19\t2000-02-02\n" +
            "s3\tb\t3\ttrue\t18\t2000-03-03\n" +
            "s4\td\t4\tfalse\t17\t2000-04-04\n" +
            "s5\te\t5\tfalse\t16\t2000-05-05\n" +
            "s5\te\t5\tfalse\t16\t2000-05-05\n" +
            "s6\tf\t6\tfalse\t15\t2000-06-06";
    protected static final String TEST_RUN1_DATA2 = "specimenID\tparticipantID\tvisitID\t" + TEST_ASSAY_DATA_PROP_NAME + "4\t" + TEST_ASSAY_DATA_PROP_NAME + "5\t" + TEST_ASSAY_DATA_PROP_NAME + "6\n" +
            "s1\ta\t1\ttrue\t20\t2000-01-01\n" +
            "s2\tb\t2\ttrue\t19\t2000-02-02\n" +
            "s3\tc\t3\ttrue\t18\t2000-03-03\n" +
            "s4\td\t4\tfalse\t17\t2000-04-04\n" +
            "s5\te\t5\tfalse\t16\t2000-05-05\n" +
            "s6\tf\t6\tfalse\t15\t2000-06-06";
    protected static final String TEST_RUN1_DATA3 = "specimenID\tparticipantID\tvisitID\t" + TEST_ASSAY_DATA_PROP_NAME + "4\t" + TEST_ASSAY_DATA_PROP_NAME + "5\t" + TEST_ASSAY_DATA_PROP_NAME + "6\n" +
            "s1\ta\t1\ttrue\t20\t2000-01-01\n" +
            "s2\tb\t2\ttrue\t19\t2000-02-02\n" +
            "s3\tc\t3\ttrue\t18\t2000-03-03\n" +
            "s4\td\t4\tfalse\t17\t2000-04-04\n" +
            "s5\te\t5\tfalse\t16\t2000-05-05\n" +
            "s6\tf\t4\tfalse\t17\t2000-04-04\n" +
            "s7\tg\t5\tfalse\t16\t2000-05-05\n" +
            "s8\th\t6\tfalse\t15\t2000-06-06";

    protected void runUITests() throws Exception
    {
        prepareProgrammaticQC();

        createProject(TEST_PROGRAMMATIC_QC_PRJ);
        setupPipeline(TEST_PROGRAMMATIC_QC_PRJ);

        defineQCAssay();
        uploadQCRuns();

        defineTransformAssay(TRANSFORM_ASSAY, false);
        uploadTransformRuns();

        // define and run an assay with both a transform and QC validator
        defineTransformAssay(TRANSFORM_QC_ASSAY, true);
        uploadTransformQCRuns();
    }

    protected void doCleanup() throws Exception
    {
        try {
            deleteEngine();
            deleteProject(TEST_PROGRAMMATIC_QC_PRJ);
        }
        catch (Throwable t) {}
    }

    private void defineQCAssay()
    {
        log("Defining a QC test assay at the project level");

        clickLinkWithText(TEST_PROGRAMMATIC_QC_PRJ);
        addWebPart("Assay List");

        clickNavButton("Manage Assays");
        clickNavButton("New Assay Design");
        checkRadioButton("providerName", "General");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        selenium.type("//input[@id='AssayDesignerName']", QC_ASSAY);

        addValidationScript(new File(WebTestHelper.getLabKeyRoot(), "/sampledata/qc/validator.jar"));

        for (int i = TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT; i < TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT + TEST_ASSAY_DATA_PROP_TYPES.length; i++)
        {
            addField("Data Fields", i, TEST_ASSAY_DATA_PROP_NAME + i, TEST_ASSAY_DATA_PROP_NAME + i, TEST_ASSAY_DATA_PROP_TYPES[i - TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT]);
        }
        sleep(1000);
        clickNavButton("Save", 0);
        waitForText("Save successful.", 20000);

        // create the list for the qc log
        ListHelper.createList(this, TEST_PROGRAMMATIC_QC_PRJ, "QC Log", ListHelper.ListColumnType.AutoInteger, "Key", _listCol1, _listCol2,
                _listCol3, _listCol4, _listCol5, _listCol6);
    }

    private void defineTransformAssay(String assayName, boolean addQCScript)
    {
        log("Defining a transform test assay at the project level");

        clickLinkWithText(TEST_PROGRAMMATIC_QC_PRJ);
        addWebPart("Assay List");

        clickNavButton("Manage Assays");
        clickNavButton("New Assay Design");
        checkRadioButton("providerName", "General");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        selenium.type("//input[@id='AssayDesignerName']", assayName);

        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), "/sampledata/qc/transform.jar"));
        if (addQCScript)
            addValidationScript(new File(WebTestHelper.getLabKeyRoot(), "/sampledata/qc/validator.jar"));

        for (int i = TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT; i < TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT + TEST_ASSAY_DATA_PROP_TYPES.length; i++)
        {
            addField("Data Fields", i, TEST_ASSAY_DATA_PROP_NAME + i, TEST_ASSAY_DATA_PROP_NAME + i, TEST_ASSAY_DATA_PROP_TYPES[i - TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT]);
        }

        // add an 'animal' field which will be populated by the transform script
        addField("Data Fields", TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT + TEST_ASSAY_DATA_PROP_TYPES.length, "Animal", "Animal", ListHelper.ListColumnType.String);

        sleep(1000);
        clickNavButton("Save", 0);
        waitForText("Save successful.", 20000);
    }

    private void uploadQCRuns()
    {
        log("uploading runs");
        clickLinkWithText(TEST_PROGRAMMATIC_QC_PRJ);
        clickLinkWithText("Assay List");
        clickLinkWithText(QC_ASSAY);

        clickNavButton("Import Data");
        clickNavButton("Next");

        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_RUN1_DATA1);
        clickNavButton("Save and Finish");

        assertTextPresent("A duplicate PTID was discovered : b");
        assertTextPresent("A duplicate PTID was discovered : e");

        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_RUN1_DATA2);
        clickNavButton("Save and Finish");

        // verify the log entry
        clickLinkWithText(TEST_PROGRAMMATIC_QC_PRJ);
        clickLinkWithText("QC Log");

        assertTextPresent("Programmatic QC was run and 2 errors were found");
        assertTextPresent("Programmatic QC was run and 0 errors were found");
    }

    private void uploadTransformRuns()
    {
        log("uploading transform runs");
        clickLinkWithText(TEST_PROGRAMMATIC_QC_PRJ);
        clickLinkWithText("Assay List");
        clickLinkWithText(TRANSFORM_ASSAY);

        clickNavButton("Import Data");
        clickNavButton("Next");

        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_RUN1_DATA1);
        clickNavButton("Save and Finish");

        assertTextPresent("A duplicate PTID was discovered : b");
        assertTextPresent("A duplicate PTID was discovered : e");

        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_RUN1_DATA2);
        clickNavButton("Save and Finish");

        clickLinkWithText("view results");

        assertTextPresent("monkey");
        assertTextPresent("hamster");

        // verify the log entry
        clickLinkWithText(TEST_PROGRAMMATIC_QC_PRJ);
        clickLinkWithText("QC Log");

        assertTextPresent("Programmatic Data Transform was run and 2 errors were found");
        assertTextPresent("Programmatic Data Transform was run and 0 errors were found");
    }

    private void uploadTransformQCRuns()
    {
        log("uploading transform & QC runs");
        clickLinkWithText(TEST_PROGRAMMATIC_QC_PRJ);
        clickLinkWithText("Assay List");
        clickLinkWithText(TRANSFORM_QC_ASSAY);

        clickNavButton("Import Data");
        clickNavButton("Next");

        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_RUN1_DATA2);
        clickNavButton("Save and Finish");

        assertTextPresent("The animal column must contain a goat");

        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_RUN1_DATA3);
        clickNavButton("Save and Finish");

        clickLinkWithText("view results");

        assertTextPresent("monkey");
        assertTextPresent("hamster");
        assertTextPresent("goat");

        // verify the log entry
        clickLinkWithText(TEST_PROGRAMMATIC_QC_PRJ);
        clickLinkWithText("QC Log");

        assertTextPresent("Programmatic QC was run and 1 errors were found");
    }

    public String getAssociatedModuleDirectory()
    {
        return "query";
    }
}
