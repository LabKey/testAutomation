package org.labkey.test.tests.list;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.Hosting;
import org.labkey.test.pages.LabkeyErrorPage;
import org.labkey.test.pages.list.GridPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.TestUser;
import org.labkey.test.util.query.QueryApiHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@Category({Daily.class, Data.class, Hosting.class})
public class CrossFolderListTest extends BaseWebDriverTest
{
    private static final String SUBFOLDER_A = "subA";
    private static String SUBFOLDER_A_PATH;

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
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
        SUBFOLDER_A_PATH = getProjectName() + "/" + SUBFOLDER_A;
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
        assertThat("expect only the data inserted here to be shown",
                subFolderListPage.getGrid().getColumnDataAsText("String Column"), hasItems("stringy", "chewy"));
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
        assertThat("expect to be shown folder and key columns",
                subFolderListPage.getGrid().getColumnNames(), hasItems("container", "Key"));

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

        // create a simple list in subfolder, insert some values, and grab a column's worth of data from it
        ListDefinition listDef = createListDef(listName, testFields());
        var dGen = listDef.create(createDefaultConnection(), getProjectName());
        dGen.withGeneratedRows(2);
        dGen.insertRows();
        var topStrings = dGen.getRows().stream().map(a-> a.get("stringColumn").toString()).collect(Collectors.toList());

        // create another list with the same name at the project level, insert a little different data and capture the string values
        ListDefinition listDef2 = createListDef(listName, testFields());
        var dgen2 = listDef2.create(createDefaultConnection(), SUBFOLDER_A_PATH);
        dgen2.withGeneratedRows(3);
        dgen2.insertRows();
        var bottomStrings = dgen2.getRows().stream().map(a-> a.get("stringColumn").toString()).collect(Collectors.toList());

        // navigate to the top folder, open the container filter to include the subfolder and ensure only data from this list is shown
        var topPage = GridPage.beginAt(this, getProjectName(), listName);
        topPage.getGrid().setContainerFilter(DataRegionTable.ContainerFilterType.CURRENT_AND_SUBFOLDERS);   // include subfolder in filter
        assertEquals("expect the view in the top folder to only show current list data even with different subfolder list by the same name",
                new HashSet(topStrings), new HashSet(topPage.getGrid().getColumnDataAsText("String Column")));

        // now view the list from the subfolder and ensure the list contents aren't mixed despite name ambiguity
        var subfolderPage = GridPage.beginAt(this, SUBFOLDER_A_PATH, listName);
        subfolderPage.getGrid().setContainerFilter(DataRegionTable.ContainerFilterType.ALL_FOLDERS);
        assertEquals("expect the view in the top folder to only show current list data even with different subfolder list by the same name",
                new HashSet(bottomStrings), new HashSet(subfolderPage.getGrid().getColumnDataAsText("String Column")));
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
        return "CrossFolderListTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
