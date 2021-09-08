/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
package org.labkey.test.util;

import org.labkey.test.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class LabKeyExpectedConditions
{
    private LabKeyExpectedConditions()
    {
        // Utility class
    }

    /**
     * An expectation for checking that an element has stopped moving
     *
     * @param loc the container element which should have css style, "position: static"
     * @return element when animation is complete
     */
    public static ExpectedCondition<WebElement> animationIsDone(final By loc) {
        return new ExpectedCondition<>() {
            @Override
            public WebElement apply(WebDriver driver)
            {
                try
                {
                    WebElement el = loc.findElement(driver);
                    return animationIsDone(el).apply(driver);
                }
                catch (StaleElementReferenceException recheck)
                {
                    return null;
                }
            }

            @Override
            public String toString()
            {
                return "animation of element: " + loc.toString();
            }
        };
    }

    /**
     * Another expectation for checking that an element has stopped moving
     *
     * @param el the element who's position changes
     * @return the element when animation is complete
     */
    public static ExpectedCondition<WebElement> animationIsDone(final WebElement el) {
        return new ExpectedCondition<>() {
            @Override
            public WebElement apply(WebDriver driver)
            {
                Point firstPosition;
                Point secondPosition;
                Dimension firstDimension;
                Dimension secondDimension;
                try
                {
                    firstDimension = el.getSize();
                    firstPosition = el.getLocation();
                    Thread.sleep(100);
                    secondDimension = el.getSize();
                    secondPosition = el.getLocation();
                }
                catch (InterruptedException fail)
                {
                    throw new IllegalStateException(fail);
                }

                if (secondPosition.equals(firstPosition) && secondDimension.equals(firstDimension))
                    return el;
                else
                    return null;
            }

            @Override
            public String toString()
            {
                return "movement of element";
            }
        };
    }

    public static ExpectedCondition<WebElement> elementIsEnabled(final Locator loc) {
        return new ExpectedCondition<>()
        {
            @Override
            public WebElement apply(WebDriver driver)
            {
                WebElement el;
                try
                {
                    el = loc.findElement(driver);
                }
                catch (NoSuchElementException ignore)
                {
                    return null;
                }

                if (el.isEnabled() && !el.getAttribute("class").contains("disabled"))
                    return el;
                else
                    return null;
            }

            @Override
            public String toString()
            {
                return "element to be enabled: " + loc.getLoggableDescription();
            }
        };
    }

    public static ExpectedCondition<Boolean> clickUntilStale(final WebElement element)
    {
        return new ExpectedCondition<>()
        {
            ExpectedCondition<Boolean> staleCheck = ExpectedConditions.stalenessOf(element);

            @Override
            public Boolean apply(WebDriver ignored)
            {
                try
                {
                    element.click();
                    return staleCheck.apply(ignored);
                }
                catch (StaleElementReferenceException success)
                {
                    return true;
                }
            }

            @Override
            public String toString()
            {
                return staleCheck.toString() + " after clicking";
            }
        };
    }

    /**
     * Wraps {@link ExpectedConditions#visibilityOfAllElements(WebElement...)}
     * This expectations accounts for the behavior of LabKey WebElement wrappers, which will throw if you attempt to
     * inspect them before the element has appeared.
     *
     * @param elements list of WebElements
     * @return the list of WebElements once they are located
     * @see org.labkey.test.selenium.LazyWebElement
     */
    public static ExpectedCondition<List<WebElement>> visibilityOfAllElements(WebElement... elements)
    {
        return new ExpectedCondition<>()
        {
            final ExpectedCondition<List<WebElement>> wrapped = ExpectedConditions.visibilityOfAllElements(elements);

            @Override
            public List<WebElement> apply(WebDriver driver)
            {
                try
                {
                    return wrapped.apply(driver);
                }
                catch (StaleElementReferenceException | NoSuchElementException ignore)
                {
                    return null;
                }
            }

            @Override
            public String toString()
            {
                return wrapped.toString();
            }
        };
    }

    /**
     * Wraps {@link ExpectedConditions#stalenessOf(WebElement)}
     * Firefox occasionally throws "NoSuchElementException: Web element reference not seen before"
     * for short lived elements.
     *
     * @param element WebElement that should go stale.
     * @return false if the element is still attached to the DOM, true otherwise.
     */
    public static ExpectedCondition<Boolean> stalenessOf(WebElement element)
    {
        return new ExpectedCondition<>()
        {
            final ExpectedCondition<Boolean> wrapped = ExpectedConditions.stalenessOf(element);

            @Override
            public Boolean apply(WebDriver driver)
            {
                try
                {
                    return wrapped.apply(driver);
                }
                catch (NoSuchElementException ignore)
                {
                    // Firefox sometimes throws the wrong exception.
                    return true;
                }
            }

            @Override
            public String toString()
            {
                return wrapped.toString();
            }
        };
    }

    /**
     * Wraps {@link ExpectedConditions#visibilityOf(WebElement)} and {@link ExpectedConditions#invisibilityOf(WebElement)}
     *
     * @param element WebElement that should become visible or invisible.
     * @param visible 'true' if the element is expected to become visible, 'false' if it not
     */
    public static ExpectedCondition<?> visibilityOf(WebElement element, boolean visible)
    {
        return visible
                ? ExpectedConditions.visibilityOf(element)
                : ExpectedConditions.invisibilityOf(element);
    }

}
