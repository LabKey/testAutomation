package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Hosting;
import org.labkey.test.components.list.ManageListsGrid;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PermissionsHelper;

import java.io.File;
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
    private final static String SHARED_LIST = "Shared list C";
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
    public static void setupProject()
    {
        ListArchiveExportTest initTest = (ListArchiveExportTest) getCurrentTest();
        initTest.doSetUp();
    }

    private void doSetUp()
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

    private void createListWithData(String name, Map rowData)
    {
        log("Creating test list in " + getCurrentContainerPath());
        _listHelper.createList(getCurrentContainerPath(), name, ListHelper.ListColumnType.AutoInteger, "RowId",
                new ListHelper.ListColumn("Shape", "Shape", ListHelper.ListColumnType.String),
                new ListHelper.ListColumn("Count", "Count", ListHelper.ListColumnType.Integer));
        _listHelper.insertNewRow(rowData);
    }

    @Test
    public void testExportListArchive()
    {
        goToProjectHome(LIST_FOLDER_A);
        impersonate(_listUser);
        ManageListsGrid listsGrid = goToManageLists().getGrid();
        listsGrid.setContainerFilter(DataRegionTable.ContainerFilterType.ALL_FOLDERS);
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
        listsGrid.uncheckCheckbox(0);
        File listExport = listsGrid.exportSelectedLists();
        Assert.assertTrue("Empty file downloaded", listExport.length() > 0);
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _containerHelper.deleteProject(LIST_FOLDER_A, afterTest);
    }
}
