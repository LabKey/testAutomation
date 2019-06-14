/*
 * Copyright (c) 2017-2019 LabKey Corporation
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

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.labkey.test.components.api.ProjectMenu;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.selenium.WebDriverUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ReclickingWebElement extends WebElementDecorator
{
    public ReclickingWebElement(WebElement decoratedElement)
    {
        super(decoratedElement);
    }

    @Override
    public void click()
    {
        try
        {
            super.click();
        }
        catch (ElementClickInterceptedException tryAgain)
        {
            if (getDriver() != null)
            {
                TestLogger.debug("Retry click: " + tryAgain.getMessage().split("\n")[0]);
                revealElement(getWrappedElement());
                super.click();
            }
            else
            {
                throw tryAgain;
            }
        }
        catch (ElementNotInteractableException e)
        {
            if (getDriver() != null
                    && getDriver().getClass().isAssignableFrom(FirefoxDriver.class))
            {
                String tagName = getWrappedElement().getTagName();
                List<String> classes = Arrays.asList(getWrappedElement().getAttribute("class").toLowerCase().trim().split("\\s"));
                if ("tr".equals(tagName))
                {
                    clickRowInFirefox();
                }
                else if ("area".equals(tagName))
                {
                    clickImageMapArea();
                }
                else if ("a".equals(tagName) && classes.contains("point")) // probably an SVG point
                {
                    actionClick();
                }
                else
                {
                    throw e;
                }
            }
            else
            {
                throw e;
            }
        }
    }

    private void actionClick()
    {
        new Actions(getDriver()).moveToElement(getWrappedElement()).click().perform();
    }

    /**
     * Geckodriver clicks at the center of the entire img rather that the center of the desired area.
     * We need to calculate the proper location
     */
    private void clickImageMapArea()
    {
        String shape = getWrappedElement().getAttribute("shape");
        if (shape.equals("default"))
        {
            throw new IllegalArgumentException("Refusing to click the 'default' <area> of an image map. Can't guarantee that it won't click a different <area> instead");
        }

        final Point areaCenter = getAreaCenter();
        Dimension mapSize = new FluentWait<>(getWrappedElement()).withTimeout(Duration.ofSeconds(5)).withMessage("image map to load").until(el ->
        {
            Dimension size = el.getSize(); // Location/size of an individual area is the same as the entire map
            if (areaCenter.getY() > size.getHeight() || areaCenter.getX() > size.getWidth())
                return null; // Wait for image to load to avoid "WebDriverException: TypeError: rect is undefined"
            return size;
        });
        Point mapCenter = new Point(mapSize.getWidth() / 2, mapSize.getHeight() / 2);
        int xOffset = areaCenter.getX() - mapCenter.getX();
        int yOffset = areaCenter.getY() - mapCenter.getY();
        new Actions(getDriver()).moveToElement(getWrappedElement(), xOffset, yOffset).click().perform();
    }

    /**
     * Calculate the center of a convex <area> in an image-map. Can handle 'rect', 'circle', and 'poly' shapes
     * Doc: https://www.w3schools.com/tags/tag_area.asp
     * TODO: Implement this formula for concave polygons [https://en.wikipedia.org/wiki/Centroid#Of_a_polygon]
     * @return The center point of the area element relative to the image
     */
    @NotNull
    private Point getAreaCenter()
    {
        List<Integer> coords = Arrays.stream(getWrappedElement().getAttribute("coords").split(",")).map(Integer::parseInt).collect(Collectors.toList());
        Integer minX = Integer.MAX_VALUE;
        Integer maxX = 0;
        Integer minY = Integer.MAX_VALUE;
        Integer maxY = 0;
        for (int i = 0; i + 1 < coords.size(); i = i + 2)
        {
            Integer x = coords.get(i);
            if (x > maxX)
                maxX = x;
            if (x < minX)
                minX = x;
            Integer y = coords.get(i + 1);
            if (y > maxY)
                maxY = y;
            if (y < minY)
                minY = y;
        }
        return new Point((maxX + minX) / 2, (maxY + minY) / 2);
    }

    /**
     * https://bugzilla.mozilla.org/show_bug.cgi?id=1448825
     */
    private void clickRowInFirefox()
    {
        TestLogger.warn("Don't click 'tr' elements directly, use a specific child 'td': " + toString());
        List<WebElement> cells = getWrappedElement().findElements(By.xpath("./td"));
        for (WebElement cell : cells)
        {
            if (cell.isDisplayed())
            {
                cell.click();
                return;
            }
        }
        // Fallback to clicking row if we can't find a displayed cell
        getWrappedElement().click();
    }

    // Allows interaction with elements that have been obscured by floating headers or tooltips
    private void revealElement(WebElement el)
    {
        WebDriverUtils.ScrollUtil scrollUtil = new WebDriverUtils.ScrollUtil(getDriver());
        scrollUtil.scrollUnderFloatingHeader(el);
        try
        {
            new ProjectMenu(getDriver()).close(); // Project menu often gets in the way after scrolling
        }
        catch (WebDriverException ignore) {}
        new WebDriverWait(getDriver(), 10).until(ExpectedConditions.elementToBeClickable(el));
    }

    private Mutable<WebDriver> _webDriver = null;
    private WebDriver getDriver()
    {
        if (_webDriver == null)
        {
            _webDriver = new MutableObject<>(WebDriverUtils.extractWrappedDriver(getWrappedElement()));
        }
        return _webDriver.getValue();
    }
}
