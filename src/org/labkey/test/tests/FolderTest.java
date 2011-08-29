/*
 * Copyright (c) 2011 LabKey Corporation
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
import org.labkey.test.util.ExtHelper;

/**
 * User: Nick
 * Date: May 5, 2011
 */
public class FolderTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "FolderTestProject";
    private static final String WIKITEST_NAME = "WikiTestFolderCreate";
    private static final String FOLDER_CREATION_FILE = "folderTest.html";
    private static final String PROJECT_FOLDER_XPATH = "//li[@class='x-tree-node' and ./div/a/span[text()='"+PROJECT_NAME+"']]";
    private static final String SERVER_ROOT = "LabKey Server Projects";
    
    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected void checkQueries() // skip query validation
    { /* Too many folder to check queries. */ }

    @Override
    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }
    
    @Override
    protected void doTestSteps() throws Exception
    {
        createProject(PROJECT_NAME);
        createFolders();

        moveFolders();
    }

    protected void createFolders()
    {
        // Initialize the Creation Wiki
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Wiki");

        createNewWikiPage();
        setFormElement("name", WIKITEST_NAME);
        setFormElement("title", WIKITEST_NAME);
        setWikiBody("Placeholder text.");
        saveWikiPage();

        setSourceFromFile(FOLDER_CREATION_FILE, WIKITEST_NAME);

        // Run the Test Script
        clickButton("Start Test", 0);
        waitForElement(Locator.button("Done."), 60000);
    }

    protected void moveFolders()
    {
        log("Moving Folders");
        clickLinkWithText(PROJECT_NAME);
        clickAdminMenuItem("Manage Project", "Folders");

        log("Ensure folders will be visible");
        selenium.windowMaximize();

        clickNavButton("Change Display Order");
        checkRadioButton("resetToAlphabetical", "false");
        selectOptionByText(Locator.name("items"), PROJECT_NAME);
        for(int i = 0; i < 100 && getElementIndex(Locator.xpath("//option[@value='"+PROJECT_NAME+"']")) > 0; i++)
            clickNavButton("Move Up", 0);
        clickNavButton("Save");

        // Use drag-and-drop to reorder folders.
        log("Reorder Projects test");
        reorderProjects(PROJECT_NAME, "Shared", Reorder.preceding, true);
        sleep(500); // Wait for folder move to complete.
        refresh();
        // TODO : Figure out how to get the order of project names within the panel
        //assertTextBefore("Shared", PROJECT_NAME);

        clickNavButton("Change Display Order");
        checkRadioButton("resetToAlphabetical", "false");
        selectOptionByText(Locator.name("items"), PROJECT_NAME);
        for(int i = 0; i < 100 && getElementIndex(Locator.xpath("//option[@value='"+PROJECT_NAME+"']")) > 0; i++)
            clickNavButton("Move Up", 0);
        clickNavButton("Save");
                
        log("Reorder folders test");
        expandFolderNode("AB");
        reorderFolder("[ABB]", "[ABA]", Reorder.preceding, true);
        sleep(500); // Wait for folder move to complete.
        refresh();
        expandNavFolders("[A]", "[AB]", "[AB]");
        assertTextBefore("[ABB]", "[ABA]");

        log("Illegal folder move test: Project demotion");
        moveFolder(PROJECT_NAME, "home", false, false);
        ExtHelper.waitForExtDialog(this, "Change Display Order");  // it should only give option to reorder projects
        clickNavButton("Cancel", 0);

        log("Illegal folder move test: Folder promotion");
        expandFolderNode("");
        moveFolder("[A]", SERVER_ROOT, false, false);

        log("Move folder test");
        sleep(500); // wait for failed move ghost to disappear.
        expandFolderNode("ABB");
        moveFolder("[ABBA]", "[ABA]", true, false);
        sleep(500); // Wait for folder move to complete.
        refresh();
        expandNavFolders("[A]", "[AB]", "[ABA]");
        assertTextBefore("[ABB]", "[ABBA]");


        //Issue 12762: Provide way to cancel a folder move
        log("Cancel folder move test");
        sleep(500); // wait for failed move ghost to disappear.
        expandFolderNode("ABA");
        moveFolder("[ABBA]", "[F]", true, false, false);
        sleep(500); // Wait for folder move to complete.
        refresh();
        expandNavFolders("[A]", "[AB]", "[ABA]", "[F]");
        assertTextBefore("[ABB]", "[ABBA]");

        log("Illegal multiple folder move: non-siblings");
        expandFolderNode("A");
        expandFolderNode("B");
        selenium.getEval("selenium.selectFolderManagementItem('FolderTestProject/[A]/[AA]', false)");
        selenium.getEval("selenium.selectFolderManagementItem('FolderTestProject/[B]/[BA]', true)");
        sleep(500);
        moveFolder("[AA]", "[C]", false, true);

        log("Move multiple folders");
        expandFolderNode("AB");
        sleep(500); // wait for failed move ghost to disappear.
        selenium.getEval("selenium.selectFolderManagementItem('FolderTestProject/[D]', false)");
        selenium.getEval("selenium.selectFolderManagementItem('FolderTestProject/[E]', true)");
        selenium.getEval("selenium.selectFolderManagementItem('FolderTestProject/[F]', true)");
        sleep(500);
        moveFolder("[D]", "[AB]", true, true);
        sleep(500);
        refresh();
        expandNavFolders("[A]", "[AB]");
        assertTextBefore("[AB]" ,"[D]");
        assertTextBefore("[D]" ,"[E]");
        assertTextBefore("[E]" ,"[F]");
        assertTextBefore("[F]" ,"[AC]");
        assertTextBefore("[F]" ,"[C]");
    }

    private void reorderProjects(String project, String targetProject, Reorder order, boolean successExpected)
    {
        log("Reorder project: '" + project + "' " + order.toString() + " '" + targetProject + "'");
        Locator p = Locator.xpath("//div/a/span[text()='"+project+"']");
        Locator t = Locator.xpath("//div/a/span[text()='"+targetProject+"']");
        
        waitForElement(p, WAIT_FOR_JAVASCRIPT);
        waitForElement(t, WAIT_FOR_JAVASCRIPT);

        sleep(1000); //TODO: Figure out what to wait for

        dragAndDrop(p, t, order == Reorder.preceding ? Position.top : Position.bottom);
        if(successExpected)
        {
            ExtHelper.waitForExtDialog(this, "Change Display Order");
            clickNavButton("Confirm Reorder", 0);
        }
    }
    
    private void reorderFolder(String folder, String targetFolder, Reorder order, boolean successExpected)
    {
        log("Reorder folder: '" + folder + "' " + order.toString() + " '"  + targetFolder + "'");
        waitForElement(Locator.xpath("//div/a/span[text()='"+folder+"']"), WAIT_FOR_JAVASCRIPT);

        sleep(1000); //TODO: Figure out what to wait for

        dragAndDrop(Locator.xpath(PROJECT_FOLDER_XPATH + "//div/a/span[text()='"+folder+"']"), Locator.xpath(PROJECT_FOLDER_XPATH + "//div/a/span[text()='"+targetFolder+"']"), order == Reorder.preceding ? Position.top : Position.bottom);
        if(successExpected)
        {
            ExtHelper.waitForExtDialog(this, "Change Display Order");
            clickNavButton("Confirm Reorder", 0);
        }
        //TODO: else {confirm failure}
    }

    private enum Reorder {following, preceding}

    private void moveFolder(String folder, String targetFolder, boolean successExpected, boolean multiple, boolean confirmMove)
    {
        log("Move folder: '" + folder + "' into '"  + targetFolder + "'");
        waitForElement(Locator.xpath("//div/a/span[text()='"+folder+"']"), WAIT_FOR_JAVASCRIPT);

        sleep(1000); //TODO: Figure out what to wait for

        dragAndDrop(Locator.xpath(PROJECT_FOLDER_XPATH + "//div/a/span[text()='"+folder+"']"), Locator.xpath("//div/a/span[text()='"+targetFolder+"']"), Position.middle);
        if(successExpected)
        {
            ExtHelper.waitForExtDialog(this, "Move Folder");
            if (multiple)
                assertTextPresent("You are moving multiple folders.");
            else
                assertTextPresent("You are moving folder '"+folder+"'");
            if(confirmMove)
            {
                clickNavButton("Confirm Move", 0);
                if (multiple) ExtHelper.waitForExtDialog(this, "Moving Folders");
                ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);
            }
            else
            {
                clickNavButton("Cancel", 0);
            }
        }
        //TODO: else {confirm failure}
    }

    private void moveFolder(String folder, String targetFolder, boolean successExpected, boolean multiple)
    {
        moveFolder(folder, targetFolder, successExpected, multiple, true);
    }

    // Specific to this test's folder naming scheme. Digs to requested folder. Adds brackets.
    private void expandFolderNode(String folder)
    {
        waitForElement(Locator.xpath(PROJECT_FOLDER_XPATH), WAIT_FOR_JAVASCRIPT);
        if(getAttribute(Locator.xpath(PROJECT_FOLDER_XPATH + "/div/img[contains(@class, 'x-tree-elbow')]"), "class").contains("x-tree-elbow-plus"))
        {
            click(Locator.xpath(PROJECT_FOLDER_XPATH + "/div/img[contains(@class, 'x-tree-elbow')]"));
            waitForElementToDisappear(Locator.xpath(PROJECT_FOLDER_XPATH + "/div/img[contains(@class, 'x-tree-elbow-plus')]"), WAIT_FOR_JAVASCRIPT);
        }

        for (int i = 1; i <= folder.length(); i++ )
        {
            String folderRowXpath = PROJECT_FOLDER_XPATH + "//li[@class='x-tree-node']/div[./a/span[text()='["+folder.substring(0, i)+"]']]";
            waitForElement(Locator.xpath(folderRowXpath), WAIT_FOR_JAVASCRIPT);
            if(getAttribute(Locator.xpath(folderRowXpath+ "/img[contains(@class, 'x-tree-elbow')]"), "class").contains("plus"))
            {
                click(Locator.xpath(folderRowXpath + "/img[contains(@class, 'x-tree-elbow')]"));
                waitForElement(Locator.xpath(folderRowXpath + "/img[not(contains(@class, 'plus'))]"), WAIT_FOR_JAVASCRIPT);
            }
        }
        sleep(500);
    }

    private void expandFolders(String... folders)
    {
        for (String folder : folders)
        {
            String folderRowXpath = "//li[@class='x-tree-node']/div[./a/span[text()='"+folder+"']]";
            waitForElement(Locator.xpath(folderRowXpath), WAIT_FOR_JAVASCRIPT);
            if(getAttribute(Locator.xpath(folderRowXpath+"/img[contains(@class, 'x-tree-elbow')]"), "class").contains("plus"))
            {
                click(Locator.xpath(folderRowXpath+"/img[contains(@class, 'x-tree-elbow')]"));
                waitForElement(Locator.xpath(folderRowXpath + "/img[not(contains(@class, 'plus'))]"), WAIT_FOR_JAVASCRIPT);
            }
        }
    }

    private void collapseFolderNode(String folder)
    {
        if(getAttribute(Locator.xpath("//div[./a/span[text()='"+folder+"']]/img[contains(@class, 'x-tree-elbow')]"), "class").contains("x-tree-elbow-minus"))
        {
            click(Locator.xpath("//div[./a/span[text()='"+folder+"']]/img[contains(@class, 'x-tree-elbow')]"));
            waitForElement(Locator.xpath("//div[./a/span[text()='"+folder+"']]/img[contains(@class, 'x-tree-elbow-plus')]"), WAIT_FOR_JAVASCRIPT);
        }
    }

    private void expandNavFolders(String... folders)
    {
        for (String folder : folders)
        {
            assertElementPresent(Locator.xpath("//tr[./td/a[text()='"+folder+"']]/td[@class='labkey-nav-tree-node']/a"));
            if(isElementPresent(Locator.xpath("//tr[./td/a[text()='"+folder+"']]/td[@class='labkey-nav-tree-node']/a/img[contains(@src, 'plus')]")))
                click(Locator.xpath("//tr[./td/a[text()='"+folder+"']]/td[@class='labkey-nav-tree-node']/a"));
        }
    }
}
