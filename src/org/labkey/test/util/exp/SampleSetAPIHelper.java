package org.labkey.test.util.exp;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.domain.DomainResponse;
import org.labkey.test.WebTestHelper;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleSetDefinition;
import org.labkey.test.util.TestDataGenerator;

import java.io.IOException;

public class SampleSetAPIHelper
{
    /**
     * Create a sample set in the specified container with the fields provided.
     * TODO: Add support for name expression, description, and import aliases
     *
     * @param containerPath Container in which to create the sample set
     * @param props domain properties for the new sample set
     * @return
     */
    public static TestDataGenerator createEmptySampleSet(String containerPath, SampleSetDefinition props)
    {
        if (props.getFields().stream().noneMatch(field -> field.getName().equalsIgnoreCase("name")))
        {
            // UI adds "name" field for you. API does not.
            props.addField(new FieldDefinition("name").setType(FieldDefinition.ColumnType.String));
        }

        TestDataGenerator dgen = new TestDataGenerator("exp.materials", props.getName(), containerPath)
                .withColumnSet(props.getFields());

        try
        {
            Connection connection = WebTestHelper.getRemoteApiConnection();
            DomainResponse dr = dgen.createDomain(connection, "SampleSet");
        }
        catch (IOException | CommandException rethrow)
        {
            throw new RuntimeException(rethrow);
        }

        return dgen;
    }
}
