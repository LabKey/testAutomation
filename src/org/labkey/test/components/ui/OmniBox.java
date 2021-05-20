/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.ui;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.selenium.RefindingWebElement;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;

public class OmniBox extends WebDriverComponent<OmniBox.ElementCache>
{
    final private WebElement _omniBoxElement;
    final private WebDriver _driver;

    public OmniBox(WebElement element, WebDriver driver)
    {
        _omniBoxElement = element;
        _driver = driver;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _omniBoxElement;
    }

    public OmniBox clearAll()
    {
        for (WebElement e : getValues())
        {
            sendClearValue();
        }

        // dismiss any filter/search/sort dropdown prompts
        if (isOpen())
            close();

        new WebDriverWait(_driver, 1).until(
                ExpectedConditions.numberOfElementsToBe(Locators.values, 0));

        return this;
    }

    public OmniBox clearLast()
    {
        int numValues = getValues().size();
        if (numValues > 0)
        {
            sendClearValue();
            new WebDriverWait(_driver, 1).until(
                    wd -> Locators.values.findElements(this).size() == numValues - 1);
        }

        return this;
    }

    public OmniBox focus()
    {
        elementCache().control.click();
        new WebDriverWait(_driver, 2)
                .until(ExpectedConditions.attributeContains(this.getComponentElement(), "class", "is-open"));
        return this;
    }

    public OmniBox blur()
    {
        getWrapper().fireEvent(elementCache().control, WebDriverWrapper.SeleniumEvent.blur);
        return this;
    }

    public OmniBox tabOut()
    {
        getComponentElement().sendKeys(Keys.TAB);
        return this;
    }

    public List<WebElement> getValues()
    {
        return Locators.values.findElements(this);
    }

    // TODO Consider making these getValue* methods private. Knowing what to do with the returned elements
    //  would be very unclear outside this class. I would also expect them to return Strings so maybe create public
    //  getters that return the selected values as Strings and rename the existing methods something
    //  like getSelectedValueElement.
    public WebElement getValue(String expected)
    {
        return Locators.values.containingIgnoreCase(expected).findElementOrNull(this);
    }

    private WebElement editingValueElement()
    {
        return Locators.editingValue.waitForElement(this, WAIT_FOR_JAVASCRIPT);
    }

    public OmniBox editValue(String expectedValue, String newValue)
    {
        getValue(expectedValue).click();
        WebDriverWrapper.waitFor(()-> isEditing(), "did not begin editing", 1500);
        getWrapper().setFormElement(editingValueElement(), newValue);
        stopEditing();
        return this;
    }

    /**
     * 'open' checks to see if 'is-open' is on the class of the component element
     * it is concerned with whether or not the ul.Omnibox-autocomplete list is expanded
     * @return
     */
    private boolean isOpen()
    {
        return getComponentElement().getAttribute("class").contains("is-open") ||
                Locator.tagWithClass("ul", "OmniBox-autocomplete").existsIn(getDriver());
    }

    private OmniBox close()
    {
        elementCache().input.sendKeys(Keys.ESCAPE);
        getWrapper().waitFor(()-> !isOpen(), 500);
        return this;
    }

    public boolean isEditing()  // there is always an input; 'editing' means it currently has a value
    {
        return !Locators.editingValue.findElement(this)
                .getAttribute("value").isEmpty();
    }

    public OmniBox stopEditing()    // click just outside the omnibox, above/center
    {
        if (!isEditing())
            return this;

        editingValueElement().sendKeys(Keys.ENTER);

        WebDriverWrapper.waitFor(()-> !isEditing(), "did not stop editing", 1500);
        return this;
    }

    private void sendClearValue()
    {
        new WebDriverWait(_driver, 1).until(ExpectedConditions.elementToBeClickable(elementCache().input));
        elementCache().input.sendKeys(Keys.BACK_SPACE);
    }

    public OmniBox setFilter(String columnName, FilterOperator operator, @Nullable String value)
    {
        return setFilter(columnName, operator.getValue(), value);
    }

    public OmniBox setFilter(String columnName, String operator, @Nullable String value)
    {
        String val  = value != null ? enquoteIfMultiWord(value) : "";
        StringBuilder expectedFilterText = new StringBuilder();     // this builds the text to search for as a filter-item in the box
        expectedFilterText.append(columnName);
        expectedFilterText.append(" " + operator);

        StringBuilder commandExpression = new StringBuilder("filter");  // this builds the command to enter into the box
        commandExpression.append(" " + enquoteIfMultiWord(columnName));
        commandExpression.append(" " + enquoteIfMultiWord(operator));
        if (null != value)
        {
            commandExpression.append(" " + enquoteIfMultiWord(value));
            expectedFilterText.append(" " + value);
        }

        this.setText(commandExpression.toString());
        if (WebDriverWrapper.waitFor(()->  getValue(expectedFilterText.toString()) != null, 2500))
            return this;

        // try again if necessary and fail if it doesn't work
        getWrapper().log("Retrying attempt to set filter with text [" + commandExpression.toString() + "]");
        this.setText(commandExpression.toString());
        WebDriverWrapper.waitFor(()->  getValue(expectedFilterText.toString()) != null,
                "Expect a new value to be present with [" + expectedFilterText.toString() + "]", 1500);

        return this;
    }

    private String enquoteIfMultiWord(String expression)
    {
        String result;
        if (expression.contains(" "))
            result = "\"" + expression + "\"";
        else
            result = expression;
        return  result;
    }

    private OmniBox setText(String inputValue)
    {
        int numStartValues = getValues().size();
        new WebDriverWait(getWrapper().getDriver(), 1).until(ExpectedConditions.elementToBeClickable(elementCache().input));
        elementCache().input.sendKeys(inputValue);
        elementCache().input.sendKeys(Keys.ENTER);
        WebDriverWrapper.waitFor(()-> {
            return getValues().size() == numStartValues +1;
        }, 1500);

        return this;
    }

    public OmniBox setSearch(String searchTerm)
    {
        return this.setText("search \"" + searchTerm + "\"");
    }

    public OmniBox setSort(String columnName, SortDirection direction)
    {
        return this.setText("sort \"" + columnName + "\"" + (direction.equals(SortDirection.DESC) ? " desc" : ""));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public WebElement control = new LazyWebElement(Locators.omniBoxControl, this);
        public WebElement input = new RefindingWebElement(Locators.omniBoxInput, this);
    }

    private static abstract class Locators
    {
        static final Locator.CssLocator omniBoxControl = Locator.css("div.OmniBox-control");
        static final Locator.CssLocator omniBoxInput = Locator.css("div.OmniBox-input > input");
        static final Locator.XPathLocator values = Locator.tagWithClass("div", "OmniBox-value").childTag("span");
        static final Locator.XPathLocator editingValue = Locator.tagWithClass("div", "OmniBox-input")
                .child(Locator.tag("input").withAttribute("value"));
    }

    public static class OmniBoxFinder extends WebDriverComponent.WebDriverComponentFinder<OmniBox, OmniBoxFinder>
    {
        private Locator _locator;

        public OmniBoxFinder(WebDriver driver)
        {
            super(driver);
            _locator= Locators.omniBoxControl;
        }

        @Override
        protected OmniBox construct(WebElement el, WebDriver driver)
        {
            return new OmniBox(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }

    /**
     * Enum for the various filter operations that the OmniBox allows.
     */
    public enum FilterOperator
    {
        EQUAL("="),
        NOT_EQUAL("<>"),
        GREATER_THAN(">"),
        LESS_THAN("<"),
        GREATER_THAN_OR_EQUALS(">="),
        LESS_THAN_OR_EQUALS("=<"),
        HAS_ANY_VALUE("has any value"),
        IS_BLANK("is blank"),
        IS_NOT_BLANK("is not blank"),
        CONTAINS("contains"),
        DOES_NOT_CONTAINS("does not contain"),
        STARTS_WITH("starts with"),
        DOES_NOT_START_WITH("does not start with");

        private final String operator;

        FilterOperator(String value)
        {
            this.operator = value;
        }

        public String getValue()
        {
            return operator;
        }
    }

}
