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
package org.labkey.test.components;

import org.labkey.test.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlateGrid
{
    public final String EXCLUDED_PLATE_SUMMARY_XPATH = "//h3[text()='$']/../..//table[contains(@class, 'plate-summary')]";

    private WebDriver _driver;
    private WebElement[][] _gridValues;
    private Map<String, Integer> _rowsIndex;
    private Map<String, Integer> _colsIndex;
    private String _plateGridXpath;

    private int ROW_OFFSET = 1;
    private int COL_OFFSET = 1;

    public PlateGrid(WebDriver driver, String plateId)
    {
        _driver = driver;
        doInit(EXCLUDED_PLATE_SUMMARY_XPATH.replace("$", plateId));
    }

    public PlateGrid(WebDriver driver)
    {
        _driver = driver;
        doInit("//table[contains(@class, 'plate-summary')]");
    }

    private void doInit(String xPath)
    {
        int rows, cols;

        _plateGridXpath = xPath;

        // If there is a header the ROW_OFFSET needs to be adjusted.
        ROW_OFFSET = Locator.findElements(_driver, Locator.xpath(_plateGridXpath + "//th")).size() + ROW_OFFSET;

        rows = Locator.findElements(_driver, Locator.xpath(_plateGridXpath)).get(0).findElements(By.tagName("tr")).size() - ROW_OFFSET;
        cols = Locator.findElements(_driver, Locator.xpath(_plateGridXpath)).get(0).findElements(By.tagName("tr")).get(1).findElements(By.tagName("td")).size() - COL_OFFSET;

        _gridValues = new WebElement[cols][rows];

        char rowName = 'A';
        _rowsIndex = new HashMap<>();
        for(int i=0; i<rows; i++)
        {
            _rowsIndex.put(Character.toString(rowName), i);
            rowName++;
        }

        _colsIndex = new HashMap<>();
        for(int j=0; j<cols; j++)
        {
            _colsIndex.put(Integer.toString(j+1), j);
        }

        _gridValues = populateGrid();
    }

    private WebElement[][] populateGrid()
    {
        int rowCount, columnCount;
        List<WebElement> rows = Locator.findElements(_driver, Locator.xpath(_plateGridXpath)).get(0).findElements(By.tagName("tr"));
        rowCount = rows.size() - ROW_OFFSET;
        columnCount = Locator.findElements(_driver, Locator.xpath(_plateGridXpath)).get(0).findElements(By.tagName("tr")).get(1).findElements(By.tagName("td")).size() - COL_OFFSET;

        WebElement[][] plateGrid = new WebElement[rowCount][columnCount];

        for(int row = ROW_OFFSET; row < rows.size(); row++)
        {
            List<WebElement> cols = rows.get(row).findElements(By.tagName("td"));
            for(int col = COL_OFFSET; col < cols.size(); col++ )
            {
                plateGrid[row- ROW_OFFSET][col- COL_OFFSET] = cols.get(col);
            }
        }

        return plateGrid;
    }

    public WebElement getCellElement(String row, String col)
    {

        return _gridValues[_rowsIndex.get(row)][_colsIndex.get(col)];
    }

    public String getCellValue(String row, String col)
    {
        return _gridValues[_rowsIndex.get(row)][_colsIndex.get(col)].getText();
    }

    public boolean isCellExcluded(String row, String col)
    {
        String classValue = _gridValues[_rowsIndex.get(row)][_colsIndex.get(col)].getAttribute("class");
        return classValue.toLowerCase().contains("excluded");
    }

    public List<WebElement> getExcludedCells()
    {
        return Locator.findElements(_driver, Locator.xpath(_plateGridXpath + "//td[contains(@class, 'excluded')]"));
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
