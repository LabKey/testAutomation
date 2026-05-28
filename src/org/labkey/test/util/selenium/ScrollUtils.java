/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
package org.labkey.test.util.selenium;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Locatable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ScrollUtils
{
    private ScrollUtils() {}

    public static boolean scrollUnderStickyFormButtons(WebElement webElement)
    {
        WebDriver webDriver = extractWebDriver(webElement);
        Optional<WebElement> formButtons = Locator.css(".form-buttons").findOptionalElement(webDriver);

        if (formButtons.isPresent())
        {
            int elY = webElement.getLocation().getY();
            int height = webElement.getSize().getHeight();
            int bottom = elY + height;
            int formButtonsY = formButtons.get().getLocation().getY();

            // If the bottom of our element is past the top of the FormButtons element, then it's at least partially
            // obscured, so we should scroll the element into view.
            if (bottom > formButtonsY)
            {
                TestLogger.debug("Scrolled under sticky form buttons");
                scrollIntoView(webElement);
                return true;
            }
        }

        return false;
    }

    public static boolean scrollUnderFloatingHeader(WebElement targetElement)
    {
        WebDriver webDriver = extractWebDriver(targetElement);

        List<WebElement> floatingHeaders = Locator.findElements(webDriver,
                Locators.floatingHeaderContainer(),
                Locators.appFloatingHeader(),
                Locators.domainDesignerFloatingHeader(),
                DataRegionTable.Locators.floatingHeader().notHidden(),
                Locators.floatingGridHeader);

        if (!floatingHeaders.isEmpty())
        {
            Rectangle rect = targetElement.getRect();

            if (floatingHeaders.stream().anyMatch(headerEl -> rectanglesOverlap(rect, headerEl.getRect())))
            {
                TestLogger.debug("Scrolled under floating headers:\n" + floatingHeaders.stream().map(WebElement::toString).collect(Collectors.joining("\n")));
                scrollIntoViewPort(targetElement);
                return true;
            }
        }
        return false;
    }

    /**
     * An alternate method for scrolling an element into view. Tends to get more of the element into view.
     *
     * @param targetElement the element to scroll into view
     * @return the target element
     */
    public static WebElement scrollIntoViewPort(WebElement targetElement)
    {
        ((Locatable) targetElement).getCoordinates().inViewPort(); // 'inViewPort()' will scroll element into view
        return targetElement;
    }

    private static boolean rectanglesOverlap(Rectangle r1, Rectangle r2)
    {
        return r1.getX() < r2.getX() + r2.getWidth() && r2.getX() < r1.getX() + r1.getWidth()
            && r1.getY() < r2.getY() + r2.getHeight() && r2.getY() < r1.getY() + r1.getHeight();
    }

    public static Long getWindowScrollY(WebDriver webDriver)
    {
        Number N = (Number) executeScript(webDriver, "return window.scrollY;");
        return null == N ? null : N.longValue();
    }

    public static void scrollBy(WebDriver webDriver, Integer x, Integer y)
    {
        executeScript(webDriver, "window.scrollBy(arguments[0], arguments[1]);", x, y);
    }

    public static void scrollTo(WebDriver webDriver, Integer x, Integer y)
    {
        executeScript(webDriver, "window.scrollTo(arguments[0], arguments[1]);", x, y);
    }

    public static WebElement scrollIntoView(WebElement webElement)
    {
        executeScript(webElement, "arguments[0].scrollIntoView();", webElement);
        return webElement;
    }

    public static WebElement scrollIntoView(WebElement webElement, Boolean alignToTop)
    {
        executeScript(webElement, "arguments[0].scrollIntoView(arguments[1]);", webElement, alignToTop);
        return webElement;
    }

    public static WebElement scrollIntoView(WebElement webElement, Alignment verticalAlignment, Alignment horizontalAlignment)
    {
        executeScript(webElement, "arguments[0].scrollIntoView({block: arguments[1], inline: arguments[2]});",
                webElement, verticalAlignment.toString(), horizontalAlignment.toString());
        return webElement;
    }

    public static WebElement scrollToMiddle(WebElement webElement)
    {
        String scrollYToMiddle = """
                var viewPortHeight = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);
                var elementTop = arguments[0].getBoundingClientRect().top;
                window.scrollBy(0, elementTop-(viewPortHeight/2));""";

        executeScript(webElement, scrollYToMiddle, webElement);
        return webElement;
    }

    private static Object executeScript(WebDriver _webDriver, @Language("JavaScript") String script, Object... args)
    {
        return ((JavascriptExecutor) _webDriver).executeScript(script, args);
    }

    private static Object executeScript(WebElement webElement, @Language("JavaScript") String script, Object... args)
    {
        return executeScript(extractWebDriver(webElement), script, args);
    }

    @NotNull
    private static WebDriver extractWebDriver(WebElement webElement)
    {
        return Objects.requireNonNull(WebDriverUtils.extractWrappedDriver(webElement));
    }

    public enum Alignment
    {
        start, center, end, nearest
    }
}
