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
        if (enabled && isDisabled())
        {
            if (hasButtons()) selectFirst();
            else getComponentElement().click();
        }
        else if (!enabled && isEnabled())
        {
            if (hasButtons()) selectSecond();
            else getComponentElement().click();
        }
        WebDriverWrapper.waitFor(()-> isEnabled() == enabled,
                "the toggle button did not become " + desiredState, 2000);
        return this;
    }

    public boolean isEnabled()
    {
        return getComponentElement().getAttribute("class").contains("toggle-on");
    }

    public boolean isDisabled()
    {
        return getComponentElement().getAttribute("class").contains("toggle-off");
    }

    /*
        the 'off' status causes the 2nd button to be active; otherwise, the one 1st button is active
     */
    public String getSelectedStatus()
    {
        if (isEnabled())
            return Locator.tag("button").index(0).findElement(this).getText();
        else
            return Locator.tag("button").index(1).findElement(this).getText();
    }

    public ToggleButton selectFirst()
    {
        Locator.tag("button").index(0).findElement(this).click();
        return this;
    }

    public ToggleButton selectSecond()
    {
        Locator.tag("button").index(1).findElement(this).click();
        return this;
    }

    private boolean hasButtons()
    {
        return Locator.tag("button").findElementOrNull(this) != null;
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
                locator = locator.withDescendant(Locator.tagWithText("button", _state));
            }

            return locator;
        }
    }
}
