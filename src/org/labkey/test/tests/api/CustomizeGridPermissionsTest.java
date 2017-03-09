package org.labkey.test.tests.api;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyC;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.list.ManageListsGrid;
import org.labkey.test.pages.list.BeginPage;
import org.labkey.test.pages.list.GridPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.PermissionsHelper.MemberType;

@Category({DailyC.class})
public class CustomizeGridPermissionsTest extends BaseWebDriverTest
{
    private static final String READER = "reader@gridpermissions.test";
    private static final String EDITOR = "editor@gridpermissions.test";
    private static final String VIEW_EDITOR = "view_editor@gridpermissions.test";
    private static final File LIST_ARCHIVE = TestFileUtils.getSampleData("lists/ListDemo.lists.zip");
    private static final String LIST_NAME = "NIMHDemographics";

    private static String listId;

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
        String href = Locator.linkWithText(LIST_NAME).findElement(getDriver()).getAttribute("href");
        listId = WebTestHelper.parseUrlQuery(new URL(href)).get("listId");
    }

    private DataRegionTable goToList()
    {
        if (listId != null)
        {
            return GridPage.beginAt(this, getProjectName(), listId).getGrid();
        }
        else
        {
            BeginPage.beginAt(this, getProjectName());
            clickAndWait(Locator.linkWithText(LIST_NAME));
            return new DataRegionTable("query", this);
        }
    }

    @Test
    public void testReaderCustomGrid() throws Exception
    {
        impersonate(READER);
        {
            DataRegionTable list = goToList();
            assertFalse("Folder columns shouldn't be visible before test", list.getColumnLabels().contains("Folder"));
            final CustomizeView customizeView = list.openCustomizeGrid();
            customizeView.addColumn("CONTAINER");
            final CustomizeView.SaveWindow saveWindow = customizeView.clickSave();
            assertFalse("Share view checkbox should be disabled for Reader", saveWindow.shareCheckbox.isEnabled());
            saveWindow.save();
            assertTrue("Failed to add Folder column", list.getColumnLabels().contains("Folder"));
        }
        stopImpersonating();

        final DataRegionTable list = goToList();
        assertFalse("Reader's customized view shouldn't be visible to other users", list.getColumnLabels().contains("Folder"));
    }

    @Test
    public void testEditorCustomGrid() throws Exception
    {
        impersonate(EDITOR);
        {
            DataRegionTable list = goToList();
            final CustomizeView customizeView = list.openCustomizeGrid();
            customizeView.addColumn("CONTAINER");
            final CustomizeView.SaveWindow saveWindow = customizeView.clickSave();
            assertTrue("Share view checkbox should be enabled for Editor", saveWindow.shareCheckbox.isEnabled());
            saveWindow.shareCheckbox.check();
            saveWindow.save();
            assertTrue("Failed to add Folder column", list.getColumnLabels().contains("Folder"));
        }
        stopImpersonating();

        final DataRegionTable list = goToList();
        assertTrue("View Editor's shared view should be visible to other users", list.getColumnLabels().contains("Folder"));
    }

    @Test
    public void testViewEditorCustomGrid() throws Exception
    {
        impersonate(VIEW_EDITOR);
        {
            DataRegionTable list = goToList();
            final CustomizeView customizeView = list.openCustomizeGrid();
            customizeView.addColumn("CONTAINER");
            final CustomizeView.SaveWindow saveWindow = customizeView.clickSave();
            assertTrue("Share view checkbox should be enabled for View Editor", saveWindow.shareCheckbox.isEnabled());
            saveWindow.shareCheckbox.check();
            saveWindow.save();
            assertTrue("Failed to add Folder column", list.getColumnLabels().contains("Folder"));
        }
        stopImpersonating();

        final DataRegionTable list = goToList();
        assertTrue("View Editor's customized view should be visible to other users", list.getColumnLabels().contains("Folder"));
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
        return Arrays.asList();
    }
}