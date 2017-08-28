/*
 * Copyright (c) 2011-2017 LabKey Corporation
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
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public abstract class AbstractViabilityTest extends AbstractQCAssayTest
{
    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("viability");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    protected abstract String getProjectName();
    protected abstract String getFolderName();
    protected abstract String getAssayName();

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);

        try{deleteEngine();}
        catch(Throwable ignored) {}
    }

    protected void initializeStudyFolder(String... tabs)
    {
        log("** Initialize Folder");
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), getProjectName(), getFolderName(), "Study", tabs, true);

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
        clickFolder(studyFolder);
        clickAndWait(Locator.linkWithText("Specimen Data"));
        waitAndClickAndWait(Locator.linkWithText("Import Specimens"));
        waitForElement(Locator.id("tsv"));
        setFormElement(Locator.id("tsv"), TestFileUtils.getFileContents(specimensPath));
        submit();
        assertTextPresent("Specimens uploaded successfully");
    }

    protected void createViabilityAssay()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        log("** Create viability assay");
        clickFolder(getFolderName());
        portalHelper.addWebPart("Assay List");
        clickButton("Manage Assays");
        clickButton("New Assay Design");
        checkRadioButton(Locator.radioButtonByNameAndValue("providerName", "Viability"));
        clickButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        setFormElement(Locator.xpath("//input[@id='AssayDesignerName']"), getAssayName());

        // Add 'Unreliable' field to Results domain
        _listHelper.addField("Result Fields", "Unreliable", "Unreliable?", ListHelper.ListColumnType.Boolean);
        _listHelper.addField("Result Fields", "IntValue", "IntValue", ListHelper.ListColumnType.Integer);

        sleep(1000);
        clickButton("Save", 0);
        waitForText(20000, "Save successful.");
    }

    protected void setupPipeline() throws Exception
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
        clickAndWait(Locator.linkWithText(getAssayName()));
        clickButton("Import Data");
        if (setBatchTargetStudy)
            selectOptionByText(Locator.name("targetStudy"), "/" + getProjectName() + "/" + getFolderName() + " (" + getFolderName() + " Study)");
        clickButton("Next");

        if (runName != null)
            setFormElement(Locator.name("name"), runName);

        uploadAssayFile(path);
    }

    protected void reuploadViabilityRun(String path, String runName)
    {
        // we should be already viewing the assay results page
        assertTitleContains(getAssayName() + " Results");
        clickAndWait(Locator.linkWithText("rerun"));

        // No need to change batch fields (TargetStudy) for now, click Next
        clickButton("Next");

        if (runName != null)
            setFormElement(Locator.name("name"), runName);

        uploadAssayFile(path);
    }

    private void uploadAssayFile(String path)
    {
        File guavaFile = new File(TestFileUtils.getLabKeyRoot() + path);
        assertTrue("Upload file doesn't exist: " + guavaFile, guavaFile.exists());
        setFormElement(Locator.name("__primaryFile__"), guavaFile);
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
        Locator xpath = Locator.xpath("//input[@name='" + id + "'][" + index + "]");
        setFormElement(xpath, value);
        pressTab(xpath);
    }

}
