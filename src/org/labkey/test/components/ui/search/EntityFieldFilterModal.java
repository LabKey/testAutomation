package org.labkey.test.components.ui.search;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.react.Tabs;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/**
 * Wraps 'labkey-ui-component' defined in <code>internal/components/search/EntityFieldFilterModal.tsx</code>
 */
public class EntityFieldFilterModal extends ModalDialog
{
    private final UpdatingComponent _linkedComponent;

    protected EntityFieldFilterModal(WebDriver driver, UpdatingComponent linkedComponent)
    {
        super(new ModalDialog.ModalDialogFinder(driver));
        _linkedComponent = linkedComponent;
    }

    @Override
    protected void waitForReady()
    {
        super.waitForReady();
        getWrapper().shortWait().until(ExpectedConditions
                .visibilityOf(elementCache().querySelectionPanel));
    }

    /**
     * Select parent/source query
     * @param queryName name of parent/source type
     * @return this component
     */
    public EntityFieldFilterModal selectQuery(String queryName)
    {
        WebElement queryItem = elementCache().findQueryOption(queryName);
        getWrapper().doAndWaitForElementToRefresh(queryItem::click,
                () -> elementCache().listItemLoc.findElement(elementCache().fieldsSelectionPanel), getWrapper().shortWait());

        getWrapper().shortWait().until(ExpectedConditions.invisibilityOfElementLocated(BootstrapLocators.loadingSpinner));

        return this;
    }

    /**
     * Get visible source/parent queries
     * @return query names in dialog
     */
    public List<String> getAvailableQueries()
    {
        return getWrapper().getTexts(elementCache().findQueryOptions());
    }

    /**
     * Select field to configure filters for
     * @param fieldLabel Field's label
     * @return this component
     */
    public EntityFieldFilterModal selectField(String fieldLabel)
    {
        WebElement fieldItem = elementCache().findFieldOption(fieldLabel);
        fieldItem.click();
        Locator.byClass("filter-modal__col-sub-title").withText("Find values for " + fieldLabel)
                .waitForElement(elementCache().filterPanel, 10_000);

        return this;
    }

    /**
     * Get fields for currently selected query
     * @return visible field labels
     */
    public List<String> getAvailableFields()
    {
        return getWrapper().getTexts(elementCache().findFieldOptions());
    }

    /**
     * Select parent/source and field to configure filters for
     * @param queryName name of parent/source type
     * @param fieldLabel Field's label
     * @return this component
     */
    public EntityFieldFilterModal selectQueryField(String queryName, String fieldLabel)
    {
        selectQuery(queryName);
        selectField(fieldLabel);

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
    public void confirm()
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
        protected final Locator listItemLoc = Locator.byClass("list-group-item");

        // Queries column
        protected final WebElement querySelectionPanel = Locator.byClass("filter-modal__col_queries")
                .findWhenNeeded(this);
        protected WebElement findQueryOption(String queryName)
        {
            return listItemLoc.withText(queryName).findElement(elementCache().querySelectionPanel);
        }
        protected List<WebElement> findQueryOptions()
        {
            return listItemLoc.findElements(elementCache().querySelectionPanel);
        }

        // Fields column
        protected final WebElement fieldsSelectionPanel = Locator.byClass("filter-modal__col_fields")
                .findWhenNeeded(this);
        protected WebElement findFieldOption(String queryName)
        {
            return listItemLoc.withText(queryName).findElement(elementCache().fieldsSelectionPanel);
        }
        protected List<WebElement> findFieldOptions()
        {
            return listItemLoc.findElements(elementCache().fieldsSelectionPanel);
        }

        // Values column
        protected final WebElement filterPanel = Locator.byClass("filter-modal__col_filter_exp")
                .findWhenNeeded(this);
        protected final Tabs filterTabs = new Tabs.TabsFinder(getDriver()).refindWhenNeeded(filterPanel);
        protected final FilterExpressionPanel filterExpressionPanel =
                new FilterExpressionPanel.FilterExpressionPanelFinder(getDriver()).refindWhenNeeded(filterPanel);
        protected final FilterFacetedPanel filterFacetedPanel =
                new FilterFacetedPanel.FilterFacetedPanelFinder(getDriver()).refindWhenNeeded(filterPanel);

        protected final WebElement submitButton = Locator.css(".modal-footer .btn-success").findWhenNeeded(this);
    }

}
