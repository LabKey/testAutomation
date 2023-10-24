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
        if (enabled && !isOn())
        {
            if (hasButtons()) selectFirst();
            else getComponentElement().click();
        }
        else if (!enabled && isOn())
        {
            if (hasButtons()) selectSecond();
            else getComponentElement().click();
        }
        else
        {
            throw new IllegalStateException("Unable to determine toggle state: " + getComponentElement());
        }
        WebDriverWrapper.waitFor(()-> isOn() == enabled,
                "the toggle button did not become " + desiredState, 2000);
        return this;
    }

    public boolean isOn()
    {
        return getComponentElement().getAttribute("class").contains("toggle-on");
    }

    /*
        the 'off' status causes the 2nd button to be active; otherwise, the one 1st button is active
     */
    public String getSelectedStatus()
    {
        if (isOn())
            return Locator.tag("button").index(0).findElement(this).getText();
        else
            return Locator.tag("button").index(1).findElement(this).getText();
    }

    private ToggleButton selectFirst()
    {
        Locator.tag("button").index(0).findElement(this).click();
        return this;
    }

    private ToggleButton selectSecond()
    {
        Locator.tag("button").index(1).findElement(this).click();
        return this;
    }

    private Boolean hasButtons = null;
    private boolean hasButtons()
    {
        if (hasButtons == null)
            hasButtons = Locator.tag("button").existsIn(this);
        return hasButtons;
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
