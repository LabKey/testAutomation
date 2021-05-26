package org.labkey.test.util.exp;

import org.labkey.remoteapi.CommandException;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.DataClassDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.TestDataGenerator;

import java.util.Arrays;
import java.util.List;

public class DataClassAPIHelper
{


    /**
     * Create a dataclass in the specified container with the fields provided.
     *
     * @param containerPath Container in which to create the dataclass.
     * @param dataClassDefinition domain properties for the new dataclass.
     * @return A TestDataGenerator for inserting rows into the created dataclass.
     */
    static public TestDataGenerator createEmptyDataClass(String containerPath, DataClassDefinition dataClassDefinition)
    {
        deleteDomain(new FieldDefinition.LookupInfo(containerPath, "exp.data", dataClassDefinition.getName()));

        TestDataGenerator dgen;
        try
        {
            return TestDataGenerator.createDomain(containerPath, dataClassDefinition);
        }
        catch (CommandException e)
        {
            throw new RuntimeException("Failed to create sample type.", e);
        }
    }

    /**
     * A set of FieldDefinition provided for convenience
     * @return
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
     * Removes the specified domain if it exists
     * @param targetDomain
     */
    public static void deleteDomain(FieldDefinition.LookupInfo targetDomain)
    {
        try
        {
            if (TestDataGenerator.doesDomainExists(targetDomain.getFolder(), targetDomain.getSchema(), targetDomain.getTable()))
            {
                TestDataGenerator.deleteDomain(targetDomain.getFolder(), targetDomain.getSchema(), targetDomain.getTable());
            }

        }
        catch (CommandException ex)
        {
            throw new RuntimeException(String
                    .format("Failed to delete '%s'.", targetDomain.getTable()), ex);
        }
    }
}
