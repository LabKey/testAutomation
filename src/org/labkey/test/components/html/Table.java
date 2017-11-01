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
import java.util.List;

public class Table extends WebDriverComponent<Table.Elements>
{
    private final WebDriver _driver;
    private final WebElement _componentElement;

    public Table(WebDriver driver, WebElement componentElement)
    {
        _componentElement = componentElement;
        _driver = driver;
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

    protected class Elements extends Component.ElementCache
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

    public List<String> getColumnHeaders(int headerRow)
    {
        List<WebElement> headerEls = getColumnHeaderElements(headerRow);
        List<String> columnHeaders = new ArrayList<>();
        for(WebElement headerEl : headerEls){columnHeaders.add(headerEl.getText());}
        return columnHeaders;
    }

    public List<WebElement> getColumnHeaderElements(int headerRow)
    {
        return getComponentElement().findElements(By.xpath("./tbody/tr["+ headerRow +"]/*[(name()='TH' or name()='TD' or name()='th' or name()='td')]"));
    }

    public List<WebElement> getColumnHeaderElements()
    {
        return getColumnHeaderElements(1);
    }

    public List<WebElement> getColumnHeaderElementsByTag()
    {
        return getComponentElement().findElements(By.xpath(".//tr/th"));
    }

    public int getColumnIndex(String headerLabel, int headerIndex)
    {
        //List is zero based, locators that are going to depend on this are 1
        return getColumnHeaders(headerIndex).indexOf(headerLabel) + 1;
    }

    public int getColumnIndex(String headerLabel)
    {
        return getColumnIndex(headerLabel, 1);
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

    public String getDataAsText(String pk, String columnName)
    {
        int row = getRowIndex(pk);
        if (row == -1)
            return null;
        int col = getColumnIndex(columnName);
        if (col == -1)
            return null;
        return getDataAsText(row, col);
    }


    private WebElement _getDataAsElement(int row, int column)
    {
        return getComponentElement().findElement(By.xpath("./tbody/tr[" +row+ "]/td[" +column+ "]"));
    }

    public WebElement getDataAsElement(String pk, String columnName)
    {
        int row = getRowIndex(pk);
        if (row == -1)
            return null;
        int col = getColumnIndex(columnName);
        if (col == -1)
            return null;
        return _getDataAsElement(row, col);
    }

    public WebElement getDataAsElement(int row, int column)
    {
        return  _getDataAsElement(row, column);
    }

    public List<String> getColumnAsText(int col, int headerIndex)
    {
        List<WebElement> columnElements = getColumnAsElement(col, headerIndex);
        List<String> columnText = new ArrayList<>();

        if (columnElements.size() > 0)
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
        return getColumnAsText(getColumnIndex(col),1);
    }

    public List<String> getColumnAsText(String col, int colIndex)
    {
        return getColumnAsText(getColumnIndex(col, colIndex), colIndex);
    }

    public List<String> getRowAsText(int row)
    {
        final int colCount = getColumnCount(row);
        List<String> rowText = new ArrayList<>();

        for (int col = 1; col <= colCount; col++)
        {
            rowText.add(getDataAsText(row, col));
        }

        return rowText;
    }

    public List<WebElement> getColumnAsElement(String name, int columnIndex)
    {
        int col = getColumnIndex(name, columnIndex);
        return getColumnAsElement(col);
    }

    public List<WebElement> getColumnAsElement(String name)
    {
        return getColumnAsElement(name, 1);
    }

    public List<WebElement> getColumnAsElement(int col)
    {
        return getColumnAsElement(col,1);
    }

    public List<WebElement> getColumnAsElement(int col, int headerIndex)
    {
        int rowCount = getRowCount();
        List<WebElement> columnElements = new ArrayList<>();
        if (rowCount > 0)
        {
            for (int row = headerIndex + 1; row < rowCount; row++)
            {
                columnElements.add(getDataAsElement(row, col));
            }
        }

        return columnElements;
    }

    /** Find the row number for the given primary key. */
    public int getRowIndex(String pk)
    {
        for(int i = 0; i < getRowCount(); i++)
        {
            if(getRowAsText(i).contains(pk)){return i;}
        }
        return -1;
    }

    public int getRowIndex(String columnLabel, String value, int headerIndex)
    {
        return getRowIndex(getColumnIndex(columnLabel, headerIndex), value);
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

    public int getHeaderRowCount(String columnHeaderClass)
    {
        //return 2 + (_floatingHeaders ? 2 : 0);
        return getComponentElement().findElements(Locator.xpath("./tr[@class='"+columnHeaderClass+"']")).size();
    }

    public int getHeaderRowCount(int headerRowIndex)
    {
        return getComponentElement().findElements(Locator.xpath("./tr[" + headerRowIndex + "]")).size();
    }

    public int getHeaderRowCount()
    {
        return getHeaderRowCount(1);
    }

    public int getColumnCount(String columnHeaderClass)
    {
        return getComponentElement().findElements(Locator.xpath("./tbody/tr/td")).size();
    }

    public int getColumnCount(int columnIndex)
    {
        return getComponentElement().findElements(Locator.xpath("./tbody/tr["+ columnIndex +"]/td")).size();
    }

    protected static String[] trimAll(String[] strings)
    {
        for (int i = 0; i < strings.length; i++)
            strings[i] = StringUtils.trim(strings[i]);
        return strings;
    }
}

