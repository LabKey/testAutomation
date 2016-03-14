package org.labkey.test.components;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.util.DataRegionTable;

public class CrosstabDataRegion extends DataRegionTable
{
    public CrosstabDataRegion(String regionName, BaseWebDriverTest test)
    {
        super(regionName, test);
    }

    @Override
    protected int getHeaderRowCount()
    {
        return super.getHeaderRowCount() + 2;
    }
}
