package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.MultiMenu;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

/**
 * TabbedGridPanel wraps components/src/public/QueryModel/TabbedGridPanel.tsx
 * The model of a tabbed grid panel is different from previous models; rather than conceiving the tabs to
 * be a subcomponent of a constant QueryGrid, the TabbedGridPanel uses reactBootstrap to swap one or many
 * grids into or out of existence, while showing only the selected one.
 */
public class TabbedGridPanel extends WebDriverComponent<TabbedGridPanel.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected TabbedGridPanel(WebElement element, WebDriver driver)
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

    public List<String> getTabs()
    {
        return getWrapper().getTexts(elementCache().navTabs());
    }

    public List<String> getTabsWithoutCounts()
    {
        return getTabs().stream()
                .map(tab -> tab.replaceFirst(" \\([0-9]+\\)$", ""))
                .collect(Collectors.toList());
    }

    private boolean isSelected(String tabText)
    {
        String tabClass = elementCache().navTab(tabText).getAttribute("class");
        return tabClass.toLowerCase().contains("active");
    }

    public QueryGrid selectGrid(String tabText)
    {
        QueryGrid grid = getSelectedGrid();

        if (!isSelected(tabText))
        {
            var tab = elementCache().navTab(tabText);
            getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(tab));
            tab.click();
            WebDriverWrapper.waitFor(()-> isSelected(tabText), "tab did not become selected in time", 2000);
            getWrapper().shortWait().until(ExpectedConditions.stalenessOf(grid.getComponentElement()));
            grid = getSelectedGrid();
        }

        WebDriverWrapper.waitFor(grid::isLoaded,
                String.format("The grid under tab '%s' did not become active in time.", tabText),
                2_500);

        return grid;
    }

    public QueryGrid getSelectedGrid()
    {
        return new QueryGrid.QueryGridFinder(getDriver()).waitFor(elementCache().body);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    public Optional<MultiMenu> optionalMoreMenu()
    {
        return elementCache().gridMoreMenu();
    }

    public MultiMenu clickAssayMenu()
    {
        MultiMenu menu;
        if (optionalMoreMenu().isPresent())
        {
            menu = optionalMoreMenu().get();
            optionalMoreMenu().get().openMenuTo("Import Assay Data");
        }
        else
            menu = elementCache().gridAssayMenu;

        return menu;
    }
    public void clickAssay(String assayName)
    {
        MultiMenu menu = clickAssayMenu();
        menu.clickSubMenu(getWrapper().defaultWaitForPage, assayName);
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        MultiMenu.MultiMenuFinder gridMoreMenuFinder = new MultiMenu.MultiMenuFinder(getDriver()).withText("More");
        Optional<MultiMenu> gridMoreMenu()
        {
            return gridMoreMenuFinder.findOptional(this);
        }

        MultiMenu gridAssayMenu = new MultiMenu.MultiMenuFinder(getDriver()).withText("Assay").findWhenNeeded(this);

        final WebElement body = Locator.tagWithClass("div", "tabbed-grid-panel__body")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        final Locator navTab = Locator.tagWithClass("ul", "nav-tabs")
                .child(Locator.tag("li").withChild(Locator.tag("a")));

        List<WebElement> navTabs()
        {
            return navTab.findElements(body);
        }

        WebElement navTab(String tabTextStartsWith)
        {
            return Locator.tagWithClass("ul", "nav-tabs")
                    .child(Locator.tag("li").withChild(Locator.tag("a").startsWith(tabTextStartsWith))).findElement(body);
        }
    }

    public static class TabbedGridPanelFinder extends WebDriverComponentFinder<TabbedGridPanel, TabbedGridPanelFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "tabbed-grid-panel");
        private String _title = null;

        public TabbedGridPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        public TabbedGridPanelFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected TabbedGridPanel construct(WebElement el, WebDriver driver)
        {
            return new TabbedGridPanel(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withChild(Locator.tagWithClass("div", "tabbed-grid-panel__title").withText(_title));
            return _baseLocator;
        }
    }
}
