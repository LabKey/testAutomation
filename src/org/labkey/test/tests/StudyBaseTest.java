/*
 * Copyright (c) 2009-2012 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ExtHelper;

import java.io.File;
import java.io.FilenameFilter;

/*
* User: adam
* Date: Aug 17, 2009
* Time: 1:42:07 PM
*/

// Provides some helpful utilities used in study-related tests.  Subclasses provide all study creation and
// verification steps.
public abstract class StudyBaseTest extends SimpleApiTest
{
    protected static final String ARCHIVE_TEMP_DIR = getStudySampleDataPath() + "drt_temp";
    protected static final String SPECIMEN_ARCHIVE_A = getStudySampleDataPath() + "specimens/sample_a.specimens";

    private SpecimenImporter _specimenImporter;

    abstract protected void doCreateSteps();

    abstract protected void doVerifySteps();

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
        if (!isLinkPresentWithText(getProjectName()))
            createProject(getProjectName());
        createSubfolder(getProjectName(), getProjectName(), getFolderName(), "Study", null, true);
    }

    // Start importing the specimen archive.  This can load in the background while executing the first set of
    // verification steps to speed up the test.  Call waitForSpecimenImport() before verifying specimens.
    protected void startSpecimenImport(int completeJobsExpected)
    {
        _specimenImporter = new SpecimenImporter(new File(getPipelinePath()), new File(getLabKeyRoot(), SPECIMEN_ARCHIVE_A), new File(getLabKeyRoot(), ARCHIVE_TEMP_DIR), getFolderName(), completeJobsExpected);
        _specimenImporter.startImport();
    }

    protected void waitForSpecimenImport()
    {
        _specimenImporter.waitForComplete();
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

    protected void doCleanup() throws Exception
    {
        try { deleteProject(getProjectName()); } catch (Throwable e) {}

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
        for (File f : logFiles)
            if (!f.delete())
                log("WARNING: couldn't delete log file " + f.getAbsolutePath());
    }

    protected void importStudy()
    {
        initializeFolder();
        initializePipeline();

        // Start importing study.xml to create the study and load all the datasets.  We'll wait for this import to
        // complete before doing any further tests.
        clickLinkWithText(getFolderName());
        clickNavButton("Process and Import Data");
        ExtHelper.waitForImportDataEnabled(this);
        ExtHelper.clickFileBrowserFileCheckbox(this, "study.xml");
        selectImportDataAction("Import Study");
    }

    protected void exportStudy(boolean useXmlFormat, boolean zipFile)
    {
        clickLinkWithText(getStudyLabel());
        clickTab("Manage");
        clickNavButton("Export Study");

        assertTextPresent("Visit Map", "Cohort Settings", "QC State Settings", "CRF Datasets", "Assay Datasets", "Specimens", "Participant Comment Settings");
        // TODO: these have moved to the folder archive, be sure to test there: "Queries", "Custom Views", "Reports", "Lists"

        checkRadioButton("format", useXmlFormat ? "new" : "old");
        checkRadioButton("location", zipFile ? "1" : "0");  // zip file vs. individual files
        clickNavButton("Export");
    }

    protected void deleteStudy(String studyLabel)
    {
        clickLinkWithText(studyLabel);
        clickTab("Manage");
        clickNavButton("Delete Study");
        checkCheckbox("confirm");
        clickNavButton("Delete", WAIT_FOR_PAGE);
    }

    private void initializePipeline()
    {
        clickAdminMenuItem("Folder", "Management");
        clickLinkWithText("Folder Type");
        toggleCheckboxByTitle("Pipeline");
        submit();
        addWebPart("Data Pipeline");
        addWebPart("Datasets");
        addWebPart("Specimens");
        addWebPart("Views");
        // Set a magic variable to prevent the data region from refreshing out from under us, which causes problems
        // in IE testing
        selenium.runScript("LABKEY.disablePipelineRefresh = true;");
        waitAndClickNavButton("Setup");
        setPipelineRoot(getPipelinePath());
    }

    // Must be on study home page or "manage study" page
    protected void setDemographicsBit(String datasetName, boolean demographics)
    {
        clickTab("Manage");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText(datasetName);
        clickButtonContainingText("Edit Definition");
        waitForElement(Locator.name("description"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        if (demographics)
            checkCheckbox("demographicData");
        else
            uncheckCheckbox("demographicData");

        clickNavButton("Save");
    }

    // Must be on study home page or "manage study" page
    protected void setVisibleBit(String datasetName, boolean showByDefault)
    {
        clickTab("Manage");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText(datasetName);
        clickButtonContainingText("Edit Definition");
        waitForElement(Locator.name("description"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        if (showByDefault)
            checkCheckbox("showByDefault");
        else
            uncheckCheckbox("showByDefault");

        clickNavButton("Save");
    }

    protected void createReport(String reportType)
    {
        // click the create button dropdown
        String id = ExtHelper.getExtElementId(this, "btn_createView");
        click(Locator.id(id));

        id = ExtHelper.getExtElementId(this, reportType);
        clickAndWait(Locator.id(id));
    }

    public void selectOption(String name, int i, String value)
    {
        selectOptionByValue(Locator.tagWithName("select", name).index(i), value);
    }

    protected void assertSelectOption(String name, int i, String expected)
    {
        assertEquals(selenium.getSelectedValue(Locator.tagWithName("select", name).index(i).toString()), expected);
    }

    protected void goToManageStudyPage(String projectName, String studyName)
    {
        log("Going to Manage Study Page of: " + studyName);  clickLinkContainingText(studyName);
        clickLinkContainingText(projectName);
        clickLinkContainingText(studyName);
        clickLinkContainingText("Manage Study");
    }

    //must be in folder whose designation you wish to change.
    protected void setStudyRedesign()
    {
        clickAdminMenuItem("Folder", "Management");
        clickLinkWithText("Folder Type");
        checkRadioButton("folderType", "Study Redesign (ITN)");
        clickButton("Update Folder");
    }
}
