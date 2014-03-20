/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
import java.util.HashMap;
import java.util.Map;

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
public class StudySimpleExportTest extends StudyBaseTest
{
    private static final String TEST_DATASET_NAME = "TestDataset";

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
        setFormElement(Locator.name("typeName"), TEST_DATASET_NAME);
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
        goToManageStudy();
        waitAndClickAndWait(Locator.linkWithText("Manage Dataset QC States"));
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
        goToManageStudy();
        waitAndClickAndWait(Locator.linkWithText("Manage Dataset QC States"));
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

        log("QC States: reset default visibility state");
        clickFolder(getFolderName());
        goToManageStudy();
        waitAndClickAndWait(Locator.linkWithText("Manage Dataset QC States"));
        waitForText("Manage Dataset QC States");
        selectOptionByText(Locator.name("showPrivateDataByDefault"), "All data");
        clickButton("Save");
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

        // Default date & number formats are now on the folder management "Formats" tab
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Formats"));
        setFormElement(Locator.name("defaultDateFormat"), "MMM dd, yyyy");
        setFormElement(Locator.name("defaultNumberFormat"), "#.000");
        clickButton("Save");

        log("Default Formats: export study folder to the pipeline as indivisual files");
        exportStudyArchive(getFolderName(), "0");

        log("Default Formats: verify xml file was created in export");
        _fileBrowserHelper.selectFileBrowserItem("export/study/datasets/datasets_manifest.xml");

        log("Default Formats: import study into subfolder");
        createSubfolderAndImportStudyFromPipeline("Default Dataset Formats");

        log("Default Formats: verify imported settings");
        clickFolder("Default Dataset Formats");
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Formats"));
        assertFormElementEquals(Locator.name("defaultDateFormat"), "MMM dd, yyyy");
        assertFormElementEquals(Locator.name("defaultNumberFormat"), "#.000");

        clickTab("Clinical and Assay Data");
        waitAndClickAndWait(Locator.linkWithText(TEST_DATASET_NAME));
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
        doubleClick(Locator.tag("div").startsWith("folder_load_"));
        assertTextPresentInThisOrder("Loading folder properties (folder type, settings and active modules)", " queries imported", "Skipping query validation.");
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

    @Test
    public void verifyVisitProperties()
    {
        String visitLabel = "My visit label";
        String visitSeqNumMin = "999.0";
        String visitSeqNumMax = "999.999";
        String visitProtocolDay = "999.001";
        String visitDescription = "My visit description - " + TRICKY_CHARACTERS_FOR_PROJECT_NAMES + INJECT_CHARS_1 + INJECT_CHARS_2;

        log("Visit Properties: create visit with description");
        goToProjectHome();
        clickFolder(getFolderName());
        goToManageStudy();
        waitAndClickAndWait(Locator.linkWithText("Manage Visits"));
        waitAndClickAndWait(Locator.linkWithText("Create New Visit"));
        waitForElement(Locator.name("description"));
        setFormElement(Locator.name("label"), visitLabel);
        setFormElement(Locator.name("sequenceNumMin"), visitSeqNumMin);
        setFormElement(Locator.name("sequenceNumMax"), visitSeqNumMax);
        setFormElement(Locator.name("description"), visitDescription);
        clickButton("Save");

        log("Visit Properties: edit visit description and set sequence num target");
        waitAndClickAndWait(Locator.xpath("//th[text()='" + visitLabel + "']/../td/a[text()='edit']"));
        waitForElement(Locator.name("description"));
        assertFormElementEquals(Locator.name("label"), visitLabel);
        assertFormElementEquals(Locator.name("description"), visitDescription);
        visitDescription += " <b>testing</b>";
        setFormElement(Locator.name("description"), visitDescription);
        setFormElement(Locator.name("protocolDay"), visitProtocolDay);
        clickButton("Save");

        log("Visit Properties: add dataset record using new visit");
        clickTab("Clinical and Assay Data");
        waitAndClickAndWait(Locator.linkWithText(TEST_DATASET_NAME));
        clickButton("Import Data");
        waitForElement(Locator.name("text"));
        setFormElement(Locator.name("text"), "ParticipantId\tSequenceNum\nPTID123\t" + visitSeqNumMin);
        clickButton("Submit");

        log("Visit Properties: export study folder to the pipeline as indivisual files");
        exportStudyArchive(getFolderName(), "0");

        log("Visit Properties: verify xml file was created in export");
        _fileBrowserHelper.selectFileBrowserItem("export/study/visit_map.xml");

        log("Visit Properties: import study into subfolder");
        createSubfolderAndImportStudyFromPipeline("Visit Properties");

        log("Visit Properties: verify imported settings");
        clickFolder("Visit Properties");
        goToManageStudy();
        waitAndClickAndWait(Locator.linkWithText("Manage Visits"));
        waitAndClickAndWait(Locator.xpath("//th[text()='" + visitLabel + "']/../td/a[text()='edit']"));
        waitForElement(Locator.name("description"));
        assertFormElementEquals(Locator.name("label"), visitLabel);
        assertFormElementEquals(Locator.name("description"), visitDescription);
        assertFormElementEquals(Locator.name("sequenceNumMin"), visitSeqNumMin);
        assertFormElementEquals(Locator.name("sequenceNumMax"), visitSeqNumMax);
        assertFormElementEquals(Locator.name("protocolDay"), visitProtocolDay);

        log("Visit Properties: verify visit description in study navigator hover");
        clickTab("Overview");
        waitAndClickAndWait(Locator.linkWithText("Study Navigator"));
        waitForText(visitLabel);
        click(Locator.css(".labkey-help-pop-up"));
        waitForElement(Locator.xpath("id('helpDivBody')").containing(visitDescription));

        log("Visit Properties: verify visit description in dataset visit column hover");
        clickTab("Clinical and Assay Data");
        waitAndClickAndWait(Locator.linkWithText(TEST_DATASET_NAME));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn(new String[]{"ParticipantVisit", "Visit"});
        _customizeViewsHelper.saveDefaultView();
        mouseOver(Locator.tagWithText("td", visitLabel));
        waitForElement(Locator.xpath("id('helpDivBody')").containing(visitDescription));

        log("Visit Properties: remove visit");
        clickFolder(getFolderName());
        goToManageStudy();
        waitAndClickAndWait(Locator.linkWithText("Manage Visits"));
        waitAndClickAndWait(Locator.xpath("//th[text()='" + visitLabel + "']/../td/a[text()='edit']"));
        clickButton("Delete visit");
        waitForText("Do you want to delete Visit");
        clickButton("Delete");
        waitForText("Manage Visits");
    }

    @Test
    public void verifyStudyProperties()
    {
        Map<String, String> origProps = new HashMap<>();
        Map<String, String> newProps = new HashMap<>();
        newProps.put("Investigator", "Investigator");
        newProps.put("Grant", "Grant");
        newProps.put("Species", "Species");
        newProps.put("Description", "Description");
        newProps.put("StartDate", "2013-01-01");
        newProps.put("EndDate", "2013-12-31");
        newProps.put("SubjectNounSingular", "Subject");
        newProps.put("SubjectNounPlural", "Subjects");
        newProps.put("SubjectColumnName", "SubjectId");

        // add tricky chars and injection script, for non-dates
        for (String key : newProps.keySet())
        {
            // subject noun fields have a length constraint, and leave the dates alone
            if (key.equals("SubjectColumnName"))
            {
                // no op, this field gets truncated by the server using ColumnInfo.legalNameFromName
            }
            else if (key.startsWith("Subject"))
                newProps.put(key, newProps.get(key) + TRICKY_CHARACTERS_FOR_PROJECT_NAMES);
            else if (!key.contains("Date"))
                newProps.put(key, newProps.get(key) + TRICKY_CHARACTERS_FOR_PROJECT_NAMES + INJECT_CHARS_1 + INJECT_CHARS_2);
        }

        log("Study Properties: set study properties of interest");
        goToProjectHome();
        clickFolder(getFolderName());
        goToManageStudy();
        waitAndClickAndWait(Locator.linkWithText("Change Study Properties"));
        waitForElement(Locator.name("Investigator"));
        for (String key : newProps.keySet())
        {
            origProps.put(key, getFormElement(Locator.name(key)));
            setFormElement(Locator.name(key), newProps.get(key));
        }
        clickButton("Submit");

        log("Study Properties: export study folder to the pipeline as indivisual files");
        exportStudyArchive(getFolderName(), "0");

        log("Study Properties: verify xml file was created in export");
        _fileBrowserHelper.selectFileBrowserItem("export/study/study.xml");

        log("Study Properties: import study into subfolder");
        createSubfolderAndImportStudyFromPipeline("Study Properties");

        log("Study Properties: verify imported settings");
        clickFolder("Study Properties");
        goToManageStudy();
        waitAndClickAndWait(Locator.linkWithText("Change Study Properties"));
        waitForElement(Locator.name("Investigator"));
        for (String key : newProps.keySet())
        {
            assertFormElementEquals(Locator.name(key), newProps.get(key));
        }

        log("Study Properties: verify display of some properties in overview webpart");
        waitAndClickAndWait(Locator.linkWithText("Overview"));
        waitForText(newProps.get("Investigator"));
        assertTextPresent(newProps.get("Grant"));
        assertTextPresent(newProps.get("Description"));
        assertElementPresent(Locator.linkWithText(newProps.get("SubjectNounPlural")), 1);

        log("Study Properties: clean up study properties");
        clickFolder(getFolderName());
        goToManageStudy();
        waitAndClickAndWait(Locator.linkWithText("Change Study Properties"));
        waitForElement(Locator.name("Investigator"));
        for (String key : origProps.keySet())
        {
            setFormElement(Locator.name(key), origProps.get(key));
        }
        clickButton("Submit");
    }

    @Test
    public void verifyCohortProperties()
    {
        String cohort1label = "Cohort1";
        String cohort1count = "10";
        String cohort1description = "First Description" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES + INJECT_CHARS_1 + INJECT_CHARS_2;
        String cohort2label = "Cohort2";
        String cohort2count = "55";
        String cohort2description = "Second Description" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES + INJECT_CHARS_1 + INJECT_CHARS_2;

        log("Cohort Properties: create new cohorts");
        goToProjectHome();
        clickFolder(getFolderName());
        goToManageStudy();
        waitAndClickAndWait(Locator.linkWithText("Manage Cohorts"));
        clickButton("Insert New");
        waitForElement(Locator.name("quf_label"));
        setFormElement(Locator.name("quf_label"), cohort1label);
        setFormElement(Locator.name("quf_subjectCount"), cohort1count);
        setFormElement(Locator.name("quf_description"), cohort1description);
        clickButton("Submit");
        clickButton("Insert New");
        waitForElement(Locator.name("quf_label"));
        setFormElement(Locator.name("quf_label"), cohort2label);
        setFormElement(Locator.name("quf_subjectCount"), cohort2count);
        setFormElement(Locator.name("quf_description"), cohort2description);
        clickButton("Submit");

        log("Cohort Properties: export study folder to the pipeline as indivisual files");
        exportStudyArchive(getFolderName(), "0");

        log("Cohort Properties: verify xml file was created in export");
        _fileBrowserHelper.selectFileBrowserItem("export/study/cohorts.xml");

        log("Cohort Properties: import study into subfolder");
        createSubfolderAndImportStudyFromPipeline("Cohort Properties");

        log("Cohort Properties: verify imported settings");
        clickFolder("Cohort Properties");
        goToManageStudy();
        waitAndClickAndWait(Locator.linkWithText("Manage Cohorts"));
        waitForText(cohort1label);
        clickAndWait(Locator.linkWithText("edit", 0));
        waitForText("Update Cohort: " + cohort1label);
        assertFormElementEquals(Locator.name("quf_subjectCount"), cohort1count);
        assertFormElementEquals(Locator.name("quf_description"), cohort1description);
        clickButton("Cancel");
        waitForText(cohort2label);
        clickAndWait(Locator.linkWithText("edit", 1));
        waitForText("Update Cohort: " + cohort2label);
        assertFormElementEquals(Locator.name("quf_subjectCount"), cohort2count);
        assertFormElementEquals(Locator.name("quf_description"), cohort2description);
        clickButton("Cancel");

        log("Cohort Properties: verify display of cohorts in subjects webpart");
        waitAndClickAndWait(Locator.linkWithText("Participants"));
        waitForElement(Locator.tagWithClass("span", "lk-filter-panel-label").withText(cohort1label));
        assertElementPresent(Locator.tagWithClass("span", "lk-filter-panel-label").withText(cohort2label));

        log("Cohort Properties: clean up cohorts");
        clickFolder(getFolderName());
        goToManageStudy();
        waitAndClickAndWait(Locator.linkWithText("Manage Cohorts"));
        clickAndWait(Locator.linkWithText("delete")); // first cohort
        clickAndWait(Locator.linkWithText("delete")); // second cohort
        waitForText("No data to show.");
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
