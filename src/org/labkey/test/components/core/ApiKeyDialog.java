/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
package org.labkey.test.components.core;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;

public class ApiKeyDialog extends ModalDialog
{
    public static final String API_KEY_TITLE = "API Key";
    public static final String SESSION_KEY_TITLE = "Session Key";

    private final String _title;

    public ApiKeyDialog(WebDriver driver, String title)
    {
        super(new ModalDialogFinder(driver).withTitle(title));
        _title = title;
    }

    public ApiKeyDialog generateApiKey()
    {
        elementCache().generateApiKeyButton.click();
        getWrapper().shortWait().until(ExpectedConditions.invisibilityOf(elementCache().descriptionInput.getComponentElement()));
        clearElementCache();
        getWrapper().shortWait().until(ExpectedConditions.visibilityOf(elementCache().inputField.getComponentElement()));
        return this;
    }

    public ApiKeyDialog copyKey()
    {
        elementCache().copyKeyButton.click();
        return this;
    }

    public String getClipboardContent() throws IOException, UnsupportedFlavorException
    {
        DataFlavor[] flavors = Toolkit.getDefaultToolkit().getSystemClipboard().getAvailableDataFlavors();
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

        // Adding debug info for TeamCity run.
        // Windows is not giving DataFlavor (MIME Type) for the data on the clipboard.
        getWrapper().log("Available flavors: " + Arrays.stream(flavors).toList());
        getWrapper().log("Best flavor: " + DataFlavor.selectBestTextFlavor(flavors));

        if (t != null)
        {

            // Adding debug info for TeamCity run.
            getWrapper().log("Is DataFlavor.imageFlavor supported? " + t.isDataFlavorSupported(DataFlavor.imageFlavor));
            getWrapper().log("Is DataFlavor.allHtmlFlavor supported? " + t.isDataFlavorSupported(DataFlavor.allHtmlFlavor));
            getWrapper().log("Is DataFlavor.fragmentHtmlFlavor supported? " + t.isDataFlavorSupported(DataFlavor.fragmentHtmlFlavor));
            getWrapper().log("Is DataFlavor.selectionHtmlFlavor supported? " + t.isDataFlavorSupported(DataFlavor.selectionHtmlFlavor));
            getWrapper().log("Is DataFlavor.javaFileListFlavor supported? " + t.isDataFlavorSupported(DataFlavor.javaFileListFlavor));
            getWrapper().log("Is DataFlavor.stringFlavor supported? " + t.isDataFlavorSupported(DataFlavor.stringFlavor));

            DataFlavor[] transferFlavors = t.getTransferDataFlavors();
            getWrapper().log("Transferable supported data flavors: " + Arrays.stream(transferFlavors).toList());

            if (flavors.length > 0)
            {
                getWrapper().log("Best Text Flavor: " + DataFlavor.selectBestTextFlavor(flavors));
                return (String) Toolkit.getDefaultToolkit().getSystemClipboard()
                        .getData(DataFlavor.selectBestTextFlavor(flavors));
            }
            else
            {
                getWrapper().log("There are no DataFlavors to use.");
                // Return a value to indicate something is on the clipboard but no DataFlavor was provided.
                return "There are no DataFlavors to use.";
            }

        }
        else
        {
            getWrapper().log("The clipboard is empty.");
            return "";
        }

    }

    public boolean isCopyButtonDisplayed()
    {
        return elementCache().copyKeyButton.isDisplayed();
    }

    public boolean isCopyButtonEnabled()
    {
        return elementCache().copyKeyButton.isEnabled();
    }

    public boolean isGenerateButtonEnabled()
    {
        return elementCache().generateApiKeyButton.isEnabled();
    }

    public boolean isGenerateButtonDisplayed()
    {
        return elementCache().generateApiKeyButton.isDisplayed();
    }

    public boolean isInputFieldEnabled()
    {
        return elementCache().inputField.getComponentElement().isEnabled();
    }

    public boolean isInputFieldDisplayed()
    {
        return elementCache().inputField.getComponentElement().isDisplayed();
    }

    public boolean isDescriptionFieldDisplayed() { return elementCache().descriptionInput.getComponentElement().isDisplayed(); }

    public void clickDone()
    {
        elementCache().doneButton.click();
    }

    public ApiKeyDialog setDescription(String description)
    {
        elementCache().descriptionInput.getComponentElement().sendKeys(description);
        return this;
    }

    public String getDescription()
    {
        return elementCache().descriptionDisplay.getText();
    }

    public String getInputFieldValue()
    {
        return elementCache().inputField.getValue();
    }

    @Override
    protected ApiKeyDialog.ElementCache newElementCache()
    {
        return new ApiKeyDialog.ElementCache();
    }

    @Override
    protected ApiKeyDialog.ElementCache elementCache()
    {
        return (ApiKeyDialog.ElementCache) super.elementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        Input descriptionInput = Input.Input(Locator.tagWithId("input", "keyDescription"), getDriver()).findWhenNeeded(this);
        WebElement descriptionDisplay = Locator.tagWithClassContaining("div", "api-key__description").findWhenNeeded(this);
        WebElement generateApiKeyButton = Locator.tagWithText("button", "Generate API Key").findWhenNeeded(this);
        Input inputField = Input.Input(Locator.tagWithClass("input", "api-key__input"), getDriver()).findWhenNeeded(this);
        WebElement copyKeyButton = Locator.tagWithName("button", "copy_apikey_token").findWhenNeeded(this);
        WebElement doneButton = Locator.tagWithText("button", "Done").findWhenNeeded(this);
    }
}
