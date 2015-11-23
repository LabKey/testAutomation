package org.labkey.test.components.ext4;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.ComponentElements;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class Window extends Component
{
    WebElement _window;
    WebDriverWrapper _driver;
    Elements _elements;

    public Window(String windowTitle, WebDriverWrapper driver)
    {
        this(Ext4Helper.Locators.window(windowTitle).waitForElement(driver.getDriver(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT), driver);
    }

    public Window(WebElement window, WebDriverWrapper driver)
    {
        _window = window;
        _driver = driver;
        _elements = new Elements();
    }

    protected WebDriverWrapper getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _window;
    }

    public void clickButton(String buttonText)
    {
        _driver.clickAndWait(elements().findButton(buttonText));
    }

    public void clickButton(String buttonText, int msWait)
    {
        _driver.clickAndWait(elements().findButton(buttonText), msWait);
    }

    public String getTitle()
    {
        return elements().title.getText();
    }

    public String getBody()
    {
        return elements().body.getText();
    }

    public void close()
    {
        elements().closeButton.click();
        waitForClose();
    }

    public void waitForClose()
    {
        waitForClose(1000);
    }

    public void waitForClose(int msWait)
    {
        _driver.waitFor(() -> {
            try
            {
                return !_window.isDisplayed();
            }
            catch (StaleElementReferenceException gone)
            {
                return true;
            }
        }, "Window did not close", msWait);
    }

    protected Elements elements()
    {
        return _elements;
    }

    protected class Elements extends ComponentElements
    {
        @Override
        protected SearchContext getContext()
        {
            return _window;
        }

        WebElement title = new LazyWebElement(Locator.css(".x4-window-header-text"), this);
        WebElement body = new LazyWebElement(Locator.css(".x4-window-body"), this);
        WebElement closeButton = new LazyWebElement(Locator.css(".x4-window-header .x4-tool-close"), this);
        WebElement findButton(String buttonText)
        {
            return Ext4Helper.Locators.ext4Button(buttonText).findElement(this);
        }
    }
}
