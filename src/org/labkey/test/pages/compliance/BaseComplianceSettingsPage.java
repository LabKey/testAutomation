package org.labkey.test.pages.compliance;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Map;

/**
 * Base test wrapper for `ComplianceController.ComplianceSettingsAction`. Subclasses represent the different tabs on that page.
 * Available tabs are defined in `org.labkey.compliance.ComplianceManager.ComplianceSetting`
 */
public abstract class BaseComplianceSettingsPage<EC extends BaseComplianceSettingsPage<EC>.ElementCache> extends LabKeyPage<EC>
{
    public BaseComplianceSettingsPage(WebDriver driver)
    {
        super(driver);
    }

    protected static void beginAt(WebDriverWrapper webDriverWrapper, SettingsTab tab)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("compliance", null, "complianceSettings",
                Map.of("tab", tab.getTabId())));
    }

    public ComplianceSettingsAccountsPage clickAccountsTab()
    {
        showTab(SettingsTab.Accounts);
        return new ComplianceSettingsAccountsPage(getDriver());
    }

    public ComplianceSettingsAuditPage clickAuditTab()
    {
        showTab(SettingsTab.Audit);
        return new ComplianceSettingsAuditPage(getDriver());
    }

    public ComplianceSettingsLoginPage clickLoginTab()
    {
        showTab(SettingsTab.Login);
        return new ComplianceSettingsLoginPage(getDriver());
    }

    public ComplianceSettingsSessionPage clickSessionTab()
    {
        showTab(SettingsTab.Session);
        return new ComplianceSettingsSessionPage(getDriver());
    }

    public ComplianceSettingsProjectLockAndReviewPage clickProjectLockingTab()
    {
        showTab(SettingsTab.ProjectLockingAndReview);
        return new ComplianceSettingsProjectLockAndReviewPage(getDriver());
    }

    protected void showTab(SettingsTab tab)
    {
        if (!isTabActive(tab))
        {
            WebElement selectTab = elementCache().settingsTab(tab);
            WebElement changeSettingsLink = Locator.tag("a").findElement(selectTab);
            shortWait().until(ExpectedConditions.elementToBeClickable(changeSettingsLink));
            clickAndWait(changeSettingsLink);
            waitFor(()-> isTabActive(tab),
                    "tab took too long to become active: " + tab.name(), 4000);
        }
    }

    private boolean isTabActive(SettingsTab tab)
    {
        WebElement tabElement = elementCache().settingsTab(tab);
        String tabClass = tabElement.getAttribute("class");
        return tabClass != null && tabClass.contains("active-tab");
    }

    public final void clickSave()
    {
        clickAndWait(elementCache().saveButton);
        clearCache(); // Saving triggers a load but stays on the page.
    }

    public final void clickCancel()
    {
        clickAndWait(elementCache().cancelButton);
    }

    @Override
    protected abstract EC newElementCache();

    protected abstract class ElementCache extends LabKeyPage<?>.ElementCache
    {
        final WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
        final WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);

        WebElement settingsTab(SettingsTab tab)
        {
            return Locator.tagWithClass("div", "tab")
                    .withChild(Locator.tagWithText("a", tab.getTabText())).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        }
    }

    public enum SettingsTab
    {
        Accounts("Accounts", "accounts"),
        Audit("Audit", "audit"),
        Login("Login", "login"),
        Session("Session", "session"),
        ProjectLockingAndReview("Project Locking & Review", "projectLockAndReview");

        private final String _tabText;
        private final String _tabId;

        SettingsTab(String tabText, String tabId)
        {
            this._tabText = tabText;
            this._tabId = tabId;
        }

        public String getTabText()
        {
            return _tabText;
        }

        public String getTabId()
        {
            return _tabId;
        }
    }
}
