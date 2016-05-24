package org.labkey.test.components.ext4;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebElement;

public class Checkbox extends FormItem
{
    WebElement _el;

    public Checkbox(WebElement checkbox)
    {
        this._el = checkbox;
    }

    public static CheckboxFinder Checkbox()
    {
        return new CheckboxFinder();
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

    public void uncheck()
    {
        if (isChecked())
            _el.click();
    }

    public void set(boolean checked)
    {
        if (checked)
            check();
        else
            uncheck();
    }

    protected void assertElementType()
    {
        String backgroundImage = _el.getCssValue("background-image");
        Assert.assertTrue("Not a checkbox or radio button: " + _el.toString(), backgroundImage.contains("checkbox") || backgroundImage.contains("radio"));
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
