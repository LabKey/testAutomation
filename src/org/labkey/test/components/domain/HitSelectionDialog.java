/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.ui.search.FilterExpressionPanel;
import org.labkey.test.util.selenium.WebElementUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;


public class HitSelectionDialog extends ModalDialog
{

    public HitSelectionDialog(WebDriver driver)
    {
        super(new ModalDialogFinder(driver));
    }

    public List<String> getAvailableFieldLabels()
    {
        return elementCache().findFieldOptions().stream().map(WebElementUtils::getTextContent).toList();
    }

    public FilterExpressionPanel selectField(String fieldName)
    {
        var fieldItem = elementCache().findFieldOption(fieldName);
        fieldItem.click();
        return elementCache().filterExpressionPanel();
    }

    public void cancel()
    {
        dismiss("Cancel");
    }

    public void clickApply()
    {
        if (!elementCache().submitButton.isEnabled())
        {
            throw new IllegalStateException("Apply button is not enabled.");
        }
        elementCache().submitButton.click();
        waitForClose();
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

        // fields panel
        WebElement fieldsPanel = Locator.tagWithClass("div", "field-modal__col")
                .withChild(Locator.tagWithClass("div", "field-modal__col-title").withText("Fields"))
                .child(Locator.tagWithClass("div", "field-modal__col-content")).findWhenNeeded(this).withTimeout(5000);
        protected WebElement findFieldOption(String queryName)
        {
            return listItemLoc.withText(queryName).findElement(fieldsPanel);
        }
        protected List<WebElement> findFieldOptions()
        {
            return listItemLoc.findElements(fieldsPanel);
        }

        // filter panel
        WebElement filterCriteriaPanel = Locator.tagWithClass("div", "field-modal__col")
                .withChild(Locator.tagWithClass("div", "field-modal__col-title").withText("Filter Criteria"))
                .child(Locator.tagWithClass("div", "field-modal__col-content")).findWhenNeeded(this).withTimeout(5000);
        protected final FilterExpressionPanel filterExpressionPanel()
        {
            return new FilterExpressionPanel.FilterExpressionPanelFinder(getDriver()).findWhenNeeded(filterCriteriaPanel);
        }

        protected final WebElement submitButton = Locator.css(".modal-footer .btn-success").findWhenNeeded(this);
    }
}
