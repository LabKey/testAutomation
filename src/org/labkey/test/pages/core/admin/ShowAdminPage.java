/*
 * Copyright (c) 2016-2026 LabKey Corporation
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
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.admin.ConfigureSystemMaintenancePage;
import org.labkey.test.pages.admin.ExternalSourcesPage;
import org.labkey.test.pages.compliance.ComplianceSettingsAccountsPage;
import org.labkey.test.pages.core.login.LoginConfigurePage;
import org.labkey.test.util.OptionalFeatureHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.function.Function;

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
        shortWait().until(ExpectedConditions.visibilityOf(elementCache().serverInfoPanel));
        return this;
    }

    public ShowAdminPage goToSettingsSection()
    {
        elementCache().sectionSettingsLinks.click();
        shortWait().until(ExpectedConditions.visibilityOf(elementCache().settingsPanel));
        return this;
    }

    public ShowAdminPage goToModuleInformationSection()
    {
        elementCache().sectionModuleInfo.click();
        shortWait().until(ExpectedConditions.visibilityOf(elementCache().moduleInfoPanel));
        return this;
    }

    public ShowAdminPage goToRecentUsersSection()
    {
        elementCache().sectionActiveUsers.click();
        shortWait().until(ExpectedConditions.visibilityOf(elementCache().recentUsersPanel));
        return this;
    }

    public void clickSettingsLink(String settingsLink)
    {
        goToSettingsSection();
        clickAndWait(Locator.linkWithText(settingsLink).findElement(elementCache().settingsPanel));
    }

    public <T> T clickSettingsLink(String settingsLink, Function<WebDriver, T> pageFactory)
    {
        clickSettingsLink(settingsLink);
        return pageFactory.apply(getDriver());
    }

    public List<String> getRecentUsers()
    {
        goToRecentUsersSection();
        return getTexts(elementCache().findRecentUsers());
    }

    public String getServerGUID()
    {
        goToServerInformationSection();
        return elementCache().serverGuidEl.getText();
    }

    public void clickAnalyticsSettings()
    {
        clickSettingsLink("analytics settings");
    }

    public void clickAllowedExternalRedirectHosts()
    {
        clickSettingsLink("allowed external redirect hosts");
        Locator.waitForAnyElement(shortWait(), Locator.tagWithText("span", "Done"), Locator.tagWithText("span", "Save"));
    }

    public ShowAuditLogPage clickAuditLog()
    {
        return clickSettingsLink("audit log", ShowAuditLogPage::new);
    }

    public ExternalSourcesPage clickAllowedExternalResourceHosts()
    {
        return clickSettingsLink("allowed external resource hosts", ExternalSourcesPage::new);
    }

    public AllowedFileExtensionAdminPage clickAllowedFileExtensions()
    {
        return clickSettingsLink("allowed file extensions", AllowedFileExtensionAdminPage::new);
    }

    public void clickAuditLogMaintenance()
    {
        clickSettingsLink("Audit Log Maintenance");
    }

    public LoginConfigurePage clickAuthentication()
    {
        return clickSettingsLink("authentication", LoginConfigurePage::new);
    }

    public void clickConfigurePageElements()
    {
        clickSettingsLink("configure page elements");
        Locator.waitForAnyElement(shortWait(), Locator.tagWithText("span", "Done"), Locator.tagWithText("span", "Save"));
    }

    public ComplianceSettingsAccountsPage clickComplianceSettings()
    {
        return clickSettingsLink("Compliance Settings", ComplianceSettingsAccountsPage::new);
    }

    public DomainDesignerPage clickChangeUserProperties()
    {
        return clickSettingsLink("change user properties", DomainDesignerPage::new);
    }

    public void clickDeprecatedFeatures()
    {
        throw new UnsupportedOperationException("Use %s to manage experimental/optional/deprecated features"
                .formatted(OptionalFeatureHelper.class.getSimpleName()));
    }

    public void clickEmailCustomization()
    {
        clickSettingsLink("email customization");
    }

    public void clickNotificationServiceAdmin()
    {
        clickSettingsLink("notification service admin");
    }

    public ConfigureFileSystemAccessPage clickFiles()
    {
        return clickSettingsLink("files", ConfigureFileSystemAccessPage::new);
    }

    public void clickFullTextSearch()
    {
        clickSettingsLink("full-text search");
    }

    public FolderTypePages clickFolderType()
    {
        return clickSettingsLink("folder types", FolderTypePages::new);
    }

    public SiteValidationPage clickSiteValidation()
    {
        return clickSettingsLink("site validation", SiteValidationPage::new);
    }

    public LookAndFeelSettingsPage clickLookAndFeelSettings()
    {
        return clickSettingsLink("look and feel settings", LookAndFeelSettingsPage::new);
    }

    public void clickMasterPatientIndex()
    {
        clickSettingsLink("Master Patient Index");
    }

    public void clickProfiler()
    {
        clickSettingsLink("profiler");
    }

    public void clickRunningThreads()
    {
        clickSettingsLink("running threads");
    }

    public CustomizeSitePage clickSiteSettings()
    {
        return clickSettingsLink("site settings", CustomizeSitePage::new);
    }

    public void clickSiteWideTerms()
    {
        clickSettingsLink("site-wide terms of use");
    }

    public ConfigureSystemMaintenancePage clickSystemMaintenance()
    {
        return clickSettingsLink("system maintenance", ConfigureSystemMaintenancePage::new);
    }

    public void clickSystemProperties()
    {
        clickSettingsLink("system properties");
    }

    public ConfigureReportsAndScriptsPage clickViewsAndScripting()
    {
        return clickSettingsLink("views and scripting", ConfigureReportsAndScriptsPage::new);
    }

    public void clickCredits()
    {
        clickSettingsLink("credits");
    }

    public void clickViewPrimarySiteLogFile()
    {
        clickSettingsLink("view primary site log file");
    }

    public void clickPostgresActivity()
    {
        clickSettingsLink("postgres activity");
    }

    public void clickPostgresLocks()
    {
        clickSettingsLink("postgres locks");
    }

    public void clickPostgresTableSizes()
    {
        clickSettingsLink("postgres table sizes");
    }

    public void clickTestEmailConfiguration()
    {
        clickSettingsLink("test email configuration");
    }

    public List<WebElement> getAllAdminConsoleLinks()
    {
        goToSettingsSection();
        return Locator.tag("a").findElements(elementCache().settingsPanel);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<ShowAdminPage.ElementCache>.ElementCache
    {
        private final WebElement adminNavPanel = Locator.id("lk-admin-nav").findWhenNeeded(this);
        private final WebElement sectionServerInfo = Locator.linkWithText("Server Information").findWhenNeeded(adminNavPanel);
        private final WebElement sectionSettingsLinks = Locator.linkWithText("Settings").findWhenNeeded(adminNavPanel);
        private final WebElement sectionModuleInfo = Locator.linkWithText("Module Information").findWhenNeeded(adminNavPanel);
        private final WebElement sectionActiveUsers = Locator.linkWithText("Active Users").findWhenNeeded(adminNavPanel);

        private final WebElement serverInfoPanel = Locator.id("info").withClass("lk-admin-section").findWhenNeeded(this);
        private final WebElement settingsPanel = Locator.id("links").withClass("lk-admin-section").findWhenNeeded(this);
        private final WebElement moduleInfoPanel = Locator.id("modules").withClass("lk-admin-section").findWhenNeeded(this);
        private final WebElement recentUsersPanel = Locator.id("users").withClass("lk-admin-section").findWhenNeeded(this);

        private List<WebElement> findRecentUsers()
        {
            return Locator.tagWithName("table", "activeUsers").append(Locator.tag("td").position(1)).findElements(recentUsersPanel);
        }

        private final WebElement serverGuidEl =  Locator.tagWithText("td", "Server GUID").followingSibling("td").findWhenNeeded(serverInfoPanel);
    }
}
