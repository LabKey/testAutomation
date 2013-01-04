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
import org.labkey.test.TestTimeoutException;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: marki
 * Date: May 18, 2009
 * Time: 12:44:03 PM
 */
public class MenuBarTest extends BaseSeleniumWebTest
{

    private static final String PROJECT_NAME = "MenuBarVerifyProject";
    private static final String WIKI_PAGE_TITLE = "Wiki Menu";
    private static final String WIKI_PAGE_CONTENT = "This is a fancy wiki";

    private static final String STUDY_ZIP = "/sampledata/study/LabkeyDemoStudy.zip";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/core";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    public boolean isFileUploadTest()
    {
        return true;
    }
    protected void doTestSteps()
    {

        log("Open new project");
        _containerHelper.createProject(PROJECT_NAME, "Collaboration");
        goToProjectSettings();
        checkRadioButton("folderDisplayMode", "IN_MENU");
        clickButtonContainingText("Save");
        clickAndWait(Locator.linkWithText("Menu Bar"));
        clickButtonContainingText("Turn On Custom Menus");
        addWebPart("AssayList2");
        addWebPart("Study List");
        addWebPart("Wiki Menu");

        clickAndWait(Locator.linkWithText(PROJECT_NAME));
        hideNavigationBar();

        goToModule("Wiki"); // Not required prior to 11.1 -- not sure how
        createNewWikiPage("HTML");
        setFormElement("name", WIKI_PAGE_TITLE);
        setFormElement("title", WIKI_PAGE_TITLE);
        setWikiBody(WIKI_PAGE_CONTENT);
        saveWikiPage();
        goToProjectSettings();
        clickAndWait(Locator.linkWithText("Menu Bar"));

        log("Test wiki customization");
        clickWebpartMenuItem("Wiki", "Customize");
        selectOptionByText("webPartContainer", "/" + PROJECT_NAME);
        waitForElement(Locator.tagWithText("option", WIKI_PAGE_TITLE + " (" + WIKI_PAGE_TITLE + ")"), 2000);
        selectOptionByText("name", WIKI_PAGE_TITLE + " (" + WIKI_PAGE_TITLE + ")");
        clickButton("Submit");
        
        clickAndWait(Locator.linkWithText(PROJECT_NAME));

        //Make sure that the menus are shown, but the content is not yet loaded.
        assertElementPresent(Locator.id("menuBarFolder"));
        assertElementPresent(Locator.menuBarItem("Assays"));
        assertElementPresent(Locator.menuBarItem("Studies"));
        assertElementPresent(Locator.menuBarItem(WIKI_PAGE_TITLE));

        log("Assert wiki, assay, and study portals not loaded");
        assertTextNotPresent(WIKI_PAGE_CONTENT);
        assertNavButtonNotPresent("Manage Assays");
        assertTextNotPresent("No Studies Found");

//        mouseOver(Locator.menuBarItem("Wiki Menu"));
//        waitForText(WIKI_PAGE_CONTENT, 3000);
        //TODO:  clean up
        Locator l = Locator.id("Wiki Menu6$Header");
        //selenium.focus(l);
        mouseOver(l);
        sleep(3000);
        assertTextPresent(WIKI_PAGE_CONTENT);
        mouseOver(Locator.menuBarItem("Assays"));
        waitForElement(Locator.navButton("Manage Assays"), 3000);


        _assayHelper.uploadXarFileAsAssayDesign(getSampledataPath() + "/menubar/Test Assay.xar", 1, "Test Assay.xar");
//        checkRadioButton("providerName", "General");
//        clickButton("Next");
//        waitForElement(Locator.id("AssayDesignerName"), WAIT_FOR_JAVASCRIPT);
//        setFormElement(Locator.id("AssayDesignerName"), "Test Assay");
//        clickButton("Save", 0);
//        waitForText("Save successful.", WAIT_FOR_JAVASCRIPT);
        clickAndWait(Locator.linkWithText(PROJECT_NAME));

        assertTextNotPresent("Test Assay");
        mouseOver(Locator.menuBarItem("Assays"));
        waitForText("Test Assay", WAIT_FOR_JAVASCRIPT);

        createSubfolder(PROJECT_NAME, PROJECT_NAME, "StudyFolder", "Study", null);
        createDefaultStudy();
        beginAt("project/" + getProjectName() + "/begin.view?");

        mouseOver(Locator.menuBarItem("Studies"));
        waitForText("StudyFolder Study", WAIT_FOR_JAVASCRIPT);


        // Custom Menu
        log("Test Custom Menu WebPart");
        goToProjectSettings();
        clickAndWait(Locator.linkWithText("Menu Bar"));
        addWebPart("Custom Menu");

        // Schema/Query/etc
        _extHelper.setExtFormElementByLabel("Title", "Wiki Render Types");
        _extHelper.clickExtDropDownMenu("userQuery_schema", "wiki");
        _extHelper.clickExtDropDownMenu("userQuery_query", "renderertype");
        _extHelper.clickExtDropDownMenu("userQuery_Column", "Value");
        _extHelper.clickExtButton("Submit");
        assertTextPresent("HTML", "RADEOX", "TEXT_WITH_LINKS");

        // Another custom menu with links to Participant Report
        createSubfolder(PROJECT_NAME, PROJECT_NAME, "DemStudyFolder", "Study", null);
        importStudyFromZip(new File(getLabKeyRoot() + STUDY_ZIP).getPath());

        goToProjectSettings();
        clickAndWait(Locator.linkWithText("Menu Bar"));
        addWebPart("Custom Menu");
        _extHelper.setExtFormElementByLabel("Title", "Participant Reports");
        _extHelper.clickExtDropDownMenu("userQuery_folders", "DemStudyFolder");
        _extHelper.clickExtDropDownMenu("userQuery_schema", "study");
        _extHelper.clickExtDropDownMenu("userQuery_query", "Participant");
        _extHelper.clickExtDropDownMenu("userQuery_Column", "ParticipantId");
        _extHelper.setExtFormElementByLabel("URL", "/study-samples/typeParticipantReport.view?participantId=${participantId}");
        _extHelper.clickExtButton("Submit");

        // Should take us to participant report page
        clickAndWait(Locator.linkWithText("249320107"));
        assertTextPresent("Specimen Report: Participant 249320107");

        // Another custom Menu with links to folders
        goToProjectSettings();
        clickAndWait(Locator.linkWithText("Menu Bar"));
        addWebPart("Custom Menu");
        _extHelper.setExtFormElementByLabel("Title", "Folders");

        Locator radioFolder = Locator.radioButtonById("folder-radio");
        click(radioFolder);
        _extHelper.clickExtDropDownMenu("userQuery_folderTypes", "Study");
        _extHelper.clickExtButton("Submit");

        clickAndWait(Locator.linkWithText("DemStudyFolder"));
        assertTextPresent("Demo Study", "Study Overview");
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }


}
