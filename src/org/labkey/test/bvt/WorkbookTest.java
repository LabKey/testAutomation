/*
 * Copyright (c) 2010-2011 LabKey Corporation
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
package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

public class WorkbookTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "Workbook Test Project";
    private static final String DEFAULT_WORKBOOK_NAME = "TestWorkbook";
    private static final String DEFAULT_WORKBOOK_DESCRIPTION = "Test Default Workbook Type";
    private static final String FILE_WORKBOOK_NAME = "TestFileWorkbook";
    private static final String FILE_WORKBOOK_DESCRIPTION = "Test File Workbook Type";
    private static final String ASSAY_WORKBOOK_DESCRIPTION = "Test Assay Workbook Type";
    private static final String ASSAY_WORKBOOK_NAME = "TestAssayWorkbook";

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/workbook";
    }

    @Override
    public void doCleanup()
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    @Override
    public void doTestSteps()
    {
        createProject(PROJECT_NAME);
        addWebPart("Workbooks");

        // Create File Workbook
        createWorkbook(PROJECT_NAME, FILE_WORKBOOK_NAME, FILE_WORKBOOK_DESCRIPTION, WorkbookFolderType.FILE_WORKBOOK);
        assertLinkPresentWithText("Files");
        assertEquals(FILE_WORKBOOK_NAME, getText(Locator.xpath("//span[preceding-sibling::span[contains(@class, 'wb-name')]]")));
        assertEquals(FILE_WORKBOOK_DESCRIPTION, getText(Locator.xpath("//div[@id='wb-description']")));
        assertLinkNotPresentWithText(FILE_WORKBOOK_NAME); // Should not appear in folder tree.

        // Create Assay Workbook
        createWorkbook(PROJECT_NAME, ASSAY_WORKBOOK_NAME, ASSAY_WORKBOOK_DESCRIPTION, WorkbookFolderType.ASSAY_WORKBOOK);
        assertLinkPresentWithText("Experiment Runs");
        assertEquals(ASSAY_WORKBOOK_NAME, getText(Locator.xpath("//span[preceding-sibling::span[contains(@class, 'wb-name')]]")));
        assertEquals(ASSAY_WORKBOOK_DESCRIPTION, getText(Locator.xpath("//div[@id='wb-description']")));
        assertLinkNotPresentWithText(ASSAY_WORKBOOK_NAME); // Should not appear in folder tree.

        // Create Defaultche Workbook
        createWorkbook(PROJECT_NAME, DEFAULT_WORKBOOK_NAME, DEFAULT_WORKBOOK_DESCRIPTION, WorkbookFolderType.DEFAULT_WORKBOOK);
        assertLinkPresentWithText("Pipeline Files");
        assertLinkPresentWithText("Experiment Runs");
        assertEquals(DEFAULT_WORKBOOK_NAME, getText(Locator.xpath("//span[preceding-sibling::span[contains(@class, 'wb-name')]]")));
        assertEquals(DEFAULT_WORKBOOK_DESCRIPTION, getText(Locator.xpath("//div[@id='wb-description']")));
        assertLinkNotPresentWithText(DEFAULT_WORKBOOK_NAME); // Should not appear in folder tree.

        // Edit Workbook Name
        waitAndClick(Locator.xpath("//span[preceding-sibling::span[contains(@class, 'wb-name')]]"));
        waitForElement(Locator.xpath("//input[@value='"+DEFAULT_WORKBOOK_NAME+"']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//input[@value='"+DEFAULT_WORKBOOK_NAME+"']"), "Renamed"+DEFAULT_WORKBOOK_NAME);
        fireEvent(Locator.xpath("//input[contains(@value, '"+DEFAULT_WORKBOOK_NAME+"')]"), SeleniumEvent.blur);
        assertTextPresent("Renamed"+DEFAULT_WORKBOOK_NAME);

        // Clear description
        click(Locator.xpath("//div[@id='wb-description']"));
        setFormElement(Locator.xpath("//textarea"), ""); // textarea is a barely used tag, so this xpath is sufficient for now.
        fireEvent(Locator.xpath("//textarea"), SeleniumEvent.blur);
        waitForText("No description provided. Click to add one.", WAIT_FOR_JAVASCRIPT); // Takes a moment to appear.

        // Check that title and description are saved
        refresh();
        assertTextPresent("Renamed"+DEFAULT_WORKBOOK_NAME);
        waitForText("No description provided. Click to add one.", WAIT_FOR_JAVASCRIPT); // Takes a moment to appear.

        clickLinkWithText(PROJECT_NAME);
        clickNavButton("Manage Workbooks");

        // Check for all workbooks in list.
        assertLinkPresentWithText("Renamed"+DEFAULT_WORKBOOK_NAME);
        assertLinkPresentWithText(ASSAY_WORKBOOK_NAME);
        assertLinkPresentWithText(FILE_WORKBOOK_NAME);

        // Delete a workbook
        checkDataRegionCheckbox("query", 2); // Select renamed workbook
        clickNavButton("Delete");
        assertConfirmation("Are you sure you want to delete the selected row?");
        assertTextNotPresent("Renamed"+DEFAULT_WORKBOOK_NAME);

        String containerId = "workbook-"+getTableCellText("dataregion_query", 2, 1);

        // Test Workbook APIs
        goToModule("Wiki");
        createNewWikiPage("HTML");
        setFormElement("name", "Workbook APIs");
        setFormElement("title", "Workbook API Test Page");
        setWikiBody("<div id='createWorkbookDiv'/><br><div id='deleteWorkbookDiv'/>\n" +
                "<script type=\"text/javascript\">\n" +
                "LABKEY.Security.createContainer({\n" +
                "   title : 'API Workbook',\n" +
                "   description : 'Workbook created by JS API',\n" +
                "   isWorkbook: true,\n" +
                "   failure: createFailure,\n" +
                "   success: createSuccess\n" +
                "   });\n" +
                "LABKEY.Security.deleteContainer({\n" +
                "   containerPath: '/"+PROJECT_NAME+"/"+containerId+"',\n" +
                "   failure: deleteFailure,\n" +
                "   success: deleteSuccess\n" +
                "   });\n" +
                "\n" +
                "function createSuccess()\n" +
                "   {document.getElementById('createWorkbookDiv').innerHTML = 'Insert complete - Success.';}\n" +
                "function createFailure(errorInfo, response)\n" +
                "   {document.getElementById('createWorkbookDiv').innerHTML = 'Insert complete - Failure: ' + errorInfo.exception;}\n" +
                "function deleteSuccess()\n" +
                "   {document.getElementById('deleteWorkbookDiv').innerHTML = 'Delete complete - Success.';}\n" +
                "function deleteFailure(errorInfo, response)\n" +
                "   {document.getElementById('deleteWorkbookDiv').innerHTML = 'Delete complete - Failure: ' + errorInfo.exception;}\n" +
                "</script>");
        saveWikiPage();

        waitForText("Insert complete", WAIT_FOR_JAVASCRIPT);
        waitForText("Delete complete", WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Insert complete - Success.", "Delete complete - Success.");

        clickLinkWithText(PROJECT_NAME);
        assertLinkPresentWithText("API Workbook");
        assertLinkNotPresentWithText(FILE_WORKBOOK_NAME);
    }

    private enum WorkbookFolderType
    {
        ASSAY_WORKBOOK("Assay Workbook"),
        FILE_WORKBOOK("File Workbook"),
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
        clickLinkWithText(project);
        clickNavButton("Create New Workbook");

        setFormElement("workbookTitle", title);
        setFormElement("workbookDescription", description);
        setFormElement("workbookFolderType", folderType.toString());

        clickNavButton("Create Workbook");
    }
}
