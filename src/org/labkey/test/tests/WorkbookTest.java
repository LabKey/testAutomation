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

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.html.SiteNavBar;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;
import org.labkey.test.util.WorkbookHelper;
import org.labkey.test.util.WorkbookHelper.WorkbookFolderType;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class WorkbookTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "Workbook Test Project";
    private static final String PROJECT_NAME2 = "Workbook Test Project 2";
    private static final String DEFAULT_WORKBOOK_NAME = "TestWorkbook" + INJECT_CHARS_2;
    private static final String DEFAULT_WORKBOOK_DESCRIPTION = "Test Default Workbook Type" + INJECT_CHARS_1;
    private static final String FILE_WORKBOOK_NAME = "TestFileWorkbook";
    private static final String FILE_WORKBOOK_DESCRIPTION = "Test File Workbook Type";
    private static final String ASSAY_WORKBOOK_DESCRIPTION = "Test Assay Workbook Type";
    private static final String ASSAY_WORKBOOK_NAME = "TestAssayWorkbook";
    private static final String APITEST_NAME = "WorkbookAPIs";
    private static final String APITEST_FILE = "workbookAPITest.html";


    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.emptyList();
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
        List<Integer> ids = createWorkbooks(PROJECT_NAME, FILE_WORKBOOK_NAME, FILE_WORKBOOK_DESCRIPTION, ASSAY_WORKBOOK_NAME,
                ASSAY_WORKBOOK_DESCRIPTION, DEFAULT_WORKBOOK_NAME, DEFAULT_WORKBOOK_DESCRIPTION);
        assertEquals("id's generated when workbooks are created should be sequential", Arrays.asList(1, 2, 3), ids);

        // Edit Workbook Name
        WebElement wbNameEl = Locator.css(".wb-name + .labkey-edit-in-place").waitForElement(shortWait());
        wbNameEl.click();
        WebElement nameInput = Locator.xpath(".").followingSibling("input").waitForElement(wbNameEl, 1000);
        nameInput.sendKeys(Keys.DELETE, "Renamed" + DEFAULT_WORKBOOK_NAME);
        fireEvent(nameInput, SeleniumEvent.blur);
        shortWait().until(ExpectedConditions.stalenessOf(nameInput));

        // Make sure that the edit stuck
        assertEquals("Renamed" + DEFAULT_WORKBOOK_NAME, wbNameEl.getText());

        // Change the focus to trigger a save
        click(Locator.id("wb-description"));
        Locator emptyDescription = Locator.id("wb-description").withText("No description provided. Click to add one.");
        // Clear description
        Locator.css("#wb-description + textarea").waitForElement(shortWait()).clear();
        waitForElement(emptyDescription);

        // Check that title and description are saved
        refresh();
        assertTextPresent("Renamed" + DEFAULT_WORKBOOK_NAME);
        waitForElement(emptyDescription);

        //Verify folder menu links
        SiteNavBar.AdminMenu menu = new SiteNavBar(getDriver()).adminMenu();
        menu.expand();

        Assert.assertEquals("The current containerPath should have two folder levels", 3, getCurrentContainerPath().split("/").length);
        assertThat("URL does not use workbook", EscapeUtil.decode(getDriver().findElement(Locator.tagWithText("a", "Manage Views")).getAttribute("href")), containsString(getCurrentContainerPath()));
        assertThat("URL does not use workbook", EscapeUtil.decode(getDriver().findElement(Locator.tagWithText("a", "Manage Lists")).getAttribute("href")), containsString(getCurrentContainerPath()));
        assertThat("URL uses workbook", EscapeUtil.decode(getDriver().findElement(Locator.tagWithText("a", "Manage Assays")).getAttribute("href")), not(containsString(getCurrentContainerPath())));
        menu.openMenuTo("Folder");
        assertThat("URL uses workbook", EscapeUtil.decode(getDriver().findElement(Locator.tagWithText("a", "Permissions")).getAttribute("href")), not(containsString(getCurrentContainerPath())));
        assertThat("URL uses workbook", EscapeUtil.decode(getDriver().findElement(Locator.tagWithText("a", "Management")).getAttribute("href")), not(containsString(getCurrentContainerPath())));
        assertThat("URL uses workbook", EscapeUtil.decode(getDriver().findElement(Locator.tagWithText("a", "Project Users")).getAttribute("href")), not(containsString(getCurrentContainerPath())));
        menu.collapse();

        menu.expand();
        menu.openMenuTo("Developer Links");
        assertThat("URL does not use workbook", EscapeUtil.decode(getDriver().findElement(Locator.tagWithText("a", "Schema Browser")).getAttribute("href")), containsString(getCurrentContainerPath()));
        menu.collapse();

        menu.expand();
        menu.openMenuTo("Go To Module");
        assertThat("URL does not use workbook", EscapeUtil.decode(getDriver().findElement(Locator.tagWithText("a", "Experiment")).getAttribute("href")), containsString(getCurrentContainerPath()));
        assertThat("URL does not use workbook", EscapeUtil.decode(getDriver().findElement(Locator.tagWithText("a", "FileContent")).getAttribute("href")), containsString(getCurrentContainerPath()));
        assertThat("URL does not use workbook", EscapeUtil.decode(getDriver().findElement(Locator.tagWithText("a", "Study")).getAttribute("href")), containsString(getCurrentContainerPath()));

        menu.openMenuTo("More Modules");
        assertThat("URL does not use workbook", EscapeUtil.decode(getDriver().findElement(Locator.tagWithText("a", "List")).getAttribute("href")), containsString(getCurrentContainerPath()));
        menu.collapse();

        goToProjectHome();

        // Check for all workbooks in list.
        assertElementPresent(Locator.linkWithText("Renamed" + DEFAULT_WORKBOOK_NAME));
        assertElementPresent(Locator.linkWithText(ASSAY_WORKBOOK_NAME));
        assertElementPresent(Locator.linkWithText(FILE_WORKBOOK_NAME));
        assertTextPresentInThisOrder(FILE_WORKBOOK_NAME, ASSAY_WORKBOOK_NAME, "Renamed" + DEFAULT_WORKBOOK_NAME);

        // Delete a workbook
        DataRegionTable workbooks = new DataRegionTable("query", this);
        workbooks.checkCheckbox(2);
        WorkbookHelper workbookHelper = new WorkbookHelper(this);
        workbookHelper.deleteWorkbooksFromDataRegion(workbooks);
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
        int id = workbookHelper.createWorkbook(PROJECT_NAME2, FILE_WORKBOOK_NAME, FILE_WORKBOOK_DESCRIPTION, WorkbookFolderType.FILE_WORKBOOK);
        assertEquals("workbook added to new project did not have id=1", id, 1);
    }

    private List<Integer> createWorkbooks(String projectName, String fileWorkbookName, String fileWorkbookDescription,
                                  String assayWorkbookName, String assayWorkbookDescription,
                                  String defaultWorkbookName, String defaultWorkbookDescription)
    {
        List<Integer> ids = new ArrayList<>();
        WorkbookHelper workbookHelper = new WorkbookHelper(this);
        ids.add(workbookHelper.createFileWorkbook(projectName, fileWorkbookName, fileWorkbookDescription));

        // Create Assay Workbook
        ids.add(workbookHelper.createWorkbook(projectName, assayWorkbookName, assayWorkbookDescription, WorkbookFolderType.ASSAY_WORKBOOK));
        assertElementPresent(Locator.linkWithText("Experiment Runs"));
        assertEquals(assayWorkbookName, workbookHelper.getEditableTitleText());
        assertEquals(assayWorkbookDescription, workbookHelper.getEditableDescriptionText());
        assertElementNotPresent(Locator.linkWithText(assayWorkbookName)); // Should not appear in folder tree.

        // Create Default Workbook
        ids.add(workbookHelper.createWorkbook(projectName, defaultWorkbookName, defaultWorkbookDescription, WorkbookFolderType.DEFAULT_WORKBOOK));
        assertElementPresent(Locator.linkWithText("Files"));
        assertElementPresent(Locator.linkWithText("Experiment Runs"));
        assertEquals(defaultWorkbookName, workbookHelper.getEditableTitleText());
        assertEquals(defaultWorkbookDescription, workbookHelper.getEditableDescriptionText());
        assertElementNotPresent(Locator.linkWithText(defaultWorkbookName)); // Should not appear in folder tree.
        return ids;
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
