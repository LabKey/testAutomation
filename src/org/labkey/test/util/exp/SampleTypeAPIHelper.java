package org.labkey.test.util.exp;

import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.test.WebTestHelper;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldInfo;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DomainUtils;
import org.labkey.test.util.DomainUtils.DomainKind;
import org.labkey.test.util.TestDataGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SampleTypeAPIHelper
{

    public static final String SCHEMA_NAME = "exp.materials";

    // Global constants to ease migration from "Sample Set" to "Sample Type"
    public static final String SAMPLE_TYPE_DOMAIN_KIND = DomainKind.SampleSet.name();
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
            throw new RuntimeException(String.format("Failed to create sample type. %s", e.getMessage()), e);
        }
    }

    /**
     * A set of FieldDefinition provided for convenience
     */
    public static List<FieldDefinition> sampleTypeTestFields(boolean withFileField)
    {
        List<FieldDefinition> fields = new ArrayList<>(Arrays.asList(
                FieldInfo.random("int,./Column", FieldDefinition.ColumnType.Integer).getFieldDefinition(), // Issue 53431: include special chars that are fieldKey encoded
                FieldInfo.random("decimalColumn", FieldDefinition.ColumnType.Decimal).getFieldDefinition(),
                FieldInfo.random("stringColumn", FieldDefinition.ColumnType.String).getFieldDefinition(),
                FieldInfo.random("sampleDate", FieldDefinition.ColumnType.DateAndTime).getFieldDefinition(),
                FieldInfo.random("boolColumn", FieldDefinition.ColumnType.Boolean).getFieldDefinition()));
        if (withFileField)
            fields.add(FieldInfo.random("file,./Column", FieldDefinition.ColumnType.File).getFieldDefinition());
        return fields;
    }

    public static Map<Integer, String> getSortedRowIdToSamplesMap(String containerPath, String sampleTypeName, List<String> sampleNames) throws IOException, CommandException
    {
        Connection connection = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("samples", sampleTypeName);
        cmd.setColumns(Arrays.asList("RowId", "Name"));
        cmd.addFilter("Name", String.join(";", sampleNames), Filter.Operator.IN);
        cmd.addSort("RowId", Sort.Direction.ASCENDING);

        SelectRowsResponse response = cmd.execute(connection, containerPath);

        String errorMsg = "The sample names returned from the query do not match the sample names sent in.";

        // There have been some TC failures where selectRows is not returning anything when it should. Adding this
        // logging to help get more logging when/if it happens again.
        if(response.getRowCount().intValue() == 0)
        {
            cmd = new SelectRowsCommand("samples", sampleTypeName);
            cmd.setColumns(Arrays.asList("Name"));

            SelectRowsResponse responseCount = cmd.execute(connection, containerPath);

            errorMsg = errorMsg + "\n" + String.format("No rows were returned with filter. The sample type '%s' has %d rows.",
                    sampleTypeName, responseCount.getRowCount().intValue());

            List<String> names = new ArrayList<>();
            for(Map<String, Object> row : responseCount.getRows())
            {
                Object tempName = row.get("Name");
                names.add(tempName.toString());
            }

            errorMsg = errorMsg + "\n Sample Names: " + names;
        }

        Map<Integer, String> rowIds = new TreeMap<>();

        for(Map<String, Object> row : response.getRows())
        {
            Object name = row.get("Name");
            Object value = row.get("RowId");
            rowIds.put(Integer.parseInt(value.toString()), name.toString());
        }

        // Check that the names returned from the query match the names sent in.
        Assert.assertEquals(errorMsg,
                new HashSet<>(sampleNames), new HashSet<>(rowIds.values()));

        return rowIds;
    }
    /**
     * Given a folder name, the name of a sample type, and a list of sample names return the list of row ids.
     * Row ids are useful when interacting with a sample/sample type using the command apis.
     *
     * @param containerPath Path of the container where the sample type is defined.
     * @param sampleTypeName The name of the sample type.
     * @param sampleNames A list of sample name you want to get the id's for.
     * @return A map of containing sample names and their corresponding row ids.
     */
    public static Map<String, Integer> getRowIdsForSamples(String containerPath, String sampleTypeName, List<String> sampleNames) throws IOException, CommandException
    {

        // Use json for the value parameter of the filter. This allows for tricky characters like ";" to be passed to the API.
        StringBuilder json = new StringBuilder("{json:[");

        Iterator<String> iterator = sampleNames.iterator();

        while (iterator.hasNext()) {
            String sampleName = iterator.next();

            json.append("\"");
            json.append(sampleName.replace("\"", "\\\""));
            json.append("\"");

            if (iterator.hasNext()) {
                json.append(", ");
            } else {
                json.append("]}");
            }
        }

        Connection connection = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("samples", sampleTypeName);
        cmd.setColumns(Arrays.asList("RowId", "Name"));
        cmd.addFilter("Name", json, Filter.Operator.IN);

        SelectRowsResponse response = cmd.execute(connection, containerPath);

        String errorMsg = "The sample names returned from the query do not match the sample names sent in.";

        // There have been some TC failures where selectRows is not returning anything when it should. Adding this
        // logging to help get more logging when/if it happens again.
        if(response.getRowCount().intValue() == 0)
        {
            cmd = new SelectRowsCommand("samples", sampleTypeName);
            cmd.setColumns(Arrays.asList("Name"));

            SelectRowsResponse responseCount = cmd.execute(connection, containerPath);

            errorMsg = errorMsg + "\n" + String.format("No rows were returned with filter. The sample type '%s' has %d rows.",
                    sampleTypeName, responseCount.getRowCount().intValue());

            List<String> names = new ArrayList<>();
            for(Map<String, Object> row : responseCount.getRows())
            {
                Object tempName = row.get("Name");
                names.add(tempName.toString());
            }

            errorMsg = errorMsg + "\n Sample Names: " + names;
        }

        Map<String, Integer> rowIds = new TreeMap<>();

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
     * Get sample state IDs defined in the specified project/folder. Useful for updating sample status via API
     * @param containerPath Path to the project/folder where the sample statuses are defined (typically project)
     * @return Map of
     */
    public static Map<String, Integer> getSampleStateIds(String containerPath) throws IOException, CommandException
    {
        Connection cn = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand insertCmd = new SelectRowsCommand("core", "DataStates");
        return insertCmd.execute(cn, containerPath).getRows().stream().collect(Collectors.toMap(row ->
            (String) row.get("label"), row -> (Integer) row.get("rowId")));
    }

    /**
     * Get sample state ID defined in the specified project/folder. Useful for updating sample status via API
     * @param label Label of the sample state (typically "Available", "Locked", or "Consumed")
     * @param containerPath Path to the project/folder where the sample statuses are defined (typically project)
     * @return rowId for the specified state
     */
    public static Integer getSampleStateId(String label, String containerPath) throws IOException, CommandException
    {
        Map<String, Integer> sampleStateIds = getSampleStateIds(containerPath);
        if(sampleStateIds.containsKey(label))
            return sampleStateIds.get(label);
        else
            throw new NoSuchElementException("Sample state '%s' not defined in '%s': %s".formatted(label, containerPath, sampleStateIds.keySet()));
    }

}
