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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.labkey.PortalTab;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.PortalHelper;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({DailyA.class})
public class TabTest extends SimpleModuleTest
{
    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected void doVerifySteps() throws Exception
    {
        doTabManagementTests();
        doTestTabbedFolder();
        doTestContainerTabConversion();
    }

    @LogMethod
    private void doTabManagementTests()
    {
         /*
         *  test add, remove, move, rename, hide, unhide.
         */
        PortalHelper portalHelper = new PortalHelper(this);

        navigateToFolder(getProjectName(), FOLDER_NAME);

        // Move tabs
        portalHelper.enableTabEditMode();
        portalHelper.moveTab("Tab 1", PortalHelper.Direction.LEFT); // Nothing should happen.
        portalHelper.moveTab("Tab 1", PortalHelper.Direction.RIGHT);
        List<PortalTab> tabs = PortalTab.findTabs(getDriver());
        assertEquals("Mice tab not in the first position", "Tab 2", tabs.get(0).getText());
        assertEquals("Overview tab not in the second position", "Tab 1", tabs.get(1).getText());
        portalHelper.moveTab("Assay Container", PortalHelper.Direction.RIGHT); // Nothing should happen.
        tabs = PortalTab.findTabs(getDriver());
        assertEquals("'Move Right' moved rightmost tab", "Assay Container", tabs.get(3).getText());

        // Remove tab
        portalHelper.hideTab("Tab 2");

        // Add tab
        portalHelper.addTab("TEST TAB 1");
        clickAndWait(Locator.linkWithText("TEST TAB 1"));
        portalHelper.addWebPart("Wiki");

        // Rename tabs
        portalHelper.renameTab("TEST TAB 1", "Tab 2", "You cannot change a tab's name to another tab's original name even if the original name is not visible.");
        portalHelper.renameTab("Tab 1", "TEST TAB 1", "A tab with the same name already exists in this folder.");
        portalHelper.renameTab("Tab 1", "test tab 1", "A tab with the same name already exists in this folder.");
        portalHelper.renameTab("TEST TAB 1", "RENAMED TAB 1");
        clickAndWait(Locator.linkWithText("RENAMED TAB 1"));
        assertEquals("Wiki not present after tab rename", "Wiki", getText(Locator.css(".labkey-wp-title-text")));

        portalHelper.showTab("Tab 2");

        // Issue 18730 - Add tab was adding tabs to the container-tab's container.
        clickTab("Study Container");
        portalHelper.addTab("Test Tab 2");

        // Issue 18731 - Couldn't rename container tabs if on container tab page.
        portalHelper.renameTab("Study Container", "Study Container Rename");
        // It's needed later in the test, so rename it back.
        portalHelper.renameTab("Study Container Rename", "Study Container");

        // Issue 18734 - Unable to show some hidden tabs in folder with container tab.
        portalHelper.hideTab("Tab 1");
        portalHelper.showTab("Tab 1");

        // TODO: Test import/export of renamed tabs if applicable
        // See Issue 16929: Folder tab order & names aren't retained through folder export/import

        //Delete tab while on different  Tab
        String tab2Delete = "RENAMED TAB 1";
        portalHelper.activateTab(tab2Delete);
        portalHelper.deleteTab("Test Tab 2");
        List<BodyWebPart> bodyparts = portalHelper.getBodyWebParts();
        assertTrue("Webparts failed to load after tab delete while on page", bodyparts != null && bodyparts.size() > 0);
        assertEquals("Wrong tab selected after tab deletion", tab2Delete, getText(PortalHelper.Locators.activeTab().containing(tab2Delete)).replace(Locator.NBSP, " ").trim());

        //Delete tab while on the Tab
        portalHelper.deleteTab(tab2Delete);
        bodyparts = portalHelper.getBodyWebParts();
        assertTrue("Webparts failed to load after tab delete", bodyparts != null && bodyparts.size() > 0);
    }

    @LogMethod
    private void doTestTabbedFolder()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        clickFolder(FOLDER_NAME);

        //it should start on tab 2
        verifyTabSelected("Tab 2");
        log("verifying webparts present in correct order");
        assertTextPresentInThisOrder("A customized web part", "Experiment Runs", "Assay List");

        //verify Tab 1
        clickTab("Tab 1");
        assertTextPresentInThisOrder("A customized web part", "Data Pipeline", "Experiment Runs", "Run Groups", "Sample Sets", "Assay List");
        portalHelper.addWebPart("Messages");

        clickTab("Tab 2");

        //verify added webpart is persisted
        clickTab("Tab 1");
        assertTextPresentInThisOrder("A customized web part", "Data Pipeline", "Experiment Runs", "Run Groups", "Sample Sets", "Assay List", "Messages");

        //there is a selector for the assay controller and tab2
        clickAndWait(Locator.linkWithText("New Assay Design"));
        verifyTabSelected("Tab 2");

        //this is a controller selector
        goToSchemaBrowser();
        verifyTabSelected("Tab 1");

        //this is a view selector
        goToModule("Pipeline");
        verifyTabSelected("Tab 2");

        //this is a regex selector
        clickFolder(FOLDER_NAME);
        portalHelper.addWebPart("Sample Sets");
        clickAndWait(Locator.linkWithText("Import Sample Set"));
        verifyTabSelected("Tab 1");

        // Test Container tabs
        portalHelper.activateTab("Assay Container");
        assertTextPresent("Assay List");
        PortalTab studyContainerTab = portalHelper.activateTab(STUDY_FOLDER_TAB_LABEL);
        assertTextPresent("Study Overview");
        clickAndWait(Locator.linkWithText("Create Study"));
        clickAndWait(Locator.linkWithText("Create Study"));
        assertTextPresent("Manage Study", "Study Container", "Overview", "Specimen Data");
        studyContainerTab.goToTabContainer("Specimen Data");
        assertTextPresent("Vial Search", "Specimens");

        // Test container tab enhancements: change tab folder's type, revert type, delete tab folder
        // Change study's type to collaboration
        log("Container tab enhancements: change tab folder type, revert");
        goToTabFolderManagement(STUDY_FOLDER_TAB_LABEL);
        waitForText(STUDY_FOLDER_TAB_NAME);
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton(Locator.radioButtonByNameAndValue("folderType", "Collaboration"));
        clickButton("Update Folder", 0);
        _extHelper.waitForExtDialog("Change Folder Type");
        click(Ext4Helper.Locators.ext4Button("Yes"));
        waitForText("The Wiki web part displays a single wiki page.");
        // TODO: assert that study tabs are gone
        // change type back to study
        goToTabFolderManagement(STUDY_FOLDER_TAB_LABEL);
        waitForText(STUDY_FOLDER_TAB_NAME);
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton(Locator.radioButtonByNameAndValue("folderType", "Study"));
        clickButton("Update Folder", 0);
        _extHelper.waitForExtDialog("Change Folder Type");
        click(Ext4Helper.Locators.ext4Button("Yes"));
        waitForText("Study tracks data in");
        // change to collaboration again
        goToTabFolderManagement(STUDY_FOLDER_TAB_LABEL);
        waitForText(STUDY_FOLDER_TAB_NAME);
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton(Locator.radioButtonByNameAndValue("folderType", "Collaboration"));
        clickButton("Update Folder", 0);
        _extHelper.waitForExtDialog("Change Folder Type");
        click(Ext4Helper.Locators.ext4Button("Yes"));
        waitForText("The Wiki web part displays a single wiki page.");
        // revert type
        goToTabFolderManagement(STUDY_FOLDER_TAB_LABEL);
        waitForText(STUDY_FOLDER_TAB_NAME);
        clickButton("Revert", 0);
        _extHelper.waitForExtDialog("Revert Folder(s)");
        click(Ext4Helper.Locators.ext4Button("Yes"));
        _extHelper.waitForExtDialog("Revert Folder");
        click(Ext4Helper.Locators.ext4Button("OK"));
        clickTab("Study Container");
        waitForText("Study tracks data in");

        // Revert via parent container
        goToTabFolderManagement(STUDY_FOLDER_TAB_LABEL);
        waitForText(STUDY_FOLDER_TAB_NAME);
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton(Locator.radioButtonByNameAndValue("folderType", "Collaboration"));
        clickButton("Update Folder", 0);
        _extHelper.waitForExtDialog("Change Folder Type");
        click(Ext4Helper.Locators.ext4Button("Yes"));
        waitForText("The Wiki web part displays a single wiki page.");
        clickTab("Tab 1");
        goToFolderManagement();
        waitForText(STUDY_FOLDER_TAB_NAME);
        clickButton("Revert", 0);
        _extHelper.waitForExtDialog("Revert Folder(s)");
        click(Ext4Helper.Locators.ext4Button("Yes"));
        _extHelper.waitForExtDialog("Revert Folders");
        click(Ext4Helper.Locators.ext4Button("OK"));
        clickTab("Study Container");
        waitForText("Study tracks data in");

        // Delete tab folder
        log("Container tab enhancements: delete tab folder type, recreate");
        goToTabFolderManagement(STUDY_FOLDER_TAB_LABEL);
        waitForText(STUDY_FOLDER_TAB_NAME);
        clickButton("Delete", 2 * WAIT_FOR_PAGE);
        assertTextPresent("You are about to delete the following folder:");
        clickButton("Delete", 2 * WAIT_FOR_PAGE);
        assertTextNotPresent(STUDY_FOLDER_TAB_NAME);

        // Resurrect tab folder
        clickTab("Tab 1");
        goToFolderManagement();
        waitForText(ASSAY_FOLDER_TAB_NAME);
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton(Locator.radioButtonByNameAndValue("folderType", "Collaboration"));
        clickButton("Update Folder");
        goToFolderManagement();
        waitForText(ASSAY_FOLDER_TAB_NAME);
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkRadioButton(Locator.radioButtonByNameAndValue("folderType", TABBED_FOLDER_TYPE));
        clickButton("Update Folder", 0);
        _extHelper.waitForExtDialog("Change Folder Type");
        assertTextPresent("Study Container");
        _ext4Helper.checkCheckbox("Study Container");     // recreate folder
        click(Ext4Helper.Locators.ext4Button("OK"));
        waitForText("A customized web part");
        clickTab("Study Container");
        clickAndWait(Locator.linkWithText("Create Study"));     // Create study
        clickAndWait(Locator.linkWithText("Create Study"));

        // Create the list again so we can pass query validation.
        log("Create list in subfolder to prevent query validation failure");
        _listHelper.createListFromTab(STUDY_FOLDER_TAB_LABEL, LIST_NAME,
                ListHelper.ListColumnType.AutoInteger, "Key",
                new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Name"),
                new ListHelper.ListColumn("Age", "Age", ListHelper.ListColumnType.Integer, "Age"),
                new ListHelper.ListColumn("Crazy", "Crazy", ListHelper.ListColumnType.Boolean, "Crazy?"));
    }

    @LogMethod
    private void doTestContainerTabConversion()
    {
        PortalHelper portalHelper = new PortalHelper(this);

        // Set up a Collaboration folder with study and assay subfolders
        final String COLLAB_FOLDER = "collab1";
        final String COLLABFOLDER_PATH = getProjectName() + "/" + COLLAB_FOLDER;
        final String EXTRA_ASSAY_WEBPART = "Run Groups";
        goToProjectHome();
        _containerHelper.createSubfolder(getProjectName(), COLLAB_FOLDER, "Collaboration");
        _containerHelper.createSubfolder(COLLABFOLDER_PATH, STUDY_FOLDER_TAB_NAME, "Study");
        _containerHelper.createSubfolder(COLLABFOLDER_PATH, ASSAY_FOLDER_TAB_NAME, "Assay");
        clickFolder(COLLAB_FOLDER);
        clickFolder(STUDY_FOLDER_TAB_NAME);
        assertTextPresent("Study Overview");
        clickAndWait(Locator.linkWithText("Create Study"));
        clickAndWait(Locator.linkWithText("Create Study"));
        clickFolder(ASSAY_FOLDER_TAB_NAME);
        portalHelper.addWebPart(EXTRA_ASSAY_WEBPART);

        // Change folder type to XML Tabbed
        clickFolder(COLLAB_FOLDER);
        assertElementNotPresent(Locator.linkWithText(STUDY_FOLDER_TAB_LABEL));
        assertElementNotPresent(Locator.linkWithText(ASSAY_FOLDER_TAB_LABEL));
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkCheckbox(Locator.radioButtonByNameAndValue("folderType", TABBED_FOLDER_TYPE));
        clickButton("Update Folder");

        // Verify that subfolders got moved into tabs
        PortalTab studyContainerTab = PortalTab.find(STUDY_FOLDER_TAB_LABEL, getDriver());
        PortalTab assayContainerTab = PortalTab.find(ASSAY_FOLDER_TAB_LABEL, getDriver());
        studyContainerTab = studyContainerTab.activate();
        assertTrue(studyContainerTab.isActive());
        assertFalse(assayContainerTab.isActive());
        projectMenu().open();
        assertFalse("container tab subfolders should not be linked in project menu",
                projectMenu().projectLinkExists(STUDY_FOLDER_TAB_LABEL));
        assertFalse("container tab subfolders should not be linked in project menu",
                projectMenu().projectLinkExists(ASSAY_FOLDER_TAB_LABEL));

        clickAndWait(Locator.linkWithText(STUDY_FOLDER_TAB_LABEL));
        assertTextPresent("Study Overview");
        studyContainerTab = PortalTab.finder(getDriver()).withTabText(STUDY_FOLDER_TAB_LABEL).findWhenNeeded();
        studyContainerTab.goToTabContainer("Specimen Data");
        assertTextPresent("Vial Search", "Import Specimens");
        clickAndWait(Locator.linkWithText(ASSAY_FOLDER_TAB_LABEL));
        assertTextPresent("Assay List", EXTRA_ASSAY_WEBPART);

        // Change back to Collab
        clickFolder(COLLAB_FOLDER);
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkCheckbox(Locator.radioButtonByNameAndValue("folderType", "Collaboration"));
        clickButton("Update Folder");

        // Study and Assay should be hidden now
        assertTextNotPresent(STUDY_FOLDER_TAB_LABEL, ASSAY_FOLDER_TAB_LABEL, STUDY_FOLDER_TAB_NAME, ASSAY_FOLDER_TAB_NAME);

        // Now change back to TABBED
        clickFolder(COLLAB_FOLDER);
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkCheckbox(Locator.radioButtonByNameAndValue("folderType", TABBED_FOLDER_TYPE));
        clickButton("Update Folder");

        // Verify that folder tabs are back
        assertTextPresent(STUDY_FOLDER_TAB_LABEL, ASSAY_FOLDER_TAB_LABEL);
        clickAndWait(Locator.linkWithText(STUDY_FOLDER_TAB_LABEL));
        assertTextPresent("Study Overview");
        PortalTab.find(STUDY_FOLDER_TAB_LABEL, getDriver()).goToTabContainer("Specimen Data");
        assertTextPresent("Vial Search", "Import Specimens");
        PortalTab.find(ASSAY_FOLDER_TAB_LABEL, getDriver()).activate();
        assertTextPresent("Assay List", EXTRA_ASSAY_WEBPART);

        _containerHelper.deleteFolder(getProjectName(), COLLAB_FOLDER);
    }

    @LogMethod(quiet = true)
    public void goToTabFolderManagement(@LoggedParam String tabText)
    {
        PortalTab tab = PortalTab.find(tabText, getDriver());
        tab.getMenu().clickSubMenu(true,"Folder", "Management");
    }
}