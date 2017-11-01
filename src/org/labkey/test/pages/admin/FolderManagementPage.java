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
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

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

    @Override
    protected void waitForPage()
    {
        waitFor(()-> Locators.folderTreeTab.findElementOrNull(getDriver()) != null, WAIT_FOR_PAGE);
    }

    public FolderManagementPage goToFolderTreePane()
    {
        scrollIntoView(elementCache().folderTreeTabLink);
        elementCache().folderTreeTabLink.click();
        waitFor(() -> getURL().toString().endsWith("?tabId=folderTree")
                        && elementCache().isTabActive(Locators.folderTreeTab),
                "Could not navigate to Folder Tree pane", 4000);
        return this;
    }

    public FileRootsManagementPage goToFilesPane()
    {
        scrollIntoView(elementCache().filesTabLink);
        elementCache().filesTabLink.click();
        waitFor(() -> getURL().toString().endsWith("?tabId=files")
                        && elementCache().isTabActive(Locators.filesTab),
                "Could not navigate to Files pane", 4000);
        return new FileRootsManagementPage(getDriver());
    }

    public FolderTypePage goToFolderTypePane()
    {
        elementCache().folderTypeTabLink.click();
        waitFor(()-> getURL().toString().endsWith("?tabId=folderType")
                && elementCache().isTabActive(Locators.folderTypeTab), 4000);
        return new FolderTypePage(getDriver());
    }

    public FolderManagementPage goToMissingValuesPane()
    {
        elementCache().missingValuesLink.click();
        waitFor(()-> getURL().toString().endsWith("?tabId=mvIndicators")
                && elementCache().isTabActive(Locators.missingValuesTab), 4000 );
        return this;
    }

    public FolderManagementPage goToModulePropertiesPane()
    {
        elementCache().modulePropertiesTabLink.click();
        waitFor(()-> getURL().toString().endsWith("?tabId=props")
                && elementCache().isTabActive(Locators.modulePropertiesTab), 4000);
        return this;
    }

    public void assertModuleEnabled(String moduleName)
    {
        goToFolderTypePane();
        assertElementPresent(Locator.xpath("//input[@type='checkbox' and @checked and @disabled and @title='" + moduleName + "']"));
    }

    public FolderManagementPage goToExportPane()
    {
        elementCache().exportTabLink.click();
        waitFor(()-> getURL().toString().endsWith("?tabId=export")
                && elementCache().isTabActive(Locators.exportTab), 4000);
        return this;
    }

    /* activates the 'import' pane */
    public FolderManagementPage goToImportPane()
    {
        elementCache().importTabLink.click();
        waitFor(()-> getURL().toString().endsWith("?tabId=import")
                && elementCache().isTabActive(Locators.importTab), 4000);
        return this;
    }

    public void goToPane(String tabId)
    {
        Locator.IdLocator tabLoc = Locator.id(tabId);
        tabLoc.childTag("a").findElement(getDriver()).click();
        waitFor(()-> getURL().toString().endsWith("?tabId=" + tabId)
                && elementCache().isTabActive(tabLoc), 4000);
    }

// Wave of the future:

    public <T extends FolderManagementTab> T goToPane(Class<T> tabClass) throws InstantiationException, IllegalAccessException
    {
        T tab = tabClass.newInstance();
        tab.setDriver(getDriver());
        String tabId = tab.getTabId();

        Locator.IdLocator tabLoc = Locator.id(tabId);
        tabLoc.childTag("a").findElement(getDriver()).click();
        waitFor(()-> getURL().toString().endsWith("?tabId=" + tabId)
                && elementCache().isTabActive(tabLoc), 4000);
        return tab;
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
        // TODO: Add other elements that are on the page
        WebElement folderTreeTabLink = Locators.folderTreeTabLink.refindWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement folderTypeTabLink = Locators.folderTypeTabLink.refindWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement filesTabLink = Locators.filesTabLink.refindWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement missingValuesLink = Locators.missingValuesTabLink.refindWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement modulePropertiesTabLink = Locators.modulePropertiesTabLink.refindWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement exportTabLink = Locators.exportTabLink.refindWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement importTabLink = Locators.importTabLink.refindWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement saveButton = new LazyWebElement(Locator.lkButton("Save"),this);


        public boolean isTabActive(Locator loc)
        {
            WebElement tab = loc.findElementOrNull(this);
            return tab != null && tab.getAttribute("class").equalsIgnoreCase("labkey-tab-active");
        }
    }

    public static class Locators
    {
        public static Locator.XPathLocator folderTreeTab = Locator.id("tabfolderTree");
        public static Locator.XPathLocator folderTreeTabLink = folderTreeTab.child("a");

        public static Locator.XPathLocator folderTypeTab = Locator.id("tabfolderType");
        public static Locator.XPathLocator folderTypeTabLink = folderTypeTab.child("a");

        public static Locator.XPathLocator filesTab = Locator.id("tabfiles");
        public static Locator.XPathLocator filesTabLink = filesTab.child("a");

        public static Locator.XPathLocator missingValuesTab = Locator.id("tabmvIndicators");
        public static Locator.XPathLocator missingValuesTabLink = missingValuesTab.child("a");

        public static Locator.XPathLocator modulePropertiesTab = Locator.id("tabprops");
        public static Locator.XPathLocator modulePropertiesTabLink = modulePropertiesTab.child("a");

        public static Locator.XPathLocator conceptsTab = Locator.id("tabconcepts");
        public static Locator.XPathLocator conceptsTabLink = conceptsTab.child("a");

        public static Locator.XPathLocator exportTab = Locator.id("tabexport");
        public static Locator.XPathLocator exportTabLink = exportTab.child("a");

        public static Locator.XPathLocator importTab = Locator.id("tabimport");
        public static Locator.XPathLocator importTabLink = importTab.child("a");
    }
}