package org.labkey.test.components.react;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class ReactDatePicker extends WebDriverComponent<ReactDatePicker.ElementCache>
{
    private final WebDriver _driver;
    private final WebElement _el;

    protected ReactDatePicker(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    public String get()
    {
        return elementCache().input.get();
    }

    public void set(String value, boolean close)
    {
        elementCache().input.set(value);
        if (close)
            dismiss();
    }

    private void dismiss()
    {
        elementCache().input.getComponentElement().sendKeys(Keys.ENTER); // Dismiss date picker

        WebDriverWrapper.waitFor(()-> !isExpanded(), "Date picker didn't close", 1000);
    }

    public void set(String value)
    {
        set(value, true);
    }

    /**
     * Use keyboard input to set year, month. then use picker to select day and time
     * Midnight time values will be ignored
     * @param value date or datetime value
     */
    public void select(LocalDateTime value)
    {
        set("", false);
        set(value.getYear() + "-" + value.getMonthValue(), false); // use keyboard input to set year and month
        elementCache().datePickerDateCell(String.valueOf(value.getDayOfMonth())).click(); // use calendar ui to select day
        if (!value.toLocalTime().equals(LocalTime.MIDNIGHT)) // use timepicker to select time
            clickTime(value.toLocalTime());
        else
            dismiss();
    }

    public void clear()
    {
        set("");
    }

    private void clickTime(LocalTime time)
    {
        int hour = time.getHour();
        String amPm = "AM";
        if (hour == 0)
            hour = 12;
        else if (hour == 12)
            amPm = "PM";
        else if (hour > 12)
        {
            hour -= 12;
            amPm = "PM";
        }
        String timeStr = "%d:%d %s".formatted(hour, time.getMinute(), amPm);
        WebElement liElement = Locator.tagWithClass("ul", "react-datepicker__time-list")
                .child(Locator.tagWithText("li", timeStr)).findElement(elementCache().popup);
        getWrapper().fireEvent(liElement, WebDriverWrapper.SeleniumEvent.click);

        WebDriverWrapper.waitFor(()-> !isExpanded(), "Date picker didn't close", 1000);
    }

    private boolean isExpanded()
    {
        return elementCache().popup.isDisplayed();
    }

    private void expand()
    {
        if (!isExpanded())
        {
            getComponentElement().click();
            WebDriverWrapper.waitFor(this::isExpanded, "Date picker didn't open", 1500);
        }
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        WebElement inputContainer = Locator.tagWithClass("div", "react-datepicker__input-container")
                .findElement(this);
        public Input input = new Input(Locator.tag("input").findWhenNeeded(inputContainer), getDriver());

        WebElement popup = Locator.xpath(".").followingSibling("div").withClass("react-datepicker__tab-loop")
                .refindWhenNeeded(this);

        /**
         * Return the date cell div of react datepicker
         * @param day '01', '02', ... '31'
         */
        WebElement datePickerDateCell(String day)
        {
            return datePickerDateLoc(day).findElement(popup);
        }
    }

    static Locator.XPathLocator datePickerDateLoc(String datePart)
    {
        return Locator.tagWithClass("div", "react-datepicker__day--0" + datePart);
    }

    public static class ReactDateInputFinder extends WebDriverComponentFinder<ReactDatePicker, ReactDateInputFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "react-datepicker-wrapper")
            .withDescendant(Locator.tag("input"));
        private String _inputId = null;
        private String _name = null;
        private String _placeholder = null;

        private String _className = null;

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

        public ReactDateInputFinder withPlaceholder(String placeholder)
        {
            _placeholder = placeholder;
            return this;
        }

        public ReactDateInputFinder withClassName(String className)
        {
            _className = className;
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
            else if (_placeholder != null)
                return _baseLocator.withDescendant(Locator.tagWithAttribute("input", "placeholder", _placeholder));
            else if (_className != null)
                return _baseLocator.withDescendant(Locator.byClass(_className));
            else
                return _baseLocator;
        }
    }
}
