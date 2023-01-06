/*
 * Copyright (c) 2018-2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.react;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.List;

import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.sleep;
import static org.labkey.test.util.TestLogger.log;

public class FilteringReactSelect extends BaseReactSelect<FilteringReactSelect>
{
    public FilteringReactSelect(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    static public SearchingReactSelectFinder finder(WebDriver driver)
    {
        return new SearchingReactSelectFinder(driver);
    }

    @Override
    protected FilteringReactSelect getThis()
    {
        return this;
    }

    /* types the value into the input as a filter,
     * then clicks the option containing that value, and
     * waits for that value to show in a selectValueLabel (which is usually how a single-select shows)*/
    public FilteringReactSelect typeAheadSelect(String value)
    {
        return typeAheadSelect(value, value, value);
    }

    public FilteringReactSelect typeAheadSelect(String value, String optionText, String selectedOptionLabel)
    {
        waitForLoaded();
        scrollIntoView();
        open();

        var elementToClick = ReactSelect.Locators.options.containing(optionText);
        var elementToWaitFor = getValueLabelLocator().containing(selectedOptionLabel);

        List<WebElement> options = setFilter(value);

        WebElement optionToClick = null;
        int tryCount = 0;
        while (null == optionToClick)
        {
            tryCount++;
            try
            {
                optionToClick = elementToClick.waitForElement(elementCache().selectMenu, 2500);
            }
            catch (NoSuchElementException nse)
            {
                if (tryCount < 6)
                {
                    close();
                    open();
                    options = setFilter(value);
                }
                else
                {
                    List<String> optionsTexts = getWrapper().getTexts(options);
                    throw new NoSuchElementException("Failed to find option '" + elementToClick + "' element. Found:" + optionsTexts.toString(), nse);
                }
            }
        }

        try
        {
            getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(optionToClick));
            optionToClick.click();
        }
        catch (StaleElementReferenceException sere)
        {
            throw sere;
        }

        if (!WebDriverWrapper.waitFor(()-> !isExpanded(), 1500))   // give it a moment to close, blur if it hasn't
        {
            log("Select didn't collapse after selecting an option. Closing it now.");
            getWrapper().fireEvent(elementCache().input, WebDriverWrapper.SeleniumEvent.blur);
        }

        WebDriverWrapper.waitFor(()-> elementToWaitFor.findElementOrNull(getComponentElement()) != null,
                () -> "Expected selection [" + elementToWaitFor.getLoggableDescription() + "] was not found. Selected value(s) are:" + getSelections(),
                WAIT_FOR_JAVASCRIPT);

        close();
        return this;
    }

    public FilteringReactSelect filterSelect(String value) // adds text for usage in cases where the item isn't in the dropdown and the text is editable
    {
        return filterSelect(value, getValueLabelLocator().containing(value));
    }

    /* for use with editable instances of a reactSelect, where the options aren't shown
       unless type-ahead filter information is keyed in first */
    public FilteringReactSelect filterSelect(String value, Locator elementToWaitFor)
    {
        waitForLoaded();
        scrollIntoView();
        WebElement success = null;
        int tryCount = 0;
        while (null == success && tryCount < 6)
        {
            tryCount++;
            open();

            if (isMulti() || hasValue())
                sleep(250);
            setFilter(value);

            WebElement elemToClick = Locator.waitForAnyElement(
                    new FluentWait<SearchContext>(getComponentElement()).withTimeout(Duration.ofMillis(WAIT_FOR_JAVASCRIPT)),
                    Locators.options.containing(value));

            log("clicking item with value [" +value+"]");
            getWrapper().scrollIntoView(elemToClick);

            WebDriverWrapper.waitFor(()-> {
                try
                {
                    if (isExpanded())
                        elemToClick.click();
                    sleep(250);
                    return !isExpanded();
                }
                catch (StaleElementReferenceException retry)
                {
                    return false;
                }
            },"failed to select item "+ elemToClick.getAttribute("class")  +" by clicking", WAIT_FOR_JAVASCRIPT);

            success = elementToWaitFor.findElement(getComponentElement());
        }

        if (success == null)
            log("Expected selection was not found. Selected value(s) are:" + getSelections());

        close();
        return this;
    }

    private List<WebElement> setFilter(String value)
    {
        elementCache().input.sendKeys(value);
        long filterStart = System.currentTimeMillis();
        WebDriverWrapper.waitFor(()-> {
            List<WebElement> options = elementCache().getOptions();
            return options.size() > 0 &&
                    !isLoading() || options.stream().anyMatch((a)-> a.getText().contains(value));
        }, WAIT_FOR_JAVASCRIPT);
        long elapsed = System.currentTimeMillis() - filterStart;
        getWrapper().log("It took [" + elapsed + "] msec to filter options (seeking {"+value+"}) in filtering select");
        return elementCache().getOptions();
    }

    /**
     * sets the filter and captures the text for each resulting option
     * @param filter    the text to set in the filter input
     * @return  A list of strings for each option shown for that filter expression
     */
    public List<String> getOptionsForFilter(String filter)
    {
        return getWrapper().getTexts(setFilter(filter));
    }

    public static class SearchingReactSelectFinder extends BaseReactSelect.BaseReactSelectFinder<FilteringReactSelect>
    {
        private SearchingReactSelectFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected FilteringReactSelect construct(WebElement el, WebDriver driver)
        {
            return new FilteringReactSelect(el, driver);
        }
    }
}
