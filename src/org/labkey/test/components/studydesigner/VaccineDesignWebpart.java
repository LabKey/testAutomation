package org.labkey.test.components.studydesigner;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.WebPart;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class VaccineDesignWebpart extends BodyWebPart<VaccineDesignWebpart.ElementCache>
{
    public VaccineDesignWebpart(WebDriver driver)
    {
        super(driver, "Vaccine Design");
    }

    @Override
    protected void waitForReady()
    {
        getWrapper().waitForElement(elementCache().getImmunogensTableLocator());
        getWrapper().waitForElement(elementCache().getAdjuvantsTableLocator());
    }

    public boolean isEmpty()
    {
        return elementCache().isImmunogensTableEmpty() && elementCache().isAdjuvantsTableEmpty();
    }

    public int getImmunogenRowCount()
    {
        return elementCache().getImmunogensRowCount();
    }

    public int getAdjuvantRowCount()
    {
        return elementCache().getAdjuvantsRowCount();
    }

    public int getImmunogenAntigenRowCount(int rowIndex)
    {
        return elementCache().getImmunogenAdjuvantRowCount(rowIndex);
    }

    public int getImmunogenDoseAndRouteRowCount(int rowIndex)
    {
        return elementCache().getImmunogenDoseAndRouteRowCount(rowIndex);
    }

    public String getImmunogenAntigenRowCellDisplayValue(String column, int outerRowIndex, int subgridRowIndex)
    {
        return elementCache().getImmunogensAntigenCell(column, outerRowIndex, subgridRowIndex).getText();
    }

    public String getImmunogenDoseAndRouteCellDisplayValue(String column, int outerRowIndex, int subgridRowIndex)
    {
        return elementCache().getImmunogensDoseAndRouteCell(column, outerRowIndex, subgridRowIndex).getText();
    }

    public String getImmunogenCellDisplayValue(String column, int rowIndex)
    {
        return elementCache().getImmunogensCell(column, rowIndex).getText();
    }

    public String getAdjuvantCellDisplayValue(String column, int rowIndex)
    {
        return elementCache().getAdjuvantsCell(column, rowIndex).getText();
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
        private Locator.XPathLocator tableOuterLoc = Locator.tagWithClass("table", "outer");
        private Locator.XPathLocator tableRowLoc = Locator.tagWithClass("tr", "row-outer");
        private Locator.XPathLocator subgridRowLoc = Locator.tagWithClass("tr", "subrow");
        private Locator.XPathLocator cellDisplayLoc = Locator.tagWithClass("td", "cell-display");
        private Locator.XPathLocator emptyLoc = Locator.tagWithClassContaining("td", "empty").withText("No data to show.");
        private Locator.XPathLocator manageLoc = Locator.linkWithText("Manage Study Products");
        private Locator.XPathLocator immunogensLoc = Locator.tagWithClass("div", "vaccine-design-immunogens");
        private Locator.XPathLocator adjuvantsLoc = Locator.tagWithClass("div", "vaccine-design-adjuvants");

        Locator.XPathLocator getImmunogensTableLocator()
        {
            return immunogensLoc.append(elementCache().tableOuterLoc);
        }

        Locator.XPathLocator getAdjuvantsTableLocator()
        {
            return adjuvantsLoc.append(elementCache().tableOuterLoc);
        }

        WebElement immunogensTable = immunogensLoc.append(tableOuterLoc).findWhenNeeded(this).withTimeout(wait);
        WebElement adjuvantsTable = adjuvantsLoc.append(tableOuterLoc).findWhenNeeded(this).withTimeout(wait);
        WebElement manageLink = manageLoc.findWhenNeeded(this).withTimeout(wait);

        WebElement getImmunogensCell(String column, int rowIndex)
        {
            Locator.XPathLocator cellLoc = immunogensLoc.append(elementCache().cellDisplayLoc.withAttribute("data-index", column).withAttribute("outer-index", rowIndex+""));
            return cellLoc.findElement(getDriver());
        }

        WebElement getImmunogensAntigenCell(String column, int outerRowIndex, int subgridRowIndex)
        {
            Locator.XPathLocator cellLoc = elementCache().immunogensLoc.append(elementCache().cellDisplayLoc.withAttribute("outer-index", outerRowIndex+""));
            Locator.XPathLocator subgridTableLoc = cellLoc.append(Locator.tagWithClass("table", "subgrid-Antigens"));
            Locator.XPathLocator subgridCellLoc = subgridTableLoc.append(elementCache().cellDisplayLoc.withAttribute("data-index", column).withAttribute("subgrid-index", subgridRowIndex+""));
            return subgridCellLoc.findElement(getDriver());
        }

        WebElement getImmunogensDoseAndRouteCell(String column, int outerRowIndex, int subgridRowIndex)
        {
            Locator.XPathLocator cellLoc = elementCache().immunogensLoc.append(elementCache().cellDisplayLoc.withAttribute("outer-index", outerRowIndex+""));
            Locator.XPathLocator subgridTableLoc = cellLoc.append(Locator.tagWithClass("table", "subgrid-DoseAndRoute"));
            Locator.XPathLocator subgridCellLoc = subgridTableLoc.append(elementCache().cellDisplayLoc.withAttribute("data-index", column).withAttribute("subgrid-index", subgridRowIndex+""));
            return subgridCellLoc.findElement(getDriver());
        }

        WebElement getAdjuvantsCell(String column, int rowIndex)
        {
            Locator.XPathLocator cellLoc = adjuvantsLoc.append(elementCache().cellDisplayLoc.withAttribute("data-index", column).withAttribute("outer-index", rowIndex+""));
            return cellLoc.findElement(getDriver());
        }

        int getImmunogensRowCount()
        {
            return immunogensLoc.append(elementCache().tableRowLoc).findElements(getDriver()).size();
        }

        int getAdjuvantsRowCount()
        {
            return adjuvantsLoc.append(elementCache().tableRowLoc).findElements(getDriver()).size();
        }

        int getImmunogenAdjuvantRowCount(int rowIndex)
        {
            Locator.XPathLocator cellLoc = elementCache().immunogensLoc.append(elementCache().cellDisplayLoc.withAttribute("outer-index", rowIndex+""));
            Locator.XPathLocator subgridTableLoc = cellLoc.append(Locator.tagWithClass("table", "subgrid-Antigens"));
            if (!getWrapper().isElementPresent(subgridTableLoc))
                return 0;
            else
                return subgridTableLoc.append(elementCache().subgridRowLoc).findElements(getDriver()).size();
        }

        int getImmunogenDoseAndRouteRowCount(int rowIndex)
        {
            Locator.XPathLocator cellLoc = elementCache().immunogensLoc.append(elementCache().cellDisplayLoc.withAttribute("outer-index", rowIndex+""));
            Locator.XPathLocator subgridTableLoc = cellLoc.append(Locator.tagWithClass("table", "subgrid-DoseAndRoute"));
            if (!getWrapper().isElementPresent(subgridTableLoc))
                return 0;
            else
                return subgridTableLoc.append(elementCache().subgridRowLoc).findElements(getDriver()).size();
        }


        boolean isImmunogensTableEmpty()
        {
            immunogensTable.findElement(By.xpath(elementCache().tableOuterLoc.getLoc()));
            return getWrapper().isElementPresent(elementCache().immunogensLoc.append(elementCache().tableOuterLoc).append(elementCache().emptyLoc));
        }

        boolean isAdjuvantsTableEmpty()
        {
            adjuvantsTable.findElement(By.xpath(elementCache().tableOuterLoc.getLoc()));
            return getWrapper().isElementPresent(elementCache().adjuvantsLoc.append(elementCache().tableOuterLoc).append(elementCache().emptyLoc));
        }
    }
}
