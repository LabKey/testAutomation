package org.labkey.test.components.react;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ToggleButton extends WebDriverComponent<WebDriverComponent<?>.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    protected ToggleButton(WebElement element, WebDriver driver)
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

    public ToggleButton set(boolean enabled)
    {
        String desiredState = enabled ? "enabled" : "disabled";
        if (get() != enabled)
            getComponentElement().click();
        WebDriverWrapper.waitFor(()-> get() == enabled,
                "the toggle button did not become " + desiredState, 2000);
        return this;
    }

    /*
        The componentElement will get the 'off' class when the slider is grayed out.
     */
    public boolean get()
    {
        return !getComponentElement().getAttribute("class").contains("off");
    }

    /*
        the 'off' status causes the child span with 'toggle-off' class to be shown; otherwise,
        the one with 'toggle-on' is shown.
     */
    public String getSelectedStatus()
    {
        if (get())
            return Locator.tagWithClass("span", "toggle-on").findElement(this).getText();
        else
            return Locator.tagWithClass("span", "toggle-off").findElement(this).getText();
    }

    public static class ToggleButtonFinder extends WebDriverComponentFinder<ToggleButton, ToggleButtonFinder>
    {
        private static final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "toggle");
        private String _state = null;
        private String _containerClass = null;

        public ToggleButtonFinder(WebDriver driver)
        {
            super(driver);
        }

        public ToggleButtonFinder withState(String state)
        {
            _state = state;
            return this;
        }

        public ToggleButtonFinder withContainerClass(String containerClass)
        {
            _containerClass = containerClass;
            return  this;
        }

        @Override
        protected ToggleButton construct(WebElement el, WebDriver driver)
        {
            return new ToggleButton(el, driver);
        }

        @Override
        protected Locator locator()
        {
            Locator.XPathLocator locator = _baseLocator;
            if (_containerClass != null)
            {
                locator = Locator.tagWithClassContaining("div", _containerClass).descendant(locator);
            }
            if (_state != null)
            {
                locator = locator.withDescendant(Locator.tagWithText("span", _state));
            }

            return locator;
        }
    }
}
