package org.labkey.test.components.glassLibrary.components;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.selenium.EphemeralWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class RbtCombo extends WebDriverComponent<RbtCombo.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public RbtCombo(WebElement element, WebDriver driver)
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

    public RbtCombo setValue(String value)
    {
        if (hasValue())
            clear();
        elementCache().input.sendKeys( value);

        elementCache().option(value).click();
        WebDriverWrapper.waitFor(()-> !isOpened() && getValue().equals(value),
                "the value was not set, or the combo did not close", 2000);
        return this;
    }

    public String getValue()
    {
        return elementCache().input.getAttribute("value");
    }

    public boolean hasValue()
    {
        return !getValue().isEmpty();
    }

    // open
    public boolean isOpened()
    {
        return elementCache().input.getAttribute("aria-expanded").equals("true");
    }

    public RbtCombo clear()
    {
        if (hasValue())
            elementCache().clearBtn.click();

        WebDriverWrapper.waitFor(()-> !hasValue(), "the value was not cleared in time", 2000);
        return this;
    }


    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        Locator inputLoc = Locator.tagWithClass("input", "rbt-input-main");
        WebElement input = inputLoc.refindWhenNeeded(this);
        EphemeralWebElement clearBtn = new EphemeralWebElement(
                Locator.tagWithClass("button", "rbt-close"), this);

        EphemeralWebElement menu = new EphemeralWebElement(
                Locator.tagWithClass("ul", "rbt-menu"), this);
        List<WebElement> options()
        {
            return Locator.tagWithClass("a", "dropdown-item").findElements(menu);
        }
        WebElement option(String text)
        {
            return Locator.tagWithClass("a", "dropdown-item").withDescendant(Locator.tagWithText("mark", text))
                    .waitForElement(menu, 2000);
        }
    }


    public static class RbtSelectFinder extends WebDriverComponentFinder<RbtCombo, RbtSelectFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "rbt")
                .withDescendant(Locator.tagWithClass("input", "rbt-input")).parent();
        private String _label = null;

        public RbtSelectFinder(WebDriver driver)
        {
            super(driver);
        }

        public RbtSelectFinder withLabel(String label)
        {
            _label = label;
            return this;
        }

        @Override
        protected RbtCombo construct(WebElement el, WebDriver driver)
        {
            return new RbtCombo(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_label != null)
                return _baseLocator.withChild(Locator.tagWithText("div", _label));
            else
                return _baseLocator;
        }
    }
}
