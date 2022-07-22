/*
 * Copyright (c) 2018-2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.react;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.selenium.EphemeralWebElement;
import org.labkey.test.selenium.RefindingWebElement;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.waitFor;
import static org.labkey.test.util.TestLogger.log;

public abstract class BaseReactSelect<T extends BaseReactSelect<T>> extends WebDriverComponent<BaseReactSelect<?>.ElementCache>
{
    final WebElement _componentElement;
    final WebDriver _driver;
    private static final String LOADING_TEXT = "loading...";
    protected static final String SELECTOR_CLASS = "select-input-container";

    public BaseReactSelect(WebElement selectOrParent, WebDriver driver)
    {
        // Component needs to be a RefindingWebElement because Select may go stale after loading initial selections
        _componentElement = new RefindingWebElement(Locator.xpath("(.|./div)").withClass(SELECTOR_CLASS), selectOrParent);
        _driver = driver;
    }

    protected abstract T getThis();

    public boolean isExpanded()
    {
        try
        {
            WebElement selectMenuElement = Locators.selectMenu.findElementOrNull(getComponentElement());

            if ((selectMenuElement != null && selectMenuElement.isDisplayed()) && isLoading())
                waitForLoaded();

            return (selectMenuElement != null && selectMenuElement.isDisplayed()) || isOpen();
        }
        catch (NoSuchElementException | StaleElementReferenceException see)
        {
            return false;
        }
    }

    private boolean hasClass(String cls)
    {
        return Locator.tagWithClass("div", cls).existsIn(getComponentElement());
    }

    public boolean isSingle()
    {
        return !isMulti();
    }

    /* tells us whether or not the current instance of ReactSelect is in multiple-select mode */
    public boolean isMulti()
    {
        return hasClass("select-input__value-container--is-multi");
    }

    public boolean isClearable()
    {
        return hasClass("select-input__clear-indicator");
    }

    public boolean isDisabled()
    {
        return hasClass("select-input__control--is-disabled");
    }

    public boolean isEnabled()
    {
        return !isDisabled();
    }

    public boolean hasValue()
    {
        return hasClass("select-input__value-container--has-value");
    }

    public boolean isLoading()
    {
        // if either are present, we're loading options
        return Locators.loadingSpinner.existsIn(getComponentElement()) ||
                getComponentElement().getText().toLowerCase().contains(LOADING_TEXT);
    }

    public boolean isOpen()
    {
        return hasClass("select-input__control--menu-is-open");
    }

    protected T waitForLoaded()
    {
        waitFor(() -> getComponentElement().isDisplayed() && !isLoading(),
                "Took too long for to become loaded", WAIT_FOR_JAVASCRIPT);
        return getThis();
    }

    public @Nullable String getPlaceholderText()
    {
        waitForLoaded();

        if (isPlaceholderVisible())
            return Locators.placeholder.findElement(getComponentElement()).getText().trim();

        return null;
    }

    public boolean isPlaceholderVisible()
    {
        var placeholder = Locators.placeholder.findElementOrNull(getComponentElement());
        return placeholder != null && placeholder.isDisplayed();
    }

    public String getValue()
    {
        waitForLoaded();

        var selections = getSelections();

        if (selections.size() == 1)
            return selections.get(0);
        return "";
    }

    public boolean hasOption(String value)
    {
        scrollIntoView();
        open();
        getWrapper().setFormElement(elementCache().input, value);
        WebElement foundElement;
        try
        {
            var optionElement = ReactSelect.Locators.options.containing(value);
            foundElement = optionElement.waitForElement(elementCache().selectMenu, 4000);
            elementCache().input.clear();
        }
        catch (NoSuchElementException nse)
        {
            return false;
        }
        return foundElement != null;
    }

    /* waits until the currently selected 'value' equals or contains the specified string */
    public T expectValue(String value)
    {
        waitFor(() -> getValue().contains(value),
                "took too long for the ReactSelect value to contain the expected value:[" + value + "]", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        return getThis();
    }

    protected T waitForInteractive()
    {
        // wait for the down-caret to be clickable/interactive
        long start = System.currentTimeMillis();
        waitFor(this::isInteractive, "The select-box did not become interactive in time", 2_000);
        long elapsed = System.currentTimeMillis() - start;
        TestLogger.debug("waited [" + elapsed + "] ms for select to become interactive");

        return getThis();
    }

    public boolean isInteractive()
    {
        try
        {
            WebElement arrowEl = Locators.arrow.findElement(getComponentElement());
            return arrowEl.isEnabled() && arrowEl.isDisplayed();
        }
        catch (StaleElementReferenceException | NoSuchElementException retry)
        {
            return false;
        }
    }

    public T open()
    {
        if (isExpanded())
            return getThis();

        waitForInteractive();

        try
        {
            elementCache().arrow.click(); // expand the options
        }
        catch (WebDriverException wde) // handle the 'other element would receive the click' situation
        {
            getWrapper().scrollIntoView(elementCache().input);
            getWrapper().fireEvent(elementCache().arrow, WebDriverWrapper.SeleniumEvent.click);
        }

        waitFor(this::isExpanded, "Select didn't expand.", 4_000);
        getWrapper().fireEvent(getComponentElement(), WebDriverWrapper.SeleniumEvent.blur);
        return getThis();
    }

    public T close()
    {
        if (!isExpanded())
            return getThis();

        waitForInteractive();

        elementCache().arrow.click(); // collapse the options

        waitForClosed();

        return getThis();
    }

    protected void waitForClosed()
    {
        WebDriverWrapper.waitFor(() -> !isExpanded(), "Select didn't close", 1_000);
    }

    public T clearSelection()
    {
        if (hasSelection())
        {
            var clear = Locators.clear.waitForElement(getComponentElement(), 1_500);
            clear.click();
            getWrapper().shortWait().until(ExpectedConditions.stalenessOf(clear));
        }
        return getThis();
    }

    public T removeSelection(String value)
    {
        waitForLoaded();

        if (isSingle())
            throw new IllegalArgumentException("This is a single value");

        scrollIntoView();

        WebElement removeBtn = Locators.removeMultiSelectValueButton(value).findWhenNeeded(getComponentElement());
        removeBtn.click();

        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(removeBtn));

        // Validate that the selected item really was removed.
        WebDriverWrapper.sleep(500);
        waitFor(()->!getSelections().contains(value), String.format("Failed to remove selection '%s'.", value), WAIT_FOR_JAVASCRIPT);

        return getThis();
    }

    public boolean hasSelection()
    {
        return !this.getSelections().isEmpty();
    }

    protected Locator.XPathLocator getValueLabelLocator()
    {
        return isMulti() ? Locators.multiValueLabels : Locators.singleValueLabel;
    }

    /**
     * Returns a list of currently selected values (by label) in the select. If this is a single-select, then this
     * will at most contain a single value. If this is a multi-select, then this will contain all selected values.
     *
     * @return List<String> of the currently selected values.
     */
    public List<String> getSelections()
    {
        waitForLoaded();

        if (!hasValue())
            return Collections.emptyList();

        var labelLocator = getValueLabelLocator();

        // Wait for at least one of the elements to be visible.
        waitFor(()-> labelLocator.findElement(getComponentElement()).isDisplayed(), "Selection not visible.", 1_000);

        List<WebElement> selectedItems = labelLocator.findElements(getComponentElement());
            List<String> rawItems = getWrapper().getTexts(selectedItems);

        // trim whitespace characters
        return rawItems.stream().map(String::trim).collect(Collectors.toList());
    }

    /**
     * Get the items that are in the dropdown list. That is the items that may be selected.
     *
     * @return List of strings for the values in the list.
     */
    public List<String> getOptions()
    {

        boolean alreadyOpened = isExpanded();

        // Can only get the list of items once the list has been opened.
        if (!alreadyOpened)
            open();

        List<WebElement> selectedItems = Locators.listItems.findElements(getComponentElement());
        List<String> rawItems = getWrapper().getTexts(selectedItems);

        // If it wasn't open before close it, otherwise leave it in the open state.
        if(!alreadyOpened)
            close();

        return rawItems.stream().map(String::trim).collect(Collectors.toList());
    }


    public String getName()
    {
        return elementCache().input.getAttribute("name");
    }

    protected T scrollIntoView()
    {
        try
        {
            if (!getComponentElement().isDisplayed())
            {
                getWrapper().scrollIntoView(getComponentElement());
                getWrapper().scrollBy(0, 200); // room for options
            }
        }
        catch (StaleElementReferenceException ignore)
        {
            log("Attempted to scroll reactSelect " + getName() + " into view, but the component element was stale");
        }

        return getThis();
    }

    /**
     * Use this method if the underlying ReactSelect allows for creation of values from user input.
     * This will input the given value directly into the ReactSelect and invoke the ReactSelect to create the value.
     */
    public T createValue(String value)
    {
        waitForLoaded();
        waitForInteractive();

        elementCache().input.sendKeys(value);
        elementCache().input.sendKeys(Keys.ENTER);

        waitFor(() -> getValueLabelLocator().withText(value).existsIn(getComponentElement()), "Failed to create value \"" + value + "\".", 1_000);

        return getThis();
    }

    /**
     * Enter the given value into the text box of the select control. Useful for filtering the options list.
     *
     * @param value Value to type in.
     * @return Reference to the select control.
     */
    public T enterValueInTextbox(String value)
    {
        waitForLoaded();
        waitForInteractive();
        elementCache().input.sendKeys(value);
        return getThis();
    }

    @Override
    public WebElement getComponentElement()
    {
        return _componentElement;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends WebDriverComponent<?>.ElementCache
    {
        WebElement input = new EphemeralWebElement(Locator.css(".select-input__input > input"), this);
        WebElement arrow = new EphemeralWebElement(Locators.arrow, this);
        WebElement selectMenu = new EphemeralWebElement(Locators.selectMenu, this).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);

        List<WebElement> getOptions()
        {
            return Locators.options.findElements(selectMenu);
        }

        @NotNull
        WebElement findOption(String option)
        {
            return Locators.options.withText(option).findElement(selectMenu);
        }
    }

    public abstract static class Locators
    {
        private Locators()
        {
            // Do nothing constructor to prevent instantiation.
        }

        public static final Locator.XPathLocator option = Locator.tagWithClass("div", "select-input__option");
        public static final Locator options = Locator.tagWithClass("div", "select-input__option");
        public static final Locator placeholder = Locator.tagWithClass("div", "select-input__placeholder");
        public static final Locator clear = Locator.tagWithClass("div","select-input__clear-indicator");
        public static final Locator arrow = Locator.tagWithClass("div","select-input__dropdown-indicator");
        public static final Locator selectMenu = Locator.tagWithClass("div", "select-input__menu-list");
        public static final Locator.XPathLocator multiValue = Locator.tagWithClass("div", "select-input__multi-value");
        public static final Locator.XPathLocator multiValueLabels = Locator.tagWithClass("div", "select-input__multi-value__label");
        public static final Locator.XPathLocator multiValueRemove = Locator.tagWithClass("div", "select-input__multi-value__remove");
        public static final Locator.XPathLocator singleValueLabel = Locator.tagWithClass("div", "select-input__single-value");
        public static final Locator loadingSpinner = Locator.tagWithClass("span", "select-input__loading-indicator");
        public static final Locator listItems = Locator.tagWithClass("div", "select-input__option");

        public static Locator.XPathLocator selectContainer()
        {
            return Locator.tagWithClass("div", BaseReactSelect.SELECTOR_CLASS);
        }

        public static Locator.XPathLocator selectValueLabelContaining(String valueContains)
        {
            return singleValueLabel.containing(valueContains);
        }

        public static Locator.XPathLocator selectValueLabel(String text)
        {
            return singleValueLabel.withText(text);
        }

        public static Locator.XPathLocator removeMultiSelectValueButton(String text)
        {
            return multiValue.withChild(multiValueLabels.withText(text)).child(Locators.multiValueRemove);
        }

        public static Locator.XPathLocator container(String id)
        {
            return selectContainer().withAttribute("id", id);
        }

        private static Locator.XPathLocator containerWithDescendant(Locator.XPathLocator descendant)
        {
            return selectContainer().withDescendant(descendant);
        }

        public static Locator.XPathLocator containerById(String inputId)
        {
            return selectContainer().withDescendant(Locator.inputById(inputId));
        }

        public static Locator.XPathLocator containerByName(String inputName)
        {
            return selectContainer().withDescendant(Locator.input(inputName));
        }
    }

    public abstract static class BaseReactSelectFinder<Select extends BaseReactSelect<Select>> extends WebDriverComponent.WebDriverComponentFinder<Select, BaseReactSelectFinder<Select>>
    {
        private Locator.XPathLocator _locator;
        private boolean _mustBeEnabled = false;

        // Issue 40267: Calling findAll for the react select test component needs to be refined.
        protected BaseReactSelectFinder(WebDriver driver)
        {
            super(driver);
            _locator = Locators.selectContainer(); // use this to find the only reactSelect in a scope
        }

        public BaseReactSelectFinder<Select> withContainerClass(String containerClass)
        {
            _locator = Locators.selectContainer().withClass(containerClass);
            return this;
        }

        public BaseReactSelectFinder<Select> withId(String id)
        {
            _locator = Locators.containerWithDescendant(Locator.tagWithId("div", id));
            return this;
        }

        public BaseReactSelectFinder<Select> withInputClass(String inputClass)
        {
            _locator = Locators.containerWithDescendant(Locator.tagWithClass("div", inputClass));
            return this;
        }

        public BaseReactSelectFinder<Select> withNamedInput(String inputName)
        {
            _locator = Locators.containerWithDescendant(Locator.tagWithName("input", inputName));
            return this;
        }

        public BaseReactSelectFinder<Select> withName(String inputName)
        {
            _locator = Locators.containerByName(inputName);
            return this;
        }

        /* use this to find a reactSelect when the label text is the only content of the label element*/
        public BaseReactSelectFinder<Select> withLabel(String label)
        {
            _locator = Locators.containerWithDescendant(Locator.tag("input").withLabel(label));
            return this;
        }

        public BaseReactSelectFinder<Select> withLabelForNamedInput(String label)
        {
            _locator = Locators.containerWithDescendant(Locator.tag("input")
                    .withAttributeMatchingOtherElementAttribute("name", Locator.tag("label").withText(label), "for"));
            return this;
        }

        /* use this to find a reactSelect when the label text is contained within a label/span*/
        public BaseReactSelectFinder<Select> withLabelwithSpan(String labelSpanText)
        {
            return followingLabelWithSpan(labelSpanText);
        }

        public BaseReactSelectFinder<Select> followingLabelWithSpan(String labelText)
        {
            _locator = Locators.containerWithDescendant(Locator.tag("label").withChild(Locator.tagWithText("span", labelText)));
            return this;
        }

        public BaseReactSelectFinder<Select> followingLabelWithClass(String cls)
        {
            _locator = Locators.containerWithDescendant(Locator.tagWithClass("label", cls));
            return this;
        }

        /**
         * Find Select within a &lt;FormGroup&gt; based on the FormGroup's label.
         * Assumes that the FormGroup has a FormLabel.
         * @param labelText Text of the FormGroup's FormLabel
         * @return component finder that will find the specified Select
         */
        public BaseReactSelectFinder<Select> withinFormGroup(String labelText)
        {
            _locator = Locator.tagWithClass("div", "form-group")
                    .withChild(Locator.tag("label").withPredicate("text() = " + Locator.xq(labelText)))
                    .descendant(Locators.selectContainer());
            return this;
        }

        public BaseReactSelectFinder<Select> enabled()
        {
            _mustBeEnabled = true;
            return this;
        }

        @Override
        protected Locator locator()
        {
            Locator.XPathLocator tmpLoc = _locator;
            if (_mustBeEnabled)
                tmpLoc = _locator.withoutClass("is-disabled");
            return tmpLoc;
        }
    }
}
