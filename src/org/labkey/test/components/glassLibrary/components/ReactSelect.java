package org.labkey.test.components.glassLibrary.components;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.selenium.EphemeralWebElement;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.sleep;
import static org.labkey.test.WebDriverWrapper.waitFor;
import static org.labkey.test.util.TestLogger.log;

public class ReactSelect extends WebDriverComponent<ReactSelect.ElementCache>
{
    private final WebElement _componentElement;
    private final WebDriver _driver;

    private Function<String, Locator> _optionLocFactory = Locators.option::withText;

    public ReactSelect(WebElement element, WebDriver driver)
    {
        _componentElement = element;
        _driver = driver;
    }

    public ReactSelect setOptionLocator(Function<String, Locator> optionLocFactory)
    {
        _optionLocFactory = optionLocFactory;
        return this;
    }

    public boolean isExpanded()
    {
        try
        {
            WebElement selectMenuElement = Locators.selectMenu.findElementOrNull(getComponentElement());

            if((selectMenuElement != null && selectMenuElement.isDisplayed()) && selectMenuElement.getText().toLowerCase().contains("loading"))
                waitFor(()-> !selectMenuElement.getText().toLowerCase().contains("loading"),
                        "React Select is stuck loading.", WAIT_FOR_JAVASCRIPT);

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

    public boolean hasValue()
    {
        return hasClass("has-value");
    }

    public boolean isLoading()
    {
        // if it's present, we're loading
        return Locators.loadingSpinner.findElementOrNull(getComponentElement()) != null;
    }

    public String getValue()
    {
        return getComponentElement().getText();
    }

    public boolean hasOption(String value)
    {
        return hasOption(value, ReactSelect.Locators.option.containing(value));
    }

    public boolean hasOption(String value, Locator optionElement)
    {
        scrollIntoView();
        open();
        getWrapper().setFormElement(elementCache().input, value);
        WebElement foundElement;
        try
        {
            foundElement = optionElement.waitForElement(elementCache().selectMenu, 4000);
            elementCache().input.clear();
        }
        catch (org.openqa.selenium.NoSuchElementException nse)
        {
            return false;
        }
        return  foundElement != null;
    }

    /* waits until the 'value' (which can include the placeholder) of the select shows or contains the expected value */
    public ReactSelect expectValue(String value)
    {
        waitFor(()-> getValue().contains(value),
                "It took too long for the ReactSelect value to contain the expected value:["+value+"]. Actual value:[" + getValue() + "].", WAIT_FOR_JAVASCRIPT);
        return this;
    }

    protected ReactSelect waitForInteractive()
    {
        // wait for the down-caret to be clickable/interactive
        long start = System.currentTimeMillis();
        waitFor(this::isInteractive, "The select-box did not become interactive in time", 2000);
        long elapsed = System.currentTimeMillis() - start;
        TestLogger.debug("waited ["+ elapsed + "] msec for select to become interactive");

        return this;
    }

    private boolean isInteractive()
    {
        try
        {
            WebElement arrowEl = Locators.arrow.findElement(getComponentElement());
            return arrowEl.isEnabled() && arrowEl.isDisplayed();
        }
        catch (StaleElementReferenceException | org.openqa.selenium.NoSuchElementException retry)
        {
            return false;
        }
    }

    public ReactSelect open()
    {
        if (isExpanded())
            return this;

        waitForInteractive();

        try
        {
            elementCache().arrow.click(); // expand the options
        }
        catch (ElementClickInterceptedException clickExcp) // handle the 'other element would receive the click' situation
        {
            getWrapper().scrollIntoView(elementCache().input);
            getWrapper().fireEvent(elementCache().arrow, WebDriverWrapper.SeleniumEvent.click);
        }

        WebDriverWrapper.waitFor(this::isExpanded, 4000);
        getWrapper().fireEvent(_componentElement, WebDriverWrapper.SeleniumEvent.blur);
        return this;
    }

    public ReactSelect close()
    {
        if (!isExpanded())
            return this;

        waitForInteractive();

        elementCache().arrow.click(); // collapse the options

        waitForClosed();

        return this;
    }

    private void waitForClosed()
    {
        WebDriverWrapper.waitFor(()->!isExpanded(), "Select didn't close", 1000);
    }

    public ReactSelect clearSelection()
    {
        if (hasSelection())
        {
            WebElement clear = elementCache().clear;
            clear.click();
            getWrapper().waitFor(()->{
                try
                {
                    return ! (clear.isEnabled() && clear.isDisplayed()); // wait for it to no longer be enabled or displayed
                }
                catch (org.openqa.selenium.NoSuchElementException | StaleElementReferenceException nse)
                {
                    return true;
                }
            }, 1000);
        }
        return this;
    }

    public boolean hasSelection()
    {
        try     // this assumes that the 'x' element is only present when a value has been selected
        {
            return Locators.clear.findElementOrNull(getComponentElement()) != null;
        }
        catch (StaleElementReferenceException | org.openqa.selenium.NoSuchElementException nope)
        {
            return false;
        }
    }

    public List<String> getSelections()
    {
        try
        {
            // Wait for at least one of the elements to be visible.
            waitFor(()-> {return Locators.selectedItems.findElement(getComponentElement()).isDisplayed();}, 1000);

            List<WebElement> selectedItems = Locators.selectedItems.findElements(getComponentElement());
            List<String> rawItems = getWrapper().getTexts(selectedItems);

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

    protected String getName()
    {
        return elementCache().input.getAttribute("name");
    }

    protected ReactSelect scrollIntoView()
    {
        try
        {
            WebElement container = Locators.selectContainer().findElement(getComponentElement());
            if (!container.isDisplayed())
            {
                getWrapper().scrollIntoView(container);
                getWrapper().scrollBy(0, 200); // room for options
            }
        }
        catch (StaleElementReferenceException ignore)
        {
            log("Attempted to scroll reactSelect into view, but the component element was stale");
        }

        return this;
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

    protected class ElementCache extends WebDriverComponent.ElementCache
    {
        WebElement input = new EphemeralWebElement(Locator.css(".Select-input > input"), this);
        WebElement clear = new EphemeralWebElement(Locators.clear, this);
        WebElement arrow = new EphemeralWebElement(Locators.arrow, this);
        WebElement selectMenu = new EphemeralWebElement(Locators.selectMenu, this).withTimeout(WAIT_FOR_JAVASCRIPT);
        List<WebElement> getOptions()
        {
            return Locators.option.findElements(selectMenu);
        }

        @NotNull
        WebElement findOption(String option)
        {
            Locator loc = _optionLocFactory.apply(option);
            return loc.findElement(selectMenu);
        }
    }

    public ReactSelect select(String option)
    {
        List<String> selections = scrollIntoView()
                .open()
                .clickOption(option)
                .getSelections();
        waitForClosed();

        // TODO Comment out this line for the time being. Issue 37897: Sample Management: Lookup field values are not consistently displayed between the edit sample panel and the create sample panel.
//        assertTrue("Expected '" + option + "' to be selected.  Current selections: " + selections, selections.contains(option));
        return this;
    }

    protected ReactSelect clickOption(String option)
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
            catch (org.openqa.selenium.NoSuchElementException nse)
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
                log("optionEl went stale, probably while attempting to scroll it into view");
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

        return this;
    }

    public static abstract class Locators
    {
        final public static Locator.XPathLocator option = Locator.tagWithClass("div", "Select-option");
        final public static Locator placeholder = Locator.tagWithClass("div", "Select-placeholder");
        final public static Locator createOptionPlaceholder = Locator.tagWithClass("div", "Select-create-option-placeholder");
        final public static Locator clear = Locator.tagWithClass("span","Select-clear-zone");
        final public static Locator arrow = Locator.tagWithClass("span","Select-arrow-zone");
        final public static Locator selectMenu = Locator.tagWithClass("div", "Select-menu");
        final public static Locator selectedItems = Locator.tagWithClass("span", "Select-value-label");
        final public static Locator loadingSpinner = Locator.tagWithClass("span", "Select-loading");

        public static Locator selectContainer()
        {
            return Locator.tagWithClassContaining("div", "Select");
        }

        public static Locator selectValueLabelContaining(String valueContains)
        {
            return selectedItems.containing(valueContains);
        }
        public static Locator selectValueLabel(String text)
        {
            return selectedItems.withText(text);
        }

        public static Locator containerById(String inputId)
        {
            return Locator.tagWithClass("div", "Select").withDescendant(
                    Locator.tagWithId("input", inputId));
        }

        public static Locator containerByName(String inputName)
        {
            return Locator.tagWithClass("div", "Select").withDescendant(
                    Locator.tagWithName("input", inputName));
        }

        public static Locator containerWithDescendant(Locator.XPathLocator descendant)
        {
            return Locator.tagWithClass("div", "Select").withDescendant(descendant);
        }

        public static Locator container(List<String> inputNames)
        {
            StringBuilder childXpath = new StringBuilder( "//input[@id="+ Locator.xq(inputNames.get(0)) + "");
            for (int i = 1; i < inputNames.size(); i++)
            {
                childXpath.append(" or @id=").append(Locator.xq(inputNames.get(i)));
            }
            childXpath.append("]");
            return Locator.tagWithClass("div", "Select").withDescendant(
                    Locator.xpath(childXpath.toString()));
        }

        public static Locator containerStartsWith(List<String> inputNames)
        {
            StringBuilder childXpath = new StringBuilder( "//input[starts-with(@id, '"+ inputNames.get(0) + "')");
            for (int i = 1; i < inputNames.size(); i++)
            {
                childXpath.append(" or starts-with(@id, '").append(inputNames.get(i)).append("')");
            }
            childXpath.append("]");
            return Locator.tagWithClass("div", "Select").withDescendant(
                    Locator.xpath(childXpath.toString()));
        }

    }

    public static class ReactSelectFinder extends WebDriverComponentFinder<ReactSelect, ReactSelectFinder >
    {
        private Locator _locator;

        public ReactSelectFinder(WebDriver driver)
        {
            super(driver);
            _locator= Locators.selectContainer();    // use this to find the only reactSelect in a scope
        }

        public ReactSelectFinder withIds(List<String> inputIds)
        {
            _locator = Locators.container(inputIds);
            return this;
        }

        /* the ID is for the Select > Select-Control > span > div > input of the ReactSelect */
        public ReactSelectFinder withId(String inputId)
        {
            _locator = Locators.containerById(inputId);
            return this;
        }

        public ReactSelectFinder withName(String inputName)
        {
            _locator = Locators.containerByName(inputName);
            return this;
        }

        /* use this to find a reactSelect when the label text is the only content of the label element*/
        public ReactSelectFinder withLabel(String label)
        {
            _locator = Locators.containerWithDescendant(Locator.tag("input")
                    .withAttributeMatchingOtherElementAttribute("id", Locator.tag("label").withText(label), "for"));
            return this;
        }

        /* use this to find a reactSelect when the label text is contained within a label/span*/
        public ReactSelectFinder withLabelWithSpan(String labelSpanText)
        {
            _locator = ReactSelect.Locators.containerWithDescendant(Locator.tag("input")
                    .withAttributeMatchingOtherElementAttribute("id", Locator.xpath("//label/span[contains(normalize-space(),'"+labelSpanText+"')]").parent(), "for"));
            return this;
        }

        public ReactSelectFinder followingLabelWithSpan(String labelText)
        {
            _locator = Locator.xpath("//label[./span[contains(normalize-space(),'" + labelText + "')]]/following-sibling::div/div[contains(concat(' ',normalize-space(@class),' '), ' Select ')]");
            return this;
        }

        public ReactSelectFinder withLabelContaining(String label)
        {
            _locator = ReactSelect.Locators.containerWithDescendant(Locator.tag("input")
                    .withLabelContaining(label));
            return this;
        }

        public ReactSelectFinder withIdsStartingWith(List<String> names)
        {
            _locator = ReactSelect.Locators.containerStartsWith(names);
            return this;
        }

        public ReactSelectFinder withIdStartingWith(String name)
        {
            _locator = ReactSelect.Locators.containerStartsWith(Arrays.asList(name));
            return this;
        }

        @Override
        protected ReactSelect construct(WebElement el, WebDriver driver)
        {
            return new ReactSelect(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }

}