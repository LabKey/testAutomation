package org.labkey.test.util.query;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.TruncateTableCommand;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.TestLogger;

import java.io.IOException;

public class QueryUtils
{
    private QueryUtils()
    {
        // Prevent instantiation
    }

    public static void truncateTable(String containerPath, String schema, String table) throws IOException, CommandException
    {
        Connection cn = WebTestHelper.getRemoteApiConnection();
        TruncateTableCommand cmd = new TruncateTableCommand(schema, table);
        cmd.execute(cn, containerPath);
    }

    public static void selectAndDeleteRows(String containerPath, String schema, String table) throws IOException, CommandException
    {
        Connection cn = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand(schema, table);
        SelectRowsResponse resp = cmd.execute(cn, containerPath);
        if (resp.getRowCount().intValue() > 0)
        {
            TestLogger.log("Deleting rows from " + schema + "." + table);
            DeleteRowsCommand delete = new DeleteRowsCommand(schema, table);
            resp.getRows().forEach(delete::addRow);
            delete.execute(cn, containerPath);
        }
    }

}
