package org.labkey.test.components.react;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ReactDateTimePicker extends WebDriverComponent<ReactDateTimePicker.ElementCache>
{
    private final WebDriver _driver;
    private final WebElement _el;

    protected ReactDateTimePicker(WebElement element, WebDriver driver)
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
        String beforeValue = elementCache().input.get();

        elementCache().input.set(value);

        // When setting a value by text there is a slight delay as formatting, if present, is applied.
        if(!value.equals(beforeValue))
        {
            WebDriverWrapper.waitFor(()->!elementCache().input.get().equals(beforeValue),
                    "Updating ReactDateTimePicker with text failed.", 1_000);
        }
        else
        {
            WebDriverWrapper.sleep(500);
        }

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
     * Can be used to set a DateTime, Date-only or Time-only field. Pass in a LocalDateTime, LocalDate or LocalTime
     * object to use the picker to set the field. If a text value is passed in it is used as a literal and jut typed
     * into the textbox.
     * If a LocalDateTime or LocalDate object is passed in the keyboard is used to input the year and month then use
     * picker to select day and time (for LocateDateTime).
     *
     * @param dateTime A LocalDateTime, LocalDate, LocalTime or String.
     */
    public void select(Object dateTime)
    {
        if(dateTime instanceof LocalDateTime localDateTime)
        {
            set("", false);
            set(localDateTime.getYear() + "-" + localDateTime.getMonthValue(), false); // use keyboard input to set year and month
            elementCache().datePickerDateCell(localDateTime.getDayOfMonth()).click(); // use calendar ui to select day
            if (!localDateTime.toLocalTime().equals(LocalTime.MIDNIGHT)) // use timepicker to select time
                selectTime(localDateTime.toLocalTime());
            else
                dismiss();
        }
        else if(dateTime instanceof LocalDate localDate)
        {
            selectDate(localDate);
        }
        else if(dateTime instanceof LocalTime localTime)
        {
            selectTime(localTime);
        }
        else if(dateTime instanceof String setValue)
        {
            set(setValue, true);
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Unable to use type %s to set a DateTime, Date or Time field.", dateTime.getClass()));
        }

    }

    /**
     * Use for a date-only field.
     * @param date Date in a {@link LocalDate} object.
     */
    public void selectDate(LocalDate date)
    {
        set("", false);
        set(date.getYear() + "-" + date.getMonthValue(), false); // use keyboard input to set year and month
        elementCache().datePickerDateCell(date.getDayOfMonth()).click(); // use calendar ui to select day
    }

    public void clear()
    {

        if(elementCache().clearButton.isDisplayed())
        {
            elementCache().clearButton.click();
        }
        else
        {
            set("");
        }
    }

    /**
     * Use for a time-only field.
     * @param time Time in a {@link LocalTime} object.
     */
    public void selectTime(LocalTime time)
    {
        // Will do nothing if the picker is already expanded.
        expand();

        // Use the first entry in the list of times as a guide to the time format.
        String timeExample = datePickerTimeListItemLocator.findElement(elementCache().popup).getText();

        String pattern  = StringUtils.countMatches(timeExample, ":") == 1 ? "hh:mm" : "hh:mm:ss";

        if(timeExample.toUpperCase().contains("AM"))
        {
            pattern = pattern + " a";
        }
        else
        {
            pattern = pattern.replace("hh", "HH");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        WebElement timeElement = elementCache().datePickerTime(time.format(formatter));
        getWrapper().fireEvent(timeElement, WebDriverWrapper.SeleniumEvent.click);
        WebDriverWrapper.waitFor(()-> !isExpanded(), "Time picker didn't close.", 1000);
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

    private static final Locator datePickerTimeListItemLocator = Locator.tagWithClass("li", "react-datepicker__time-list-item");

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
        WebElement datePickerDateCell(int day)
        {
            return datePickerDateLoc(StringUtils.leftPad(String.valueOf(day), 2, "0")).findElement(popup);
        }

        WebElement datePickerTime(String timeStr)
        {
            return datePickerTimeListItemLocator.withText(timeStr)
                    .findWhenNeeded(elementCache().popup);
        }

        WebElement clearButton = Locator.tagWithClass("button", "react-datepicker__close-icon")
                .refindWhenNeeded(inputContainer);
    }

    static Locator.XPathLocator datePickerDateLoc(String datePart)
    {
        return Locator.tagWithClass("div", "react-datepicker__day--0" + datePart)
                // Don't get day from previous month
                .withoutClass("react-datepicker__day--outside-month");
    }

    public static class ReactDateTimeInputFinder extends WebDriverComponentFinder<ReactDateTimePicker, ReactDateTimeInputFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "react-datepicker-wrapper")
            .withDescendant(Locator.tag("input"));
        private String _inputId = null;
        private String _name = null;
        private String _placeholder = null;

        private String _className = null;

        public ReactDateTimeInputFinder(WebDriver driver)
        {
            super(driver);
        }

        public ReactDateTimeInputFinder withInputId(String id)
        {
            _inputId = id;
            return this;
        }

        public ReactDateTimeInputFinder withName(String name)
        {
            _name = name;
            return this;
        }

        public ReactDateTimeInputFinder withPlaceholder(String placeholder)
        {
            _placeholder = placeholder;
            return this;
        }

        public ReactDateTimeInputFinder withClassName(String className)
        {
            _className = className;
            return this;
        }

        @Override
        protected ReactDateTimePicker construct(WebElement el, WebDriver driver)
        {
            return new ReactDateTimePicker(el, driver);
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
