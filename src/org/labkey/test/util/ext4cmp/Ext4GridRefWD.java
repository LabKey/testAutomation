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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

/**
 * Created with IntelliJ IDEA.
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

    public static Locator locateExt4GridCell(int rowIdx, int cellIndex, String parentId)
    {
        Locator row = Ext4GridRefWD.locateExt4GridRow(rowIdx, parentId);
        return Locator.xpath("(" + ((Locator.XPathLocator)row).getPath() + "//td[contains(@class, 'x4-grid-cell')])[" + cellIndex + "]");
    }

    public static Locator locateExt4GridCell(String contents)
    {
        return Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner') and text() = '" + contents + "']");
    }

    public void setGridCell(int rowIdx, int cellIdx, String value)
    {
        Locator cell = Ext4GridRefWD.locateExt4GridCell(rowIdx, cellIdx, _id);
        _test.doubleClick(cell);
        Locator input = Locator.css("div.x4-grid-editor input");
        _test.setFormElement(input, value);

        //shift focus to commit changes
        input.findElement(_test.getDriver()).sendKeys("\t");
        _test.sleep(100);

        _test.click(Locator.css("body"));
        _test.sleep(100);
    }
}
