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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Wraps 'labkey-ui-component' defined in <code>public/QueryModel/GridFilterModal.tsx</code>
 */
public class GridFilterModal extends ModalDialog
{
    private final UpdatingComponent _linkedComponent;

    public GridFilterModal(WebDriver driver, UpdatingComponent linkedComponent)
    {
        super(new ModalDialogFinder(driver));
        _linkedComponent = linkedComponent;
    }

    @Override
    protected void waitForReady()
    {
        getWrapper().shortWait().until(ExpectedConditions.visibilityOf(elementCache().fieldsSelectionPanel));
    }

    /**
     * Select field to configure filters for
     * @param fieldLabel Field's label
     * @return this component
     */
    public GridFilterModal selectField(String fieldLabel)
    {
        WebElement fieldItem = elementCache().findFieldOption(fieldLabel);
        fieldItem.click();
        Locator.byClass("field-modal__col-sub-title").withText("Find values for " + fieldLabel)
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

    public List<String> getFilteredFields()
    {
        List<WebElement> filteredElements = Locator.byClass("list-group-item").withChild(
                Locator.tagWithClass("span", "field-modal__field_dot"))
                .findElements(elementCache().fieldsSelectionPanel);

        return filteredElements.stream().map(WebElement::getText).collect(Collectors.toList());
    }

    /**
     * Select the filter expression tab for the current field.
     * @return panel for configuring the filter expression
     */
    public FilterExpressionPanel selectExpressionTab()
    {
        elementCache().filterTabs.selectTab("Filter");
        return elementCache().filterExpressionPanel();
    }

    /**
     * Select the facet tab for the current field. Will throw <code>NoSuchElementException</code> if tab isn't present.
     * @return panel for configuring faceted filter
     */
    public FilterFacetedPanel selectFacetTab()
    {
        elementCache().filterTabs.selectTab("Choose values");
        return elementCache().filterFacetedPanel();
    }

    /**
     * Get the text of the tabs that are shown.
     *
     * @return List of the tabs.
     */
    public List<String> getTabText()
    {
        return elementCache().filterTabs.getTabText();
    }

    public String getActiveTab()
    {
        String activeTab = "";

        for(String tabText : getTabText())
        {
            if(elementCache().filterTabs.isTabSelected(tabText))
            {
                activeTab = tabText;
                break;
            }
        }

        return activeTab;
    }

    /**
     * Save current changes to the search criteria.
     * Throw <code>IllegalStateException</code> if the save button is disabled because no changes have been made.
     */
    public void confirm()
    {
        _linkedComponent.doAndWaitForUpdate(() -> {
            elementCache().submitButton.click();
            waitForClose();
        });
    }

    public String confirmExpectingError()
    {
        clickConfirm();
        return getWrapper().shortWait().until(ExpectedConditions.visibilityOf(elementCache().errorAlert)).getText();
    }

    protected void clickConfirm()
    {
        if (!elementCache().submitButton.isEnabled())
        {
            throw new IllegalStateException("Confirmation button is not enabled.");
        }
        elementCache().submitButton.click();
    }

    public void cancel()
    {
        dismiss("Cancel");
    }

    public String getErrorMsg() {
        return elementCache().errorAlert.getText();
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
        public final Locator listItemLoc = Locator.byClass("list-group-item");

        // Fields column
        public final WebElement fieldsSelectionPanel = Locator.byClass("filter-modal__col_fields")
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
        protected final FilterExpressionPanel filterExpressionPanel()
        {
            return new FilterExpressionPanel.FilterExpressionPanelFinder(getDriver()).findWhenNeeded(filterPanel);
        }
        protected final FilterFacetedPanel filterFacetedPanel()
        {
            return new FilterFacetedPanel.FilterFacetedPanelFinder(getDriver()).findWhenNeeded(filterPanel);
        }
        protected final WebElement submitButton = Locator.css(".modal-footer .btn-success").findWhenNeeded(this);
        protected final WebElement errorAlert = Locator.css(".modal-body .alert-danger").findWhenNeeded(this);
    }

}
