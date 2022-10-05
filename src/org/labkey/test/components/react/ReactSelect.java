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
import static org.labkey.test.WebDriverWrapper.waitFor;

public class ReactSelect extends BaseReactSelect<ReactSelect>
{
    private Function<String, Locator> _optionLocFactory = Locators.options::withText;

    public ReactSelect(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    public ReactSelect(ReactSelect wrapped)
    {
        this(wrapped.getComponentElement(), wrapped.getDriver());
    }

    public static ReactSelectFinder finder(WebDriver driver)
    {
        return new ReactSelectFinder(driver);
    }

    @Override
    protected ReactSelect getThis()
    {
        return this;
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

    public void typeOptionThenSelect(String option)
    {
        waitForLoaded();
        scrollIntoView();
        open();
        enterValueInTextbox(option);
        waitForLoaded();

        waitFor(()->getOptions().contains(option), String.format("Option '%s' is not in the list of options.", option), 1_000);

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
                getWrapper().scrollIntoView(optionEl);
            }
            catch (NoSuchElementException nse)
            {
                if (tryCount < 6)
                {
                    close();
                    open();

                    // Since this is a retry method try to improve the odds of finding the item in the list by entering
                    // it into the textbox, which should filter the list. Also, closing and opening the dropdown will
                    // clear any value that may have been entered in the text box, this should protect against that as well.
                    enterValueInTextbox(option);

                    // Don't know if this is will work as expected. It may take a moment for the list to populate
                    // after typing in a value.
                    waitFor(()->!getOptions().contains("Loading..."), 1_000);

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
            getWrapper().scrollIntoView(optionEl);
            TestLogger.debug("scroll optionEl into view, attempt " + i);
        }

        assertTrue("Expected '" + option + "' to be displayed.", optionEl.isDisplayed());
        sleep(500); // either react or the test is moving to fast/slow for one another
        TestLogger.debug("optionEl is displayed, clicking");
        optionEl.click();

        new FluentWait<>(getWrapper().getDriver()).withTimeout(Duration.ofSeconds(1)).until(ExpectedConditions.stalenessOf(optionEl));
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
        public ReactSelectFinder(WebDriver driver)
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

