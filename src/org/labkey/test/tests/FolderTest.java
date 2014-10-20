/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.FolderManagementFolderTree;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.JavascriptExecutor;

import java.io.File;

@Category({DailyB.class})
public class FolderTest extends BaseWebDriverTest
{
    private static final File FOLDER_CREATION_SCRIPT = new File(TestFileUtils.getLabKeyRoot(), "server/test/data/api/folderTest.js");
    public final FolderManagementFolderTree folderManagement = new FolderManagementFolderTree(this, getProjectName());

    @Override
    public java.util.List<String> getAssociatedModules()
    {
        return null;
    }

    @Override
    public String getProjectName()
    {
        return "FolderTest#Project";
    }

    @Override
    protected void checkQueries() // skip query validation
    { /* Too many folder to check queries. */ }

    @Override
    public void checkLinks(){} // too many folders

    @BeforeClass
    public static void testSetup()
    {
        FolderTest init = (FolderTest)getCurrentTest();

        init._containerHelper.createProject(init.getProjectName(), null);
        init.moveTestProjectToTop();
        init.createFolders();
    }

    @LogMethod
    private void createFolders()
    {
        clickProject(getProjectName());

        JavascriptExecutor exec = (JavascriptExecutor) getDriver();
        exec.executeAsyncScript(TestFileUtils.getFileContents(FOLDER_CREATION_SCRIPT));
    }

    @LogMethod
    private void moveTestProjectToTop()
    {
        clickProject(getProjectName());
        goToFolderManagement();
        waitForElement(Ext4Helper.Locators.folderManagementTreeNode(getProjectName()));

        log("Ensure folders will be visible");

        clickButton("Change Display Order");
        checkCheckbox(Locator.radioButtonByNameAndValue("resetToAlphabetical", "false"));
        selectOptionByText(Locator.name("items"), getProjectName());
        for(int i = 0; i < 100 && getElementIndex(Locator.xpath("//option[@value='"+ getProjectName() +"']")) > 0; i++)
            clickButton("Move Up", 0);
        clickButton("Save");
    }

    @Before
    public void preTest()
    {
        clickProject(getProjectName());
        goToFolderManagement();
    }

    @Test @Ignore("Test and helpers need update for Ext4 UI")
    public void testMoveFolder()
    {
        log("Reorder folders test");
        folderManagement.expandFolderNode("AB");
        folderManagement.reorderFolder("[ABB]", "[ABA]", FolderManagementFolderTree.Reorder.preceding, true);
        sleep(500); // Wait for folder move to complete.
        refresh();
        folderManagement.expandNavFolders("[A]", "[AB]", "[AB]");
        assertTextBefore("[ABB]", "[ABA]");

        log("Illegal folder move test: Project demotion");
        folderManagement.moveFolder(getProjectName(), "home", false, false);
        _extHelper.waitForExtDialog("Change Display Order");  // it should only give option to reorder projects
        clickButton("Cancel", 0);

        log("Move folder test");
        sleep(500); // wait for failed move ghost to disappear.
        folderManagement.expandFolderNode("ABB");
        folderManagement.moveFolder("[ABBA]", "[ABA]", true, false);
        sleep(500); // Wait for folder move to complete.
        refresh();
        folderManagement.expandNavFolders("[A]", "[AB]", "[ABA]");
        assertTextBefore("[ABB]", "[ABBA]");


        //Issue 12762: Provide way to cancel a folder move
        log("Cancel folder move test");
        sleep(500); // wait for failed move ghost to disappear.
        folderManagement.expandFolderNode("ABA");
        folderManagement.moveFolder("[ABBA]", "[F]", true, false, false);
        sleep(500); // Wait for folder move to complete.
        refresh();
        folderManagement.expandNavFolders("[A]", "[AB]", "[ABA]", "[F]");
        assertTextBefore("[ABB]", "[ABBA]");

        log("Illegal multiple folder move: non-siblings");
        folderManagement.expandFolderNode("A");
        folderManagement.expandFolderNode("B");
        folderManagement.selectFolderManagementTreeItem(getProjectName() + "/[A]/[AA]", false);
        sleep(100);
        folderManagement.selectFolderManagementTreeItem(getProjectName() + "/[B]/[BA]", true);
        sleep(500);
        folderManagement.moveFolder("[AA]", "[C]", false, true);
        _ext4Helper.waitForMaskToDisappear(); // shouldn't be a confirmation dialog.

        log("Move multiple folders");
        sleep(500); // wait for failed move ghost to disappear.
        folderManagement.expandFolderNode("AB");
        folderManagement.selectFolderManagementTreeItem(getProjectName() + "/[D]", false);
        sleep(100);
        folderManagement.selectFolderManagementTreeItem(getProjectName() + "/[E]", true);
        sleep(100);
        folderManagement.selectFolderManagementTreeItem(getProjectName() + "/[F]", true);
        sleep(500);
        folderManagement.moveFolder("[D]", "[AB]", true, true);
        sleep(500);
        refresh();
        folderManagement.expandNavFolders("[A]", "[AB]");
        assertTextBefore("[AB]" ,"[D]");
        assertTextBefore("[D]" ,"[E]");
        assertTextBefore("[E]" ,"[F]");
        assertTextBefore("[F]" ,"[AC]");
        assertTextBefore("[F]" ,"[C]");
    }

    private void reorderProjects(String project, String targetProject, FolderManagementFolderTree.Reorder order, boolean successExpected)
    {
        log("Reorder project: '" + project + "' " + order.toString() + " '" + targetProject + "'");
//        getXpathCount(Locator.xpath("//div[contains(@class, 'x4-unselectable') and text()='"+project+"']/.."));
        Locator p = Locator.xpath("//div[contains(@class, 'x4-unselectable') and text()='"+project+"']/..");
        Locator t = Locator.xpath("//div[contains(@class, 'x4-unselectable') and text()='"+targetProject+"']/..");

//        Locator p = Locator.xpath("//div/a/span[text()='"+project+"']");
//        Locator t = Locator.xpath("//div/a/span[text()='"+targetProject+"']");

        waitForElement(p, WAIT_FOR_JAVASCRIPT);
        waitForElement(t, WAIT_FOR_JAVASCRIPT);

        sleep(1000); //TODO: Figure out what to wait for

        dragAndDrop(p, t, order == FolderManagementFolderTree.Reorder.preceding ? Position.top : Position.bottom);
        if(successExpected)
        {
            _extHelper.waitForExtDialog("Change Display Order");
            clickButton("Confirm Reorder", 0);
        }
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
