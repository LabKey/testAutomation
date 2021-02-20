package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;


/* Some grids have a set of nav-tabs situated between the GridBar element and the table element.
* This class exposes a list of tabs, supports switching between grid views */
public class GridTabBar extends WebDriverComponent<GridTabBar.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;
    final ResponsiveGrid _grid;

    public GridTabBar(WebElement element, ResponsiveGrid grid,  WebDriver driver)
    {
        _el = element;
        _driver = driver;
        _grid = grid;
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

    public ResponsiveGrid getGrid()
    {
        return _grid;
    }

    public List<GridTab> getTabs()
    {
        return new GridTab.GridTabFinder(getDriver()).findAll(this);
    }

    public GridTab getActiveTab()
    {
        return getTabs().stream().filter(a-> a.isActive()).findFirst().orElse(null);
    }

    public GridTab waitForTab(String startsWith)
    {
        return new GridTab.GridTabFinder(getDriver()).startsWith(startsWith).waitFor(this);
    }

    public boolean hasTab(String partialTabText)
    {
        return getTabs().stream().anyMatch(a-> a.getText().startsWith(partialTabText));
    }

    /* tabs usually have a name plus a parenthetical number (denoting record count).
     * startsWith is the text before the open-parenthesis [(] */
    public GridTabBar selectTab(String startsWith)
    {
        getWrapper().waitFor(()-> getGrid().isLoaded() && hasTab(startsWith), WebDriverWrapper.WAIT_FOR_JAVASCRIPT);

        GridTab tab = waitForTab(startsWith);
        if (tab.isActive())
        {
            return this;
        }

        _grid.doAndWaitForUpdate(()-> tab.select());
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        //List<GridTab> tabs = new GridTab.GridTabFinder(getDriver()).findAll(this);    // don't cache for now
    }

    public static class GridTabBarFinder extends WebDriverComponentFinder<GridTabBar, GridTabBarFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("ul", "nav-tabs");
        private ResponsiveGrid _responsiveGrid;

        public GridTabBarFinder(WebDriver driver, ResponsiveGrid responsiveGrid)
        {
            super(driver);
            _responsiveGrid = responsiveGrid;
        }

        @Override
        protected GridTabBar construct(WebElement el, WebDriver driver)
        {
            return new GridTabBar(el, _responsiveGrid, driver);
        }

        @Override
        protected Locator locator()
        {
            return  _baseLocator;
        }
    }
}
