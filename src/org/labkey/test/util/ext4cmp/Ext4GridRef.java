/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
        return Locator.xpath("(" + ((Locator.XPathLocator) row).toXpath() + "//td[contains(@class, 'x4-grid-cell')])[" + cellIndex + "]").append(Locator.tagWithClass("div", "x4-grid-cell-inner"));
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

        // NOTE: sometimes this editor is picky about appearing
        // for now, solve this by repeating.  however, it would be better to resolve this issue.
        // one theory is that we need to shift focus prior to the doubleclick
        WebElement el = null;
        int i = 0;
        while (el == null)
        {
            el = startEditing(rowIdx, colName);
            assert i < 4 : "Unable to trigger editor after " + i + " attempts";
            i++;
        }

        Locator moreSpecific = Locator.id(el.getAttribute("id"));
        _test.setFormElement(moreSpecific, value);

        //if the editor is still displayed, try to close it
        if (el.isDisplayed())
        {
            completeEdit();
        }

        assertFalse("Grid input should not be visible", el.isDisplayed());
        waitForGridEditorToDisappear();
    }

    public void waitForGridEditor()
    {
        _test.waitFor(() ->  getActiveGridEditor() != null,
                "Unable to find element", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForGridEditorToDisappear()
    {
        _test.waitFor(() -> getActiveGridEditor() == null,
                "Unable to find element", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
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
        _test.waitFor(() -> getRowCount() == count,
                "Expected row count did not appear", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForSelected(final int count)
    {
        _test.waitFor(() -> getSelectedCount() == count,
                "Expected selected row count did not appear", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    public WebElement getActiveGridEditor()
    {
        //TODO: we need a more specific selector
        String selector = "div.x4-grid-editor input";

        List<WebElement> visible = new ArrayList<>();
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

    public Locator getTbarButton(String label)
    {
        return Locator.id(_id).append(Locator.tag("a").withClass("x4-toolbar-item")).append(Locator.tag("span").withText(label));
    }

    public void clickTbarButton(String label)
    {
        _test.waitAndClick(getTbarButton(label));
    }

    //uses 1-based coordinates
    public WebElement startEditing(int rowIdx, String colName)
    {
        int cellIdx = getIndexOfColumn(colName, true);  //NOTE: Ext 4.2.1 seems to not render hidden columns, unlike previous ext versions

        WebElement el = getActiveGridEditor();
        if (el == null)
        {
            Locator cell = Ext4GridRef.locateExt4GridCell(rowIdx, cellIdx, _id);
            // NOTE: this should ultimately get improved or removed.  there are intermittent
            // failures involving the cell not being found.  whenever i put breakpoints below,
            // the element does exist.  for now, just try twice, but this should get replaced with
            // something more reliable
            try
            {
                _test.waitForElement(cell);
            }
            catch (NoSuchElementException e)
            {
                _test.log("grid present: " + _test.isElementPresent(Locator.id(_id)));
                _test.log("row present: " + _test.isElementPresent(Ext4GridRef.locateExt4GridRow(rowIdx, _id)));
                _test.log("cell present: " + _test.isElementPresent(Ext4GridRef.locateExt4GridCell(rowIdx, cellIdx, _id)));
                _test.sleep(200);

                if (_test.isElementPresent(Ext4GridRef.locateExt4GridCell(rowIdx, cellIdx, _id)))
                {
                    _test.log("cell was present on second try");
                    //test.waitForElement(cell);
                }
                else
                {
                    throw e;
                }
            }

            _test.scrollIntoView(cell); // aligns to bottom
            _test.scrollBy(0, 100); // bumps up a few rows, above any footers or scrollbars
            if (_clicksToEdit > 1)
                _test.doubleClick(cell);
            else
                _test.click(cell);

            _test.sleep(200);
            el = getActiveGridEditor();
        }

        return el;
    }

    //1-based
    public WebElement startEditingJS(int rowIdx, String colName)
    {
        Integer colIdx = getIndexOfColumn(colName, false);
        completeEdit();

        Boolean didStart = (Boolean)getFnEval("return this.editingPlugin.startEdit(" + (rowIdx-1) + ", " + (colIdx-1) + ");");
        assertTrue("Unable to start grid edit", didStart);

        waitForGridEditor();

        return getActiveGridEditor();
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
