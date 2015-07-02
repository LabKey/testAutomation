package org.labkey.test.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;

import java.util.List;

public abstract class WebElementWrapper implements WebElement
{
    /**
     * This will be called with every attempt to interact with the WebElement
     */
    protected abstract WebElement getElement();

    @Override
    public void click()
    {
        getElement().click();
    }

    @Override
    public void submit()
    {
        getElement().submit();
    }

    @Override
    public void sendKeys(CharSequence... keysToSend)
    {
        getElement().sendKeys(keysToSend);
    }

    @Override
    public void clear()
    {
        getElement().clear();
    }

    @Override
    public String getTagName()
    {
        return getElement().getTagName();
    }

    @Override
    public String getAttribute(String name)
    {
        return getElement().getAttribute(name);
    }

    @Override
    public boolean isSelected()
    {
        return getElement().isSelected();
    }

    @Override
    public boolean isEnabled()
    {
        return getElement().isEnabled();
    }

    @Override
    public String getText()
    {
        return getElement().getText();
    }

    @Override
    public List<WebElement> findElements(By by)
    {
        return getElement().findElements(by);
    }

    @Override
    public WebElement findElement(By by)
    {
        return getElement().findElement(by);
    }

    @Override
    public boolean isDisplayed()
    {
        return getElement().isDisplayed();
    }

    @Override
    public Point getLocation()
    {
        return getElement().getLocation();
    }

    @Override
    public Dimension getSize()
    {
        return getElement().getSize();
    }

    @Override
    public String getCssValue(String propertyName)
    {
        return getElement().getCssValue(propertyName);
    }
}
