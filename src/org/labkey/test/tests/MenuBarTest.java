/*
 * Copyright (c) 2009-2011 LabKey Corporation
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

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/core";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected void doTestSteps()
    {

        log("Open new project");
        createProject(PROJECT_NAME, "Collaboration");
        clickAdminMenuItem("Manage Project", "Project Settings");
        checkRadioButton("folderDisplayMode", "IN_MENU");
        clickButtonContainingText("Save Properties");
        clickLinkWithText("Menu Bar");
        clickButtonContainingText("Turn On Custom Menus");
        addWebPart("AssayList2");
        addWebPart("Study List");
        addWebPart("Wiki Menu");

        clickLinkWithText(PROJECT_NAME);
        hideNavigationBar();

        goToModule("Wiki"); // Not required prior to 11.1 -- not sure how
        createNewWikiPage("HTML");
        setFormElement("name", WIKI_PAGE_TITLE);
        setFormElement("title", WIKI_PAGE_TITLE);
        setWikiBody(WIKI_PAGE_CONTENT);
        saveWikiPage();
        clickAdminMenuItem("Manage Project", "Project Settings");
        clickLinkWithText("Menu Bar");

        log("Test wiki customization");
        clickWebpartMenuItem("Wiki", "Customize");
        selectOptionByText("webPartContainer", "/" + PROJECT_NAME);
        waitForElement(Locator.tagWithText("option", WIKI_PAGE_TITLE + " (" + WIKI_PAGE_TITLE + ")"), 2000);
        selectOptionByText("name", WIKI_PAGE_TITLE + " (" + WIKI_PAGE_TITLE + ")");
        clickNavButton("Submit");
        
        clickLinkWithText(PROJECT_NAME);

        //Make sure that the menus are shown, but the content is not yet loaded.
        assertElementPresent(Locator.id("menuBarFolder"));
        assertElementPresent(Locator.menuBarItem("Assays"));
        assertElementPresent(Locator.menuBarItem("Studies"));
        assertElementPresent(Locator.menuBarItem(WIKI_PAGE_TITLE));

        log("Assert wiki, assay, and study portals not loaded");
        assertTextNotPresent(WIKI_PAGE_CONTENT);
        assertNavButtonNotPresent("Manage Assays");
        assertTextNotPresent("No Studies Found");

        mouseOver(Locator.menuBarItem("Wiki Menu"));
        waitForText(WIKI_PAGE_CONTENT, 3000);
        mouseOver(Locator.menuBarItem("Assays"));
        waitForElement(Locator.navButton("Manage Assays"), 3000);

        clickNavButton("Manage Assays");
        clickNavButton("New Assay Design");
        checkRadioButton("providerName", "General");
        clickNavButton("Next");
        waitForElement(Locator.id("AssayDesignerName"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.id("AssayDesignerName"), "Test Assay");
        clickNavButton("Save", 0);
        waitForText("Save successful.", WAIT_FOR_JAVASCRIPT);
        clickLinkWithText(PROJECT_NAME);

        assertTextNotPresent("Test Assay");
        mouseOver(Locator.menuBarItem("Assays"));
        waitForText("Test Assay", WAIT_FOR_JAVASCRIPT);

        createSubfolder(PROJECT_NAME, PROJECT_NAME, "StudyFolder", "Study", null);
        clickNavButton("Create Study");
        clickNavButton("Create Study");

        clickLinkWithText(PROJECT_NAME);

        mouseOver(Locator.menuBarItem("Studies"));
        waitForText("StudyFolder Study", WAIT_FOR_JAVASCRIPT);
    }

    protected void doCleanup()
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }


}
