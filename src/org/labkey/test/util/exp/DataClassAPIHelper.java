package org.labkey.test.util.exp;

import org.labkey.remoteapi.CommandException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.DataClassDefinition;
import org.labkey.test.util.DomainUtils;
import org.labkey.test.util.TestDataGenerator;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

}
