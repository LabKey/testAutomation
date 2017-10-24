package org.labkey.test.selenium;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.selenium.WebDriverUtils;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.WrapsDriver;
import org.openqa.selenium.internal.WrapsElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.List;

public class ReclickingWebElement extends WebElementDecorator
{
    public ReclickingWebElement(WebElement decoratedElement)
    {
        super(decoratedElement);
    }

    @Override
    public void click()
    {
        try
        {
            super.click();
        }
        catch (TimeoutException rethrow)
        {
            throw rethrow; // No retry for WebDriver timeout
        }
        catch (WebDriverException tryAgain)
        {
            if (getDriver() != null)
            {
                TestLogger.debug("Retry click: " + tryAgain.getMessage().split("\n")[0]);
                boolean clickBlocked = tryAgain.getMessage().contains("Other element would receive the click");
                revealElement(getWrappedElement(), clickBlocked);
                super.click();
            }
            else
            {
                throw tryAgain;
            }
        }
    }

    // Allows interaction with elements that have been obscured by the floating page header
    private void revealElement(WebElement el, boolean clickBlocked)
    {
        boolean revealed = false;
        WebDriverUtils.ScrollUtil scrollUtil = new WebDriverUtils.ScrollUtil(getDriver());
        if (clickBlocked)
        {
            revealed = scrollUtil.scrollUnderFloatingHeader(el);
        }
        if (!revealed)
        {
            scrollUtil.scrollIntoView(el);
            if (clickBlocked)
                WebDriverWrapper.sleep(2500); // Wait for a mask to disappear
            new WebDriverWait(getDriver(), 10).until(ExpectedConditions.elementToBeClickable(el));
        }
    }

    private final List<WebDriver> _webDriver = new ArrayList<>();
    private WebDriver getDriver()
    {
        if (_webDriver.isEmpty())
        {
            Object peeling = getWrappedElement();
            while (peeling instanceof WrapsElement)
            {
                peeling = ((WrapsElement) peeling).getWrappedElement();
            }
            while (peeling instanceof WrapsDriver)
            {
                peeling = ((WrapsDriver) peeling).getWrappedDriver();
            }
            if (peeling instanceof WebDriver)
                _webDriver.add((WebDriver) peeling);
            else
                _webDriver.add(null); // Only dig once, even if null
        }
        return _webDriver.get(0);
    }
}
