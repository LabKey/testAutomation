/*
 * Copyright (c) 2011-2012 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.ListHelper;

import java.io.File;

/**
 * User: kevink
 * Date: Feb 23, 2011
 */
public abstract class AbstractViabilityTest extends AbstractQCAssayTest
{
    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/viability";
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected abstract String getProjectName();
    protected abstract String getFolderName();
    protected abstract String getAssayName();

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
        try{deleteEngine();}
        catch(Throwable T) {}

        deleteDir(getTestTempDir());
    }

    protected void initializeStudyFolder(String... tabs)
    {
        log("** Initialize Folder");
        if (!isLinkPresentWithText(getProjectName()))
            _containerHelper.createProject(getProjectName(), null);
        createSubfolder(getProjectName(), getProjectName(), getFolderName(), "Study", tabs, true);
        
        log("** Create Study");
        clickFolder(getFolderName());
        clickButton("Create Study");
        clickButton("Create Study");
    }


    protected void importSpecimens()
    {
        importSpecimens(getFolderName(), "/sampledata/viability/specimens.txt");
    }

    protected void importSpecimens(String studyFolder, String specimensPath)
    {
        log("** Import specimens");
        clickLinkWithText(studyFolder);
        clickLinkWithText("Specimen Data");
        addWebPart("Specimens");
        clickLinkWithText("By Vial Group");
        clickButton("Import Specimens");
        setFormElement(Locator.id("tsv"), getFileContents(specimensPath));
        submit();
        assertTextPresent("Specimens uploaded successfully");
    }

    protected void createViabilityAssay()
    {
        log("** Create viability assay");
        clickFolder(getFolderName());
        addWebPart("Assay List");
        clickButton("Manage Assays");
        clickButton("New Assay Design");
        checkRadioButton("providerName", "Viability");
        clickButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        selenium.type("//input[@id='AssayDesignerName']", getAssayName());

        // Add 'Unreliable' field to Results domain
        addField("Result Fields", 11, "Unreliable", "Unreliable?", ListHelper.ListColumnType.Boolean);
        addField("Result Fields", 12, "IntValue", "IntValue", ListHelper.ListColumnType.Integer);

        sleep(1000);
        clickButton("Save", 0);
        waitForText("Save successful.", 20000);
    }

    protected void setupPipeline()
    {
        log("** Setting pipeline root");
        setupPipeline(getProjectName());
    }

    protected void uploadViabilityRun(String path, boolean setBatchTargetStudy)
    {
        uploadViabilityRun(path, null, setBatchTargetStudy);
    }

    protected void uploadViabilityRun(String path, String runName, boolean setBatchTargetStudy)
    {
        log("** Upload viability run " + (runName != null ? runName : "<unnamed>"));
        clickFolder(getFolderName());
        clickLinkWithText(getAssayName());
        clickButton("Import Data");
        if (setBatchTargetStudy)
            selectOptionByText("targetStudy", "/" + getProjectName() + "/" + getFolderName() + " (" + getFolderName() + " Study)");
        clickButton("Next");

        if (runName != null)
            setFormElement("name", runName);

        uploadAssayFile(path);
    }

    protected void reuploadViabilityRun(String path, String runName)
    {
        // we should be already viewing the assay results page
        assertTitleContains(getAssayName() + " Results");
        clickLinkWithText("rerun");

        // No need to change batch fields (TargetStudy) for now, click Next
        clickButton("Next");

        if (runName != null)
            setFormElement("name", runName);

        uploadAssayFile(path);
    }

    private void uploadAssayFile(String path)
    {
        File guavaFile = new File(getLabKeyRoot() + path);
        Assert.assertTrue("Upload file doesn't exist: " + guavaFile, guavaFile.exists());
        setFormElement("__primaryFile__", guavaFile);
        clickButton("Next", 8000);
    }


    public void addSpecimenIds(String id, String... values)
    {
        for (int i = 0; i < values.length; i++)
        {
            String value = values[i];
            addSpecimenId(id, value, i+1);
        }
    }

    public void addSpecimenId(String id, String value, int index)
    {
        String xpath = "//input[@name='" + id + "'][" + index + "]";
        setFormElement(xpath, value);
        pressTab(xpath);
    }

}
