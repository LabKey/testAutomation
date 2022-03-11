package org.labkey.test.components.ui.search;

import org.labkey.remoteapi.query.Filter.Operator;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.react.Tabs;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Wraps 'labkey-ui-component' defined in <code>internal/components/search/EntityFieldFilterModal.tsx</code>
 */
public class EntityFieldFilterModal extends ModalDialog
{
    private static final Locator listItem = Locator.byClass("list-group-item");

    private final UpdatingComponent _linkedComponent;

    protected EntityFieldFilterModal(WebDriver driver, UpdatingComponent linkedComponent)
    {
        super(new ModalDialog.ModalDialogFinder(driver));
        _linkedComponent = linkedComponent;
    }

    @Override
    protected void waitForReady(ModalDialog.ElementCache ec)
    {
        super.waitForReady(ec);
        getWrapper().shortWait().until(ExpectedConditions
                .visibilityOfNestedElementsLocatedBy(elementCache().querySelectionPanel, listItem));
    }

    public EntityFieldFilterModal selectParentQuery(String query)
    {
        WebElement queryItem = listItem.withText(query).findElement(elementCache().querySelectionPanel);
        getWrapper().doAndWaitForElementToRefresh(queryItem::click,
                () -> listItem.findElement(elementCache().fieldsSelectionPanel), getWrapper().shortWait());

        getWrapper().shortWait().until(ExpectedConditions.invisibilityOfElementLocated(BootstrapLocators.loadingSpinner));

        return this;
    }

    public EntityFieldFilterModal selectField(String fieldLabel)
    {
        WebElement fieldItem = listItem.withText(fieldLabel).findElement(elementCache().fieldsSelectionPanel);
        fieldItem.click();
        getWrapper().shortWait().until(wd -> elementCache().filterPanel.isDisplayed() &&
                Locator.byClass("parent-search-panel__col-sub-title").findElement(this)
                        .getText().equals("Find values for " + fieldLabel));

        return this;
    }

    /**
     * @see FilterExpressionPanel#setFilterValue(Operator)
     * @return this component
     */
    public EntityFieldFilterModal setFilterValue(Operator operator)
    {
        selectExpressionTab().setFilterValue(operator);
        return this;
    }

    /**
     * @see FilterExpressionPanel#setFilterValue(Operator, String)
     * @return this component
     */
    public EntityFieldFilterModal setFilterValue(Operator operator, String value)
    {
        selectExpressionTab().setFilterValue(operator, value);
        return this;
    }

    /**
     * @see FilterExpressionPanel#setFilterValue(Operator, String, String)
     * @return this component
     */
    public EntityFieldFilterModal setFilterValue(Operator operator, String value1, String value2)
    {
        selectExpressionTab().setFilterValue(operator, value1, value2);
        return this;
    }

    /**
     * Select the filter expression tab for the current field.
     * @return panel for configuring the filter expression
     */
    public FilterExpressionPanel selectExpressionTab()
    {
        elementCache().filterTabs.selectTab("Filter");
        return elementCache().filterExpressionPanel;
    }

    /**
     * Select the facet tab for the current field. Will throw <code>NoSuchElementException</code> if tab isn't present.
     * @return panel for configuring faceted filter
     */
    public FilterFacetedPanel selectFacetTab()
    {
        elementCache().filterTabs.selectTab("Choose values");
        return elementCache().filterFacetedPanel;
    }

    /**
     * Save current changes to the search criteria.
     * Throw <code>IllegalStateException</code> if the save button is disabled because no changes have been made.
     */
    public void clickFindSamples()
    {
        if (!elementCache().submitButton.isEnabled())
        {
            throw new IllegalStateException("Confirmation button is not enabled.");
        }

        _linkedComponent.doAndWaitForUpdate(() -> {
            elementCache().submitButton.click();
            waitForClose();
        });
    }

    public void cancel()
    {
        dismiss("Cancel");
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        // Entities column
        protected final WebElement querySelectionPanel = Locator.byClass("parent-search-panel__col_queries")
                .findWhenNeeded(this);

        // Fields column
        protected final WebElement fieldsSelectionPanel = Locator.byClass("parent-search-panel__col_fields")
                .findWhenNeeded(this);

        // Values column
        protected final WebElement filterPanel = Locator.byClass("parent-search-panel__col_filter_exp")
                .findWhenNeeded(this);
        protected final Tabs filterTabs = new Tabs.TabsFinder(getDriver()).refindWhenNeeded(filterPanel);
        protected final FilterExpressionPanel filterExpressionPanel =
                new FilterExpressionPanel.FilterExpressionPanelFinder(getDriver()).refindWhenNeeded(filterPanel);
        protected final FilterFacetedPanel filterFacetedPanel =
                new FilterFacetedPanel.FilterFacetedPanelFinder(getDriver()).refindWhenNeeded(filterPanel);

        protected final WebElement submitButton = Locator.css(".modal-footer .btn-success").findWhenNeeded(this);
    }

}
