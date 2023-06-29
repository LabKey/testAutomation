package org.labkey.test.components.ui.search;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.ui.grids.GridFilterModal;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

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
    protected void waitForReady()
    {
        getWrapper().shortWait().until(ExpectedConditions.and(
                ExpectedConditions.visibilityOf(elementCache().querySelectionPanel),
                ExpectedConditions.invisibilityOfAllElements(BootstrapLocators.loadingSpinner.findElements(this))));
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

    }

}
