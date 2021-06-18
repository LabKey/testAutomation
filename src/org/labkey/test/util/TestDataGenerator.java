/*
 * Copyright (c) 2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.util;

import org.apache.commons.lang3.time.DateUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.labkey.api.collections.CaseInsensitiveLinkedHashMap;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.remoteapi.domain.CreateDomainCommand;
import org.labkey.remoteapi.domain.DomainResponse;
import org.labkey.remoteapi.domain.DropDomainCommand;
import org.labkey.remoteapi.domain.GetDomainCommand;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.property.DomainProps;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;


/* Use this class to generate random test data for a given column schema
* */
public class TestDataGenerator
{
    // chose a Character random from this String
    private static final String ALPHANUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz";

    private final Map<Integer, PropertyDescriptor> _indices = new HashMap<>();  // used to keep columns and row keys aligned
    private final Map<String, PropertyDescriptor> _columns = new CaseInsensitiveLinkedHashMap<>();
    private final Map<String, Supplier<Object>> _dataSuppliers = new CaseInsensitiveHashMap<>();
    private List<Map<String, Object>> _rows = new ArrayList<>();

    private FieldDefinition.LookupInfo _lookupInfo;


    /*  use TestDataGenerator to generate data to a specific fieldSet
    *  */
    public TestDataGenerator(String schema, String queryName, String containerPath)
    {
        _lookupInfo = new FieldDefinition.LookupInfo(containerPath, schema, queryName);
    }
    public TestDataGenerator(FieldDefinition.LookupInfo lookupInfo)
    {
        _lookupInfo = lookupInfo;
    }

    public String getSchema()
    {
        return _lookupInfo.getSchema();
    }

    public String getQueryName()
    {
        return _lookupInfo.getTable();
    }

    /**
     *
     * @param columns   The fieldSet for the domain/sampleset/list.
     * @return
     */
    public TestDataGenerator withColumns(List<? extends PropertyDescriptor> columns)
    {
        int index = 0;
        for (PropertyDescriptor fieldDef : columns)
        {
            _columns.put(fieldDef.getName(), fieldDef);
            _indices.put(index, fieldDef);
            index++;
        }
        return this;
    }

    public TestDataGenerator withGeneratedRows(int desiredRowCount)
    {
        boolean generate = getRowCount() == 0;

        if (generate)
            generateRows(desiredRowCount);

        return this;
    }

    public TestDataGenerator addDataSupplier(String columnName, Supplier<Object> supplier)
    {
        _dataSuppliers.put(columnName, supplier);
        return this;
    }

    public TestDataGenerator addIntSupplier(String columnName, int min, int max)
    {
        _dataSuppliers.put(columnName, () -> randomInt(min, max));
        return this;
    }

    public TestDataGenerator addStringSupplier(String columnName, int length)
    {
        _dataSuppliers.put(columnName, () -> randomString(length));
        return this;
    }

    /**
     *  // helper to allow adding values as List.of(a, b, c)
     *  Note: this helper uses the indexes of the fieldSet in testDataGenerator, so you can only use this helper
     *  for fields that are explicitly part of the domain.  To add values into off-domain/lookup columns, use @addCustomRow
     * @param values
     * @return
     */
    public TestDataGenerator addRow(List<Object> values)
    {
        Assert.assertEquals("Did not provide the correct size row", _indices.size(), values.size());

        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i < values.size(); i++)     // walk across keys in index order, insert values in that order
        {
            String keyAtIndex = _indices.get(i).getName();
            row.put(keyAtIndex, values.get(i));
        }
        addCustomRow(row);
        return this;
    }

    /**
     *  Adds the specified row to the internal collection of rowMaps the object contains.
     *  To insert them to the server, use this.
     *  it is acceptable to add columns that don't exist in the destination, but be careful
     * @param customRow use Map.of(colName1, colValue1 ...)
     * @return
     */
    public TestDataGenerator addCustomRow(Map<String, Object> customRow)
    {
        _rows.add(new CaseInsensitiveHashMap<>(customRow));
        return this;
    }

    public List<Map<String, Object>> getRows()
    {
        return _rows;
    }

    public int getRowCount()
    {
        return _rows.size();
    }

    public List<PropertyDescriptor> getColumns()
    {
        List<PropertyDescriptor> cols = new ArrayList<>();
        for (PropertyDescriptor field : _columns.values())
        {
            cols.add(field);
        }
        return cols;
    }

    public void generateRows(int numberOfRowsToGenerate)
    {
        if (_columns.keySet().size() == 0)
            throw new IllegalStateException("can't generate row data without column definitions");

        for (int i= 0; i < numberOfRowsToGenerate; i++)
        {
            Map<String, Object> newRow = new HashMap<>();

            for (String key : _columns.keySet())
            {
                PropertyDescriptor columnDefinition = _columns.get(key);
                // get the column definition
                String columnName = columnDefinition.getName();
                String columnType = columnDefinition.getRangeURI();

                Object columnValue;
                columnValue = _dataSuppliers.getOrDefault(columnName, getDefaultDataSupplier(columnType)).get();
                newRow.put(columnName, columnValue);
            }
            addCustomRow(newRow);
        }
    }

    private Supplier<Object> getDefaultDataSupplier(String columnType)
    {
        switch (columnType.toLowerCase())
        {
            case "string":
                return () -> randomString(20);
            case "http://www.w3.org/2001/xmlschema#string":
                return () -> randomString(20);
            case "int":
                return () -> randomInt(0, 20);
            case "http://www.w3.org/2001/xmlschema#int":
                return () -> randomInt(0, 20);
            case "float":
                return () -> randomFloat(0, 20);
            case "double":
                return () -> randomDouble(0, 20);
            case "boolean":
                return () -> randomBoolean();
            case "date":
            case "datetime":
                return () -> randomDateString(DateUtils.addWeeks(new Date(), -39), new Date());
            default:
                throw new IllegalArgumentException("ColumnType " + columnType + " isn't implemented yet");
        }
    }

    public String randomString(int size)
    {
        StringBuilder val = new StringBuilder();
        for (int i=0; i<size; i++)
        {
            int randIndex = (int)(ALPHANUMERIC_STRING.length() * Math.random());
            val.append(ALPHANUMERIC_STRING.charAt(randIndex));
        }
        return val.toString();
    }

    public int randomInt(int min, int max)
    {
        if (min >= max)
            throw new IllegalArgumentException("min must be less than max");

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    public float randomFloat(float min, float max)
    {
        if (min >= max)
            throw new IllegalArgumentException("min must be less than max");
        Random r = new Random();
        return  min + r.nextFloat() * (max - min);
    }

    public Double randomDouble(double min, double max)
    {
        if (min >= max)
            throw new IllegalArgumentException("min must be less than max");
        Random r = new Random();
        return  min + r.nextDouble() * (max - min);
    }

    public String randomDateString(Date min, Date max)
    {
        return randomDateString("yyyy-MM-dd HH:mm", min, max);
    }

    /*
     * simple way to get a dateformat:  (String)executeScript("return LABKEY.container.formats.dateTimeFormat");
     * */
    public String randomDateString(String dateFormat, Date min, Date max)
    {
        long random = ThreadLocalRandom.current().nextLong(min.getTime(), max.getTime());
        Date date = new Date(random);
        return new SimpleDateFormat(dateFormat).format(date);
    }

    public boolean randomBoolean()
    {
        return ThreadLocalRandom.current().nextBoolean();
    }

    /**
     * generates tsv-formatted content using the rows in the current instance;
     * @return
     */
    public String writeTsvContents()
    {
        StringBuilder builder = new StringBuilder();
        for (Integer index: _indices.keySet())
        {
            builder.append(_indices.get(index).getName() + "\t");
        }
        builder.append("\n");

        for (Map row : _rows)
        {
            for (Integer index: _indices.keySet())
            {
                String keyAtIndex = _indices.get(index).getName();
                builder.append(row.get(keyAtIndex) + "\t");
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    /**
     * Creates a file containing the contents of the current rows, formatted in TSV.
     * The file is written to the test temp dir
     * @param fileName  the name of the file, e.g. 'testDataFileForMyTest.tsv'
     * @return File object pointing at created TSV
     */
    public File writeData(String fileName)
    {
        if (!TestFileUtils.getTestTempDir().exists())
            TestFileUtils.getTestTempDir().mkdirs();
        return TestFileUtils.saveFile(TestFileUtils.getTestTempDir(), fileName, writeTsvContents());
    }

    public DomainResponse createDomain(Connection cn, String domainKind) throws IOException, CommandException
    {
        return createDomain(cn, domainKind, null);
    }

    public DomainResponse createDomain(Connection cn, String domainKind, Map domainOptions) throws IOException, CommandException
    {
        CreateDomainCommand cmd = new CreateDomainCommand(domainKind, getQueryName());
        cmd.getDomainDesign().setFields(getColumns());

        if (domainOptions!= null)
            cmd.setOptions(domainOptions);

        return cmd.execute(cn, _lookupInfo.getFolder());
    }

    public DomainResponse createList(Connection cn, String keyName) throws IOException, CommandException
    {
        return createDomain(cn, "IntList", Map.of("keyName", keyName));
    }

    public DomainResponse getDomain(Connection cn) throws IOException, CommandException
    {
        GetDomainCommand cmd = new GetDomainCommand(getSchema(), getQueryName());
        DomainResponse response = cmd.execute(cn, _lookupInfo.getFolder());
        return response;
    }

    public CommandResponse deleteDomain(Connection cn) throws IOException, CommandException
    {
        DropDomainCommand delCmd = new DropDomainCommand(getSchema(), getQueryName());
        return delCmd.execute(cn, _lookupInfo.getFolder());
    }

    public SaveRowsResponse insertRows() throws IOException, CommandException
    {
        return insertRows(WebTestHelper.getRemoteApiConnection(), getRows());
    }

    public SaveRowsResponse insertRows(Connection cn) throws IOException, CommandException
    {
        return insertRows(cn, getRows());
    }

    public SaveRowsResponse insertRows(Connection cn, List<Map<String, Object>> rows) throws IOException, CommandException
    {
        InsertRowsCommand insertRowsCommand = new InsertRowsCommand(getSchema(), getQueryName());
        insertRowsCommand.setRows(rows);
        insertRowsCommand.setTimeout(180000);       // default here will support large inserts
        return insertRowsCommand.execute(cn, _lookupInfo.getFolder());
    }

    public SaveRowsResponse updateRows(Connection cn, List<Map<String, Object>> rows) throws IOException, CommandException
    {
        UpdateRowsCommand updateRowsCommand = new UpdateRowsCommand(getSchema(), getQueryName());
        updateRowsCommand.setRows(rows);
        updateRowsCommand.setTimeout(180000);
        return  updateRowsCommand.execute(cn, _lookupInfo.getFolder());
    }

    public SelectRowsResponse getRowsFromServer(Connection cn) throws IOException, CommandException
    {
        return getRowsFromServer(cn, null, null);
    }

    public SelectRowsResponse getRowsFromServer(Connection cn, List<String> intendedColumns) throws IOException, CommandException
    {
        return getRowsFromServer(cn, intendedColumns, null);
    }

    public SelectRowsResponse getRowsFromServer(Connection cn, List<String> intendedColumns, @Nullable  List<Filter> filters) throws IOException, CommandException
    {
        SelectRowsCommand cmd = new SelectRowsCommand(getSchema(), getQueryName());

        if(filters != null)
        {
            for (Filter filter : filters)
            {
                cmd.addFilter(filter);
            }
        }

        if (intendedColumns!=null)
            cmd.setColumns(intendedColumns);

        return cmd.execute(cn, _lookupInfo.getFolder());
    }

    /**
     *
     * @param cn
     * @param rowsToDelete
     * @return  a list of the rows that were deleted
     * @throws IOException
     * @throws CommandException
     */
    public SaveRowsResponse deleteRows(Connection cn, List<Map<String,Object>> rowsToDelete) throws IOException, CommandException
    {
        DeleteRowsCommand cmd = new DeleteRowsCommand(getSchema(), getQueryName());
        cmd.setRows(rowsToDelete);
        return cmd.execute(cn, _lookupInfo.getFolder());
    }

    public TestDataValidator getValidator()
    {
        return new TestDataValidator(_lookupInfo, _columns, _rows);
    }

    /**
     * @deprecated Inline this.
     */
    @Deprecated
    static public PropertyDescriptor simpleFieldDef(String name, FieldDefinition.ColumnType type)
    {
        return new FieldDefinition(name, type);
    }

    public static TestDataGenerator createDomain(String containerPath, DomainProps def) throws CommandException
    {
        Connection connection = WebTestHelper.getRemoteApiConnection();

        CreateDomainCommand createDomainCommand = def.getCreateCommand();
        try
        {
            createDomainCommand.execute(connection, containerPath);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to create domain.", e);
        }

        return def.getTestDataGenerator(containerPath);
    }

    /**
     * Delete a domain.
     *
     * @param containerPath Container where the domain exists.
     * @param schema The schema of the domain. For example for sample types it would be 'exp.data'.
     * @param queryName The name of the query to delete. For example for a sample type it would be its name.
     * @return The command response after executing the delete. Calling function would need to validate the response.
     * @throws CommandException Thrown if there is some kind of exception deleting the domain.
     */
    public static CommandResponse deleteDomain(final String containerPath, final String schema,
                                                 final String queryName)
            throws CommandException
    {
        Connection connection = WebTestHelper.getRemoteApiConnection();
        DropDomainCommand delCmd = new DropDomainCommand(schema, queryName);

        try
        {
            return delCmd.execute(connection, containerPath);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to delete domain.", e);
        }

    }

    /**
     * Check to see if a domain exists.
     *
     * @param containerPath The container to look for the domain/query.
     * @param schema The schema of the domain. For example for sample types it would be 'exp.data'.
     * @param queryName The name of the query to look for. For example for a sample type it would be its name.
     * @return True if it exists in the given container false otherwise.
     */
    public static boolean doesDomainExists(final String containerPath, final String schema,
                                                final String queryName)
    {
        Connection connection = WebTestHelper.getRemoteApiConnection();
        GetDomainCommand cmd = new GetDomainCommand(schema, queryName);
        try
        {
            DomainResponse response = cmd.execute(connection, containerPath);
            return true;
        }
        catch (CommandException ce)
        {
            if(ce.getStatusCode() == 404)
            {
                return false;
            }
            throw new RuntimeException("Exception while looking for domain.", ce);
        }
        catch (IOException ioe)
        {
            throw new RuntimeException("IO exception while looking for the domain.", ioe);
        }

    }

}
