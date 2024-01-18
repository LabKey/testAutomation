/*
 * Copyright (c) 2011-2018 LabKey Corporation
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

package org.labkey.test.tests.viability;

import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.tests.AbstractAssayTest;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.QCAssayScriptHelper;
import org.openqa.selenium.WebDriverException;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public abstract class AbstractViabilityTest extends AbstractAssayTest
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

    @Override
    protected abstract String getProjectName();
    protected abstract String getFolderName();
    protected abstract String getAssayName();

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);

        try
        {
            new QCAssayScriptHelper(this).deleteEngine();
        }
        catch(WebDriverException ignored) {}
    }

    protected void initializeStudyFolder(String... tabs)
    {
        log("** Initialize Folder");
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModule("Specimen");
        _containerHelper.createSubfolder(getProjectName(), getProjectName(), getFolderName(), "Study", tabs, true);

        log("** Create Study");
        clickFolder(getFolderName());
        clickButton("Create Study");
        clickButton("Create Study");
    }

    protected void importSpecimens()
    {
        importSpecimens(getFolderName(), TestFileUtils.getSampleData("viability/specimens.txt"));
    }

    protected void importSpecimens(String studyFolder, File specimensPath)
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
        log("** Create viability assay");
        PortalHelper portalHelper = new PortalHelper(this);
        clickFolder(getFolderName());
        portalHelper.addWebPart("Assay List");

        clickButton("Manage Assays");
        ReactAssayDesignerPage assayDesignerPage = _assayHelper.createAssayDesign("Viability", getAssayName());

        // Add 'Unreliable' field to Results domain
        DomainFormPanel resultsPanel = assayDesignerPage.expandFieldsPanel("Result");
        resultsPanel.addField("Unreliable").setType(FieldDefinition.ColumnType.Boolean).setLabel("Unreliable?");
        resultsPanel.addField("IntValue").setType(FieldDefinition.ColumnType.Integer).setLabel("IntValue");

        assayDesignerPage.clickFinish();
    }

    protected void setupPipeline() throws Exception
    {
        log("** Setting pipeline root");
        setupPipeline(getProjectName());
    }

    protected void uploadViabilityRun(File file, boolean setBatchTargetStudy)
    {
        uploadViabilityRun(file, null, setBatchTargetStudy);
    }

    protected void uploadViabilityRun(File file, String runName, boolean setBatchTargetStudy)
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

        uploadAssayFile(file);
    }

    protected void reuploadViabilityRun(File file, String runName)
    {
        // we should be already viewing the assay results page
        assertTitleContains(getAssayName() + " Results");
        clickAndWait(Locator.linkWithText("rerun"));

        // No need to change batch fields (TargetStudy) for now, click Next
        clickButton("Next");

        if (runName != null)
            setFormElement(Locator.name("name"), runName);

        uploadAssayFile(file);
    }

    private void uploadAssayFile(File guavaFile)
    {
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
