/*
 * Copyright (c) 2009-2017 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({DailyA.class})
public class MenuBarTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "MenuBarVerifyProject";
    private static final String WIKI_PAGE_TITLE = "A Wiki Menu" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    private static final String WIKI_PAGE_CONTENT = "This is a fancy wiki";

    private static final File STUDY_ZIP = TestFileUtils.getSampleData("studies/LabkeyDemoStudy.zip");
    private static final String DEM_STUDY_FOLDER = "DemStudyFolder";
    private static final String STUDY_FOLDER = "StudyFolder";

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("core");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Test
    public void testSteps()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        WikiHelper wikiHelper = new WikiHelper(this);

        log("Open new project");
        _containerHelper.createProject(PROJECT_NAME, "Collaboration");
        goToProjectSettings();
        clickAndWait(Locator.linkWithText("Menu Bar"));

        log("Add menu bar webparts");
        portalHelper.addWebPart("AssayList2");
        portalHelper.addWebPart("Study List");
        portalHelper.addWebPart("Wiki Menu");

        clickProject(PROJECT_NAME);

        goToModule("Wiki");
        wikiHelper.createNewWikiPage("HTML");
        setFormElement(Locator.name("name"), WIKI_PAGE_TITLE);
        setFormElement(Locator.name("title"), WIKI_PAGE_TITLE);
        wikiHelper.setWikiBody(WIKI_PAGE_CONTENT);
        wikiHelper.saveWikiPage();
        goToProjectSettings();
        clickAndWait(Locator.linkWithText("Menu Bar"));

        log("Test wiki customization");
        portalHelper.clickWebpartMenuItem("Wiki", "Customize");
        selectOptionByText(Locator.name("webPartContainer"), "/" + PROJECT_NAME);
        waitForElement(Locator.tagWithText("option", WIKI_PAGE_TITLE + " (" + WIKI_PAGE_TITLE + ")"), 2000);
        selectOptionByText(Locator.name("name"), WIKI_PAGE_TITLE + " (" + WIKI_PAGE_TITLE + ")");
        clickButton("Submit");
        
        clickProject(PROJECT_NAME);

        //Make sure that the menus are shown, but the content is not yet loaded.
        assertElementPresent(Locator.tagWithClass("div", "navbar-header"));
        assertElementPresent(Locator.menuBarItem("Assays"));
        assertElementPresent(Locator.menuBarItem("Studies"));
        assertElementPresent(Locator.menuBarItem(WIKI_PAGE_TITLE));

        log("Assert wiki, assay, and study portals not loaded");
        assertTextNotPresent(WIKI_PAGE_CONTENT);
        assertElementNotPresent(Locator.lkButton("Manage Assays"));
        assertTextNotPresent("No Studies Found");

        openMenu(WIKI_PAGE_TITLE);
        waitForElement(Locator.xpath("//div").withClass("labkey-wiki").withText(WIKI_PAGE_CONTENT));
        openMenu("Assays");
        waitForElement(Locator.lkButton("Manage Assays"), 3000);

        _assayHelper.uploadXarFileAsAssayDesign(TestFileUtils.getSampleData("menubar/Test Assay.xar"), 1);
        clickProject(PROJECT_NAME);

        assertTextNotPresent("Test Assay");
        openMenu("Assays");
        waitForElement(Locator.linkWithText("Test Assay"));

        _containerHelper.createSubfolder(PROJECT_NAME, PROJECT_NAME, "StudyFolder", "Study", null);
        createDefaultStudy();
        clickProject(getProjectName());

        Locator.XPathLocator studyLink = Locator.linkWithText("StudyFolder Study");
        assertElementNotPresent(studyLink);
        WebElement menu = openMenu("Studies");
        waitForElement(studyLink, WAIT_FOR_JAVASCRIPT);
        menu.click(); // Close manually
        mouseOut(); // Menu intermittently reopens during admin menu interaction
        shortWait().until(ExpectedConditions.invisibilityOfElementLocated(studyLink));

        // Custom Menu
        log("Test Custom Menu WebPart");
        goToProjectSettings();
        clickAndWait(Locator.linkWithText("Menu Bar"));
        portalHelper.addWebPart("Custom Menu");

        // Schema/Query/etc
        _extHelper.setExtFormElementByLabel("Title", "Wiki Render Types");
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        waitForElementToDisappear(Locator.css(".ext-el-mask"));
        _extHelper.selectComboBoxItem("Schema", "wiki");
        waitForElementToDisappear(Locator.css(".ext-el-mask"));
        _extHelper.selectComboBoxItem("Query", "RendererType");
        waitForElementToDisappear(Locator.css(".ext-el-mask"));
        _extHelper.selectComboBoxItem("Title Column", "Value");
        _extHelper.clickExtButton("Submit");

        clickProject(getProjectName());
        openMenu("Wiki Render Types");
        Locator.XPathLocator menuLoc = Locator.tagWithClass("ul", "lk-custom-dropdown-menu");
        waitForElement(menuLoc.append(Locator.tagWithText("td", "HTML")));
        for (String tdTxt : Arrays.asList("HTML", "RADEOX", "MARKDOWN", "TEXT_WITH_LINKS"))
            assertElementPresent(menuLoc.append(Locator.tagWithText("td", tdTxt)));

        // Another custom menu with links to Participant Report
        _containerHelper.createSubfolder(PROJECT_NAME, PROJECT_NAME, DEM_STUDY_FOLDER, "Study", null);
        importStudyFromZip(STUDY_ZIP);

        goToProjectSettings();
        clickAndWait(Locator.linkWithText("Menu Bar"));
        portalHelper.addWebPart("Custom Menu");
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        _extHelper.setExtFormElementByLabel("Title", "Participant Reports");
        _extHelper.selectComboBoxItem("Folder", "\u00a0\u00a0" + DEM_STUDY_FOLDER);
        waitForElementToDisappear(Locator.css(".ext-el-mask"));
        _extHelper.selectComboBoxItem("Schema", "study");
        waitForElementToDisappear(Locator.css(".ext-el-mask"));
        _extHelper.selectComboBoxItem("Query", "Participant");
        waitForElementToDisappear(Locator.css(".ext-el-mask"));
        _extHelper.selectComboBoxItem("Title Column", "ParticipantId");
        _extHelper.setExtFormElementByLabel("URL", "/study-samples/typeParticipantReport.view?participantId=${participantId}");
        _extHelper.clickExtButton("Submit");

        // Should take us to participant report page
        clickProject(getProjectName());
        openMenu("Participant Reports");
        waitAndClickAndWait(Locator.linkWithText("249320107"));
        assertTextPresent("Specimen Report: Participant 249320107");

        // Another custom Menu with links to folders
        goToProjectSettings();
        clickAndWait(Locator.linkWithText("Menu Bar"));
        portalHelper.addWebPart("Custom Menu");
        _extHelper.setExtFormElementByLabel("Title", "Folders");

        Locator radioFolder = Locator.radioButtonById("folder-radio");
        click(radioFolder);
        _extHelper.selectComboBoxItem("Root Folder", "/");
        _extHelper.selectComboBoxItem("Folder Types", "Study");
        clickButton("Submit");

        clickFolder(DEM_STUDY_FOLDER);
        assertTextPresent("Demo Study", "Study Overview");
        openMenu("Folders");
        waitForElement(Locator.linkWithText(DEM_STUDY_FOLDER));
        assertElementPresent(Locator.linkWithText(STUDY_FOLDER));
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
