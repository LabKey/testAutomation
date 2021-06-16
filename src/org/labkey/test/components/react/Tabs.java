package org.labkey.test.components.react;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.HashMap;
import java.util.Map;

/**
 * Wraps 'Tab' and 'Tabs' components from 'react-bootstrap'
 */
public class Tabs extends WebDriverComponent<Tabs.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected Tabs(WebElement element, WebDriver driver)
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

    public WebElement findPanelForTab(String tabText)
    {
        return elementCache().findTabPanel(tabText);
    }

    public WebElement selectTab(String tabText)
    {
        WebElement panel = findPanelForTab(tabText);
        if (!panel.isDisplayed())
        {
            elementCache().findTab(tabText).click();
            getWrapper().shortWait().until(ExpectedConditions.visibilityOf(panel));
        }
        return panel;
    }

    public boolean isTabSelected(String tabText)
    {
        return Boolean.valueOf(elementCache().findTab(tabText).getAttribute("aria-selected"));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement tabList = Locator.xpath("./ul").withClass("nav-tabs").findWhenNeeded(this);
        final Map<String, WebElement> tabs = new HashMap<>();
        WebElement findTab(String tabText)
        {
            if (!tabs.containsKey(tabText))
            {
                WebElement tabEl = Locator.tag("a").withAttribute("role", "tab").withText(tabText).findElement(tabList);
                tabs.put(tabText, tabEl);
            }
            return tabs.get(tabText);
        }

        final WebElement tabContent = Locator.xpath("./div").withClass("tab-content").findWhenNeeded(this);
        final Map<String, WebElement> tabPanels = new HashMap<>();
        WebElement findTabPanel(String tabText)
        {
            if (!tabPanels.containsKey(tabText))
            {
                String panelId = findTab(tabText).getAttribute("aria-controls");
                tabPanels.put(tabText, Locator.id(panelId).findElement(tabContent));
            }
            return tabPanels.get(tabText);
        }
    }

    public static class TabsFinder extends WebDriverComponentFinder<Tabs, TabsFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("ul", "tablist").parent();

        public TabsFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected Tabs construct(WebElement el, WebDriver driver)
        {
            return new Tabs(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
