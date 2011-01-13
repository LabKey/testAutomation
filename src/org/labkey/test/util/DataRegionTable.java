/*
 * Copyright (c) 2007-2011 LabKey Corporation
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
package org.labkey.test.util;

import junit.framework.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.apache.commons.lang.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * DataRegionTable class
 * <p/>
 * Created: Feb 19, 2007
 *
 * @author bmaclean
 */
public class DataRegionTable
{
    protected String _tableName;
    protected BaseSeleniumWebTest _test;
    protected boolean _selectors;
    protected Map<String, Integer> _mapColumns = new HashMap<String, Integer>();
    protected Map<String, Integer> _mapRows = new HashMap<String, Integer>();

    public DataRegionTable(String tableName, BaseSeleniumWebTest test)
    {
        this(tableName, test, true);

        // Already being created at the beginning of tests before tables are on the page.
        // _test.assertElementPresent(Locator.xpath("//table[@id='" + getHtmlName() + "']"));
    }

    public DataRegionTable(String tableName, BaseSeleniumWebTest test, boolean selectors)
    {
        _tableName = tableName;
        _selectors = selectors;
        reload(test);
    }

    public String getTableName()
    {
        return _tableName;
    }

    public String getHtmlName()
    {
        return "dataregion_" + _tableName;
    }

    public void reload(BaseSeleniumWebTest test)
    {
        _test = test;
    }

    private String getJSParam(String func, int i, boolean bStr)
    {
        int lParen = func.indexOf("(");
        int rParen = func.indexOf(")");
        if (lParen == -1 || lParen > rParen)
            return null;

        String[] params = func.substring(lParen + 1, rParen).split(",");
        String param = params[i].trim();
        if (!bStr)
            return param;

        if ((param.charAt(0) != '"' || param.charAt(param.length() - 1) != '"') &&
                (param.charAt(0) != '\'' || param.charAt(param.length() - 1) != '\''))
            return null;

        return param.substring(1, param.length() - 1);
    }

    public int getDataRowCount()
    {
        return getDataRowCount(1);
    }
    
    public int getDataRowCount(int div)
    {
        int rows = 0;
        try
        {
            while (getDataAsText(rows, 0) != null)
                rows += div;
        }
        catch (Exception e)
        {
            // Throws an exception, if row is out of bounds.
        }

        if (rows == 1 && "No data to show.".equals(getDataAsText(0, 0)))
            rows = 0;

        return rows;
    }

    public Locator.XPathLocator xpath(int row, int col)
    {
        return Locator.xpath("//table[@id='" + getHtmlName() + "']/tbody/tr[" + (row+3) + "]/td[" + (col + 1 + (_selectors ? 1 : 0)) + "]");
    }

    public void clickLink(int row, int col)
    {
        Locator.XPathLocator cell = xpath(row, col);
        _test.clickAndWait(cell.child("a[1]"));
    }

    public int getColumn(String name)
    {
        name = name.replaceAll(" ", "");
        Integer colIndex = _mapColumns.get(name);
        if (colIndex != null)
            return colIndex.intValue();
        
        try
        {
            int sel = (_selectors ? 1 : 0);
            for (int col = 0; getDataAsText(0, col) != null; col++)
            {
                String header = _test.getText(Locator.xpath("//table[@id='" + getHtmlName() + "']/tbody/tr[2]/td[" + (col+sel+1) + "]/div"));
                String headerName = header.split("\n")[0];
                headerName = headerName.replaceAll(" ", "");
                if (!StringUtils.isEmpty(headerName))
                    _mapColumns.put(headerName, col);
                if (headerName.equals(name))
                {
                    return col;
                }
            }
            _test.log("Column '" + name + "' not found");
        }
        catch (Exception e)
        {            
            // _test.log("Failed to get column named " + name);
        }

        return -1;
    }

    /** Find the row number for the given primary key. */
    public int getRow(String pk)
    {
        Assert.assertTrue("Need the selector checkbox's value to find the row with the given pk", _selectors);

        Integer cached = _mapRows.get(pk);
        if (cached != null)
            return cached.intValue();

        int row = 0;
        try
        {
            while (true)
            {
                String value = _test.getAttribute(Locator.xpath("//table[@id='dataregion_query']//tr[" + (row+3) + "]//input[@name='.select']/"), "value");
                _mapRows.put(value, row);
                if (value.equals(pk))
                    return row;
                row += 1;
            }
        }
        catch (Exception e)
        {
            // Throws an exception, if row is out of bounds.
        }

        return -1;
    }

    public String getDataAsText(int row, int column)
    {
        String ret = null;
        try
        {
            ret = _test.getTableCellText(getHtmlName(), row + 2, column + (_selectors ? 1 : 0));
        } catch(Exception ignore) {}
        return ret;
    }

    public String getDataAsText(int row, String columnName)
    {
        int col = getColumn(columnName);
        if (col == -1)
            return null;
        return getDataAsText(row, col);
    }

    public String getDataAsText(String pk, String columnName)
    {
        int row = getRow(pk);
        if (row == -1)
            return null;
        int col = getColumn(columnName);
        if (col == -1)
            return null;
        return getDataAsText(row, col);
    }

    public void setSort(String columnName, SortDirection direction)
    {
        _test.setSort(_tableName, columnName, direction);
    }

    public void setFilter(String columnName, String filterType, String filter)
    {
        _test.setFilter(_tableName, columnName, filterType, filter);
    }

    public void clearFilter(String columnName)
    {
        _test.clearFilter(_tableName, columnName);
    }

    public void clearAllFilters(String columnName)
    {
        _test.clearAllFilters(_tableName, columnName);
    }

    public void checkAllOnPage()
    {
        _test.checkAllOnPage(_tableName);
    }

    public void uncheckAllOnPage()
    {
        _test.uncheckAllOnPage(_tableName);
    }

    public void checkAll()
    {
        throw new UnsupportedOperationException("Not Yet Implemented");
    }

    public void uncheckAll()
    {
        throw new UnsupportedOperationException("Not Yet Implemented");
    }

    public void checkCheckbox(String value)
    {
        _test.checkDataRegionCheckbox(_tableName, value);
    }

    public void checkCheckbox(int index)
    {
        _test.checkDataRegionCheckbox(_tableName, index);
    }

    public void uncheckCheckbox(int index)
    {
        _test.uncheckDataRegionCheckbox(_tableName, index);
    }

    public void pageFirst()
    {
        _test.dataRegionPageFirst(_tableName);
    }

    public void pageLast()
    {
        _test.dataRegionPageLast(_tableName);
    }

    public void pageNext()
    {
        _test.dataRegionPageNext(_tableName);
    }

    public void pagePrev()
    {
        _test.dataRegionPagePrev(_tableName);
    }

    public void showAll()
    {
        _test.clickLinkWithText("Show All");
    }

    public void setPageSize(int size)
    {
        _test.clickLinkWithText(size + " per page");
    }

    /**
     * Set the current offset by manipulating the url rather than using the pagination buttons.
     * @param offset
     */
    public void setOffset(int offset)
    {
        String url = replaceParameter(_tableName + ".offset", String.valueOf(offset));
        _test.beginAt(url);
    }

    /**
     * Set the page size by manipulating the url rather than using the "XXX per page" menu items.
     * @param size new page size
     */
    public void setMaxRows(int size)
    {
        String url = replaceParameter(_tableName + ".maxRows", String.valueOf(size));
        _test.beginAt(url);
    }

    private String replaceParameter(String param, String newValue)
    {
        URL url = _test.getURL();
        String file = url.getFile();
        file = file.replaceAll("&" + param + "=\\p{Alnum}+?", "");
        if (newValue != null)
            file += "&" + param + "=" + newValue;

        try
        {
            url = new URL(url.getProtocol(), url.getHost(), url.getPort(), file);
        }
        catch (MalformedURLException mue)
        {
            throw new RuntimeException(mue);
        }
        return url.getFile();
    }
}
