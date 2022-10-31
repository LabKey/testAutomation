/*
 * Copyright (c) 2017-2019 LabKey Corporation
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

import static org.junit.Assert.assertTrue;

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

    public RConfigurationPage goToRConfigTab()
    {
        selectTab("rConfig");
        return new RConfigurationPage(getDriver());
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

    public ExportFolderPage goToExportTab()
    {
        selectTab("export");
        return new ExportFolderPage(getDriver());
    }

    public ImportFolderPage goToImportTab()
    {
        return selectTab(ImportFolderPage.class);
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

    public <T extends FolderManagementTab> T selectTab(Class<T> tabClass)
    {
        T tab;
        try
        {
            tab = tabClass.newInstance();
        }
        catch (InstantiationException | IllegalAccessException e)
        {
            throw new RuntimeException("Unable to instantiate page class: " + tabClass.getName(), e);
        }
        tab.setDriver(getDriver());
        String tabId = tab.getTabId();

        selectTab(tabId);
        return tab;
    }

    public void selectTab(String tabId)
    {
        if (!isActiveTab(tabId))
        {
            clickAndWait(elementCache().findTabLink(tabId));
            clearCache();
        }
        assertTrue("On wrong folder management tab - expected " + tabId, isActiveTab(tabId));
    }

    /**
     * Is the &lt;li&gt; element associated with this tabId marked as active?
     */
    private boolean isActiveTab(String tabId)
    {
        WebElement element = elementCache().findTab(tabId);
        return "labkey-tab-active".equals(element.getAttribute("class"));
    }

    public ReorderFoldersPage clickChangeDisplayOrder()
    {
        beginAt(WebTestHelper.buildRelativeUrl("admin", getCurrentContainerPath(), "reorderFolders"));
        return new ReorderFoldersPage(getDriver());
    }

    public RenameFolderPage clickFolderRename()
    {
        beginAt(WebTestHelper.buildRelativeUrl("admin", getCurrentContainerPath(), "renameFolder"));
        return new RenameFolderPage(getDriver());
    }

    @Override
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
