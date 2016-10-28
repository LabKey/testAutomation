/*
 * Copyright (c) 2016 LabKey Corporation
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
import org.labkey.test.components.ext4.Window;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebElement;

public class BaseManageVaccineDesignVisitPage extends BaseManageVaccineDesignPage
{
    public BaseManageVaccineDesignVisitPage(BaseWebDriverTest test)
    {
        super(test);
    }

    public void addAllExistingVisitColumns(Locator.XPathLocator table)
    {
        addExistingVisitColumn(table, "[Show All]");
    }

    public void addExistingVisitColumn(Locator.XPathLocator table, String visitLabel)
    {
        clickOuterAddNewVisit(table);

        waitForElement(visitElements().existingVisitLoc);
        _ext4Helper.selectComboBoxItem(visitElements().existingVisitLoc, visitLabel);

        Window addVisitWindow = new Window("Add Visit", getDriver());
        doAndWaitForElementToRefresh(() -> addVisitWindow.clickButton("Select", 0), visitElements().addVisitIconLoc, shortWait());
        removeFocusAndWait();
    }

    public void addNewVisitColumn(Locator.XPathLocator table, String label, Double rangeMin, Double rangeMax)
    {
        clickOuterAddNewVisit(table);

        visitElements().getNewVisitRadio().click();
        setFormElement(visitElements().getNewVisitLabelField(), label);
        if (rangeMin != null)
            setFormElement(visitElements().getNewVisitMinField(), rangeMin.toString());
        if (rangeMax != null)
            setFormElement(visitElements().getNewVisitMaxField(), rangeMax.toString());

        Window addVisitWindow = new Window("Add Visit", getDriver());
        doAndWaitForElementToRefresh(() -> addVisitWindow.clickButton("Submit", 0), visitElements().addVisitIconLoc, shortWait());
        removeFocusAndWait();
    }

    protected void clickOuterAddNewVisit(Locator.XPathLocator table)
    {
        visitElements().getAddVisitIcon(table).click();
    }

    protected BaseVisitElements visitElements()
    {
        return new BaseVisitElements();
    }

    protected class BaseVisitElements extends BaseElements
    {
        private Locator.XPathLocator newVisitRadioLoc = Ext4Helper.Locators.radiobutton(_test, "Create a new study visit:");
        private Locator.XPathLocator newVisitLabelLoc = Locator.tagWithName("input", "newVisitLabel");
        private Locator.XPathLocator newVisitMinLoc = Locator.tagWithName("input", "newVisitRangeMin");
        private Locator.XPathLocator newVisitMaxLoc = Locator.tagWithName("input", "newVisitRangeMax");

        Locator.XPathLocator existingVisitLoc = Locator.tagWithClass("table", "x4-field").withDescendant(Locator.tagWithName("input", "existingVisit"));
        Locator.XPathLocator addVisitIconLoc = Locator.tagWithClass("i", "add-visit-column");

        WebElement getAddVisitIcon(Locator.XPathLocator table)
        {
            return table.append(addVisitIconLoc).refindWhenNeeded(getDriver()).withTimeout(wait);
        }

        WebElement getNewVisitRadio()
        {
            return newVisitRadioLoc.findWhenNeeded(getDriver());
        }

        WebElement getNewVisitLabelField()
        {
            return visitElements().newVisitLabelLoc.findWhenNeeded(getDriver());
        }

        WebElement getNewVisitMinField()
        {
            return visitElements().newVisitMinLoc.findWhenNeeded(getDriver());
        }

        WebElement getNewVisitMaxField()
        {
            return visitElements().newVisitMaxLoc.findWhenNeeded(getDriver());
        }
    }

    public static class Visit
    {
        private Integer _rowId;
        private String _label;
        private Double _rangeMin;
        private Double _rangeMax;

        public Visit(String visit)
        {
            _label = visit;
        }

        public Visit(String visit, Double rangeMin, Double rangeMax)
        {
            _label = visit;
            _rangeMin = rangeMin;
            _rangeMax = rangeMax;
        }

        public Integer getRowId()
        {
            return _rowId;
        }

        public void setRowId(Integer rowId)
        {
            _rowId = rowId;
        }

        public String getLabel()
        {
            return _label;
        }

        public Double getRangeMin()
        {
            return _rangeMin;
        }

        public Double getRangeMax()
        {
            return _rangeMax;
        }
    }
}
