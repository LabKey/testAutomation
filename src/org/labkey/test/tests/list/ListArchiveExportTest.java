/*
 * Copyright (c) 2023-2026 LabKey Corporation
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
package org.labkey.test.tests.list;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
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

import static org.labkey.test.util.PermissionsHelper.FOLDER_ADMIN_ROLE;
import static org.labkey.test.util.PermissionsHelper.READER_ROLE;

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
        ListArchiveExportTest initTest = getCurrentTest();
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
        _permissionHelper.addMemberToRole(_listUser, FOLDER_ADMIN_ROLE, PermissionsHelper.MemberType.user);

        _permissionHelper = new ApiPermissionsHelper(getProjectName());
        _permissionHelper.addMemberToRole(_listUser, READER_ROLE, PermissionsHelper.MemberType.user);
    }

    private void createListWithData(String name, Map<String, Object> rowData) throws IOException, CommandException
    {
        var connection = createDefaultConnection();
        // Issue 53672: Use a long key column name
        var listDef = new IntListDefinition(name, "keyA123456789a123456789A123456789a123456789a123456789a123456789").setFields(List.of(
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
        listsGrid.setFilter("Container", "Equals One Of", LIST_FOLDER_A + ";" + getProjectName());
        listsGrid.checkAllOnPage();
        listsGrid.clickHeaderButton("Export List Archive");
        Assert.assertEquals("Invalid error message", "List archive export is only supported for Lists in folders where you are an administrator. Try filtering to select only Lists in the local folder.",
                Locator.tagWithClass("div", "labkey-error").findElement(getDriver()).getText());
        goBack();
        stopImpersonating();

        goToProjectHome();
        listsGrid = goToManageLists().getGrid();
        listsGrid.setContainerFilter(DataRegionTable.ContainerFilterType.ALL_FOLDERS);
        listsGrid.setFilter("Name", "Equals One Of", LIST_A + ";" + LIST_B);
        listsGrid.checkAllOnPage();
        listsGrid.clickHeaderButton("Export List Archive");
        Assert.assertEquals("Invalid error message", "'" + LIST_A + "' is already selected, please select Lists with unique names to Export.",
                Locator.tagWithClass("div", "labkey-error").findElement(getDriver()).getText());
        goBack();

        listsGrid = new ManageListsGrid(getDriver());
        listsGrid.uncheckAllOnPage();
        listsGrid.clearAllFilters();
        listsGrid.setFilter("Container", "Equals One Of", LIST_FOLDER_A + ";" + getProjectName());
        Assert.assertEquals("Incorrect list after container filter", Arrays.asList(LIST_A, LIST_B), listsGrid.getColumnDataAsText("Name"));
        listsGrid.checkAllOnPage();
        File listExport = listsGrid.exportSelectedLists();
        Assert.assertTrue("Empty export file downloaded", listExport.length() > 0);

        // Issue 53672: Delete the lists and reimport, ensuring we have no errors
        listsGrid.deleteSelectedLists();
        listsGrid = goToManageLists().getGrid();
        assertTextNotPresent(LIST_A, LIST_B);
        listsGrid.clickImportArchive().setZipFile(listExport).clickImport();
        goToManageLists().getGrid();
        assertTextPresent(LIST_A, LIST_B);
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _containerHelper.deleteProject(LIST_FOLDER_A, afterTest);
    }
}
