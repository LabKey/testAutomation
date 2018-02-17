/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
import org.labkey.test.pages.LabKeyPage;
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

    public ShowAdminPage goToAdminConsoleLinksSection()
    {
        elementCache().sectionAdminConsoleLinks.click();
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

    public void clickAnalyticsSettings()
    {
        goToAdminConsoleLinksSection();
        clickAndWait(elementCache().analyticsSettingsLink);
    }

    public ShowAuditLogPage clickAuditLog()
    {
        goToAdminConsoleLinksSection();
        clickAndWait(elementCache().auditLogLink);
        return new ShowAuditLogPage(getDriver());
    }

    public void clickAuthentication()
    {
        goToAdminConsoleLinksSection();
        clickAndWait(elementCache().authenticationLink);
    }

    public void clickConfigureFooter()
    {
        goToAdminConsoleLinksSection();
        clickAndWait(elementCache().configureFooterLink);
    }

    public void clickConfigureHeader()
    {
        goToAdminConsoleLinksSection();
        clickAndWait(elementCache().configureHeaderLink);
    }

    public void clickEmailCustomization()
    {
        goToAdminConsoleLinksSection();
        clickAndWait(elementCache().emailCustomizationLink);
    }

    public ConfigureFileSystemAccessPage clickFiles()
    {
        goToAdminConsoleLinksSection();
        clickAndWait(elementCache().filesLink);
        return new ConfigureFileSystemAccessPage(getDriver());
    }

    public void clickFullTextSearch()
    {
        goToAdminConsoleLinksSection();
        clickAndWait(elementCache().fullTextSearchLink);
    }

    public void clickLookAndFeelSettings()
    {
        goToAdminConsoleLinksSection();
        clickAndWait(elementCache().lookAndFeelSettingsLink);
    }

    public void clickProfiler()
    {
        goToAdminConsoleLinksSection();
        clickAndWait(elementCache().profilerLink);
    }

    public void clickRunningThreads()
    {
        goToAdminConsoleLinksSection();
        clickAndWait(elementCache().runningThreadsLink);
    }

    public CustomizeSitePage clickSiteSettings()
    {
        goToAdminConsoleLinksSection();
        clickAndWait(elementCache().siteSettingsLink);
        return new CustomizeSitePage(getDriver());
    }

    public void clickSiteWideTerms()
    {
        goToAdminConsoleLinksSection();
        clickAndWait(elementCache().siteWideTermsLink);
    }

    public void clickSystemMaintenance()
    {
        goToAdminConsoleLinksSection();
        clickAndWait(elementCache().systemMaintenanceLink);
    }

    public void clickSystemProperties()
    {
        goToAdminConsoleLinksSection();
        clickAndWait(elementCache().systemPropertiesLink);
    }

    public void clickViewsAndScripting()
    {
        goToAdminConsoleLinksSection();
        clickAndWait(elementCache().viewsAndScriptingLink);
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected WebElement sectionServerInfo = Locator.linkWithText("Server Information").findWhenNeeded(this);
        protected WebElement sectionAdminConsoleLinks = Locator.linkWithText("Admin Console Links").findWhenNeeded(this);
        protected WebElement sectionModuleInfo = Locator.linkWithText("Module Information").findWhenNeeded(this);
        protected WebElement sectionActiveUsers = Locator.linkWithText("Active Users").findWhenNeeded(this);

        protected WebElement analyticsSettingsLink = Locator.linkWithText("analytics settings").findWhenNeeded(this);
        protected WebElement auditLogLink = Locator.linkWithText("audit log").findWhenNeeded(this);
        protected WebElement authenticationLink = Locator.linkWithText("authentication").findWhenNeeded(this);
        protected WebElement configureFooterLink = Locator.linkWithText("configure footer").findWhenNeeded(this);
        protected WebElement configureHeaderLink = Locator.linkWithText("configure header").findWhenNeeded(this);
        protected WebElement emailCustomizationLink = Locator.linkWithText("email customization").findWhenNeeded(this);
        protected WebElement filesLink = Locator.linkWithText("files").findWhenNeeded(this);
        protected WebElement fullTextSearchLink = Locator.linkWithText("full-text search").findWhenNeeded(this);
        protected WebElement lookAndFeelSettingsLink = Locator.linkWithText("look and feel settings").findWhenNeeded(this);
        protected WebElement profilerLink = Locator.linkWithText("profiler").findWhenNeeded(this);
        protected WebElement runningThreadsLink = Locator.linkWithText("running threads").findWhenNeeded(this);
        protected WebElement siteSettingsLink = Locator.linkWithText("site settings").findWhenNeeded(this);
        protected WebElement siteWideTermsLink = Locator.linkContainingText("site-wide terms of use").findWhenNeeded(this);
        protected WebElement systemMaintenanceLink = Locator.linkWithText("system maintenance").findWhenNeeded(this);
        protected WebElement systemPropertiesLink = Locator.linkContainingText("system properties").findWhenNeeded(this);
        protected WebElement viewsAndScriptingLink = Locator.linkWithText("views and scripting").findWhenNeeded(this);

        protected List<WebElement> findActiveUsers()
        {
            return Locator.tagWithName("table", "activeUsers").append(Locator.tag("td").position(1)).findElements(this);
        }
    }
}