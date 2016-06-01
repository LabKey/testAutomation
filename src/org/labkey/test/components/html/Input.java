package org.labkey.test.components.html;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebDriverWrapperImpl;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class Input extends WebDriverComponent
{
    protected final WebElement _el;
    protected final WebDriverWrapper _wDriver; // getFormElement requires javascript

    public Input(WebElement el, WebDriver driver)
    {
        _el = el;
        _wDriver = new WebDriverWrapperImpl(driver);
    }

    @Override
    protected WebDriver getDriver()
    {
        return _wDriver.getDriver();
    }

    public String getValue()
    {
        return _wDriver.getFormElement(_el);
    }

    public void setValue(String value)
    {
        _wDriver.setFormElement(_el, value);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }
}
