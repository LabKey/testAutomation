package org.labkey.test.components.ui.search;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.ui.grids.TabbedGridPanel;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Wraps 'labkey-ui-component' defined in <code>internal/components/search/SampleFinderSection.tsx</code>
 */
public class SampleFinder extends WebDriverComponent<SampleFinder.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected SampleFinder(WebDriver driver)
    {
        _el = Locator.byClass("g-section")
                .withDescendant(Locator.byClass("panel-content-title-large").withText("Find Samples"))
                .waitForElement(getDriver(), 5_000);
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

    /**
     * Open the entity filter dialog for the specified parent type.
     *
     * @param parentNoun "Source" or "Parent" in SM. "Registry Parent" or "SampleParent" in Biologics
     * @return component wrapper for the EntityFieldFilterModal
     */
    public EntityFieldFilterModal clickAddParent(String parentNoun)
    {
        elementCache().findAddParentButton(parentNoun).click();
        return new EntityFieldFilterModal(getDriver(), this::doAndWaitForUpdate);
    }

    public TabbedGridPanel getResultsGrid()
    {
        if (!elementCache().resultsGrid.getComponentElement().isDisplayed())
        {
            throw new NoSuchElementException("No search results currently shown");
        }
        return elementCache().resultsGrid;
    }

    /**
     * Waiter that will wait for the search results to load. This is a hook for perf tests to support longer waits.
     * @return WebDriverWait to be used by {@link #waitForReady()}
     */
    protected WebDriverWait loadWait()
    {
        return getWrapper().shortWait();
    }

    /**
     * Waits for initial state (empty filter card panel) or for search results grid to appear
     */
    @Override
    protected void waitForReady()
    {
        loadWait().withMessage("Sample finder loading").until(wd -> !BootstrapLocators.loadingSpinner.existsIn(this) &&
        (Locator.css(".filter-cards.empty").isDisplayed(this)
                || elementCache().resultsGrid.getComponentElement().isDisplayed()));
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

        WebElement findAddParentButton(String parentNoun)
        {
            return BootstrapLocators.button(" " + parentNoun + " Properties")
                    .withChild(Locator.byClass("container--addition-icon")).findElement(panelHeading);
        }

        final WebElement filterCardsSection = Locator.byClass("filter-cards").findWhenNeeded(this);
        List<FilterCard> findFilterCards()
        {
            return Locator.byClass("filter-cards__card").findElements(filterCardsSection)
                    .stream().map(FilterCard::new).collect(Collectors.toList());
        }

        FilterCard findFilterCard(String queryName)
        {
            return new FilterCard(Locator.byClass("filter-cards__card").withDescendant(
                    Locator.byClass("primary-text").withText(queryName)).findElement(filterCardsSection));
        }

        final TabbedGridPanel resultsGrid = new TabbedGridPanel.TabbedGridPanelFinder(getDriver()).findWhenNeeded(this);
    }

    /**
     * Represents a single filter card. Don't expose outside this class because its lifecycle is confusing
     */
    private class FilterCard extends Component<Component<?>.ElementCache>
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

        String getQueryName()
        {
            return name.getText();
        }
    }
}
