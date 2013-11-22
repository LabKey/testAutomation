/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.categories.Study;
import org.labkey.test.util.ListHelper;

import java.io.File;

import static org.junit.Assert.*;

/**
 * User: cnathe
 * Date: 10/28/13
 *
 * This test is designed to test individual parts/properties of the study import/export archive.
 * The @BeforeClass creates a new study manuall using the default settings.
 * Each @Test then sets a property in that study, exports the study, and reimports it into a subfolder
 */
@Category({DailyB.class, Study.class, FileBrowser.class})
public class StudySimpleExportTest extends StudyBaseTestWD
{
    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getFolderName()
    {
        return "Manually Created Study";
    }

    @Test @Ignore
    public void testSteps(){}

    @Override
    protected void doVerifySteps(){}

    @Override
    protected void doCreateSteps(){}

    @BeforeClass
    public static void doSetup() throws Exception
    {
        StudySimpleExportTest initTest = new StudySimpleExportTest();
        initTest.doCleanup(false);

        initTest.initializeFolder();
        initTest.setPipelineRoot(initTest.getPipelinePath());

        // click button to create manual study
        initTest.clickTab("Overview");
        initTest.clickButton("Create Study");
        // use all of the default study settings
        initTest.clickButton("Create Study");
        // populate study with one dataset, one ptid, and one visit
        initTest.createSimpleDataset();

        // quickly verify some expected text on the overview tab to make sure we have a study
        initTest.clickTab("Overview");
        initTest.waitForElement(Locator.linkWithText("1 dataset"));
        initTest.assertTextPresentInThisOrder("Study tracks data in", "over 1 visit. Data is present for 1 Participant");

        currentTest = initTest;
    }

    private void createSimpleDataset()
    {
        log("Do Setup: create simple dataset with one ptid and one visit");
        clickFolder(getFolderName());
        goToManageDatasets();
        waitForText("Create New Dataset");
        clickAndWait(Locator.linkWithText("Create New Dataset"));
        waitForElement(Locator.name("typeName"));
        setFormElement(Locator.name("typeName"), "TestDataset");
        clickButton("Next");
        waitForElement(Locator.name("ff_name0"));
        _listHelper.deleteField("Dataset Fields", 0);
        _listHelper.addField("Dataset Fields", 0, "TestInt", "TestInt", ListHelper.ListColumnType.Integer);
        _listHelper.addField("Dataset Fields", 1, "TestDate", "TestDate", ListHelper.ListColumnType.DateTime);
        clickButton("Save");
        clickButton("View Data");
        clickButton("Import Data");
        waitForElement(Locator.name("text"));
        setFormElement(Locator.name("text"), "ParticipantId\tSequenceNum\tTestInt\tTestDate\nPTID123\t1.0\t999\t2013-10-29");
        clickButton("Submit");
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        deleteDir(new File(getPipelinePath() + "export"));
    }

    @Test
    public void verifyDatasetQCStates()
    {
        log("QC States: go to Manage Dataset QC States page");
        goToProjectHome();
        clickFolder(getFolderName());
        clickTab("Manage");
        click(Locator.linkWithText("Manage Dataset QC States"));
        waitForText("Manage Dataset QC States");

        log("QC States: set [none] state to be public data, i.e. opposite of default");
        click(Locator.name("blankQCStatePublic"));

        log("QC States: create 3 new QC states (one for each default state type)");
        addNewQCState("First QC State", "The first qc state description", false);
        addNewQCState("Second QC State", "The second qc state description", false);
        addNewQCState("Third QC State", "The third qc state description", false);

        log("QC States: set the default states for dataset data and visibility state");
        selectOptionByText(Locator.name("defaultPipelineQCState"), "First QC State");
        selectOptionByText(Locator.name("defaultAssayQCState"), "Second QC State");
        selectOptionByText(Locator.name("defaultDirectEntryQCState"), "Third QC State");
        selectOptionByText(Locator.name("showPrivateDataByDefault"), "Public data");
        clickButton("Save");

        log("QC States: export study folder to the pipeline as indivisual files");
        exportStudyArchive(getFolderName(), "0");

        log("QC States: verify xml file was created in export");
        _fileBrowserHelper.selectFileBrowserItem("export/study/quality_control_states.xml");

        log("QC States: import study into subfolder");
        createSubfolderAndImportStudyFromPipeline("QC States");

        log("QC States: verify imported settings");
        clickFolder("QC States");
        clickTab("Manage");
        click(Locator.linkWithText("Manage Dataset QC States"));
        waitForText("Manage Dataset QC States");
        assertFormElementEquals(Locator.name("blankQCStatePublic"), "true");
        assertFormElementEquals(Locator.name("labels").index(0), "First QC State");
        assertFormElementEquals(Locator.name("descriptions").index(0), "The first qc state description");
        assertNotChecked(Locator.name("publicData").index(0));
        assertFormElementEquals(Locator.name("labels").index(1), "Second QC State");
        assertFormElementEquals(Locator.name("descriptions").index(1), "The second qc state description");
        assertNotChecked(Locator.name("publicData").index(1));
        assertFormElementEquals(Locator.name("labels").index(2), "Third QC State");
        assertFormElementEquals(Locator.name("descriptions").index(2), "The third qc state description");
        assertNotChecked(Locator.name("publicData").index(2));
        assertEquals("First QC State", getSelectedOptionText(Locator.name("defaultPipelineQCState")).trim());
        assertEquals("Second QC State", getSelectedOptionText(Locator.name("defaultAssayQCState")).trim());
        assertEquals("Third QC State", getSelectedOptionText(Locator.name("defaultDirectEntryQCState")).trim());
        assertEquals("Public data", getSelectedOptionText(Locator.name("showPrivateDataByDefault")).trim());
    }

    private void addNewQCState(String name, String description, boolean publicData)
    {
        setFormElement(Locator.name("newLabel"), name);
        setFormElement(Locator.name("newDescription"), description);
        if (!publicData)
            click(Locator.name("newPublicData"));
        clickButton("Save");

        Locator l = Locator.tagWithName("input", "labels");
        assertFormElementEquals(l.index(getElementCount(l) - 1), name);
    }

    @Test
    public void verifyDefaultDatasetFormats()
    {
        log("Default Formats: set default formats for study");
        goToProjectHome();
        clickFolder(getFolderName());
        goToManageDatasets();
        setFormElement(Locator.name("dateFormat"), "MMM dd, yyyy");
        setFormElement(Locator.name("numberFormat"), "#.000");
        clickButton("Submit");

        log("Default Formats: export study folder to the pipeline as indivisual files");
        exportStudyArchive(getFolderName(), "0");

        log("Default Formats: verify xml file was created in export");
        _fileBrowserHelper.selectFileBrowserItem("export/study/datasets/datasets_manifest.xml");

        log("Default Formats: import study into subfolder");
        createSubfolderAndImportStudyFromPipeline("Default Dataset Formats");

        log("Default Formats: verify imported settings");
        clickFolder("Default Dataset Formats");
        goToManageDatasets();
        waitForElement(Locator.name("dateFormat"));
        assertFormElementEquals(Locator.name("dateFormat"), "MMM dd, yyyy");
        assertFormElementEquals(Locator.name("numberFormat"), "#.000");
        clickAndWait(Locator.linkWithText("TestDataset"));
        clickButton("View Data");
        assertTextPresentInThisOrder("999.000", "Oct 29, 2013");
    }

    @Test
    public void verifySuppressQueryValidation()
    {
        log("Query Validation: import study folder zip without query validation enabled");
        goToProjectHome();
        createSubfolder(getProjectName(), getProjectName(), "Query Validation", "Collaboration", null, true);
        importFolderFromZip(new File(getPipelinePath(), "LabkeyDemoStudyWithCharts.folder.zip"), false, 1);
        goToModule("FileContent");
        _fileBrowserHelper.selectFileBrowserItem("/unzip/");
        //TODO: broken: https://www.labkey.org/issues/home/Developer/issues/details.view?issueId=19029
        doubleClick(Locator.tag("div").startsWith("folder_load_"));
        assertTextPresentInThisOrder("Loading folder type and active modules", " queries imported", "Skipping query validation.");
    }

    @Test
    public void verifyCustomParticipantView()
    {
        log("Custom Ptid View: create custom ptid view");
        goToProjectHome();
        clickFolder(getFolderName());
        clickTab("Participants");
        clickAndWait(Locator.linkWithText("PTID123"));
        clickAndWait(Locator.linkWithText("Customize View"));
        checkRadioButton(Locator.radioButtonByNameAndValue("useCustomView", "true"));
        setFormElement(Locator.name("customScript"), "This is my custom participant view");
        clickButton("Save and Finish");
        waitForElement(Locator.tagWithText("div", "This is my custom participant view"));

        log("Custom Ptid View: export study folder to the pipeline as indivisual files");
        exportStudyArchive(getFolderName(), "0");

        log("Custom Ptid View: verify xml file was created in export");
        _fileBrowserHelper.selectFileBrowserItem("export/study/views/settings.xml");
        _fileBrowserHelper.selectFileBrowserItem("export/study/views/participant.html");

        log("Custom Ptid View: import study into subfolder");
        createSubfolderAndImportStudyFromPipeline("Custom Participant View");

        log("Custom Ptid View: verify imported settings");
        clickFolder("Custom Participant View");
        clickTab("Participants");
        clickAndWait(Locator.linkWithText("PTID123"));
        waitForElement(Locator.tagWithText("div", "This is my custom participant view"));
        clickAndWait(Locator.linkWithText("Customize View"));
        assertRadioButtonSelected(Locator.radioButtonByNameAndValue("useCustomView", "true"));
    }

    private void exportStudyArchive(String folder, String location)
    {
        clickFolder(folder);
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Export"));
        checkRadioButton(Locator.radioButtonByNameAndValue("location", location));
        clickButton("Export");
    }

    private void createSubfolderAndImportStudyFromPipeline(String subfolderName)
    {
        createSubfolder(getProjectName(), getProjectName(), subfolderName, "Collaboration", null, true);
        clickFolder(subfolderName);
        setPipelineRoot(getPipelinePath());
        importFolderFromPipeline("/export/folder.xml");
    }
}
