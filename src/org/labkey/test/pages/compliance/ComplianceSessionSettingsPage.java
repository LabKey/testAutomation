package org.labkey.test.pages.compliance;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.openqa.selenium.WebDriver;


public class ComplianceSessionSettingsPage extends BaseComplianceSettingsPage
{
    public ComplianceSessionSettingsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ComplianceSessionSettingsPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        BaseComplianceSettingsPage.beginAt(webDriverWrapper, "session");
        return new ComplianceSessionSettingsPage(webDriverWrapper.getDriver());
    }

    public ComplianceSessionSettingsPage showBackgroundBehindLoggedOutModal()
    {
        checkRadioButton(Locator.radioButtonByNameAndValue("backgroundHideEnabled", "false"));
        clickButton("Save");
        return this;
    }

    public ComplianceSessionSettingsPage blurBackgroundBehindLoggedOutModal()
    {
        checkRadioButton(Locator.radioButtonByNameAndValue("backgroundHideEnabled", "true"));
        clickButton("Save");
        return this;
    }
}
