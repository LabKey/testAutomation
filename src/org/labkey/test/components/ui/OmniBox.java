/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.ui;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.ui.OmniBoxValue.OmniBoxValueFinder;
import org.labkey.test.components.ui.OmniBoxValue.OmniType;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class OmniBox extends WebDriverComponent<OmniBox.ElementCache>
{
    private final WebElement _omniBoxElement;
    private final WebDriver _driver;
    private final UpdatingComponent _linkedComponent;

    private OmniBox(WebElement element, WebDriver driver, UpdatingComponent linkedComponent)
    {
        _omniBoxElement = element;
        _driver = driver;
        _linkedComponent = linkedComponent;
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
        List<OmniBoxValue> valueItems = getValues();
        for (int i = getValues().size() - 1 ; i >= 0; i--) // dismiss from the right first;
        {
            OmniBoxValue obValue = valueItems.get(i);
            _linkedComponent.doAndWaitForUpdate(obValue::dismiss);
        }

        Assert.assertEquals("not all of the omnibox values were cleared", 0, getValues().size());

        return this;
    }

    public OmniBox focus()
    {
        elementCache().input.click();
        new WebDriverWait(_driver, Duration.ofSeconds(2))
                .until(ExpectedConditions.attributeContains(this.getComponentElement(), "class", "is-open"));
        return this;
    }

    public OmniBox blur()
    {
        getWrapper().fireEvent(elementCache().input, WebDriverWrapper.SeleniumEvent.blur);
        return this;
    }

    public OmniBox tabOut()
    {
        getComponentElement().sendKeys(Keys.TAB);
        return this;
    }

    public List<OmniBoxValue> getValues()
    {
        return new OmniBoxValueFinder(getDriver()).findAll(this);
    }

    public OmniBoxValue getValue(String expected)
    {
        return new OmniBoxValueFinder(getDriver()).withText(expected).find(this);
    }

    public Optional<OmniBoxValue> getOptionalValue(String expected)
    {
        return new OmniBoxValueFinder(getDriver()).withText(expected).findOptional(this);
    }

    public OmniBox editValue(String expectedValue, String newValue)
    {
        var input = getValue(expectedValue).openEdit();
        WebDriverWrapper.waitFor(this::isEditing, "did not begin editing", 1500);
        input.set(newValue);
        stopEditing();
        return this;
    }

    /**
     * 'open' checks to see if 'is-open' is on the class of the component element
     * it is concerned with whether or not the ul.Omnibox-autocomplete list is expanded
     */
    private boolean isOpen()
    {
        return getComponentElement().getAttribute("class").contains("is-open") ||
                Locator.tagWithClass("ul", "OmniBox-autocomplete").existsIn(getDriver());
    }

    private OmniBox close()
    {
        if (isOpen())
        {
            elementCache().input.sendKeys(Keys.ESCAPE);
            WebDriverWrapper.waitFor(() -> !isOpen(), "Didn't close omnibox popup", 500);
        }
        return this;
    }

    public boolean isEditing()  // there is always an input; 'editing' means it currently has a value
    {
        return !elementCache().input.getAttribute("value").isEmpty();
    }

    public OmniBox stopEditing()
    {
        if (!isEditing())
            return this;

        elementCache().input.sendKeys(Keys.ENTER);

        WebDriverWrapper.waitFor(()-> !isEditing(), "did not stop editing", 1500);
        getWrapper().mouseOver(elementCache().input); // Make sure mouse isn't over a value
        return this;
    }

    public OmniBox setFilter(String columnName, FilterOperator operator)
    {
        return setFilter(columnName, operator.getValue(), null);
    }

    /**
     * Set a column filter in the OmniBox.
     *
     * @param columnName Name of the column to filter on.
     * @param operator The filter {@link FilterOperator} to use.
     * @param value The value to compare to.
     * @return A reference to this OmniBox.
     */
    public OmniBox setFilter(String columnName, FilterOperator operator, @Nullable String value)
    {
        return setFilter(columnName, operator.getValue(), value);
    }

    /**
     * @deprecated Use {@link OmniBox#setFilter(String, FilterOperator, String)}
     */
    @Deprecated
    public OmniBox setFilter(String columnName, String operator, @Nullable String value)
    {
        StringBuilder expectedFilterText = new StringBuilder();     // this builds the text to search for as a filter-item in the box
        expectedFilterText.append(columnName);
        expectedFilterText.append(" ").append(operator);

        StringBuilder commandExpression = new StringBuilder("filter");  // this builds the command to enter into the box
        commandExpression.append(" ").append(enquoteIfMultiWord(columnName));
        commandExpression.append(" ").append(enquoteIfMultiWord(operator));
        if (null != value)
        {
            commandExpression.append(" ").append(enquoteIfMultiWord(value));
            expectedFilterText.append(" ").append(value);
        }

        this.setText(commandExpression.toString());
        valueFinder(OmniType.filter, expectedFilterText.toString()).waitFor(this);

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

    private void setText(String inputValue)
    {
        new WebDriverWait(getWrapper().getDriver(), Duration.ofSeconds(1)).until(ExpectedConditions.elementToBeClickable(elementCache().input));

        _linkedComponent.doAndWaitForUpdate(() -> {
            elementCache().input.sendKeys(inputValue);
            stopEditing();
        });
    }

    public OmniBox setSearch(String searchTerm)
    {
        this.setText("search \"" + searchTerm + "\"");
        valueFinder(OmniType.search, searchTerm).waitFor(this);
        return this;
    }

    public OmniBox setSort(String columnName, SortDirection direction)
    {
        this.setText("sort \"" + columnName + "\"" + (direction == SortDirection.DESC ? " desc" : ""));
        valueFinder(OmniType.sort, columnName).waitFor(this);
        return this;
    }

    private OmniBoxValueFinder valueFinder(OmniType type, String value)
    {
        return new OmniBoxValueFinder(getDriver()).withType(type).withText(value);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public WebElement input = Locator.css("div.OmniBox-input > input").refindWhenNeeded(this);
    }

    public static class OmniBoxFinder extends WebDriverComponent.WebDriverComponentFinder<OmniBox, OmniBoxFinder>
    {
        private final Locator _baseLocator = Locator.css("div.OmniBox-control");
        private final UpdatingComponent _linkedComponent;

        public OmniBoxFinder(WebDriver driver, UpdatingComponent linkedComponent)
        {
            super(driver);
            _linkedComponent = linkedComponent;
        }

        public OmniBoxFinder(WebDriver driver)
        {
            this(driver, UpdatingComponent.NO_OP);
        }

        @Override
        protected OmniBox construct(WebElement el, WebDriver driver)
        {
            return new OmniBox(el, driver, _linkedComponent);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
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
