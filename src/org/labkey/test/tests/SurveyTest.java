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

import org.junit.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.ext4cmp.Ext4FieldRefWD;

import java.io.File;

/**
 * User: cnathe
 * Date: 1/4/13
 */
public class SurveyTest extends BaseWebDriverTest
{
    private final String folderName = "subfolder";
    private final String pipelineLoc =  "/sampledata/survey";
    private final String projectSurveyDesign = "My Project Survey Design";
    private final String subfolderSurveyDesign = "My Subfolder Survey Design";
    private final String firstSurvey = "First Survey";
    private final String secondSurvey = "Second Survey";

    @Override
    protected String getProjectName()
    {
        return "SurveyTest" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setupProjectFolder();
        setupSubfolder();

        verifySurveyFromProject();
        verifyEditSurvey(folderName, firstSurvey);
        verifySubmitSurvey(folderName, firstSurvey);
        verifySurveyFromSubfolder();
        verifySurveyContainerPermissions();
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true; // for list archive import
    }

    private void setupProjectFolder()
    {
        _containerHelper.createProject(getProjectName(), null);

        log("Create survey disign at the project level");
        _listHelper.importListArchive(getProjectName(), new File(getLabKeyRoot() + pipelineLoc, "ListA.zip"));
        enableModule(getProjectName(), "Survey");
        addWebPart("Survey Designs");
        createSurveyDesign(getProjectName(), projectSurveyDesign, "listA");
    }

    private void setupSubfolder()
    {
        _containerHelper.createSubfolder(getProjectName(), folderName, null);

        log("Create survey disign at the subfolder level");
        _listHelper.importListArchive(folderName, new File(getLabKeyRoot() + pipelineLoc, "ListA.zip"));
        enableModule(folderName, "Survey");
        addWebPart("Survey Designs");
        createSurveyDesign(folderName, subfolderSurveyDesign, "listA");
    }

    private void createSurveyDesign(String folder, String designName, String listName)
    {
        log("Create new survey design");
        clickFolder(folder);
        waitForElement(Locator.id("dataregion_query"));
        clickButton("Add New Survey");
        waitForElement(Locator.name("label"));
        setFormElement(Locator.name("label"), designName);
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Schema']]"), "lists");
        // the schema selection enables the query combo, so wait for it to enable
        waitForElementToDisappear(Locator.xpath("//table[contains(@class,'item-disabled')]//label[text() = 'Query']"), WAIT_FOR_JAVASCRIPT);
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Query']]"), listName);
        clickButton("Generate Survey Questions", 0);
        sleep(1000); // give it a second to generate the metadata
        String metadataValue = getFormElement(Locator.name("metadata"));
        Assert.assertNotNull("No generate survey question metadata available", metadataValue);
        clickButton("Save Survey");
        waitForText(designName);
    }

    private void verifySurveyFromProject()
    {
        // add a survey webpart to the subfolder using the project survey design
        clickFolder(folderName);
        addSurveyWebpart(projectSurveyDesign);

        log("Create a new survey instance (i.e. take the survey)");
        clickButtonByIndex("Create New Survey", 0, WAIT_FOR_JAVASCRIPT);
        waitForText("Survey Label*");
        // verify that the save and submit buttons are disabled (and that they are visible, since this is the "auto" survey layout)
        Assert.assertTrue("Save button should be initially disabled", isElementPresent(Locator.xpath("//div[contains(@class,'item-disabled')]//span[text() = 'Save']")));
        Assert.assertTrue("Submit button should be initially disabled", isElementPresent(Locator.xpath("//div[contains(@class,'item-disabled')]//span[text() = 'Submit completed form']")));
        // set form field values
        setFormElement(Locator.name("_surveyLabel_"), firstSurvey);
        setFormElement(Locator.name("txtField"), "txtField");
        setFormElement(Locator.name("txtAreaField"), "txtAreaField");
        _ext4Helper.checkCheckbox("Bool Field");
        setFormElement(Locator.name("intField"), "999");
        setFormElement(Locator.name("dblField"), "999.1");
        setFormElement(Locator.name("dtField"), "2013-01-04");
        addSurveyFileAttachment("attField", pipelineLoc + "/TestAttachment.txt");
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Lk Field']]"), "Test1");
        log("Wait for the survey autosave (save attempts every minute)");
        waitForText("Responses automatically saved at", 65000);

        log("Verify that the survey was saved and the responses were saved to the subfolder list");
        clickFolder(folderName);
        waitForText(firstSurvey);
        clickAndWait(Locator.linkWithText("listA"));
        assertTextPresentInThisOrder("txtField", "txtAreaField", "true", "999", "999.1", "2013-01-04", "Test1");
        clickFolder(getProjectName());
        clickAndWait(Locator.linkWithText("listA"));
        waitForText("No data to show.");
    }

    private void addSurveyFileAttachment(String inputName, String fileName)
    {
        // TODO: implement file attachment for the survey form
        //setFormElement(Locator.name(inputName), new File(getLabKeyRoot() + fileName));
    }

    private void verifyEditSurvey(String folder, String surveyName)
    {
        log("Edit the survey in the specified folder");
        clickFolder(folder);
        clickEditForLabel(surveyName, true);
        _ext4Helper.waitForMaskToDisappear();
        setFormElement(Locator.name("txtAreaField"), "txtAreaField\nnew line");
        _ext4Helper.uncheckCheckbox("Bool Field");
        clickButton("Save", 0);
        _extHelper.waitForExtDialog("Success");
        _extHelper.waitForExtDialogToDisappear("Success");

        //verify that the reponses were saved with their changes
        clickFolder(folderName);
        clickAndWait(Locator.linkWithText("listA"));
        assertTextPresentInThisOrder("txtField", "txtAreaField", "new line", "false", "999", "999.1", "2013-01-04", "Test1");
    }

    private void verifySubmitSurvey(String folder, String surveyName)
    {
        // TODO: add a required field to the survey and verify the submit button disables when it doesn't have a value

        log("Submit the completed survey in the specified folder");
        clickFolder(folder);
        clickEditForLabel(surveyName, true);
        _ext4Helper.waitForMaskToDisappear();
        assertElementPresent(Locator.button("Save"));
        assertElementPresent(Locator.button("Submit completed form"));
        clickButton("Submit completed form");
        // go back to the submitted survey and verify the submit button is gone
        // TODO: add verification that site/project admins can still see Save button but other users can not for a submitted survey
        clickEditForLabel(surveyName, true);
        _ext4Helper.waitForMaskToDisappear();
        assertElementNotPresent(Locator.button("Submit completed form"));
    }

    private void verifySurveyFromSubfolder()
    {
        log("Customize the survey design metadata (card layout, multiple sections, show question counts, etc.)");
        clickFolder(folderName);
        clickEditForLabel(subfolderSurveyDesign, false);
        String json = getFileContents(pipelineLoc + "/CustomSurveyMetadata.json");
        // hack: since we are not able to update the CodeMirror input field via selenium, we reshow the
        // textarea and enter the value there, the SurveyDesignPanel will then use that value instead of the CodeMirror value
        executeScript("document.getElementsByName('metadata')[0].style.display = 'block';");
        setFormElement(Locator.name("metadata"), json);
        clickButton("Save Survey");

        // add the subfolder survey design webpart
        addSurveyWebpart(subfolderSurveyDesign);

        log("Verify the card layout (i.e. has section headers on left, not all questions visible, etc.)");
        clickButtonByIndex("Create New Survey", 1, WAIT_FOR_JAVASCRIPT);
        waitForText("Begin Survey");
        assertElementPresent(Locator.xpath("//li[text()='Begin Survey']"));
        assertElementPresent(Locator.xpath("//li[text()='Section 1']"));
        assertElementPresent(Locator.xpath("//li[text()='Section 2']"));
        assertElementPresent(Locator.xpath("//li[text()='End Survey']"));
        assertElementPresent(Locator.button("Previous"));
        assertElementPresent(Locator.button("Next"));
        assertElementNotPresent(Locator.button("Submit completed form"));

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
        setFormElement(Locator.name("dblField"), "999.1");
        // set the date to an invalid format
        setFormElement(Locator.name("dtField"), "01/04/2013");
        // check survey skip logic that attachment field appears with selectin of lkField = Test1
        assertElementPresent(Locator.xpath("//table[contains(@style,'display: none;')]//label[text()='Att Field']"));
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Lk Field']]"), "Test1");
        assertElementNotPresent(Locator.xpath("//table[contains(@style,'display: none;')]//label[text()='Att Field']"));
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Lk Field']]"), "Test2");
        assertElementPresent(Locator.xpath("//table[contains(@style,'display: none;')]//label[text()='Att Field']"));
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Lk Field']]"), "Test1");
        addSurveyFileAttachment("attField", pipelineLoc + "/TestAttachment.txt");
        clickButton("Next", 0);
        // check submit button text about invalid fields
        waitForText("Note: The following fields must be valid before you can submit the form");
        assertTextPresentInThisOrder("-Txt Field", "-Int Field", "-Dt Field");
        // go back and fix the validation errors
        clickButton("Previous", 0);
        waitForText("Description for section 2");
        setFormElement(Locator.name("intField"), "999");
        setFormElement(Locator.name("dtField"), "2013-01-04");
        clickButton("Previous", 0);
        waitForText("Description for section 1");
        setFormElement(Locator.name("txtField"), "txtField");
        clickButton("Next", 0);
        clickButton("Next", 0);
        // verify question counts on section header
        assertTextPresent("Section 1 (2)");
        assertTextPresent("Section 2 (5)");
        assertTextNotPresent("-Txt Field", "-Int Field", "-Dt Field");
        Assert.assertTrue("Submit button should not be disabled", !isElementPresent(Locator.xpath("//div[contains(@class,'item-disabled')]//span[text() = 'Submit completed form']")));
        clickButton("Submit completed form");
        waitForText("Surveys: " + subfolderSurveyDesign);
        assertTextPresent(secondSurvey);
        // verify survey reponses in the current folder
        clickAndWait(Locator.linkWithText("listA"));
        assertTextPresentInThisOrder("[{\"field1\":\"field1\",\"field2\":\"field2\"}]", "true");
    }

    private void addSurveyWebpart(String surveyDesignName)
    {
        log("Configure Surveys webpart");
        addWebPart("Surveys");
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Survey Design:']]"), surveyDesignName);
        clickButton("Submit");
        waitForText("Surveys: " + surveyDesignName);
    }

    private void verifySurveyContainerPermissions()
    {
        log("Add Survey webpart to project and verify subfolder survey is not present");
        clickFolder(getProjectName());
        addSurveyWebpart(projectSurveyDesign);
        assertTextPresent("No data to show.");
        assertTextNotPresent(firstSurvey);

        // TODO: more to be added here once we implement more of the role based permissions
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
        Assert.assertTrue(getFormElement(Locator.name("field1")).equals(val1));
        setFormElement(Locator.name("field2"), val2);
        clickButton("Update", 0);

        log("Remove record for SurveyGridQuestion");
        clickButton("Add Record", 0);
        _extHelper.waitForExtDialog("Add Record");
        setFormElement(Locator.name("field2"), "tobedeleted");
        clickButton("Update", 0);
        _ext4Helper.clickGridRowText("tobedeleted", 0);
        clickButton("Remove Selected", 0);
        assertTextNotPresent("tobedeleted");
    }

    private void clickEditForLabel(String label, boolean link)
    {
        Locator l = Locator.xpath("//a[text()='edit'][../../td[text()='" + label + "']]");
        if (link)
            l = Locator.xpath("//a[text()='edit'][../..//a[text()='" + label + "']]");

        waitForElement(l);
        clickAndWait(l);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/survey";
    }
}
