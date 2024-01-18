package org.labkey.test.util.compliance;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.pages.query.ExecuteQueryPage;
import org.labkey.test.util.DataRegionTable;

public final class ComplianceUtils
{
    private ComplianceUtils() { /* Do not instantiate */ }

    public static DataRegionTable viewQueryAuditEvents(WebDriverWrapper wdw, String containerPath)
    {
        return ExecuteQueryPage.beginAt(wdw, containerPath, "auditLog", "SelectQuery").getDataRegion();
    }

    public enum PhiColumnBehavior
    {
        SHOW,
        HIDE,
        BLANK
    }

    public enum QueryLoggingBehavior
    {
        NONE,
        PHI,
        ALL
    }
}
