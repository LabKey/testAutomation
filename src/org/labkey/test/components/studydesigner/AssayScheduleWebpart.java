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

public class AssayScheduleWebpart extends BodyWebPart<AssayScheduleWebpart.ElementCache>
{
    public AssayScheduleWebpart(WebDriver driver)
    {
        super(driver, "Assay Schedule");
    }

    @Override
    protected void waitForReady()
    {
        elementCache().assaysTable.isDisplayed();
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
        private Locator.XPathLocator tableOuterLoc = Locator.tagWithClass("table", "outer");
        private Locator.XPathLocator tableRowLoc = Locator.tagWithClass("tr", "row-outer");
        private Locator.XPathLocator cellDisplayLoc = Locator.tagWithClass("td", "cell-display");
        private Locator.XPathLocator emptyLoc = Locator.tagWithClassContaining("td", "empty").withText("No data to show.");
        private Locator.XPathLocator assaysLoc = Locator.tagWithClass("div", "vaccine-design-assays");
        private Locator.XPathLocator assayPlanLoc = Locator.tag("p").withAttribute("data-index", "AssayPlan");

        WebElement assaysTable = assaysLoc.append(tableOuterLoc).findWhenNeeded(this).withTimeout(wait);
        WebElement assayPlan = assayPlanLoc.findWhenNeeded(this).withTimeout(wait);
        WebElement manageLink = Locator.linkWithText("Manage Assay Schedule").findWhenNeeded(this).withTimeout(wait);

        WebElement getAssayCell(String column, int rowIndex, String dataFilterValue)
        {
            Locator.XPathLocator rowLoc = assaysLoc.append(tableRowLoc);
            Locator.XPathLocator cellLoc = rowLoc.append(cellDisplayLoc.withAttribute("outer-index", rowIndex+"").withAttribute("data-index", column));
            if (dataFilterValue != null)
                cellLoc = cellLoc.withAttribute("data-filter-value", dataFilterValue);

            return cellLoc.findElement(this);
        }

        int getAssayRowCount()
        {
            return assaysLoc.append(tableRowLoc).findElements(this).size();
        }

        boolean isAssayTableEmpty()
        {
            return emptyLoc.findElementOrNull(assaysTable) != null;
        }
    }
}
