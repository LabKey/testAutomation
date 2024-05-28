/*
 * Copyright (c) 2011-2019 LabKey Corporation
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
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.assay.ImportRunResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.APIAssayHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.QCAssayScriptHelper;
import org.openqa.selenium.WebDriverException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 7)
public class ProgrammaticQCTest extends AbstractAssayTest
{
    protected final static String TEST_PROGRAMMATIC_QC_PRJ = "Programmatic QC Test";
    protected final static String QC_ASSAY = "QC Assay";
    protected final static String TRANSFORM_ASSAY = "Transform Assay";
    protected final static String TRANSFORM_QC_ASSAY = "Transform & QC Assay";

    protected static final String TEST_ASSAY_DATA_PROP_NAME = "testAssayDataProp";
    public static final int TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT = 4;
    protected static final FieldDefinition.ColumnType[] TEST_ASSAY_DATA_PROP_TYPES = { FieldDefinition.ColumnType.Boolean, FieldDefinition.ColumnType.Integer, FieldDefinition.ColumnType.DateAndTime };

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
        new QCAssayScriptHelper(this).ensureEngineConfig();

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

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        try {
            new QCAssayScriptHelper(this).deleteEngine();
        }
        catch (WebDriverException t) {}
    }

    private void defineQCAssay()
    {
        log("Defining a QC test assay at the project level");

        goToProjectHome(TEST_PROGRAMMATIC_QC_PRJ);
        new PortalHelper(this).addWebPart("Assay List");

        _assayHelper.uploadXarFileAsAssayDesign(TestFileUtils.getSampleData("ProgrammaticQC/QC Assay.xar"), 1);

        goToProjectHome();
        clickAndWait(Locator.linkContainingText("QC Assay"));

        ReactAssayDesignerPage assayDesigner = _assayHelper.clickEditAssayDesign();
        assayDesigner.addTransformScript(TestFileUtils.getSampleData("qc/validator.jar"));
        assayDesigner.clickFinish();
        goToProjectHome();
        _listHelper.importListArchive(getProjectName(), TestFileUtils.getSampleData("ProgrammaticQC/Programmatic QC.lists.zip"));
    }

    private void defineTransformAssay(String assayName, boolean addQCScript)
    {
        log("Defining a transform test assay at the project level");

        goToProjectHome(TEST_PROGRAMMATIC_QC_PRJ);
        new PortalHelper(this).addWebPart("Assay List");

        ReactAssayDesignerPage assayDesigner = _assayHelper.createAssayDesign("General", assayName);

        assayDesigner.addTransformScript(TestFileUtils.getSampleData("qc/transform.jar"));
        if (addQCScript)
        {
            assayDesigner.addTransformScript(TestFileUtils.getSampleData("qc/validator.jar"));
        }

        for (int i = TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT; i < TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT + TEST_ASSAY_DATA_PROP_TYPES.length; i++)
        {
            assayDesigner.goToResultsFields().addField(new FieldDefinition (TEST_ASSAY_DATA_PROP_NAME + i,TEST_ASSAY_DATA_PROP_TYPES[i - TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT])
                    .setLabel(TEST_ASSAY_DATA_PROP_NAME + i));
        }

        // add an 'animal' field which will be populated by the transform script
        assayDesigner.goToResultsFields()
            .addField(new FieldDefinition( "Animal", FieldDefinition.ColumnType.String).setLabel("Animal"));
        assayDesigner.clickFinish();
    }

    private void uploadQCRuns()
    {
        log("uploading runs");
        goToProjectHome(TEST_PROGRAMMATIC_QC_PRJ);
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
        goToProjectHome(TEST_PROGRAMMATIC_QC_PRJ);
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
        goToProjectHome(TEST_PROGRAMMATIC_QC_PRJ);
        clickAndWait(Locator.linkWithText("QC Log"));

        assertTextPresent("Programmatic Data Transform was run and 2 errors were found",
                "Programmatic Data Transform was run and 0 errors were found");


        APIAssayHelper ah = new APIAssayHelper(this);
        int assayId = ah.getIdFromAssayName(TRANSFORM_ASSAY, TEST_PROGRAMMATIC_QC_PRJ);

        // assay-importRun.api via dataRows, expect error
        try
        {
            List<Map<String, Object>> dataRows = dataRows(TEST_RUN1_DATA1);
            ImportRunResponse resp = ah.importAssay(assayId, "importRuns.api with dataRows", dataRows, TEST_PROGRAMMATIC_QC_PRJ, null, null);
            fail("Expected importRows.api to throw a validation exception");
        }
        catch (CommandException e)
        {
            log(e.getResponseText());
            assertEquals("A duplicate PTID was discovered : b", e.getMessage());
        }
        catch (IOException e)
        {
            fail(e.getMessage());
        }

        // assay-importRun.api via dataRows, successfully
        try
        {
            List<Map<String, Object>> dataRows = dataRows(TEST_RUN1_DATA2);
            ImportRunResponse resp = ah.importAssay(assayId, "importRuns.api with dataRows", dataRows, TEST_PROGRAMMATIC_QC_PRJ, null, null);
            int runId = resp.getRunId();
            assertTrue("Expected to insert a run", runId > 0);

            beginAt(resp.getSuccessURL());
            assertTextPresent("monkey", "hamster");
        }
        catch (CommandException | IOException e)
        {
            fail(e.getMessage());
        }
    }

    private void uploadTransformQCRuns()
    {
        log("uploading transform & QC runs");
        goToProjectHome(TEST_PROGRAMMATIC_QC_PRJ);
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

        assertTextPresent("monkey", "hamster", "goat");

        // verify the log entry
        goToProjectHome(TEST_PROGRAMMATIC_QC_PRJ);
        clickAndWait(Locator.linkWithText("QC Log"));

        assertTextPresent("Programmatic QC was run and 1 errors were found");


        APIAssayHelper ah = new APIAssayHelper(this);
        int assayId = ah.getIdFromAssayName(TRANSFORM_QC_ASSAY, TEST_PROGRAMMATIC_QC_PRJ);

        // assay-importRun.api via dataRows, expect error
        try
        {
            List<Map<String, Object>> dataRows = dataRows(TEST_RUN1_DATA2);
            ImportRunResponse resp = ah.importAssay(assayId, "importRuns.api with dataRows", dataRows, TEST_PROGRAMMATIC_QC_PRJ, null, null);
            fail("Expected importRows.api to throw a validation exception");
        }
        catch (CommandException e)
        {
            log(e.getResponseText());
            assertEquals("The animal column must contain a goat", e.getMessage());
        }
        catch (IOException e)
        {
            fail(e.getMessage());
        }

        // assay-importRun.api via dataRows, successfully
        try
        {
            List<Map<String, Object>> dataRows = dataRows(TEST_RUN1_DATA3);
            ImportRunResponse resp = ah.importAssay(assayId, "importRuns.api with dataRows", dataRows, TEST_PROGRAMMATIC_QC_PRJ, null, null);
            int runId = resp.getRunId();
            assertTrue("Expected to insert a run", runId > 0);

            beginAt(resp.getSuccessURL());
            assertTextPresent("monkey", "hamster", "goat");
        }
        catch (CommandException | IOException e)
        {
            fail(e.getMessage());
        }
    }


    // turn the test tsv data into a set of row maps
    private List<Map<String, Object>> dataRows(String tsv)
    {
        List<Map<String, Object>> dataRows = new ArrayList<>();

        String[] tsvRows = tsv.split("\n");
        String[] headers = tsvRows[0].split("\t");
        for (int i = 1; i < tsvRows.length; i++)
        {
            String[] values = tsvRows[i].split("\t");

            Map<String, Object> rowMap = new HashMap<>();
            for (int col = 0; col < headers.length; col++)
            {
                String header = headers[col];
                String value = values[col];
                rowMap.put(header, value);
            }
            dataRows.add(rowMap);
        }

        return dataRows;
    }

    @Override
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
