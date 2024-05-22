/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Hosting;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.pages.FolderManagementFolderTree;
import org.labkey.test.pages.admin.FolderManagementPage;
import org.labkey.test.pages.admin.ReorderFoldersPage;
import org.labkey.test.pages.list.BeginPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WorkbookHelper;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({Daily.class, Hosting.class})
@BaseWebDriverTest.ClassTimeout(minutes = 25 )
public class FolderTest extends BaseWebDriverTest
{
    private static String secondProject = "FolderTestProject2";

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Override
    public String getProjectName()
    {
        return "FolderTestProject";
    }

    @Override
    protected void checkQueries() { /* Too many folder to check queries. */ }

    @Override
    public void checkLinks() { /* too many folders */ }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _containerHelper.deleteProject(secondProject, afterTest);
    }

    @BeforeClass
    public static void testSetup()
    {
        FolderTest init = (FolderTest)getCurrentTest();
        init._containerHelper.createProject(init.getProjectName(), null);
        init._containerHelper.createProject(secondProject, null);
        init.goToProjectHome();
        init.importFolderFromZip(TestFileUtils.getSampleData("FolderTest/FolderTestProject.folder.zip"));
        init.moveTestProjectToTop();
    }

    @LogMethod
    private void moveTestProjectToTop()     // todo: use FolderManagementPage, ReorderFoldersPage
    {
        clickProject(getProjectName());
        FolderManagementPage managePage = goToFolderManagement();
        waitForElement(Ext4Helper.Locators.folderManagementTreeSelectedNode(getProjectName()));

        log("Ensure folders will be visible");

        ReorderFoldersPage reorderPage = managePage.clickChangeDisplayOrder();
        checkCheckbox(Locator.radioButtonByNameAndValue("resetToAlphabetical", "false"));
        selectOptionByText(Locator.name("items"), getProjectName());
        for(int i = 0; i < 100 && getElementIndex(Locator.xpath("//option[@value='"+ getProjectName() +"']")) > 0; i++)
            clickButton("Move Up", 0);
        managePage = reorderPage.clickSave();

        reorderPage = managePage.clickChangeDisplayOrder();
        checkCheckbox(Locator.radioButtonByNameAndValue("resetToAlphabetical", "false"));
        selectOptionByText(Locator.name("items"), secondProject);
        for(int i = 0; i < 100 && getElementIndex(Locator.xpath("//option[@value='"+ secondProject +"']")) > 0; i++)
            clickButton("Move Up", 0);
        reorderPage.clickSave();
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
        goToFolderManagement();
    }

    @Test
    public void testMoveFolder()
    {
        final FolderManagementFolderTree folderManagement = new FolderManagementFolderTree(this, getProjectName());

        log("Reorder folders test");
        folderManagement.expandFolderNode("A");
        folderManagement.expandFolderNode("AB");

        folderManagement.reorderFolder("ABB", "ABA", FolderManagementFolderTree.Reorder.preceding, true);
        refresh();
        folderManagement.expandNavFolders("A", "AB");
        assertTextBefore("ABB", "ABA");
        log("Illegal folder move test: Project demotion");
        folderManagement.moveFolder(getProjectName(), "home", "Cannot move one Project into another", "", false, false);

        log("Move folder test");
        folderManagement.expandFolderNode("ABB");
        assertTextPresent("ABBA");
        folderManagement.moveFolder("ABBA", "ABA", "Move Folder", "Move Folder", true, false);
        refresh();
        folderManagement.expandNavFolders("A", "AB", "ABA");
        assertTextBefore("ABB", "ABBA");

        //Issue 12762: Provide way to cancel a folder move
        log("Cancel folder move test");
        folderManagement.expandFolderNode("ABA");
        folderManagement.moveFolder("ABBA", "E","Move Folder","Move Folder", true, false, false);
        refresh();
        folderManagement.expandNavFolders("A", "AB", "ABA", "E");
        assertTextBefore("ABB", "ABBA");

        log("Illegal multiple folder move: non-siblings");
        folderManagement.expandFolderNode("A");
        folderManagement.expandFolderNode("B");
        folderManagement.selectFolderManagementTreeItem(getProjectName() + "/A/AA", false);
        folderManagement.selectFolderManagementTreeItem(getProjectName() + "/B/BA", true);
        folderManagement.moveFolder("AA", "C", "Move Folder", "Move Folder", false, true);
        Window window = Window.Window(getDriver()).waitFor();
        assertEquals("Wrong warning when attempting to move non-siblings", "When selecting multiple folders please only select folders that are directly under the same parent folder.", window.getBody());
        window.clickButton("OK", true);
        _ext4Helper.waitForMaskToDisappear();

        log("Move multiple folders");
        folderManagement.expandFolderNode("A");
        folderManagement.expandFolderNode("B");
        folderManagement.selectFolderManagementTreeItem(getProjectName() + "/B/BA", false);
        folderManagement.selectFolderManagementTreeItem(getProjectName() + "/B/BB", true);
        folderManagement.selectFolderManagementTreeItem(getProjectName() + "/B/BC", true);
        folderManagement.moveFolder("BA", "A", "Move Folder", "Move Folder",true, true);

        log("verify folder contents");
        String[] folders = {"AA", "AB", "AC", "BA", "BB", "BC", "ABB", "ABA", "ABBA"};
        for(String currentFolder : folders)
        {
            verifyFolderContents(currentFolder, getProjectName());
        }

        log("verify folder move between projects");
        goToFolderManagement();
        folderManagement.moveFolder("AB", secondProject, "Move Folder","Change Project", true, false);
        goToProjectHome(secondProject);
        String[] proj2Folders = {"AB","ABB", "ABA", "ABBA"};
        for(String currentFolder : proj2Folders)
        {
            verifyFolderContents(currentFolder, secondProject);
        }
    }

    @LogMethod
    private void verifyFolderContents(@LoggedParam String folder, @LoggedParam String project)
    {
        goToProjectHome(project);
        clickFolder(folder);
        waitAndClick(Locator.linkWithText("12 datasets"));
        saveLocation();
        waitAndClick(Locator.linkWithText("LuminexAssay"));
        waitForText("Contains up to one row of LuminexAssay data for each Participant/Date/RowId combination.");
        assertElementPresent(Locator.linkWithText("249318596"));

        recallLocation();
        waitAndClick(Locator.linkWithText("Participation and Genetic Consent"));
        waitForText("Contains up to one row of Participation and Genetic Consent data for each Participant combination.");
        assertElementPresent(Locator.linkWithText("249325717"));

        recallLocation();
        waitAndClick(Locator.linkWithText("Demographics"));
        waitForText("Contains up to one row of Demographics data for each Participant combination.", "Group 1: Accute HIV-1", "Group 2: HIV-1 Negative");

        recallLocation();
        click(Locator.linkWithText("Status Assessment"));
        waitForText("Contains up to one row of Status Assessment data for each Participant/Date combination.");
        assertElementPresent(Locator.linkWithText("249320107"));
        assertElementPresent(Locator.linkWithText("249320127"));
        assertElementPresent(Locator.linkWithText("249320489"));

        recallLocation();
        waitAndClick(Locator.linkWithText("HIV Test Results"));
        waitForText("Contains up to one row of HIV Test Results data for each Participant/Date combination.", "Positive", "Negative");

        final BeginPage beginPage = goToManageLists();
        beginPage.getGrid().setContainerFilter(DataRegionTable.ContainerFilterType.CURRENT_AND_SUBFOLDERS);
        verifyLinksWithText("Lab Machines", "Reagents", "Technicians");
    }

    //Just make sure the list loaded in a DRT without error
    @LogMethod
    private void verifyLinksWithText(@LoggedParam String... texts)
    {
        Set<String> hrefs = new HashSet<>();
        for (String text : texts)
        {
            List<WebElement> links = Locator.linkWithText(text).findElements(getDriver());
            for (WebElement link : links)
            {
                hrefs.add(link.getAttribute("href"));
            }
        }
        for (String href : hrefs)
        {
            beginAt(href);
            waitForElement(Locator.tagWithAttribute("a", "data-original-title", "Grid views"));
            assertElementPresent(Locator.tagWithAttribute("a", "data-original-title","Charts / Reports"));
            assertElementPresent(Locator.lkButton("Design"));
            assertElementPresent(Locator.tagWithAttribute("a", "data-original-title","edit"));
            assertElementPresent(Locator.tagWithAttribute("a", "data-original-title","details"));
        }
    }

    @Test
    public void folderTemplateTest()
    {
        log("creating a collaboration folder");
        _containerHelper.createSubfolder(getProjectName(),"Parent Folder","Collaboration");

        log("creating template folder from above folder");
        _containerHelper.createSubFolderFromTemplateWithTitle(getProjectName(),"Child Folder","/"+getProjectName()+"/Parent Folder","Child Title");
        assertElementPresent(Locator.tagContainingText("h3","Child Title"));
    }

    private void createListWithData(String subfolder) throws Exception
    {
        String containerPath = getProjectName() + "/" + subfolder;
        _listHelper.createList(containerPath, "List1", "RowId",
                new FieldDefinition("Col1", FieldDefinition.ColumnType.String).setLabel("ColLabel"));

        InsertRowsCommand ir = new InsertRowsCommand("lists", "List1");
        Map<String, Object> row1 = new HashMap<>();
        row1.put("Col1", 1);

        ir.addRow(row1);

        Connection cn = new Connection(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        ir.execute(cn, containerPath);
    }

    @Test
    public void deleteFoldersWithoutChildrenTest() throws Exception
    {
        _containerHelper.createSubfolder(getProjectName(), "NoChildren");
        createListWithData("NoChildren");

        clickProject(getProjectName());
        ensureAdminMode();

        //Delete a folder without children, expect only one-step confirmation needed
        goToFolderManagement();
        waitForElement(Ext4Helper.Locators.folderManagementTreeSelectedNode(getProjectName()));
        waitForElement(Locator.tagWithText("span", "Shared").withClass("x4-tree-node-text"));
        sleep(1000); //allow the tree to expand
        waitAndClick(Locator.tagWithText("span", "NoChildren").withClass("x4-tree-node-text"));
        waitForElement(Ext4Helper.Locators.folderManagementTreeSelectedNode("NoChildren"));
        clickButton("Delete", WAIT_FOR_PAGE);
        waitForElement(Locator.tagWithText("td", "You are about to delete the following folder:"));
        waitForElement(Locator.tagContainingText("li", getProjectName() + "/NoChildren, containing the following objects:"));
        waitForElement(Locator.tagWithText("li", "1 lists"));
        clickButton("Yes, delete all", WAIT_FOR_PAGE);
    }

    @Test
    public void deleteFoldersWithChildrenTest() throws Exception
    {
        _containerHelper.createSubfolder(getProjectName(), "HasChildren");
        createListWithData("HasChildren");
        _containerHelper.createSubfolder(getProjectName() + "/HasChildren", "Subfolder");

        clickProject(getProjectName());
        ensureAdminMode();

        //Delete a folder with children, expect two-step confirmation needed
        goToFolderManagement();
        waitForElement(Ext4Helper.Locators.folderManagementTreeSelectedNode(getProjectName()));
        waitForElement(Locator.tagWithText("span", "Shared").withClass("x4-tree-node-text"));
        sleep(1000); //allow the tree to expand
        waitAndClick(Locator.tagWithText("span", "HasChildren").withClass("x4-tree-node-text"));
        waitForElement(Ext4Helper.Locators.folderManagementTreeSelectedNode("HasChildren"));
        clickButton("Delete", WAIT_FOR_PAGE);
        waitForElement(Locator.tagContainingText("li", getProjectName() + "/HasChildren, containing the following objects:"));
        waitForElement(Locator.tagContainingText("li", getProjectName() + "/HasChildren/Subfolder"));
        waitForElement(Locator.tagWithText("li", "1 lists"));
        clickButton("Yes, delete all", WAIT_FOR_PAGE);
    }

    @Test
    public void deleteWorkbooksTest() throws Exception
    {
        //now delete multiple workbooks:
        _containerHelper.createSubfolder(getProjectName(), "WorkbookParent");
        goToProjectHome(getProjectName() + "/WorkbookParent");
        PortalHelper ph = new PortalHelper(this);
        ph.addBodyWebPart("Workbooks");

        WorkbookHelper workbookHelper = new WorkbookHelper(this);
        workbookHelper.createWorkbook(getProjectName() + "/WorkbookParent", "WB1", "Description", WorkbookHelper.WorkbookFolderType.DEFAULT_WORKBOOK);
        int wb = workbookHelper.createWorkbook(getProjectName() + "/WorkbookParent", "WB2", "Description", WorkbookHelper.WorkbookFolderType.DEFAULT_WORKBOOK);
        createListWithData("WorkbookParent/" + wb);

        workbookHelper.createWorkbook(getProjectName() + "/WorkbookParent", "WB3", "Description", WorkbookHelper.WorkbookFolderType.DEFAULT_WORKBOOK);
        goToProjectHome(getProjectName() + "/WorkbookParent");
        DataRegionTable dr = DataRegionTable.findDataRegionWithinWebpart(this, "Workbooks");

        assertEquals("Incorrect row count", 3, dr.getDataRowCount());

        dr.checkCheckbox(1);
        dr.checkCheckbox(2);
        dr.clickHeaderButton("Delete");
        assertTrue(acceptAlert().contains("Are you sure you want to delete the selected rows?"));
        waitForElement(Locator.tagWithText("td", "You are about to delete the following folders:"));
        waitForElement(Locator.tagContainingText("li", getProjectName() + "/WorkbookParent/2, containing the following objects"));
        waitForElement(Locator.tagWithText("li", "1 lists"));
        waitForElement(Locator.tagContainingText("li", getProjectName() + "/WorkbookParent/3 and all the objects it contains"));
        clickButton("Yes, delete all", WAIT_FOR_PAGE);

        //make sure we cannot delete using the client API
        goToProjectHome(getProjectName() + "/WorkbookParent");
        try
        {
            Connection cn = new Connection(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
            SelectRowsCommand sr = new SelectRowsCommand("core", "workbooks");
            sr.setColumns(Arrays.asList("EntityId"));
            SelectRowsResponse srr = sr.execute(cn, getProjectName() + "/WorkbookParent");
            Object entityId = srr.getRows().get(0).get("EntityId");

            DeleteRowsCommand drc = new DeleteRowsCommand("core", "workbooks");
            Map<String, Object> row1 = new HashMap<>();
            row1.put("EntityId", entityId);
            drc.addRow(row1);

            SaveRowsResponse resp = drc.execute(new Connection(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword()), getContainerId());

            throw new Exception("That command should have failed");
        }
        catch (CommandException e)
        {
            assertEquals("Incorrect error message", "Deleting workbooks through the query service is not supported", e.getMessage());
        }

        _containerHelper.deleteFolder(getProjectName(), "WorkbookParent");
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
