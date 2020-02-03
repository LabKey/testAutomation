package org.labkey.test.components.html;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ToggleButton extends WebDriverComponent<ToggleButton.ElementCache>
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

    public boolean get()
    {
        return !getComponentElement().getAttribute("class").contains("off");
    }
    

    public static class ToggleButtonFinder extends WebDriverComponentFinder<ToggleButton, ToggleButtonFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "toggle");
        private String _state = null;

        public ToggleButtonFinder(WebDriver driver)
        {
            super(driver);
        }

        public ToggleButtonFinder withState(String state)
        {
            _state = state;
            return this;
        }

        @Override
        protected ToggleButton construct(WebElement el, WebDriver driver)
        {
            return new ToggleButton(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_state != null)
                return _baseLocator.withDescendant(Locator.tagWithText("span", _state));
            else
                return _baseLocator;
        }
    }
}
