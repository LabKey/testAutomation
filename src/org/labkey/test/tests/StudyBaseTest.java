/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

import org.apache.hc.core5.http.HttpStatus;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.pages.DatasetPropertiesPage;
import org.labkey.test.pages.admin.ExportFolderPage;
import org.labkey.test.params.FieldDefinition.PhiSelectType;
import org.labkey.test.util.APITestHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.StudyHelper;
import org.labkey.test.util.study.specimen.SpecimenHelper;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

/**
 * @deprecated TODO: Move shared functionality to a Helper class
 * This class does not leave enough flexibility in test design.
 *
 * Provides some helpful utilities used in study-related tests.  Subclasses provide all study creation and
 * verification steps.
 */
@Deprecated
public abstract class StudyBaseTest extends BaseWebDriverTest
{
    protected static final File ARCHIVE_TEMP_DIR = StudyHelper.getStudyTempDir();
    protected int datasetCount = getDatasetCount();
    protected int visitCount = 65;

    private SpecimenImporter _specimenImporter;

    abstract protected void doCreateSteps();

    abstract protected void doVerifySteps() throws Exception;

    protected int getDatasetCount(){return 48;}

    protected void setupRequestStatuses()
    {
        new SpecimenHelper(this).setupRequestStatuses();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("study");
    }

    @Override
    protected String getProjectName()
    {
        return "StudyVerifyProject";
    }

    protected String getStudyLabel()
    {
        return "Study 001";
    }

    protected String getFolderName()
    {
        return "My Study";
    }

    protected void initializeFolder()
    {
        int response = WebTestHelper.getHttpResponse(WebTestHelper.buildURL("project", getProjectName(), "begin")).getResponseCode();

        if (HttpStatus.SC_OK != response)
        {
            _containerHelper.createProject(getProjectName(), null);
        }

        _containerHelper.createSubfolder(getProjectName(), getFolderName(), "Study");
        if (_studyHelper.isSpecimenModulePresent())
            _containerHelper.enableModule("Specimen");
        new ApiPermissionsHelper(this).checkInheritedPermissions();
    }

    // Start importing the specimen archive. This can load in the background while executing the first set of
    // verification steps to speed up the test. Call waitForSpecimenImport() before verifying specimens.
    protected void startSpecimenImport(int completeJobsExpected)
    {
        startSpecimenImport(completeJobsExpected, StudyHelper.SPECIMEN_ARCHIVE_A);
    }

    protected void startSpecimenImport(int completeJobsExpected, File specimenArchive)
    {
        _specimenImporter = new SpecimenImporter(new File(StudyHelper.getStudySubfolderPath()), specimenArchive, ARCHIVE_TEMP_DIR, getFolderName(), completeJobsExpected);
        _specimenImporter.startImport();
    }

    protected void waitForSpecimenImport()
    {
        _specimenImporter.waitForComplete();
    }

    protected void setExpectSpecimenImportError(boolean expected)
    {
        _specimenImporter.setExpectError(expected);
    }

    protected void runUITests() throws Exception
    {
        doCreateSteps();
        doVerifySteps();
    }

    protected File[] getTestFiles()
    {
        return new File[0]; 
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);

        deleteLogFiles(".");
        deleteLogFiles("datasets");
        TestFileUtils.deleteDir(new File(StudyHelper.getStudySubfolderPath(), "assaydata"));
        TestFileUtils.deleteDir(new File(StudyHelper.getStudySubfolderPath(), "reports_temp"));
        TestFileUtils.deleteDir(ARCHIVE_TEMP_DIR);
    }

    private void deleteLogFiles(String directoryName)
    {
        File dataRoot = new File(StudyHelper.getStudySubfolderPath() + directoryName);
        File[] logFiles = dataRoot.listFiles((dir, name) -> name.endsWith(".log"));
        if (null != logFiles)
            for (File f : logFiles)
                if (!f.delete())
                    log("WARNING: couldn't delete log file " + f.getAbsolutePath());
    }

    protected void importStudy(){importStudy(null);}

    protected void importStudy(String pipelinePath)
    {
        initializeFolder();
        initializePipeline(pipelinePath);

        clickFolder(getFolderName());

        // Start importing a folder archive to create the study and load all the datasets. We'll wait for this import to
        // complete before doing any further tests.
        log("Import new study with alt-ID");
        importFolderFromZip(TestFileUtils.getSampleData("studies/AltIdStudy.folder.zip"));
    }

    protected void importStudy(File folderArchive, @Nullable String pipelinePath)
    {
        initializeFolder();
        initializePipeline(pipelinePath);
        clickFolder(getFolderName());
        importFolderFromZip(folderArchive);
    }

    protected void exportStudy(boolean zipFile)
    {
        exportStudy(zipFile, true, null);
    }

    protected void exportStudy(boolean zipFile, boolean exportPhi, PhiSelectType exportPhiLevel)
    {
        exportStudy(zipFile, exportPhiLevel, false, false, false, Collections.emptySet());
    }

    @LogMethod protected void exportStudy(boolean zipFile, @Nullable PhiSelectType exportPhiLevel,
                                          boolean useAlternateIDs, boolean useAlternateDates, boolean maskClinic,
                                          @Nullable Set<String> uncheckObjects)
    {
        clickTab("Manage");
        clickButton("Export Study");
        ExportFolderPage exportFolderPage = new ExportFolderPage(getDriver());

        waitForText("Visit Map", "Cohort Settings", "QC State Settings", "Datasets: Study Dataset Definitions", "Datasets: Study Dataset Data",
                "Datasets: Assay Dataset Definitions", "Datasets: Assay Dataset Data", "Participant Comment Settings");

        if (_studyHelper.isSpecimenModuleActive())
        {
            assertTextPresent("Specimens", "Specimen Settings");
        }

        if (exportPhiLevel != null)
        {
            exportFolderPage.includePhiColumns(exportPhiLevel);
        }

        if (uncheckObjects != null)
        {
            for (String folderObject : uncheckObjects)
            {
                exportFolderPage.includeObject(folderObject, false);
            }
        }

        if(useAlternateIDs)
            new Checkbox(Locator.tagWithClass("input", "alternate-ids").findElement(getDriver())).check();
        if(useAlternateDates)
            new Checkbox(Locator.tagWithClass("input", "shift-dates").findElement(getDriver())).check();
        if(maskClinic)
            new Checkbox(Locator.tagContainingText("label", "Mask Clinic Names").precedingSibling("input").findElement(getDriver())).check();

        if (zipFile)
        {
            exportFolderPage.exportToPipelineAsZip();
        }
        else
        {
            exportFolderPage.exportToPipelineAsIndividualFiles();
        }
    }

    /* pre-requisite: test is already in the folder where the study exists */
    protected void deleteStudy()
    {
        clickTab("Manage");
        clickButton("Delete Study");
        checkCheckbox(Locator.checkboxByName("confirm"));
        clickButton("Delete", WAIT_FOR_PAGE * 2);
    }

    protected void initializePipeline(String pipelinePath)
    {
        if(pipelinePath==null)
            pipelinePath = StudyHelper.getStudySubfolderPath();

        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkCheckbox(Locator.checkboxByTitle("Pipeline"));
        if (_studyHelper.isSpecimenModulePresent())
            checkCheckbox(Locator.checkboxByTitle("Specimen")); // Activate specimen module to enable specimen UI/webparts
        clickButton("Update Folder");
        new PortalHelper(getDriver()).doInAdminMode(portalHelper -> {
            portalHelper.addWebPart("Data Pipeline");
            portalHelper.addWebPart("Datasets");
            if (_studyHelper.isSpecimenModulePresent())
                portalHelper.addWebPart("Specimens");
            portalHelper.addWebPart("Views");
        });
        setPipelineRoot(pipelinePath);
    }

    // Must be on study home page or "manage study" page
    protected DatasetPropertiesPage setDemographicsBit(String datasetLabel, boolean demographics)
    {
        return _studyHelper.goToManageDatasets()
                .selectDatasetByLabel(datasetLabel)
                .clickEditDefinition()
                .setIsDemographicData(demographics)
                .clickSave();
    }

    // Must be on study home page or "manage study" page
    protected DatasetPropertiesPage setVisibleBit(String datasetLabel, boolean showByDefault)
    {
        return _studyHelper.goToManageDatasets()
                .selectDatasetByLabel(datasetLabel)
                .clickEditDefinition()
                .setShowInOverview(showByDefault)
                .clickSave();
    }

    public void selectOption(String name, int i, String value)
    {
        selectOptionByValue(Locator.tagWithName("select", name).index(i), value);
    }

    protected void assertSelectOption(String name, int i, String expected)
    {
        Select select = new Select(Locator.tagWithName("select", name).index(i).findElement(getDriver()));
        assertEquals("Expected option was not selected", expected, select.getFirstSelectedOption().getAttribute("value"));
    }

    protected void goToManageStudyPage(String projectName, String studyName)
    {
        log("Going to Manage Study Page of: " + studyName);
        navigateToFolder(projectName, studyName);
        waitAndClick(Locator.linkWithText("Manage Study"));
        waitForElement(Locator.tagWithClassContaining("div", "lk-body-title")
                .withChild(Locator.tagWithText("h3", "Manage Study")));
    }

    protected void goToSpecimenData()
    {
        clickTab("Specimen Data");
        waitForElement(Locator.css(".specimenSearchLoaded"));
    }

    /**
     * @deprecated Inline
     */
    @Deprecated
    protected void enterStudySecurity()
    {
        _studyHelper.enterStudySecurity();
    }

    protected Pattern[] getIgnoredElements()
    {
        return new Pattern[0];
    }

    public void runApiTests() throws Exception
    {
        APITestHelper apiTester = new APITestHelper(this);
        apiTester.setTestFiles(getTestFiles());
        apiTester.setIgnoredElements(getIgnoredElements());
        apiTester.runApiTests();
    }

    @Test
    public void testSteps() throws Exception
    {
        runUITests();
        runApiTests();
    }
}
