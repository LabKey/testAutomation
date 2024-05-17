package org.labkey.test.util.exp;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.WebTestHelper;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.DataClassDefinition;
import org.labkey.test.util.DomainUtils;
import org.labkey.test.util.TestDataGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DataClassAPIHelper
{

    public static final String SCHEMA_NAME = "exp.data";
    public static final String DATA_CLASS_DATA_REGION_NAME = "DataClass";

    /**
     * Create a dataclass in the specified container with the fields provided.
     *
     * @param containerPath Container in which to create the dataclass.
     * @param dataClassDefinition domain properties for the new dataclass.
     * @return A TestDataGenerator for inserting rows into the created dataclass.
     */
    static public TestDataGenerator createEmptyDataClass(String containerPath, DataClassDefinition dataClassDefinition)
    {
        DomainUtils.ensureDeleted(containerPath, SCHEMA_NAME, dataClassDefinition.getName());

        try
        {
            return dataClassDefinition.create(WebTestHelper.getRemoteApiConnection(), containerPath);
        }
        catch (CommandException | IOException e)
        {
            throw new RuntimeException("Failed to create DataClass.", e);
        }
    }

    /**
     * A set of FieldDefinition provided for convenience
     */
    public static List<FieldDefinition> dataClassTestFields()
    {
        return Arrays.asList(
                new FieldDefinition("intColumn", FieldDefinition.ColumnType.Integer),
                new FieldDefinition("decimalColumn", FieldDefinition.ColumnType.Decimal),
                new FieldDefinition("stringColumn", FieldDefinition.ColumnType.String),
                new FieldDefinition("sampleDate", FieldDefinition.ColumnType.DateAndTime),
                new FieldDefinition("boolColumn", FieldDefinition.ColumnType.Boolean),
                new FieldDefinition("attachmentColumn", FieldDefinition.ColumnType.Attachment));
    }

    /**
     * Given a folder name, the name of a source type, and a list of source names return the list of row ids.
     *
     * @param containerPath Path of the container where the source type is defined.
     * @param sourceTypeName Name of the source type that contains the sources.
     * @param sourceNames List of sources to get row ids for.
     * @return A map of containing source names and their corresponding row ids.
     * @throws IOException Can be thrown by the call to SelectRowsCommand.
     * @throws CommandException Can be thrown by the call to SelectRowsCommand.
     */
    public static Map<String, Integer> getRowIdsForSources(String containerPath, String sourceTypeName, List<String> sourceNames) throws IOException, CommandException
    {

        Connection connection = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("exp.data", sourceTypeName);
        cmd.setColumns(Arrays.asList("RowId", "Name"));
        cmd.addFilter("Name", String.join(";", sourceNames), Filter.Operator.IN);

        SelectRowsResponse response = cmd.execute(connection, containerPath);

        Map<String, Integer> rowIds = new HashMap<>();

        for(Map<String, Object> row : response.getRows())
        {
            Object name = row.get("Name");
            Object value = row.get("RowId");
            rowIds.put(name.toString(), Integer.parseInt(value.toString()));
        }

        // Check that the names returned from the query match the names sent in.
        Assert.assertEquals("The names of the sources returned from the query do not match the names sent in.",
                new HashSet<>(sourceNames), rowIds.keySet());

        return rowIds;
    }

    @NotNull
    public static String convertMapToTsv(@NotNull List<Map<String, String>> data)
    {
        // first the header
        List<String> rows = new ArrayList<>();
        rows.add(String.join("\t", data.get(0).keySet()));
        data.forEach(dataMap -> {
            StringBuilder row = new StringBuilder();
            data.get(0).keySet().forEach(key -> {
                row.append(dataMap.get(key));
                row.append("\t");
            });
            rows.add(row.substring(0, row.lastIndexOf("\t")));
        });
        return String.join("\n", rows);
    }

}
