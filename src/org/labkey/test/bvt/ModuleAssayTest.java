/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

    protected void doCleanup() throws Exception
    {
        try
        {
            deleteProject(PROJECT_NAME);
            deleteFile(getTestTempDir());
//            ModuleUtil.deleteModule(MODULE_NAME);
        }
        catch (Throwable t) { }
    }

    protected void doTestSteps() throws Exception
    {
        log("Starting ModuleAssayTest");

//        deploySimpleAssayModule();
        checkModuleDeployed();
        setupProject();

        uploadRun("Run01", "sampledata/miniassay/data/run01.tsv");
        clickLinkWithText("Run01");
        clickLinkWithText("details");
        sleep(500); // wait for data to be fetched on client
        assertTextPresent("Monkey 1");
        assertTextPresent("1 Nov 2008 11:22:33");
        assertTextPresent("3.2");
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
        selectOptionByText("providerName", "simple");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_GWT);

        log("Setting up simple assay");
        selenium.type("//input[@id='AssayDesignerName']", ASSAY_NAME);
        selenium.type("//textarea[@id='AssayDesignerDescription']", "My Simple Assay Description");

        sleep(1000);
        clickNavButton("Save", 0);
        waitForText("Save successful.", 20000);
    }

    protected void uploadRun(String runName, String uploadedFile)
    {
        log("Uploading run: " + runName);
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(ASSAY_NAME);

        clickNavButton("Import Data");
        // skip run set properties for now
        clickNavButton("Next");
        setFormElement("name", runName);
        setFormElement("TextAreaDataCollector.textArea", getFileContents(uploadedFile));
        clickNavButton("Save and Finish");
    }
}
