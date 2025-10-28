package org.labkey.test.tests.list;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.Hosting;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.LabkeyErrorPage;
import org.labkey.test.pages.list.GridPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldInfo;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.DomainUtils;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.query.QueryApiHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.labkey.test.util.ListHelper.LIST_SCHEMA;

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
        DomainUtils.ensureDeleted("/Shared", LIST_SCHEMA, SHARED_LIST);
    }

    @BeforeClass
    public static void setupProject()
    {
        CrossFolderListTest init = getCurrentTest();
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
        listEditPage.selectAutoIntegerKeyField();
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
                .isEqualTo(DataRegionTable.ContainerFilterType.CURRENT_PLUS_PROJECT_AND_SHARED);
    }

    @Test
    public void testRenameList() throws Exception
    {
        goToProjectHome();
        String listName = "list_to_be_renamed";
        String newListName = "list_that_was_renamed";
        ListDefinition listDef = createListDef(listName, testFields());
        listDef.create(createDefaultConnection(), getProjectName());

        goToProjectFolder(getProjectName(), SUBFOLDER_A);
        new ListHelper(getDriver())
                .goToEditDesign(listName)
                .setName(newListName)
                .clickSave();

        // Issue 47356: Rewrite returnUrl in the event of a list name change
        new GridPage(getDriver()).getGrid();
        assertThat(getURL().getQuery()).contains("name=" + newListName);
        assertThat(getURL().getPath()).contains("/" + SUBFOLDER_A);
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
        String intColumnName = sharedListDef.getFieldByNamePart("intColumn").getName();
        String decimalColumnName = sharedListDef.getFieldByNamePart("decimalColumn").getName();
        String stringColumnName = sharedListDef.getFieldByNamePart("stringColumn").getName();
        DataRegionTable.DataRegion(getDriver()).find().clickInsertNewRow()
                .update(Map.of(intColumnName, 1, decimalColumnName, "2.2", stringColumnName, "stringy"));

        // navigate to the shared list but view it in the project
        helper.beginAtList(getProjectName(), SHARED_LIST);

        // click the 'stringy' link in the record we put into the shared list
        clickAndWait(Locator.linkWithText("stringy"));  // the presence of this link confirms data in this view
        assertThat(getCurrentContainerPath()).as("expect data url to be in project, is not in /Shared")
                . isEqualTo("/" + getProjectName());
        // now click on the list link, expecting that to navigate us to where it is defined, in the /Shared folder
        clickAndWait(Locator.linkWithText(SHARED_LIST));
        assertThat(getCurrentContainerPath()).as("expect link to shared list to navigate to shared folder")
                .isEqualTo("/Shared");

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

        String intColumnName = listDef.getFieldByNamePart("intColumn").getName();
        String decimalColumnName = listDef.getFieldByNamePart("decimalColumn").getName();
        String stringColumnName = listDef.getFieldByNamePart("stringColumn").getName();
        String dateColumnName = listDef.getFieldByNamePart("dateColumn").getName();
        String boolColumnName = listDef.getFieldByNamePart("boolColumn").getName();

        // make a couple rows of data for the subfolder
        List<Map<String, Object>> rowsToInsert = List.of(
                Map.of(intColumnName, 1, decimalColumnName, 1.1,
                        stringColumnName, "stringy", dateColumnName, "11/11/2023",
                        boolColumnName, true),
                Map.of(intColumnName, 2, decimalColumnName, 2.2,
                        stringColumnName, "chewy", dateColumnName, "11/12/2023",
                        boolColumnName, false)
        );

        // insert 2 records into the list, in the subfolder
        var qah = new QueryApiHelper(createDefaultConnection(), SUBFOLDER_A_PATH, LIST_SCHEMA, listName);
        qah.insertRows(rowsToInsert);
        var subFolderListPage = GridPage.beginAt(this, SUBFOLDER_A_PATH, listName);

        var displayedData = subFolderListPage.getGrid().getTableData();
        assertEquals("expect only subfolder data to be shown here by default",
                rowsToInsert.size(), displayedData.size());
        assertThat(subFolderListPage.getGrid().getColumnDataAsText(stringColumnName))
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

        String intColumnName = listDef.getFieldByNamePart("intColumn").getName();
        String decimalColumnName = listDef.getFieldByNamePart("decimalColumn").getName();
        String stringColumnName = listDef.getFieldByNamePart("stringColumn").getName();
        String dateColumnName = listDef.getFieldByNamePart("dateColumn").getName();
        String boolColumnName = listDef.getFieldByNamePart("boolColumn").getName();

        // make a row of data for the subfolder
        List<Map<String, Object>> rowToInsert = List.of(
                Map.of(intColumnName, 3,
                        decimalColumnName, 3.3,
                        stringColumnName, "meta",
                        dateColumnName, "11/14/2023",
                        boolColumnName, true
                ));

        // insert 1 record into the list, in the subfolder
        var qah = new QueryApiHelper(createDefaultConnection(), SUBFOLDER_A_PATH, LIST_SCHEMA, listName);
        qah.insertRows(rowToInsert);
        var subFolderListPage = GridPage.beginAt(this, SUBFOLDER_A_PATH, listName);

        // modify the default view to include key, folder
        var customizeView = subFolderListPage.getGrid().openCustomizeGrid();
        customizeView.showHiddenItems();
        customizeView.addColumn("Key");
        customizeView.addColumn("container");   // fun fact: the label is 'Folder'
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
        List<FieldDefinition> fields = testFields();
        ListDefinition listDef = createListDef(listName, fields);
        var dGen = listDef.create(createDefaultConnection(), SUBFOLDER_A_PATH);
        dGen.withGeneratedRows(2);
        dGen.insertRows();
        String stringColumnName = listDef.getFieldByNamePart("stringColumn").getName();
        var subfolderData = dGen.getRows().stream().map(a-> a.get(stringColumnName).toString()).toList();

        // create another list with the same name at the project level, insert a little different data and capture the string values
        ListDefinition listDef2 = createListDef(listName, fields);
        var dgen2 = listDef2.create(createDefaultConnection(), getProjectName());
        dgen2.withGeneratedRows(3);
        dgen2.insertRows();
        var topFolderData = dgen2.getRows().stream().map(a-> a.get(stringColumnName).toString()).toList();

        // navigate to the top folder, open the container filter to include the subfolder and ensure only data from this list is shown
        var topPage = GridPage.beginAt(this, getProjectName(), listName);
        topPage.getGrid().setContainerFilter(DataRegionTable.ContainerFilterType.CURRENT_AND_SUBFOLDERS_PLUS_SHARED);
        assertEquals("expect the view in the top folder to only show current list data even with different subfolder list by the same name",
                new HashSet<>(topFolderData), new HashSet<>(topPage.getGrid().getColumnDataAsText(stringColumnName)));

        // now view the list from the subfolder and ensure the list contents aren't mixed despite name ambiguity
        var subfolderPage = GridPage.beginAt(this, SUBFOLDER_A_PATH, listName);
        subfolderPage.getGrid().setContainerFilter(DataRegionTable.ContainerFilterType.CURRENT_PLUS_PROJECT_AND_SHARED);
        assertEquals("expect the view in the top folder to only show current list data even with different subfolder list by the same name",
                new HashSet<>(subfolderData), new HashSet<>(subfolderPage.getGrid().getColumnDataAsText(stringColumnName)));
    }

    @Test // Issue 52501
    public void testLookupValidatorFolderScope() throws Exception
    {
        var firstListName = TestDataGenerator.randomDomainName(null, DomainUtils.DomainKind.IntList);
        var secondListName = TestDataGenerator.randomDomainName(null, DomainUtils.DomainKind.IntList);
        var textColumnName = TestDataGenerator.randomFieldName("Text", null, DomainUtils.DomainKind.IntList);
        var lookupFieldName = TestDataGenerator.randomFieldName("LookAtFirst", null, DomainUtils.DomainKind.IntList);
        var encodedLookupFieldName = EscapeUtil.fieldKeyEncodePart(lookupFieldName);

        // Create and configure list definitions
        {
            createListDef(firstListName, List.of(new FieldDefinition(textColumnName)))
                    .create(createDefaultConnection(), getProjectName());

            createListDef(secondListName, List.of(
                new FieldDefinition("Name"),
                new FieldDefinition(lookupFieldName, new FieldDefinition.IntLookup(null, LIST_SCHEMA, firstListName))
            )).create(createDefaultConnection(), getProjectName());

            // Update the lookup to require validation
            var domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), LIST_SCHEMA, secondListName);
            domainDesignerPage.fieldsPanel()
                    .getField(lookupFieldName)
                    .setLookupValidatorEnabled(true);
            domainDesignerPage.clickFinish();
        }

        // Insert data into the first list
        {
            var projectApi = new QueryApiHelper(createDefaultConnection(), getProjectName(), LIST_SCHEMA, firstListName);
            projectApi.insertRows(List.of(Map.of(textColumnName, "Alpha"), Map.of(textColumnName, "Aileron"), Map.of(textColumnName, "Alpaca")));

            var subfolderApi = new QueryApiHelper(createDefaultConnection(), SUBFOLDER_A_PATH, LIST_SCHEMA, firstListName);
            subfolderApi.insertRows(List.of(Map.of(textColumnName, "Beta"), Map.of(textColumnName, "Bingo"), Map.of(textColumnName, "Beep")));
        }

        var secondListGridPage = GridPage.beginAt(this, SUBFOLDER_A_PATH, secondListName);
        secondListGridPage.getGrid()
                .clickInsertNewRow()
                .setField("Name", "OnFirst")
                .setField(lookupFieldName, OptionSelect.SelectOption.textOption("Beep"))
                .submit();
        secondListGridPage = new GridPage(getDriver());

        // Act
        // Attempt to update a validated list lookup
        secondListGridPage.getGrid()
                .clickEditRow(0)
                .setField(lookupFieldName, OptionSelect.SelectOption.textOption("Bingo"))
                .submit();

        // Assert
        // Expect to be navigated back to the grid
        secondListGridPage = new GridPage(getDriver());
        var grid = secondListGridPage.getGrid();
        assertEquals("Unexpected number of rows in the list", 1, grid.getDataRowCount());

        var rowMap = grid.getRowDataAsMap(0);
        assertEquals("Unexpected number of column values", 2, rowMap.size());
        assertEquals(String.format("Unexpected value for the lookup field \"%s\"", lookupFieldName), "Bingo", rowMap.get(encodedLookupFieldName));
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
                FieldInfo.random("intColumn", FieldDefinition.ColumnType.Integer, DomainUtils.DomainKind.IntList).getFieldDefinition(),
                FieldInfo.random("decimalColumn", FieldDefinition.ColumnType.Decimal, DomainUtils.DomainKind.IntList).getFieldDefinition(),
                FieldInfo.random("stringColumn", FieldDefinition.ColumnType.String, DomainUtils.DomainKind.IntList).getFieldDefinition(),
                FieldInfo.random("dateColumn", FieldDefinition.ColumnType.DateAndTime, DomainUtils.DomainKind.IntList).getFieldDefinition(),
                FieldInfo.random("boolColumn", FieldDefinition.ColumnType.Boolean, DomainUtils.DomainKind.IntList).getFieldDefinition()
        );
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
