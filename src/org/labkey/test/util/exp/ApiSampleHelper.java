package org.labkey.test.util.exp;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.test.BaseWebDriverTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ApiSampleHelper
{
    private final Supplier<Connection> _connection;
    private final String _containerPath;

    public ApiSampleHelper(Connection connection, String containerPath)
    {
        _connection = () -> connection;
        _containerPath = containerPath;
    }

    public ApiSampleHelper(BaseWebDriverTest test)
    {
        _connection = test::createDefaultConnection;
        _containerPath = test.getPrimaryTestProject();
    }

    public void addSampleParent(String sampleType, String sampleId, String parentType, String parentName) throws IOException, CommandException
    {
        addParents(sampleType, List.of(sampleId), Map.of(parentType, List.of(parentName)), Collections.emptyMap());
    }

    public void addSampleParents(String sampleType, String sampleId, String parentType, List<String> parentNames) throws IOException, CommandException
    {
        addParents(sampleType, List.of(sampleId), Map.of(parentType, parentNames), Collections.emptyMap());
    }

    public void addSourceParent(String sampleType, String sampleId, String parentType, String parentName) throws IOException, CommandException
    {
        addParents(sampleType, List.of(sampleId), Collections.emptyMap(), Map.of(parentType, List.of(parentName)));
    }

    public void addSourceParents(String sampleType, String sampleId, String parentType, List<String> parentNames) throws IOException, CommandException
    {
        addParents(sampleType, List.of(sampleId), Collections.emptyMap(), Map.of(parentType, parentNames));
    }

    public void addParents(String sampleType, List<String> sampleIds, Map<String, List<String>> parents, Map<String, List<String>> sources) throws IOException, CommandException
    {
        Map<String, Integer> rowIds = SampleTypeAPIHelper.getRowIdsForSamples(_containerPath, sampleType, sampleIds);

        Map<String, Object> sampleParentsColumns = new HashMap<>();
        for (Map.Entry<String, List<String>> parentEntries : parents.entrySet())
        {
            sampleParentsColumns.put("materialInputs/" + parentEntries.getKey(), String.join(",", parentEntries.getValue()));
        }
        Map<String, Object> sourceParentColumns = new HashMap<>();
        for (Map.Entry<String, List<String>> sourceEntries : sources.entrySet())
        {
            sourceParentColumns.put("dataInputs/" + sourceEntries.getKey(), String.join(",", sourceEntries.getValue()));
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String sampleId : sampleIds)
        {
            Map<String, Object> row = new HashMap<>();
            row.put("RowId", rowIds.get(sampleId));
            row.putAll(sampleParentsColumns);
            row.putAll(sourceParentColumns);
            rows.add(row);
        }
        UpdateRowsCommand updateRowsCommand = new UpdateRowsCommand(SampleTypeAPIHelper.SCHEMA_NAME, sampleType);
        updateRowsCommand.setRows(rows);
        updateRowsCommand.execute(_connection.get(), _containerPath);
    }
}
