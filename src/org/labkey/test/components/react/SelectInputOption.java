package org.labkey.test.components.react;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.collections.CaseInsensitiveMapWrapper;
import org.labkey.api.util.Pair;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.test.components.html.Input.Input;

/*
    This component is meant to wrap the verbose options in filteringReactSelect, ReactSelect
 */
public class SelectInputOption extends WebDriverComponent<SelectInputOption.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected SelectInputOption(WebElement element, WebDriver driver)
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

    public boolean isFocused()
    {
        return getComponentElement().getAttribute("class").contains("select-input__option--is-focused");
    }

    public Map<String, String> getData()
    {
        return elementCache().getData();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        public Locator.XPathLocator text_truncatePairLoc = Locator.tagWithClass("div", "text__truncate");

        public Map<String, String> getData()
        {
            Map<String, String> data = new CaseInsensitiveHashMap<>();
            var elements = text_truncatePairLoc.findElements(this);
            for (WebElement el : elements)
            {
                WebElement keyEl = Locator.tagWithClass("span", "identifying_field_label").findElement(el);
                WebElement valEl = Locator.tag("span").findElement(el);
                data.put(StringUtils.stripEnd(keyEl.getText(), ":"), valEl.getText());
            }
            return data;
        }

    }


    public static class SelectInputOptionFinder extends WebDriverComponentFinder<SelectInputOption, SelectInputOptionFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "select-input__option");
        private String _key = null;
        private String _value = null;

        public SelectInputOptionFinder(WebDriver driver)
        {
            super(driver);
        }

        public SelectInputOptionFinder withValue(String key, String value)
        {
            _key = key;
            _value = value;
            return this;
        }

        @Override
        protected SelectInputOption construct(WebElement el, WebDriver driver)
        {
            return new SelectInputOption(el, driver);
        }


        @Override
        protected Locator locator()
        {
            if (_key != null)
                return _baseLocator.withChild(Locator.tagWithClass("div", "text__truncate")
                        .withChild(Locator.tagWithText("strong",_key))
                        .parent()   // children are siblings
                        .withChild(Locator.tagWithAttributeContaining("span", "title", _value)));
            else
                return _baseLocator;
        }
    }
}
