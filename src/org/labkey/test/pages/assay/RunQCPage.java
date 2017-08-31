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
package org.labkey.test.pages.assay;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.components.PlateGrid;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class RunQCPage<EC extends RunQCPage.ElementCache> extends LabKeyPage<EC>
{
    WebDriver _driver;

    public RunQCPage(WebDriver driver){
        super(driver);
        _driver = driver;
    }

    public WebElement getSpecimenSection(String sectionTitle)
    {
        String xpath = elementCache().DILUTION_SUMMARY_XPATH + "//tbody//tr//td[text()='" + sectionTitle + "']";
        WebElement weSpecimen = new LazyWebElement(Locator.xpath(xpath), _driver);
        return weSpecimen;
    }

    public void clickNext()
    {
        elementCache().next.click();
        waitForElementToBeVisible(Locator.xpath("//div[@id='nabQCDiv']//div[contains(@class, 'x4-fit-item')][2]"));
        sleep(500);
    }

    public void clickPrevious()
    {
        elementCache().previous.click();
        waitForElementToBeVisible(Locator.xpath("//div[@id='nabQCDiv']//div[contains(@class, 'x4-fit-item')][1]"));
        sleep(500);
    }

    public void clickCancel()
    {
        elementCache().cancel.click();
        waitForPage();
        waitForElement(Locator.xpath("//table[contains(@class, 'plate-summary')]"));
    }

    public void clickFinish()
    {
        elementCache().finish.click();
        waitForPage();
        waitForElement(Locator.xpath("//table[contains(@class, 'plate-summary')]"));
        sleep(1000);
    }

    public void selectPlateItemsToIgnore(String plateTitle, List<String> valuesToIgnore)
    {
        String xpath = elementCache().PLATE_CONTROLS_VALUES_XPATH.replace("$", plateTitle);
        for(String value : valuesToIgnore)
        {
            click(Locator.xpath(xpath + "//label[text()='" + value + "']"));
        }
    }

    public void selectDilutionItemsToIgnore(String sectionTitle, List<String> valuesToIgnore)
    {
        String xpath = elementCache().DILUTION_SUMMARY_VALUES_XPATH.replace("$", sectionTitle);
        for(String value : valuesToIgnore)
        {
            click(Locator.xpath(xpath + "//label[text()='" + value + "']"));
        }
    }

    public List<String> getValuesFromDilution(String sectionTitle)
    {

        String xpath = elementCache().DILUTION_SUMMARY_ALL_VALUES_XPATH.replace("$", sectionTitle);
        List<String> values = new ArrayList<>();
        List<WebElement> elements = Locator.findElements(_driver, Locator.xpath(xpath));
        for(WebElement we : elements)
        {
            values.add(we.getText());
        }

        return values;
    }

    public void setExcludedComment(String row, String column, String comment)
    {
        WebElement excludedRow = findExcludedRow(row, column);
        setFormElement(excludedRow.findElement(By.className("field-exclusion-comment")), comment);
    }

    public void setExcludedComment(int index, String comment)
    {
        List<WebElement> excludedFields = getExcludedFields();
        setFormElement(excludedFields.get(index).findElement(By.className("field-exclusion-comment")), comment);
    }

    public String getExcludedComment(String row, String column)
    {
        WebElement excludedRow = findExcludedRow(row, column);
        return getFormElement(excludedRow.findElement(By.className("field-exclusion-comment")));
    }

    public String getExcludedComment(int index)
    {
        List<WebElement> excludedFields = getExcludedFields();
        return getFormElement(excludedFields.get(index).findElement(By.className("field-exclusion-comment")));
    }

    public void removeExclusion(String row, String column)
    {
        WebElement excludedRow = findExcludedRow(row, column);
        click(excludedRow.findElement(By.className("remove-exclusion")));
    }

    public List<WebElement> getExcludedFields()
    {
        return Locator.findElements(_driver, Locator.xpath(elementCache().EXCLUDED_FIELDS_XPATH));
    }

    private WebElement findExcludedRow(String row, String column)
    {
        List<WebElement> excludedFields = getExcludedFields();
        WebElement excludedRow = null;

        for(WebElement we : excludedFields)
        {
            if((we.findElements(By.tagName("td")).get(1).getText().toLowerCase().equals(row.toLowerCase())) &&
                    (we.findElements(By.tagName("td")).get(2).getText().toLowerCase().equals(column.toLowerCase())))
            {
                excludedRow = we;
                break;
            }
        }

        return excludedRow;
    }

    public PlateGrid getPlateGrid(String plateId)
    {
        return new PlateGrid(_driver, plateId);
    }

    public PlateGrid getPlateGrid()
    {
        return new PlateGrid(_driver);
    }

    @Override
    protected EC newElementCache()
    {
        return (EC) new RunQCPage.ElementCache();
    }


    protected class ElementCache extends LabKeyPage.ElementCache
    {
        public final String NEXT_XPATH = "//span[text()='Next']/following-sibling::span[@role='img']";
        public final String PREVIOUS_XPATH = "//span[text()='Previous']/following-sibling::span[@role='img']";
        public final String CANCEL_XPATH = "//span[text()='Cancel']/following-sibling::span[@role='img']";
        public final String FINISH_XPATH = "//span[text()='Finish']/following-sibling::span[@role='img']";
        public final String RUN_SUMMARY_XPATH = "//table[contains(@class, 'run-summary')]";
        public final String PLATE_CONTROLS_XPATH = "//table[contains(@class, 'plate-controls')]";
        public final String PLATE_CONTROLS_VALUES_XPATH = "//h3[text()='$']/../.." + PLATE_CONTROLS_XPATH;
        public final String DILUTION_SUMMARY_XPATH = "//table[contains(@class, 'dilution-summary')]";
        public final String DILUTION_SUMMARY_VALUES_XPATH = "//h3[text()='$']/../.." + DILUTION_SUMMARY_XPATH + "//table[contains(@class, 'labkey-data-region')]";
        public final String DILUTION_SUMMARY_ALL_VALUES_XPATH = DILUTION_SUMMARY_VALUES_XPATH + "//td[@class='dilution-checkbox']//label[contains(@class, 'x4-form-cb-label')]";
        public final String EXCLUDED_FIELD_WELLS_XPATH = "//table[contains(@class, 'field-exclusions')]";
        public final String EXCLUDED_FIELDS_XPATH = EXCLUDED_FIELD_WELLS_XPATH + "//tr[contains(@class, 'field-exclusion')]";

        public final WebElement next = new LazyWebElement(Locator.xpath(NEXT_XPATH), _driver);
        public final WebElement previous = new LazyWebElement(Locator.xpath(PREVIOUS_XPATH), _driver);
        public final WebElement cancel = new LazyWebElement(Locator.xpath(CANCEL_XPATH), _driver);
        public final WebElement finish = new LazyWebElement(Locator.xpath(FINISH_XPATH), _driver);

    }
}
