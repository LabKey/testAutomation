/*
 * Copyright (c) 2014-2019 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Ext4GridRef extends Ext4CmpRef
{
    private int _clicksToEdit = 2;

    public Ext4GridRef(String id, WebDriverWrapper test)
    {
        super(id, test);
    }

    public void setClicksToEdit(int clicksToEdit)
    {
        _clicksToEdit = clicksToEdit;
    }

    public static Locator locateExt4GridRow(int rowIndex, String parentId)
    {
        String base = "//table[contains(@class, 'x4-grid-table')]";

        if(parentId != null)
            base = "//*[@id='" + parentId + "']" + base;

        return Locator.xpath("(" + base + "//tr[contains(@class, 'x4-grid-data-row')])[" + rowIndex + "]");
    }

    public Locator getRow(int rowIndex)
    {
        return Ext4GridRef.locateExt4GridRow(rowIndex, _id);
    }

    public Locator getCell(int rowIndex, String colName)
    {
        int cellIdx = getIndexOfColumn(colName, true);  //NOTE: Ext 4.2.1 seems to not render hidden columns, unlike previous ext versions
        return Ext4GridRef.locateExt4GridCell(rowIndex, cellIdx, _id);
    }

    public Locator getCell(int rowIndex, int colIndex)
    {
        return Ext4GridRef.locateExt4GridCell(rowIndex, colIndex, _id);
    }

    public static Locator locateExt4GridCell(int rowIdx, int cellIndex, String parentId)
    {
        Locator row = Ext4GridRef.locateExt4GridRow(rowIdx, parentId);
        return Locator.xpath("(" + ((Locator.XPathLocator) row).toXpath() + "//td[contains(@class, 'x4-grid-cell')])[" + cellIndex + "]");
    }

    //1-based rowIdx
    public Object getFieldValue(int rowIdx, String fieldName)
    {
        rowIdx--;
        return getEval("store.getAt('" + rowIdx + "').get('" + fieldName + "')");
    }

    //1-based rowIdx
    public Date getDateFieldValue(int rowIdx, String fieldName)
    {
        try
        {
            rowIdx--;
            String val = (String)getFnEval("return this.store.getAt('" + rowIdx + "').get('" + fieldName + "') ? Ext4.Date.format(this.store.getAt('" + rowIdx + "').get('" + fieldName + "'), 'c') : null");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
            return val == null ? null : dateFormat.parse(val);
        }
        catch (ParseException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static Locator locateExt4GridCell(String contents)
    {
        return Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner') and text() = '" + contents + "']");
    }

    //uses 1-based coordinates
    public void setGridCellJS(int rowIdx, String columnName, Object value)
    {
        rowIdx--;
        getEval("store.getAt('" + rowIdx + "').set('" + columnName + "', arguments[0])", value);
    }

    public boolean isColumnPresent(String colName, boolean visibleOnly)
    {
        return getIndexOfColumn(colName, visibleOnly, false) > -1;
    }

    //1-based result for consistency w/ other methods
    public int getIndexOfColumn(String column, boolean visibleOnly)
    {
        return getIndexOfColumn(column, visibleOnly, true);
    }

    protected int getIndexOfColumn(String column, boolean visibleOnly, boolean assertPresent)
    {
        int idx = getIndexOfColumn(column, "name", visibleOnly);
        if (idx == -1)
            idx = getIndexOfColumn(column, "dataIndex", visibleOnly);

        if (assertPresent)
            assertTrue("Unable to find column where either name or dataIndex has value: " + column, idx >= 0);

        return idx;
    }

    //1-based result for consistency w/ other methods
    protected int getIndexOfColumn(String value, String propName, boolean visibleOnly)
    {
        Long idx = (Long)getFnEval("for (var i=0;i<this.columns.length;i++){if (this.columns[i]['"+propName+"'] == '" + value + "') return " + (visibleOnly ? "this.columns[i].getVisibleIndex()" : "i") + ";}; return -1");

        return idx.intValue() > -1 ? idx.intValue() + 1 : -1;
    }

    //uses 1-based coordinates
    @LogMethod
    public void setGridCell(@LoggedParam int rowIdx, @LoggedParam String colName, @LoggedParam String value)
    {
        completeEdit();

        WebElement el = startEditing(rowIdx, colName);

        _test.setFormElementJS(el, "");
        el.sendKeys(value);
        _test.sleep(1000);
        Ext4Helper.Locators.comboListItem().withText(value + " " + Locator.NBSP)
                .findOptionalElement(_test.getDriver())
                .ifPresent(WebElement::click);

        //if the editor is still displayed, try to close it
        if (el.isDisplayed())
        {
            completeEdit();
        }

        assertFalse("Grid input should not be visible", el.isDisplayed());
        waitForGridEditorToDisappear();
    }

    public  void clickArrowOnGrid(@LoggedParam int rowIdx, @LoggedParam String colName)
    {
        WebElement el = startEditing(rowIdx, colName);
        Locator.xpath("../../td").withClass("x4-trigger-cell").findElement(el).click();
    }

    public void clickDownArrowOnGrid(@LoggedParam int rowIdx, @LoggedParam String colName)
    {
        WebElement el = startEditing(rowIdx, colName);
        Locator.xpath("../../td/div").withClass("x4-form-arrow-trigger").findElement(el).click();
    }

    public void waitForGridEditorToDisappear()
    {
        WebDriverWrapper.waitFor(() -> getActiveGridEditor() == null,
                "Grid editor did not disappear", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    public int getRowCount()
    {
        return ((Long)getEval("store.getCount()")).intValue();
    }

    public int getSelectedCount()
    {
        return ((Long)getEval("getSelectionModel().getSelection().length;")).intValue();
    }

    public void waitForRowCount(final int count)
    {
        WebDriverWrapper.waitFor(() -> getRowCount() == count,
                "Expected row count did not appear", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForSelected(final int count)
    {
        WebDriverWrapper.waitFor(() -> getSelectedCount() == count,
                "Expected selected row count did not appear", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    public WebElement getActiveGridEditor()
    {
        Locator.XPathLocator activeEditorLocator = Locator.id(_id).append(Locator.tagWithClass("input", "x4-form-focus")).notHidden();
        return activeEditorLocator.findElementOrNull(_test.getDriver());
    }

    public WebElement waitForActiveGridEditor(int timeout)
    {
        WebDriverWrapper.waitFor(() -> getActiveGridEditor() != null, timeout);
        return getActiveGridEditor();
    }

    public Locator getTbarButton(String label)
    {
        return Locator.id(_id).append(Locator.tag("a").withClass("x4-toolbar-item")).append(Locator.tag("span").withText(label));
    }

    public void clickTbarButton(String label)
    {
        _test.waitAndClick(getTbarButton(label));
    }

    public WebElement startEditing(int rowIdx, String colName)
    {
        // NOTE: sometimes this editor is picky about appearing
        // for now, solve this by repeating.  however, it would be better to resolve this issue.
        // one theory is that we need to shift focus prior to the doubleclick
        WebElement el = null;
        final int maxRetries = 4;
        for (int i = 1; el == null && i <= maxRetries; i++)
        {
            try
            {
                el = tryStartEditing(rowIdx, colName);
            }
            catch (WebDriverException retry)
            {
                if (i == maxRetries)
                    throw retry;
            }
        }
        Assert.assertNotNull("Unable to trigger editor after " + maxRetries + " attempts", el);

        return el;
    }

    //uses 1-based coordinates
    private WebElement tryStartEditing(int rowIdx, String colName)
    {
        int cellIdx = getIndexOfColumn(colName, true);  //NOTE: Ext 4.2.1 seems to not render hidden columns, unlike previous ext versions

        WebElement el = getActiveGridEditor();
        if (el == null)
        {
            WebElement cell;
            WebElement grid = null;
            Locator cellLoc = Ext4GridRef.locateExt4GridCell(rowIdx, cellIdx, _id);
            // NOTE: this should ultimately get improved or removed.  there are intermittent
            // failures involving the cell not being found.  whenever i put breakpoints below,
            // the element does exist.  for now, just try twice, but this should get replaced with
            // something more reliable
            try
            {
                grid = Locator.id(_id).findElement(_test.getDriver());
                cell = cellLoc.waitForElement(grid, 10000);
            }
            catch (NoSuchElementException e)
            {
                _test.log("grid present: " + (grid != null));
                _test.log("row present: " + _test.isElementPresent(Ext4GridRef.locateExt4GridRow(rowIdx, _id)));
                _test.sleep(300);

                grid = Locator.id(_id).findElement(_test.getDriver());
                cell = cellLoc.findElementOrNull(grid);
                if (cell != null)
                {
                    _test.log("cell was present on second try");
                }
                else
                {
                    throw e;
                }
            }

            _test.scrollIntoView(cell,true);
            new Actions(_test.getDriver()).moveToElement(cell).build().perform();
            if (_clicksToEdit > 1)
                _test.doubleClick(cell);
            else
                cell.click();

            el = waitForActiveGridEditor(1000);
        }

        return el;
    }

    public void cancelEdit()
    {
        getFnEval("this.editingPlugin.cancelEdit();");
        waitForGridEditorToDisappear();
    }

    public void completeEdit()
    {
        getFnEval("this.editingPlugin.completeEdit();");
        waitForGridEditorToDisappear();

        // note: after we completeEdit(), the grid may refresh itself.  this is generally a non-issue for people clicking,
        // but the tests sometimes run into problems.  this should allow time between editing cells
        _test.sleep(200);
    }

    public Ext4FieldRef getActiveEditor(int rowIdx, String colName)
    {
        startEditing(rowIdx, colName);
        String fieldId = (String)getFnEval("return this.editingPlugin.getActiveEditor().items.getAt(0).id;");

        return new Ext4FieldRef(fieldId, _test);
    }
}
