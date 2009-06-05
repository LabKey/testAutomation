/*
 * Copyright (c) 2009 LabKey Corporation
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
package org.labkey.test.daily;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.io.File;
import java.io.FilenameFilter;

/**
 * User: jgarms
 * Date: May 19, 2009
 */
public class IDRIParticleSizeTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "Particle Size";

    private static final String DATA_DIRECTORY = "c:/temp/IDRI";

    private static final String FORMULATIONS_DATA = "Batch\tDM\tBatchSize\tNB_Pg\tAdjuvant\tSqualene_Oil\tPC\tFailure\tUsedInExperiments\n" +
            "TD100\t1-Jan-2004\t25ml\t150-1\tMPL\tShark - Sigma\tEgg - Avanti\t\t\n" +
            "TD101\t1-Jan-2005\t50ml\t150-2\tMPL\tShark - Sigma\tEgg - Avanti\t\t\n";

    /**
     * Assumes that you already have a project named "Particle Size" with a particle size assay definition, and that
     * it has a run list web part on its portal page. It also assumes that you have defined a Formulations sample set.
     *
     * It iterates through all input files and upload them, and moving them to subdirectories based on the result.
     */
    protected void doTestSteps() throws Exception
    {
//        defineProject();
//        clickLinkWithText("IDRI Particle Size Assay");
        clickLinkWithText("Particle Size Database");
        clickButtonContainingText("Import Data");

        File root = new File(DATA_DIRECTORY);
        File emptyDir = new File(root, "empty");
        File errorDir = new File(root, "error");
        File successDir = new File(root, "success");

        emptyDir.mkdirs();
        errorDir.mkdirs();
        successDir.mkdirs();


        // Look for TDxxx.xls files
        File[] allFiles = root.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.matches("^TD[0-9]+\\.xls");
            }
        });

        for(File file : allFiles)
        {
            log("uploading " + file.getName());
            setFormElement("upload-run-field-file", file);
            sleep(2500);
            if (isMaterialPopupVisible())
            {
                // if we don't have any material, submit an empty entry
                click(getButtonLocator("Submit"));
            }
            int seconds = 0;
            while (!isTextPresent(file.getName()) && seconds < 10)
            {
                seconds++;
                sleep(1000);
            }
            
            if (isTextPresent("The data file " + file.getName() + " contains no rows") ||
                isTextPresent("Failure when communicating with the server: Data file contained zero data rows"))
            {
                click(getButtonLocator("OK"));
                file.renameTo(new File(emptyDir, file.getName()));
            }
            else if (isTextPresent("Failure when communicating with the server"))
            {
                click(getButtonLocator("OK"));
                file.renameTo(new File(errorDir, file.getName()));
            }
            else
            {
                assertTextPresent(file.getName());
                file.renameTo(new File(successDir, file.getName()));
            }
            
            clickNavButton("Done");

            clickLinkWithText("Import Data");
            sleep(1000);
        }

        clickButtonContainingText("Done");

        for (File file : allFiles)
        {
            assertTextPresent(file.getName());
        }

        log("Excel files uploaded");
    }

    private boolean isMaterialPopupVisible()
    {
        String divClass = selenium.getEval("this.browserbot.getCurrentWindow().document.getElementById('material').className");
        return !divClass.equals("x-hidden");
    }

    protected void doCleanup() throws Exception
    {
        // IMPORTANT: don't do this in production!
//        deleteProject(PROJECT_NAME);
    }

    private void defineProject() throws Exception
    {
        if (isTextPresent(PROJECT_NAME))
        {
            clickLinkWithText(PROJECT_NAME);
            return;
        }
        createProject(PROJECT_NAME);
        enableModule(PROJECT_NAME, "ParticleSize");
        enableModule(PROJECT_NAME, "Experiment");
        enableModule(PROJECT_NAME, "Pipeline");

        setupPipeline();
        addWebPart("Assay List");
        addWebPart("Sample Sets");

        log("Import formulations");
        clickButtonContainingText("Import Sample Set");
        selenium.type("name","Formulations");
        selenium.type("textbox", FORMULATIONS_DATA);
        submit();

        log("Create assay design");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Manage Assays");

        clickButtonContainingText("New Assay Design");
        click(Locator.xpath("//input[@type='radio' and @value='IDRI Particle Size']"));

        clickButtonContainingText("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_GWT);
        selenium.type("AssayDesignerName", "IDRI Particle Size Assay");
        clickButtonContainingText("Save & Close");
    }

    public String getAssociatedModuleDirectory()
    {
        return "none";
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    private void setupPipeline()
    {
        addWebPart("Data Pipeline");
        clickNavButton("Setup");
        File dir = getTestTempDir();
        dir.mkdirs();

        setFormElement("path", dir.getAbsolutePath());
        clickNavButton("Set");

        //make sure it was set
        assertTextPresent("The pipeline root was set to '" + dir.getAbsolutePath() + "'.");
        clickLinkWithText(PROJECT_NAME);
    }
}
