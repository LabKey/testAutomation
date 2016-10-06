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

    public void addNewVisitColumn(Locator.XPathLocator table, String label, Integer rangeMin, Integer rangeMax)
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
        private Integer _rangeMin;
        private Integer _rangeMax;

        public Visit(String visit)
        {
            _label = visit;
        }

        public Visit(String visit, Integer rangeMin, Integer rangeMax)
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

        public Integer getRangeMin()
        {
            return _rangeMin;
        }

        public Integer getRangeMax()
        {
            return _rangeMax;
        }
    }
}
