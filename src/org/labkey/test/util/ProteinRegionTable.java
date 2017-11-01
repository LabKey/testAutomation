/*
 * Copyright (c) 2007-2017 LabKey Corporation
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
import org.labkey.test.SortDirection;

public class ProteinRegionTable extends DataRegionTable
{
    protected BaseWebDriverTest _test;
    protected double _maxProbability;
    protected int _pages;
    protected int _maxGroup;
    protected int _maxSubGroup;

    public ProteinRegionTable(double maxProbability, BaseWebDriverTest test)
    {
        super("ProteinGroupsWithQuantitation", test);

        _test = test;
        _maxProbability = maxProbability;

        // Give this a little extra time, since we have timed out here.
        int wait = _test.getDefaultWaitForPage();
        _test.setDefaultWaitForPage(wait * 2);
        setFilter("GroupProbability", "Is Greater Than or Equal To", Double.toString(_maxProbability));
        setSort("GroupNumber", SortDirection.ASC);
        _test.setDefaultWaitForPage(wait);
    }

    private int getDataRowCount(int div)
    {
        int rows = 0;
        while (getDataAsText(rows, 0) != null)
            rows += div;

        if (rows == 1 && "No data to show.".equals(getDataAsText(0, 0)))
            rows = 0;

        return rows;
    }

    public int getProtCount()
    {
        return getDataRowCount(2) / 2;
    }

    public int getProtRow(int prot)
    {
        return prot * 2;
    }

    public void resetPages()
    {
        _pages = 0;
    }

    public int getPages()
    {
        return _pages;
    }

    public boolean nextPage()
    {
        if (_pages == 0)
        {
            _pages = 1;
            _maxGroup = 0;
            _maxSubGroup = 0;
            return true;
        }
        if (getProtCount() == 0)
        {
            if (_maxSubGroup == 0)
                return false;
            _maxSubGroup = 0;
        }
        else
        {
            String group = getDataAsText(getProtRow(getProtCount() - 1), getColumnIndex("Group"));
            if (group.contains("-"))
            {
                String[] parts = group.split("-");
                _maxSubGroup = Integer.parseInt(parts[1]);
                group = parts[0];
            }
            _maxGroup = Integer.parseInt(group);

            if (!_test.isTextPresent("Displaying only the first 1000 rows"))
            {
                return false;
            }
        }

        if (_maxSubGroup == 0)
        {
            setFilter("GroupNumber", "Is Greater Than", Integer.toString(_maxGroup));
            setFilter("IndistinguishableCollectionId", "Is Not Blank", null);
        }
        else
        {
            setFilter("GroupNumber", "Is Equal To", Integer.toString(_maxGroup));
            setFilter("IndistinguishableCollectionId", "Is Greater Than",
                    Integer.toString(_maxSubGroup));
        }
        _pages++;

        return true;
    }
}
