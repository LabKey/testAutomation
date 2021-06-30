/*
 * Copyright (c) 2013-2019 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyB.class})
@BaseWebDriverTest.ClassTimeout(minutes = 7)
public class SurveyTest extends BaseWebDriverTest
{
    private static final String FOLDER_NAME = "subfolder";
    private static final File PIPELINE_LOC = TestFileUtils.getSampleData("survey");
    private static final String PROJECT_SURVEY_DESIGN = "My Project Survey Design";
    private static final String SUBFOLDER_SURVEY_DESIGN = "My Subfolder Survey Design";
    private static final String FIRST_SURVEY = "First Survey";
    private static final String SECOND_SURVEY = "Second Survey";
    public static final String EDITOR = "editor_survey@survey.test";

    private final PortalHelper portalHelper = new PortalHelper(this);

    private static final String SUCCESS_DIALOG_TITLE = "Success";

    private static final String LIST_NAME = "listA";
    private static final String LIST_SCHEMA = "lists";

    // Name of survey fields on the form (used by locators).
    private static final String SURVEY_LABEL_NAME = "_surveyLabel_";
    private static final String TEXT_FIELD_NAME = "txtfield";
    private static final String TEXT_AREA_FIELD_NAME = "txtareafield";
    private static final String DATE_FIELD_NAME = "dtfield";
    private static final String DATETIME_DATE_FIELD_NAME = "dttimefield-date";
    private static final String DATETIME_TIME_FIELD_NAME = "dttimefield-time";

    // Prompt for the survey label on the form. Used to check that the page has loaded.
    private static final String SURVEY_LABEL_PROMPT = "Survey Label*";

    @Override
    protected String getProjectName()
    {
        return "SurveyTest" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @BeforeClass
    @LogMethod
    public static void doSetup()
    {
        SurveyTest initTest = (SurveyTest)getCurrentTest();
        initTest.setupProject();
    }

    @LogMethod
    private void setupProject()
    {
        setupProjectFolder();
        setupSubfolder();
    }

    @Test
    public void testProjectSurvey()
    {
        verifySurveyFromProject();
        verifyEditSurvey();
        verifySubmitSurvey();
    }

    @Test
    public void testSubfolderSurvey()
    {
        verifySurveyFromSubfolder();
        verifySurveyContainerPermissions();
    }

    @Test
    public void testDateTimeWithExtConfig()
    {

        final String dateSurveyDesign = "My extDate Survey Design";

        File metadata = TestFileUtils.getSampleData("survey/DateAndDateTimeSurvey.json");
        createSurveyDesign(getProjectName(), null, null, dateSurveyDesign, null, LIST_SCHEMA, LIST_NAME, metadata);

        clickProject(getProjectName());
        clickFolder(FOLDER_NAME);

        log("Add a web part to take the survey.");
        addSurveyWebpart(dateSurveyDesign);
        clickCreateSurvey(dateSurveyDesign);

        var dateSurveyLabel = "Date Survey";
        setFormElement(Locator.name(SURVEY_LABEL_NAME), dateSurveyLabel);

        // Enter some text values just as a sanity check that the round trip in general is working.
        var txtFieldValue = "Hot!";
        setFormElement(Locator.name(TEXT_FIELD_NAME), txtFieldValue);

        var txtAreaFieldValue = "It is really hot today!";
        setFormElement(Locator.name(TEXT_AREA_FIELD_NAME), txtAreaFieldValue);

        var dateFieldValue = "2021-06-28";
        setFormElement(Locator.name(DATE_FIELD_NAME), dateFieldValue);

        var dateTimeFieldDateValue = "2021-06-27";
        setFormElement(Locator.name(DATETIME_DATE_FIELD_NAME), dateTimeFieldDateValue);

        var dateTimeFieldTimeValue = "12:45";
        setFormElement(Locator.name(DATETIME_TIME_FIELD_NAME), dateTimeFieldTimeValue);

        log("Save the survey.");
        clickSaveButton();

        log("Verify that the survey was saved and the responses were saved to the subfolder list");
        clickFolder(FOLDER_NAME);
        waitForText(dateSurveyLabel);
        clickAndWait(Locator.linkWithText(LIST_NAME));

        assertTextPresent(txtFieldValue, txtAreaFieldValue, dateFieldValue, String.format("%s %s", dateTimeFieldDateValue, dateTimeFieldTimeValue));

        log("Open the saved survey response and validate that the fields are populated as expected.");

        clickProject(getProjectName());
        clickFolder(FOLDER_NAME);

        DataRegionTable drt = DataRegionTable.findDataRegionWithinWebpart(this, String.format("Surveys: %s", dateSurveyDesign));
        drt.clickEditRow(0);

        waitForText(SURVEY_LABEL_PROMPT);

        var retrievedValue = getFormElement(Locator.name(SURVEY_LABEL_NAME));
        checker().verifyEquals("Label not as expected.", dateSurveyLabel, retrievedValue);

        retrievedValue = getFormElement(Locator.name(TEXT_FIELD_NAME));
        checker().verifyEquals("Text field not as expected.", txtFieldValue, retrievedValue);

        retrievedValue = getFormElement(Locator.name(TEXT_AREA_FIELD_NAME));
        checker().verifyEquals("TextArea field not as expected.", txtAreaFieldValue, retrievedValue);

        retrievedValue = getFormElement(Locator.name(DATE_FIELD_NAME));
        checker().verifyEquals("Date field not as expected.", dateFieldValue, retrievedValue);

        retrievedValue = getFormElement(Locator.name(DATETIME_DATE_FIELD_NAME));
        checker().verifyEquals("DateTime Date field not as expected.", dateTimeFieldDateValue, retrievedValue);

        retrievedValue = getFormElement(Locator.name(DATETIME_TIME_FIELD_NAME));
        checker().verifyEquals("DateTime Time field not as expected.", String.format("%s:00", dateTimeFieldTimeValue), retrievedValue);

    }

    private void clickSaveButton()
    {
        clickButton("Save", 0);
        _extHelper.waitForExtDialog(SUCCESS_DIALOG_TITLE);
        _extHelper.waitForExtDialogToDisappear(SUCCESS_DIALOG_TITLE);
    }

    private void clickCreateSurvey(String surveyName)
    {
        clickCreateSurvey(surveyName, SURVEY_LABEL_PROMPT);
    }

    private void clickCreateSurvey(String surveyName, String waitForText)
    {
        DataRegionTable
                .findDataRegionWithinWebpart(this, String.format("Surveys: %s", surveyName))
                .clickHeaderButton("Create Survey");

        waitForText(waitForText);
    }

    @LogMethod
    private void setupProjectFolder()
    {
        _containerHelper.createProject(getProjectName(), null);

        log("Create survey design at the project level");
        _listHelper.importListArchive(getProjectName(), new File(PIPELINE_LOC, "ListA.zip"));
        _containerHelper.enableModule(getProjectName(), "Survey");
        portalHelper.addWebPart("Survey Designs");
        createSurveyDesign(getProjectName(), null, null, PROJECT_SURVEY_DESIGN, null, LIST_SCHEMA, LIST_NAME, null);
    }

    @LogMethod
    private void setupSubfolder()
    {
        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME);

        log("Create survey design at the subfolder level");
        _listHelper.importListArchive(FOLDER_NAME, new File(PIPELINE_LOC, "ListA.zip"));
        clickFolder(FOLDER_NAME);
        portalHelper.addWebPart("Survey Designs");
        createSurveyDesign(FOLDER_NAME, null, null, SUBFOLDER_SURVEY_DESIGN, null, LIST_SCHEMA, LIST_NAME, null);

        log("Add users that will be used for permissions testing");
        _userHelper.createUser(EDITOR);
        clickProject(getProjectName());
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setUserPermissions(EDITOR, "Reader");
        clickButton("Save and Finish");
        clickFolder(FOLDER_NAME);
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setUserPermissions(EDITOR, "Editor");
        clickButton("Save and Finish");
    }

    protected void createSurveyDesign(String project, @Nullable String folder, @Nullable String tabName, String designName, @Nullable String description,
                                      String schemaName, String queryName, @Nullable File metadataFile)
    {
        log("Create new survey design");
        if (folder != null && !isElementPresent(Locator.id("folderBar").withText(folder)))
            clickFolder(folder);
        if (tabName != null && !isElementPresent(Locator.xpath("//li[contains(@class, 'tab-nav-active')]/a").withText(tabName)))
            clickAndWait(Locator.linkWithText(tabName));
        DataRegionTable.DataRegion(getDriver()).withName("query").waitFor();
        createSurveyDesign(designName, description, schemaName, queryName, metadataFile);
    }

    @LogMethod
    public void verifySurveyFromProject()
    {
        // add a survey webpart to the subfolder using the project survey design
        clickProject(getProjectName());
        clickFolder(FOLDER_NAME);
        addSurveyWebpart(PROJECT_SURVEY_DESIGN);

        log("Create a new survey instance (i.e. take the survey)");
        clickCreateSurvey(PROJECT_SURVEY_DESIGN);

        // verify that the save and submit buttons are disabled (and that they are visible, since this is the "auto" survey layout)
        assertTrue("Save button should be initially disabled", isElementPresent(Locator.xpath("//a[contains(@class,'item-disabled')]//span[text() = 'Save']")));
        assertTrue("Submit button should be initially disabled", isElementPresent(Locator.xpath("//a[contains(@class,'item-disabled')]//span[text() = 'Submit completed form']")));
        // set form field values
        setFormElement(Locator.name(SURVEY_LABEL_NAME), FIRST_SURVEY);
        setFormElement(Locator.name(TEXT_FIELD_NAME), "txtField");
        setFormElement(Locator.name(TEXT_AREA_FIELD_NAME), "txtAreaField");
        _ext4Helper.checkCheckbox("Bool Field");
        setFormElement(Locator.name("intfield"), "999");
        setFormElement(Locator.name("dblfield"), "999.1");
        setFormElement(Locator.name(DATE_FIELD_NAME), "2013-01-04");
        addSurveyFileAttachment("attfield", new File(PIPELINE_LOC, "TestAttachment.txt"));
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Lk Field']]"), Ext4Helper.TextMatchTechnique.CONTAINS, "Test1");
        log("Wait for the survey autosave (save attempts every minute)");
        waitForText(65000, "Responses automatically saved at");

        log("Verify that the survey was saved and the responses were saved to the subfolder list");
        clickFolder(FOLDER_NAME);
        waitForText(FIRST_SURVEY);
        clickAndWait(Locator.linkWithText(LIST_NAME));
        assertTextPresent("txtField", "txtAreaField", "true", "999", "999.1", "2013-01-04", "Test1");
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText(LIST_NAME));
        waitForText("No data to show.");
    }

    private void addSurveyFileAttachment(String inputName, File file)
    {
        // TODO: implement file attachment for the survey form
        //setFormElement(Locator.name(inputName), file);
    }

    @LogMethod
    private void verifyEditSurvey()
    {
        log("Edit the survey in the specified folder");
        clickFolder(FOLDER_NAME);
        clickEditForLabel("Surveys: My Project Survey Design" , FIRST_SURVEY);
        _ext4Helper.waitForMaskToDisappear();
        setFormElement(Locator.name(TEXT_AREA_FIELD_NAME), "txtAreaField\nnew line");
        _ext4Helper.uncheckCheckbox("Bool Field");
        sleep(500); // give the save button a split second to enable based on form changes
        clickSaveButton();

        //verify that the responses were saved with their changes
        clickFolder(FOLDER_NAME);
        clickAndWait(Locator.linkWithText(LIST_NAME));
        assertTextPresent("txtField", "txtAreaField", "new line", "false", "999", "999.1", "2013-01-04", "Test1");
    }

    @LogMethod
    private void verifySubmitSurvey()
    {
        // TODO: add a required field to the survey and verify the submit button disables when it doesn't have a value

        log("Submit the completed survey in the specified folder");
        clickFolder(FOLDER_NAME);
        clickEditForLabel("Surveys: My Project Survey Design", FIRST_SURVEY);
        _ext4Helper.waitForMaskToDisappear();
        assertElementPresent(Ext4Helper.Locators.ext4Button("Save"));
        assertElementPresent(Ext4Helper.Locators.ext4Button("Submit completed form"));
        clickButton("Submit completed form");
        // go back to the submitted survey and verify the submit button is gone
        // TODO: add verification that site/project admins can still see Save button but other users can not for a submitted survey
        clickEditForLabel("Surveys: My Project Survey Design", FIRST_SURVEY);
        _ext4Helper.waitForMaskToDisappear();
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Submit completed form"));
        assertTextPresent("Submitted by");

        log("Verify that only admins can make changes to a submitted survey");
        // we should currently be logged in as site admin
        assertTextPresent("You are allowed to make changes to this form because you are a project/site administrator.");
        assertTrue("Save button should be disabled", isElementPresent(Locator.xpath("//a[contains(@class,'item-disabled')]//span[text() = 'Save']")));
        setFormElement(Locator.name(TEXT_AREA_FIELD_NAME), "edit by admin after submit");
        _ext4Helper.checkCheckbox("Bool Field");
        sleep(500); // give the save button a split second to enable based on form changes
        clickSaveButton();

        log("Verify that non-admin can't edit a submitted survey");
        pushLocation();
        impersonate(EDITOR);
        popLocation();
        waitForText(SURVEY_LABEL_PROMPT);
        assertEquals("edit by admin after submit", getFormElement(Locator.name(TEXT_AREA_FIELD_NAME)));
        assertTextPresent("Submitted by");
        assertTextNotPresent("You are allowed to make changes to this form");
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Save"));
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Submit completed form"));
        stopImpersonating();
        clickProject(getProjectName());

        // remove the webpart
        clickFolder(FOLDER_NAME);
        removeSurveyWebpart(PROJECT_SURVEY_DESIGN);
    }

    @LogMethod
    private void verifySurveyFromSubfolder()
    {

        String headerWikiBody = "Header wiki content to appear above the survey form panel.";
        String footerWikiBody = "Footer wiki content to appear below the survey form panel.";

        WikiHelper wikiHelper = new WikiHelper(this);

        log("Create wikis for survey header/footer");
        clickProject(getProjectName());
        clickFolder(FOLDER_NAME);
        goToModule("Wiki");
        wikiHelper.createNewWikiPage();
        setFormElement(Locator.name("name"), "header_wiki");
        wikiHelper.setWikiBody(headerWikiBody);
        wikiHelper.saveWikiPage();
        goToModule("Wiki");
        wikiHelper.createNewWikiPage();
        setFormElement(Locator.name("name"), "footer_wiki");
        wikiHelper.setWikiBody(footerWikiBody);
        wikiHelper.saveWikiPage();

        log("Customize the survey design metadata (card layout, multiple sections, show question counts, etc.)");
        clickFolder(FOLDER_NAME);
        clickEditForLabel("Survey Designs", SUBFOLDER_SURVEY_DESIGN);
        String json = TestFileUtils.getFileContents(new File(PIPELINE_LOC, "CustomSurveyMetadata.json"));
        _extHelper.setCodeMirrorValue("metadata", json);
        clickButton("Save Survey");

        // add the subfolder survey design webpart
        addSurveyWebpart(SUBFOLDER_SURVEY_DESIGN);

        log("Verify the card layout (i.e. has section headers on left, not all questions visible, etc.)");

        clickCreateSurvey(SUBFOLDER_SURVEY_DESIGN, "Start");

        assertElementPresent(Locator.xpath("//li[text()='Start']"));
        assertElementPresent(Locator.xpath("//li[text()='Section 1']"));
        assertElementPresent(Locator.xpath("//li[text()='Section 2']"));
        assertElementPresent(Locator.xpath("//li[text()='Save / Submit']"));
        assertElementPresent(Ext4Helper.Locators.ext4Button("Previous"));
        assertElementPresent(Ext4Helper.Locators.ext4Button("Next"));
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Submit completed form"));
        assertTextPresentInThisOrder(headerWikiBody, footerWikiBody);

        // set form field values
        setFormElement(Locator.name(SURVEY_LABEL_NAME), SECOND_SURVEY);
        clickButton("Next", 0);
        waitForText("Description for section 1");
        // leave the required txtField blank for now
        addSurveyGridQuestionRecord("field1", "field2");
        clickButton("Next", 0);
        waitForText("Description for section 2");
        _ext4Helper.checkCheckbox("Bool Field");
        // leave the required intField blank for now
        setFormElement(Locator.name("dblfield"), "999.1");
        // set the date to an invalid format
        setFormElement(Locator.name(DATE_FIELD_NAME), "01/04/2013");
        // set the date for the date/time field
        setFormElement(Locator.name(DATETIME_DATE_FIELD_NAME), "2018-10-01");

        // check survey skip logic that attachment field appears with selectin of lkField = Test1
        assertElementPresent(Locator.xpath("//table[contains(@style,'display: none;')]//label[text()='Att Field']"));
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Lk Field']]"), Ext4Helper.TextMatchTechnique.CONTAINS, "Test1");
        assertElementNotPresent(Locator.xpath("//table[contains(@style,'display: none;')]//label[text()='Att Field']"));
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Lk Field']]"), Ext4Helper.TextMatchTechnique.CONTAINS, "Test2");
        assertElementPresent(Locator.xpath("//table[contains(@style,'display: none;')]//label[text()='Att Field']"));
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Lk Field']]"), Ext4Helper.TextMatchTechnique.CONTAINS, "Test1");
        addSurveyFileAttachment("attField", new File(PIPELINE_LOC, "TestAttachment.txt"));
        clickButton("Next", 0);
        // check submit button text about invalid fields
        waitForText("Note: The following fields must be valid before you can submit the form");
        assertTextPresentInThisOrder("-Txt Field", "-Int Field", "-Dt Field");
        // go back and fix the validation errors
        clickButton("Previous", 0);
        waitForText("Description for section 2");
        setFormElement(Locator.name("intfield"), "999");
        setFormElement(Locator.name(DATE_FIELD_NAME), "2013-01-04");
        clickButton("Previous", 0);
        waitForText("Description for section 1");
        setFormElement(Locator.name(TEXT_FIELD_NAME), "txtField");
        clickButton("Next", 0);
        clickButton("Next", 0);
        // verify question counts on section header
        assertTextPresent("Section 1 (2)", "Section 2 (7)");
        assertTextNotPresent("-Txt Field", "-Int Field", "-Dt Field");
        assertTrue("Submit button should not be disabled", !isElementPresent(Locator.xpath("//a[contains(@class,'item-disabled')]//span[text() = 'Submit completed form']")));
        clickButton("Submit completed form");
        waitForText("Surveys: " + SUBFOLDER_SURVEY_DESIGN);
        assertTextPresent(SECOND_SURVEY);
        // verify survey responses in the current folder
        clickAndWait(Locator.linkWithText(LIST_NAME));
        assertTextPresent("[{\"field1\":\"field1\",\"field2\":\"field2\"}]", "2018-10-01 00:00");

        clickFolder(FOLDER_NAME);
        removeSurveyWebpart(SUBFOLDER_SURVEY_DESIGN);
    }

    private void addSurveyWebpart(String surveyDesignName)
    {
        log("Configure Surveys webpart");
        portalHelper.addWebPart("Surveys");
        waitForElement(Locator.css(".survey-designs-loaded-marker"));
        _ext4Helper.selectComboBoxItem("Survey Design:", surveyDesignName);
        clickButton("Submit");
        waitForText("Surveys: " + surveyDesignName);
    }

    private void removeSurveyWebpart(String surveyDesignName)
    {
        log("Remove Surveys webpart");
        portalHelper.removeWebPart("Surveys: " + surveyDesignName);
    }

    @LogMethod
    private void verifySurveyContainerPermissions()
    {
        log("Verify survey designs (current and parent container)");
        clickFolder(FOLDER_NAME);
        assertTextPresentInThisOrder("My Project Survey Design", "My Subfolder Survey Design");
        clickProject(getProjectName());
        assertTextPresent("My Project Survey Design");
        assertTextNotPresent("My Subfolder Survey Design");

        log("Add Survey webpart to project and verify subfolder survey is not present");
        clickProject(getProjectName());
        addSurveyWebpart(PROJECT_SURVEY_DESIGN);
        assertTextPresent("No data to show.");
        assertTextNotPresent(FIRST_SURVEY, SECOND_SURVEY);

        removeSurveyWebpart(PROJECT_SURVEY_DESIGN);
    }

    private void addSurveyGridQuestionRecord(String val1, String val2)
    {
        log("Add record for SurveyGridQuestion");
        clickButton("Add Record", 0);
        _extHelper.waitForExtDialog("Add Record");
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Field 1:']]"), val1);
        clickButton("Update", 0);

        log("Edit record for SurveyGridQuestion");
        _ext4Helper.clickGridRowText("field1", 0);
        clickButton("Edit Selected", 0);
        _extHelper.waitForExtDialog("Edit Record");
        assertEquals("Wrong value for 'field1'", val1, getFormElement(Locator.name("field1")));
        setFormElement(Locator.name("field2"), val2);
        clickButton("Update", 0);

        log("Delete record for SurveyGridQuestion");
        clickButton("Add Record", 0);
        _extHelper.waitForExtDialog("Add Record");
        setFormElement(Locator.name("field2"), "tobedeleted");
        clickButton("Update", 0);
        _ext4Helper.clickGridRowText("tobedeleted", 0);
        clickButton("Delete Selected", 0);
        assertTextNotPresent("tobedeleted");
    }

    private void clickEditForLabel(String webPartTitle, String label)
    {
        waitForText(label);
        DataRegionTable dt = DataRegionTable.findDataRegionWithinWebpart(this, webPartTitle);
        dt.clickEditRow(dt.getRowIndex("Label", label));
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteUsersIfPresent(EDITOR);
        super.doCleanup(afterTest);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.singletonList("survey");
    }

    @Test
    public void testSurveyAutoSave()
    {
        clickProject(getProjectName());
        clickFolder(FOLDER_NAME);

        // add survey metadata which overrides the autosave interval to 500 ms.
        String autoSaveSurveyDesign = "Auto Save Survey Design";

        File metadata = TestFileUtils.getSampleData("survey/AutoSaveMetadata.json");
        createSurveyDesign(getCurrentProject(), null, null, autoSaveSurveyDesign, null, LIST_SCHEMA, LIST_NAME, metadata);

        addSurveyWebpart(autoSaveSurveyDesign);

        clickCreateSurvey(autoSaveSurveyDesign);

        setFormElement(Locator.name(SURVEY_LABEL_NAME), "autosave override");
        waitForText(1000, "Responses automatically saved at");
        clickProject(getProjectName());
        clickFolder(FOLDER_NAME);

        // add survey metadata which disables autosave.
        clickEditForLabel("Survey Designs", autoSaveSurveyDesign);
        String json = TestFileUtils.getFileContents(new File(PIPELINE_LOC, "AutoSaveDisabledMetadata.json"));
        _extHelper.setCodeMirrorValue("metadata", json);
        clickButton("Save Survey");

        clickCreateSurvey(autoSaveSurveyDesign);

        setFormElement(Locator.name(SURVEY_LABEL_NAME), "autosave disabled");
        waitFor(() -> isTextPresent("Responses automatically saved at"), 65000);
        assertTextNotPresent("Responses automatically saved at");
        clickSaveButton();

        clickProject(getProjectName());
        clickFolder(FOLDER_NAME);
        removeSurveyWebpart(autoSaveSurveyDesign);
    }
}
