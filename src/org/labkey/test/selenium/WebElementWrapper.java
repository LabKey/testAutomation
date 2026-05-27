/*
 * Copyright (c) 2015-2026 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Coordinates;
import org.openqa.selenium.interactions.Locatable;
import org.openqa.selenium.WrapsElement;

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
    public void sendKeys(CharSequence @NotNull ... keysToSend)
    {
        getWrappedElement().sendKeys(keysToSend);
    }

    @Override
    public void clear()
    {
        getWrappedElement().clear();
    }

    @Override
    public @NotNull String getTagName()
    {
        return getWrappedElement().getTagName();
    }

    @Override
    public String getAttribute(@NotNull String name)
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
    public @NotNull String getText()
    {
        return getWrappedElement().getText();
    }

    @Override
    public @NotNull List<WebElement> findElements(@NotNull By by)
    {
        return getWrappedElement().findElements(by);
    }

    @Override
    public @NotNull WebElement findElement(@NotNull By by)
    {
        return getWrappedElement().findElement(by);
    }

    @Override
    public boolean isDisplayed()
    {
        return getWrappedElement().isDisplayed();
    }

    @Override
    public @NotNull Point getLocation()
    {
        return getWrappedElement().getLocation();
    }

    @Override
    public @NotNull Dimension getSize()
    {
        return getWrappedElement().getSize();
    }

    @Override
    public @NotNull String getCssValue(@NotNull String propertyName)
    {
        return getWrappedElement().getCssValue(propertyName);
    }

    @Override
    public <X> @NotNull X getScreenshotAs(@NotNull OutputType<X> target) throws WebDriverException
    {
        return getWrappedElement().getScreenshotAs(target);
    }

    @Override
    public @NotNull Rectangle getRect()
    {
        return getWrappedElement().getRect();
    }

    @Override
    public String getDomProperty(@NotNull String name)
    {
        return getWrappedElement().getDomProperty(name);
    }

    @Override
    public String getDomAttribute(@NotNull String name)
    {
        return getWrappedElement().getDomAttribute(name);
    }

    @Override
    public String getAriaRole()
    {
        return getWrappedElement().getAriaRole();
    }

    @Override
    public String getAccessibleName()
    {
        return getWrappedElement().getAccessibleName();
    }

    @Override
    public @NotNull SearchContext getShadowRoot()
    {
        return getWrappedElement().getShadowRoot();
    }

    @Override
    public @NotNull Coordinates getCoordinates()
    {
        return ((Locatable)getWrappedElement()).getCoordinates();
    }
}
