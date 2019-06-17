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

import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.remoteapi.domain.CreateDomainCommand;
import org.labkey.remoteapi.domain.DeleteDomainCommand;
import org.labkey.remoteapi.domain.DeleteDomainResponse;
import org.labkey.remoteapi.domain.DomainResponse;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.params.FieldDefinition;

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

    private final Map<Integer, FieldDefinition> _indices = new HashMap<>();  // used to keep columns and row keys aligned
    // TODO: Make `_columns` a `Map<String, FieldDefinition>`
    private final Map<String, FieldDefinition> _columns = new CaseInsensitiveHashMap<>();
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
    public TestDataGenerator withColumnSet(List<FieldDefinition> columns)
    {
        int index = 0;
        for (FieldDefinition fieldDef : columns)
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
        _rows.add(customRow);
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

    public List<Map<String, Object>> getColumns()
    {
        List<Map<String, Object>> cols = new ArrayList<>();
        for (String key: _columns.keySet())
        {
            cols.add(_columns.get(key).toMap());
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
                Map<String, Object> columnDefinition = _columns.get(key).toMap();
                // get the column definition
                String columnName = columnDefinition.get("name").toString().toLowerCase();
                String columnType = columnDefinition.get("rangeURI").toString().toLowerCase();

                Object columnValue;
                columnValue = _dataSuppliers.getOrDefault(columnName, getDefaultDataSupplier(columnType)).get();
                newRow.put(columnName, columnValue);
            }
            getRows().add(newRow);
        }
    }

    private Supplier<Object> getDefaultDataSupplier(String columnType)
    {
        switch (columnType)
        {
            case "string":
                return () -> randomString(20);
            case "http://www.w3.org/2001/xmlschema#string":
                return () -> randomString(20);
            case "int":
                return () -> randomInt(0, 20);
            case "http://www.w3.org/2001/xmlschema#int":
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

    /*
    * simple way to get a dateformat:  (String)executeScript("return LABKEY.container.formats.dateTimeFormat");
    * */
    public String randomDateString(String dateFormat, Date min, Date max)
    {
        long random = ThreadLocalRandom.current().nextLong(min.getTime(), max.getTime());
        Date date = new Date(random);
        return new SimpleDateFormat(dateFormat).format(date);
    }

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
                builder.append(row.get(_indices.get(index).getName()) + "\t");
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    public DomainResponse createDomain(Connection cn, String domainKind) throws IOException, CommandException
    {
        CreateDomainCommand cmd = new CreateDomainCommand(domainKind, getQueryName());
        cmd.setColumns(getColumns());
        return cmd.execute(cn, _lookupInfo.getFolder());
    }

    public DeleteDomainResponse deleteDomain(Connection cn) throws IOException, CommandException
    {
        DeleteDomainCommand delCmd = new DeleteDomainCommand(getSchema(), getQueryName());
        return delCmd.execute(cn, _lookupInfo.getFolder());
    }

    public SaveRowsResponse insertRows(Connection cn, List<Map<String, Object>> rows) throws IOException, CommandException
    {
        InsertRowsCommand insertRowsCommand = new InsertRowsCommand(getSchema(), getQueryName());
        insertRowsCommand.setRows(rows);
        insertRowsCommand.setTimeout(180000);       // default here will support large inserts
        return insertRowsCommand.execute(cn, _lookupInfo.getFolder());
    }

    public SelectRowsResponse getRowsFromServer(Connection cn) throws IOException, CommandException
    {
        return getRowsFromServer(cn, null);
    }

    public SelectRowsResponse getRowsFromServer(Connection cn, List<String> intendedColumns) throws IOException, CommandException
    {
        SelectRowsCommand cmd = new SelectRowsCommand(getSchema(), getQueryName());
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

    // helper to generate a column or field definition
    static public FieldDefinition simpleFieldDef(String name, FieldDefinition.ColumnType type)
    {
        FieldDefinition fieldDef = new FieldDefinition(name);
        fieldDef.setType(type);
        return fieldDef;
    }
}
