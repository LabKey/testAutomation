package org.labkey.test.components.ui;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class FilterStatusValue extends WebDriverComponent<FilterStatusValue.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected FilterStatusValue(WebElement element, WebDriver driver)
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

    private boolean isActive()
    {
        return getComponentElement().getAttribute("class").contains("is-active");
    }

    private boolean isClose()
    {
        return elementCache().icon.getAttribute("class").contains("fa-close");
    }

    public void remove()
    {
        String originalText = getText();
        getWrapper().mouseOver(getComponentElement());
        getWrapper().mouseOver(elementCache().icon);
        WebDriverWrapper.waitFor(()-> isActive() && isClose(),
                "the filter status item with text ["+getText()+"] did not become active", 500);
        elementCache().icon.click();

        // if the item you're dismissing is not the rightmost, it won't become stale; instead, its text will
        // be swapped out with the one to its right.  So, we check to see that either the text has changed or
        // the item became stale.
        WebDriverWrapper.waitFor(()->  {
                return ExpectedConditions.stalenessOf(getComponentElement()).apply(getDriver())
                        || !getText().equals(originalText);
        }, "the value item ["+originalText+"] did not disappear", 1000);
    }

    /**
     * A filter will be locked if it was applied by a view.
     *
     * @return True if the locked icon is present, false otherwise.
     */
    public boolean isLocked()
    {
        return Locator.tagWithClass("i", "fa-lock").findWhenNeeded(this).isDisplayed();
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


    public static class FilterStatusValueFinder extends WebDriverComponentFinder<FilterStatusValue, FilterStatusValueFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "filter-status-value");
        private String _text = null;
        private FilterStatusType _type = null;

        public FilterStatusValueFinder(WebDriver driver)
        {
            super(driver);
        }

        public FilterStatusValueFinder withText(String text)
        {
            _text = text;
            return this;
        }

        public FilterStatusValueFinder withType(FilterStatusType type)
        {
            _type = type;
            return this;
        }

        @Override
        protected FilterStatusValue construct(WebElement el, WebDriver driver)
        {
            return new FilterStatusValue(el, driver);
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

    public enum FilterStatusType
    {
        filter("fa-filter"),
        view("fa-table");

        private final String iconCls;

        FilterStatusType(String iconCls)
        {
            this.iconCls = iconCls;
        }

        public String getIconClass()
        {
            return iconCls;
        }
    }
}
