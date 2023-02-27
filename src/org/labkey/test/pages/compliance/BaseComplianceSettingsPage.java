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
public abstract class BaseComplianceSettingsPage<EC extends BaseComplianceSettingsPage.ElementCache> extends LabKeyPage<EC>
{
    public BaseComplianceSettingsPage(WebDriver driver)
    {
        super(driver);
    }

    protected static void beginAt(WebDriverWrapper webDriverWrapper, String tabId)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("compliance", null, "complianceSettings",
                Map.of("tab", tabId)));
    }

    // todo: pull the accounts and audit tabs into subclasses of this class

    public ComplianceLoginSettingsPage showLoginTab()
    {
        showTab(SettingsTab.Login);
        return new ComplianceLoginSettingsPage(getDriver());
    }

    public ComplianceSessionSettingsPage showSessionTab()
    {
        showTab(SettingsTab.Session);
        return new ComplianceSessionSettingsPage(getDriver());
    }

    protected void showTab(ComplianceSettingsPage.SettingsTab tab)
    {
        if (!isTabActive(tab))
        {
            WebElement selectTab = elementCache().settingsTab(tab);
            WebElement changeSettingsLink = Locator.tag("a").findElement(selectTab);
            shortWait().until(ExpectedConditions.elementToBeClickable(changeSettingsLink));
            clickAndWait(changeSettingsLink);
            waitFor(()-> isTabActive(tab),
                    "tab took too long to become active", 4000);
        }
    }

    private boolean isTabActive(ComplianceSettingsPage.SettingsTab tab)
    {
        WebElement tabElement = elementCache().settingsTab(tab);
        String tabClass = tabElement.getAttribute("class");
        return tabClass != null && tabClass.contains("active-tab");
    }

    @Override
    protected EC newElementCache()
    {
        return (EC) new ElementCache();
    }

    protected EC elementCache()
    {
        return (EC) super.elementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement settingsTab(ComplianceSettingsPage.SettingsTab tab)
        {
            return Locator.tagWithClass("div", "tab")
                    .withChild(Locator.tagWithText("a", tab._text)).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        }
    }

    public enum SettingsTab
    {
        Accounts("Accounts"),
        Audit("Audit"),
        Login("Login"),
        Session("Session"),
        ProjectLockingAndReview("Project Locking & Review");

        public final String _text;
        SettingsTab(String tabText)
        {
            this._text = tabText;
        }
    }
}
