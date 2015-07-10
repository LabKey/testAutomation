package org.labkey.test.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.WrapsElement;

import java.util.List;

public abstract class WebElementWrapper implements WebElement, WrapsElement
{
    @Override
    public void click()
    {
        getWrappedElement().click();
    }

    @Override
    public void submit()
    {
        getWrappedElement().submit();
    }

    @Override
    public void sendKeys(CharSequence... keysToSend)
    {
        getWrappedElement().sendKeys(keysToSend);
    }

    @Override
    public void clear()
    {
        getWrappedElement().clear();
    }

    @Override
    public String getTagName()
    {
        return getWrappedElement().getTagName();
    }

    @Override
    public String getAttribute(String name)
    {
        return getWrappedElement().getAttribute(name);
    }

    @Override
    public boolean isSelected()
    {
        return getWrappedElement().isSelected();
    }

    @Override
    public boolean isEnabled()
    {
        return getWrappedElement().isEnabled();
    }

    @Override
    public String getText()
    {
        return getWrappedElement().getText();
    }

    @Override
    public List<WebElement> findElements(By by)
    {
        return getWrappedElement().findElements(by);
    }

    @Override
    public WebElement findElement(By by)
    {
        return getWrappedElement().findElement(by);
    }

    @Override
    public boolean isDisplayed()
    {
        return getWrappedElement().isDisplayed();
    }

    @Override
    public Point getLocation()
    {
        return getWrappedElement().getLocation();
    }

    @Override
    public Dimension getSize()
    {
        return getWrappedElement().getSize();
    }

    @Override
    public String getCssValue(String propertyName)
    {
        return getWrappedElement().getCssValue(propertyName);
    }
}
