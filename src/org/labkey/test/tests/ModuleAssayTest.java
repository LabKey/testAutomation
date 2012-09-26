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

//import org.labkey.test.ModuleUtil;
import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;

import java.io.File;

/**
 * User: kevink
 * Date: Dec 28, 2008
 */
public class ModuleAssayTest extends AbstractAssayTest
{
    private final static String PROJECT_NAME = "ModuleAssayTest";
    private static final String MODULE_NAME = "miniassay";
    private static final String ASSAY_NAME = "My Simple Assay";

    private static final String SAMPLE_SET = "Test Sample Set";
    private static final String SAMPLE_SET_ROWS = "Name\tBarcode\n" +
            "First\t251379110131_A01\n" +
            "Second\t251379110131_A01\n" +
            "Third\t\n" +
            "Fourth\t\n";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void doCleanup() throws Exception
    {
        try{ deleteProject(PROJECT_NAME); }catch (Throwable t) {/*ignore*/}

        deleteDir(getTestTempDir());
    }

    protected void runUITests() throws Exception
    {
        if (!isFileUploadAvailable())
            return;

        log("Starting ModuleAssayTest");

        checkModuleDeployed();
        setupProject();

        String batchName = "First Batch";
        uploadBatch(batchName, "run01.tsv", "run02.tsv");

        log("Visit batches page");
        clickLinkWithText(ASSAY_NAME + " Batches");
        assertTitleEquals(ASSAY_NAME + " Batches: /" + PROJECT_NAME);

        // Verify file-based view associated with assay design shows up, with expected columns
        clickMenuButton("Views", "AssayDesignBatches");
        assertTextPresent("Modified", "Created By");
        assertTextNotPresent("First Batch", "Target Study", "Target Study", "Run Count");

        // Verify file-based view associated with assay type shows up, with expected columns
        clickMenuButton("Views", "AssayTypeBatches");
        assertTextPresent("First Batch", "Created By");
        assertTextNotPresent("Modified", "Run Count");

        clickMenuButton("Views", "default");

        log("Visit batch details page");
        clickLinkWithText("details");
        assertTitleEquals(batchName + " Details: /" + PROJECT_NAME);
        waitForElement(Locator.id("RunName_0"), WAIT_FOR_JAVASCRIPT);
        assertElementContains(Locator.id("RunName_0"), "run01");
        assertElementContains(Locator.id("SampleId_0_0"), "Monkey 1");
        assertElementContains(Locator.id("DoubleData_0_0"), String.valueOf(3.2));
        assertElementContains(Locator.id("RunName_1"), "run02");
        assertElementContains(Locator.id("SampleId_1_2"), "Monkey 3");
        assertElementContains(Locator.id("DoubleData_1_2"), String.valueOf(1.5));

        log("Visit runs page");
        // back to batches page
        selenium.goBack();
        waitForPageToLoad();
        clickLinkWithText(batchName);
        assertTitleEquals(ASSAY_NAME + " Runs: /" + PROJECT_NAME);

        // Verify file-based view associated with assay design shows up, with expected columns
        clickMenuButton("Views", "AssayDesignRuns");
        assertTextPresent("Created By", "Modified");
        assertTextNotPresent("Assay Id", "MetaOverride Double Run", "Run Count", "run01.tsv", "run02.tsv");

        // Verify file-based view associated with assay type shows up, with expected columns
        clickMenuButton("Views", "AssayTypeRuns");
        assertTextPresent("Assay Id", "Created By", "MetaOverride Double Run", "run01.tsv", "run02.tsv");
        assertTextNotPresent("Modified", "Run Count");

        clickMenuButton("Views", "default");

        log("Visit run details page");
        setSort(ASSAY_NAME + " Runs", "Name", SortDirection.ASC);
        clickLinkWithText("details");
        assertTitleEquals("run01.tsv Details: /" + PROJECT_NAME);
        assertElementContains(Locator.id("SampleId_0"), "Monkey 1");
        assertElementContains(Locator.id("DoubleData_0"), String.valueOf(3.2));

        log("Visit results page");
        // back to runs page
        selenium.goBack();
        waitForPageToLoad();
        clickLinkWithText("run01.tsv");
        assertTitleEquals(ASSAY_NAME + " Results: /" + PROJECT_NAME);

                // Verify file-based view associated with assay design shows up, with expected columns
        clickMenuButton("Views", "AssayDesignData");
        assertTextPresent("Created By", "Modified");
        assertTextNotPresent("Sample Id", "Monkey 1", "Monkey 2", "MetaOverride Double Run", "Run Count");

        // Verify file-based view associated with assay type shows up, with expected columns
        clickMenuButton("Views", "AssayTypeData");
        assertTextPresent("Sample Id", "Monkey 1", "Monkey 2", "Time Point", "Double Data", "Assay Id", "run01.tsv");
        assertTextNotPresent("MetaOverride Double Run", "Run Count", "Target Study");

        clickMenuButton("Views", "default");

        log("Visit result details page");
        clickLinkWithText("details");
        assertElementContains(Locator.id("SampleId_div"), "Monkey 1");
        assertElementContains(Locator.id("TimePoint_div"), "2008/11/01 11:22:33");
        assertElementContains(Locator.id("DoubleData_div"), String.valueOf(3.2));
        assertElementContains(Locator.id("HiddenData_div"), "super secret!");


        log("Issue 13404: Test assay query metadata is available when module is active or inactive");
        selenium.goBack();
        waitForPageToLoad();
        clickLinkContainingText("view runs");
        assertTextPresent("Simple Assay Button");
        pushLocation();
        enableModule(PROJECT_NAME, "miniassay");
        popLocation();
        clickButton("Simple Assay Button", 0);
        assertAlert("button clicked");
    }

    protected void checkModuleDeployed()
    {
        log("Checking miniassay module is deployed");
        goToAdminConsole();
        assertTextPresent(MODULE_NAME);
    }

    protected void setupProject()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        enableModule("miniassay", true);
        setupPipeline(PROJECT_NAME);
        createAssayDesign();
        createSampleSet();
    }

    protected void createAssayDesign()
    {
        log("Creating assay design");
        clickLinkWithText(PROJECT_NAME);

        addWebPart("Assay List");
        clickButton("Manage Assays");
        clickButton("New Assay Design");
        checkRadioButton("providerName", "Noblis Simple");
        clickButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        log("Setting up simple assay");
        selenium.type("//input[@id='AssayDesignerName']", ASSAY_NAME);
        selenium.type("//textarea[@id='AssayDesignerDescription']", "My Simple Assay Description");

        // Make the ParticipantViewResolver field not required
        setRequired("Batch Fields", 0);

        sleep(1000);
        clickButton("Save", 0);
        waitForText("Save successful.", 20000);
    }

    protected void createSampleSet()
    {
        log("Creating sample set");
        clickLinkWithText(PROJECT_NAME);

        addWebPart("Sample Sets");
        clickButton("Import Sample Set");
        setFormElement("name", SAMPLE_SET);
        setFormElement("data", SAMPLE_SET_ROWS);
        submit();
    }

    protected void uploadBatch(String batchName, String... uploadedFiles)
    {
        File dataRoot = new File(getLabKeyRoot(), "/sampledata/miniassay/data");
        Assert.assertTrue(dataRoot.isDirectory());

        log("Uploading batch: " + batchName);
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(ASSAY_NAME);

        clickButton("Import Data");
        assertTitleEquals("Data Import: /" + PROJECT_NAME);

        setFormElement("batch_name_input", batchName);

        setFormElement("batch_comment_input", batchName + " comments...");

        clickButton("Save", 0);

        // check name and comments stuck
        Assert.assertEquals(batchName, getFormElement("batch_name_input"));
        Assert.assertEquals(batchName + " comments...", getFormElement("batch_comment_input"));

        for (int i = 0; i < uploadedFiles.length; i++)
        {
            String uploadedFile = uploadedFiles[i];
            File file = new File(dataRoot, uploadedFile);
            Assert.assertTrue(file.exists());
            setFormElement("upload-run-field-file", file);
            int count = 5;
            do
            {
                sleep(1500);
                String runCountStr = selenium.getText(Locator.id("batch_runCount_div").toString());
                if (runCountStr != null && !runCountStr.equals("") && Integer.parseInt(runCountStr) == i+1)
                    break;
            } while (--count > 0);
            assertElementContains(Locator.id("batch_runCount_div"), String.valueOf(i+1));
            // file name should appear in runs grid on upload page
            waitForText(uploadedFile, WAIT_FOR_JAVASCRIPT);
        }
    }
}
