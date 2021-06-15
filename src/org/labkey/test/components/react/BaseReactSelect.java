/*
 * Copyright (c) 2018-2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.react;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebDriverWrapperImpl;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.selenium.EphemeralWebElement;
import org.labkey.test.selenium.RefindingWebElement;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
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

    public BaseReactSelect(WebElement selectOrParent, WebDriver driver)
    {
        // Component needs to be refinding because Select may go stale after loading initial selections
        _componentElement = new RefindingWebElement(Locator.xpath("(.|./div)").withClass("Select"), selectOrParent);
        _driver = driver;
        _wrapper = new WebDriverWrapperImpl(driver);
    }

    public boolean isExpanded()
    {
        try
        {
            WebElement selectMenuElement = Locators.selectMenu.findElementOrNull(getComponentElement());

            if((selectMenuElement != null && selectMenuElement.isDisplayed()) && isLoading())
                waitForLoaded();

            return (selectMenuElement != null && selectMenuElement.isDisplayed()) || hasClass("is-open");
        }
        catch (NoSuchElementException | StaleElementReferenceException see)
        {
            return false;
        }
    }

    private boolean hasClass(String cls)
    {
        String attr = getComponentElement().getAttribute("class");
        return attr != null && attr.contains(cls);
    }

    /* tells us whether or not the current instance of ReactSelect is in multiple-select mode */
    public boolean isMulti()
    {
        return hasClass("Select--multi");
    }

    public boolean isSearchable()
    {
        return hasClass("is-searchable");
    }

    public boolean isClearable()
    {
        return hasClass("is-clearable");
    }

    public boolean isEnabled()
    {
        return !hasClass("is-disabled");
    }

    public boolean hasValue()
    {
        return hasClass("has-value");
    }

    public boolean isLoading()
    {
        // if either are present, we're loading options
        return Locators.loadingSpinner.existsIn(getComponentElement()) ||
                getComponentElement().getText().toLowerCase().equals(LOADING_TEXT);
    }

    protected T waitForLoaded()
    {
        _wrapper.waitFor(()-> !isLoading(),
                "Took too long for to become loaded", WAIT_FOR_JAVASCRIPT);
        return (T) this;
    }

    public String getValue()
    {
        waitForLoaded();

        String val = getComponentElement().getText();
        if (val.toLowerCase().equals(LOADING_TEXT))
            return null;
        else
            return val;
    }

    public boolean hasOption(String value)
    {
        return hasOption(value, ReactSelect.Locators.options.containing(value));
    }

    public boolean hasOption(String value, Locator optionElement)
    {
        scrollIntoView();
        open();
        _wrapper.setFormElement(elementCache().input, value);
        WebElement foundElement;
        try
        {
            foundElement = optionElement.waitForElement(elementCache().selectMenu, 4000);
            elementCache().input.clear();
        }
        catch (NoSuchElementException nse)
        {
            return false;
        }
        return  foundElement != null;
    }

    /* waits until the currently selected 'value' (which can include the placeholder) equals or contains the specified string */
    public T expectValue(String value)
    {
        _wrapper.waitFor(()-> getValue().contains(value),
                "took too long for the ReactSelect value to contain the expected value:["+value+"]", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        return (T) this;
    }

    protected T waitForInteractive()
    {
        // wait for the down-caret to be clickable/interactive
        long start = System.currentTimeMillis();
        _wrapper.waitFor(this::isInteractive, "The select-box did not become interactive in time", 2_000);
        long elapsed = System.currentTimeMillis() - start;
        getWrapper().log("waited ["+ elapsed + "] msec for select to become interactive");

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

        WebDriverWrapper.waitFor(this::isExpanded, 4_000);
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
        WebDriverWrapper.waitFor(()->!isExpanded(), "Select didn't close", 1_000);
    }

    public T clearSelection()
    {
        if (hasSelection())
        {
            WebElement clear = elementCache().clear;
            clear.click();
            _wrapper.waitFor(()->{
                try
                {
                    return ! (clear.isEnabled() && clear.isDisplayed()); // wait for it to no longer be enabled or displayed
                }
                catch (NoSuchElementException | StaleElementReferenceException nse)
                {
                    return true;
                }
            }, 1000);
        }
        return (T) this;
    }

    public boolean hasSelection()
    {
        try     // this assumes that the 'x' element is only present when a value has been selected
        {
            return Locators.clear.findElementOrNull(getComponentElement()) != null;
        }
        catch (StaleElementReferenceException | NoSuchElementException nope)
        {
            return false;
        }
    }

    public List<String> getSelections()
    {
        waitForLoaded();
        try
        {
            // Wait for at least one of the elements to be visible.
            waitFor(()-> Locators.selectedItems.findElement(getComponentElement()).isDisplayed(), 1_000);

            List<WebElement> selectedItems = Locators.selectedItems.findElements(getComponentElement());
            List<String> rawItems = _wrapper.getTexts(selectedItems);

            // trim whitespace characters
            return rawItems.stream().map(String::trim).collect(Collectors.toList());
        }
        catch(NoSuchElementException nse)
        {
            // If there has been an error and the selection couldn't be loaded the html structure is different.
            // There should be the placeholder text so get it.
            WebElement placeHolder = Locators.placeholder.findElement(getComponentElement());
            return List.of(placeHolder.getText());
        }

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
            WebElement container = Locators.selectContainer().findElement(getComponentElement());
            if (!container.isDisplayed())
            {
                _wrapper.scrollIntoView(container);
                _wrapper.scrollBy(0, 200); // room for options
            }
        }
        catch (StaleElementReferenceException ignore)
        {
            log("Attempted to scroll reactSelect " + getName() +" into view, but the component element was stale");
        }

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
        WebElement input = new EphemeralWebElement(Locator.css(".Select-input > input"), this);
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
            Locator loc = Locators.options.withText(option);
            return loc.findElement(selectMenu);
        }
    }

    public static abstract class Locators
    {
        final public static Locator.XPathLocator option = Locator.tagWithClass("div", "Select-option");
        public static Locator options = Locator.tagWithClass("div", "Select-option");
        public static Locator placeholder = Locator.tagWithClass("div", "Select-placeholder");
        public static Locator createOptionPlaceholder = Locator.tagWithClass("div", "Select-create-option-placeholder");
        public static Locator clear = Locator.tagWithClass("span","Select-clear-zone");
        public static Locator arrow = Locator.tagWithClass("span","Select-arrow-zone");
        public static Locator selectMenu = Locator.tagWithClass("div", "Select-menu");
        public static Locator.XPathLocator selectedItems = Locator.tagWithClass("span", "Select-value-label");
        public static Locator loadingSpinner = Locator.tagWithClass("span", "Select-loading");
        final public static Locator listItems = Locator.tagWithClass("div", "Select-option");

        public static Locator.XPathLocator selectContainer()
        {
            return Locator.tagWithClassContaining("div", "Select");
        }

        public static Locator.XPathLocator selectValueLabelContaining(String valueContains)
        {
            return selectedItems.containing(valueContains);
        }
        public static Locator.XPathLocator selectValueLabel(String text)
        {
            return selectedItems.withText(text);
        }

        public static Locator.XPathLocator container(String inputName)
        {
            return Locator.tagWithClass("div", "Select").withDescendant(
                    Locator.tagWithId("input", inputName));
        }

        public static Locator.XPathLocator containerWithDescendant(Locator.XPathLocator descendant)
        {
            return Locator.tagWithClass("div", "Select").withDescendant(descendant);
        }

        public static Locator.XPathLocator container(List<String> inputNames)
        {
            StringBuilder childXpath = new StringBuilder( "//input[@id="+ Locator.xq(inputNames.get(0)) + "");
            for (int i = 1; i < inputNames.size(); i++)
            {
                childXpath.append(" or @id=" + Locator.xq(inputNames.get(i)) + "");
            }
            childXpath.append("]");
            return Locator.tagWithClass("div", "Select").withDescendant(
                    Locator.xpath(childXpath.toString()));
        }

        public static Locator.XPathLocator containerStartsWith(List<String> inputNames)
        {
            StringBuilder childXpath = new StringBuilder( "//input[starts-with(@id, '"+ inputNames.get(0) + "')");
            for (int i = 1; i < inputNames.size(); i++)
            {
                childXpath.append(" or starts-with(@id, '"+inputNames.get(i)+"')");
            }
            childXpath.append("]");
            return Locator.tagWithClass("div", "Select").withDescendant(
                    Locator.xpath(childXpath.toString()));
        }

        public static Locator.XPathLocator containerById(String inputId)
        {
            return Locator.tagWithClass("div", "Select").withDescendant(
                    Locator.tagWithId("input", inputId));
        }

        public static Locator.XPathLocator containerByName(String inputName)
        {
            return Locator.tagWithClass("div", "Select").withDescendant(
                    Locator.tagWithName("input", inputName));
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
            _locator=Locators.selectContainer();    // use this to find the only reactSelect in a scope
        }

        public BaseReactSelectFinder<Select> withIds(List<String> inputNames)
        {
            _locator = Locators.container(inputNames);
            return this;
        }

        /* the ID is for the Select > Select-Control > span > div > input of the ReactSelect */
        public BaseReactSelectFinder<Select> withId(String inputName)
        {
            _locator = Locators.container(inputName);
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
            _locator = Locator.tag("label").withChild(Locator.tagWithText("span", labelText)).followingSibling("div").child(Locator.tagWithClass("div", "Select"));
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

        public BaseReactSelectFinder<Select> withLabelContaining(String label)
        {
            _locator = ReactSelect.Locators.containerWithDescendant(Locator.tag("input")
                    .withLabelContaining(label));
            return this;
        }

        public BaseReactSelectFinder<Select> withIdsStartingWith(List<String> names)
        {
            _locator = ReactSelect.Locators.containerStartsWith(names);
            return this;
        }

        public BaseReactSelectFinder<Select> withIdStartingWith(String name)
        {
            _locator = ReactSelect.Locators.containerStartsWith(Arrays.asList(name));
            return this;
        }

        public BaseReactSelectFinder<Select> enabled()
        {
            _mustBeEnabled = true;
            return this;
        }

        /**
         * Most React Selects are wrapped by a div that is less prone to going stale. Use this to find the exact Select
         * div when there is no wrapper div.
         */
        public BaseReactSelectFinder<Select> notParent()
        {
            _findParent = false;
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
