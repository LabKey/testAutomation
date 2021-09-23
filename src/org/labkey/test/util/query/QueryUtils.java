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
