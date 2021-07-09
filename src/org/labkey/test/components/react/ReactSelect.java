/*
 * Copyright (c) 2016-2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.react;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.Locator;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertTrue;
import static org.labkey.test.WebDriverWrapper.sleep;

public class ReactSelect extends BaseReactSelect<ReactSelect>
{
    private Function<String, Locator> _optionLocFactory = Locators.options::withText;

    public ReactSelect(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    static public ReactSelectFinder finder(WebDriver driver)
    {
        return new ReactSelectFinder(driver);
    }

    public ReactSelect setOptionLocator(Function<String, Locator> optionLocFactory)
    {
        _optionLocFactory = optionLocFactory;
        return this;
    }

    public void select(String option)
    {
        waitForLoaded();
        scrollIntoView();
        open();
        clickOption(option);
        waitForClosed();
    }

    protected void clickOption(String option)
    {
        WebElement optionEl = null;
        int tryCount = 0;

        while (null == optionEl)
        {
            tryCount++;
            try
            {
                optionEl = elementCache().findOption(option);
                _wrapper.scrollIntoView(optionEl);
            }
            catch (NoSuchElementException nse)
            {
                if (tryCount < 6)
                {
                    close();
                    open();
                }
                else
                {
                    List<String> optionsTexts = getWrapper().getTexts(elementCache().getOptions());
                    throw new RuntimeException("Failed to find option '" + option + "' element. Found:" + optionsTexts.toString(), nse);
                }
            }
            catch (StaleElementReferenceException sere)
            {
                TestLogger.debug("optionEl went stale, probably while attempting to scroll it into view");
                sleep(500);
            }
        }
        TestLogger.debug("Found optionEl after " + tryCount + " tries");

        for (int i = 0; i < 5 && !optionEl.isDisplayed(); i++)
        {
            sleep(500);
            _wrapper.scrollIntoView(optionEl);
            TestLogger.debug("scroll optionEl into view, attempt " + i);
        }

        assertTrue("Expected '" + option + "' to be displayed.", optionEl.isDisplayed());
        sleep(500); // either react or the test is moving to fast/slow for one another
        TestLogger.debug("optionEl is displayed, clicking");
        optionEl.click();

        new FluentWait<>(_wrapper.getDriver()).withTimeout(Duration.ofSeconds(1)).until(ExpectedConditions.stalenessOf(optionEl));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BaseReactSelect<ReactSelect>.ElementCache
    {
        @Override
        @NotNull
        WebElement findOption(String option)
        {
            Locator loc = _optionLocFactory.apply(option);
            return loc.findElement(selectMenu);
        }
    }

    public static class ReactSelectFinder extends BaseReactSelect.BaseReactSelectFinder<ReactSelect>
    {
        private ReactSelectFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected ReactSelect construct(WebElement el, WebDriver driver)
        {
            return new ReactSelect(el, driver);
        }
    }
}

