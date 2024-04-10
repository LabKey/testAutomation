package org.labkey.test.util.exp;

import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.WebTestHelper;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DomainUtils;
import org.labkey.test.util.TestDataGenerator;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class SampleTypeAPIHelper
{

    public static final String SCHEMA_NAME = "exp.materials";

    // Global constants to ease migration from "Sample Set" to "Sample Type"
    public static final String SAMPLE_TYPE_DOMAIN_KIND = "SampleSet";
    public static final String SAMPLE_TYPE_DATA_REGION_NAME = "SampleSet";
    public static final String SAMPLE_TYPE_COLUMN_NAME = "Sample Set";
    public static final String SAMPLE_NAME_EXPRESSION = "S-${now:date}-${dailySampleCount}";

    /**
     * Create a sample type in the specified container with the fields provided.
     *
     * @param containerPath Container in which to create the sample type.
     * @param sampleTypeDefinition domain properties for the new sample type.
     * @return A TestDataGenerator for inserting rows into the created sample type.
     */
    public static TestDataGenerator createEmptySampleType(String containerPath, SampleTypeDefinition sampleTypeDefinition)
    {
        DomainUtils.ensureDeleted(containerPath, "samples", sampleTypeDefinition.getName());
        try
        {
            return sampleTypeDefinition.create(WebTestHelper.getRemoteApiConnection(), containerPath);
        }
        catch (CommandException | IOException e)
        {
            throw new RuntimeException("Failed to create sample type.", e);
        }
    }

    /**
     * A set of FieldDefinition provided for convenience
     * @return
     */
    public static List<FieldDefinition> sampleTypeTestFields()
    {
        return Arrays.asList(
                new FieldDefinition("intColumn", FieldDefinition.ColumnType.Integer),
                new FieldDefinition("decimalColumn", FieldDefinition.ColumnType.Decimal),
                new FieldDefinition("stringColumn", FieldDefinition.ColumnType.String),
                new FieldDefinition("sampleDate", FieldDefinition.ColumnType.DateAndTime),
                new FieldDefinition("boolColumn", FieldDefinition.ColumnType.Boolean));
    }

    /**
     * Given a folder name, the name of a sample type, and a list of sample names return the list of row ids.
     * Row ids are useful when interacting with a sample/sample type using the command apis.
     *
     * @param containerPath Path of the container where the sample type is defined.
     * @param sampleTypeName The name of the sample type.
     * @param sampleNames A list of sample name you want to get the id's for.
     * @return A map of containing sample names and their corresponding row ids.
     * @throws Exception Because this uses the Select Rows Command it can throw a few different type of exceptions.
     */
    public static Map<String, Integer> getRowIdsForSamples(String containerPath, String sampleTypeName, List<String> sampleNames) throws IOException, CommandException
    {

        Connection connection = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("samples", sampleTypeName);
        cmd.setColumns(Arrays.asList("RowId", "Name"));
        cmd.addFilter("Name", String.join(";", sampleNames), Filter.Operator.IN);

        SelectRowsResponse response = cmd.execute(connection, containerPath);

        String errorMsg = "The sample names returned from the query do not match the sample names sent in.";

        if(response.getRowCount().intValue() == 0)
        {
            cmd = new SelectRowsCommand("samples", sampleTypeName);
            cmd.setColumns(Arrays.asList("RowId"));

            SelectRowsResponse responseCount = cmd.execute(connection, containerPath);

            errorMsg = errorMsg + "\n" + String.format(" Now rows were returned with filter, but sample type '%s' has %d rows.",
                    sampleTypeName, responseCount.getRowCount().intValue());
        }

        Map<String, Integer> rowIds = new HashMap<>();

        for(Map<String, Object> row : response.getRows())
        {
            Object name = row.get("Name");
            Object value = row.get("RowId");
            rowIds.put(name.toString(), Integer.parseInt(value.toString()));
        }

        // Check that the names returned from the query match the names sent in.
        Assert.assertEquals(errorMsg,
                new HashSet<>(sampleNames), rowIds.keySet());

        return rowIds;
    }

    /**
     * This method has a misleading name. "Name" and "Sample ID" refer to the same column. This is actually fetching
     * row IDs of the specified samples.
     * @deprecated Use {@link #getRowIdsForSamples(String, String, List)}
     */
    @Deprecated(since = "22.4")
    public static Map<String, Integer> getSampleIdFromName(String folder, String sampleTypeName, List<String> sampleNames) throws IOException, CommandException
    {
        return getRowIdsForSamples(folder, sampleTypeName, sampleNames);
    }
}
