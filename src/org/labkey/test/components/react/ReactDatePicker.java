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

public class ReactDatePicker extends WebDriverComponent<ReactDatePicker.ElementCache>
{
    private final WebDriver _driver;
    private final WebElement _el;

    private static final String YMD = "\\d{4}-\\d{2}-\\d{2}";
    private static final String YMDHS = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}";

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
            elementCache().input.getComponentElement().sendKeys(Keys.ENTER); // Dismiss date picker
    }

    public void set(String value)
    {
        set(value, true);
    }


    /**
     * use keyboard input to set year, month. then use picker to select day and time
     * @param value date or datetime string
     * @return false if date string is not one of "yyyy-MM-dddd" or "yyyy-MM-dddd hh:ss" format
     */
    public boolean select(String value)
    {
        if (value.matches(YMD) || value.matches(YMDHS))
        {
            set("", false);
            String[] dateParts = getParsedDateParts(value);
            set(dateParts[0], false); // use keyboard input to set year and month
            elementCache().datePickerDateCell(dateParts[1]).click(); // use calendar ui to select day
            if (!StringUtils.isEmpty(dateParts[2])) // use timepicker to select time
                clickTime(dateParts[2]);

            return true;
        }

        return false;
    }

    public void clear()
    {
        set("");
        WebDriverWrapper.waitFor(()-> !isExpanded(), "Date picker didn't close", 1000);
    }

    public ReactDatePicker clickTime(String time)
    {
        expand();
        elementCache().timeListItem(time).click();
        WebDriverWrapper.waitFor(()-> !isExpanded(), "Date picker didn't close", 1000);
        return this;
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

    /**
     * Parse "2022-07-11 08:30" to ["2022-07", "11", "8:30 AM"]
     * @param fullDateStr
     * @return
     */
    private String[] getParsedDateParts(String fullDateStr)
    {
        String[] parts = fullDateStr.split(" ") ;
        String[] dayParts = parts[0].split("-");
        String yearMon = dayParts[0] + "-" + dayParts[1];
        String dayPart = dayParts[2];
        String timePart = parts.length > 1 ? parts[1] : "";
        if (!StringUtils.isEmpty(timePart))
        {
            String[] timeParts = timePart.split(":");
            String amPM = "AM";
            int hour = Integer.parseInt(timeParts[0]);
            if (hour > 12)
            {
                amPM = "PM";
                hour -= 12;
            }
            timePart = hour + ":" + timeParts[1] + " " + amPM;
        }

        return new String[]{yearMon, dayPart, timePart};
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

        WebElement timeList = Locator.tagWithClass("div", "react-datepicker__time-box")
                    .child(Locator.tagWithClass("ul", "react-datepicker__time-list"))
                    .refindWhenNeeded(popup);

        WebElement timeListItem(String text)
        {
            return Locator.tagWithClass("li", "react-datepicker__time-list-item").withText(text)
                    .findElement(timeList);
        }

        /**
         * Return the date cell div of react datepicker
         * @param day '01', '02', ... '31'
         * @return
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
