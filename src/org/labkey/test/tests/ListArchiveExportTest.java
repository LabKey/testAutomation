package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Hosting;
import org.labkey.test.components.list.ManageListsGrid;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PermissionsHelper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({Daily.class, Hosting.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class ListArchiveExportTest extends BaseWebDriverTest
{
    private final static String LIST_SUBFOLDER = "List subfolder";
    private final static String LIST_FOLDER_A = "LIST_FOLDER_A";
    private final static String LIST_A = "List Export A";
    private final static String LIST_B = "List Export B";
    private final static String _listUser = "listuser@listarchiveexport.test";

    @Override
    protected @Nullable String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("list");
    }

    @BeforeClass
    public static void setupProject() throws IOException, CommandException
    {
        ListArchiveExportTest initTest = (ListArchiveExportTest) getCurrentTest();
        initTest.doSetUp();
    }

    private void doSetUp() throws IOException, CommandException
    {
        _containerHelper.createProject(getProjectName(), null);
        createListWithData(LIST_B, Map.of("Shape", "Triangle", "Count", "4"));

        _containerHelper.createSubfolder(getProjectName(), LIST_SUBFOLDER);
        createListWithData(LIST_A, Map.of("Shape", "Square", "Count", "7"));

        _containerHelper.createProject(LIST_FOLDER_A);
        createListWithData(LIST_A, Map.of("Shape", "Circle", "Count", "10"));

        _userHelper.createUser(_listUser);
        ApiPermissionsHelper _permissionHelper = new ApiPermissionsHelper(LIST_FOLDER_A);
        _permissionHelper.addMemberToRole(_listUser, "Folder Administrator", PermissionsHelper.MemberType.user);

        _permissionHelper = new ApiPermissionsHelper(getProjectName());
        _permissionHelper.addMemberToRole(_listUser, "Reader", PermissionsHelper.MemberType.user);
    }

    private void createListWithData(String name, Map rowData) throws IOException, CommandException
    {
        var connection = createDefaultConnection();
        var listDef = new IntListDefinition(name, "RowId").setFields(List.of(
                new FieldDefinition("Shape", FieldDefinition.ColumnType.String),
                new FieldDefinition("Count", FieldDefinition.ColumnType.Integer)));
        var dataGenerator = listDef.create(connection, getCurrentContainerPath());
        dataGenerator.insertRows(connection, List.of(rowData));
    }

    /*
        Test coverage for
        Issue 47289: Export List Archive if the user is an Admin of the folders of the selected Lists, else throw Permission error
     */
    @Test
    public void testExportListArchive()
    {
        goToProjectHome(LIST_FOLDER_A);
        impersonate(_listUser);
        ManageListsGrid listsGrid = goToManageLists().getGrid();
        listsGrid.setContainerFilter(DataRegionTable.ContainerFilterType.ALL_FOLDERS);
        listsGrid.setFilter("Container", "Equals One Of (example usage: a;b;c)", LIST_FOLDER_A + ";" + getProjectName());
        listsGrid.checkAllOnPage();
        listsGrid.clickHeaderButton("Export List Archive");
        waitForElement(Locators.labkeyErrorHeading);
        Assert.assertEquals("Incorrect permission error message",
                "You do not have the permission to export List '" + LIST_B + "' from Folder '/" + getProjectName() + "'.",
                Locators.labkeyErrorSubHeading.findElement(getDriver()).getText());
        stopImpersonating();

        goToProjectHome();
        listsGrid = goToManageLists().getGrid();
        listsGrid.setContainerFilter(DataRegionTable.ContainerFilterType.ALL_FOLDERS);
        listsGrid.setFilter("Name", "Equals One Of (example usage: a;b;c)", LIST_A + ";" + LIST_B);
        listsGrid.checkAllOnPage();
        listsGrid.clickHeaderButton("Export List Archive");
        Assert.assertEquals("Invalid error message", "'" + LIST_A + "' is already selected, please select Lists with unique names to Export.",
                Locator.tagWithClass("div", "labkey-error").findElement(getDriver()).getText());
        goBack();

        listsGrid = new ManageListsGrid(getDriver());
        listsGrid.setFilter("Container", "Equals One Of (example usage: a;b;c)", LIST_FOLDER_A + ";" + getProjectName());
        listsGrid.checkAllOnPage();
        File listExport = listsGrid.exportSelectedLists();
        Assert.assertTrue("Empty export file downloaded", listExport.length() > 0);
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _containerHelper.deleteProject(LIST_FOLDER_A, afterTest);
    }
}
