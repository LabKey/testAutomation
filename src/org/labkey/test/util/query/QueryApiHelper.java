package org.labkey.test.util.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.domain.DomainDetailsResponse;
import org.labkey.remoteapi.domain.DropDomainCommand;
import org.labkey.remoteapi.domain.GetDomainDetailsCommand;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.remoteapi.query.TruncateTableCommand;
import org.labkey.remoteapi.query.TruncateTableResponse;
import org.labkey.remoteapi.query.UpdateRowsCommand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QueryApiHelper
{
    private final Connection _connection;
    private final String _containerPath;
    private final String _schema;
    private final String _query;

    private int _insertTimout = 180_000;

    public QueryApiHelper(Connection connection, String containerPath, String schema, String query)
    {
        _connection = connection;
        _containerPath = containerPath;
        _schema = schema;
        _query = query;
    }

    public QueryApiHelper setInsertTimout(int insertTimout)
    {
        _insertTimout = insertTimout;
        return this;
    }

    public SelectRowsResponse selectRows() throws IOException, CommandException
    {
        return selectRows(null, null, null);
    }

    public SelectRowsResponse selectRows(List<String> columns) throws IOException, CommandException
    {
        return selectRows(columns, null, null);
    }

    public SelectRowsResponse selectRows(List<String> columns, @Nullable List<Filter> filters) throws IOException, CommandException
    {
        return selectRows(columns, filters, null);
    }

    public SelectRowsResponse selectRows(List<String> columns, @Nullable List<Filter> filters, @Nullable List<Sort> sorts) throws IOException, CommandException
    {
        SelectRowsCommand cmd = new SelectRowsCommand(_schema, _query);

        if(filters != null)
        {
            cmd.setFilters(new ArrayList<>(filters));
        }

        if(sorts != null)
        {
            cmd.setSorts(sorts);
        }

        if (columns!=null)
            cmd.setColumns(columns);

        return cmd.execute(_connection, _containerPath);
    }

    public SaveRowsResponse insertRows(List<Map<String, Object>> rows) throws IOException, CommandException
    {
        InsertRowsCommand insertRowsCommand = new InsertRowsCommand(_schema, _query);
        insertRowsCommand.setRows(rows);
        insertRowsCommand.setTimeout(_insertTimout);
        return insertRowsCommand.execute(_connection, _containerPath);
    }

    public SaveRowsResponse updateRows(List<Map<String, Object>> rows) throws IOException, CommandException
    {
        UpdateRowsCommand updateRowsCommand = new UpdateRowsCommand(_schema, _query);
        updateRowsCommand.setRows(rows);
        updateRowsCommand.setTimeout(_insertTimout);
        return  updateRowsCommand.execute(_connection, _containerPath);
    }

    /**
     * @param rowsToDelete Should include primary key(s) for the table
     * @return a list of the rows that were deleted
     */
    public SaveRowsResponse deleteRows(List<Map<String,Object>> rowsToDelete) throws IOException, CommandException
    {
        DeleteRowsCommand cmd = new DeleteRowsCommand(_schema, _query);
        cmd.setRows(rowsToDelete);
        return cmd.execute(_connection, _containerPath);
    }

    /**
     * Delete all rows in table
     * @return response object
     */
    public TruncateTableResponse truncateTable() throws IOException, CommandException
    {
        TruncateTableCommand truncateCommand = new TruncateTableCommand(_schema, _query);
        return truncateCommand.execute(_connection, _containerPath);
    }

    public DomainDetailsResponse getDomainDetails() throws IOException, CommandException
    {
        GetDomainDetailsCommand cmd = new GetDomainDetailsCommand(_schema, _query);
        return cmd.execute(_connection, _containerPath);
    }

    public CommandResponse deleteDomain() throws IOException, CommandException
    {
        DropDomainCommand delCmd = new DropDomainCommand(_schema, _query);
        return delCmd.execute(_connection, _containerPath);
    }

}
