/*
 * Copyright (c) 2025-2026 LabKey Corporation
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

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimplePostCommand;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AllowedFileExtensionAdminPage extends LabKeyPage<AllowedFileExtensionAdminPage.ElementCache>
{
    public AllowedFileExtensionAdminPage(WebDriver driver)
    {
        super(driver);
    }

    public static AllowedFileExtensionAdminPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("admin", "allowList",
                Map.of("type", "FileExtension")));
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

    /**
     * Clears any file extensions that are set as the only allowable names, letting users upload any filename they like.
     *
     * @param connection A connection object to use in the command.execute call.
     */
    public static void deleteAllAllowedFileExtension(Connection connection) throws IOException, CommandException
    {
        SimplePostCommand command = new SimplePostCommand("admin", "deleteAllValues");
        Map<String, Object> params = new HashMap<>();
        params.put("type", "FileExtension");
        command.setParameters(params);
        command.execute(connection, "/");
    }


    public AllowedFileExtensionAdminPage setExtension(String extension)
    {
        elementCache().extension.set(extension);
        return this;
    }

    public String clickSaveExpectingError()
    {
        return clickButtonExpectingError(elementCache().saveExtension);
    }

    public AllowedFileExtensionAdminPage clickSaveExtension()
    {
        return clickButtonNoError(elementCache().saveExtension);
    }

    public AllowedFileExtensionAdminPage updateExtension(String oldExtension, String newExtension)
    {
        getAllowedExtension(getAllowedExtensionIndex(oldExtension)).set(newExtension);
        clearCache();
        return this;
    }

    public AllowedFileExtensionAdminPage clickSaveUpdateExtension()
    {
        return clickButtonNoError(elementCache().saveUpdateExtension);
    }

    public String clickUpdateExtensionExpectingError()
    {
        return clickButtonExpectingError(elementCache().saveUpdateExtension);
    }

    private AllowedFileExtensionAdminPage clickButtonNoError(WebElement button)
    {
        clickAndWait(button);
        clearCache();
        assertNoLabKeyErrors();
        return this;
    }

    private String clickButtonExpectingError(WebElement button)
    {
        clickAndWait(button);
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
        if (acceptAlert)
        {
            doAndWaitForPageToLoad(() -> {
                elementCache().deleteAll.click();
                acceptAlert();
            });
            clearCache();
            assertFalse("Delete All button should not be present after deleting all extensions", elementCache().deleteAll.isDisplayed());
        }
        else
        {
            elementCache().deleteAll.click();
            cancelAlert();
            assertTrue("Delete All button should be present", elementCache().deleteAll.isDisplayed());
        }
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
