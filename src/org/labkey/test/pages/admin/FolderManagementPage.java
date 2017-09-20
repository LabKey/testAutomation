package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

// TODO: Page classes should contain all functionality for a single page/action
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
        scrollIntoView(newElementCache().folderTreeTabLink);
        newElementCache().folderTreeTabLink.click();
        waitFor(() -> getURL().toString().endsWith("?tabId=folderTree")
                        && newElementCache().isTabActive(Locators.folderTreeTab),
                "Could not navigate to Folder Tree pane", 4000);
        return this;
    }

    public FileRootsManagementPage goToFilesPane()
    {
        scrollIntoView(newElementCache().filesTabLink);
        newElementCache().filesTabLink.click();
        waitFor(() -> getURL().toString().endsWith("?tabId=files")
                        && newElementCache().isTabActive(Locators.filesTab),
                "Could not navigate to Files pane", 4000);
        return new FileRootsManagementPage(getDriver());
    }

    // TODO: Add specific pane component
    public FolderManagementPage goToFolderTypePane()
    {
    //        This is a gross workaround to a silly problem; sometimes clicking the tab fails
    //          because another element (an ext4 mask, invisible, is in the way).
    //
    //            scrollIntoView(newElementCache().folderTypeTabLink);
    //            newElementCache().folderTypeTabLink.click();
    //            waitFor(()-> getURL().toString().endsWith("?tabId=folderType")
    //                    && newElementCache().isTabActive(Locators.folderTypeTab),
    //                    "Could not navigate to Folder Type Pane", 4000 );
    //            return this;
        newElementCache().folderTypeTabLink.click();
        waitFor(()-> getURL().toString().endsWith("?tabId=folderType")
                && newElementCache().isTabActive(Locators.folderTypeTab), 4000);
        return this;
    }

    public FolderManagementPage goToMissingValuesPane()
    {
        newElementCache().missingValuesLink.click();
        waitFor(()-> getURL().toString().endsWith("?tabId=mvIndicators")
                && newElementCache().isTabActive(Locators.missingValuesTab), 4000 );
        return this;
    }

    public FolderManagementPage goToModulePropertiesPane()
    {
        newElementCache().modulePropertiesTabLink.click();
        waitFor(()-> getURL().toString().endsWith("?tabId=props")
                && newElementCache().isTabActive(Locators.modulePropertiesTab), 4000);
        return this;
    }

    public void assertModuleEnabled(String moduleName)
    {
        goToFolderTypePane();
        assertElementPresent(Locator.xpath("//input[@type='checkbox' and @checked and @disabled and @title='" + moduleName + "']"));
    }

    public FolderManagementPage goToExportPane()
    {
        newElementCache().exportTabLink.click();
        waitFor(()-> getURL().toString().endsWith("?tabId=export")
                && newElementCache().isTabActive(Locators.exportTab), 4000);
        return this;
    }

    /* activates the 'import' pane */
    public FolderManagementPage goToImportPane()
    {
        newElementCache().importTabLink.click();
        waitFor(()-> getURL().toString().endsWith("?tabId=import")
                && newElementCache().isTabActive(Locators.importTab), 4000);
        return this;
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
        WebElement saveButton = new LazyWebElement(Locator.xpath(".//a[contains(@class, 'labkey-button')]//span[contains(text(), 'Save')]/ancestor::a"),this);


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