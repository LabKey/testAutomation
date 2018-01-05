/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.test.pages.study;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ManageVisitPage extends LabKeyPage<ManageVisitPage.ElementCache>
{
    // TODO refactor more of the Manage Visit page usages and page components

    public ManageVisitPage(WebDriver driver)
    {
        super(driver);
    }

    public static ManageVisitPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ManageVisitPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("study", containerPath, "manageVisits"));
        return new ManageVisitPage(driver.getDriver());
    }

    public int getVisitRowCount()
    {
        waitForElement(elementCache().visitsTableLoc);
        return elementCache().getVisitRows().size();
    }

    public boolean hasVisitForSequenceRange(String rangeStr)
    {
        return elementCache().getVisitsBySequenceRange(rangeStr).size() == 1;
    }

    public void goToChangeVisitOrder()
    {
        clickAndWait(elementCache().changeVisitOrderLoc);
    }

    public void goToCreateNewVisit()
    {
        clickAndWait(elementCache().createNewVisitLoc);
    }

    public void goToImportVisitMap()
    {
        clickAndWait(elementCache().importVisitMapLoc);
    }

    public DeleteMultipleVisitsPage goToDeleteMultipleVisits()
    {
        clickAndWait(elementCache().deleteMultipleVisitsLoc);
        return new DeleteMultipleVisitsPage(getDriver());
    }

    public void goToEditVisit(String visitLabel)
    {
        clickAndWait(elementCache().getVisitEditLink(visitLabel));
        waitForElement(Locator.lkButton("Delete Visit"));
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Locator.XPathLocator visitsTableLoc = Locator.id("visits");
        Locator.XPathLocator changeVisitOrderLoc = Locator.linkWithText("Change Visit Order");
        Locator.XPathLocator createNewVisitLoc = Locator.linkWithText("Create New Visit");
        Locator.XPathLocator deleteMultipleVisitsLoc = Locator.linkWithText("Delete Multiple Visits");
        Locator.XPathLocator importVisitMapLoc = Locator.linkWithText("Import Visit Map");

        List<WebElement> getVisitRows()
        {
            return visitsTableLoc.append(Locator.tagWithClass("tr", "visit-row")).findElements(this);
        }

        List<WebElement> getVisitsBySequenceRange(String rangeStr)
        {
            return visitsTableLoc.append(Locator.tagWithClass("td", "visit-range-cell").withText(rangeStr)).findElements(this);
        }

        WebElement getVisitEditLink(String label)
        {
            Locator.XPathLocator loc = Locator.xpath("//tr[./td[text() = '" + label + "']]/td/a[@data-original-title = 'edit']");

            return visitsTableLoc.append(loc).findElement(this);
        }
    }
}