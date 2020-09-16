package org.labkey.test.components.ui;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

public class ReactDatePicker extends WebDriverComponent<ReactDatePicker.ElementCache>
{
    private WebDriver _driver;
    private WebElement _el;

    public ReactDatePicker(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    public WebElement getComponentElement()
    {
        return _el;
    }

    public WebDriver getDriver()
    {
        return _driver;
    }

    public String get()
    {
        return elementCache().input.get();
    }

    public ReactDatePicker set(String value)
    {
        return set(value, true);
    }

    public ReactDatePicker set(String value, boolean collapseAfter)
    {
        elementCache().input.set(value);
        if (collapseAfter)
            collapse();
        return this;
    }

    public void clear()
    {
        elementCache().closeBtn().click();
        getWrapper().waitFor(()-> !isExpanded() && elementCache().input.get().isEmpty(), 1000);
    }

    public ReactDatePicker clickTime(String time)
    {
        expand();
        elementCache().timeListItem(time).click();
        getWrapper().waitFor(()-> !isExpanded(), 1000);
        return this;
    }

    private boolean isExpanded()
    {
        return elementCache().popupLoc.existsIn(getDriver());
    }

    private ReactDatePicker expand()
    {
        if (!isExpanded())
        {
            getComponentElement().click();
            getWrapper().waitFor(()-> isExpanded(), 1500);
        }
        return this;
    }

    private ReactDatePicker collapse()
    {
        if (isExpanded())
        {
            // attempt to click at a spot just above or below the containing element, on
            // the standard way to close the dialog is to click outside of its containing rectangle,
            // or select a time from the time list
            String placement = elementCache().popup().getAttribute("data-placement");
            int offset = elementCache().inputContainer.getSize().height;
            int height = placement == "bottom-start" ? offset : -offset;    // attempt to click above or below,
                                                                            // depending on where the picker is
            Actions builder = new Actions(getDriver());
            builder.moveToElement(elementCache().inputContainer, 0, height)
                    .click()
                    .build()
                    .perform();

            getWrapper().waitFor(() -> !isExpanded(), 1500);
        }
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component.ElementCache
    {
        Locator popupLoc = Locator.tagWithClass("div", "react-datepicker-popper");
        Locator closeBtnLoc = Locator.tagWithClass("button", "react-datepicker__close-icon");
        WebElement inputContainer = Locator.tagWithClass("div", "react-datepicker__input-container")
                .findElement(this);
        public Input input = new Input(Locator.tag("input").findWhenNeeded(inputContainer), getDriver());

        public WebElement closeBtn()
        {
            return closeBtnLoc.findElement(inputContainer);
        }

        WebElement popup()
        {
            return popupLoc.findElementOrNull(getDriver());  // it falls outside of the component
        }

        WebElement timeList()
        {
            if (popup() != null)
                return Locator.tagWithClass("div", "react-datepicker__time-box")
                    .child(Locator.tagWithClass("ul", "react-datepicker__time-list"))
                    .findElement(popup());
            else return null;
        }
        WebElement timeListItem(String text)
        {
            if (timeList() != null)
                return Locator.tagWithClass("li", "react-datepicker__time-list-item")
                    .withText(text).findElement(timeList());
            else return null;
        }
    }

    public static class ReactDateInputFinder extends WebDriverComponentFinder<ReactDatePicker, ReactDateInputFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "react-datepicker-wrapper")
            .withDescendant(Locator.tag("input"));
        private String _inputId = null;
        private String _name = null;

        public ReactDateInputFinder(WebDriver driver)
        {
            super(driver);
        }

        public ReactDateInputFinder withInputId(String id)
        {
            _inputId = id;
            return this;
        }

        public ReactDateInputFinder withName(String name)
        {
            _name = name;
            return this;
        }

        @Override
        protected ReactDatePicker construct(WebElement el, WebDriver driver)
        {
            return new ReactDatePicker(el, driver);
        }


        @Override
        protected Locator locator()
        {
            if (_inputId != null)
                return _baseLocator.withDescendant(Locator.inputById( _inputId));
            else if (_name != null)
                return _baseLocator.withDescendant(Locator.input(_name));
            else
                return _baseLocator;
        }
    }
}
