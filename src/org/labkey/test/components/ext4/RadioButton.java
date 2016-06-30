package org.labkey.test.components.ext4;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebElement;

public class RadioButton extends Checkbox
{
    private RadioButton(WebElement radio)
    {
        super(radio);
    }

    public static RadioButtonFinder RadioButton()
    {
        return new RadioButtonFinder();
    }

    protected void assertElementType()
    {
        String backgroundImage = getComponentElement().getCssValue("background-image");
        Assert.assertTrue("Not a radio button: " + getComponentElement().toString(), backgroundImage.contains("radio"));
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
