/*
 * Copyright (c) 2009-2013 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.ListHelperWD;

import java.io.*;

/**
 * User: klum
 * Date: Apr 6, 2009
 * Time: 4:39:49 PM
 */
@Category({DailyA.class})
public class ProgrammaticQCTest extends AbstractQCAssayTest
{
    protected final static String TEST_PROGRAMMATIC_QC_PRJ = "Programmatic QC Test";
    protected final static String QC_ASSAY = "QC Assay";
    protected final static String TRANSFORM_ASSAY = "Transform Assay";
    protected final static String TRANSFORM_QC_ASSAY = "Transform & QC Assay";

    protected static final String TEST_ASSAY_DATA_PROP_NAME = "testAssayDataProp";
    public static final int TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT = 4;
    protected static final ListHelperWD.ListColumnType[] TEST_ASSAY_DATA_PROP_TYPES = { ListHelperWD.ListColumnType.Boolean, ListHelperWD.ListColumnType.Integer, ListHelperWD.ListColumnType.DateTime };

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

        _containerHelper.createProject(TEST_PROGRAMMATIC_QC_PRJ, null);
        setupPipeline(TEST_PROGRAMMATIC_QC_PRJ);

        defineQCAssay();
        uploadQCRuns();

        defineTransformAssay(TRANSFORM_ASSAY, false);
        uploadTransformRuns();

        // define and run an assay with both a transform and QC validator
        defineTransformAssay(TRANSFORM_QC_ASSAY, true);
        uploadTransformQCRuns();
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
        try {deleteEngine();}
        catch (Throwable t) {}
    }

    private void defineQCAssay()
    {
        log("Defining a QC test assay at the project level");

        clickAndWait(Locator.linkWithText(TEST_PROGRAMMATIC_QC_PRJ));
        addWebPart("Assay List");

        _assayHelper.uploadXarFileAsAssayDesign(getSampledataPath() + "/ProgrammaticQC/QC Assay.xar", 1);

        goToProjectHome();
        clickAndWait(Locator.linkContainingText("QC Assay"));
        clickEditAssayDesign(false);
        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), "/sampledata/qc/validator.jar"), 0);
        clickButton("Save & Close");
        goToProjectHome();
        _listHelper.importListArchive(getProjectName(), new File(getSampledataPath(), "/ProgrammaticQC/Programmatic QC.lists.zip"));
    }

    private void defineTransformAssay(String assayName, boolean addQCScript)
    {
        log("Defining a transform test assay at the project level");

        clickAndWait(Locator.linkWithText(TEST_PROGRAMMATIC_QC_PRJ));
        addWebPart("Assay List");

        clickButton("Manage Assays");
        clickButton("New Assay Design");
        checkRadioButton("providerName", "General");
        clickButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        setFormElement(Locator.xpath("//input[@id='AssayDesignerName']"), assayName);

        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), "/sampledata/qc/transform.jar"), 0);
        if (addQCScript)
            addTransformScript(new File(WebTestHelper.getLabKeyRoot(), "/sampledata/qc/validator.jar"), 1);

        for (int i = TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT; i < TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT + TEST_ASSAY_DATA_PROP_TYPES.length; i++)
        {
            _listHelper.addField("Data Fields", i, TEST_ASSAY_DATA_PROP_NAME + i, TEST_ASSAY_DATA_PROP_NAME + i, TEST_ASSAY_DATA_PROP_TYPES[i - TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT]);
        }

        // add an 'animal' field which will be populated by the transform script
        _listHelper.addField("Data Fields", TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT + TEST_ASSAY_DATA_PROP_TYPES.length, "Animal", "Animal", ListHelperWD.ListColumnType.String);

        sleep(1000);
        clickButton("Save", 0);
        waitForText("Save successful.", 20000);
    }

    private void uploadQCRuns()
    {
        log("uploading runs");
        clickAndWait(Locator.linkWithText(TEST_PROGRAMMATIC_QC_PRJ));
        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(QC_ASSAY));

        clickButton("Import Data");
        clickButton("Next");

        click(Locator.xpath("//input[@value='textAreaDataProvider']"));
        setFormElement(Locator.id("TextAreaDataCollector.textArea"), TEST_RUN1_DATA1);
        clickButton("Save and Finish");

        assertTextPresent("A duplicate PTID was discovered : b");
        assertTextPresent("A duplicate PTID was discovered : e");

        click(Locator.xpath("//input[@value='textAreaDataProvider']"));
        setFormElement(Locator.id("TextAreaDataCollector.textArea"), TEST_RUN1_DATA2);
        clickButton("Save and Finish");

        // verify the log entry
        clickAndWait(Locator.linkWithText(TEST_PROGRAMMATIC_QC_PRJ));
        clickAndWait(Locator.linkWithText("QC Log"));

        assertTextPresent("Programmatic QC was run and 2 errors were found");
        assertTextPresent("Programmatic QC was run and 0 errors were found");
    }

    private void uploadTransformRuns()
    {
        log("uploading transform runs");
        clickAndWait(Locator.linkWithText(TEST_PROGRAMMATIC_QC_PRJ));
        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TRANSFORM_ASSAY));

        clickButton("Import Data");
        clickButton("Next");

        click(Locator.xpath("//input[@value='textAreaDataProvider']"));
        setFormElement(Locator.id("TextAreaDataCollector.textArea"), TEST_RUN1_DATA1);
        clickButton("Save and Finish");

        assertTextPresent("A duplicate PTID was discovered : b");
        assertTextPresent("A duplicate PTID was discovered : e");

        click(Locator.xpath("//input[@value='textAreaDataProvider']"));
        setFormElement(Locator.id("TextAreaDataCollector.textArea"), TEST_RUN1_DATA2);
        clickButton("Save and Finish");

        clickAndWait(Locator.linkWithText("view results"));

        assertTextPresent("monkey");
        assertTextPresent("hamster");

        // verify the log entry
        clickAndWait(Locator.linkWithText(TEST_PROGRAMMATIC_QC_PRJ));
        clickAndWait(Locator.linkWithText("QC Log"));

        assertTextPresent("Programmatic Data Transform was run and 2 errors were found");
        assertTextPresent("Programmatic Data Transform was run and 0 errors were found");
    }

    private void uploadTransformQCRuns()
    {
        log("uploading transform & QC runs");
        clickAndWait(Locator.linkWithText(TEST_PROGRAMMATIC_QC_PRJ));
        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TRANSFORM_QC_ASSAY));

        clickButton("Import Data");
        clickButton("Next");

        click(Locator.xpath("//input[@value='textAreaDataProvider']"));
        setFormElement(Locator.id("TextAreaDataCollector.textArea"), TEST_RUN1_DATA2);
        clickButton("Save and Finish");

        assertTextPresent("The animal column must contain a goat");

        click(Locator.xpath("//input[@value='textAreaDataProvider']"));
        setFormElement(Locator.id("TextAreaDataCollector.textArea"), TEST_RUN1_DATA3);
        clickButton("Save and Finish");

        clickAndWait(Locator.linkWithText("view results"));

        assertTextPresent("monkey");
        assertTextPresent("hamster");
        assertTextPresent("goat");

        // verify the log entry
        clickAndWait(Locator.linkWithText(TEST_PROGRAMMATIC_QC_PRJ));
        clickAndWait(Locator.linkWithText("QC Log"));

        assertTextPresent("Programmatic QC was run and 1 errors were found");
    }

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/query";
    }

    @Override
    protected String getProjectName()
    {
        return TEST_PROGRAMMATIC_QC_PRJ;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
