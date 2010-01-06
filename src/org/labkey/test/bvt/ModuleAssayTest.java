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

//import org.labkey.test.ModuleUtil;
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

    public String getAssociatedModuleDirectory()
    {
        return "study";
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void doCleanup() throws Exception
    {
        try
        {
            deleteProject(PROJECT_NAME);
//            ModuleUtil.deleteModule(MODULE_NAME);
        }
        catch (Throwable t) { }

        deleteDir(getTestTempDir());
    }

    protected void runUITests() throws Exception
    {
        if (!isFileUploadAvailable())
            return;

        log("Starting ModuleAssayTest");

//        deploySimpleAssayModule();
        checkModuleDeployed();
        setupProject();

        String batchName = "First Batch";
        uploadBatch(batchName, "run01.tsv", "run02.tsv");

        log("Visit batches page");
        clickLinkWithText(ASSAY_NAME + " Batches");
        assertTitleEquals(ASSAY_NAME + " Batches: /" + PROJECT_NAME);

        log("Visit batch details page");
        clickLinkWithText("details");
        assertTitleEquals(batchName + " Details: /" + PROJECT_NAME);
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

        log("Visit result details page");
        clickLinkWithText("details");
        assertElementContains(Locator.id("SampleId_div"), "Monkey 1");
        assertElementContains(Locator.id("TimePoint_div"), "1 Nov 2008 11:22:33");
        assertElementContains(Locator.id("DoubleData_div"), String.valueOf(3.2));
        assertElementContains(Locator.id("HiddenData_div"), "super secret!");

    }

//    protected void deploySimpleAssayModule() throws Exception
//    {
//        log("Deploying miniassay module");
//        File moduleBaseDir = new File(WebTestHelper.getLabKeyRoot(), "/sampledata/miniassay/module");
//        ModuleUtil.createModule(moduleBaseDir, MODULE_NAME);
//    }

    protected void checkModuleDeployed()
    {
        log("Checking miniassay module is deployed");
        clickLinkWithText("Admin Console");
        assertTextPresent(MODULE_NAME);
    }

    protected void setupProject()
    {
        createProject(PROJECT_NAME);
        setupPipeline(PROJECT_NAME);
        createAssayDesign();
    }

    protected void createAssayDesign()
    {
        log("Creating assay design");
        clickLinkWithText(PROJECT_NAME);

        addWebPart("Assay List");
        clickLinkWithText("Manage Assays");
        clickNavButton("New Assay Design");
        checkRadioButton("providerName", "Simple");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_GWT);

        log("Setting up simple assay");
        selenium.type("//input[@id='AssayDesignerName']", ASSAY_NAME);
        selenium.type("//textarea[@id='AssayDesignerDescription']", "My Simple Assay Description");

        sleep(1000);
        clickNavButton("Save", 0);
        waitForText("Save successful.", 20000);
    }

    protected void uploadBatch(String batchName, String... uploadedFiles)
    {
        File dataRoot = new File(getLabKeyRoot(), "/sampledata/miniassay/data");

        log("Uploading batch: " + batchName);
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(ASSAY_NAME);

        clickNavButton("Import Data");
        assertTitleEquals("Data Import: /" + PROJECT_NAME);

//        setFormElement("batch_name_input", batchName);
        selenium.typeKeys(Locator.id("batch_name_input").toString(), batchName + " "); // trims the last char for some reason
//        selenium.keyPress(Locator.id("batch_name_input").toString(), "\\13"); // enter

//        setFormElement("batch_comment_input", batchName + " comments...");
        selenium.typeKeys(Locator.id("batch_comment_input").toString(), batchName + " comment "); // trims the last char for some reason
//        selenium.keyPress(Locator.id("batch_comment_input").toString(), "\\13"); // enter

        clickButton("Save", 0);

        // check name and comments stuck
        assertEquals(batchName, getFormElement("batch_name_input"));
        // 7408 : expose batch comments column in JSON serialization
        //assertEquals(batchName + " comments...", getFormElement("batch_comment_input"));

        for (int i = 0; i < uploadedFiles.length; i++)
        {
            String uploadedFile = uploadedFiles[i];
            setFormElement("upload-run-field-file", new File(dataRoot, uploadedFile));
            int count = 5;
            do
            {
                sleep(1500);
                String runCountStr = selenium.getText(Locator.id("batch_runCount_div").toString());
                if (runCountStr != null && Integer.parseInt(runCountStr) == i+1)
                    break;
            } while (--count > 0);
            assertElementContains(Locator.id("batch_runCount_div"), String.valueOf(i+1));
            // file name should appear in runs grid on upload page
            assertTextPresent(uploadedFile);
        }
    }
}
