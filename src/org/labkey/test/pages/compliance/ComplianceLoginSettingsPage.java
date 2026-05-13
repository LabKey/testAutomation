package org.labkey.test.pages.compliance;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
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
        final WebElement acceptOnlyFicamChk = Locator.checkboxById("acceptOnlyFICAMProviders").findWhenNeeded(this);
        final WebElement ficamProvidersDiv = Locator.tagWithId("div", "FICAMProviders").findWhenNeeded(this);
    }
}
