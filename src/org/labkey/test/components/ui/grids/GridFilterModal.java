package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.react.Tabs;
import org.labkey.test.components.ui.search.FilterExpressionPanel;
import org.labkey.test.components.ui.search.FilterFacetedPanel;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Wraps 'labkey-ui-component' defined in <code>public/QueryModel/GridFilterModal.tsx</code>
 */
public class GridFilterModal extends ModalDialog
{
    protected static final Locator listItem = Locator.byClass("list-group-item");

    private final UpdatingComponent _linkedComponent;

    public GridFilterModal(WebDriver driver, UpdatingComponent linkedComponent)
    {
        super(new ModalDialogFinder(driver));
        _linkedComponent = linkedComponent;
    }

    @Override
    protected void waitForReady(ModalDialog.ElementCache ec)
    {
        getWrapper().shortWait().until(ExpectedConditions.visibilityOf(listItem.findElementOrNull(elementCache().fieldsSelectionPanel)));
    }

    /**
     * Select field to configure filters for
     * @param fieldLabel Field's label
     * @return this component
     */
    public GridFilterModal selectField(String fieldLabel)
    {
        WebElement fieldItem = listItem.withText(fieldLabel).findElement(elementCache().fieldsSelectionPanel);
        fieldItem.click();
        Locator.byClass("filter-modal__col-sub-title").withText("Find values for " + fieldLabel)
                .waitForElement(elementCache().filterPanel, 10_000);

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
        // Fields column
        public final WebElement fieldsSelectionPanel = Locator.byClass("filter-modal__col_fields")
                .findWhenNeeded(this);

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
