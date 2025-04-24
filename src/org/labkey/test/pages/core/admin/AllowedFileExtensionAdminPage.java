package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class AllowedFileExtensionAdminPage extends LabKeyPage<AllowedFileExtensionAdminPage.ElementCache>
{
    public AllowedFileExtensionAdminPage(WebDriver driver)
    {
        super(driver);
    }

    public static AllowedFileExtensionAdminPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("admin", "allowList"));
        return new AllowedFileExtensionAdminPage(webDriverWrapper.getDriver());
    }

    public AllowedFileExtensionAdminPage setExtension(String extension)
    {
        elementCache().extension.set(extension);
        return this;
    }

    public String clickSaveExpectingError()
    {
        elementCache().saveExtension.click();
        clearCache();
        return waitForElement(Locators.labkeyError).getText();
    }

    public AllowedFileExtensionAdminPage clickSaveExtension()
    {
        elementCache().saveExtension.click();
        clearCache();
        return this;
    }

    public AllowedFileExtensionAdminPage updateExtension(String oldExtension, String newExtension)
    {
        elementCache().allowedExtension(oldExtension).set(newExtension);
        clearCache();
        return this;
    }

    public AllowedFileExtensionAdminPage deleteExtension(int index)
    {
        elementCache().deleteExtension(index);
        clearCache();
        return this;
    }

    public AllowedFileExtensionAdminPage clickUpdateExtension()
    {
        elementCache().updateExtension.click();
        clearCache();
        return this;
    }

    public String clickUpdateExtensionExpectingError()
    {
        elementCache().updateExtension.click();
        clearCache();
        return waitForElement(Locators.labkeyError).getText();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<ElementCache>.ElementCache
    {
        final WebElement registerNewAllowedFileExtension = PortalHelper.Locators.webPart("Register New Allowed File Extension")
                .findWhenNeeded(this);
        final Input extension = new Input(Locator.name("newValue").findWhenNeeded(registerNewAllowedFileExtension), getDriver());
        final WebElement saveExtension = Locator.lkButton("Save").findWhenNeeded(registerNewAllowedFileExtension);

        final WebElement existingAllowedFileExtensions = PortalHelper.Locators.webPart("Existing Allowed File Extensions")
                .findWhenNeeded(this);
        final WebElement updateExtension = Locator.lkButton("Save").findWhenNeeded(existingAllowedFileExtensions);

        final Input allowedExtension(String value)
        {
            return new Input(Locator.inputByIdContaining("existingValue").withAttribute("value", value)
                    .findWhenNeeded(existingAllowedFileExtensions), getDriver());
        }

        final WebElement deleteExtension(int index)
        {
            return Locator.linkWithText("Delete").index(index).findWhenNeeded(existingAllowedFileExtensions);
        }
    }
}
