/*
 * Copyright (c) 2012 LabKey Corporation
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
package org.labkey.test.util.ext4cmp;

import junit.framework.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * User: bimber
 * Date: 11/7/12
 * Time: 8:48 PM
 */
public class Ext4GridRefWD extends Ext4CmpRefWD
{
    public Ext4GridRefWD(String id, BaseWebDriverTest test)
    {
        super(id, test);
    }


    public static Locator locateExt4GridRow(int rowIndex, String parentId)
    {
        String base = "//table[contains(@class, 'x4-grid-table')]";

        if(parentId != null)
            base = "//*[@id='" + parentId + "']" + base;

        return Locator.xpath("(" + base + "//tr[contains(@class, 'x4-grid-row')])[" + rowIndex + "]");
    }

    public Locator getRow(int rowIndex)
    {
        return Ext4GridRefWD.locateExt4GridRow(rowIndex, _id);
    }

    public static Locator locateExt4GridCell(int rowIdx, int cellIndex, String parentId)
    {
        Locator row = Ext4GridRefWD.locateExt4GridRow(rowIdx, parentId);
        return Locator.xpath("(" + ((Locator.XPathLocator) row).getPath() + "//td[contains(@class, 'x4-grid-cell')])[" + cellIndex + "]");
    }

    public Object getFieldValue(int rowIdx, String fieldName)
    {
        String recordId = getRecordId(rowIdx);
        return getEval("store.data.get('" + recordId + "').get('" + fieldName + "')");
    }

    public static Locator locateExt4GridCell(String contents)
    {
        return Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner') and text() = '" + contents + "']");
    }

    //uses 1-based coordinates
    public void setGridCellJS(int rowIdx, String columnName, Object value)
    {
        String recordId = getRecordId(rowIdx);
        getEval("store.data.get('" + recordId + "').set('" + columnName + "', arguments[0])", value);
    }

    //uses 1-based coordinates
    public void setGridCellJS(int rowIdx, int cellIdx, Object value)
    {
        String dataIndex = (String)getEval("columns[" + cellIdx + "].dataIndex");
        setGridCellJS(rowIdx, dataIndex, value);
    }

    //does Ext really not have a cleaner API for this?  i dont think we can rely on the grid row index being the same as the store index
    //rowIdx is 1-based for consistency with other methods
    public String getRecordId(int rowIdx)
    {
        rowIdx = rowIdx - 1;
        String script = "return this.getView().getNodes(" + rowIdx + "," + rowIdx + ")[0].viewRecordId;";
        return (String)getFnEval(script);
    }

    //uses 1-based coordinates
    public void setGridCell(int rowIdx, String colName, String value)
    {
        Integer cellIdx = getIndexOfColumn(colName);
        setGridCell(rowIdx, cellIdx, value);
    }

    //1-based result for consistency w/ other methods
    public int getIndexOfColumn(String column)
    {
        return getIndexOfColumn(column, "name");
    }

    //1-based result for consistency w/ other methods
    public int getIndexOfColumn(String value, String propName)
    {
        Long idx = (Long)getFnEval("for (var i=0;i<this.columns.length;i++){if (this.columns[i]['"+propName+"'] == '" + value + "') return i;}; return -1");
        Assert.assertTrue("Unable to find column where property: " + propName + " has value: " + value, idx >= 0);
        return idx.intValue() > -1 ? idx.intValue() + 1 : -1;
    }

    //uses 1-based coordinates
    public void setGridCell(int rowIdx, int cellIdx, String value)
    {
        Locator cell = Ext4GridRefWD.locateExt4GridCell(rowIdx, cellIdx, _id);
        _test.doubleClick(cell);
        _test.sleep(200);

        WebElement el = getActiveGridEditor();
        if (el == null)
        {
            _test.doubleClick(cell);
            _test.sleep(200);
            el = getActiveGridEditor();
        }

        waitForGridEditor();

        Locator moreSpecific = Locator.id(el.getAttribute("id"));
        _test.setFormElement(moreSpecific, value);

        //if the editor is still displayed, try to close it
        if (el.isDisplayed())
        {
            //this is an alternate method to complete the edit, instead of calling Ext
            //el.sendKeys(Keys.TAB);
            //_test.getDriver().switchTo().activeElement().sendKeys(Keys.ESCAPE);

            eval("editingPlugin.completeEdit()");
            _test.sleep(100);
        }

        Assert.assertFalse("Grid input should not be visible", el.isDisplayed());
        _test.sleep(300);
    }

    private void waitForGridEditor()
    {
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return getActiveGridEditor() != null;

            }
        }, "Unable to find element", _test.WAIT_FOR_JAVASCRIPT);
    }

    private WebElement getActiveGridEditor()
    {
        //TODO: we need a more specific selector
        String selector = "div.x4-grid-editor input";
        _test.waitForElement(Locator.css(selector));

        List<WebElement> visible = new ArrayList<WebElement>();
        for (WebElement element : _test.getDriver().findElements(By.cssSelector(selector)))
        {
            if (element.isDisplayed())
            {
                visible.add(element);
            }
        }

        if (visible.size() > 1)
        {
            throw new RuntimeException("Incorrect number of grid cells found: " + visible.size());
        }

        return visible.size() == 1 ? visible.get(0) : null;
    }
}
