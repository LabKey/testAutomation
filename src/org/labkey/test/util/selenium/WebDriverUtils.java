package org.labkey.test.util.selenium;

import org.labkey.test.Locators;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

public abstract class WebDriverUtils
{
    public static class ScrollUtil
    {
        private final WebDriver _webDriver;

        public ScrollUtil(WebDriver webDriver)
        {
            _webDriver = webDriver;
        }

        public boolean scrollUnderFloatingHeader(WebElement blockedElement)
        {
            return scrollUnderFloatingElement(blockedElement, Locators.floatingHeaderContainer().findElementOrNull(_webDriver));
        }

        public boolean scrollUnderFloatingElement(WebElement blockedElement, WebElement floatingElement)
        {
            if (floatingElement != null)
            {
                int headerHeight = floatingElement.getSize().getHeight();
                if (headerHeight > ((RemoteWebElement) blockedElement).getCoordinates().inViewPort().getY())
                {
                    int elHeight = blockedElement.getSize().getHeight();
                    scrollBy(0, -(headerHeight + elHeight));
                    return true;
                }
            }
            return false;
        }

        public WebElement scrollIntoView(WebElement el)
        {
            ((JavascriptExecutor)_webDriver).executeScript("arguments[0].scrollIntoView();", el);
            return el;
        }

        public WebElement scrollIntoView(WebElement el, Boolean alignToTop)
        {
            ((JavascriptExecutor)_webDriver).executeScript("arguments[0].scrollIntoView(arguments[1]);", el, alignToTop);
            return el;
        }

        public void scrollBy(Integer x, Integer y)
        {
            ((JavascriptExecutor)_webDriver).executeScript("window.scrollBy(" + x.toString() +", " + y.toString() + ");");
        }
    }
}
