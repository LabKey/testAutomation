/*
 * Copyright (c) 2010-2012 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

public class WorkbookTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "Workbook Test Project";
    private static final String DEFAULT_WORKBOOK_NAME = "TestWorkbook";
    private static final String DEFAULT_WORKBOOK_DESCRIPTION = "Test Default Workbook Type";
    private static final String FILE_WORKBOOK_NAME = "TestFileWorkbook";
    private static final String FILE_WORKBOOK_DESCRIPTION = "Test File Workbook Type";
    private static final String ASSAY_WORKBOOK_DESCRIPTION = "Test Assay Workbook Type";
    private static final String ASSAY_WORKBOOK_NAME = "TestAssayWorkbook";
    private static final String APITEST_NAME = "WorkbookAPIs";
    private static final String APITEST_FILE = "workbookAPITest.html";


    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/workbook";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public void doCleanup(boolean afterTest)
    {
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    public void doTestSteps()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        addWebPart("Workbooks");

        // Create File Workbook
        createWorkbook(PROJECT_NAME, FILE_WORKBOOK_NAME, FILE_WORKBOOK_DESCRIPTION, WorkbookFolderType.FILE_WORKBOOK);
        assertLinkPresentWithText("Files");
        Assert.assertEquals(FILE_WORKBOOK_NAME, getText(Locator.xpath("//span[preceding-sibling::span[contains(@class, 'wb-name')]]")));
        Assert.assertEquals(FILE_WORKBOOK_DESCRIPTION, getText(Locator.xpath("//div[@id='wb-description']")));
        assertLinkNotPresentWithText(FILE_WORKBOOK_NAME); // Should not appear in folder tree.

        // Create Assay Workbook
        createWorkbook(PROJECT_NAME, ASSAY_WORKBOOK_NAME, ASSAY_WORKBOOK_DESCRIPTION, WorkbookFolderType.ASSAY_WORKBOOK);
        assertLinkPresentWithText("Experiment Runs");
        Assert.assertEquals(ASSAY_WORKBOOK_NAME, getText(Locator.xpath("//span[preceding-sibling::span[contains(@class, 'wb-name')]]")));
        Assert.assertEquals(ASSAY_WORKBOOK_DESCRIPTION, getText(Locator.xpath("//div[@id='wb-description']")));
        assertLinkNotPresentWithText(ASSAY_WORKBOOK_NAME); // Should not appear in folder tree.

        // Create Default Workbook
        createWorkbook(PROJECT_NAME, DEFAULT_WORKBOOK_NAME, DEFAULT_WORKBOOK_DESCRIPTION, WorkbookFolderType.DEFAULT_WORKBOOK);
        assertLinkPresentWithText("Files");
        assertLinkPresentWithText("Experiment Runs");
        Assert.assertEquals(DEFAULT_WORKBOOK_NAME, getText(Locator.xpath("//span[preceding-sibling::span[contains(@class, 'wb-name')]]")));
        Assert.assertEquals(DEFAULT_WORKBOOK_DESCRIPTION, getText(Locator.xpath("//div[@id='wb-description']")));
        assertLinkNotPresentWithText(DEFAULT_WORKBOOK_NAME); // Should not appear in folder tree.

        // Edit Workbook Name
        waitAndClick(Locator.xpath("//span[preceding-sibling::span[contains(@class, 'wb-name')]]"));
        waitForElement(Locator.xpath("//input[@value='"+DEFAULT_WORKBOOK_NAME+"']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//input[@value='"+DEFAULT_WORKBOOK_NAME+"']"), "Renamed"+DEFAULT_WORKBOOK_NAME);
        click(Locator.css("body"));
        assertTextPresent("Renamed"+DEFAULT_WORKBOOK_NAME);

        // Clear description
        click(Locator.xpath("//div[@id='wb-description']"));
        setFormElement(Locator.xpath("//textarea"), ""); // textarea is a barely used tag, so this xpath is sufficient for now.
        waitForText("No description provided. Click to add one.", WAIT_FOR_JAVASCRIPT); // Takes a moment to appear.

        // Check that title and description are saved
        refresh();
        assertTextPresent("Renamed"+DEFAULT_WORKBOOK_NAME);
        waitForText("No description provided. Click to add one.", WAIT_FOR_JAVASCRIPT); // Takes a moment to appear.

        clickLinkWithText(PROJECT_NAME);

        // Check for all workbooks in list.
        assertLinkPresentWithText("Renamed"+DEFAULT_WORKBOOK_NAME);
        assertLinkPresentWithText(ASSAY_WORKBOOK_NAME);
        assertLinkPresentWithText(FILE_WORKBOOK_NAME);
        assertTextPresentInThisOrder(FILE_WORKBOOK_NAME, ASSAY_WORKBOOK_NAME, "Renamed"+DEFAULT_WORKBOOK_NAME);

        // Delete a workbook
        checkDataRegionCheckbox("query", 2); // Select renamed workbook
        clickButton("Delete", 0);
        assertAlert("Are you sure you want to delete the selected row?");
        waitForTextToDisappear("Renamed"+DEFAULT_WORKBOOK_NAME);

        // Test Workbook APIs

        // Initialize the Creation Wiki
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Wiki");

        createNewWikiPage();
        setFormElement(Locator.name("name"), APITEST_NAME);
        setFormElement(Locator.name("title"), APITEST_NAME);
        setWikiBody("Placeholder text.");
        saveWikiPage();

        setSourceFromFile(APITEST_FILE, APITEST_NAME);


        clickButton("RunAPITest", 0);

        waitForText("Insert complete", WAIT_FOR_JAVASCRIPT);
        waitForText("Delete complete", WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Insert complete - Success.", "Delete complete - Success.");

    }

    private enum WorkbookFolderType
    {
        ASSAY_WORKBOOK("Assay Test Workbook"),
        FILE_WORKBOOK("File Test Workbook"),
        DEFAULT_WORKBOOK("Workbook");

        private final String _type;

        WorkbookFolderType(String type)
        {
            this._type = type;
        }

        @Override
        public String toString()
        {
            return _type;
        }
    }

    private void createWorkbook(String project, String title, String description, WorkbookFolderType folderType)
    {
        clickFolder(project);
        clickButton("Insert New");

        setFormElement(Locator.id("workbookTitle"), title);
        setFormElement(Locator.id("workbookDescription"), description);
        selectOptionByText(Locator.id("workbookFolderType"), folderType.toString());

        clickButton("Create Workbook");
    }
}
