package org.labkey.test.pages.core.admin;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    protected void waitForPage()
    {
        waitFor(()-> {
            try
            {
                return !BootstrapLocators.loadingSpinner.areAnyVisible(getDriver()) &&
                        elementCache().extension.getComponentElement().isDisplayed();
            }
            catch (NoSuchElementException | StaleElementReferenceException | TimeoutException retry)
            {
                return false;
            }
        }, "Allowed File Extensions page did not load in time.", 3_000);

    }

    public AllowedFileExtensionAdminPage setExtension(String extension)
    {
        elementCache().extension.set(extension);
        return this;
    }

    public String clickSaveExpectingError()
    {
        String errorText;
        elementCache().saveExtension.click();
        errorText = waitForElement(Locators.labkeyError).getText();
        clearCache();
        return errorText;
    }

    public AllowedFileExtensionAdminPage clickSaveExtension()
    {
        elementCache().saveExtension.click();
        clearCache();
        return this;
    }

    public AllowedFileExtensionAdminPage updateExtension(String oldExtension, String newExtension)
    {
        getAllowedExtension(getAllowedExtensionIndex(oldExtension)).set(newExtension);
        clearCache();
        return this;
    }

    public AllowedFileExtensionAdminPage clickSaveUpdateExtension()
    {
        elementCache().saveUpdateExtension.click();
        sleep(750);
        clearCache();
        return this;
    }

    public String clickUpdateExtensionExpectingError()
    {
        elementCache().saveUpdateExtension.click();
        clearCache();
        return waitForElement(Locators.labkeyError).getText();
    }

    public AllowedFileExtensionAdminPage deleteExtension(int index)
    {
        WebElement deleteButton = elementCache().deleteExtensions.get(index);
        deleteButton.click();

        shortWait().withMessage("Existing extenstion was not deleted.")
                .until(ExpectedConditions.stalenessOf(deleteButton));

        clearCache();
        return this;
    }

    public AllowedFileExtensionAdminPage deleteExtension(String extension)
    {
        return deleteExtension(getAllowedExtensionIndex(extension));
    }

    public AllowedFileExtensionAdminPage deleteAllExtensions(boolean acceptAlert)
    {
        elementCache().deleteAll.click();

        if (acceptAlert)
        {
            acceptAlert();
            shortWait().withMessage("'Delete All' button should have gone away.")
                    .until(ExpectedConditions.stalenessOf(elementCache().deleteAll));
        }
        else
        {
            cancelAlert();
        }

        clearCache();
        return this;
    }

    public List<Input> getAllowedExtensions()
    {
        List<WebElement> collection = Locator.inputByIdContaining("existingValue")
                .findElements(elementCache().existingPanel);

        return collection.stream().map(el -> new Input(el, getDriver())).collect(Collectors.toList());

    }

    public Input getAllowedExtension(int index)
    {
        return getAllowedExtensions().get(index);
    }

    public Integer getAllowedExtensionIndex(String extension)
    {
        int index = 0;
        List<Input> allowedExtensions = getAllowedExtensions();

        for(Input element : allowedExtensions)
        {
            if (element.getValue().equalsIgnoreCase(extension))
                return index;

            index++;
        }

        return  -1;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<ElementCache>.ElementCache
    {
        final WebElement registerNewPanel = PortalHelper.Locators.webPart("Register New Allowed File Extension").findWhenNeeded(this);
        final WebElement existingPanel = PortalHelper.Locators.webPart("Existing Allowed File Extensions").findWhenNeeded(this);

        final Input extension = new Input(Locator.id("newValueTextField").findWhenNeeded(registerNewPanel), getDriver());
        final WebElement saveExtension = Locator.lkButton("Save").findWhenNeeded(registerNewPanel);

        final WebElement saveUpdateExtension = Locator.lkButton("Save").findWhenNeeded(existingPanel);

        List<Input> allowedExtensions()
        {
            return Locator.inputByIdContaining("existingValue")
                    .findElements(elementCache().existingPanel)
                    .stream().map(el -> new Input(el, getDriver())).collect(Collectors.toList());
        }

        final List<WebElement> deleteExtensions = Locator.linkWithText("Delete").findElements(existingPanel);

        final WebElement deleteAll = Locator.linkWithText("Delete All").findWhenNeeded(existingPanel);

    }
}
