package org.labkey.test.components.html;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebDriverWrapperImpl;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class Input extends WebDriverComponent implements FormItem<String>
{
    protected final WebElement _el;
    protected final WebDriverWrapper _wDriver; // getFormElement requires javascript

    public Input(WebElement el, WebDriver driver)
    {
        _el = el;
        _wDriver = new WebDriverWrapperImpl(driver);
    }

    public static SimpleComponentFinder<Input> Input(Locator loc, WebDriver driver)
    {
        return new SimpleComponentFinder<Input>(loc)
        {
            @Override
            protected Input construct(WebElement el)
            {
                return new Input(el, driver);
            }
        };
    }

    @Override
    protected WebDriver getDriver()
    {
        return getWrapper().getDriver();
    }

    protected WebDriverWrapper getWrapper()
    {
        return _wDriver;
    }

    @Deprecated
    public String getValue()
    {
        return get();
    }

    @Deprecated
    public void setValue(String value)
    {
        set(value);
    }

    @Override
    public String get()
    {
        return getWrapper().getFormElement(_el);
    }

    @Override
    public void set(String value)
    {
        getWrapper().setFormElement(_el, value);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }
}
