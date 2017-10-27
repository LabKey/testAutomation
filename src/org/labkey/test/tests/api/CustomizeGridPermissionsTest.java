/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.test.tests.api;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyC;
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.PermissionsHelper.MemberType;

@Category({DailyC.class})
public class CustomizeGridPermissionsTest extends BaseWebDriverTest
{
    private static final String READER = "gp_reader@gridpermissions.test";
    private static final String EDITOR = "gp_editor@gridpermissions.test";
    private static final String VIEW_EDITOR = "gp_view_editor@gridpermissions.test";
    private static final File LIST_ARCHIVE = TestFileUtils.getSampleData("lists/ListDemo.lists.zip");
    private static final String LIST_NAME = "NIMHDemographics";

    private static final String VIEW_NAME = "My View";
    private static final String COLUMN_NAME = "Container";
    private static final String COLUMN_LABEL = "Folder";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        _userHelper.deleteUsers(afterTest, READER, EDITOR, VIEW_EDITOR);
    }

    @BeforeClass
    public static void setupProject()
    {
        CustomizeGridPermissionsTest init = (CustomizeGridPermissionsTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        new PortalHelper(this).addWebPart("Lists");

        _userHelper.createUser(READER);
        _userHelper.createUser(EDITOR);
        _userHelper.createUser(VIEW_EDITOR);

        ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);
        permissionsHelper.addMemberToRole(READER, "Reader", MemberType.user, getProjectName());
        permissionsHelper.addMemberToRole(EDITOR, "Editor", MemberType.user, getProjectName());
        permissionsHelper.addMemberToRole(VIEW_EDITOR, "Shared View Editor", MemberType.user, getProjectName());
    }

    @Before
    public void preTest() throws Exception
    {
        recreateList();
    }

    private void recreateList() throws Exception
    {
        final ManageListsGrid manageListsGrid = BeginPage.beginAt(this, getProjectName()).getGrid();
        if (manageListsGrid.getDataRowCount() > 0)
        {
            manageListsGrid.checkAll();
            manageListsGrid.deleteSelectedLists();
        }
        manageListsGrid.
                clickImportArchive().
                setZipFile(LIST_ARCHIVE).
                clickImport();
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
    public void testViewEditorNamedCustomGrid() throws Exception
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
        return Arrays.asList("list", "api");
    }
}