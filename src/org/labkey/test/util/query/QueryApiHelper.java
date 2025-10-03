package org.labkey.test.util.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.domain.DomainDetailsResponse;
import org.labkey.remoteapi.domain.DropDomainCommand;
import org.labkey.remoteapi.domain.GetDomainDetailsCommand;
import org.labkey.remoteapi.query.BaseRowsCommand;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.ImportDataCommand;
import org.labkey.remoteapi.query.ImportDataResponse;
import org.labkey.remoteapi.query.ImportExperimentDataCommand;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.MoveRowsCommand;
import org.labkey.remoteapi.query.RowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.remoteapi.query.TruncateTableCommand;
import org.labkey.remoteapi.query.TruncateTableResponse;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.test.util.AuditLogHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryApiHelper
{
    private final Connection _connection;
    private final String _containerPath;
    private final String _schema;
    private final String _query;

    private int _insertTimeout = 180_000;

    public QueryApiHelper(Connection connection, String containerPath, String schema, String query)
    {
        _connection = connection;
        _containerPath = containerPath;
        _schema = schema;
        _query = query;
    }

    public QueryApiHelper setInsertTimeout(int insertTimeout)
    {
        _insertTimeout = insertTimeout;
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
        return selectRows(columns, filters, sorts, null);
    }

    public SelectRowsResponse selectRows(List<String> columns, @Nullable List<Filter> filters, @Nullable List<Sort> sorts, @Nullable ContainerFilter cf) throws IOException, CommandException
    {
        SelectRowsCommand cmd = new SelectRowsCommand(_schema, _query);
        if (filters != null)
            cmd.setFilters(new ArrayList<>(filters));
        if (sorts != null)
            cmd.setSorts(sorts);
        if (columns!=null)
            cmd.setColumns(columns);
        if (cf != null)
            cmd.setContainerFilter(cf);
        return cmd.execute(_connection, _containerPath);
    }

    public <T> RowsResponse insertRows(List<Map<String, T>> rows) throws IOException, CommandException
    {
        InsertRowsCommand insertRowsCommand = new InsertRowsCommand(_schema, _query);
        insertRowsCommand.setRows(makeApiRows(rows));
        insertRowsCommand.setTimeout(_insertTimeout);
        insertRowsCommand.setAuditBehavior(BaseRowsCommand.AuditBehavior.DETAILED);
        return insertRowsCommand.execute(_connection, _containerPath);
    }

    public <T> RowsResponse updateRows(List<Map<String, T>> rows) throws IOException, CommandException
    {
        UpdateRowsCommand updateRowsCommand = new UpdateRowsCommand(_schema, _query);
        updateRowsCommand.setRows(makeApiRows(rows));
        updateRowsCommand.setTimeout(_insertTimeout);
        updateRowsCommand.setAuditBehavior(BaseRowsCommand.AuditBehavior.DETAILED);
        return updateRowsCommand.execute(_connection, _containerPath);
    }

    public <T> MoveRowsCommand createMoveRowsCommand(List<Map<String, T>> rows, String targetContainerPath)
    {
        MoveRowsCommand moveRowsCommand = new MoveRowsCommand(targetContainerPath, _schema, _query);
        moveRowsCommand.setRows(makeApiRows(rows));
        moveRowsCommand.setTimeout(_insertTimeout);
        moveRowsCommand.setAuditBehavior(BaseRowsCommand.AuditBehavior.DETAILED);
        return moveRowsCommand;
    }

    public <T> MoveRowsResponse moveRows(List<Map<String, T>> rows, String targetContainerPath) throws IOException, CommandException
    {
        return moveRows(createMoveRowsCommand(rows, targetContainerPath));
    }

    public MoveRowsResponse moveRows(MoveRowsCommand moveRowsCommand) throws IOException, CommandException
    {
        RowsResponse response = moveRowsCommand.execute(_connection, _containerPath);
        return new MoveRowsResponse(response);
    }

    public ImportDataResponse importData(String text) throws IOException, CommandException
    {
        ImportDataCommand importDataCommand = new ImportDataCommand(_schema, _query);
        importDataCommand.setText(text);
        importDataCommand.setTimeout(_insertTimeout);
        return importDataCommand.execute(_connection, _containerPath);
    }

    public ImportDataResponse importData(File file) throws IOException, CommandException
    {
        ImportDataCommand importDataCommand = new ImportDataCommand(_schema, _query);
        importDataCommand.setFile(file);
        importDataCommand.setTimeout(_insertTimeout);
        return importDataCommand.execute(_connection, _containerPath);
    }

    public ImportDataResponse importExperimentData(String text, AuditLogHelper.AuditBehaviorType auditBehaviorType, ImportDataCommand.InsertOption insertOption, boolean isCrossType, boolean isCrossFolder, boolean isAsync) throws IOException, CommandException
    {
        ImportExperimentDataCommand importDataCommand = new ImportExperimentDataCommand(_schema, _query, _containerPath);
        importDataCommand.setAuditBehavior(auditBehaviorType);
        importDataCommand.setUseAsync(isAsync);
        importDataCommand.setCrossFolderImport(isCrossFolder);
        importDataCommand.setCrossTypeImport(isCrossType);
        importDataCommand.setText(text);
        importDataCommand.setInsertOption(insertOption);
        importDataCommand.setTimeout(_insertTimeout);
        return importDataCommand.execute(_connection, _containerPath);
    }

    /**
     * @param rowsToDelete Should include primary key(s) for the table
     * @return a list of the rows that were deleted
     */
    public <T> RowsResponse deleteRows(List<Map<String, T>> rowsToDelete) throws IOException, CommandException
    {
        DeleteRowsCommand cmd = new DeleteRowsCommand(_schema, _query);
        cmd.setRows(makeApiRows(rowsToDelete));
        cmd.setAuditBehavior(BaseRowsCommand.AuditBehavior.DETAILED);
        return cmd.execute(_connection, _containerPath);
    }

    private <T> List<Map<String, Object>> makeApiRows(List<Map<String, T>> rows)
    {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, T> row : rows)
        {
            Map<String, Object> rowMap = new HashMap<>(row);
            result.add(rowMap);
        }
        return result;
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
