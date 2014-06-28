package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

@Category({DailyA.class, FileBrowser.class})
public class FileContentActionButtonsTest extends BaseWebDriverTest
{
    @BeforeClass
    public static void doSetup() throws Exception
    {
        FileContentActionButtonsTest initTest = new FileContentActionButtonsTest();
        initTest.doCleanup(false);

        initTest.doSetupSteps();

        currentTest = initTest;
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }

    private void doSetupSteps()
    {
        _containerHelper.createProject(getProjectName(), null);
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Files");
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
        _fileBrowserHelper.waitForFileGridReady();
    }

    @Test
    public void testEditorActions()
    {
        impersonateRoles("Editor");

        _fileBrowserHelper.waitForFileGridReady();
        assertActionsAvailable(
                FileBrowserHelper.BrowserAction.FOLDER_TREE,
                FileBrowserHelper.BrowserAction.UP,
                FileBrowserHelper.BrowserAction.RELOAD,
                FileBrowserHelper.BrowserAction.NEW_FOLDER,
                FileBrowserHelper.BrowserAction.DOWNLOAD,
                FileBrowserHelper.BrowserAction.DELETE,
                FileBrowserHelper.BrowserAction.RENAME,
                FileBrowserHelper.BrowserAction.MOVE,
                FileBrowserHelper.BrowserAction.EDIT_PROPERTIES,
                FileBrowserHelper.BrowserAction.UPLOAD,
                FileBrowserHelper.BrowserAction.IMPORT_DATA,
                FileBrowserHelper.BrowserAction.EMAIL_SETTINGS
        );

        stopImpersonatingRole();
    }

    @Test
    public void testSubmitterReaderActions()
    {
        impersonateRoles("Submitter", "Reader");

        _fileBrowserHelper.waitForFileGridReady();
        assertActionsAvailable(
                FileBrowserHelper.BrowserAction.FOLDER_TREE,
                FileBrowserHelper.BrowserAction.UP,
                FileBrowserHelper.BrowserAction.RELOAD,
                FileBrowserHelper.BrowserAction.NEW_FOLDER,
                FileBrowserHelper.BrowserAction.DOWNLOAD,
                FileBrowserHelper.BrowserAction.EDIT_PROPERTIES,
                FileBrowserHelper.BrowserAction.UPLOAD,
                FileBrowserHelper.BrowserAction.IMPORT_DATA,
                FileBrowserHelper.BrowserAction.EMAIL_SETTINGS
        );

        stopImpersonatingRole();
    }

    @Test
    public void testAuthorActions()
    {
        impersonateRoles("Author");

        _fileBrowserHelper.waitForFileGridReady();
        assertActionsAvailable(
                FileBrowserHelper.BrowserAction.FOLDER_TREE,
                FileBrowserHelper.BrowserAction.UP,
                FileBrowserHelper.BrowserAction.RELOAD,
                FileBrowserHelper.BrowserAction.NEW_FOLDER,
                FileBrowserHelper.BrowserAction.DOWNLOAD,
                FileBrowserHelper.BrowserAction.EDIT_PROPERTIES,
                FileBrowserHelper.BrowserAction.UPLOAD,
                FileBrowserHelper.BrowserAction.IMPORT_DATA,
                FileBrowserHelper.BrowserAction.EMAIL_SETTINGS
        );

        stopImpersonatingRole();
    }

    @Test
    public void testReaderActions()
    {
        impersonateRoles("Reader");

        _fileBrowserHelper.waitForFileGridReady();
        assertActionsAvailable(
                FileBrowserHelper.BrowserAction.FOLDER_TREE,
                FileBrowserHelper.BrowserAction.UP,
                FileBrowserHelper.BrowserAction.RELOAD,
                FileBrowserHelper.BrowserAction.DOWNLOAD,
                FileBrowserHelper.BrowserAction.EMAIL_SETTINGS
        );

        stopImpersonatingRole();
    }

    @Test
    public void testCutomizeToolbar()
    {
        assertDefaultBrowserButtons();

        _fileBrowserHelper.goToConfigureButtonsTab();
        _fileBrowserHelper.removeToolbarButton("refresh");
        click(Locator.xpath("//tr[@data-recordid='parentFolder']/td[2]")); // Unhide text for 'Parent Folder' button
        click(Locator.xpath("//tr[@data-recordid='parentFolder']/td[3]")); // Hide icon for 'Parent Folder' button
        click(Ext4Helper.Locators.ext4Button("submit"));
        waitForElementToDisappear(FileBrowserHelper.BrowserAction.RELOAD.getButtonIconLocator());
        waitForElementToDisappear(FileBrowserHelper.BrowserAction.UP.getButtonIconLocator());
        waitForElement(FileBrowserHelper.BrowserAction.UP.getButtonTextLocator());

        // Verify custom action buttons
        _fileBrowserHelper.goToConfigureButtonsTab();
        _fileBrowserHelper.removeToolbarButton("parentFolder");
        _fileBrowserHelper.addToolbarButton("refresh");
        click(Ext4Helper.Locators.ext4Button("submit"));
        waitForElementToDisappear(FileBrowserHelper.BrowserAction.UP.getButtonTextLocator());
        waitForElementToDisappear(FileBrowserHelper.BrowserAction.UP.getButtonIconLocator());
        waitForElement(FileBrowserHelper.BrowserAction.RELOAD.getButtonIconLocator());
        waitForElement(FileBrowserHelper.BrowserAction.RELOAD.getButtonTextLocator());

        _fileBrowserHelper.goToConfigureButtonsTab();
        clickButton("Reset To Default", 0);
        waitAndClick(Ext4Helper.ext4WindowButton("Confirm Reset", "Yes"));
        _ext4Helper.waitForMaskToDisappear();
        refresh(); // TODO: Reset doesn't update buttons in place
        waitForElement(FileBrowserHelper.BrowserAction.UP.getButtonIconLocator());
        assertDefaultBrowserButtons();
    }

    @LogMethod(quiet = true)
    private void assertDefaultBrowserButtons()
    {
        Collection<FileBrowserHelper.BrowserAction> buttonsWithText = new HashSet<>(Arrays.asList(
                FileBrowserHelper.BrowserAction.UPLOAD,
                FileBrowserHelper.BrowserAction.IMPORT_DATA,
                FileBrowserHelper.BrowserAction.AUDIT_HISTORY,
                FileBrowserHelper.BrowserAction.ADMIN));

        // All icons are present by default
        for (FileBrowserHelper.BrowserAction action : FileBrowserHelper.BrowserAction.values())
        {
            assertElementPresent(action.getButtonIconLocator());
            if (buttonsWithText.contains(action))
                assertElementPresent(action.getButtonTextLocator());
            else
                assertElementNotPresent(action.getButtonTextLocator());
        }
    }

    private void assertActionsAvailable(FileBrowserHelper.BrowserAction... actions)
    {
        assertEquals(Arrays.asList(actions), _fileBrowserHelper.getAvailableBrowserActions());
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "filecontent";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
