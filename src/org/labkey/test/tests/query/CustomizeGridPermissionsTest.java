/*
 * Copyright (c) 2017-2026 LabKey Corporation
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
package org.labkey.test.tests.query;

import org.apache.hc.core5.http.HttpStatus;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteQueryViewCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.RenameQueryViewCommand;
import org.labkey.remoteapi.query.SaveQueryViewsCommand;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.list.ManageListsGrid;
import org.labkey.test.pages.list.BeginPage;
import org.labkey.test.pages.list.GridPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.PermissionsHelper.EDITOR_ROLE;
import static org.labkey.test.util.PermissionsHelper.MemberType;
import static org.labkey.test.util.PermissionsHelper.READER_ROLE;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 7)
public class CustomizeGridPermissionsTest extends BaseWebDriverTest
{
    private static final String READER = "gp_reader@gridpermissions.test";
    private static final String EDITOR = "gp_editor@gridpermissions.test";
    private static final String VIEW_EDITOR = "gp_view_editor@gridpermissions.test";
    private static final File LIST_ARCHIVE = TestFileUtils.getSampleData("lists/ListDemo.lists.zip");
    private static final String LIST_NAME = "NIMHDemographics";

    private static final String VIEW_NAME = "My View";
    private static final String COLUMN_NAME = "container";
    private static final String COLUMN_LABEL = "Folder";

    private static final String SHARED_VIEW_EDITOR_ROLE = "Shared View Editor";

    private static final String SUBFOLDER_NAME = "Child";
    private static final String SUB_VIEW_EDITOR = "gp_sub_view_editor@gridpermissions.test";
    private static final String SHARED_VIEW_NAME = "Shared Grid View";
    private static final String LOCAL_VIEW_NAME = "Subfolder Grid View";
    private static final String RENAMED_VIEW_NAME = "Renamed Grid View";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        _userHelper.deleteUsers(afterTest, READER, EDITOR, VIEW_EDITOR, SUB_VIEW_EDITOR);
    }

    @BeforeClass
    public static void setupProject()
    {
        CustomizeGridPermissionsTest init = getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        new PortalHelper(this).addWebPart("Lists");

        _userHelper.createUser(READER);
        _userHelper.createUser(EDITOR);
        _userHelper.createUser(VIEW_EDITOR);
        _userHelper.createUser(SUB_VIEW_EDITOR);

        ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);
        permissionsHelper.addMemberToRole(READER, READER_ROLE, MemberType.user, getProjectName());
        permissionsHelper.addMemberToRole(EDITOR, EDITOR_ROLE, MemberType.user, getProjectName());
        permissionsHelper.addMemberToRole(VIEW_EDITOR, SHARED_VIEW_EDITOR_ROLE, MemberType.user, getProjectName());

        _containerHelper.createSubfolder(getProjectName(), SUBFOLDER_NAME);
        permissionsHelper.addMemberToRole(READER, READER_ROLE, MemberType.user, getSubfolderPath());
        permissionsHelper.addMemberToRole(SUB_VIEW_EDITOR, SHARED_VIEW_EDITOR_ROLE, MemberType.user, getSubfolderPath());
    }

    @Before
    public void preTest() throws Exception
    {
        recreateList();
    }

    private void recreateList()
    {
        final ManageListsGrid manageListsGrid = BeginPage.beginAt(this, getProjectName()).getGrid();
        manageListsGrid.setContainerFilter(DataRegionTable.ContainerFilterType.CURRENT_FOLDER);

        if (manageListsGrid.getDataRowCount() > 0)
        {
            manageListsGrid.checkAllOnPage();
            manageListsGrid.deleteSelectedLists();
        }

        manageListsGrid.clickImportArchive()
                .setZipFile(LIST_ARCHIVE)
                .clickImport();
    }

    private DataRegionTable goToList()
    {
        return GridPage.beginAt(this, getProjectName(), LIST_NAME).getGrid();
    }

    @Test
    public void testReaderDefaultCustomGrid() throws Exception
    {
        impersonate(READER);
        {
            DataRegionTable list = goToList();
            assertFalse("Folder columns shouldn't be visible before test", list.getColumnLabels().contains(COLUMN_LABEL));
            final CustomizeView customizeView = list.openCustomizeGrid();
            customizeView.addColumn(COLUMN_NAME);
            final CustomizeView.SaveWindow saveWindow = customizeView.clickSave();
            assertFalse("Share view checkbox should be disabled for Reader", saveWindow.shareCheckbox.isEnabled());
            saveWindow.save();
            assertTrue("Failed to add Folder column", list.getColumnLabels().contains(COLUMN_LABEL));
        }
        stopImpersonating();

        final DataRegionTable list = goToList();
        assertFalse("Reader's customized view shouldn't be visible to other users", list.getColumnLabels().contains(COLUMN_LABEL));
    }

    @Test
    public void testEditorDefaultCustomGrid() throws Exception
    {
        impersonate(EDITOR);
        {
            DataRegionTable list = goToList();
            assertFalse("Folder columns shouldn't be visible before test", list.getColumnLabels().contains(COLUMN_LABEL));
            final CustomizeView customizeView = list.openCustomizeGrid();
            customizeView.addColumn(COLUMN_NAME);
            final CustomizeView.SaveWindow saveWindow = customizeView.clickSave();
            assertTrue("Share view checkbox should be enabled for Editor", saveWindow.shareCheckbox.isEnabled());
            saveWindow.shareCheckbox.check();
            saveWindow.save();
            assertTrue("Failed to add Folder column", list.getColumnLabels().contains(COLUMN_LABEL));
        }
        stopImpersonating();

        final DataRegionTable list = goToList();
        assertTrue("View Editor's shared view should be visible to other users", list.getColumnLabels().contains(COLUMN_LABEL));
    }

    @Test
    public void testViewEditorDefaultCustomGrid() throws Exception
    {
        impersonate(VIEW_EDITOR);
        {
            DataRegionTable list = goToList();
            assertFalse("Folder columns shouldn't be visible before test", list.getColumnLabels().contains(COLUMN_LABEL));
            final CustomizeView customizeView = list.openCustomizeGrid();
            customizeView.addColumn(COLUMN_NAME);
            final CustomizeView.SaveWindow saveWindow = customizeView.clickSave();
            assertTrue("Share view checkbox should be enabled for View Editor", saveWindow.shareCheckbox.isEnabled());
            saveWindow.shareCheckbox.check();
            saveWindow.save();
            assertTrue("Failed to add Folder column", list.getColumnLabels().contains(COLUMN_LABEL));
            pushLocation();
        }
        stopImpersonating();

        final DataRegionTable list = goToList();
        assertTrue("View Editor's customized view should be visible to other users", list.getColumnLabels().contains(COLUMN_LABEL));
    }

    @Test
    public void testReaderNamedCustomGrid() throws Exception
    {
        impersonate(READER);
        {
            DataRegionTable list = goToList();
            assertFalse("Folder columns shouldn't be visible before test", list.getColumnLabels().contains(COLUMN_LABEL));
            final CustomizeView customizeView = list.openCustomizeGrid();
            customizeView.addColumn(COLUMN_NAME);
            final CustomizeView.SaveWindow saveWindow = customizeView.clickSave();
            assertFalse("Share view checkbox should be disabled for Reader", saveWindow.shareCheckbox.isEnabled());
            saveWindow.setName(VIEW_NAME);
            saveWindow.save();
            assertTrue("Failed to add Folder column", list.getColumnLabels().contains(COLUMN_LABEL));
            pushLocation();
        }
        stopImpersonating();

        popLocation();
        final DataRegionTable list = new DataRegionTable("query", this);
        assertFalse("Reader's customized view shouldn't be visible to other users", list.getColumnLabels().contains(COLUMN_LABEL));
    }

    @Test
    public void testEditorNamedCustomGrid() throws Exception
    {
        impersonate(EDITOR);
        {
            DataRegionTable list = goToList();
            assertFalse("Folder columns shouldn't be visible before test", list.getColumnLabels().contains(COLUMN_LABEL));
            final CustomizeView customizeView = list.openCustomizeGrid();
            customizeView.addColumn(COLUMN_NAME);
            final CustomizeView.SaveWindow saveWindow = customizeView.clickSave();
            assertTrue("Share view checkbox should be enabled for Editor", saveWindow.shareCheckbox.isEnabled());
            saveWindow.shareCheckbox.check();
            saveWindow.setName(VIEW_NAME);
            saveWindow.save();
            assertTrue("Failed to add Folder column", list.getColumnLabels().contains(COLUMN_LABEL));
            pushLocation();
        }
        stopImpersonating();

        popLocation();
        final DataRegionTable list = new DataRegionTable("query", this);
        assertTrue("View Editor's shared view should be visible to other users", list.getColumnLabels().contains(COLUMN_LABEL));
    }

    @Test
    public void testViewEditorNamedCustomGrid()
    {
        impersonate(VIEW_EDITOR);
        {
            DataRegionTable list = goToList();
            assertFalse("Folder columns shouldn't be visible before test", list.getColumnLabels().contains(COLUMN_LABEL));
            final CustomizeView customizeView = list.openCustomizeGrid();
            customizeView.addColumn(COLUMN_NAME);
            final CustomizeView.SaveWindow saveWindow = customizeView.clickSave();
            assertTrue("Share view checkbox should be enabled for View Editor", saveWindow.shareCheckbox.isEnabled());
            saveWindow.shareCheckbox.check();
            saveWindow.setName(VIEW_NAME);
            saveWindow.save();
            assertTrue("Failed to add Folder column", list.getColumnLabels().contains(COLUMN_LABEL));
            pushLocation();
        }
        stopImpersonating();

        popLocation();
        final DataRegionTable list = new DataRegionTable("query", this);
        assertTrue("View Editor's customized view should be visible to other users", list.getColumnLabels().contains(COLUMN_LABEL));
    }

    @Test // GitHub Issue #1397
    public void testReaderCannotRenameSharedView() throws Exception
    {
        createSharedView(getProjectName(), SHARED_VIEW_NAME, false);

        assertEquals("Reader should not be able to rename a shared view", HttpStatus.SC_FORBIDDEN,
                executeAs(READER, renameCommand(SHARED_VIEW_NAME, RENAMED_VIEW_NAME), getProjectName()));

        assertViewPresent(getProjectName(), SHARED_VIEW_NAME);
        assertViewAbsent(getProjectName(), RENAMED_VIEW_NAME);
    }

    @Test // GitHub Issue #1397
    public void testReaderCannotRenameInheritedViewFromSubfolder() throws Exception
    {
        createSharedView(getProjectName(), SHARED_VIEW_NAME, true);

        assertEquals("Reader should not be able to rename a view inherited from the project", HttpStatus.SC_FORBIDDEN,
                executeAs(READER, renameCommand(SHARED_VIEW_NAME, RENAMED_VIEW_NAME), getSubfolderPath()));

        assertViewPresent(getProjectName(), SHARED_VIEW_NAME);
        assertViewAbsent(getProjectName(), RENAMED_VIEW_NAME);
    }

    @Test // GitHub Issue #1397
    public void testSharedViewEditorCannotRenameInheritedView() throws Exception
    {
        createSharedView(getProjectName(), SHARED_VIEW_NAME, true);

        assertEquals("Subfolder's shared view editor should not be able to rename the project's view", HttpStatus.SC_FORBIDDEN,
                executeAs(SUB_VIEW_EDITOR, renameCommand(SHARED_VIEW_NAME, RENAMED_VIEW_NAME), getSubfolderPath()));

        assertViewPresent(getProjectName(), SHARED_VIEW_NAME);
        assertViewAbsent(getProjectName(), RENAMED_VIEW_NAME);
    }

    @Test // GitHub Issue #1397
    public void testSharedViewEditorCanRenameOwnFolderSharedView() throws Exception
    {
        assertEquals("Shared view editor should be able to share a view in their own folder", HttpStatus.SC_OK,
                executeAs(SUB_VIEW_EDITOR, saveSharedViewCommand(LOCAL_VIEW_NAME, false), getSubfolderPath()));
        assertEquals("Shared view editor should be able to rename a shared view in their own folder", HttpStatus.SC_OK,
                executeAs(SUB_VIEW_EDITOR, renameCommand(LOCAL_VIEW_NAME, RENAMED_VIEW_NAME), getSubfolderPath()));

        assertViewPresent(getSubfolderPath(), RENAMED_VIEW_NAME);
        assertViewAbsent(getSubfolderPath(), LOCAL_VIEW_NAME);
    }

    @Test // GitHub Issue #1397
    public void testSharedViewEditorCannotDeleteInheritedView() throws Exception
    {
        createSharedView(getProjectName(), SHARED_VIEW_NAME, true);

        assertEquals("Subfolder's shared view editor should not be able to delete the project's view", HttpStatus.SC_FORBIDDEN,
                executeAs(SUB_VIEW_EDITOR, new DeleteQueryViewCommand("lists", LIST_NAME, SHARED_VIEW_NAME), getSubfolderPath()));

        assertViewPresent(getProjectName(), SHARED_VIEW_NAME);
    }

    @Test // GitHub Issue #1397
    public void testReaderCannotDeleteSharedView() throws Exception
    {
        createSharedView(getProjectName(), SHARED_VIEW_NAME, false);

        assertEquals("Reader should not be able to delete a shared view", HttpStatus.SC_FORBIDDEN,
                executeAs(READER, new DeleteQueryViewCommand("lists", LIST_NAME, SHARED_VIEW_NAME), getProjectName()));

        assertViewPresent(getProjectName(), SHARED_VIEW_NAME);
    }

    private String getSubfolderPath()
    {
        return getProjectName() + "/" + SUBFOLDER_NAME;
    }

    private SaveQueryViewsCommand saveSharedViewCommand(String viewName, boolean inherit)
    {
        return new SaveQueryViewsCommand("lists", LIST_NAME).addView(viewName, List.of(COLUMN_NAME), true, inherit);
    }

    private RenameQueryViewCommand renameCommand(String viewName, String newName)
    {
        return new RenameQueryViewCommand("lists", LIST_NAME, viewName, newName);
    }

    private void createSharedView(String containerPath, String viewName, boolean inherit) throws Exception
    {
        saveSharedViewCommand(viewName, inherit).execute(createDefaultConnection(), containerPath);
    }

    /** Executes the command while impersonating the given user, returning the HTTP status code rather than throwing. */
    private int executeAs(String user, Command<?> command, String containerPath) throws Exception
    {
        Connection connection = createDefaultConnection();
        connection.impersonate(user);
        try
        {
            return command.execute(connection, containerPath).getStatusCode();
        }
        catch (CommandException e)
        {
            return e.getStatusCode();
        }
        finally
        {
            connection.stopImpersonating();
        }
    }

    // query.CustomViews only lists views owned by the container, so a row here proves where the view lives
    private List<String> getCustomViewNames(String containerPath) throws Exception
    {
        SelectRowsCommand command = new SelectRowsCommand("query", "CustomViews");
        command.setColumns(List.of("Name"));
        command.addFilter("QueryName", LIST_NAME, Filter.Operator.EQUAL);

        return command.execute(createDefaultConnection(), containerPath).getRows().stream()
                .map(row -> (String) row.get("Name"))
                .toList();
    }

    private void assertViewPresent(String containerPath, String viewName) throws Exception
    {
        List<String> viewNames = getCustomViewNames(containerPath);
        assertTrue(String.format("Folder '%s' should own a view named '%s', found: %s", containerPath, viewName, viewNames),
                viewNames.contains(viewName));
    }

    private void assertViewAbsent(String containerPath, String viewName) throws Exception
    {
        List<String> viewNames = getCustomViewNames(containerPath);
        assertFalse(String.format("Folder '%s' should not own a view named '%s'", containerPath, viewName),
                viewNames.contains(viewName));
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "CustomizeGridPermissionsTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("list", "api", "query");
    }
}
