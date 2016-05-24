package org.labkey.test.components.ext4;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebElement;

public class RadioButton extends FormItem
{
    WebElement _el;

    private RadioButton(WebElement radio)
    {
        this._el = radio;
    }

    public static RadioButtonFinder RadioButton()
    {
        return new RadioButtonFinder();
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    public boolean isEnabled()
    {
        return getComponentElement().isEnabled();
    }

    public boolean isSelected()
    {
        return isChecked();
    }

    public boolean isDisplayed()
    {
        return getComponentElement().isDisplayed();
    }

    public boolean isChecked()
    {
        assertElementType();

        String atlasPosition = _el.getCssValue("background-position");
        String atlasYOffset = atlasPosition.split(" ")[1];
        return atlasYOffset.contains("-"); // Probably '-13px' or '-26px'. Unchecked states are all at offset zero
    }

    public void check()
    {
        if (!isChecked())
            _el.click();
    }

    protected void assertElementType()
    {
        String backgroundImage = _el.getCssValue("background-image");
        Assert.assertTrue("Not a radio button: " + _el.toString(), backgroundImage.contains("radio"));
    }

    public static class RadioButtonFinder extends FormItemFinder<RadioButton, RadioButtonFinder>
    {
        protected RadioButtonFinder() {}

        @Override
        protected RadioButton construct(WebElement el)
        {
            return new RadioButton(el);
        }

        protected Locator.XPathLocator itemLoc()
        {
            return Locator.tagWithClass("input", Ext4Helper.getCssPrefix() + "form-radio");
        }
    }
}
