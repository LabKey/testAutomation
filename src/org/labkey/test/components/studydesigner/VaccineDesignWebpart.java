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
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.WebPart;
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
        elementCache().immunogensTable.isDisplayed();
        elementCache().adjuvantsTable.isDisplayed();
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

    public int getChallengesRowCount()
    {
        return elementCache().getChallengesRowCount();
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

    public String getChallengeCellDisplayValue(String column, int rowIndex)
    {
        return elementCache().getChallengesCell(column, rowIndex).getText();
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
        private Locator.XPathLocator challengesLoc = Locator.tagWithClass("div", "vaccine-design-challenges");

        WebElement immunogensTable = immunogensLoc.append(tableOuterLoc).findWhenNeeded(this).withTimeout(wait);
        WebElement adjuvantsTable = adjuvantsLoc.append(tableOuterLoc).findWhenNeeded(this).withTimeout(wait);
        WebElement manageLink = manageLoc.findWhenNeeded(this).withTimeout(wait);

        WebElement getImmunogensCell(String column, int rowIndex)
        {
            Locator.XPathLocator cellLoc = immunogensLoc.append(cellDisplayLoc.withAttribute("data-index", column).withAttribute("outer-index", rowIndex+""));
            return cellLoc.findElement(this);
        }

        WebElement getImmunogensAntigenCell(String column, int outerRowIndex, int subgridRowIndex)
        {
            Locator.XPathLocator cellLoc = immunogensLoc.append(cellDisplayLoc.withAttribute("outer-index", outerRowIndex+""));
            Locator.XPathLocator subgridTableLoc = cellLoc.append(Locator.tagWithClass("table", "subgrid-Antigens"));
            Locator.XPathLocator subgridCellLoc = subgridTableLoc.append(cellDisplayLoc.withAttribute("data-index", column).withAttribute("subgrid-index", subgridRowIndex+""));
            return subgridCellLoc.findElement(this);
        }

        WebElement getImmunogensDoseAndRouteCell(String column, int outerRowIndex, int subgridRowIndex)
        {
            Locator.XPathLocator cellLoc = immunogensLoc.append(cellDisplayLoc.withAttribute("outer-index", outerRowIndex+""));
            Locator.XPathLocator subgridTableLoc = cellLoc.append(Locator.tagWithClass("table", "subgrid-DoseAndRoute"));
            Locator.XPathLocator subgridCellLoc = subgridTableLoc.append(cellDisplayLoc.withAttribute("data-index", column).withAttribute("subgrid-index", subgridRowIndex+""));
            return subgridCellLoc.findElement(this);
        }

        WebElement getAdjuvantsCell(String column, int rowIndex)
        {
            Locator.XPathLocator cellLoc = adjuvantsLoc.append(cellDisplayLoc.withAttribute("data-index", column).withAttribute("outer-index", rowIndex+""));
            return cellLoc.findElement(this);
        }

        int getImmunogensRowCount()
        {
            return immunogensLoc.append(tableRowLoc).findElements(this).size();
        }

        int getAdjuvantsRowCount()
        {
            return adjuvantsLoc.append(tableRowLoc).findElements(this).size();
        }

        int getImmunogenAdjuvantRowCount(int rowIndex)
        {
            Locator.XPathLocator cellLoc = immunogensLoc.append(cellDisplayLoc.withAttribute("outer-index", rowIndex+""));
            Locator.XPathLocator subgridTableLoc = cellLoc.append(Locator.tagWithClass("table", "subgrid-Antigens"));
            if (!getWrapper().isElementPresent(subgridTableLoc))
                return 0;
            else
                return subgridTableLoc.append(subgridRowLoc).findElements(this).size();
        }

        int getImmunogenDoseAndRouteRowCount(int rowIndex)
        {
            Locator.XPathLocator cellLoc = immunogensLoc.append(cellDisplayLoc.withAttribute("outer-index", rowIndex+""));
            Locator.XPathLocator subgridTableLoc = cellLoc.append(Locator.tagWithClass("table", "subgrid-DoseAndRoute"));
            if (!getWrapper().isElementPresent(subgridTableLoc))
                return 0;
            else
                return subgridTableLoc.append(subgridRowLoc).findElements(this).size();
        }

        boolean isImmunogensTableEmpty()
        {
            return emptyLoc.findElementOrNull(immunogensTable) != null;
        }

        boolean isAdjuvantsTableEmpty()
        {
            return emptyLoc.findElementOrNull(adjuvantsTable) != null;
        }

        int getChallengesRowCount()
        {
            return challengesLoc.append(tableRowLoc).findElements(this).size();
        }

        WebElement getChallengesCell(String column, int rowIndex)
        {
            Locator.XPathLocator cellLoc = challengesLoc.append(cellDisplayLoc.withAttribute("data-index", column).withAttribute("outer-index", rowIndex+""));
            return cellLoc.findElement(this);
        }
    }
}
