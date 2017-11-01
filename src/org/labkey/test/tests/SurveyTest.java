/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
public class SurveyTest extends BaseWebDriverTest
{
    private final String folderName = "subfolder";
    protected final String pipelineLoc =  "/sampledata/survey";
    private final String projectSurveyDesign = "My Project Survey Design";
    private final String subfolderSurveyDesign = "My Subfolder Survey Design";
    private final String firstSurvey = "First Survey";
    private final String secondSurvey = "Second Survey";
    private final String headerWikiBody = "Header wiki content to appear above the survey form panel.";
    private final String footerWikiBody = "Footer wiki content to appear below the survey form panel.";
    public static final String EDITOR = "editor_survey@survey.test";

    private final PortalHelper portalHelper = new PortalHelper(this);

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

    @Test
    public void testSteps()
    {
        setupProjectFolder();
        setupSubfolder();

        verifySurveyFromProject();
        verifyEditSurvey();
        verifySubmitSurvey();
        verifySurveyFromSubfolder();
        verifySurveyContainerPermissions();
    }

    @LogMethod
    private void setupProjectFolder()
    {
        _containerHelper.createProject(getProjectName(), null);

        log("Create survey design at the project level");
        _listHelper.importListArchive(getProjectName(), new File(TestFileUtils.getLabKeyRoot() + pipelineLoc, "ListA.zip"));
        _containerHelper.enableModule(getProjectName(), "Survey");
        portalHelper.addWebPart("Survey Designs");
        createSurveyDesign(getProjectName(), null, null, projectSurveyDesign, null, "lists", "listA");
    }

    @LogMethod
    private void setupSubfolder()
    {
        _containerHelper.createSubfolder(getProjectName(), folderName);

        log("Create survey design at the subfolder level");
        _listHelper.importListArchive(folderName, new File(TestFileUtils.getLabKeyRoot() + pipelineLoc, "ListA.zip"));
        clickFolder(folderName);
        portalHelper.addWebPart("Survey Designs");
        createSurveyDesign(folderName, null, null, subfolderSurveyDesign, null, "lists", "listA");

        log("Add users that will be used for permissions testing");
        _userHelper.createUser(EDITOR);
        clickProject(getProjectName());
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setUserPermissions(EDITOR, "Reader");
        clickButton("Save and Finish");
        clickFolder(folderName);
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setUserPermissions(EDITOR, "Editor");
        clickButton("Save and Finish");
    }

    protected void createSurveyDesign(String project, @Nullable String folder, @Nullable String tabName, String designName, @Nullable String description,
                                      String schemaName, String queryName)
    {
        log("Create new survey design");
        if (folder != null && !isElementPresent(Locator.id("folderBar").withText(folder)))
            clickFolder(folder);
        if (tabName != null && !isElementPresent(Locator.xpath("//li[contains(@class, 'tab-nav-active')]/a").withText(tabName)))
            clickAndWait(Locator.linkWithText(tabName));
        DataRegionTable.DataRegion(getDriver()).withName("query").waitFor();
        createSurveyDesign(designName, description, schemaName, queryName, null);
    }

    @LogMethod
    private void verifySurveyFromProject()
    {
        // add a survey webpart to the subfolder using the project survey design
        clickFolder(folderName);
        addSurveyWebpart(projectSurveyDesign);

        log("Create a new survey instance (i.e. take the survey)");
        clickButtonByIndex("Create Survey", 0, WAIT_FOR_JAVASCRIPT);
        waitForText("Survey Label*");
        // verify that the save and submit buttons are disabled (and that they are visible, since this is the "auto" survey layout)
        assertTrue("Save button should be initially disabled", isElementPresent(Locator.xpath("//a[contains(@class,'item-disabled')]//span[text() = 'Save']")));
        assertTrue("Submit button should be initially disabled", isElementPresent(Locator.xpath("//a[contains(@class,'item-disabled')]//span[text() = 'Submit completed form']")));
        // set form field values
        setFormElement(Locator.name("_surveyLabel_"), firstSurvey);
        setFormElement(Locator.name("txtfield"), "txtField");
        setFormElement(Locator.name("txtareafield"), "txtAreaField");
        _ext4Helper.checkCheckbox("Bool Field");
        setFormElement(Locator.name("intfield"), "999");
        setFormElement(Locator.name("dblfield"), "999.1");
        setFormElement(Locator.name("dtfield"), "2013-01-04");
        addSurveyFileAttachment("attfield", pipelineLoc + "/TestAttachment.txt");
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Lk Field']]"), Ext4Helper.TextMatchTechnique.CONTAINS, "Test1");
        log("Wait for the survey autosave (save attempts every minute)");
        waitForText(65000, "Responses automatically saved at");

        log("Verify that the survey was saved and the responses were saved to the subfolder list");
        clickFolder(folderName);
        waitForText(firstSurvey);
        clickAndWait(Locator.linkWithText("listA"));
        assertTextPresentInThisOrder("txtField", "txtAreaField", "true", "999", "999.1", "2013-01-04", "Test1");
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("listA"));
        waitForText("No data to show.");
    }

    private void addSurveyFileAttachment(String inputName, String fileName)
    {
        // TODO: implement file attachment for the survey form
        //setFormElement(Locator.name(inputName), new File(getLabKeyRoot() + fileName));
    }

    @LogMethod
    private void verifyEditSurvey()
    {
        log("Edit the survey in the specified folder");
        clickFolder(folderName);
        clickEditForLabel("Surveys: My Project Survey Design" ,firstSurvey);
        _ext4Helper.waitForMaskToDisappear();
        setFormElement(Locator.name("txtareafield"), "txtAreaField\nnew line");
        _ext4Helper.uncheckCheckbox("Bool Field");
        sleep(500); // give the save button a split second to enable based on form changes
        clickButton("Save", 0);
        _extHelper.waitForExtDialog("Success");
        _extHelper.waitForExtDialogToDisappear("Success");

        //verify that the responses were saved with their changes
        clickFolder(folderName);
        clickAndWait(Locator.linkWithText("listA"));
        assertTextPresentInThisOrder("txtField", "txtAreaField", "new line", "false", "999", "999.1", "2013-01-04", "Test1");
    }

    @LogMethod
    private void verifySubmitSurvey()
    {
        // TODO: add a required field to the survey and verify the submit button disables when it doesn't have a value

        log("Submit the completed survey in the specified folder");
        clickFolder(folderName);
        clickEditForLabel("Surveys: My Project Survey Design", firstSurvey);
        _ext4Helper.waitForMaskToDisappear();
        assertElementPresent(Ext4Helper.Locators.ext4Button("Save"));
        assertElementPresent(Ext4Helper.Locators.ext4Button("Submit completed form"));
        clickButton("Submit completed form");
        // go back to the submitted survey and verify the submit button is gone
        // TODO: add verification that site/project admins can still see Save button but other users can not for a submitted survey
        clickEditForLabel("Surveys: My Project Survey Design", firstSurvey);
        _ext4Helper.waitForMaskToDisappear();
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Submit completed form"));
        assertTextPresent("Submitted by");

        log("Verify that only admins can make changes to a submitted survey");
        // we should currently be logged in as site admin
        assertTextPresent("You are allowed to make changes to this form because you are a project/site administrator.");
        assertTrue("Save button should be disabled", isElementPresent(Locator.xpath("//a[contains(@class,'item-disabled')]//span[text() = 'Save']")));
        setFormElement(Locator.name("txtareafield"), "edit by admin after submit");
        _ext4Helper.checkCheckbox("Bool Field");
        sleep(500); // give the save button a split second to enable based on form changes
        click(Ext4Helper.Locators.ext4Button("Save"));
        _extHelper.waitForExtDialog("Success");
        _extHelper.waitForExtDialogToDisappear("Success");

        log("Verify that non-admin can't edit a submitted survey");
        pushLocation();
        impersonate(EDITOR);
        popLocation();
        waitForText("Survey Label*");
        assertEquals("edit by admin after submit", getFormElement(Locator.name("txtareafield")));
        assertTextPresent("Submitted by");
        assertTextNotPresent("You are allowed to make changes to this form");
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Save"));
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Submit completed form"));
        stopImpersonating();
        clickProject(getProjectName());
    }

    @LogMethod
    private void verifySurveyFromSubfolder()
    {
        WikiHelper wikiHelper = new WikiHelper(this);

        log("Create wikis for survey header/footer");
        clickFolder(folderName);
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
        clickFolder(folderName);
        clickEditForLabel("Survey Designs", subfolderSurveyDesign);
        String json = TestFileUtils.getFileContents(pipelineLoc + "/CustomSurveyMetadata.json");
        _extHelper.setCodeMirrorValue("metadata", json);
        clickButton("Save Survey");

        // add the subfolder survey design webpart
        addSurveyWebpart(subfolderSurveyDesign);

        log("Verify the card layout (i.e. has section headers on left, not all questions visible, etc.)");
        clickButtonByIndex("Create Survey", 1, WAIT_FOR_JAVASCRIPT);
        waitForText("Start");
        assertElementPresent(Locator.xpath("//li[text()='Start']"));
        assertElementPresent(Locator.xpath("//li[text()='Section 1']"));
        assertElementPresent(Locator.xpath("//li[text()='Section 2']"));
        assertElementPresent(Locator.xpath("//li[text()='Save / Submit']"));
        assertElementPresent(Ext4Helper.Locators.ext4Button("Previous"));
        assertElementPresent(Ext4Helper.Locators.ext4Button("Next"));
        assertElementNotPresent(Ext4Helper.Locators.ext4Button("Submit completed form"));
        assertTextPresentInThisOrder(headerWikiBody, footerWikiBody);

        // set form field values
        setFormElement(Locator.name("_surveyLabel_"), secondSurvey);
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
        setFormElement(Locator.name("dtfield"), "01/04/2013");
        // check survey skip logic that attachment field appears with selectin of lkField = Test1
        assertElementPresent(Locator.xpath("//table[contains(@style,'display: none;')]//label[text()='Att Field']"));
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Lk Field']]"), Ext4Helper.TextMatchTechnique.CONTAINS, "Test1");
        assertElementNotPresent(Locator.xpath("//table[contains(@style,'display: none;')]//label[text()='Att Field']"));
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Lk Field']]"), Ext4Helper.TextMatchTechnique.CONTAINS, "Test2");
        assertElementPresent(Locator.xpath("//table[contains(@style,'display: none;')]//label[text()='Att Field']"));
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Lk Field']]"), Ext4Helper.TextMatchTechnique.CONTAINS, "Test1");
        addSurveyFileAttachment("attField", pipelineLoc + "/TestAttachment.txt");
        clickButton("Next", 0);
        // check submit button text about invalid fields
        waitForText("Note: The following fields must be valid before you can submit the form");
        assertTextPresentInThisOrder("-Txt Field", "-Int Field", "-Dt Field");
        // go back and fix the validation errors
        clickButton("Previous", 0);
        waitForText("Description for section 2");
        setFormElement(Locator.name("intfield"), "999");
        setFormElement(Locator.name("dtfield"), "2013-01-04");
        clickButton("Previous", 0);
        waitForText("Description for section 1");
        setFormElement(Locator.name("txtfield"), "txtField");
        clickButton("Next", 0);
        clickButton("Next", 0);
        // verify question counts on section header
        assertTextPresent("Section 1 (2)", "Section 2 (5)");
        assertTextNotPresent("-Txt Field", "-Int Field", "-Dt Field");
        assertTrue("Submit button should not be disabled", !isElementPresent(Locator.xpath("//a[contains(@class,'item-disabled')]//span[text() = 'Submit completed form']")));
        clickButton("Submit completed form");
        waitForText("Surveys: " + subfolderSurveyDesign);
        assertTextPresent(secondSurvey);
        // verify survey responses in the current folder
        clickAndWait(Locator.linkWithText("listA"));
        assertTextPresent("[{\"field1\":\"field1\",\"field2\":\"field2\"}]");
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

    @LogMethod
    private void verifySurveyContainerPermissions()
    {
        log("Verify survey designs (current and parent container)");
        clickFolder(folderName);
        assertTextPresentInThisOrder("My Project Survey Design", "My Subfolder Survey Design");
        clickProject(getProjectName());
        assertTextPresent("My Project Survey Design");
        assertTextNotPresent("My Subfolder Survey Design");

        log("Add Survey webpart to project and verify subfolder survey is not present");
        clickProject(getProjectName());
        addSurveyWebpart(projectSurveyDesign);
        assertTextPresent("No data to show.");
        assertTextNotPresent(firstSurvey, secondSurvey);
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
}
