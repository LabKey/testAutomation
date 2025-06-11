package org.labkey.test.components.react;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controls 'Tabs' and 'Tab' components from 'react-bootstrap' or 'packages/components/src/internal/Tabs.tsx'
 *
 * Corresponding application code looks something like:
 * <pre>{@code
 * <Tabs className="panel-tabs" >
 *     <Tab title="First Tab">
 *         <PanelComponent1/>
 *     </Tab>
 *
 *     <Tab title="Second Tab">
 *         <PanelComponent2/>
 *     </Tab>
 * </Tabs>
 * }</pre>
 */
public class Tabs extends WebDriverComponent<Tabs.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    public Tabs(WebElement element, WebDriver driver)
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

    public WebElement findTab(String tabText)
    {
        return elementCache().findTab(tabText);
    }

    public WebElement findPanelForTab(String tabText)
    {
        return elementCache().findTabPanel(elementCache().findTab(tabText));
    }

    public WebElement selectTab(String tabText)
    {
        WebElement tab = elementCache().findTab(tabText);
        getWrapper().scrollIntoView(tab);
        tab.click();
        WebElement panel = elementCache().findTabPanel(tab);
        getWrapper().shortWait().until(ExpectedConditions.visibilityOf(panel));
        return panel;
    }

    public WebElement findPanelForActiveTab()
    {
        return elementCache().findTabPanel(elementCache().findSelectedTab());
    }

    public boolean isTabSelected(String tabText)
    {
        return Boolean.valueOf(elementCache().findTab(tabText).getDomAttribute("aria-selected"));
    }

    public List<String> getTabText()
    {
        List<WebElement> tabs = elementCache().findAllTabs();
        return tabs
                .stream().map(WebElement::getText).toList();
    }

    public String getSelectedTabText()
    {
        return elementCache().findSelectedTab().getText();
    }

    public String getSelectedTabKey()
    {
        return elementCache().findSelectedTab().getDomAttribute("data-event-key");
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement tabList = Locator.xpath("(ul|div/ul)").withClass("nav-tabs").findWhenNeeded(this);
        final Map<String, WebElement> tabMap = new HashMap<>();
        final List<WebElement> tabs = new ArrayList<>();
        private final Locator.XPathLocator tabLoc = Locator.tag("a").withAttribute("role", "tab");
        final WebElement tabContent = Locator.xpath("(div|div/div)").withClass("tab-content").findWhenNeeded(this);

        public ElementCache()
        {
            if (!WebDriverWrapper.waitFor(() -> !findAllTabs().isEmpty(), 10_000))
            {
                tabLoc.findElement(this); // Should trigger a 'NoSuchElementException'
            }
        }

        protected WebElement findSelectedTab()
        {
            return tabLoc.withAttribute("aria-selected", "true").findElement(this);
        }

        List<WebElement> findAllTabs()
        {
            if (tabs.isEmpty())
            {
                tabs.addAll(tabLoc.findElements(tabList));
            }
            return tabs;
        }

        WebElement findTab(String tabText)
        {
            if (!tabMap.containsKey(tabText))
            {
                WebElement tabEl;
                try
                {
                    // Use 'containing' here because it may happen that the counts get loaded into the tabs after the call to this method,
                    // which causes the name to change from, say 'Included Samples' to 'Included Samples (7)'.
                    tabEl = tabLoc.containing(tabText).findElement(tabList);
                }
                catch (NoSuchElementException ex)
                {
                    throw new NoSuchElementException(String.format("'%s' not among available tabs: %s",
                        tabText, getWrapper().getTexts(findAllTabs())), ex);
                }
                tabMap.put(tabText, tabEl);
            }
            return tabMap.get(tabText);
        }

        // Tab panels can be updated and changed when flipping between tabs. Don't persist the panel element find it each time.
        WebElement findTabPanel(WebElement tabElement)
        {
            String panelId = tabElement.getDomAttribute("aria-controls");
            WebElement panelEl;
            try
            {
                panelEl = Locator.id(panelId).findElement(tabContent);
            }
            catch (NoSuchElementException ex)
            {
                throw new NoSuchElementException("Panel not found for tab : " + tabElement.getText(), ex);
            }

            return panelEl;
        }
    }

    public static class TabsFinder extends WebDriverComponentFinder<Tabs, TabsFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("ul", "nav-tabs").parent();

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
