package org.labkey.test.util.selenium;

import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.Locatable;

import java.util.List;

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
            List<WebElement> floatingHeaders = Locator.findElements(_webDriver, Locators.floatingHeaderContainer(), DataRegionTable.Locators.floatingHeader().notHidden());

            int headerHeight = 0;
            for (WebElement floatingHeader : floatingHeaders)
            {
                headerHeight += floatingHeader.getSize().getHeight();
            }
            if (headerHeight > 0 && headerHeight > ((Locatable) blockedElement).getCoordinates().inViewPort().getY())
            {
                int elHeight = blockedElement.getSize().getHeight();
                scrollBy(0, -(headerHeight + elHeight));
                return true;
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
