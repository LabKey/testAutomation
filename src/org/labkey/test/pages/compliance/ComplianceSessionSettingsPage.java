package org.labkey.test.pages.compliance;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.html.RadioButton;
import org.openqa.selenium.WebDriver;

public class ComplianceSessionSettingsPage extends BaseComplianceSettingsPage<ComplianceSessionSettingsPage.ElementCache>
{
    public ComplianceSessionSettingsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ComplianceSessionSettingsPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        BaseComplianceSettingsPage.beginAt(webDriverWrapper, SettingsTab.Session);
        return new ComplianceSessionSettingsPage(webDriverWrapper.getDriver());
    }

    public ComplianceSessionSettingsPage showBackgroundBehindLoggedOutModal()
    {
        elementCache().showBackgroundRadio.check();
        return this;
    }

    public ComplianceSessionSettingsPage blurBackgroundBehindLoggedOutModal()
    {
        elementCache().blurBackgroundRadio.check();
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BaseComplianceSettingsPage<ElementCache>.ElementCache
    {
        final RadioButton showBackgroundRadio = new RadioButton.RadioButtonFinder().withNameAndValue("backgroundHideEnabled", "false").findWhenNeeded(this);
        final RadioButton blurBackgroundRadio = new RadioButton.RadioButtonFinder().withNameAndValue("backgroundHideEnabled", "true").findWhenNeeded(this);
    }
}
