/*
 * Copyright (c) 2008-2012 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ListHelper;

/**
 * User: Erik
 * Date: Jun 23, 2008
 * Time: 4:20:38 PM
 */
public class MicroarrayTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "MicroarrayBVTProject";
    private static final String EXTRACTION_SERVER = "http://www.google.com";
    private static final String ASSAY_NAME = "Test Assay 1";
    private static final String IMPORT_MAGEML = "Import MAGE-ML";
    private static final String ASSAY_DESCRIPTION = "Test Assay 1 Description";
    private static final String MAGEML_FILE1 = "test1_MAGEML.xml";
    private static final String MAGEML_FILE2 = "test2_MAGEML.xml";
    private static final String BATCH_STRING_FIELD = "BatchStringField";
    private static final String RUN_STRING_FIELD = "RunStringField";
    private static final String RUN_INTEGER_FIELD = "RunIntegerField";
    private static final String XPATH_TEST = "/MAGE-ML/Descriptions_assnlist/Description/Annotations_assnlist/OntologyEntry[@category='Producer']/@value";
    private static final String DATA_FIELD_TEST_NAME = "TestDataField1";
    private static final String SAMPLE_SET = "Test Sample Set";   
    private static final String SAMPLE_SET_ROWS = "Name\tBarcode\n" +
            "First\t251379110131_A01\n" +
            "Second\t251379110131_A01\n" +
            "Third\t\n" +
            "Fourth\t\n";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/microarray";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected void doCleanup(boolean afterTest) throws Exception
    {
        deleteProject(getProjectName(), afterTest);
    }

    protected void doTestSteps()
    {
        log("Create Project");
        log("Create Project");
        _containerHelper.createProject(PROJECT_NAME, null);
        goToFolderManagement();
        clickLinkWithText("Folder Type");
        checkRadioButton(Locator.radioButtonByNameAndValue("folderType", "Microarray"));
        submit();

        log("Create an assay");
        clickButton("Manage Assays");
        clickButton("New Assay Design");
        checkRadioButton("providerName", "Microarray");
        clickButton("Next");
        waitForElement(Locator.raw("//td[contains(text(), 'Name')]/..//td/input"), defaultWaitForPage);
        setFormElement(Locator.raw("//td[contains(text(), 'Name')]/..//td/input"), ASSAY_NAME);
        setFormElement(Locator.raw("//td[contains(text(), 'Description')]/..//td/textarea"), ASSAY_DESCRIPTION);
        addField("Batch Fields", 0, BATCH_STRING_FIELD, BATCH_STRING_FIELD, ListHelper.ListColumnType.String);
        addField("Run Fields", 0, RUN_STRING_FIELD, RUN_STRING_FIELD, ListHelper.ListColumnType.String);
        setFormElement("//td[contains(text(), 'Run Fields')]/../..//td/textarea[@id='propertyDescription']", XPATH_TEST);
        addField("Run Fields", 1, RUN_INTEGER_FIELD, RUN_INTEGER_FIELD, ListHelper.ListColumnType.Integer);
        addField("Data Properties", 0, DATA_FIELD_TEST_NAME, DATA_FIELD_TEST_NAME, ListHelper.ListColumnType.String);
        clickButton("Save", 0);
        waitForText("Save successful.", 20000);
        clickButton("Save & Close");
        
        log("Setup the pipeline");

        setPipelineRoot(getLabKeyRoot() + "/sampledata/Microarray");
        assertTextPresent("The pipeline root was set to");
        clickLinkWithText("Microarray Dashboard");

        log("Create Sample Set");
        addWebPart("Sample Sets");
        clickButton("Import Sample Set");
        setFormElement("name", SAMPLE_SET);
        setFormElement("data", SAMPLE_SET_ROWS);
        submit();

        // First try importing the runs individually
        clickLinkWithText("Microarray Dashboard");
        clickButton("Process and Import Data");

        _extHelper.waitForImportDataEnabled();
        _extHelper.clickFileBrowserFileCheckbox(MAGEML_FILE1);
        _extHelper.clickFileBrowserFileCheckbox(MAGEML_FILE2);

        selectImportDataAction("Use " + ASSAY_NAME);

        setFormElement("batchStringField", "SingleRunProperties");
        clickButton("Next");
        assertTextPresent(MAGEML_FILE1);
        waitForElement(Locator.raw("//div[contains(text(), 'Sample 1')]/../..//tr/td/select"), defaultWaitForPage);
        waitForElement(Locator.raw("//option[contains(text(), 'Second')]"), defaultWaitForPage);
        setFormElement("runIntegerField", "115468001");
        clickButton("Save and Import Next File");

        log("Import second run");
        waitForElement(Locator.raw("//div[contains(text(), 'Sample 2')]/../..//tr/td/select"), defaultWaitForPage);
        assertTextPresent(MAGEML_FILE2);
        setFormElement("runIntegerField", "115468002");
        selectOptionByText("//div[contains(text(), 'Sample 1')]/../..//tr/td/select", "Third");
        selectOptionByText("//div[contains(text(), 'Sample 2')]/../..//tr/td/select", "Fourth");
        clickButton("Save and Finish");
        waitForText(ASSAY_NAME + " Runs", 30000);
        assertTextPresent("SingleRunProperties");

        validateRuns();

        // Now try doing the runs in bulk, so delete the existing runs
        checkAllOnPage("Runs");
        clickButton("Delete");
        clickButton("Confirm Delete");

        // Start the upload wizard again
        clickButton("Import Data");

        _extHelper.clickFileBrowserFileCheckbox(MAGEML_FILE1);
        _extHelper.clickFileBrowserFileCheckbox(MAGEML_FILE2);

        selectImportDataAction("Use " + ASSAY_NAME);

        setFormElement("batchStringField", "BulkProperties");
        
        assertFormElementEquals("batchStringField", "BulkProperties");
        checkRadioButton("__enableBulkProperties", "on");
        // Try with an invalid sample name first
        setFormElement(Locator.name("__bulkProperties"), "Barcode\tProbeID_Cy3\tProbeID_Cy5\t" + RUN_STRING_FIELD + "\t" + RUN_INTEGER_FIELD + "\n" +
                "251379110131_A01\tBogusSampleName!!\tSecond\tFirstString\t11\n" +
                "251379110137_A01\tThird\tFourth\tSecondString\t22\n");
        clickButton("Next");
        assertTextPresent("No sample with name 'BogusSampleName!!' was found");

        // Try with invalid sample set name
        setFormElement(Locator.name("__bulkProperties"), "Barcode\tProbeID_Cy3\tProbeID_Cy5\t" + RUN_STRING_FIELD + "\t" + RUN_INTEGER_FIELD + "\n" +
                "251379110131_A01\tBogus__SampleSetName.First\tSecond\tFirstString\t11\n" +
                "251379110137_A01\tBogus__SampleSetName.Third\tFourth\tSecondString\t22\n");
        clickButton("Next");
        assertTextPresent("No sample with name 'Bogus__SampleSetName.First' was found");

        // Try with incorrect barcodes
        setFormElement(Locator.name("__bulkProperties"), "Barcode\tProbeID_Cy3\tProbeID_Cy5\t" + RUN_STRING_FIELD + "\t" + RUN_INTEGER_FIELD + "\n" +
                "FakeBarcode_A01\t" + SAMPLE_SET + ".First\t" + SAMPLE_SET + ".Second\tFirstString\t11\n" +
                "251379110137_A01\t" + SAMPLE_SET + ".Third\t" + SAMPLE_SET + ".Fourth\tSecondString\t22\n");
        clickButton("Next");
        assertTextPresent("Could not find a row for barcode '251379110131_A01'");

        // Try with incorrect property type
        setFormElement(Locator.name("__bulkProperties"), "Barcode\tProbeID_Cy3\tProbeID_Cy5\t" + RUN_STRING_FIELD + "\t" + RUN_INTEGER_FIELD + "\n" +
                "251379110131_A01\t" + SAMPLE_SET + ".First\t" + SAMPLE_SET + ".Second\tFirstString\t11\n" +
                "251379110137_A01\t" + SAMPLE_SET + ".Third\t" + SAMPLE_SET + ".Fourth\tSecondString\t22a\n");
        clickButton("Next");
        assertTextPresent(RUN_INTEGER_FIELD + " must be of type Integer");

        // Try with the wrong sample colum names
        setFormElement(Locator.name("__bulkProperties"), "Barcode\tProbeID_Cy3a\tProbeID_Cy5a\t" + RUN_STRING_FIELD + "\t" + RUN_INTEGER_FIELD + "\n" +
                "251379110131_A01\t" + SAMPLE_SET + ".First\t" + SAMPLE_SET + ".Second\tFirstString\t11\n" +
                "251379110137_A01\t" + SAMPLE_SET + ".Third\t" + SAMPLE_SET + ".Fourth\tSecondString\t22\n");
        clickButton("Next");
        assertTextPresent("Could not find a 'ProbeID_Cy3' column for sample information.");

        // Try with the wrong number of samples
        setFormElement(Locator.name("__bulkProperties"), "Barcode\tProbeID_Cy3\tProbeID_Cy5\t" + RUN_STRING_FIELD + "\t" + RUN_INTEGER_FIELD + "\n" +
                "251379110131_A01\t\t\tFirstString\t11\n" +
                "251379110137_A01\t" + SAMPLE_SET + ".Third\t" + SAMPLE_SET + ".Fourth\tSecondString\t22\n");
        clickButton("Next");
        assertTextPresent("No sample information specified for 'ProbeID_Cy3'");

        // Try the same submit again to make sure the form was repopulated
        clickButton("Next");
        assertTextPresent("No sample information specified for 'ProbeID_Cy3'");

        // Finally do it with the right info
        setFormElement(Locator.name("__bulkProperties"), "Barcode\tProbeID_Cy3\tProbeID_Cy5\t" + RUN_STRING_FIELD + "\t" + RUN_INTEGER_FIELD + "\n" +
                "251379110131_A01\t" + SAMPLE_SET + ".First\t" + SAMPLE_SET + ".Second\tFirstString\t115468001\n" +
                "251379110137_A01\t" + SAMPLE_SET + ".Third\t" + SAMPLE_SET + ".Fourth\tSecondString\t115468002\n");
        clickButton("Next");

        validateRuns();
    }

    private void validateRuns()
    {
        log("Test run inputs");
        clickLinkWithText(MAGEML_FILE1);
        assertTextPresent("115468001");
        clickLinkWithText("First");
        assertTextPresent(MAGEML_FILE1);
        assertTextNotPresent(MAGEML_FILE2);
        assertTextPresent(SAMPLE_SET);

        log("Test run outputs/ data files");
        clickTab("Microarray Dashboard");
        clickLinkWithText(ASSAY_NAME);
        clickLinkWithText(MAGEML_FILE2);
        assertTextPresent("115468002");
        clickLink(Locator.raw("//a[contains(text(), '" + MAGEML_FILE2 + "')]/../..//td/a[contains(text(), 'view')]"));
        waitForText(ASSAY_NAME + " Description", 30000);
        assertTextPresent(DATA_FIELD_TEST_NAME);
        clickLinkWithText("view results");
        waitForText(ASSAY_NAME + " Description", 30000);
        assertTextPresent(DATA_FIELD_TEST_NAME);

        log("Test graph views");
        clickLinkWithText("Microarray Dashboard");
        clickLinkWithText(MAGEML_FILE1);
        clickLinkWithText("Graph Summary View");
        clickLinkWithText("Graph Detail View");

        log("Test assay view");
        clickLinkWithText(ASSAY_NAME);
        assertTextPresent(ASSAY_NAME + " Protocol");
        clickLinkWithText("Microarray Dashboard");
        clickLinkWithText("Assay List");
        clickLinkWithText(ASSAY_NAME);
        assertTextPresent("Agilent Feature Extraction Software");
    }
}
