/*
 * Copyright (c) 2008-2017 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class ExperimentRunTable extends DataRegionTable
{
    private boolean _cache;
    private int _rows = -1;
    private List<String> _listNames;

    public ExperimentRunTable(String tableName, BaseWebDriverTest test, boolean cache)
    {
        super(tableName, test);

        _cache = cache;
    }

    public int getDataRowCount()
    {
        if (!_cache || _rows == -1)
            _rows = super.getDataRowCount();
        return _rows;
    }

    public int getNameColumn()
    {
        return getColumnIndex("Name");
    }

    public boolean hasRun(String name)
    {
        return getRunRow(name) != -1;
    }

    public int getRunRow(String name)
    {
        if (!_cache || _listNames == null)
        {
            int rows = getDataRowCount();
            int colName = getNameColumn();

            _listNames = new ArrayList<>();
            for (int i = 0; i < rows; i++)
                _listNames.add(getDataAsText(i, colName));
        }
        
        for (int i = 0; i < _listNames.size(); i++)
        {
            String s = _listNames.get(i);
            if (s != null && s.indexOf(name) != -1)
                return i;
        }

        return -1;
    }

    public String getRunName(int row)
    {
        return getDataAsText(row, getNameColumn());
    }

    public String getRunName(String name)
    {
        int row = getRunRow(name);
        if (row == -1)
            return null;

        return getRunName(row);
    }

    public void clickRunLink(String name)
    {
        clickRunLink(getExpectedRunRow(name));
    }

    public void clickRunLink(int row)
    {
        getWrapper().clickAndWait(Locator.linkWithText(getRunName(row)));
    }

    public void clickGraphLink(String name)
    {
        clickGraphLink(getExpectedRunRow(name));
    }

    public void clickGraphLink(int row)
    {
        // todo: not hardcoded!
        getWrapper().clickAndWait(link(row, 2));
    }

    private int getExpectedRunRow(String name)
    {
        int row = getRunRow(name);
        assertTrue("Run not found " + name, row != -1);
        return row;
    }
}
