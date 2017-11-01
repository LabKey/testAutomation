/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.FileBrowserHelper.BrowserAction;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({DailyA.class, FileBrowser.class})
public class FileContentActionButtonsTest extends BaseWebDriverTest
{
    @BeforeClass
    public static void doSetup() throws Exception
    {
        FileContentActionButtonsTest initTest = (FileContentActionButtonsTest)getCurrentTest();

        initTest.doSetupSteps();
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

        // ensure that the file browser is in the default state
        resetToDefaultToolbar();
    }

    @Test
    public void testDefaultActions()
    {
        Collection<BrowserAction> buttonsWithText = new HashSet<>(Arrays.asList(
                BrowserAction.UPLOAD,
                BrowserAction.IMPORT_DATA,
                BrowserAction.AUDIT_HISTORY,
                BrowserAction.ADMIN)
        );

        // All icons are present by default
        for (BrowserAction action : BrowserAction.values())
        {
            assertElementPresent(action.getButtonIconLocator());
            if (buttonsWithText.contains(action))
                assertElementPresent(action.getButtonTextLocator());
            else
                assertElementNotPresent(action.getButtonTextLocator());
        }
    }

    @Test
    public void testEditorActions()
    {
        impersonateRoles("Editor");

        assertActionsAvailable(
                BrowserAction.FOLDER_TREE,
                BrowserAction.UP,
                BrowserAction.RELOAD,
                BrowserAction.NEW_FOLDER,
                BrowserAction.DOWNLOAD,
                BrowserAction.DELETE,
                BrowserAction.RENAME,
                BrowserAction.MOVE,
                BrowserAction.EDIT_PROPERTIES,
                BrowserAction.UPLOAD,
                BrowserAction.IMPORT_DATA,
                BrowserAction.EMAIL_SETTINGS
        );

        stopImpersonatingRole();
    }

    @Test
    public void testSubmitterReaderActions()
    {
        impersonateRoles("Submitter", "Reader");

        assertActionsAvailable(
                BrowserAction.FOLDER_TREE,
                BrowserAction.UP,
                BrowserAction.RELOAD,
                BrowserAction.NEW_FOLDER,
                BrowserAction.DOWNLOAD,
                BrowserAction.EDIT_PROPERTIES,
                BrowserAction.UPLOAD,
                BrowserAction.IMPORT_DATA,
                BrowserAction.EMAIL_SETTINGS
        );

        stopImpersonatingRole();
    }

    @Test
    public void testAuthorActions()
    {
        impersonateRoles("Author");

        assertActionsAvailable(
                BrowserAction.FOLDER_TREE,
                BrowserAction.UP,
                BrowserAction.RELOAD,
                BrowserAction.NEW_FOLDER,
                BrowserAction.DOWNLOAD,
                BrowserAction.EDIT_PROPERTIES,
                BrowserAction.UPLOAD,
                BrowserAction.IMPORT_DATA,
                BrowserAction.EMAIL_SETTINGS
        );

        stopImpersonatingRole();
    }

    @Test
    public void testReaderActions()
    {
        impersonateRoles("Reader");

        assertActionsAvailable(
                BrowserAction.FOLDER_TREE,
                BrowserAction.UP,
                BrowserAction.RELOAD,
                BrowserAction.DOWNLOAD,
                BrowserAction.EMAIL_SETTINGS
        );

        stopImpersonatingRole();
    }

    @Test
    public void testCustomizeToolbar()
    {
        _fileBrowserHelper.goToConfigureButtonsTab();
        _fileBrowserHelper.removeToolbarButton("refresh");
        click(Locator.xpath("//tr[@data-recordid='download']/td[2]")); // Unhide text for 'Download' button
        click(Locator.xpath("//tr[@data-recordid='download']/td[3]")); // Hide icon for 'Download' button
        click(Ext4Helper.Locators.ext4Button("submit"));
        waitForElementToDisappear(BrowserAction.RELOAD.getButtonIconLocator());
        waitForElementToDisappear(BrowserAction.DOWNLOAD.getButtonIconLocator());
        waitForElement(BrowserAction.DOWNLOAD.getButtonTextLocator());

        // Verify custom action buttons
        _fileBrowserHelper.goToConfigureButtonsTab();
        _fileBrowserHelper.removeToolbarButton("download");
        _fileBrowserHelper.addToolbarButton("refresh");
        click(Ext4Helper.Locators.ext4Button("submit"));
        waitForElementToDisappear(BrowserAction.DOWNLOAD.getButtonTextLocator());
        waitForElementToDisappear(BrowserAction.DOWNLOAD.getButtonIconLocator());
        waitForElement(BrowserAction.RELOAD.getButtonIconLocator());
        waitForElement(BrowserAction.RELOAD.getButtonTextLocator());

        resetToDefaultToolbar();
        waitForElement(BrowserAction.UP.getButtonIconLocator());
    }

    private void assertActionsAvailable(BrowserAction... actions)
    {
        assertEquals(Arrays.asList(actions), _fileBrowserHelper.getAvailableBrowserActions());
    }

    private void resetToDefaultToolbar()
    {
        _fileBrowserHelper.goToConfigureButtonsTab();
        clickButton("Reset to Default", 0);
        waitAndClick(Ext4Helper.Locators.windowButton("Confirm Reset", "Yes"));
        _ext4Helper.waitForMaskToDisappear();
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("filecontent");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
