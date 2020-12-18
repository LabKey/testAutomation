package org.labkey.test.util.exp;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.WebTestHelper;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.TestDataGenerator;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SampleTypeAPIHelper
{
    // Global constants to ease migration from "Sample Set" to "Sample Type"
    public static final String SAMPLE_TYPE_DOMAIN_KIND = "SampleSet";
    public static final String SAMPLE_TYPE_DATA_REGION_NAME = "SampleSet";
    public static final String SAMPLE_TYPE_COLUMN_NAME = "Sample Set";

    /**
     * Create a sample type in the specified container with the fields provided.
     *
     * @param containerPath Container in which to create the sample type.
     * @param def domain properties for the new sample type.
     * @return A TestDataGenerator for inserting rows into the created sample type.
     */
    public static TestDataGenerator createEmptySampleType(String containerPath, SampleTypeDefinition def)
    {
        return createEmptySampleType(containerPath, def, null);
    }

    static public TestDataGenerator createEmptySampleType(String containerPath, SampleTypeDefinition sampleTypeDefinition, String dateFormatString)
    {
        deleteDomain(new FieldDefinition.LookupInfo(containerPath, "samples", sampleTypeDefinition.getName()));

        TestDataGenerator dgen;
        try
        {
            dgen = TestDataGenerator.createDomain(containerPath, sampleTypeDefinition);
        }
        catch (CommandException e)
        {
            throw new RuntimeException("Failed to create sample type.", e);
        }

        if (null != dateFormatString)
        {
            dgen.addDataSupplier("DateColumn", () -> dgen.randomDateString(dateFormatString,
                    DateUtils.addWeeks(new Date(), -39),
                    new Date()));
        }
        return dgen;
    }

    static public SampleTypeDefinition sampleTypeDefinition(String sampleTypeName,
                                                            List<FieldDefinition> fields,
                                                            String parentAlias,
                                                            String nameExpression)
    {
        SampleTypeDefinition def = new SampleTypeDefinition(sampleTypeName);
        def.setFields(fields);
        if (null != parentAlias)
            def.addParentAlias(parentAlias);
        if (null != nameExpression)
            def.setNameExpression(nameExpression);

        return def;
    }

    public static List<FieldDefinition> sampleTypeTestFields()
    {
        return Arrays.asList(
                new FieldDefinition("intColumn", FieldDefinition.ColumnType.Integer),
                new FieldDefinition("decimalColumn", FieldDefinition.ColumnType.Decimal),
                new FieldDefinition("stringColumn", FieldDefinition.ColumnType.String),
                new FieldDefinition("sampleDate", FieldDefinition.ColumnType.DateAndTime),
                new FieldDefinition("boolColumn", FieldDefinition.ColumnType.Boolean));
    }

    static public void deleteDomain(FieldDefinition.LookupInfo targetDomain)
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
