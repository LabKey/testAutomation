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

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;

import static org.labkey.test.util.Ext4Helper.Locators.ext4Button;

public class ManageTreatmentsSingleTablePage extends LabKeyPage<ManageTreatmentsSingleTablePage.ElementCache>
{
    private final int GROUP_COHORT_COLUMN = 1;
    private final int PARTICIPANT_COUNT_COLUMN = 2;
    private final String GROUP_COHORT_NAME = "Label";
    private final String PARTICIPANT_COUNT_NAME = "SubjectCount";

    WebDriverWrapper _webDriverWrapper;
    private ElementCache _elementCache;

    public ManageTreatmentsSingleTablePage(WebDriver driver)
    {
        super(driver);
        waitForElement(elementCache().treatmentScheduleTable);
        sleep(1000);
    }

    public ManageTreatmentsSingleTablePage(WebDriverWrapper driver)
    {
        super(driver);
        waitForElement(elementCache().treatmentScheduleTable);
        _webDriverWrapper = driver;
        sleep(1000);
    }

    public static ManageTreatmentsSingleTablePage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ManageTreatmentsSingleTablePage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("controller", containerPath, "action"));
        return new ManageTreatmentsSingleTablePage(driver.getDriver());
    }

    public int numberOfRows()
    {
        return elementCache().treatmentScheduleTable.append("/tbody/child::tr").findElements(_webDriverWrapper.getDriver()).size();
    }

    public int numberOfColumns()
    {
        return columnHeaders().size();
    }

    /**
     *
     * @return Returns a list of strings that are the column headers.
     */
    public List<String> columnHeaders()
    {
        List<String> headerText = new ArrayList<>();
        List<WebElement> headers = elementCache().treatmentScheduleTable.append("//tr[@class='header-row']//td").findElements(_webDriverWrapper.getDriver());

        for(WebElement header : headers)
        {
            headerText.add(header.getText());
        }

        return headerText;
    }

    /**
     * Get the value of a given cell.
     * @param rowIndex zero based
     * @param colIndex zero based
     * @return Text value of the input in the cell. If no input is found an error will be thrown.
     */
    public String getCellValue(int rowIndex, int colIndex)
    {
        // Need to increment because the collection of trs returned by selenium starts at 1.
        rowIndex++;
        List<WebElement> cells  = elementCache().treatmentScheduleTable.append("/tbody/child::tr[" + rowIndex + "]/child::td").findElements(_webDriverWrapper.getDriver());
        return getFormElement(cells.get(colIndex).findElement(By.tagName("input")));
    }

    /**
     * Set the value for the Group/Cohort cell.
     * @param rowIndex Row to set.
     * @param value Value to use.
     */
    public ManageTreatmentsSingleTablePage setGroupCohortCell(int rowIndex, String value)
    {
        setCellValue(rowIndex, GROUP_COHORT_NAME, value);
        return this;
    }

    /**
     * Set the value for the Participant Count cell.
     * @param rowIndex Row to set.
     * @param value Value to use.
     */
    public ManageTreatmentsSingleTablePage setParticipantCountCell(int rowIndex, String value)
    {
        setCellValue(rowIndex, PARTICIPANT_COUNT_NAME, value);
        return this;
    }

    private ManageTreatmentsSingleTablePage setCellValue(int rowIndex, String inputName, String value)
    {
        // Need to increment because the collection of trs returned by selenium starts at 1.
        rowIndex++;
        setFormElement(elementCache().treatmentScheduleTable.append("/tbody/child::tr[" + rowIndex + "]/child::td//input[@name='" + inputName + "']").findElement(_webDriverWrapper.getDriver()), value);
        return this;
    }

    /**
     * Click a given cell in the grid. If the cell is a treatment cell it will return a reference to the treatment dialog, null otherwise.
     * @param rowIndex rowIndex of the cell to click.
     * @param colIndex columnIndex of the cell to click. Needs tp be column 3 or higher.
     * @return A reference to the Treatment dialog, null otherwise.
     */
    public TreatmentDialog clickCell(int rowIndex, int colIndex)
    {
        // Need to increment because the collection of trs returned by selenium starts at 1.
        rowIndex++;
        List<WebElement> cells  = elementCache().treatmentScheduleTable.append("/tbody/child::tr[" + rowIndex + "]/child::td").findElements(_webDriverWrapper.getDriver());
        cells.get(colIndex).findElement(By.tagName("input")).click();

        // The first several columns will not open a treatment dialog, so take that into account.
        if(colIndex > 2)
            return new TreatmentDialog(_webDriverWrapper.getDriver());
        else
        {
            _webDriverWrapper.log("***** You are trying to open a treatment dialog froma  column that is not a treatment value. ******");
            return null;
        }
    }

    public void save()
    {
        doAndWaitForPageToLoad(() -> elementCache().saveButton.click());
    }

    public void cancel()
    {
        elementCache().cancelButton.click();
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Locator.XPathLocator treatmentScheduleTable = Locator.tagWithClass("div", "study-vaccine-design").append(Locator.tagWithClass("table", "outer"));

        Locator.XPathLocator addNewRow = Locator.tagWithClass("i", "fa-plus-circle").containing("Add new row");
        Locator.XPathLocator addNewVisit = Locator.tagWithClass("i", "fa-plus-circle").containing("Add new visit");

        WebElement saveButton = ext4Button("Save").findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement cancelButton = ext4Button("Cancel").findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
    }

}
