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
import org.labkey.test.categories.BVT;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;
import java.util.List;

@Category(BVT.class)
public class MicroarrayTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "MicroarrayBVTProject";
    private static final String EXTRACTION_SERVER = "http://www.google.com";
    private static final String ASSAY_NAME = "Agilent mRNA 1-Color Microarray v10.";
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

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("microarray");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }

    @Test
    public void testSteps()
    {
        log("Create Project");
        _containerHelper.createProject(PROJECT_NAME, "Microarray");

        log("Create an assay");
        clickButton("Manage Assays");
        clickButton("New Assay Design");
        checkRadioButton(Locator.radioButtonByNameAndValue("providerName", "Microarray"));
        clickButton("Next");
        waitForElement(Locator.xpath("//td[contains(text(), 'Name')]/..//td/input"), defaultWaitForPage);
        setFormElement(Locator.xpath("//td[contains(text(), 'Name')]/..//td/input"), ASSAY_NAME);
        setFormElement(Locator.xpath("//td[contains(text(), 'Description')]/..//td/textarea"), ASSAY_DESCRIPTION);
        _listHelper.addField("Batch Fields", BATCH_STRING_FIELD, BATCH_STRING_FIELD, ListHelper.ListColumnType.String);
        _listHelper.addField("Run Fields", RUN_STRING_FIELD, RUN_STRING_FIELD, ListHelper.ListColumnType.String);
        setFormElement(Locator.xpath("//td[contains(text(), 'Run Fields')]/../..//td/textarea[@id='propertyDescription']"), XPATH_TEST);
        _listHelper.addField("Run Fields", RUN_INTEGER_FIELD, RUN_INTEGER_FIELD, ListHelper.ListColumnType.Integer);
        _listHelper.addField("Data Properties", DATA_FIELD_TEST_NAME, DATA_FIELD_TEST_NAME, ListHelper.ListColumnType.String);
        clickButton("Save", 0);
        waitForText(20000, "Save successful.");
        clickButton("Save & Close");

        log("Setup the pipeline");

        setPipelineRoot(TestFileUtils.getLabKeyRoot() + "/sampledata/Microarray");
        assertTextPresent("The pipeline root was set to");
        clickAndWait(Locator.linkWithText("Microarray Dashboard"));

        log("Create Sample Set");
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Sample Sets");
        clickButton("Import Sample Set");
        setFormElement(Locator.id("name"), SAMPLE_SET);
        setFormElement(Locator.name("data"), SAMPLE_SET_ROWS);
        fireEvent(Locator.name("data"), SeleniumEvent.change);
        waitForFormElementToEqual(Locator.id("idCol1"), "0");
        clickButton("Submit");
        waitForElement(Locator.id("Material"));

        // First try importing the runs individually
        clickAndWait(Locator.linkWithText("Microarray Dashboard"));
        clickButton("Process and Import Data");

        _fileBrowserHelper.waitForImportDataEnabled();
        _fileBrowserHelper.checkFileBrowserFileCheckbox(MAGEML_FILE1);
        _fileBrowserHelper.checkFileBrowserFileCheckbox(MAGEML_FILE2);
        _fileBrowserHelper.selectImportDataAction("Use " + ASSAY_NAME);

        setFormElement(Locator.name("batchStringField"), "SingleRunProperties");
        clickButton("Next");
        assertTextPresent(MAGEML_FILE1);
        waitForElement(Locator.xpath("//div[contains(text(), 'Sample 1')]/../..//tr/td/select"));
        waitForElement(Locator.xpath("//option[contains(text(), 'Second')]"));
        setFormElement(Locator.name("runIntegerField"), "115468001");
        clickButton("Save and Import Next File");

        log("Import second run");
        waitForElement(Locator.xpath("//div[contains(text(), 'Sample 2')]/../..//tr/td/select"));
        assertTextPresent(MAGEML_FILE2);
        setFormElement(Locator.name("runIntegerField"), "115468002");
        selectOptionByText(Locator.xpath("//div[contains(text(), 'Sample 1')]/../..//tr/td/select"), "Third");
        selectOptionByText(Locator.xpath("//div[contains(text(), 'Sample 2')]/../..//tr/td/select"), "Fourth");
        clickButton("Save and Finish");
        waitForText(30000, ASSAY_NAME + " Runs");
        assertTextPresent("SingleRunProperties");

        verifyCanCreateAndSaveCustomView();
        validateRuns();

        // Now try doing the runs in bulk, so delete the existing runs
        checkAllOnPage("Runs");
        clickButton("Delete");
        clickButton("Confirm Delete");

        // Start the upload wizard again
        clickButton("Import Data");

        _fileBrowserHelper.checkFileBrowserFileCheckbox(MAGEML_FILE1);
        _fileBrowserHelper.checkFileBrowserFileCheckbox(MAGEML_FILE2);
        _fileBrowserHelper.selectImportDataAction("Use " + ASSAY_NAME);

        setFormElement(Locator.name("batchStringField"), "BulkProperties");

        assertFormElementEquals("batchStringField", "BulkProperties");
        checkRadioButton(Locator.radioButtonByNameAndValue("__enableBulkProperties", "on"));
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

        // Try with the wrong sample column names
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

    private void verifyCanCreateAndSaveCustomView()
    {
        //Issue 16934: Assay schema names too long for query.customview
        _customizeViewsHelper.openCustomizeViewPanel();
        String name = "unneeded view";
        _customizeViewsHelper.saveCustomView(name);
        assertElementNotPresent(Locator.tagWithClass("*", "labkey-error").withPredicate("string-length() > 0"));
        assertElementPresent(Locator.css(".labkey-dataregion-msg").withText("View: " + name));

        //Issue 16936: Microarray, Viability, Elispot, and other assays fail to find custom run view
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.closeCustomizeViewPanel();


    }


    private void validateRuns()
    {
        log("Test run inputs");
        clickAndWait(Locator.linkWithText(MAGEML_FILE1));
        assertTextPresent("115468001");
        clickAndWait(Locator.linkWithText("First"));
        assertTextPresent(MAGEML_FILE1);
        assertTextNotPresent(MAGEML_FILE2);
        assertTextPresent(SAMPLE_SET);

        log("Test run outputs/ data files");
        clickTab("Microarray Dashboard");
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickAndWait(Locator.linkWithText(MAGEML_FILE2));
        assertTextPresent("115468002");
        clickAndWait(Locator.xpath("//a[contains(text(), '" + MAGEML_FILE2 + "')]/../..//td/a[contains(text(), 'view')]"));
        waitForText(30000, ASSAY_DESCRIPTION);
        assertTextPresent(DATA_FIELD_TEST_NAME);
        clickAndWait(Locator.linkWithText("view results"));
        waitForText(30000, ASSAY_DESCRIPTION);
        assertTextPresent(DATA_FIELD_TEST_NAME);

        log("Test graph views");
        clickAndWait(Locator.linkWithText("Microarray Dashboard"));
        clickAndWait(Locator.linkWithText(MAGEML_FILE1));
        clickAndWait(Locator.linkWithText("Graph Summary View"));
        clickAndWait(Locator.linkWithText("Graph Detail View"));

        log("Test assay view");
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        assertTextPresent(ASSAY_NAME + " Protocol");
        clickAndWait(Locator.linkWithText("Microarray Dashboard"));
        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        assertTextPresent("Agilent Feature Extraction Software");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
