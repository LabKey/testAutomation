package org.labkey.test.components.ui;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class OmniBoxValue extends WebDriverComponent<OmniBoxValue.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected OmniBoxValue(WebElement element, WebDriver driver)
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

    public String getText()
    {
        return elementCache().textSpan().getText();
    }

    public boolean isActive()
    {
        return getComponentElement().getAttribute("class").contains("is-active");
    }

    public boolean isSort()
    {
        return elementCache().icon.getAttribute("class").contains("fa-sort");
    }

    public boolean isFilter()
    {
        return elementCache().icon.getAttribute("class").contains("fa-filter");
    }

    public boolean isSearch()
    {
        return elementCache().icon.getAttribute("class").contains("fa-search");
    }

    public boolean isClose()
    {
        return elementCache().icon.getAttribute("class").contains("fa-close");
    }

    public Input openEdit()
    {
        getWrapper().mouseOver(getComponentElement());
        WebDriverWrapper.waitFor(()-> isActive() && isClose(),
                "the omnibox item with text ["+getText()+"] did not become active", 500);
        elementCache().textSpan().click();
        return Input.Input(Locator.tagWithClass("div", "OmniBox-input")
                .child(Locator.tag("input").withAttribute("value")), getDriver()).waitFor();
    }

    public void dismiss()
    {
        String originalText = getText();
        getWrapper().mouseOver(getComponentElement());
        WebDriverWrapper.waitFor(()-> isActive() && isClose(),
                "the omnibox item with text ["+getText()+"] did not become active", 500);
        elementCache().icon.click();

        // if the item you're dismissing is not the rightmost, it won't become stale; instead, its text will
        // be swapped out with the one to its right.  So, we check to see that either the text has changed or
        // the item became stale.
        WebDriverWrapper.waitFor(()->  {
                return ExpectedConditions.stalenessOf(getComponentElement()).apply(getDriver())
                        || !getText().equals(originalText);
        }, "the value item ["+originalText+"] did not disappear", 1000);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public WebElement textSpan()
        {
            return Locator.tag("span").findElement(getComponentElement());
        }

        public final WebElement icon = Locator.tag("i").findWhenNeeded(getComponentElement());
    }


    public static class OmniBoxValueFinder extends WebDriverComponentFinder<OmniBoxValue, OmniBoxValueFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "OmniBox-value");
        private String _text = null;
        private OmniType _type = null;

        public OmniBoxValueFinder(WebDriver driver)
        {
            super(driver);
        }

        public OmniBoxValueFinder withText(String text)
        {
            _text = text;
            return this;
        }

        public OmniBoxValueFinder withType(OmniType type)
        {
            _type = type;
            return this;
        }

        @Override
        protected OmniBoxValue construct(WebElement el, WebDriver driver)
        {
            return new OmniBoxValue(el, driver);
        }

        @Override
        protected Locator locator()
        {
            Locator.XPathLocator locator = _baseLocator;
            if (_text != null)
            {
                locator = locator.withDescendant(Locator.tag("span").containingIgnoreCase(_text));
            }
            if (_type != null)
            {
                locator = locator.withChild(Locator.tagWithClass("i", _type.getIconClass()));
            }

            return locator;
        }
    }

    public enum OmniType
    {
        sort("fa-sort"),
        filter("fa-filter"),
        search("fa-search"),
        view("fa-table");

        private final String iconCls;

        OmniType(String iconCls)
        {
            this.iconCls = iconCls;
        }

        public String getIconClass()
        {
            return iconCls;
        }
    }
}
