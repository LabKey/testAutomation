/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.test.components;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlateGrid extends WebDriverComponent
{
    private static final String EXCLUDED_PLATE_SUMMARY_XPATH = "//h3[text()='$']/../..//table[contains(@class, 'plate-summary')]";

    private final WebDriver _driver;
    private final WebElement _plateGridEl;
    private List<List<WebElement>> _gridValues;
    private Map<String, Integer> _rowsIndex;
    private Map<String, Integer> _colsIndex;

    private PlateGrid(WebDriver driver, Locator.XPathLocator plateGridLocator)
    {
        _driver = driver;
        _plateGridEl = plateGridLocator.waitForElement(driver, 10000);
        doInit();
    }

    public PlateGrid(WebDriver driver, String plateId)
    {
        this(driver, Locator.xpath(EXCLUDED_PLATE_SUMMARY_XPATH.replace("$", plateId)));
    }

    public PlateGrid(WebDriver driver)
    {
        this(driver, Locator.tagWithClass("table", "plate-summary"));
    }

    private void doInit()
    {
        // If there is a header the ROW_OFFSET needs to be adjusted.
        int rowOffset = Locator.tag("th").findElements(this).size() + 1;

        List<WebElement> rows = Locator.tag("tr").findElements(this);
        // Remove header row(s)
        rows.subList(0, rowOffset).clear();

        _gridValues = new ArrayList<>();
        for (WebElement row : rows)
        {
            List<WebElement> cols = Locator.tag("td").findElements(row);
            cols.subList(0, 1).clear();
            _gridValues.add(cols);
        }

        char rowName = 'A';
        _rowsIndex = new HashMap<>();
        for (int i = 0; i < rows.size(); i++)
        {
            _rowsIndex.put(Character.toString(rowName), i);
            rowName++;
        }

        final int columnCount = _gridValues.get(0).size();
        _colsIndex = new HashMap<>();
        for(int j = 0; j < columnCount; j++)
        {
            _colsIndex.put(Integer.toString(j+1), j);
        }
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _plateGridEl;
    }

    public WebElement getCellElement(String row, String col)
    {

        return _gridValues.get(_rowsIndex.get(row)).get(_colsIndex.get(col));
    }

    public String getCellValue(String row, String col)
    {
        return getCellElement(row, col).getText();
    }

    public boolean isCellExcluded(String row, String col)
    {
        String classValue = getCellElement(row, col).getAttribute("class");
        return classValue.toLowerCase().contains("excluded");
    }

    public List<WebElement> getExcludedCells()
    {
        return Locator.tagWithClass("td", "excluded").findElements(this);
    }

    public List<String> getExcludedValues()
    {
        List<String> values = new ArrayList<>();
        List<WebElement> excludeCells = getExcludedCells();

        for (WebElement we : excludeCells)
        {
            values.add(we.getText());
        }

        return values;
    }
}
