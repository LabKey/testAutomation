/*
 * Copyright (c) 2011 LabKey Corporation
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

import org.labkey.test.Locator;

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
    protected boolean isDatabaseSupported(DatabaseInfo info)
    {
        return info.productName.equals("PostgreSQL") ||
                (info.productName.equals("Microsoft SQL Server") && !info.productVersion.startsWith("08.00"));
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected abstract String getProjectName();
    protected abstract String getFolderName();
    protected abstract String getAssayName();

    protected void doCleanup() throws Exception
    {
        try
        {
            deleteProject(getProjectName());
            deleteEngine();
        }
        catch(Throwable T) {}

        deleteDir(getTestTempDir());
    }

    protected void initializeStudyFolder(String... tabs)
    {
        log("** Initialize Folder");
        if (!isLinkPresentWithText(getProjectName()))
            createProject(getProjectName());
        createSubfolder(getProjectName(), getProjectName(), getFolderName(), "Study", tabs, true);
        
        log("** Create Study");
        clickLinkWithText(getFolderName());
        clickNavButton("Create Study");
        clickNavButton("Create Study");
    }


    protected void importSpecimens()
    {
        importSpecimens(getFolderName(), "/sampledata/viability/specimens.txt");
    }

    protected void importSpecimens(String studyFolder, String specimensPath)
    {
        log("** Import specimens");
        clickLinkWithText(studyFolder);
        clickLinkWithText("By Vial Group");
        clickNavButton("Import Specimens");
        setLongTextField("tsv", getFileContents(specimensPath));
        submit();
        assertTextPresent("Specimens uploaded successfully");
    }

    protected void createViabilityAssay()
    {
        log("** Create viability assay");
        clickLinkWithText(getFolderName());
        addWebPart("Assay List");
        clickNavButton("Manage Assays");
        clickNavButton("New Assay Design");
        checkRadioButton("providerName", "Viability");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        selenium.type("//input[@id='AssayDesignerName']", getAssayName());

        sleep(1000);
        clickNavButton("Save", 0);
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
        log("** Upload guava run");
        clickLinkWithText(getFolderName());
        clickLinkWithText(getAssayName());
        clickNavButton("Import Data");
        if (setBatchTargetStudy)
            selectOptionByText("targetStudy", "/" + getProjectName() + "/" + getFolderName() + " (" + getFolderName() + " Study)");
        clickNavButton("Next");

        if (runName != null)
            setFormElement("name", runName);
        
        File guavaFile = new File(getLabKeyRoot() + path);
        assertTrue("Upload file doesn't exist: " + guavaFile, guavaFile.exists());
        setFormElement("__primaryFile__", guavaFile);
        clickNavButton("Next", 8000);
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
