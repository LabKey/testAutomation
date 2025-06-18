/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
package org.labkey.test.components.html;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Table extends WebDriverComponent<Table.Elements>
{
    private final WebDriver _driver;
    private final WebElement _componentElement;
    private final int _bodyHeaderRowIndex; // '0' indicates that the table uses a 'thead'

    public Table(WebDriver driver, WebElement componentElement, int headerIndex)
    {
        _componentElement = componentElement;
        _driver = driver;
        _bodyHeaderRowIndex = headerIndex;
    }

    public Table(WebDriver driver, WebElement componentElement)
    {
        this(driver, componentElement, 1);
    }

    public Table(WebDriverWrapper driverWrapper, WebElement componentElement)
    {
        this(driverWrapper.getDriver(), componentElement);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _componentElement;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    protected class Elements extends Component<?>.ElementCache
    {
        List<WebElement> rows;

        public List<WebElement> getRows()
        {
            if (rows == null)
                rows = Locator.xpath("./tbody/tr").findElements(this);
            return rows;
        }
    }

    public int getRowCount()
    {
        return elementCache().getRows().size();
    }

    public List<WebElement> getRows()
    {
        return elementCache().getRows();
    }

    /**
     * For a well formed html table get the text from the th elements in the thead.
     *
     * @return A list of the text values contained in the th tags in the thead.
     */
    public List<String> getTableHeaderTexts()
    {
        List<WebElement> headerEls = Locator.xpath("./thead/tr[1]/th").findElements(this);
        List<String> columnHeaders = new ArrayList<>();
        for(WebElement headerEl : headerEls){columnHeaders.add(headerEl.getText());}
        return columnHeaders;
    }

    public int getTableHeaderIndex(String headerText)
    {
        List<WebElement> headerEls = Locator.xpath("./thead/tr[1]/th").findElements(this);
        int counter = 1;
        for(WebElement headerEl : headerEls)
        {
            if(headerEl.getText().equalsIgnoreCase(headerText))
                return counter;
            counter++;
        }
        throw new RuntimeException( headerText + " column not found");
    }

    /**
     * Get table data as a list of maps. Each map represents a row.<br>
     * Assumes a simple table with a single header row with no colspans and unique header labels
     * @return table data
     */
    public List<Map<String, String>> getTableData()
    {
        List<Map<String, String>> data = new ArrayList<>();

        List<String> headerTexts = getTableHeaderTexts();
        List<WebElement> rows = elementCache().getRows();

        for (WebElement row : rows)
        {
            List<String> dataTexts = getWrapper().getTexts(Locator.tag("td").findElements(row));
            if (headerTexts.size() != dataTexts.size())
            {
                throw new IllegalStateException("Size of row %s doesn't match table header %s".formatted(dataTexts, headerTexts));
            }
            Map<String, String> rowMap = new LinkedHashMap<>();
            for (int i = 0; i < headerTexts.size(); i++)
            {
                rowMap.put(headerTexts.get(i), dataTexts.get(i));
            }
            data.add(rowMap);
        }

        return data;
    }

    public List<String> getTableHeaderColumnData(String headerText)
    {
        List<String> columnData = new ArrayList<>();
        int columnIndex = getTableHeaderIndex(headerText);
        for(int i = _bodyHeaderRowIndex; i <= getRowCount(); i++)
            columnData.add(Locator.xpath("//tbody//tr[" + i + "]/td[" + columnIndex + "]").findElement(getDriver()).getText());
        return columnData;
    }

    public List<String> getColumnHeaders()
    {
        List<WebElement> headerEls = getColumnHeaderElements();
        List<String> columnHeaders = new ArrayList<>();
        for(WebElement headerEl : headerEls){columnHeaders.add(headerEl.getText());}
        return columnHeaders;
    }

    // TODO: This finder makes an assumption that the column headers will be in a tr in the tbody, that is not always the case.
    // Maybe a possible solution would be to remove "./tbody" from the locator, but that is a thread I am not willing to pull at this time.
    public List<WebElement> getColumnHeaderElements()
    {
        return getComponentElement().findElements(By.xpath("./tbody/tr["+ _bodyHeaderRowIndex +"]/*[(name()='TH' or name()='TD' or name()='th' or name()='td')]"));
    }

    public List<WebElement> getColumnHeaderElementsByTag()
    {
        return getComponentElement().findElements(By.xpath(".//tr/th"));
    }

    public int getColumnIndex(String headerLabel)
    {
        //List is zero based, locators that are going to depend on this are 1
        return getColumnHeaders().indexOf(headerLabel) + 1;
    }

    public String getDataAsText(int row, int col)
    {
        return _getDataAsText(row , col);
    }

    private String _getDataAsText(int row, int column)
    {
        String ret = null;

        try
        {
            ret = _getDataAsElement(row, column).getText();
        }
        catch (NoSuchElementException ignore) {}

        return ret;
    }

    public String getDataAsText(int row, String columnName)
    {
        return getColumnAsText(columnName).get(row);
    }

    private WebElement _getDataAsElement(int row, int column)
    {
        return getComponentElement().findElement(By.xpath("./tbody/tr[" +row+ "]/td[" +column+ "]"));
    }

    public WebElement getDataAsElement(int row, int column)
    {
        return  _getDataAsElement(row, column);
    }

    public List<String> getColumnAsText(int col)
    {
        List<WebElement> columnElements = getColumnAsElement(col);
        List<String> columnText = new ArrayList<>();

        if (!columnElements.isEmpty())
        {
            for (WebElement columnElement : columnElements)
            {
                columnText.add(columnElement.getText());
            }
        }

        return columnText;
    }

    public List<String> getColumnAsText(String col)
    {
        return getColumnAsText(getColumnIndex(col));
    }

    public List<String> getRowAsText(int row)
    {
        List<WebElement> cells = Locator.xpath("./td").findElements(elementCache().getRows().get(row));
        return getWrapper().getTexts(cells);
    }

    public List<WebElement> getColumnAsElement(int col)
    {
        int rowCount = getRowCount();
        List<WebElement> columnElements = new ArrayList<>();
        if (rowCount > 0)
        {
            for (int row = _bodyHeaderRowIndex + 1; row <= rowCount; row++)
            {
                columnElements.add(getDataAsElement(row, col));
            }
        }

        return columnElements;
    }

    public int getRowIndex(String columnLabel, String value)
    {
        return getRowIndex(getColumnIndex(columnLabel), value);
    }

    public int getRowIndex(int columnIndex, String value)
    {
        int rowCount = getRowCount();
        for (int i=0; i < rowCount; i++)
        {
            if (value.equals(getDataAsText(i, columnIndex)))
                return i;
        }
        return -1;
    }

    protected static String[] trimAll(String[] strings)
    {
        for (int i = 0; i < strings.length; i++)
            strings[i] = StringUtils.trim(strings[i]);
        return strings;
    }
}

