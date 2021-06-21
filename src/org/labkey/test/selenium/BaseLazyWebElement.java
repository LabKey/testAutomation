package org.labkey.test.selenium;

import org.openqa.selenium.WebElement;

public abstract class BaseLazyWebElement extends WebElementWrapper
{
    private WebElement _wrappedElement;

    @Override
    public WebElement getWrappedElement()
    {
        if (null == _wrappedElement)
        {
            _wrappedElement = findWrappedElement();
        }

        return _wrappedElement;
    }

    protected void setWrappedElement(WebElement el)
    {
        _wrappedElement = el;
    }

    protected abstract WebElement findWrappedElement();

    protected String getUnfoundToString()
    {
        return super.toString();
    }

    @Override
    public String toString()
    {
        if (_wrappedElement != null)
        {
            return _wrappedElement.toString();
        }
        else
        {
            return getUnfoundToString();
        }
    }
}
