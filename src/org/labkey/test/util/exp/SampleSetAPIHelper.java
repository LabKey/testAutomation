package org.labkey.test.util.exp;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.GetDomainCommand;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.remoteapi.domain.SaveDomainCommand;
import org.labkey.test.WebTestHelper;
import org.labkey.test.params.experiment.SampleSetDefinition;
import org.labkey.test.util.TestDataGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
}
