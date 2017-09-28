/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.labkey.api.data.PHI;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.APITestHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.StudyHelper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
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
    protected static final String ARCHIVE_TEMP_DIR = getStudySampleDataPath() + "drt_temp";
    protected static final String SPECIMEN_ARCHIVE_A = getStudySampleDataPath() + "specimens/sample_a.specimens";
    protected int datasetCount = getDatasetCount();
    protected int visitCount = 65;

    private SpecimenImporter _specimenImporter;

    abstract protected void doCreateSteps();

    abstract protected void doVerifySteps() throws Exception;

    protected int getDatasetCount(){return 48;}

    protected void setupRequestStatuses()
    {
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Request Statuses"));
        setFormElement(Locator.name("newLabel"), "New Request");
        clickButton("Save");
        setFormElement(Locator.name("newLabel"), "Processing");
        clickButton("Save");
        setFormElement(Locator.name("newLabel"), "Completed");
        checkCheckbox(Locator.checkboxByName("newFinalState"));
        clickButton("Save");
        setFormElement(Locator.name("newLabel"), "Rejected");
        checkCheckbox(Locator.checkboxByName("newFinalState"));
        uncheckCheckbox(Locator.checkboxByName("newSpecimensLocked"));
        clickButton("Done");
    }

    protected void setupSpecimenManagement()
    {
        goToManageStudy();
        clickAndWait(Locator.linkWithText("Manage Request Statuses"));
        setFormElement(Locator.name("newLabel"), "New Request");
        clickButton("Save");
        setFormElement(Locator.name("newLabel"), "Processing");
        clickButton("Save");
        setFormElement(Locator.name("newLabel"), "Completed");
        checkCheckbox(Locator.name("newFinalState"));
        clickButton("Save");
        setFormElement(Locator.name("newLabel"), "Rejected");
        checkCheckbox(Locator.name("newFinalState"));
        uncheckCheckbox(Locator.name("newSpecimensLocked"));
        clickButton("Done");
    }

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("study");
    }

    /**
     * @deprecated TODO: Inline and remove
     */
    @Deprecated
    protected static String getStudySampleDataPath()
    {
        return StudyHelper.getStudySampleDataPath();
    }

    /**
     * @deprecated TODO: Inline and remove
     */
    @Deprecated
    protected String getPipelinePath()
    {
        return StudyHelper.getPipelinePath();
    }

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
        openProjectMenu();
        int response = -1;
        try{
            response = WebTestHelper.getHttpGetResponse(getBaseURL() + "/" + WebTestHelper.stripContextPath(getAttribute(Locator.linkWithText(getProjectName()), "href")));
        }
        catch (NoSuchElementException | IOException ignore){/*No link or bad response*/}

        if (HttpStatus.SC_OK != response)
        {
            _containerHelper.createProject(getProjectName(), null);
        }

        _containerHelper.createSubfolder(getProjectName(), getProjectName(), getFolderName(), "Study", null, true);
    }

    // Start importing the specimen archive.  This can load in the background while executing the first set of
    // verification steps to speed up the test.  Call waitForSpecimenImport() before verifying specimens.
    protected void startSpecimenImport(int completeJobsExpected)
    {
        startSpecimenImport(completeJobsExpected, SPECIMEN_ARCHIVE_A);
    }
    protected void startSpecimenImport(int completeJobsExpected, String specimenArchivePath)
    {
        _specimenImporter = new SpecimenImporter(new File(getPipelinePath()), new File(TestFileUtils.getLabKeyRoot(), specimenArchivePath), new File(TestFileUtils.getLabKeyRoot(), ARCHIVE_TEMP_DIR), getFolderName(), completeJobsExpected);
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

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);

        deleteLogFiles(".");
        deleteLogFiles("datasets");
        TestFileUtils.deleteDir(new File(getPipelinePath(), "assaydata"));
        TestFileUtils.deleteDir(new File(getPipelinePath(), "reports_temp"));
        TestFileUtils.deleteDir(new File(TestFileUtils.getLabKeyRoot(), ARCHIVE_TEMP_DIR));
    }

    private void deleteLogFiles(String directoryName)
    {
        File dataRoot = new File(getPipelinePath() + directoryName);
        File[] logFiles = dataRoot.listFiles(new FilenameFilter(){
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".log");
            }
        });
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

        // Start importing study.xml to create the study and load all the datasets.  We'll wait for this import to
        // complete before doing any further tests.
        clickFolder(getFolderName());

        log("Import new study with alt-ID");
        importFolderFromZip(TestFileUtils.getSampleData("studies/AltIdStudy.folder.zip"));
    }

    protected void importStudy(File studyArchive, @Nullable String pipelinePath)
    {
        initializeFolder();
        initializePipeline(pipelinePath);
        clickFolder(getFolderName());
        importFolderFromZip(studyArchive);
    }

    protected void exportStudy(boolean zipFile)
    {
        exportStudy(zipFile, true, null);
    }

    protected void exportStudy(boolean zipFile, boolean exportPhi, PHI exportPhiLevel)
    {
        exportStudy(zipFile, exportPhi, exportPhiLevel, false, false, false, Collections.emptySet());
    }

    @LogMethod protected void exportStudy(boolean zipFile, boolean exportPhi, PHI exportPhiLevel,
                                          boolean useAlternateIDs, boolean useAlternateDates, boolean maskClinic,
                                          @Nullable Set<String> uncheckObjects)
    {
        clickTab("Manage");
        clickButton("Export Study");

        waitForText("Visit Map", "Cohort Settings", "QC State Settings", "CRF Datasets", "Assay Datasets",
                "Dataset Data", "Specimens", "Specimen Settings", "Participant Comment Settings");

        if (uncheckObjects != null)
        {
            for (String uncheckObject : uncheckObjects)
                uncheckCheckbox(Locator.checkboxByNameAndValue("types", uncheckObject));
        }
        checkRadioButton(Locator.radioButtonByNameAndValue("location", zipFile ? "1" : "0"));  // zip file vs. individual files
        if(!exportPhi)
        {
            checkCheckbox(Locator.name("removePhi"));
            setFormElementJS(Locator.input("exportPhiLevel"), exportPhiLevel.name());
        }
        if(useAlternateIDs)
            checkCheckbox(Locator.name("alternateIds"));
        if(useAlternateDates)
            checkCheckbox(Locator.name("shiftDates"));
        if(maskClinic)
            checkCheckbox(Locator.name("maskClinic"));
        clickButton("Export");
    }

    /* pre-requisite: test is already in the folder where the study exists */
    protected void deleteStudy()
    {
        clickTab("Manage");
        clickButton("Delete Study");
        checkCheckbox(Locator.checkboxByName("confirm"));
        clickButton("Delete", WAIT_FOR_PAGE * 2);
    }

    protected void initializePipeline()
    {
        initializePipeline(getPipelinePath());
    }
    
    protected void initializePipeline(String pipelinePath)
    {
        if(pipelinePath==null)
            pipelinePath = getPipelinePath();

        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkCheckbox(Locator.checkboxByTitle("Pipeline"));
        if (LabKeySiteWrapper.IS_BOOTSTRAP_LAYOUT)
        {
            clickButton("Update Folder");
        } else
        {
            submit();
        }
        new PortalHelper(getDriver()).addWebPart("Data Pipeline");
        new PortalHelper(getDriver()).addWebPart("Datasets");
        new PortalHelper(getDriver()).addWebPart("Specimens");
        new PortalHelper(getDriver()).addWebPart("Views");
        // Set a magic variable to prevent the data region from refreshing out from under us, which causes problems
        // in IE testing
        executeScript("LABKEY.disablePipelineRefresh = true;");
        clickButton("Setup", defaultWaitForPage);
        setPipelineRoot(pipelinePath);
    }

    // Must be on study home page or "manage study" page
    protected void setDemographicsBit(String datasetName, boolean demographics)
    {
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText(datasetName));
        mashButton("Edit Definition");
        waitForElement(Locator.name("description"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        if (demographics)
            checkCheckbox(Locator.checkboxByName("demographicData"));
        else
            uncheckCheckbox(Locator.checkboxByName("demographicData"));

        clickButton("Save");
    }

    // Must be on study home page or "manage study" page
    protected void setVisibleBit(String datasetName, boolean showByDefault)
    {
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText(datasetName));
        clickButtonContainingText("Edit Definition");
        waitForElement(Locator.name("description"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        if (showByDefault)
            checkCheckbox(Locator.checkboxByName("showByDefault"));
        else
            uncheckCheckbox(Locator.checkboxByName("showByDefault"));

        clickButton("Save");
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
        if (IS_BOOTSTRAP_LAYOUT)
        {
            projectMenu().navigateToMenuLink(projectName, studyName);
            waitAndClick(Locator.linkWithText("Manage Study"));
            waitForElement(Locator.tagWithClassContaining("div", "lk-body-title")
                    .withChild(Locator.tagWithText("h3", "Manage Study")));
        }
        else
        {
            waitForElement(Locator.id("folderBar"));
            if (!getText(Locator.id("folderBar")).equals(projectName))
                clickProject(projectName);
            clickFolder(studyName);
            waitAndClick(Locator.linkWithText("Manage Study"));
            waitForElement(Locator.xpath("id('labkey-nav-trail-current-page')[text()='Manage Study']"));
        }
    }

    protected void goToSpecimenData()
    {
        clickTab("Specimen Data");
        waitForElement(Locator.css(".specimenSearchLoaded"));
    }

    //must be in folder whose designation you wish to change.
    protected void setStudyITNFolderType()
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkCheckbox(Locator.radioButtonByNameAndValue("folderType", "Study (ITN)"));
        clickButton("Update Folder");
    }

    protected void enterStudySecurity()
    {
        _permissionsHelper.enterPermissionsUI();
        _ext4Helper.clickTabContainingText("Study Security");
        clickButton("Study Security", defaultWaitForPage);
    }

    // TODO Dan Duffek: The following function are in here temporarily. They have been added as part of the goal of removing the SimpleApiTest.java module
    protected void ensureConfigured()
    {

    }

    protected void cleanUp()
    {

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
        ensureConfigured();
        runUITests();
        runApiTests();
        cleanUp();
    }

    // TODO Dan Duffek: End of the inserted functions.
}
