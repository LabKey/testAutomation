package org.labkey.test.components.ext4;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

public class Checkbox extends Component
{
    WebElement _el;

    public Checkbox(String label, SearchContext context)
    {
        this(findCheckbox(label, context));
    }

    public Checkbox(WebElement checkbox)
    {
        this._el = checkbox;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
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

    private static WebElement findCheckbox(String label, SearchContext context)
    {
        try
        {
            return Locator.xpath("//input[contains(@class,'" + Ext4Helper.getCssPrefix() + "form-checkbox')][../label[text()='" + label + "']]").findElement(context);
        }
        catch (NoSuchElementException other)
        {
            return Locator.xpath("//input[contains(@class,'" + Ext4Helper.getCssPrefix() + "form-checkbox')][../../td/label[text()='" + label + "']]").findElement(context);
        }
    }
}
