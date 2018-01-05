/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.components.studydesigner;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebElement;

import static org.labkey.test.util.Ext4Helper.Locators.ext4Button;

public class BaseManageVaccineDesignPage extends LabKeyPage
{
    public BaseManageVaccineDesignPage(BaseWebDriverTest test)
    {
        super(test);
        waitForElement(Locators.pageSignal("VaccineDesign_renderviewcomplete"));
    }

    protected void clickOuterAddNewRow(Locator.XPathLocator table)
    {
        clickAddNewAndWait(baseElements().getOutergridAddNewRowLocator(table));
    }

    protected void clickSubgridAddNewRow(Locator.XPathLocator table, String column, int rowIndex)
    {
        clickAddNewAndWait(baseElements().getSubgridAddNewRowLocator(table, column, rowIndex));
    }

    private void clickAddNewAndWait(Locator.XPathLocator locToClick)
    {
        doAndWaitForElementToRefresh(() -> waitAndClick(locToClick), locToClick, shortWait());
    }

    protected void setOuterTextFieldValue(Locator.XPathLocator table, String column, String value, int rowIndex)
    {
        setTextFieldValue(baseElements().getOuterCellLocator(table, column, rowIndex), column, value);
    }

    protected void setOuterTextAreaValue(Locator.XPathLocator table, String column, String value, int rowIndex)
    {
        Locator.XPathLocator cellLoc = baseElements().getOuterCellLocator(table, column, rowIndex);
        setFormElement(cellLoc.append(Locator.tagWithName("textarea", column)), value);
        removeFocusAndWait();

    }

    protected void setSubgridTextFieldValue(Locator.XPathLocator table, String outerColumn, String column, String value, int outerRowIndex, int subgridRowIndex)
    {
        Locator.XPathLocator cellLoc = getSubgridCellLoc(table, outerColumn, column, outerRowIndex, subgridRowIndex);
        setTextFieldValue(cellLoc, column, value);
    }

    protected void setTextFieldValue(Locator.XPathLocator cellLoc, String column, String value)
    {
        setFormElement(cellLoc.append(Locator.tagWithName("input", column)), value);
        removeFocusAndWait();
    }

    protected void setOuterComboFieldValue(Locator.XPathLocator table, String column, String value, int rowIndex)
    {
        setComboFieldValue(baseElements().getOuterCellLocator(table, column, rowIndex), value);
    }

    protected void setSubgridComboFieldValue(Locator.XPathLocator table, String outerColumn, String column, String value, int outerRowIndex, int subgridRowIndex)
    {
        Locator.XPathLocator cellLoc = getSubgridCellLoc(table, outerColumn, column, outerRowIndex, subgridRowIndex);
        setComboFieldValue(cellLoc, value);
    }

    protected void setComboFieldValue(Locator.XPathLocator cellLoc, String value)
    {
        _ext4Helper.selectComboBoxItem(cellLoc, Ext4Helper.TextMatchTechnique.STARTS_WITH, value);
        removeFocusAndWait();
    }

    protected Locator.XPathLocator getSubgridCellLoc(Locator.XPathLocator table, String outerColumn, String column, int outerRowIndex, int subgridRowIndex)
    {
        Locator.XPathLocator subgridTableLoc = baseElements().getSubgridTableLocator(table, outerColumn, outerRowIndex);
        return subgridTableLoc.append(baseElements().cellValueLoc.withAttribute("outer-data-index", outerColumn).withAttribute("data-index", column).withAttribute("subgrid-index", subgridRowIndex+""));
    }

    protected void removeFocusAndWait()
    {
        removeFocusAndWait(null);
    }

    protected void removeFocusAndWait(String sectionTitle)
    {
        Locator loc;
        if (sectionTitle != null)
            loc = Locator.tagWithClass("div", "main-title").withText(sectionTitle);
        else
            loc = Locator.tagWithClass("div", "lk-body-title");

        loc.findElement(getDriver()).click(); // click outside field to remove focus
        sleep(1000); // give the store a second to update
    }

    public void cancel()
    {
        baseElements().cancelButton.click();
    }

    protected BaseElements baseElements()
    {
        return new BaseElements();
    }

    protected class BaseElements
    {
        private Locator.XPathLocator tableOuterLoc = Locator.tagWithClass("table", "outer");
        private Locator.XPathLocator cellValueLoc = Locator.tagWithClass("td", "cell-value");
        private Locator.XPathLocator cellDisplayLoc = Locator.tagWithClass("td", "cell-display");
        private Locator.XPathLocator addRowIconLoc = Locator.tagWithClass("i", "add-new-row");

        protected int wait = BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
        protected Locator.XPathLocator tableRowLoc = Locator.tagWithClass("tr", "row-outer");
        protected Locator.XPathLocator studyVaccineDesignLoc = Locator.tagWithClass("div", "study-vaccine-design");
        protected Locator.XPathLocator outerAddRowIconLoc = Locator.tagWithClass("i", "outer-add-new-row");

        Locator.XPathLocator getOuterCellLocator(Locator.XPathLocator table, String column, int rowIndex)
        {
            Locator.XPathLocator tableLoc = table.append(tableRowLoc);
            return tableLoc.append(cellValueLoc.withAttribute("data-index", column).withAttribute("outer-index", rowIndex+""));
        }

        Locator.XPathLocator getOutergridAddNewRowLocator(Locator.XPathLocator table)
        {
            return table.append(outerAddRowIconLoc);
        }

        Locator.XPathLocator getSubgridTableLocator(Locator.XPathLocator table, String column, int rowIndex)
        {
            Locator.XPathLocator tableLoc = table.append(tableRowLoc);
            Locator.XPathLocator cellLoc = tableLoc.append(cellDisplayLoc.withAttribute("outer-index", rowIndex+""));
            return cellLoc.append(Locator.tagWithClass("table", "subgrid-" + column));
        }

        Locator.XPathLocator getSubgridAddNewRowLocator(Locator.XPathLocator table, String column, int rowIndex)
        {
            Locator.XPathLocator subgridTable = getSubgridTableLocator(table, column, rowIndex);
            return subgridTable.append(addRowIconLoc);
        }

        WebElement saveButton = ext4Button("Save").findWhenNeeded(getDriver()).withTimeout(wait);
        WebElement cancelButton = ext4Button("Cancel").findWhenNeeded(getDriver()).withTimeout(wait);
    }
}
