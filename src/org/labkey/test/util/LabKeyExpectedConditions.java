/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

public abstract class LabKeyExpectedConditions
{
    /**
     * An expectation for checking that an element has stopped moving
     *
     * @param loc the container element which should have css style, "position: static"
     * @return true when animation is complete
     */
    public static ExpectedCondition<Boolean> animationIsDone(final Locator loc) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver)
            {
                Point firstPosition;
                Point secondPosition;
                Dimension firstDimension;
                Dimension secondDimension;
                WebElement el = loc.findElement(driver);
                try
                {
                    firstDimension = el.getSize();
                    firstPosition = el.getLocation();
                    Thread.sleep(100);
                    secondDimension = el.getSize();
                    secondPosition = el.getLocation();
                }
                catch (StaleElementReferenceException | NoSuchElementException recheck)
                {
                    return false;
                }
                catch (InterruptedException fail)
                {
                    throw new IllegalStateException(fail);
                }

                return secondPosition.equals(firstPosition) && secondDimension.equals(firstDimension);
            }

            @Override
            public String toString()
            {
                return "animation of element: " + loc.getLoggableDescription();
            }
        };
    }

    /**
     * Another expectation for checking that an element has stopped moving
     *
     * @param el the element who's position changes
     * @return true when animation is complete
     */
    public static ExpectedCondition<Boolean> animationIsDone(final WebElement el) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver)
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

                return secondPosition.equals(firstPosition) && secondDimension.equals(firstDimension);
            }

            @Override
            public String toString()
            {
                return "movement of element";
            }
        };
    }

    public static ExpectedCondition<WebElement> elementIsEnabled(final Locator loc) {
        return new ExpectedCondition<WebElement>()
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

    public static ExpectedCondition<WebElement> elementPresent(final Locator loc, final SearchContext context) {
        return new ExpectedCondition<WebElement>()
        {
            @Override
            public WebElement apply(WebDriver ignoreDriver)
            {
                return loc.findElementOrNull(context);
            }

            @Override
            public String toString()
            {
                return "element '" + loc.getLoggableDescription() + "'" +
                        "to be present in context: " + context.toString();
            }
        };
    }

    public static ExpectedCondition<Boolean> clickUntilStale(final WebElement element)
    {
        return new ExpectedCondition<Boolean>()
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
}
