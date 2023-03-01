package org.labkey.test.pages.compliance;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

public class ComplianceLoginSettingsPage extends BaseComplianceSettingsPage
{
    public ComplianceLoginSettingsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ComplianceLoginSettingsPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        BaseComplianceSettingsPage.beginAt(webDriverWrapper, "login");
        return new ComplianceLoginSettingsPage(webDriverWrapper.getDriver());
    }


    public void enableLoginControls()
    {
        checkCheckbox(elementCache().enableLoginChk);
        waitForElement(elementCache().loginAttemptCountCombo.enabled());
    }

    public void disableLoginControls()
    {
        uncheckCheckbox(elementCache().enableLoginChk);
        //TODO: make this wait not dumb
        sleep(2000);
    }

    public void setLoginAttemptCount(String count)
    {
        setFormElement(elementCache().loginAttemptCountInput(), count);
    }

    public void selectLoginAttemptCount(String count)
    {
        _ext4Helper.selectComboBoxItem(elementCache().loginAttemptCountCombo, count);
    }

    public void selectLoginAttemptPeriod(String period)
    {
        _ext4Helper.selectComboBoxItem(elementCache().loginAttemptPeriodCombo, period);
    }

    public void selectLoginAttemptRecoveryTime(String time)
    {
        _ext4Helper.selectComboBoxItem(elementCache().loginAttemptRecoveryTimeCombo, time);
    }

    public void setLoginAttemptRecoveryTime(String time)
    {
        setFormElement(elementCache().loginAttemptRecoveryTimeInput(), time);
    }

    public void setLoginAttemptPeriod(String time)
    {
        setFormElement(elementCache().loginAttemptPeriodInput(), time);
    }


    public void enableFicamProviders()
    {
        checkCheckbox(elementCache().acceptOnlyFicamChk);
        waitForElementToBeVisible(elementCache().ficamProvidersDiv);
    }

    public boolean isFicamProvidersChecked()
    {
        return isChecked(elementCache().acceptOnlyFicamChk);
    }

    public boolean isFicamProvidersDivDisplayed()
    {
        return elementCache().ficamProvidersDiv().isDisplayed();
    }

    public void disableFicamProviders()
    {
        uncheckCheckbox(elementCache().acceptOnlyFicamChk);
    }

    public String getFicamProviersList()
    {
        return getText(elementCache().ficamProvidersList);
    }

    public void clickSaveExpectingAlert(String expectedAlert)
    {
        elementCache().saveBtn.waitForElement(getDriver(), 2000).click();
        assertAlert(expectedAlert);
    }

    public ComplianceLoginSettingsPage clickSave()
    {
        clickButton("Save");
        return new ComplianceLoginSettingsPage(getDriver());
    }

    public ComplianceLoginSettingsPage clickCancel()
    {
        clickButton("Cancel");
        return new ComplianceLoginSettingsPage(getDriver());
    }

    @Override
    protected BaseComplianceSettingsPage.ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends BaseComplianceSettingsPage.ElementCache
    {
        final Locator.XPathLocator loginTab = Locator.tagWithClass("div", "tab").childTag("a").withText("Login");
        final Locator.XPathLocator enableLoginChk = Locator.checkboxById("enableLogin");
        final Locator.XPathLocator loginAttemptCountCombo = Locator.tagWithId("span", "loginAttemptCount");
        final Locator.XPathLocator loginAttemptPeriodCombo = Locator.tagWithId("span", "loginAttemptPeriod");
        final Locator.XPathLocator loginAttemptRecoveryTimeCombo = Locator.tagWithId("span", "loginAttemptRecoveryTime");
        final Locator.XPathLocator loginAttemptCountInput = Locator.input("attemptLimit");
        final Locator.XPathLocator loginAttemptPeriodInput = Locator.input("attemptPeriod");
        final Locator.XPathLocator loginAttemptRecoveryTimeInput = Locator.input("resetTime");

        final Locator.XPathLocator acceptOnlyFicamChk = Locator.checkboxById("acceptOnlyFICAMProviders");
        final Locator.XPathLocator ficamProvidersDiv = Locator.tagWithId("div", "FICAMProviders");
         final Locator.XPathLocator ficamProvidersList = Locator.xpath("//div[@id='FICAMProviders']//li");

        final Locator.XPathLocator saveBtn = Locator.linkWithSpan("Save");

        WebElement enableLoginChk()
        {
            return enableLoginChk.waitForElement(this, 1_000);
        }

        WebElement loginAttemptCount()
        {
            return loginAttemptCountCombo.waitForElement(this, 1_000);
        }

        WebElement loginAttemptPeriod()
        {
            return loginAttemptPeriodCombo.waitForElement(this, 1_000);
        }

        WebElement loginAttemptRecoveryTime()
        {
            return loginAttemptRecoveryTimeCombo.waitForElement(this, 1_000);
        }

        WebElement loginAttemptCountInput()
        {
            return loginAttemptCountInput.waitForElement(this, 1_000);
        }

        WebElement loginAttemptPeriodInput()
        {
            return loginAttemptPeriodInput.waitForElement(this, 1_000);
        }

        WebElement loginAttemptRecoveryTimeInput()
        {
            return loginAttemptRecoveryTimeInput.waitForElement(this, 1_000);
        }

        WebElement acceptOnlyFicamChk()
        {
            return acceptOnlyFicamChk.waitForElement(this, 1_000);
        }

        WebElement ficamProvidersDiv()
        {
            return ficamProvidersDiv.waitForElement(this, 1_000);
        }
    }
}
