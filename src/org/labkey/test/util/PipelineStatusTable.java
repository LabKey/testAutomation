/*
 * Copyright (c) 2008-2016 LabKey Corporation
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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class PipelineStatusTable extends DataRegionTable
{
    private final Map<String, String> _mapDescriptionStatus;

    public PipelineStatusTable(BaseWebDriverTest test)
    {
        super("StatusFiles", test);
        _mapDescriptionStatus = new LinkedHashMap<>();
    }

    /**
     * @deprecated No longer need to specify caching. Retain for 16.2.3 feature branches
     */
    @Deprecated
    public PipelineStatusTable(BaseWebDriverTest test, boolean cache)
    {
        this(test);
    }

    @Override
    protected void clearCache()
    {
        super.clearCache();
        _mapDescriptionStatus.clear();
    }

    public int getStatusColumn()
    {
        return getColumnIndex("Status");
    }

    public int getDescriptionColumn()
    {
        return getColumnIndex("Description");
    }

    public boolean hasJob(String description)
    {
        return getJobStatus(description) != null;
    }

    public String getJobStatus(String description)
    {
        return getMapDescriptionStatus().get(description);
    }

    private Map<String, String> getMapDescriptionStatus()
    {
        if (_mapDescriptionStatus.isEmpty())
        {
            int rows = getDataRowCount();
            int colStatus = getStatusColumn();
            int colDescripton = getDescriptionColumn();

            for (int i = 0; i < rows; i++)
            {
                _mapDescriptionStatus.put(getDataAsText(i, colDescripton),
                        getDataAsText(i, colStatus));
            }
        }

        return _mapDescriptionStatus;
    }

    public int getJobRow(String description)
    {
        return getJobRow(description, false);
    }

    public int getJobRow(String description, boolean descriptionStartsWith)
    {
        int i = 0;
        for (String actualDescription : getMapDescriptionStatus().keySet())
        {
            if (description.equals(actualDescription) || (descriptionStartsWith && actualDescription.startsWith(description)))
                return i;
            i++;
        }

        return -1;
    }

    public String getJobDescription(int row)
    {
        return getDataAsText(row, getDescriptionColumn());
    }

    public void clickStatusLink(String description)
    {
        clickStatusLink(getExpectedJobRow(description));
    }

    public void clickStatusLink(int row)
    {
        _driver.clickAndWait(link(row, getStatusColumn()));
    }

    private int getExpectedJobRow(String description)
    {
        int row = getJobRow(description);
        assertTrue("Job not found " + description, row != -1);
        return row;
    }
}
