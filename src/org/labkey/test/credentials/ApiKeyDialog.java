package org.labkey.test.credentials;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

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
        return new ApiKeyDialog(getDriver(), _title);
    }

    public ApiKeyDialog copyKey()
    {
        elementCache().copyKeyButton.click();
        return this;
    }

    public String getClipboardContent() throws IOException, UnsupportedFlavorException
    {
        return  (String) Toolkit.getDefaultToolkit().getSystemClipboard()
                .getData(DataFlavor.stringFlavor);
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
        Input descriptionInput = Input.Input(Locator.tagWithId("input", "keyDescription"), getDriver()).refindWhenNeeded(this);
        WebElement descriptionDisplay = Locator.tagWithClassContaining("div", "api-key__description").refindWhenNeeded(this);
        WebElement generateApiKeyButton = Locator.tagWithText("button", "Generate API Key").findWhenNeeded(this);
        Input inputField = Input.Input(Locator.tagWithClass("input", "api-key__input"), getDriver()).findWhenNeeded(this);
        WebElement copyKeyButton = Locator.tagWithName("button", "copy_apikey_token").findWhenNeeded(this);
        WebElement doneButton = Locator.tagWithText("button", "Done").findWhenNeeded(this);
    }
}
