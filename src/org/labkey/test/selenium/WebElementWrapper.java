/*
 * Copyright (c) 2015-2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.internal.Coordinates;
import org.openqa.selenium.internal.Locatable;
import org.openqa.selenium.internal.WrapsElement;

import java.util.List;

public abstract class WebElementWrapper implements WebElement, WrapsElement, Locatable
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

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException
    {
        return getWrappedElement().getScreenshotAs(target);
    }

    @Override
    public Rectangle getRect()
    {
        return getWrappedElement().getRect();
    }

    @Override
    public Coordinates getCoordinates()
    {
        return ((Locatable)getWrappedElement()).getCoordinates();
    }
}
