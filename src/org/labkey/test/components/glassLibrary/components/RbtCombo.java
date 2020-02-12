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

    /**
     * TODO:
     * For components that are, essentially, singletons on a page, you may want to omit this Finder class
     * Note that even in that case, a Finder class can be useful for lazily finding components
     * Usage: 'new Component.ComponentFinder(getDriver()).withTitle("title").findWhenNeeded();'
     */
    public static class RbtSelectFinder extends WebDriverComponentFinder<RbtCombo, RbtSelectFinder>
    {
        // TODO: This locator should find all instances of this component
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "my-component");
        private String _title = null;

        public RbtSelectFinder(WebDriver driver)
        {
            super(driver);
        }

        public RbtSelectFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected RbtCombo construct(WebElement el, WebDriver driver)
        {
            return new RbtCombo(el, driver);
        }

        /**
         * TODO:
         * Add methods and fields, as appropriate, to build a Locator that will find the element(s)
         * that this component represents
         */
        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withAttribute("title", _title);
            else
                return _baseLocator;
        }
    }
}
