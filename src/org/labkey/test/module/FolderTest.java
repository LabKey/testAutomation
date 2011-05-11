package org.labkey.test.module;

import org.labkey.test.BaseSeleniumWebTest;

/**
 * User: Nick
 * Date: May 5, 2011
 */
public class FolderTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "FolderTestProject";
    private static final String WIKITEST_NAME = "WikiTestFolderCreate";
    private static final String FOLDER_CREATION_FILE = "folderTest.html";
    
    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }
    
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
        clickButton("Start Test");
        waitForText("Done.", 60000);
    }

    protected void moveFolders()
    {
        log("Moving Folders");
    }
}
