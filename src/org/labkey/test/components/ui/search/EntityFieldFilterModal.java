/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.components.ui.search;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.ui.grids.GridFilterModal;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

import static org.labkey.test.WebDriverWrapper.sleep;

/**
 * Wraps 'labkey-ui-component' defined in <code>internal/components/search/EntityFieldFilterModal.tsx</code>
 */
public class EntityFieldFilterModal extends GridFilterModal
{
    public EntityFieldFilterModal(WebDriver driver, UpdatingComponent linkedComponent)
    {
        super(driver, linkedComponent, "Select");
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
        queryItem.click();
        sleep(500); // wait for the fields to be displayed or updated.
        // The wait below does not consistently work. It works for the first rendering of the modal, but
        // if the modal is opened with a query already selected, selecting another query does not cause
        // staleness of the field panel elements, only an update of the contents.
//        getWrapper().doAndWaitForElementToRefresh(queryItem::click,
//                () -> elementCache().listItemLoc.findElement(elementCache().fieldsSelectionPanel), getWrapper().shortWait());

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
