/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.core.login.LoginConfigurePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

// TODO: Missing lots of functionality
public class ShowAdminPage extends LabKeyPage<ShowAdminPage.ElementCache>
{
    public ShowAdminPage(WebDriver driver)
    {
        super(driver);
    }

    public static ShowAdminPage beginAt(WebDriverWrapper driver)
    {
        driver.beginAt(WebTestHelper.buildURL("admin", "showAdmin"));
        return new ShowAdminPage(driver.getDriver());
    }

    public ShowAdminPage goToServerInformationSection()
    {
        elementCache().sectionServerInfo.click();
        return this;
    }

    public ShowAdminPage goToSettingsSection()
    {
        elementCache().sectionSettingsLinks.click();
        return this;
    }

    public ShowAdminPage goToModuleInformationSection()
    {
        elementCache().sectionModuleInfo.click();
        return this;
    }

    public ShowAdminPage goToActiveUsersSection()
    {
        elementCache().sectionActiveUsers.click();
        return this;
    }

    public List<String> getActiveUsers()
    {
        goToActiveUsersSection();
        return getTexts(elementCache().findActiveUsers());
    }

    public String getServerGUID()
    {
        goToServerInformationSection();
        return elementCache().findServerGUID().getText();
    }

    public void clickAnalyticsSettings()
    {
        goToSettingsSection();
        clickAndWait(elementCache().analyticsSettingsLink);
    }

    public void clickExternalRedirectHosts()
    {
        goToSettingsSection();
        clickAndWait(elementCache().externalRedirectHostLink);
        Locator.waitForAnyElement(shortWait(), Locator.tagWithText("span", "Done"), Locator.tagWithText("span", "Save"));
    }

    public ShowAuditLogPage clickAuditLog()
    {
        goToSettingsSection();
        clickAndWait(elementCache().auditLogLink);
        return new ShowAuditLogPage(getDriver());
    }

    public LoginConfigurePage clickAuthentication()
    {
        goToSettingsSection();
        clickAndWait(elementCache().authenticationLink);
        return new LoginConfigurePage(getDriver());
    }

    public void clickConfigurePageElements()
    {
        goToSettingsSection();
        clickAndWait(elementCache().configurePageElements);
        Locator.waitForAnyElement(shortWait(), Locator.tagWithText("span", "Done"), Locator.tagWithText("span", "Save"));
    }

    public void clickEmailCustomization()
    {
        goToSettingsSection();
        clickAndWait(elementCache().emailCustomizationLink);
    }

    public void clickNotificationServiceAdmin()
    {
        goToSettingsSection();
        clickAndWait(elementCache().notificationServiceAdminLink);
    }

    public ConfigureFileSystemAccessPage clickFiles()
    {
        goToSettingsSection();
        clickAndWait(elementCache().filesLink);
        return new ConfigureFileSystemAccessPage(getDriver());
    }

    public void clickFullTextSearch()
    {
        goToSettingsSection();
        clickAndWait(elementCache().fullTextSearchLink);
    }

    public FolderTypePages clickFolderType()
    {
        goToSettingsSection();
        clickAndWait(elementCache().folderTypeLink);
        return new FolderTypePages(getDriver());
    }

    public LookAndFeelSettingsPage clickLookAndFeelSettings()
    {
        goToSettingsSection();
        clickAndWait(elementCache().lookAndFeelSettingsLink);
        return new LookAndFeelSettingsPage(getDriver());
    }

    public void clickMasterPatientIndex()
    {
        goToSettingsSection();
        clickAndWait(elementCache().masterPatientIndex);
    }

    public void clickProfiler()
    {
        goToSettingsSection();
        clickAndWait(elementCache().profilerLink);
    }

    public void clickRunningThreads()
    {
        goToSettingsSection();
        clickAndWait(elementCache().runningThreadsLink);
    }

    public CustomizeSitePage clickSiteSettings()
    {
        goToSettingsSection();
        clickAndWait(elementCache().siteSettingsLink);
        return new CustomizeSitePage(getDriver());
    }

    public void clickSiteWideTerms()
    {
        goToSettingsSection();
        clickAndWait(elementCache().siteWideTermsLink);
    }

    public void clickSystemMaintenance()
    {
        goToSettingsSection();
        clickAndWait(elementCache().systemMaintenanceLink);
    }

    public void clickSystemProperties()
    {
        goToSettingsSection();
        clickAndWait(elementCache().systemPropertiesLink);
    }

    public ConfigureReportsAndScriptsPage clickViewsAndScripting()
    {
        goToSettingsSection();
        clickAndWait(elementCache().viewsAndScriptingLink);
        return new ConfigureReportsAndScriptsPage(this);
    }

    public void clickCredits()
    {
        goToSettingsSection();
        clickAndWait(elementCache().creditsLink);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected WebElement sectionServerInfo = Locator.linkWithText("Server Information").findWhenNeeded(this);
        protected WebElement sectionSettingsLinks = Locator.linkWithText("Settings").findWhenNeeded(this);
        protected WebElement sectionModuleInfo = Locator.linkWithText("Module Information").findWhenNeeded(this);
        protected WebElement sectionActiveUsers = Locator.linkWithText("Active Users").findWhenNeeded(this);

        protected WebElement analyticsSettingsLink = Locator.linkWithText("analytics settings").findWhenNeeded(this);
        protected WebElement externalRedirectHostLink = Locator.linkWithText("External Redirect Hosts").findElement(this);
        protected WebElement auditLogLink = Locator.linkWithText("audit log").findWhenNeeded(this);
        protected WebElement authenticationLink = Locator.linkWithText("authentication").findWhenNeeded(this);
        protected WebElement configurePageElements = Locator.linkWithText("configure page elements").findWhenNeeded(this);
        protected WebElement emailCustomizationLink = Locator.linkWithText("email customization").findWhenNeeded(this);
        protected WebElement notificationServiceAdminLink = Locator.linkWithText("notification service admin").findWhenNeeded(this);
        protected WebElement filesLink = Locator.linkWithText("files").findWhenNeeded(this);
        protected WebElement fullTextSearchLink = Locator.linkWithText("full-text search").findWhenNeeded(this);
        protected WebElement folderTypeLink = Locator.linkWithText("folder types").findWhenNeeded(this);
        protected WebElement lookAndFeelSettingsLink = Locator.linkWithText("look and feel settings").findWhenNeeded(this);
        protected WebElement masterPatientIndex = Locator.linkWithText("Master Patient Index").findWhenNeeded(this);
        protected WebElement profilerLink = Locator.linkWithText("profiler").findWhenNeeded(this);
        protected WebElement runningThreadsLink = Locator.linkWithText("running threads").findWhenNeeded(this);
        protected WebElement siteSettingsLink = Locator.linkWithText("site settings").findWhenNeeded(this);
        protected WebElement siteWideTermsLink = Locator.linkContainingText("site-wide terms of use").findWhenNeeded(this);
        protected WebElement systemMaintenanceLink = Locator.linkWithText("system maintenance").findWhenNeeded(this);
        protected WebElement systemPropertiesLink = Locator.linkContainingText("system properties").findWhenNeeded(this);
        protected WebElement viewsAndScriptingLink = Locator.linkWithText("views and scripting").findWhenNeeded(this);
        protected WebElement creditsLink = Locator.linkWithText("credits").findElementOrNull(this);

        protected List<WebElement> findActiveUsers()
        {
            return Locator.tagWithName("table", "activeUsers").append(Locator.tag("td").position(1)).findElements(this);
        }

        protected WebElement findServerGUID()
        {
            return Locator.tagWithText("td", "Server GUID").followingSibling("td").findElement(this);
        }
    }
}