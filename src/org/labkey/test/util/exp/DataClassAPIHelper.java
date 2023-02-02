package org.labkey.test.util.exp;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataClassAPIHelper
{

    public static final String SCHEMA_NAME = "exp.data";

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

    public static Map<String, Integer> getRowIdsForDataEntity(String containerPath, String dataTypeName, List<String> entityNames) throws IOException, CommandException
    {

        Connection connection = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("exp.data", dataTypeName);
        cmd.setColumns(Arrays.asList("RowId", "Name"));
        cmd.addFilter("Name", String.join(";", entityNames), Filter.Operator.IN);

        SelectRowsResponse response = cmd.execute(connection, containerPath);

        Map<String, Integer> rowIds = new HashMap<>();

        for(Map<String, Object> row : response.getRows())
        {
            Object name = row.get("Name");
            Object value = row.get("RowId");
            rowIds.put(name.toString(), Integer.parseInt(value.toString()));
        }

        // Check that the names returned from the query match the names sent in.
        Set<String> names = new HashSet<>(entityNames);
        Assert.assertTrue("The names of the data entities returned from the query do not match the names sent in.",
                names.containsAll(rowIds.keySet()));

        return rowIds;
    }

}
