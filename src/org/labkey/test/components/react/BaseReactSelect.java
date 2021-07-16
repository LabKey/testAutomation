/*
 * Copyright (c) 2018-2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.react;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebDriverWrapperImpl;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.waitFor;
import static org.labkey.test.util.TestLogger.log;

public abstract class BaseReactSelect<T extends BaseReactSelect> extends WebDriverComponent<BaseReactSelect.ElementCache>
{
    final WebElement _componentElement;
    final WebDriver _driver;
    final WebDriverWrapper _wrapper;
    private final String LOADING_TEXT = "loading...";
    protected static final String SELECTOR_CLASS = "select-input-container";

    public BaseReactSelect(WebElement selectOrParent, WebDriver driver)
    {
        // Component needs to be refinding because Select may go stale after loading initial selections
        _componentElement = new RefindingWebElement(Locator.xpath("(.|./div)").withClass(SELECTOR_CLASS), selectOrParent);
        _driver = driver;
        _wrapper = new WebDriverWrapperImpl(driver);
    }

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
                LOADING_TEXT.equalsIgnoreCase(getComponentElement().getText());
    }

    public boolean isOpen()
    {
        return hasClass("select-input__control--menu-is-open");
    }

    protected T waitForLoaded()
    {
        waitFor(() -> !isLoading(),
                "Took too long for to become loaded", WAIT_FOR_JAVASCRIPT);
        return (T) this;
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

    /**
     *
     * @return
     */
    public @Nullable String getValue()
    {
        waitForLoaded();

        var selections = getSelections();

        if (selections.size() == 1)
            return selections.get(0);
        return null;
    }

    public boolean hasOption(String value)
    {
        scrollIntoView();
        open();
        _wrapper.setFormElement(elementCache().input, value);
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
        waitFor(() ->
                {
                    var _value = getValue();
                    return _value != null && _value.contains(value);
                },
                "took too long for the ReactSelect value to contain the expected value:[" + value + "]", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        return (T) this;
    }

    protected T waitForInteractive()
    {
        // wait for the down-caret to be clickable/interactive
        long start = System.currentTimeMillis();
        waitFor(this::isInteractive, "The select-box did not become interactive in time", 2_000);
        long elapsed = System.currentTimeMillis() - start;
        TestLogger.debug("waited [" + elapsed + "] msec for select to become interactive");

        return (T) this;
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
            return (T) this;

        waitForInteractive();

        try
        {
            elementCache().arrow.click(); // expand the options
        }
        catch (WebDriverException wde) // handle the 'other element would receive the click' situation
        {
            _wrapper.scrollIntoView(elementCache().input);
            _wrapper.fireEvent(elementCache().arrow, WebDriverWrapper.SeleniumEvent.click);
        }

        waitFor(this::isExpanded, 4_000);
        _wrapper.fireEvent(_componentElement, WebDriverWrapper.SeleniumEvent.blur);
        return (T) this;
    }

    public T close()
    {
        if (!isExpanded())
            return (T) this;

        waitForInteractive();

        elementCache().arrow.click(); // collapse the options

        waitForClosed();

        return (T) this;
    }

    protected void waitForClosed()
    {
        WebDriverWrapper.waitFor(() -> !isExpanded(), "Select didn't close", 1_000);
    }

    public T clearSelection()
    {
        if (hasSelection())
        {
            WebElement clear = elementCache().clear;
            clear.click();
            waitFor(() -> {
                try
                {
                    return !(clear.isEnabled() && clear.isDisplayed()); // wait for it to no longer be enabled or displayed
                }
                catch (NoSuchElementException | StaleElementReferenceException nse)
                {
                    return true;
                }
            }, 1000);
        }
        return (T) this;
    }

    public T removeSelection(String value)
    {
        waitForLoaded();

        if (isSingle())
            throw new IllegalArgumentException("This is a single value");

        scrollIntoView();

        var removeBtn = Locators.removeMultiSelectValueButton(value).waitForElement(getComponentElement(), 1_500);
        removeBtn.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(removeBtn));

        return (T) this;
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
        waitFor(()-> labelLocator.findElement(getComponentElement()).isDisplayed(), 1_000);

        List<WebElement> selectedItems = labelLocator.findElements(getComponentElement());
        List<String> rawItems = _wrapper.getTexts(selectedItems);

        // trim whitespace characters
        return rawItems.stream().map(String::trim).collect(Collectors.toList());
    }

    /**
     * Get the items that are in the drop down list. That is the items that may be selected.
     *
     * @return List of strings for the values in the list.
     */
    public List<String> getOptions()
    {
        // Can only get the list of items once the list has been opened.
        open();
        List<WebElement> selectedItems = Locators.listItems.findElements(getComponentElement());
        List<String> rawItems = getWrapper().getTexts(selectedItems);
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
                _wrapper.scrollIntoView(getComponentElement());
                _wrapper.scrollBy(0, 200); // room for options
            }
        }
        catch (StaleElementReferenceException ignore)
        {
            log("Attempted to scroll reactSelect " + getName() + " into view, but the component element was stale");
        }

        return (T) this;
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

        return (T) this;
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
        WebElement clear = new EphemeralWebElement(Locators.clear, this);
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

    public static abstract class Locators
    {
        final public static Locator.XPathLocator option = Locator.tagWithClass("div", "select-input__option");
        public static Locator options = Locator.tagWithClass("div", "select-input__option");
        public static Locator placeholder = Locator.tagWithClass("div", "select-input__placeholder");
        // TODO: Update this locator
        public static Locator createOptionPlaceholder = Locator.tagWithClass("div", "Select-create-option-placeholder");
        public static Locator clear = Locator.tagWithClass("div","select-input__clear-indicator");
        public static Locator arrow = Locator.tagWithClass("div","select-input__dropdown-indicator");
        public static Locator selectMenu = Locator.tagWithClass("div", "select-input__menu-list");
        public static Locator.XPathLocator multiValue = Locator.tagWithClass("div", "select-input__multi-value");
        public static Locator.XPathLocator multiValueLabels = Locator.tagWithClass("div", "select-input__multi-value__label");
        public static Locator.XPathLocator multiValueRemove = Locator.tagWithClass("div", "select-input__multi-value__remove");
        public static Locator.XPathLocator singleValueLabel = Locator.tagWithClass("div", "select-input__single-value");
        public static Locator loadingSpinner = Locator.tagWithClass("span", "select-input__loading-indicator");
        final public static Locator listItems = Locator.tagWithClass("div", "select-input__option");

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

    public static abstract class BaseReactSelectFinder<Select extends BaseReactSelect> extends WebDriverComponent.WebDriverComponentFinder<Select, BaseReactSelectFinder<Select>>
    {
        private Locator.XPathLocator _locator;
        private boolean _mustBeEnabled = false;
        private boolean _findParent = true;

        // Issue 40267: Calling findAll for the react select test component needs to be refined.
        protected BaseReactSelectFinder(WebDriver driver)
        {
            super(driver);
            _locator = Locators.selectContainer();    // use this to find the only reactSelect in a scope
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
            if (_findParent)
                tmpLoc.parent();
            return tmpLoc;
        }
    }
}
