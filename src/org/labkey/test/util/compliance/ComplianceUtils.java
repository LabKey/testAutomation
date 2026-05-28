/*
 * Copyright (c) 2023-2026 LabKey Corporation
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
