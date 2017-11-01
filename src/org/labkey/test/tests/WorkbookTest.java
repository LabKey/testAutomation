/*
 * Copyright (c) 2010-2017 LabKey Corporation
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
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;
import org.labkey.test.util.WorkbookHelper;
import org.labkey.test.util.WorkbookHelper.WorkbookFolderType;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({DailyB.class})
public class WorkbookTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "Workbook Test Project";
    private static final String PROJECT_NAME2 = "Workbook Test Project 2";
    private static final String DEFAULT_WORKBOOK_NAME = "TestWorkbook";
    private static final String DEFAULT_WORKBOOK_DESCRIPTION = "Test Default Workbook Type";
    private static final String FILE_WORKBOOK_NAME = "TestFileWorkbook";
    private static final String FILE_WORKBOOK_DESCRIPTION = "Test File Workbook Type";
    private static final String ASSAY_WORKBOOK_DESCRIPTION = "Test Assay Workbook Type";
    private static final String ASSAY_WORKBOOK_NAME = "TestAssayWorkbook";
    private static final String APITEST_NAME = "WorkbookAPIs";
    private static final String APITEST_FILE = "workbookAPITest.html";


    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("workbook");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _containerHelper.deleteProject(PROJECT_NAME2, afterTest);
    }

    @Test
    public void testSteps()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        WikiHelper wikiHelper = new WikiHelper(this);

        _containerHelper.createProject(PROJECT_NAME, null);
        portalHelper.addWebPart("Workbooks");
        int[] ids = createWorkbooks(PROJECT_NAME, FILE_WORKBOOK_NAME, FILE_WORKBOOK_DESCRIPTION, ASSAY_WORKBOOK_NAME,
                ASSAY_WORKBOOK_DESCRIPTION, DEFAULT_WORKBOOK_NAME, DEFAULT_WORKBOOK_DESCRIPTION);
        //id's generated when workbooks are created should be sequential
        int lastid = 0;
        for(int i=0; i>ids.length; i++)
        {
            assertEquals("non-sequential name for workbook found",ids[i],lastid + 1);
        }

        // Edit Workbook Name
        waitAndClick(Locator.xpath("//span[preceding-sibling::span[contains(@class, 'wb-name')]]"));
        waitForElement(Locator.xpath("//input[@value='" + DEFAULT_WORKBOOK_NAME + "']"));
        setFormElement(Locator.xpath("//input[@value='" + DEFAULT_WORKBOOK_NAME + "']"), "Renamed" + DEFAULT_WORKBOOK_NAME);

        // Change the focus to trigger a save
        click(Locator.id("wb-description"));

        // Make sure that the edit stuck
        assertTextPresent("Renamed" + DEFAULT_WORKBOOK_NAME);

        // Clear description
        setFormElement(Locator.xpath("//textarea"), ""); // textarea is a barely used tag, so this xpath is sufficient for now.
        waitForText("No description provided. Click to add one.");

        // Check that title and description are saved
        refresh();
        assertTextPresent("Renamed" + DEFAULT_WORKBOOK_NAME);
        waitForText("No description provided. Click to add one.");

        goToProjectHome();

        // Check for all workbooks in list.
        assertElementPresent(Locator.linkWithText("Renamed" + DEFAULT_WORKBOOK_NAME));
        assertElementPresent(Locator.linkWithText(ASSAY_WORKBOOK_NAME));
        assertElementPresent(Locator.linkWithText(FILE_WORKBOOK_NAME));
        assertTextPresentInThisOrder(FILE_WORKBOOK_NAME, ASSAY_WORKBOOK_NAME, "Renamed" + DEFAULT_WORKBOOK_NAME);

        // Delete a workbook
        DataRegionTable workbooks = new DataRegionTable("query", this);
        workbooks.checkCheckbox(2);
        workbooks.clickHeaderButton("Delete");
        assertAlert("Are you sure you want to delete the selected row?");
        waitForTextToDisappear("Renamed" + DEFAULT_WORKBOOK_NAME);

        // Test Workbook APIs

        // Initialize the Creation Wiki
        goToProjectHome();
        portalHelper.addWebPart("Wiki");

        wikiHelper.createNewWikiPage();
        setFormElement(Locator.name("name"), APITEST_NAME);
        setFormElement(Locator.name("title"), APITEST_NAME);
        wikiHelper.setWikiBody("Placeholder text.");
        wikiHelper.saveWikiPage();

        wikiHelper.setSourceFromFile(APITEST_FILE, APITEST_NAME);

        clickButton("RunAPITest", 0);

        waitForText("Insert complete", "Delete complete");
        assertTextPresent("Insert complete - Success.", "Delete complete - Success.");

        //Create new project, add a workbook to it and ensure that the id is 1
        _containerHelper.createProject(PROJECT_NAME2, null);
        portalHelper.addWebPart("Workbooks");
        WorkbookHelper workbookHelper = new WorkbookHelper(this);
        int id = workbookHelper.createWorkbook(PROJECT_NAME2, FILE_WORKBOOK_NAME, FILE_WORKBOOK_DESCRIPTION, WorkbookFolderType.FILE_WORKBOOK);
        assertEquals("workbook added to new project did not have id=1", id, 1);
    }

    private int[] createWorkbooks(String projectName, String fileWorkbookName, String fileWorkbookDescription,
                                  String assayWorkbookName, String assayWorkbookDescription,
                                  String defaultWorkbookName, String defaultWorkbookDescription)
    {
        int[] names = new int[3];
        WorkbookHelper workbookHelper = new WorkbookHelper(this);
        names[0] = workbookHelper.createFileWorkbook(projectName, fileWorkbookName, fileWorkbookDescription);

        // Create Assay Workbook
        names[1] = workbookHelper.createWorkbook(projectName, assayWorkbookName, assayWorkbookDescription, WorkbookFolderType.ASSAY_WORKBOOK);
        assertElementPresent(Locator.linkWithText("Experiment Runs"));
        assertEquals(assayWorkbookName, workbookHelper.getEditableTitleText());
        assertEquals(assayWorkbookDescription, workbookHelper.getEditableDescriptionText());
        assertElementNotPresent(Locator.linkWithText(assayWorkbookName)); // Should not appear in folder tree.

        // Create Default Workbook
        names[2] = workbookHelper.createWorkbook(projectName, defaultWorkbookName, defaultWorkbookDescription, WorkbookFolderType.DEFAULT_WORKBOOK);
        assertElementPresent(Locator.linkWithText("Files"));
        assertElementPresent(Locator.linkWithText("Experiment Runs"));
        assertEquals(defaultWorkbookName, workbookHelper.getEditableTitleText());
        assertEquals(defaultWorkbookDescription, workbookHelper.getEditableDescriptionText());
        assertElementNotPresent(Locator.linkWithText(defaultWorkbookName)); // Should not appear in folder tree.
        return names;
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
