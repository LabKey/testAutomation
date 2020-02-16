package org.labkey.test.util.exp;

import org.json.simple.JSONObject;
import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.GetDomainCommand;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.remoteapi.domain.SaveDomainCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.WebTestHelper;
import org.labkey.test.params.experiment.SampleSetDefinition;
import org.labkey.test.util.TestDataGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SampleSetAPIHelper
{
    /**
     * Create a sample set in the specified container with the fields provided.
     * TODO: Add support for name expression, description, and import aliases
     *
     * @param containerPath Container in which to create the sample set
     * @param def domain properties for the new sample set
     * @return
     */
    public static TestDataGenerator createEmptySampleSet(String containerPath, SampleSetDefinition def)
    {
        Connection connection = WebTestHelper.getRemoteApiConnection();

        PostCommand<CommandResponse> createSampleSetCommand = new PostCommand<>("experiment", "createSampleSetApi");
        JSONObject props = new JSONObject();
        props.put("name", def.getName());
        props.put("nameExpression", def.getNameExpression());
        props.put("description", def.getDescription());
        createSampleSetCommand.setJsonObject(props);
        try
        {
            CommandResponse response = createSampleSetCommand.execute(connection, containerPath);
            long domainId = response.getProperty("sampleSet.domainId");

            GetDomainCommand getDomainCommand = new GetDomainCommand(domainId);
            Domain domain = getDomainCommand.execute(connection, containerPath).getDomain();
            List<PropertyDescriptor> fields = new ArrayList<>(domain.getFields());
            // Include to default field ("name")
            def.getFields().forEach(field -> fields.add(field.toPropertyDescriptor()));
            domain.setFields(fields);

            SaveDomainCommand saveDomainCommand = new SaveDomainCommand(domainId);
            saveDomainCommand.setDomainDesign(domain);
            saveDomainCommand.execute(connection, containerPath);
        }
        catch (CommandException | IOException e)
        {
            throw new RuntimeException("Failed to create sample set", e);
        }

        return new TestDataGenerator("exp.materials", def.getName(), containerPath)
                .withColumnSet(def.getFields());
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
