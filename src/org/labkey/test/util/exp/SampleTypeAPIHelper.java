package org.labkey.test.util.exp;

import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.WebTestHelper;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.TestDataGenerator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SampleTypeAPIHelper
{
    /**
     * Create a sample type in the specified container with the fields provided.
     *
     * @param containerPath Container in which to create the sample type.
     * @param def domain properties for the new sample type.
     * @return A TestDataGenerator for inserting rows into the created sample type.
     */
    public static TestDataGenerator createEmptySampleType(String containerPath, SampleTypeDefinition def)
    {
        try
        {
            return TestDataGenerator.createDomain(containerPath, def);
        }
        catch (CommandException e)
        {
            throw new RuntimeException("Failed to create sample type.", e);
        }

    }

    /**
     * Given a folder name, the name of a sample type, and a list of sample names return the list of sample ids.
     * Sample ids are useful when interacting with a sample/sample type using the command apis.
     *
     * @param folder Name of the folder where the sample type is.
     * @param sampleTypeName The name of the sample type.
     * @param sampleNames A list of sample name you want to get the id's for.
     * @return A map of containing the sample name and its id.
     * @throws Exception Because this uses the Select Rows Command it can throw a few different type of exceptions.
     */
    public static Map<String, Long> getSampleIdFromName(String folder, String sampleTypeName, List<String> sampleNames) throws Exception
    {
        StringBuilder nameFilter = new StringBuilder();
        for(String name : sampleNames)
        {
            nameFilter.append(name);
            // Keep adding ';' until the last Sample Id.
            if(sampleNames.indexOf(name) != sampleNames.size() - 1)
            {
                nameFilter.append(";");
            }
        }

        Connection connection = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("samples", sampleTypeName);
        cmd.setColumns(Arrays.asList("RowId", "Name"));
        cmd.addFilter("Name", nameFilter.toString(), Filter.Operator.IN);

        SelectRowsResponse response = cmd.execute(connection, folder);

        Map<String, Long> sampleIds = new HashMap<>();

        for(Map<String, Object> row : response.getRows())
        {
            Object name = row.get("Name");
            Object value = row.get("RowId");
            sampleIds.put(name.toString(), Long.parseLong(value.toString()));
        }

        // Check that the names returned from the query match the names sent in.
        Set<String> names = new HashSet<>(sampleNames);
        Assert.assertTrue("The sample names returned from the query do not match the sample names sent in.",
                names.containsAll(sampleIds.keySet()));

        return sampleIds;
    }

}
