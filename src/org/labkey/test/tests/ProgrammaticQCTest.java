/*
 * Copyright (c) 2009-2017 LabKey Corporation
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
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebDriverException;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({DailyA.class})
public class ProgrammaticQCTest extends AbstractQCAssayTest
{
    protected final static String TEST_PROGRAMMATIC_QC_PRJ = "Programmatic QC Test";
    protected final static String QC_ASSAY = "QC Assay";
    protected final static String TRANSFORM_ASSAY = "Transform Assay";
    protected final static String TRANSFORM_QC_ASSAY = "Transform & QC Assay";

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

    @Test
    public void runUITests() throws Exception
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
        _containerHelper.deleteProject(getProjectName(), afterTest);
        try {deleteEngine();}
        catch (WebDriverException t) {}
    }

    private void defineQCAssay()
    {
        log("Defining a QC test assay at the project level");

        clickAndWait(Locator.linkWithText(TEST_PROGRAMMATIC_QC_PRJ));
        new PortalHelper(this).addWebPart("Assay List");

        _assayHelper.uploadXarFileAsAssayDesign(TestFileUtils.getSampleData("ProgrammaticQC/QC Assay.xar"), 1);

        goToProjectHome();
        clickAndWait(Locator.linkContainingText("QC Assay"));
        _assayHelper.clickEditAssayDesign();
        AssayDesignerPage assayDesigner = new AssayDesignerPage(this.getDriver());
        assayDesigner.addTransformScript((TestFileUtils.getSampleData("qc/validator.jar")));
        assayDesigner.saveAndClose();
        goToProjectHome();
        _listHelper.importListArchive(getProjectName(), TestFileUtils.getSampleData("ProgrammaticQC/Programmatic QC.lists.zip"));
    }

    private void defineTransformAssay(String assayName, boolean addQCScript)
    {
        log("Defining a transform test assay at the project level");

        clickAndWait(Locator.linkWithText(TEST_PROGRAMMATIC_QC_PRJ));
        new PortalHelper(this).addWebPart("Assay List");

        clickButton("Manage Assays");
        clickButton("New Assay Design");
        checkCheckbox(Locator.radioButtonByNameAndValue("providerName", "General"));
        clickButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        setFormElement(Locator.xpath("//input[@id='AssayDesignerName']"), assayName);

        AssayDesignerPage assayDesigner = new AssayDesignerPage(this.getDriver());
        assayDesigner.addTransformScript(new File(TestFileUtils.getLabKeyRoot(), "/sampledata/qc/transform.jar"));
        if (addQCScript)
        {
            assayDesigner.addTransformScript(new File(TestFileUtils.getLabKeyRoot(), "/sampledata/qc/validator.jar"));
        }

        for (int i = TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT; i < TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT + TEST_ASSAY_DATA_PROP_TYPES.length; i++)
        {
            _listHelper.addField("Data Fields", TEST_ASSAY_DATA_PROP_NAME + i, TEST_ASSAY_DATA_PROP_NAME + i, TEST_ASSAY_DATA_PROP_TYPES[i - TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT]);
        }

        // add an 'animal' field which will be populated by the transform script
        _listHelper.addField("Data Fields", "Animal", "Animal", ListHelper.ListColumnType.String);

        sleep(1000);
        clickButton("Save & Close");
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

        assertTextPresent("A duplicate PTID was discovered : b", "A duplicate PTID was discovered : e");

        click(Locator.xpath("//input[@value='textAreaDataProvider']"));
        setFormElement(Locator.id("TextAreaDataCollector.textArea"), TEST_RUN1_DATA2);
        clickButton("Save and Finish");

        // verify the log entry
        clickAndWait(Locator.linkWithText(TEST_PROGRAMMATIC_QC_PRJ));
        clickAndWait(Locator.linkWithText("QC Log"));

        assertTextPresent("Programmatic QC was run and 2 errors were found", "Programmatic QC was run and 0 errors were found");
    }

    private void uploadTransformRuns()
    {
        log("uploading transform runs");
        clickAndWait(Locator.linkWithText(TRANSFORM_ASSAY));

        clickButton("Import Data");
        clickButton("Next");

        click(Locator.xpath("//input[@value='textAreaDataProvider']"));
        setFormElement(Locator.id("TextAreaDataCollector.textArea"), TEST_RUN1_DATA1);
        clickButton("Save and Finish");

        assertTextPresent("A duplicate PTID was discovered : b", "A duplicate PTID was discovered : e");

        click(Locator.xpath("//input[@value='textAreaDataProvider']"));
        setFormElement(Locator.id("TextAreaDataCollector.textArea"), TEST_RUN1_DATA2);
        clickButton("Save and Finish");

        clickAndWait(Locator.linkWithText("view results"));

        assertTextPresent("monkey", "hamster");

        // verify the log entry
        clickAndWait(Locator.linkWithText(TEST_PROGRAMMATIC_QC_PRJ));
        clickAndWait(Locator.linkWithText("QC Log"));

        assertTextPresent("Programmatic Data Transform was run and 2 errors were found",
                "Programmatic Data Transform was run and 0 errors were found");
    }

    private void uploadTransformQCRuns()
    {
        log("uploading transform & QC runs");
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

        assertTextPresent("monkey", "hamster", "goat");

        // verify the log entry
        clickAndWait(Locator.linkWithText(TEST_PROGRAMMATIC_QC_PRJ));
        clickAndWait(Locator.linkWithText("QC Log"));

        assertTextPresent("Programmatic QC was run and 1 errors were found");
    }

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("query");
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
