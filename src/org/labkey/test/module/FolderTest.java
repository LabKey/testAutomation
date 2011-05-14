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
