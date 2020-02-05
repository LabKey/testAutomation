package org.labkey.test.pages.core.login;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SvgCheckbox extends WebDriverComponent<SvgCheckbox.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public SvgCheckbox(WebElement element, WebDriver driver)    // the intended element is a span, with class no-highlight clickable
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

    public boolean get()
    {
        return elementCache().svgSquare.getAttribute("data-icon").equals("check-square");
    }

    public SvgCheckbox set(boolean check)
    {
        if (check != get())
        {
            String intendedState = check ? "checked" : "unchecked";
            getComponentElement().click();
            WebDriverWrapper.waitFor(()-> check == get(),
                    "the checkbox did not become " + intendedState + "in time.", 2000);
        }
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends WebDriverComponent.ElementCache
    {
        final WebElement svgSquare = Locator.tag("svg").withClass("svg-inline--fa").findElement(this);
    }
}
