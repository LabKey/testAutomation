package org.labkey.test.tests.list;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.Hosting;
import org.labkey.test.pages.list.BeginPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.VarListDefinition;
import org.labkey.test.util.DomainUtils;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.TestUser;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.labkey.test.util.PermissionsHelper.EDITOR_ROLE;

@Category({Daily.class, Data.class, Hosting.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class ListDeleteTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ListDeleteTest";
    private static final String PROJECT_PATH = "/" + PROJECT_NAME;
    private static final String SUBFOLDER_A_NAME = "SubfolderA";
    private static final String SUBFOLDER_A_PATH = PROJECT_PATH + "/" + SUBFOLDER_A_NAME;
    private static final String SUBFOLDER_B_NAME = "SubfolderB";
    private static final String SUBFOLDER_B_PATH = PROJECT_PATH + "/" + SUBFOLDER_B_NAME;

    private static final TestUser LIST_DESIGNER_USER = new TestUser("listdesigner@listdelete.test");

    private static final String attachmentFieldName = TestDataGenerator.randomFieldName("Attachment", null, DomainUtils.DomainKind.IntList);
    private static final String booleanFieldName = TestDataGenerator.randomFieldName("Boolean", null, DomainUtils.DomainKind.IntList);
    private static final String integerFieldName = TestDataGenerator.randomFieldName("Integer", null, DomainUtils.DomainKind.IntList);
    private static final String stringFieldName = TestDataGenerator.randomFieldName("String", null, DomainUtils.DomainKind.IntList);

    private static final String autoIncrementKeyFieldName1 = TestDataGenerator.randomFieldName("Key", null, DomainUtils.DomainKind.IntList);

    protected static final File IMG_FILE = TestFileUtils.getSampleData("InlineImages/help.jpg"); // use in Project
    protected static final File PDF_FILE = TestFileUtils.getSampleData("InlineImages/agraph.pdf"); // use in Subfolder A
    protected static final File TXT_FILE = TestFileUtils.getSampleData("InlineImages/test.txt"); // use in Subfolder B

    private static IntListDefinition LIST_1; // int list with attachment column
    private static VarListDefinition LIST_2; // var list with attachment column, keyed by string

    @BeforeClass
    public static void setupProject() throws Exception
    {
        ListDeleteTest init = getCurrentTest();
        init.doSetup();
    }

    private void doSetup() throws Exception
    {
        // Create project and subfolders
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), SUBFOLDER_A_NAME);
        _containerHelper.createSubfolder(getProjectName(), SUBFOLDER_B_NAME);

        // Create user with Assay Designer + Editor permissions
        LIST_DESIGNER_USER.create(this)
                .setInitialPassword()
                .addPermission(EDITOR_ROLE, PROJECT_PATH)
                .addPermission("Assay Designer", PROJECT_PATH)
                .addPermission(EDITOR_ROLE, SUBFOLDER_A_PATH)
                .addPermission("Assay Designer", SUBFOLDER_A_PATH)
                .addPermission(EDITOR_ROLE, SUBFOLDER_B_PATH);

        var conn = createDefaultConnection();

        // Create list 1 with attachment column
        var list1Name = DomainUtils.DomainKind.IntList.randomName("DEL1");
        LIST_1 = (IntListDefinition) new IntListDefinition(list1Name, autoIncrementKeyFieldName1)
                .setFields(List.of(
                        new FieldDefinition(attachmentFieldName, FieldDefinition.ColumnType.Attachment),
                        new FieldDefinition(booleanFieldName, FieldDefinition.ColumnType.Boolean),
                        new FieldDefinition(integerFieldName, FieldDefinition.ColumnType.Integer),
                        new FieldDefinition(stringFieldName, FieldDefinition.ColumnType.String)
                ));
        LIST_1.getCreateCommand().execute(conn, PROJECT_PATH);

        // Create list 2 — var list with attachment column, keyed by string
        var stringKeyField = new FieldDefinition(stringFieldName);
        var list2Name = DomainUtils.DomainKind.VarList.randomName("DEL2");
        LIST_2 = (VarListDefinition) new VarListDefinition(list2Name)
                .setKeyName(stringKeyField.getName())
                .setFields(List.of(
                        stringKeyField,
                        new FieldDefinition(attachmentFieldName, FieldDefinition.ColumnType.Attachment),
                        new FieldDefinition(booleanFieldName, FieldDefinition.ColumnType.Boolean),
                        new FieldDefinition(integerFieldName, FieldDefinition.ColumnType.Integer)
                ));
        LIST_2.getCreateCommand().execute(conn, PROJECT_PATH);

        // Populate data in project folder for both lists
        populateList1(PROJECT_PATH, IMG_FILE);
        populateList2(PROJECT_PATH, IMG_FILE);

        // Populate data in subfolder A for both lists
        populateList1(SUBFOLDER_A_PATH, PDF_FILE);
        populateList2(SUBFOLDER_A_PATH, PDF_FILE);

        // Populate data in subfolder B for both lists
        populateList1(SUBFOLDER_B_PATH, TXT_FILE);
        populateList2(SUBFOLDER_B_PATH, TXT_FILE);
    }

    private void populateList1(String containerPath, File attachment) throws IOException
    {
        var dataGenerator = LIST_1.getTestDataGenerator(containerPath)
                .addDataSupplier(attachmentFieldName, () -> attachment);

        // Insert rows with attachment values via UI (only way to provide attachment values)
        var attachmentRows = dataGenerator.withGeneratedRows(1)
                .getRows();

        _listHelper.beginAtList(containerPath, LIST_1.getName());
        var newRow = new CaseInsensitiveHashMap<>();
        newRow.putAll(attachmentRows.getFirst());
        _listHelper.insertNewRow(newRow, false);
    }

    private void populateList2(String containerPath, File attachment) throws IOException
    {
        var dataGenerator = LIST_2.getTestDataGenerator(containerPath)
                .addDataSupplier(stringFieldName, () -> "String" + containerPath)
                .addDataSupplier(attachmentFieldName, () -> attachment);

        // Insert rows without attachments via API
        var attachmentRows = dataGenerator.withGeneratedRows(1)
                .getRows();

        _listHelper.beginAtList(containerPath, LIST_2.getName());
        var newRow = new CaseInsensitiveHashMap<>();
        newRow.putAll(attachmentRows.getFirst());
        _listHelper.insertNewRow(newRow, false);
    }

    private void verifyConfirmationPage(String containerPath, List<String> listNames)
    {
        // Navigate to manage lists page, clear all row selections, verify delete button is disabled
        var listsPage = BeginPage.beginAt(this, containerPath);
        var grid = listsPage.getGrid();
        grid.uncheckAllOnPage();
        var deleteButton = grid.getHeaderButton("Delete");
        checker().withScreenshot().verifyTrue("Delete button should be disabled when no rows selected",
                deleteButton.getAttribute("class").contains("disabled"));

        // Select lists, verify delete menu is enabled and has 2 options
        grid.selectLists(listNames);
        var menuOptions = grid.getHeaderMenuOptions("Delete");
        checker().withScreenshot().verifyEquals("Expected 2 delete menu options",
                List.of("Delete List", "Delete All Data from List"), menuOptions);

        // Click DELETE -> "Delete List", verify landing on confirmation page with expected text, cancel
        listsPage = BeginPage.beginAt(this, containerPath);
        grid = listsPage.getGrid();
        grid.selectLists(listNames);
        grid.clickHeaderMenu("Delete", true, "Delete List");
        assertTextPresent("Are you sure you want to delete the following Lists?");
        for (String listName : listNames)
            assertElementPresent(Locator.linkWithText(listName));
        clickButton("Cancel");

        // Click DELETE -> "Delete All Data from List", verify landing on confirmation page with expected text, cancel
        listsPage = BeginPage.beginAt(this, containerPath);
        grid = listsPage.getGrid();
        grid.selectLists(listNames);
        grid.clickHeaderMenu("Delete", true, "Delete All Data from List");
        assertTextPresent("Are you sure you want to delete all data");
        assertTextPresent("This action cannot be undone and will result in an empty list.");
        for (String listName : listNames)
        {
            assertElementPresent(Locator.linkWithText(listName));
        }
        assertTextPresent("1 row");
        clickButton("Cancel");
    }

    private void verifyListRowCount(String containerPath, String listName, int expectedCount)
    {
        _listHelper.beginAtList(containerPath, listName);
        var table = new DataRegionTable("query", getDriver());
        checker().withScreenshot().verifyEquals("Expected " + expectedCount + " rows in " + listName + " at " + containerPath,
                expectedCount, table.getDataRowCount());
    }

    private void verifyListDataWithAttachment(String containerPath, String listName, int expectedCount, String attachmentFileName)
    {
        _listHelper.beginAtList(containerPath, listName);
        var table = new DataRegionTable("query", getDriver());
        checker().withScreenshot().verifyEquals("Expected " + expectedCount + " rows in " + listName + " at " + containerPath,
                expectedCount, table.getDataRowCount());

        if (attachmentFileName.contains(IMG_FILE.getName()))
        {
            log("Hover over the thumbnail for the image and make sure the pop-up is as expected.");
            verifyImagePopupInGrid(IMG_FILE);

        }
        else
        {
            File download = doAndWaitForDownload(() -> click(Locator.linkWithText(attachmentFileName)));
            checker().withScreenshot().verifyTrue("Downloaded attachment should exist: " + attachmentFileName, download.exists());
        }

    }

    /**
     * Verifies list deletion and data truncation across folders and permission levels.
     *
     * <ol>
     *   <li>Verify "Delete List" and "Delete All Data from List" confirmation pages render correctly
     *       for single and multi-list selections across project and subfolders.</li>
     *   <li>Impersonate a non-admin designer user (Editor + Assay Designer) and verify they see a
     *       simple "Delete" button (no "Delete All Data" menu option) and can reach the delete
     *       confirmation page. Cancel without deleting.</li>
     *   <li>As admin, truncate all list data from Subfolder A. Verify both lists are empty in
     *       Subfolder A while data and attachments remain intact in the project and Subfolder B.</li>
     *   <li>As admin, truncate only LIST_2 data from the project. Verify LIST_2 is empty in the
     *       project while LIST_1 data in the project and all data in Subfolder B are unaffected.</li>
     *   <li>Impersonate the designer user again. Verify no Delete button appears in Subfolder B
     *       (where the user lacks Assay Designer permission). Delete LIST_1 from Subfolder A and
     *       LIST_2 from the project, verifying the list definitions are removed.</li>
     * </ol>
     */
    @Test
    public void testDeleteListData() throws IOException, CommandException
    {
        verifyConfirmationPage(getProjectName(), List.of(LIST_1.getName()));
        verifyConfirmationPage(getProjectName(), List.of(LIST_1.getName(), LIST_2.getName()));
        verifyConfirmationPage(SUBFOLDER_A_PATH, List.of(LIST_1.getName()));
        verifyConfirmationPage(SUBFOLDER_B_PATH, List.of(LIST_1.getName(), LIST_2.getName()));

        LIST_DESIGNER_USER.impersonate();

        // verify DESIGNER don't see the menu option to "Delete All Data from List", only "Delete" button
        var listsPage = BeginPage.beginAt(this, getProjectName());
        var grid = listsPage.getGrid();
        grid.uncheckAllOnPage();
        var deleteButton = grid.getHeaderButton("Delete");
        checker().withScreenshot().verifyTrue("Delete button should be disabled when no rows selected",
                deleteButton.getAttribute("class").contains("disabled"));

        // Select lists, verify delete button is enabled
        grid.selectLists(List.of(LIST_1.getName(), LIST_2.getName()));
        // verify DELETE button is enabled, verify click DELETE land on confirmation page, click cancel
        checker().withScreenshot().verifyFalse("Delete button should be enabled when rows are selected",
                deleteButton.getAttribute("class").contains("disabled"));
        grid.clickHeaderButton("Delete");
        assertTextPresent("Are you sure you want to delete the following Lists?");
        for (String listName : List.of(LIST_1.getName(), LIST_2.getName()))
            assertElementPresent(Locator.linkWithText(listName));
        clickButton("Cancel");
        stopImpersonating();

        // Verify deleting data from Subfolder A doesn't impact lists or data in project folder or Subfolder B
        listsPage = BeginPage.beginAt(this, SUBFOLDER_A_PATH);
        grid = listsPage.getGrid();
        grid.selectLists(List.of(LIST_1.getName(), LIST_2.getName()));
        grid.clickHeaderMenu("Delete", true, "Delete All Data from List");
        assertTextPresent("Are you sure you want to delete all data");
        assertTextPresent("This action cannot be undone and will result in an empty list.");
        for (String listName : List.of(LIST_1.getName(), LIST_2.getName()))
        {
            assertElementPresent(Locator.linkWithText(listName));
            assertTextPresent("1 row");
        }
        clickButton("Confirm Delete All Data");

        // Verify data deleted in Subfolder A — both lists should be empty
        verifyListRowCount(SUBFOLDER_A_PATH, LIST_1.getName(), 0);
        verifyListRowCount(SUBFOLDER_A_PATH, LIST_2.getName(), 0);

        // Verify data still exists in project folder
        // Go to LIST_1, verify grid is not empty, verify attachment can still be downloaded successfully
        verifyListDataWithAttachment(PROJECT_PATH, LIST_1.getName(), 1, IMG_FILE.getName());

        // Go to Subfolder B, go to LIST_2, verify data present, verify attachment can still be downloaded successfully
        verifyListDataWithAttachment(SUBFOLDER_B_PATH, LIST_2.getName(), 1, TXT_FILE.getName());

        // Now delete just LIST_2 data from project folder
        listsPage = BeginPage.beginAt(this, PROJECT_PATH);
        grid = listsPage.getGrid();
        grid.uncheckAllOnPage();
        grid.selectLists(List.of(LIST_2.getName()));
        grid.clickHeaderMenu("Delete", true, "Delete All Data from List");

        // Verify confirmation page
        assertTextPresent("Are you sure you want to delete all data");
        assertElementPresent(Locator.linkWithText(LIST_2.getName()));
        assertElementNotPresent(Locator.linkWithText(LIST_1.getName()));
        assertTextPresent("1 row");
        clickButton("Confirm Delete All Data");

        // Verify data deleted from LIST_2 in project
        verifyListRowCount(PROJECT_PATH, LIST_2.getName(), 0);

        // Verify data still present in LIST_2 in Subfolder B and in LIST_1 in both folders
        verifyListRowCount(SUBFOLDER_B_PATH, LIST_2.getName(), 1);
        verifyListRowCount(PROJECT_PATH, LIST_1.getName(), 1);
        verifyListRowCount(SUBFOLDER_B_PATH, LIST_1.getName(), 1);

        LIST_DESIGNER_USER.impersonate();

        // From Subfolder B, verify LIST_DESIGNER_USER cannot delete LIST_1 or LIST_2
        // since they don't have designer permission in the sub folder
        listsPage = BeginPage.beginAt(this, SUBFOLDER_B_PATH);
        grid = listsPage.getGrid();
        checker().withScreenshot().verifyFalse("Delete button should not be present without designer permission",
                grid.hasHeaderMenu("Delete"));

        // From Subfolder A, verify LIST_DESIGNER_USER can delete LIST_1
        listsPage = BeginPage.beginAt(this, SUBFOLDER_A_PATH);
        grid = listsPage.getGrid();
        grid.uncheckAllOnPage();
        grid.selectLists(List.of(LIST_1.getName()));
        grid.clickHeaderButtonAndWait("Delete");
        assertTextPresent("Are you sure you want to delete the following Lists?");
        assertElementPresent(Locator.linkWithText(LIST_1.getName()));
        clickButton("Confirm Delete");

        // Verify LIST_1 is deleted successfully
        listsPage = BeginPage.beginAt(this, PROJECT_PATH);
        grid = listsPage.getGrid();
        checker().withScreenshot().verifyFalse("LIST_1 should no longer exist",
                grid.getListNames().contains(LIST_1.getName()));
        checker().withScreenshot().verifyTrue("LIST_2 should still exist",
                grid.getListNames().contains(LIST_2.getName()));

        // From project folder, verify LIST_DESIGNER_USER can delete LIST_2
        grid.selectLists(List.of(LIST_2.getName()));
        grid.clickHeaderButtonAndWait("Delete");
        assertTextPresent("Are you sure you want to delete the following Lists?");
        assertElementPresent(Locator.linkWithText(LIST_2.getName()));
        clickButton("Confirm Delete");

        stopImpersonating();
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        _userHelper.deleteUsers(afterTest, LIST_DESIGNER_USER);
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return List.of("list");
    }
}