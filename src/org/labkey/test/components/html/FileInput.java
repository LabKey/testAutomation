package org.labkey.test.components.html;

import org.junit.Assert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

public class FileInput extends Input
{
    public FileInput(WebElement el, WebDriver driver)
    {
        super(el, driver);
    }

    public void set(File file)
    {
        getWrapper().setFormElement(getComponentElement(), file);
    }

    @Override
    protected void assertElementType(WebElement el)
    {
        String type = el.getAttribute("type");
        Assert.assertEquals("Not a file input: " + el.toString(), "file", type);
    }
}
