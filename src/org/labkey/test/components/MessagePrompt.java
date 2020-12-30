package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;

import static org.labkey.test.components.html.Input.Input;

/**
 * Interact with an Ext4.Msg.prompt() dialog box.
 */
public class MessagePrompt extends Window<MessagePrompt.ElementCache>
{
    public MessagePrompt(String title, WebDriver driver)
    {
        super(title, driver);
    }

    public String getValue()
    {
        return elementCache().input.getValue();
    }

    public MessagePrompt setValue(String value)
    {
        elementCache().input.setValue(value);
        return this;
    }

    public void clickOK()
    {
        clickButton("OK", true);
    }

    public void clickCancel()
    {
        clickButton("Cancel", true);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    class ElementCache extends Window.ElementCache
    {
        protected Input input = Input(Locator.tagWithAttribute("input", "type", "text"), getDriver()).findWhenNeeded(this);
    }
}
