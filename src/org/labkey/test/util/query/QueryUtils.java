/*
 * Copyright (c) 2021-2026 LabKey Corporation
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
package org.labkey.test.util.query;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.TruncateTableCommand;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.TestLogger;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class QueryUtils
{
    private QueryUtils()
    {
        // Prevent instantiation
    }

    /**
     * Convenience method for deleting rows via `TruncateTableCommand`
     */
    @LogMethod
    public static void truncateTable(@LoggedParam String containerPath, @LoggedParam String schema, @LoggedParam String table)
            throws IOException, CommandException
    {
        Connection cn = WebTestHelper.getRemoteApiConnection();
        TruncateTableCommand cmd = new TruncateTableCommand(schema, table);
        cmd.execute(cn, containerPath);
    }

    /**
     * Delete all rows in the specified table using `DeleteRowsCommand`
     * {@link #truncateTable} is preferable but is not supported by all tables
     */
    public static void selectAndDeleteAllRows(String containerPath, String schema, String table)
            throws IOException, CommandException
    {
        Connection cn = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand(schema, table);
        SelectRowsResponse resp = cmd.execute(cn, containerPath);
        final List<Map<String, Object>> rows = resp.getRows();
        if (!rows.isEmpty())
        {
            TestLogger.log(String.format("Deleting %d rows from %s.%s in %s", rows.size(), schema, table, containerPath));
            DeleteRowsCommand delete = new DeleteRowsCommand(schema, table);
            rows.forEach(delete::addRow);
            delete.execute(cn, containerPath);
        }
    }

}
