/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
import org.labkey.test.components.ext4.Checkbox;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ManageAssaySchedulePage extends BaseManageVaccineDesignVisitPage
{
    public ManageAssaySchedulePage(BaseWebDriverTest test, boolean canInsert)
    {
        super(test);
        waitForElements(elements().studyVaccineDesignLoc, 2);
        if (canInsert)
            waitForElements(elements().outerAddRowIconLoc, 1);
    }

    public int getAssayRowCount()
    {
        return elements().getAssayTableRowCount();
    }

    public boolean canAddNewRow()
    {
        return isElementPresent(baseElements().getOutergridAddNewRowLocator(elements().assaysLoc));
    }

    public void addNewAssayRow(String label, String description, int rowIndex)
    {
        clickOuterAddNewRow(elements().assaysLoc);

        setOuterTextFieldValue(elements().assaysLoc, "AssayName", label, rowIndex);
        if (description != null)
            setOuterTextFieldValue(elements().assaysLoc, "Description", description, rowIndex);
    }

    public void setBaseProperties(String lab, String sampleType, Integer sampleQuantity, String sampleUnits, String dataset, int rowIndex)
    {
        if (lab != null)
            setOuterComboFieldValue(elements().assaysLoc, "Lab", lab, rowIndex);
        if (sampleType != null)
            setOuterComboFieldValue(elements().assaysLoc, "SampleType", sampleType, rowIndex);
        if (sampleQuantity != null)
            setOuterTextFieldValue(elements().assaysLoc, "SampleQuantity", sampleQuantity+"", rowIndex);
        if (sampleUnits != null)
            setOuterComboFieldValue(elements().assaysLoc, "SampleUnits", sampleUnits, rowIndex);
        if (dataset != null)
            setOuterComboFieldValue(elements().assaysLoc, "DataSet", dataset, rowIndex);
    }

    public void setComboFieldValue(String column, String value, int rowIndex)
    {
        setOuterComboFieldValue(elements().assaysLoc, column, value, rowIndex);
    }

    public void setTextFieldValue(String column, String value, int rowIndex)
    {
        setOuterTextFieldValue(elements().assaysLoc, column, value, rowIndex);
    }

    public void selectVisits(List<Visit> visits, int rowIndex)
    {
        for (Visit visit : visits)
            selectVisit(visit, rowIndex);
    }

    public void selectVisit(Visit visit, int rowIndex)
    {
        Checkbox visitCb = new Checkbox(elements().getVisitCheckbox(visit, rowIndex));
        visitCb.check();

        removeFocusAndWait();
    }

    public void setAssayPlan(String value)
    {
        setFormElement(elements().assayPlan, value);
    }

    public void addAllExistingVisitColumns()
    {
        addAllExistingVisitColumns(elements().assaysLoc);
    }

    public void addExistingVisitColumn(String visitLabel, boolean isVisitBased)
    {
        addExistingVisitColumn(elements().assaysLoc, isVisitBased, visitLabel);
    }

    public void addNewVisitColumn(String label, Double rangeMin, Double rangeMax)
    {
        addNewVisitColumn(elements().assaysLoc, label, rangeMin, rangeMax);
    }

    public void save()
    {
        doAndWaitForPageToLoad(() -> elements().saveButton.click());
    }

    protected Elements elements()
    {
        return new Elements();
    }

    protected class Elements extends BaseElements
    {
        private Locator.XPathLocator assayPlanLoc = Locator.tagWithName("textarea", "assayPlan");
        private Locator.XPathLocator checkbox = Locator.tagWithClass("input", "x4-form-checkbox");

        Locator.XPathLocator assaysLoc = Locator.tagWithClass("div", "vaccine-design-assays");

        WebElement assayPlan = assayPlanLoc.findWhenNeeded(getDriver()).withTimeout(wait);

        WebElement getVisitCheckbox(Visit visit, int rowIndex)
        {
            Locator.XPathLocator cellLoc = getOuterCellLocator(assaysLoc, "VisitMap", rowIndex);
            cellLoc = cellLoc.withAttribute("data-filter-value", visit.getRowId().toString());
            return cellLoc.append(checkbox).findElement(getDriver());
        }

        int getAssayTableRowCount()
        {
            return assaysLoc.append(tableRowLoc).findElements(getDriver()).size();
        }
    }
}
