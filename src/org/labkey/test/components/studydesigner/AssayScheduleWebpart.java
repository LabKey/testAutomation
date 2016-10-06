package org.labkey.test.components.studydesigner;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.WebPart;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class AssayScheduleWebpart extends BodyWebPart<AssayScheduleWebpart.ElementCache>
{
    public AssayScheduleWebpart(WebDriver driver)
    {
        super(driver, "Assay Schedule");
    }

    @Override
    protected void waitForReady()
    {
        getWrapper().waitForElement(elementCache().getAssayTableLocator());
    }

    public boolean isEmpty()
    {
        return elementCache().isAssayTableEmpty();
    }

    public int getAssayRowCount()
    {
        return elementCache().getAssayRowCount();
    }

    public String getAssayCellDisplayValue(String column, int rowIndex)
    {
        return getAssayCellDisplayValue(column, rowIndex, null);
    }

    public String getAssayCellDisplayValue(String column, int rowIndex, String dataFilterValue)
    {
        return elementCache().getAssayCell(column, rowIndex, dataFilterValue).getText();
    }

    public String getAssayPlan()
    {
        return elementCache().assayPlan.getText();
    }

    public void manage()
    {
        elementCache().manageLink.click();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    public class ElementCache extends WebPart.ElementCache
    {
        private int wait = BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
        private Locator.XPathLocator manageLoc = Locator.linkWithText("Manage Assay Schedule");
        private Locator.XPathLocator tableOuterLoc = Locator.tagWithClass("table", "outer");
        private Locator.XPathLocator tableRowLoc = Locator.tagWithClass("tr", "row-outer");
        private Locator.XPathLocator cellDisplayLoc = Locator.tagWithClass("td", "cell-display");
        private Locator.XPathLocator emptyLoc = Locator.tagWithClassContaining("td", "empty").withText("No data to show.");
        private Locator.XPathLocator assaysLoc = Locator.tagWithClass("div", "vaccine-design-assays");
        private Locator.XPathLocator assayPlanLoc = Locator.tag("p").withAttribute("data-index", "AssayPlan");

        Locator.XPathLocator getAssayTableLocator()
        {
            return assaysLoc.append(elementCache().tableOuterLoc);
        }

        WebElement assaysTable = assaysLoc.append(tableOuterLoc).findWhenNeeded(this).withTimeout(wait);
        WebElement assayPlan = assayPlanLoc.findWhenNeeded(this).withTimeout(wait);
        WebElement manageLink = manageLoc.findWhenNeeded(this).withTimeout(wait);

        WebElement getAssayCell(String column, int rowIndex, String dataFilterValue)
        {
            Locator.XPathLocator rowLoc = elementCache().assaysLoc.append(elementCache().tableRowLoc);
            Locator.XPathLocator cellLoc = rowLoc.append(elementCache().cellDisplayLoc.withAttribute("outer-index", rowIndex+"").withAttribute("data-index", column));
            if (dataFilterValue != null)
                cellLoc = cellLoc.withAttribute("data-filter-value", dataFilterValue);

            return cellLoc.findElement(getDriver());
        }

        int getAssayRowCount()
        {
            return assaysLoc.append(elementCache().tableRowLoc).findElements(getDriver()).size();
        }

        boolean isAssayTableEmpty()
        {
            assaysTable.findElement(By.xpath(elementCache().tableOuterLoc.getLoc()));
            return getWrapper().isElementPresent(elementCache().assaysLoc.append(elementCache().tableOuterLoc).append(elementCache().emptyLoc));
        }
    }
}
