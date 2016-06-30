package org.labkey.test.components.ext4;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebElement;

public class Checkbox extends org.labkey.test.components.html.Checkbox
{
    public Checkbox(WebElement checkbox)
    {
        super(checkbox);
    }

    public static CheckboxFinder Ext4Checkbox()
    {
        return new CheckboxFinder();
    }

    public boolean isChecked()
    {
        assertElementType();

        String atlasPosition = getComponentElement().getCssValue("background-position");
        String atlasYOffset = atlasPosition.split(" ")[1];
        return atlasYOffset.contains("-"); // Probably '-13px' or '-26px'. Unchecked states are all at offset zero
    }

    protected void assertElementType()
    {
        String backgroundImage = getComponentElement().getCssValue("background-image");
        Assert.assertTrue("Not a checkbox or radio button: " + getComponentElement().toString(), backgroundImage.contains("checkbox") || backgroundImage.contains("radio"));
    }

    public static class CheckboxFinder extends FormItemFinder<Checkbox, CheckboxFinder>
    {
        @Override
        protected Checkbox construct(WebElement el)
        {
            return new Checkbox(el);
        }

        protected Locator.XPathLocator itemLoc()
        {
            return Locator.tagWithClass("input", Ext4Helper.getCssPrefix() + "form-checkbox");
        }
    }
}
