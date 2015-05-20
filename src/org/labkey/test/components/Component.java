package org.labkey.test.components;

import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;

import java.util.List;

public abstract class Component
{
    public abstract WebElement getComponentElement();

    public WebElement findElement(Locator locator)
    {
        return locator.findElement(getComponentElement());
    }

    public List<WebElement> findElements(Locator locator)
    {
        return locator.findElements(getComponentElement());
    }
}
