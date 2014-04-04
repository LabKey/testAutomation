/*
 * Copyright (c) 2008-2014 LabKey Corporation
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

import java.util.*;

import static org.junit.Assert.*;

/**
 * <code>PipelineStatusTable</code>
*/
public class PipelineStatusTable extends DataRegionTable
{
    private boolean _cache;
    private int _rows = -1;
    private Map<String, String> _mapDescriptionStatus;

    public PipelineStatusTable(BaseWebDriverTest test, boolean selectors, boolean cache)
    {
        // 13760: PipelineTest fails because status table no longer has floating headers
        super("StatusFiles", test, selectors, false);

        _cache = cache;
    }

    public int getDataRowCount()
    {
        if (!_cache || _rows == -1)
            _rows = super.getDataRowCount();
        return _rows;
    }

    public int getStatusColumn()
    {
        return getColumn("Status");
    }

    public int getDescriptionColumn()
    {
        return getColumn("Description");
    }

    public boolean hasJob(String name)
    {
        return getJobStatus(name) != null;
    }

    public String getJobStatus(String name)
    {
        return getMapDescriptionStatus().get(name);
    }

    private Map<String, String> getMapDescriptionStatus()
    {
        if (!_cache || _mapDescriptionStatus == null)
        {
            int rows = getDataRowCount();
            int colStatus = getStatusColumn();
            int colDescripton = getDescriptionColumn();

            _mapDescriptionStatus = new LinkedHashMap<>();
            for (int i = 0; i < rows; i++)
            {
                _mapDescriptionStatus.put(getDataAsText(i, colDescripton),
                        getDataAsText(i, colStatus));
            }
        }

        return _mapDescriptionStatus;
    }

    public int getJobRow(String name)
    {
        int i = 0;
        for (String description : getMapDescriptionStatus().keySet())
        {
            if (name.equals(description))
                return i;
            i++;
        }

        return -1;
    }

    public String getJobDescription(int row)
    {
        return getDataAsText(row, getDescriptionColumn() + (_selectors ? 1 : 0));
    }

    public void clickStatusLink(String name)
    {
        clickStatusLink(getExpectedJobRow(name));
    }

    public void clickStatusLink(int row)
    {
        clickLink(row, getStatusColumn());
    }

    private int getExpectedJobRow(String name)
    {
        int row = getJobRow(name);
        assertTrue("Job not found " + name, row != -1);
        return row;
    }
}
