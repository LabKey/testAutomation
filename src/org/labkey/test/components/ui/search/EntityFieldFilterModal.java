package org.labkey.test.components.ui.search;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.ui.grids.GridFilterModal;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Wraps 'labkey-ui-component' defined in <code>internal/components/search/EntityFieldFilterModal.tsx</code>
 */
public class EntityFieldFilterModal extends GridFilterModal
{
    public EntityFieldFilterModal(WebDriver driver, UpdatingComponent linkedComponent)
    {
        super(driver, linkedComponent);
    }

    @Override
    protected void waitForReady(ModalDialog.ElementCache ec)
    {
        getWrapper().shortWait().until(ExpectedConditions.visibilityOf(listItem.findElementOrNull(elementCache().querySelectionPanel)));
    }

    /**
     * Select parent
     * @param parentName name of parent type
     * @return this component
     */
    public EntityFieldFilterModal selectParent(String parentName)
    {
        WebElement queryItem = listItem.withText(parentName).findElement(elementCache().querySelectionPanel);
        getWrapper().doAndWaitForElementToRefresh(queryItem::click,
                () -> listItem.findElement(elementCache().fieldsSelectionPanel), getWrapper().shortWait());

        getWrapper().shortWait().until(ExpectedConditions.invisibilityOfElementLocated(BootstrapLocators.loadingSpinner));

        return this;
    }

    /**
     * Select parent and field to configure filters for
     * @param parentName name of parent type
     * @param fieldLabel Field's label
     * @return this component
     */
    public EntityFieldFilterModal selectParentField(String parentName, String fieldLabel)
    {
        selectParent(parentName);
        selectField(fieldLabel);

        return this;
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

    protected class ElementCache extends GridFilterModal.ElementCache
    {
        // Entities column
        protected final WebElement querySelectionPanel = Locator.byClass("filter-modal__col_queries")
                .findWhenNeeded(this);
    }

}
