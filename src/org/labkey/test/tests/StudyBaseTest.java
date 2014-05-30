/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

// Provides some helpful utilities used in study-related tests.  Subclasses provide all study creation and
// verification steps.
public abstract class StudyBaseTest extends SimpleApiTest
{
    protected static final String ARCHIVE_TEMP_DIR = getStudySampleDataPath() + "drt_temp";
    protected static final String SPECIMEN_ARCHIVE_A = getStudySampleDataPath() + "specimens/sample_a.specimens";
    protected int datasetCount = 48;
    protected int visitCount = 65;

    private SpecimenImporter _specimenImporter;

    abstract protected void doCreateSteps();

    abstract protected void doVerifySteps() throws Exception;


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

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }

    protected static String getStudySampleDataPath()
    {
        return "/sampledata/study/";
    }

    protected String getPipelinePath()
    {
        return getLabKeyRoot() + getStudySampleDataPath();
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
        hoverProjectBar();
        int response = -1;
        try{
            response = WebTestHelper.getHttpGetResponse(getBaseURL() + "/" + stripContextPath(getAttribute(Locator.linkWithText(getProjectName()), "href")));
        }
        catch(Exception e){/*No link or bad response*/}

        if (HttpStatus.SC_OK != response)
        {
            _containerHelper.createProject(getProjectName(), null);
        }

        createSubfolder(getProjectName(), getProjectName(), getFolderName(), "Study", null, true);
    }

    // Start importing the specimen archive.  This can load in the background while executing the first set of
    // verification steps to speed up the test.  Call waitForSpecimenImport() before verifying specimens.
    protected void startSpecimenImport(int completeJobsExpected)
    {
        startSpecimenImport(completeJobsExpected, SPECIMEN_ARCHIVE_A);
    }
    protected void startSpecimenImport(int completeJobsExpected, String specimenArchivePath)
    {
        _specimenImporter = new SpecimenImporter(new File(getPipelinePath()), new File(getLabKeyRoot(), specimenArchivePath), new File(getLabKeyRoot(), ARCHIVE_TEMP_DIR), getFolderName(), completeJobsExpected);
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

    @Override
    protected void runUITests() throws Exception
    {
        doCreateSteps();
        doVerifySteps();
    }

    @Override
    protected File[] getTestFiles()
    {
        return new File[0]; 
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);

        deleteLogFiles(".");
        deleteLogFiles("datasets");
        deleteDir(new File(getPipelinePath(), "assaydata"));
        deleteDir(new File(getPipelinePath(), "reports_temp"));
        deleteDir(new File(getLabKeyRoot(), ARCHIVE_TEMP_DIR));
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
        importFolderFromPipeline("AltIdStudy.folder.zip");
    }

    protected void exportStudy(boolean useXmlFormat, boolean zipFile)
    {
        exportStudy(useXmlFormat, zipFile, true);
    }

    protected void exportStudy(boolean useXmlFormat, boolean zipFile, boolean exportProtected)
    {
        exportStudy(useXmlFormat, zipFile, exportProtected, false, false, false, Collections.<String>emptySet());
    }

    @LogMethod protected void exportStudy(boolean useXmlFormat, boolean zipFile, boolean exportProtected,
                               boolean useAlternateIDs, boolean useAlternateDates, boolean maskClinic,
                               @Nullable Set<String> uncheckObjects)
    {
        clickTab("Manage");
        clickButton("Export Study");

        assertTextPresent("Visit Map", "Cohort Settings", "QC State Settings", "CRF Datasets", "Assay Datasets", "Specimens", "Specimen Settings", "Participant Comment Settings");

        if (uncheckObjects != null)
        {
            for (String uncheckObject : uncheckObjects)
                uncheckCheckbox(Locator.checkboxByNameAndValue("types", uncheckObject));
        }
        checkRadioButton(Locator.radioButtonByNameAndValue("format", useXmlFormat ? "new" : "old"));
        checkRadioButton(Locator.radioButtonByNameAndValue("location", zipFile ? "1" : "0"));  // zip file vs. individual files
        if(!exportProtected)
            checkCheckbox(Locator.name("removeProtected"));
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
        clickButton("Delete", WAIT_FOR_PAGE);
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
        click(Locator.checkboxByTitle("Pipeline"));
        submit();
        addWebPart("Data Pipeline");
        addWebPart("Datasets");
        addWebPart("Specimens");
        addWebPart("Views");
        // Set a magic variable to prevent the data region from refreshing out from under us, which causes problems
        // in IE testing
        executeScript("LABKEY.disablePipelineRefresh = true;");
        waitAndClickButton("Setup");
        setPipelineRoot(pipelinePath);
    }

    // Must be on study home page or "manage study" page
    protected void setDemographicsBit(String datasetName, boolean demographics)
    {
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText(datasetName));
        clickButtonContainingText("Edit Definition");
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
        waitForElement(Locator.id("folderBar"));
        if (!getText(Locator.id("folderBar")).equals(projectName))
            clickProject(projectName);
        clickFolder(studyName);
        waitAndClick(Locator.linkWithText("Manage Study"));
        waitForElement(Locator.xpath("id('labkey-nav-trail-current-page')[text()='Manage Study']"));
    }

    //must be in folder whose designation you wish to change.
    protected void setStudyRedesign()
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkCheckbox(Locator.radioButtonByNameAndValue("folderType", "Study Redesign (ITN)"));
        clickButton("Update Folder");
    }

    protected void enterStudySecurity()
    {
        _permissionsHelper.enterPermissionsUI();
        _ext4Helper.clickTabContainingText("Study Security");
        waitAndClickButton("Study Security");
    }

    public void goToManageDatasets()
    {
        goToManageStudy();
        waitAndClickAndWait(Locator.linkWithText("Manage Datasets"));
    }

    protected void goToAxisTab(String axisLabel)
    {
        // Workaround: (Selenium 2.33) Unable to click axis labels reliably for some reason. Use javascript
        fireEvent(Locator.css("svg text").containing(axisLabel).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT), SeleniumEvent.click);
        waitForElement(Ext4Helper.Locators.ext4Button("Cancel")); // Axis label windows always have a cancel button. It should be the only one on the page
    }

    //this assumes a QWP for VisitTag schema has been created and is visible
    protected void insertVisitTag(String QWPname, VisitTag tag)
    {
        clickAndWait(getBtnLocatorFromQWP(QWPname, "Insert New"));
        waitForElement(Locator.input("quf_Name"));
        setFormElement(Locator.input("quf_Name"), tag.name);
        setFormElement(Locator.input("quf_Caption"), tag.caption);
        setFormElement(Locator.tagWithName("textarea", "quf_Description"), tag.description);
        if(tag.isSingleUse)
        {
            click(Locator.checkboxByName("quf_SingleUse"));
        }
        clickAndWait(Locator.linkWithSpan("Submit"));
    }

    //this assumes a QWP for VisitTagMap schema has been created and is visible
    protected void insertVisitTagMap(String QWPname, VisitTagMap map)
    {
        clickAndWait(getBtnLocatorFromQWP(QWPname, "Insert New"));
        waitForElement(Locator.name("quf_VisitTag"));
        selectOptionByValue(Locator.name("quf_VisitTag"), map.visitTag);
        selectOptionByText(Locator.name("quf_Visit"), map.visit);
        if(null != map.cohort && !map.cohort.isEmpty())
            selectOptionByText(Locator.name("quf_Cohort"), map.cohort);
        clickAndWait(Locator.linkWithSpan("Submit"));
    }

    protected Locator getBtnLocatorFromQWP(String QWPTitle, String ButtonText)
    {
        return Locator.xpath("//th[@title= '" + QWPTitle + "']/../../..//span[contains(.,'" + ButtonText + "')]");
    }

    public class VisitTag
    {
        protected String name;
        protected String caption;
        protected String description;
        protected Boolean isSingleUse;

        public VisitTag(String name, String caption, String description, Boolean isSingleUse)
        {
            this.name = name;
            this.caption = caption;
            this.description = description;
            this.isSingleUse = isSingleUse;
        }
    }

    public class VisitTagMap
    {
        protected String visitTag;
        protected String visit;
        protected String cohort;

        public VisitTagMap(String visitTag, String visit, String cohort)
        {
            this.visitTag = visitTag;
            this.visit = visit;
            this.cohort = cohort;
        }
    }
}
