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
package org.labkey.test.util.selenium;

import org.apache.commons.lang3.SystemUtils;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.WrapsElement;
import org.openqa.selenium.interactions.Locatable;

import java.util.List;
import java.util.stream.Collectors;

public abstract class WebDriverUtils
{
    /**
     * Modifier key that differs by OS. Has a couple of common uses:
     * <ul>
     * <li> Multi-select:
     * <pre><code>
     *    new Actions(getDriver())
     *        .keyDown(MODIFIER_KEY)
     *        .click(el1)
     *        .click(el2)
     *        .keyUp(MODIFIER_KEY)
     *        .perform();
     * </code></pre>
     * </li>
     * <li> Keyboard shortcuts (e.g. select-all):
     * <pre><code>
     *    new Actions(getDriver())
     *        .keyDown(MODIFIER_KEY)
     *        .sendKeys(input, "a")
     *        .keyUp(MODIFIER_KEY)
     *        .perform();
     * </code></pre>
     * </li>
     * </ul>
     */
    public static final Keys MODIFIER_KEY = SystemUtils.IS_OS_MAC ? Keys.COMMAND : Keys.CONTROL;

    public static class ScrollUtil
    {
        private final WebDriver _webDriver;

        public ScrollUtil(WebDriver webDriver)
        {
            _webDriver = webDriver;
        }

        public boolean scrollUnderFloatingHeader(WebElement blockedElement)
        {
            List<WebElement> floatingHeaders = Locator.findElements(_webDriver,
                Locators.floatingHeaderContainer(),
                Locators.appFloatingHeader(),
                DataRegionTable.Locators.floatingHeader().notHidden());

            int headerHeight = 0;
            for (WebElement floatingHeader : floatingHeaders)
            {
                headerHeight += floatingHeader.getSize().getHeight();
            }
            if (headerHeight > 0)
            {
                int elYInViewPort = blockedElement.getLocation().getY() - getWindowScrollY().intValue();
                if (headerHeight > elYInViewPort)
                {
                    TestLogger.debug("Scrolled under floating headers:\n" + floatingHeaders.stream().map(WebElement::toString).collect(Collectors.joining("\n")));
                    ((Locatable) blockedElement).getCoordinates().inViewPort(); // 'inViewPort()' will scroll element into view
                    return true;
                }
            }
            return false;
        }

        private Long getWindowScrollY()
        {
            Number N = (Number) ((JavascriptExecutor)_webDriver).executeScript("return window.scrollY;");
            return null==N ? null : N.longValue();
        }

        public WebElement scrollIntoView(WebElement el)
        {
            ((JavascriptExecutor)_webDriver).executeScript("arguments[0].scrollIntoView();", el);
            return el;
        }

        public WebElement scrollIntoView(WebElement el, Boolean alignToTop)
        {
            ((JavascriptExecutor)_webDriver).executeScript("arguments[0].scrollIntoView(arguments[1]);", el, alignToTop);
            return el;
        }

        public void scrollBy(Integer x, Integer y)
        {
            ((JavascriptExecutor)_webDriver).executeScript("window.scrollBy(" + x.toString() +", " + y.toString() + ");");
        }
    }

    /**
     * Extract a WebDriver instance from an arbitrarily wrapped object
     * @param peeling Object that wraps a WebDriver. Typically a Component, SearchContext, or WebElement
     * @return WebDriver instance or null if none is found
     */
    public static WebDriver extractWrappedDriver(Object peeling)
    {
        while (peeling instanceof WrapsElement)
        {
            peeling = ((WrapsElement) peeling).getWrappedElement();
        }
        while (peeling instanceof WrapsDriver)
        {
            peeling = ((WrapsDriver) peeling).getWrappedDriver();
        }
        if (peeling instanceof WebDriver)
            return (WebDriver) peeling;
        else
            return null;
    }
}
