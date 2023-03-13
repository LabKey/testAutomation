package org.labkey.test.pages.compliance;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.ext4.ComboBox;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class ComplianceLoginSettingsPage extends BaseComplianceSettingsPage<ComplianceLoginSettingsPage.ElementCache>
{
    public ComplianceLoginSettingsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ComplianceLoginSettingsPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        BaseComplianceSettingsPage.beginAt(webDriverWrapper, SettingsTab.Login);
        return new ComplianceLoginSettingsPage(webDriverWrapper.getDriver());
    }


    public void enableLoginControls()
    {
        checkCheckbox(elementCache().enableLoginChk);
        shortWait().until(wd -> elementCache().loginAttemptCountCombo.isEnabled());
    }

    public void disableLoginControls()
    {
        uncheckCheckbox(elementCache().enableLoginChk);
        shortWait().until(wd -> !elementCache().loginAttemptCountCombo.isEnabled());
    }

    public void setLoginAttemptCount(String count)
    {
        setFormElement(elementCache().loginAttemptCountInput, count);
    }

    public void selectLoginAttemptCount(String count)
    {
        elementCache().loginAttemptCountCombo.selectComboBoxItem(count);
    }

    public void selectLoginAttemptPeriod(String period)
    {
        elementCache().loginAttemptPeriodCombo.selectComboBoxItem(period);
    }

    public void selectLoginAttemptRecoveryTime(String time)
    {
        elementCache().loginAttemptRecoveryTimeCombo.selectComboBoxItem(time);
    }

    public void setLoginAttemptRecoveryTime(String time)
    {
        setFormElement(elementCache().loginAttemptRecoveryTimeInput, time);
    }

    public void setLoginAttemptPeriod(String time)
    {
        setFormElement(elementCache().loginAttemptPeriodInput, time);
    }

    public void enableFicamProviders()
    {
        checkCheckbox(elementCache().acceptOnlyFicamChk);
        shortWait().until(ExpectedConditions.visibilityOf(elementCache().ficamProvidersDiv));
    }

    public boolean isFicamProvidersChecked()
    {
        return elementCache().acceptOnlyFicamChk.isSelected();
    }

    public boolean isFicamProvidersDivDisplayed()
    {
        return elementCache().ficamProvidersDiv.isDisplayed();
    }

    public void disableFicamProviders()
    {
        uncheckCheckbox(elementCache().acceptOnlyFicamChk);
    }

    public List<String> getFicamProviersList()
    {
        return getTexts(Locator.tag("li").findElements(elementCache().ficamProvidersDiv));
    }

    public void clickSaveExpectingAlert(String expectedAlert)
    {
        elementCache().saveButton.click();
        assertAlert(expectedAlert);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BaseComplianceSettingsPage<ElementCache>.ElementCache
    {
        final WebElement enableLoginChk = Locator.checkboxById("enableLogin").findWhenNeeded(this);
        final ComboBox loginAttemptCountCombo = new ComboBox.ComboBoxFinder(getDriver()).locatedBy(Locator.tagWithId("span", "loginAttemptCount")).findWhenNeeded(this);
        final ComboBox loginAttemptPeriodCombo = new ComboBox.ComboBoxFinder(getDriver()).locatedBy(Locator.tagWithId("span", "loginAttemptPeriod")).findWhenNeeded(this);
        final ComboBox loginAttemptRecoveryTimeCombo = new ComboBox.ComboBoxFinder(getDriver()).locatedBy(Locator.tagWithId("span", "loginAttemptRecoveryTime")).findWhenNeeded(this);
        final WebElement loginAttemptCountInput = Locator.input("attemptLimit").findWhenNeeded(this);
        final WebElement loginAttemptPeriodInput = Locator.input("attemptPeriod").findWhenNeeded(this);
        final WebElement loginAttemptRecoveryTimeInput = Locator.input("resetTime").findWhenNeeded(this);

        final WebElement acceptOnlyFicamChk = Locator.checkboxById("acceptOnlyFICAMProviders").findWhenNeeded(this);
        final WebElement ficamProvidersDiv = Locator.tagWithId("div", "FICAMProviders").findWhenNeeded(this);

    }
}
