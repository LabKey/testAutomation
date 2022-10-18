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
import org.junit.Assert;
import org.junit.Test;
import org.labkey.test.Locator;
import org.labkey.test.components.core.ProjectMenu;
import org.labkey.test.util.LabKeyExpectedConditions;
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
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReclickingWebElement extends WebElementDecorator
{
    // Extract the element info from ElementClickInterceptedException message.
    private static final Pattern interceptingElPattern = Pattern.compile("Element .* is not clickable .*<(?<tag>[a-zA-Z]+) (?<attributes>.+)> obscures it");
    private static final Pattern elAttributePattern = Pattern.compile("(?<name>[a-zA-Z-]+)=\"(?<value>[^\"]+)\"");

    public ReclickingWebElement(@NotNull WebElement decoratedElement)
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
        catch (ElementClickInterceptedException ex)
        {
            if (getDriver() != null)
            {
                final String shortMessage = ex.getMessage().split("\n")[0];
                TestLogger.debug("Retry click: " + shortMessage);
                revealElement(getWrappedElement(), shortMessage);
                super.click();
            }
            else
            {
                throw ex;
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
                    if (!clickRowInFirefox())
                    {
                        throw e;
                    }
                }
                else if ("area".equals(tagName))
                {
                    clickImageMapArea();
                }
                else if ("a".equals(tagName) && classes.contains("point")) // probably an SVG point
                {
                    actionClick();
                }
                else if (e.getRawMessage().contains("could not be scrolled into view") && getWrappedElement().isDisplayed())
                {
                    // Add some information to help test developer find a better element.
                    throw new ElementNotInteractableException("Click failed; try clicking a child element. Firefox doesn't like clicking certain wrapping elements\n" + e.getRawMessage(), e);
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
     * Calculate the center of a convex &lt;area&gt; in an image-map. Can handle 'rect', 'circle', and 'poly' shapes
     * Doc: <a href="https://www.w3schools.com/tags/tag_area.asp">tag_area</a>
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
    private boolean clickRowInFirefox()
    {
        TestLogger.warn("Don't click 'tr' elements directly, use a specific child 'td': " + toString());
        List<WebElement> cells = getWrappedElement().findElements(By.xpath("./td"));
        for (WebElement cell : cells)
        {
            if (cell.isDisplayed())
            {
                cell.click();
                return true;
            }
        }
        return false;
    }

    // Allows interaction with elements that have been obscured by floating headers or tooltips
    private void revealElement(WebElement el, String shortMessage)
    {
        try
        {
            ProjectMenu.finder(getDriver()).findOptional().ifPresent(ProjectMenu::close); // Project menu often gets in the way after scrolling
        }
        catch (WebDriverException ignore) {}

        WebDriverUtils.ScrollUtil scrollUtil = new WebDriverUtils.ScrollUtil(getDriver());
        scrollUtil.scrollUnderFloatingHeader(el);

        Locator.XPathLocator interceptingElLoc = parseInterceptingElementLoc(shortMessage);
        if (interceptingElLoc != null)
        {
            List<WebElement> interceptingElements = interceptingElLoc.findElements(getDriver());
            if (!interceptingElements.isEmpty())
            {
                final ExpectedCondition<?>[] expectations = (ExpectedCondition<?>[]) interceptingElements.stream()
                        .map(interceptingElement -> ExpectedConditions.or(
                                LabKeyExpectedConditions.animationIsDone(interceptingElement),
                                ExpectedConditions.invisibilityOf(interceptingElement)
                        )).toArray(ExpectedCondition[]::new);
                new WebDriverWait(getDriver(), Duration.ofSeconds(5))
                        .until(ExpectedConditions.and(expectations));
            }
        }
    }

    private static Locator.XPathLocator parseInterceptingElementLoc(String shortMessage)
    {
        Locator.XPathLocator interceptingElLoc = null;
        Matcher matcher = interceptingElPattern.matcher(shortMessage);
        if (matcher.matches())
        {
            // Try to parse exception message to learn about intercepting element.
            String tag = matcher.group("tag");
            String attributes = matcher.group("attributes");
            Matcher attributeMatcher = elAttributePattern.matcher(attributes);
            interceptingElLoc = Locator.tag(tag);
            while (attributeMatcher.find())
            {
                String name = attributeMatcher.group("name");
                String value = attributeMatcher.group("value");
                interceptingElLoc = interceptingElLoc.withAttribute(name, value);
            }
        }
        return interceptingElLoc;
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

    public static class TempEceptionParser
    {
        @Test
        public void testInterceptinElLoc()
        {
            final Locator.XPathLocator xPathLocator = parseInterceptingElementLoc("Element <a href=\"something\"> is not clickable at point (732,301) because another element " +
                    "<div id=\"elId\" class=\"cls1 cls2\"> obscures it");
            Assert.assertEquals(Locator.tag("div").withAttribute("id", "elId").withAttribute("class", "cls1 cls2"), xPathLocator);
        }
    }
}
