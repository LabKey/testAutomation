package org.labkey.test.tests.list;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.Hosting;
import org.labkey.test.pages.LabkeyErrorPage;
import org.labkey.test.pages.list.GridPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.DomainUtils;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.query.QueryApiHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Category({Daily.class, Data.class, Hosting.class})
public class CrossFolderListTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "CrossFolderListTest Project";
    private static final String SUBFOLDER_A = "subA";
    private static final String SUBFOLDER_A_PATH = PROJECT_NAME + "/" + SUBFOLDER_A;

    private static final String SHARED_LIST = "shared_list_for_crossfolder_test";

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        DomainUtils.ensureDeleted("/Shared", "lists", SHARED_LIST);
    }

    @BeforeClass
    public static void setupProject()
    {
        CrossFolderListTest init = (CrossFolderListTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), SUBFOLDER_A);
    }

    @Test
    public void testCannotAddSameNameListToSubfolder() throws Exception
    {
        // begin by creating a list in the project level
        String listName = "name_collision_test_list";
        ListDefinition listDef = createListDef(listName, testFields());
        listDef.create(createDefaultConnection(), getProjectName());

        // use the UI to attempt to create a list by the same name in the subfolder
        // expect the UI to refuse
        var listEditPage = new ListHelper(getDriver()).beginCreateList(SUBFOLDER_A_PATH, listName);
        listEditPage.addField(new FieldDefinition("field", FieldDefinition.ColumnType.String));
        var errors = listEditPage.clickSaveExpectingErrors();

        assertThat(errors).as("error should explain that our list name is already in use")
                .containsOnly("The name 'name_collision_test_list' is already in use.");
    }

    @Test
    public void testListViewShowsContainerByDefault()
    {
        goToProjectHome(getProjectName());
        var listsPage = goToManageLists();

        // ensure 'folder' is represented in the default view here
        assertThat(listsPage.getGrid().getColumnNames()).as("expect default view to contain 'Container'")
                .contains("Container");

        // expect default folder filter to be "current folder, project, and shared project"
        assertThat(listsPage.getGrid().getContainerFilter()).as("expect current, project, shared")
                .isEqualTo("Current folder, project, and Shared project");
    }

    @Test
    public void testCanDeleteTestInSeparateFolder() throws Exception
    {
        // create a list in a subfolder
        String listName = "suba_folder_delete_test";
        ListDefinition listDef = createListDef(listName, testFields());
        listDef.create(createDefaultConnection(), SUBFOLDER_A_PATH);

        // also a list in '/Shared'
        ListDefinition sharedListDef = createListDef(SHARED_LIST, testFields());
        sharedListDef.create(createDefaultConnection(), "/Shared");

        goToProjectHome();
        var helper = new ListHelper(getDriver());
        helper.beginAtList(getProjectName(), SHARED_LIST);

        // insert a record into the shared list, in the current folder
        DataRegionTable.DataRegion(getDriver()).find().clickInsertNewRow()
                .update(Map.of("intColumn", 1, "decimalColumn", "2.2", "stringColumn", "stringy"));

        // navigate to the shared list but view it in the project
        helper.beginAtList(getProjectName(), SHARED_LIST);

        // click the 'stringy' link in the record we put into the shared list
        clickAndWait(Locator.linkWithText("stringy"));  // the presence of this link confirms data in this view
        assertThat(getURL().toString()).as("expect data url to be in project, is not in /Shared")
                .contains("CrossFolderListTest")
                .doesNotContain("Shared");
        // now click on the list link, expecting that to navigate us to where it is defined, in ths /Shared folder
        clickAndWait(Locator.linkWithText(SHARED_LIST));
        assertThat(getURL().toString()).as("expect link to shared list to navigate to shared folder")
                .doesNotContain("CrossFolderListTest")
                .contains("Shared");

        // navigate to lists in the subfolderA
        navigateToFolder(getProjectName(), SUBFOLDER_A);
        var listsPage = goToManageLists();
        // ensure the created lists are visible in this view (assumes folder filter is set to Current folder, project, and Shared)
        var listNames = listsPage.getGrid().getColumnDataAsText("Name");
        assertThat(listNames).as("expect shared and other list to be present")
                .contains(listName, SHARED_LIST);

        // delete these lists via the UI
        var testListIndex = listsPage.getGrid().getRowIndex("Name", listName);
        var sharedListIndex = listsPage.getGrid().getRowIndex("Name", SHARED_LIST);
        listsPage.getGrid().checkCheckbox(testListIndex);
        listsPage.getGrid().checkCheckbox(sharedListIndex);
        listsPage.getGrid().deleteSelectedLists();

        // verify they are no longer present
        var afterListNames = listsPage.getGrid().getColumnDataAsText("Name");
        assertThat(afterListNames).as("expect shared and other list to be absent niw")
                .doesNotContain(listName, SHARED_LIST);
    }

    @Test
    public void testAddDataInSubfolderToTopLevelList() throws Exception
    {
        // define a list in the top folder, give it some random data
        String listName = "top_folder_list_for_subfolder_data";
        ListDefinition listDef = createListDef(listName, testFields());
        var dGen = listDef.create(createDefaultConnection(), getProjectName());
        dGen.withGeneratedRows(3);
        dGen.insertRows();

        // make a couple rows of data for the subfolder
        List<Map<String, Object>> rowsToInsert = List.of(
                Map.of("intColumn", 1, "decimalColumn", 1.1,
                        "stringColumn", "stringy", "dateColumn", "11/11/2023", "boolColumn", true),
                Map.of("intColumn", 2, "decimalColumn", 2.2,
                        "stringColumn", "chewy", "dateColumn", "11/12/2023", "boolColumn", false)
        );

        // insert 2 records into the list, in the subfolder
        var qah = new QueryApiHelper(createDefaultConnection(), SUBFOLDER_A_PATH, "lists", listName);
        qah.insertRows(rowsToInsert);
        var subFolderListPage = GridPage.beginAt(this, SUBFOLDER_A_PATH, listName);

        var displayedData = subFolderListPage.getGrid().getTableData();
        assertEquals("expect only subfolder data to be shown here by default",
                rowsToInsert.size(), displayedData.size());
        assertThat(subFolderListPage.getGrid().getColumnDataAsText("String Column"))
                .as("expect only the data inserted here to be shown").contains("stringy", "chewy");
    }

    @Test
    public void testListMetadataIsAvailableInSubfolder() throws Exception
    {
        // define a list in the top folder, give it some random data
        String listName = "top_folder_list_for_metadata";
        ListDefinition listDef = createListDef(listName, testFields());
        var dGen = listDef.create(createDefaultConnection(), getProjectName());
        dGen.withGeneratedRows(2);
        dGen.insertRows();

        // make a row of data for the subfolder
        List<Map<String, Object>> rowToInsert = List.of(
                Map.of("intColumn", 3, "decimalColumn", 3.3,
                        "stringColumn", "meta", "dateColumn", "11/14/2023", "boolColumn", true));

        // insert 1 record into the list, in the subfolder
        var qah = new QueryApiHelper(createDefaultConnection(), SUBFOLDER_A_PATH, "lists", listName);
        qah.insertRows(rowToInsert);
        var subFolderListPage = GridPage.beginAt(this, SUBFOLDER_A_PATH, listName);

        // modify the default view to include key, folder
        var customizeView = subFolderListPage.getGrid().openCustomizeGrid();
        customizeView.showHiddenItems();
        customizeView.addColumn("Key");
        customizeView.addColumn("Container");   // fun fact: the label is 'Folder'
        customizeView.saveDefaultView();

        // now ensure we got the metadata
        var displayedData = subFolderListPage.getGrid().getTableData();
        assertEquals("expect only 1 record to be visible here",
                1, displayedData.size());
        assertThat(subFolderListPage.getGrid().getColumnNames())
                .as("expect to be shown folder and key columns").contains("container", "Key");

        var displayedRecord = displayedData.get(0);
        assertEquals("expect subfolder name metadata to be available here",
                SUBFOLDER_A, displayedRecord.get("container"));
        assertNotNull("expect list metadata like key to be accessible in a subfolder",
                displayedRecord.get("Key"));
    }

    @Test
    public void testSubfolderDefinedListCannotBeQueriedFromTopLevel() throws Exception
    {
        // define a list in a subfolder, give it some random data
        String listName = "subfolder_list_for_metadata";
        ListDefinition listDef = createListDef(listName, testFields());
        listDef.create(createDefaultConnection(), SUBFOLDER_A_PATH);

        // attempt to navigate to the list but in the project level
        GridPage.beginAt(this, getProjectName(), listName);
        // expect to land on an error page instead
        var errPage = new LabkeyErrorPage(getDriver());
        assertEquals("Expect error explaining that the specified list is not here",
                "List does not exist in this container", errPage.getErrorHeading());
    }

    @Test
    public void testListsWithSameNameInTopAndSubfolder() throws Exception
    {
        String listName = "test_name_collision_list";

        // The following requires that the list be defined in the subfolder before defining a list with the same
        // name in the top-level folder. This is done to ensure backwards compatibility of lists existing with the
        // same name in the same scope.

        // create a simple list in subfolder, insert some values, and grab a column's worth of data from it
        ListDefinition listDef = createListDef(listName, testFields());
        var dGen = listDef.create(createDefaultConnection(), SUBFOLDER_A_PATH);
        dGen.withGeneratedRows(2);
        dGen.insertRows();
        var subfolderData = dGen.getRows().stream().map(a-> a.get("stringColumn").toString()).collect(Collectors.toList());

        // create another list with the same name at the project level, insert a little different data and capture the string values
        ListDefinition listDef2 = createListDef(listName, testFields());
        var dgen2 = listDef2.create(createDefaultConnection(), getProjectName());
        dgen2.withGeneratedRows(3);
        dgen2.insertRows();
        var topFolderData = dgen2.getRows().stream().map(a-> a.get("stringColumn").toString()).collect(Collectors.toList());

        // navigate to the top folder, open the container filter to include the subfolder and ensure only data from this list is shown
        var topPage = GridPage.beginAt(this, getProjectName(), listName);
        topPage.getGrid().setContainerFilter(DataRegionTable.ContainerFilterType.CURRENT_AND_SUBFOLDERS_PLUS_SHARED);
        assertEquals("expect the view in the top folder to only show current list data even with different subfolder list by the same name",
                new HashSet<>(topFolderData), new HashSet<>(topPage.getGrid().getColumnDataAsText("String Column")));

        // now view the list from the subfolder and ensure the list contents aren't mixed despite name ambiguity
        var subfolderPage = GridPage.beginAt(this, SUBFOLDER_A_PATH, listName);
        subfolderPage.getGrid().setContainerFilter(DataRegionTable.ContainerFilterType.CURRENT_PLUS_PROJECT_AND_SHARED);
        assertEquals("expect the view in the top folder to only show current list data even with different subfolder list by the same name",
                new HashSet<>(subfolderData), new HashSet<>(subfolderPage.getGrid().getColumnDataAsText("String Column")));
    }

    private ListDefinition createListDef(String listName, List<FieldDefinition> listColumns)
    {
        ListDefinition listDef = new IntListDefinition(listName, "Key");
        listDef.setFields(listColumns);
        return listDef;
    }

    private List<FieldDefinition> testFields()
    {
        return Arrays.asList(
                new FieldDefinition("intColumn", FieldDefinition.ColumnType.Integer),
                new FieldDefinition("decimalColumn", FieldDefinition.ColumnType.Decimal),
                new FieldDefinition("stringColumn", FieldDefinition.ColumnType.String),
                new FieldDefinition("dateColumn", FieldDefinition.ColumnType.DateAndTime),
                new FieldDefinition("boolColumn", FieldDefinition.ColumnType.Boolean));
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
