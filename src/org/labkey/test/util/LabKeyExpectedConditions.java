/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

import java.util.List;

public class LabKeyExpectedConditions
{
    private LabKeyExpectedConditions(){}


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
                try
                {
                    return loc.findElement(driver).getCssValue("position").equals("static");
                }
                catch (StaleElementReferenceException ignore)
                {
                    return false;
                }
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
                try
                {
                    firstPosition = el.getLocation();
                    Thread.sleep(100);
                    secondPosition = el.getLocation();
                }
                catch (InterruptedException recheck)
                {
                    return false;
                }

                return secondPosition.equals(firstPosition);
            }

            @Override
            public String toString()
            {
                return "movement of element";
            }
        };
    }

    /**
     * An expectation for checking that a row of the dumbster data region is expanded
     *
     * @param emailIndex one-based index of the expanding email table row
     * @return true when email body is visible
     */
    public static ExpectedCondition<Boolean> emailIsExpanded(final int emailIndex) {
        return new ExpectedCondition<Boolean>(){
            @Override
            public Boolean apply(WebDriver driver)
            {
                return !driver.findElement(By.id("email_body_" + emailIndex)).getCssValue("display").equals("none");
            }

            @Override
            public String toString()
            {
                return "expansion of dumbster row " + emailIndex;
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
                try{
                    el = loc.findElement(driver);
                }
                catch (ElementNotFoundException ignore)
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

    public static ExpectedCondition<WebElement> dataRegionPanelIsExpanded(@Nullable Locator.IdLocator dataRegion)
    {
        final Locator.IdLocator _dataRegion = dataRegion == null ? Locator.id("") : dataRegion;
        return new ExpectedCondition<WebElement>()
        {
            @Override
            public WebElement apply(WebDriver d)
            {
                List<WebElement> els = _dataRegion.toCssLocator().append(".labkey-data-region-header td.labkey-ribbon > div:not(.x-hide-display)").findElements(d);
                for (WebElement el : els)
                {
                    try
                    {
                        if ("static".equalsIgnoreCase(el.getCssValue("position")) && el.isDisplayed())
                            return el;
                    }
                    catch (StaleElementReferenceException retry)
                    {
                        return null;
                    }
                }
                return null;
            }

            @Override
            public String toString()
            {
                return "data region panel to open";
            }
        };
    }
}
