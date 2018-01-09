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
package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class FolderManagementPage extends LabKeyPage<FolderManagementPage.ElementCache>
{
    public FolderManagementPage(WebDriver driver)
    {
        super(driver);
    }

    public static FolderManagementPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static FolderManagementPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("admin", containerPath, "manageFolders"));
        return new FolderManagementPage(driver.getDriver());
    }

    public FolderManagementPage goToFolderTreeTab()
    {
        selectTab("folderTree");
        return this;
    }

    public FileRootsManagementPage goToFilesTab()
    {
        selectTab("files");
        return new FileRootsManagementPage(getDriver());
    }

    /**
     * @deprecated Renamed {@link #goToFilesTab()}
     */
    @Deprecated
    public FileRootsManagementPage goToFilesPane()
    {
        return goToFilesTab();
    }

    public FolderTypePage goToFolderTypeTab()
    {
        selectTab("folderType");
        return new FolderTypePage(getDriver());
    }

    public FolderManagementPage goToMissingValuesTab()
    {
        selectTab("mvIndicators");
        return this;
    }

    public FolderManagementPage goToModulePropertiesTab()
    {
        selectTab("props");
        return this;
    }

    public void assertModuleEnabled(String moduleName)
    {
        goToFolderTypeTab();
        assertElementPresent(Locator.xpath("//input[@type='checkbox' and @checked and @disabled and @title='" + moduleName + "']"));
    }

    public FolderManagementPage goToExportTab()
    {
        selectTab("export");
        return this;
    }

    public FolderManagementPage goToImportTab()
    {
        selectTab("import");
        return this;
    }

    /**
     * @deprecated Use {@link #selectTab(String)} or {@link #selectTab(Class)}
     */
    @Deprecated
    public void goToPane(String tabId)
    {
        if (tabId.startsWith("tab"))
            tabId = tabId.substring(3);
        selectTab(tabId);
    }

// Wave of the future:

    public <T extends FolderManagementTab> T selectTab(Class<T> tabClass) throws InstantiationException, IllegalAccessException
    {
        T tab = tabClass.newInstance();
        tab.setDriver(getDriver());
        String tabId = tab.getTabId();

        selectTab(tabId);
        return tab;
    }

    public void selectTab(String tabId)
    {
        if (!tabId.equals(getCurrentTabIdFromUrl()))
            clickAndWait(elementCache().findTabLink(tabId));
        assertEquals("On wrong folder management tab", tabId, getCurrentTabIdFromUrl());
    }

    /**
     * This isn't totally cosmetic. Url parameter is mostly cosmetic.
     *
     */
    private String getCurrentTabIdFromUrl()
    {
        return getUrlParam("tabId");
    }

    public ReorderFoldersPage clickChangeDisplayOrder()
    {
        beginAt(WebTestHelper.buildRelativeUrl("admin", getCurrentContainerPath(), "reorderFolders"));
        return new ReorderFoldersPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        private final Map<String, WebElement> tabs = new HashMap<>();
        private final Map<String, WebElement> tabLinks = new HashMap<>();

        public ElementCache()
        {
            findTab("folderTree");
        }

        public WebElement findTab(String tabId)
        {
            if (!tabs.containsKey(tabId))
                tabs.put(tabId, Locators.folderManagementTab(tabId).waitForElement(this, WAIT_FOR_JAVASCRIPT));
            return tabs.get(tabId);
        }

        public WebElement findTabLink(String tabId)
        {
            if (!tabLinks.containsKey(tabId))
                tabLinks.put(tabId, Locators.folderManagementTabLink(tabId).findElement(this));
            return tabLinks.get(tabId);
        }
    }

    private static class Locators
    {
        private static Locator.XPathLocator folderManagementTab(String tabId)
        {
            return Locator.id("tab" + tabId);
        }

        private static Locator.XPathLocator folderManagementTabLink(String tabId)
        {
            return folderManagementTab(tabId).childTag("a");
        }
    }
}