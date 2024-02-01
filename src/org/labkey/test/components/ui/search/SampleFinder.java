package org.labkey.test.components.ui.search;

import org.apache.commons.lang3.StringUtils;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.react.MultiMenu;
import org.labkey.test.components.ui.grids.TabbedGridPanel;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wraps 'labkey-ui-component' defined in <code>internal/components/search/SampleFinderSection.tsx</code>
 */
public class SampleFinder extends WebDriverComponent<SampleFinder.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected SampleFinder(WebElement el, WebDriver driver)
    {
        _el = el;
        _driver = driver;
    }

    public SampleFinder(WebDriver driver)
    {
        this(Locator.byClass("g-section")
                .withDescendant(Locator.byClass("filter-cards"))
                .waitForElement(driver, 5_000), driver);
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

    /**
     * Waits for initial state (empty filter card panel) or for search results grid to appear
     */
    @Override
    public void waitForReady()
    {
        loadingWait().withMessage("Sample finder loading").until(wd -> !BootstrapLocators.loadingSpinner.existsIn(this) &&
                (isEmptySearch() || elementCache().resultsGrid.getComponentElement().isDisplayed()));
    }

    /**
     * Open the entity filter dialog for the specified filter type.
     *
     * @param filterKind "Source" or "Parent" or "Assay" in SM. "Registry Parent" or "Sample Parent" or "Assay" in Biologics
     * @return component wrapper for the EntityFieldFilterModal
     */
    public EntityFieldFilterModal clickAddParent(String filterKind)
    {
        elementCache().findFilterKindButton(filterKind).click();
        return new EntityFieldFilterModal(getDriver(), this::doAndWaitForUpdate);
    }

    /**
     * returns the list of search/filter buttons contained in the header section
     * @return
     */
    public List<WebElement> getFilterButtons()
    {
        return elementCache().getFilterKindButtons();
    }

    /**
     * Get the tabbed query grid panel containing the current search results.
     * Throws {@link NoSuchElementException} if there are no search criteria currently defined
     */
    public TabbedGridPanel getResultsGrid()
    {
        if (isEmptySearch())
        {
            throw new NoSuchElementException("No search criteria are currently set");
        }
        return elementCache().resultsGrid;
    }

    /**
     * remove the search card for the specified entity
     * @param queryName name of the entity (Sample Type, Source Type, etc.) to be removed
     */
    public void removeSearchCard(String queryName)
    {
        elementCache().findFilterCard(queryName).clickRemove();
    }

    /**
     * Reset sample finder to its initial state, with no search criteria
     */
    public void removeAllSearchCards()
    {
        List<WebElement> removeButtons = Locator.tagWithAttribute("i", "title", "Remove filter")
                .findElements(elementCache().filterCardsSection);
        Collections.reverse(removeButtons);
        // Don't wait for search results to update after each removed card
        for (WebElement button : removeButtons)
        {
            button.click();
            getWrapper().shortWait().until(ExpectedConditions.stalenessOf(button));
        }
        getWrapper().shortWait().withMessage("Clearing all search cards").until(wd -> isEmptySearch());

        clearElementCache();
    }

    /**
     * Get components wrapping the filter cards currently present
     * @return All current filter cards
     */
    public List<FilterCard> getSearchCards()
    {
        return elementCache().findFilterCards();
    }

    public SavedSearchesMenu getSaveSearchMenu()
    {
        return new SavedSearchesMenu(elementCache().savedSearchesContainer, getDriver(), elementCache().savedSearchesButton);
    }

    public MultiMenu getSaveSearchDropdownBtn()
    {
        return elementCache().saveDropdown;
    }

    /**
     * Clicks the 'Save Search' button next to the saved searches menu
     * This button will only be present if a search has been done
     * @return
     */
    public SaveSampleFinderViewModal clickSaveSearchBtn()
    {
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().saveSearchButton));
        elementCache().saveSearchButton.click();
        return new SaveSampleFinderViewModal(getDriver());
    }

    /**
     * Waiter that will wait for the search results to load. This is a hook for perf tests to support longer waits.
     * @return WebDriverWait to be used by {@link #waitForReady()}
     */
    protected WebDriverWait loadingWait()
    {
        return getWrapper().shortWait();
    }

    public boolean isEmptySearch()
    {
        return Locator.css(".filter-cards.empty").isDisplayed(this);
    }

    private void doAndWaitForUpdate(Runnable func)
    {
        WebElement resultsGridEl = elementCache().resultsGrid.getComponentElement();
        if (resultsGridEl.isDisplayed())
        {
            func.run();
            getWrapper().shortWait().until(ExpectedConditions.stalenessOf(resultsGridEl));
        }
        else
        {
            func.run();
        }

        clearElementCache();
        elementCache(); // waitForReady
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement panelHeading = Locator.byClass("panel-heading").findWhenNeeded(this);

        WebElement findFilterKindButton(String filterNoun)
        {
            return BootstrapLocators.button(filterNoun + " Properties")
                    .withChild(Locator.byClass("container--addition-icon")).findElement(panelHeading);
        }


        WebElement saveSearchButton = BootstrapLocators.button("Save Search").findWhenNeeded(panelHeading);

        List<WebElement> getFilterKindButtons()
        {
            return Locator.tagWithClass("button", "btn-default")
                    .withChild(Locator.tagWithClass("i", "container--addition-icon"))
                    .findElements(panelHeading);
        }

        final WebElement filterCardsSection = Locator.byClass("filter-cards").findWhenNeeded(this);
        List<FilterCard> findFilterCards()
        {
            if (filterCardsSection.getAttribute("class").contains("empty"))
            {
                return Collections.emptyList();
            }
            return Locator.byClass("filter-cards__card").findElements(filterCardsSection)
                    .stream().map(FilterCard::new).collect(Collectors.toList());
        }

        FilterCard findFilterCard(String queryName)
        {
            return new FilterCard(Locator.byClass("filter-cards__card").withDescendant(
                    Locator.byClass("primary-text").withText(queryName)).findElement(filterCardsSection));
        }

        final TabbedGridPanel resultsGrid = new TabbedGridPanel.TabbedGridPanelFinder(getDriver()).findWhenNeeded(this);

        final MultiMenu saveDropdown = new MultiMenu.MultiMenuFinder(getDriver()).withClass("split-button-dropdown").findWhenNeeded(this);

        final Locator.XPathLocator savedSearchesButtonLoc = Locator.byClass("search-selector");

        final WebElement savedSearchesButton = savedSearchesButtonLoc.findWhenNeeded(this);

        final WebElement savedSearchesContainer = Locator.byClass("dropdown").withChild(savedSearchesButtonLoc).refindWhenNeeded(this);
    }

    public class SavedSearchesMenu  extends BootstrapMenu
    {
        final static String SAVE_MENU_OPTION = "Save as custom search";
        final static String MANGE_MENU_OPTION = "Manage saved searches";

        private final WebElement _dropdownEl;
        SavedSearchesMenu(WebElement componentElement, WebDriver driver, WebElement dropdownEl)
        {
            super(driver, componentElement);
            _dropdownEl = dropdownEl;
        }

        public String getTitle()
        {
            return _dropdownEl.getText();
        }

        public SaveSampleFinderViewModal clickSaveAs()
        {
            clickSubMenu(false, SAVE_MENU_OPTION);
            return new SaveSampleFinderViewModal(getDriver());
        }

        public ManageSampleFinderViewsModal clickManage()
        {
            clickSubMenu(false, MANGE_MENU_OPTION);
            return new ManageSampleFinderViewsModal(getDriver());
        }

        public boolean isDisabled()
        {
            return !_dropdownEl.isEnabled();
        }

        public boolean isSaveEnabled()
        {
            WebElement disabledEl = findDisabledMenuItemOrNull(SAVE_MENU_OPTION);
            if (disabledEl == null)
                return findVisibleMenuItemOrNull(SAVE_MENU_OPTION) != null;
            return false;
        }

        public boolean isManageEnabled()
        {
            WebElement disabledEl = findDisabledMenuItemOrNull(MANGE_MENU_OPTION);
            if (disabledEl == null)
                return findVisibleMenuItemOrNull(MANGE_MENU_OPTION) != null;
            return false;
        }

        public String getSelectedView()
        {
            List<WebElement> views = getViewsWithCls("active");
            if (views.size() > 0)
                return views.get(0).getText();

            return null;
        }

        public String getLastSearchedView()
        {
            List<WebElement> views = getViewsWithCls("session-finder-view");
            if (views.size() > 0)
                return views.get(0).getText();

            return null;
        }

        public List<String> getSavedViews()
        {
            List<String> viewNames = new ArrayList<>();
            getViewsWithCls("saved-finder-view").forEach(el -> viewNames.add(el.getText()));
            collapse();
            return viewNames;
        }

        public List<String> getBuiltInViews()
        {
            List<String> viewNames = new ArrayList<>();
            getViewsWithCls("built-in-finder-view").forEach(el -> viewNames.add(el.getText()));
            collapse();
            return viewNames;
        }

        public List<WebElement> getViewsWithCls(String cls)
        {
            expand();
            List<WebElement> elements = findVisibleMenuItemsWithClass(cls);
            return elements;
        }

        public void clickView(String viewName)
        {
            clickSubMenu(false, viewName);
            clearElementCache();
            elementCache(); // waitForReady
        }

        public void clickLastSearchedView()
        {
            clickView(getLastSearchedView());
        }
    }


    /**
     * Represents a single filter card. Don't expose outside this class because its lifecycle is confusing
     */
    public class FilterCard extends Component<Component<?>.ElementCache>
    {
        private final WebElement cardEl;
        private final WebElement name = Locator.byClass("primary-text").findWhenNeeded(this);
        private final WebElement editButton =
                Locator.tagWithAttribute("i", "title", "Edit filter").findWhenNeeded(this);
        private final WebElement removeButton =
                Locator.tagWithAttribute("i", "title", "Remove filter").findWhenNeeded(this);

        private FilterCard(WebElement el)
        {
            this.cardEl = el;
        }

        @Override
        public WebElement getComponentElement()
        {
            return cardEl;
        }

        EntityFieldFilterModal clickEdit()
        {
            editButton.click();
            return new EntityFieldFilterModal(getDriver(), SampleFinder.this::doAndWaitForUpdate);
        }

        void clickRemove()
        {
            doAndWaitForUpdate(() -> {
                int initialCount = SampleFinder.this.elementCache().findFilterCards().size();
                removeButton.click();
                // This card's element isn't removed from the DOM if it isn't the farthest right card
                getWrapper().shortWait().until(wd ->
                        SampleFinder.this.elementCache().findFilterCards().size() == initialCount - 1);
            });
        }

        public String getParentName()
        {
            return name.getText();
        }

        public FilterCardValues getFilterValues()
        {
            Map<String, WebElement> filters = new HashMap<>();
            List<WebElement> rows = Locator.byClass("filter-display__row").findElements(this);
            for (WebElement row : rows)
            {
                WebElement labelEl = Locator.byClass("filter-display__field-label").findElement(row);
                WebElement valueEl = Locator.byClass("filter-display__filter-value").findElement(row);

                String label = StringUtils.strip(labelEl.getText(), ":");

                filters.put(label, valueEl);
            }
            return new FilterCardValues(filters);
        }
    }

    public static class FilterCardValues
    {
        private final Map<String, WebElement> _filters = new CaseInsensitiveHashMap<>();
        private final Map<String, WebElement> _negatedFilters = new CaseInsensitiveHashMap<>();

        public FilterCardValues(Map<String, WebElement> allFilters)
        {
            for (Map.Entry<String, WebElement> entry : allFilters.entrySet())
            {
                if (entry.getValue().getAttribute("class").contains("negate"))
                {
                    _negatedFilters.put(entry.getKey(), entry.getValue());
                }
                else
                {
                    _filters.put(entry.getKey(), entry.getValue());
                }
            }
        }

        public Map<String, WebElement> getFilters()
        {
            return _filters;
        }

        public Map<String, WebElement> getNegatedFilters()
        {
            return _negatedFilters;
        }
    }
}
