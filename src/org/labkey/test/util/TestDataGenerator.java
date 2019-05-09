package org.labkey.test.util;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.remoteapi.domain.CreateDomainCommand;
import org.labkey.remoteapi.domain.DeleteDomainCommand;
import org.labkey.remoteapi.domain.DeleteDomainResponse;
import org.labkey.remoteapi.domain.DomainResponse;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;


/* Use this class to generate random test data for a given column schema
* */
public class TestDataGenerator
{
    // chose a Character random from this String
    private static final String ALPHANUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz";

    private final Map<String, Map<String, Object>> _columns = new CaseInsensitiveHashMap<>();
    private final Map<String, Supplier<Object>> _dataSuppliers = new CaseInsensitiveHashMap<>();
    private List<Map<String, Object>> _rows = new ArrayList<>();
    private int _rowCount = 0;
    private String _schema;
    private String _queryName;
    private String _containerPath;


    /*  use TestDataGenerator to generate data to a specific fieldSet
    *  */
    public TestDataGenerator(String schema, String queryName, String containerPath)
    {
        _schema=schema;
        _queryName=queryName;
        _containerPath = containerPath;
    }

    /*
    this constructor extracts the fieldSet from the specified domain
    * */
    public TestDataGenerator(Connection cn, String schema, String queryName, String containerPath) throws IOException, CommandException
    {
        _schema=schema;
        _queryName=queryName;
        _containerPath = containerPath;
        List<Map<String, Object>> fieldSet = extractFieldSetFrom(cn, schema, queryName, containerPath);
        withColumnSet(fieldSet);   // extract the columns from the specified query
    }

    public String getSchema()
    {
        return _schema;
    }

    public String getQueryName()
    {
        return _queryName;
    }

    public TestDataGenerator withColumnSet(List<Map<String, Object>> columns)
    {
        for (Map<String, Object> col : columns)
        {
            _columns.put(col.get("name").toString(), col);
        }
        return this;
    }

    public TestDataGenerator withGeneratedRows(int desiredRowCount)
    {
        boolean generate = _rowCount == 0;
        _rowCount = desiredRowCount;

        if (generate)
            generateRows();

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

    public TestDataGenerator addCustomRow(Map<String, Object> customRow)
    {
        // make sure the map conforms to our column names
        Set<String> columnNames = _columns.keySet();
        if (!columnNames.containsAll(customRow.keySet()))
            throw new IllegalArgumentException("");

        getRows().add(customRow);
        _rowCount = getRows().size();
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
            cols.add(_columns.get(key));
        }
        return cols;
    }

    public void generateRows()
    {
        if (_columns.keySet().size() == 0)
            throw new IllegalStateException("can't generate row data without column definitions");

        for (int i= 0; i < _rowCount; i++)
        {
            Map<String, Object> newRow = new HashMap<>();

            for (String key : _columns.keySet())
            {
                Map<String, Object> columnDefinition = _columns.get(key);
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

    private String randomString(int size)
    {
        StringBuilder val = new StringBuilder();
        for (int i=0; i<size; i++)
        {
            int randIndex = (int)(ALPHANUMERIC_STRING.length() * Math.random());
            val.append(ALPHANUMERIC_STRING.charAt(randIndex));
        }
        return val.toString();
    }

    private int randomInt(int min, int max)
    {
        if (min >= max)
            throw new IllegalArgumentException("min must be less than max");

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    /*
    * simple way to get a dateformat:  (String)executeScript("return LABKEY.container.formats.dateTimeFormat");
    * */
    private String randomDate(String dateFormat, Date min, Date max)
    {
        long random = ThreadLocalRandom.current().nextLong(min.getTime(), max.getTime());
        Date date = new Date(random);
        return new SimpleDateFormat(dateFormat).format(date);
    }

    public DomainResponse createDomain(Connection cn, String domainKind) throws IOException, CommandException
    {
        CreateDomainCommand cmd = new CreateDomainCommand(domainKind, getQueryName());
        cmd.setColumns(getColumns());
        return cmd.execute(cn, _containerPath);
    }

    public DeleteDomainResponse deleteDomain(Connection cn) throws IOException, CommandException
    {
        DeleteDomainCommand delCmd = new DeleteDomainCommand(getSchema(), getQueryName());
        return delCmd.execute(cn, _containerPath);
    }

    public SaveRowsResponse insertGeneratedRows(Connection cn) throws IOException, CommandException
    {
        InsertRowsCommand insertRowsCommand = new InsertRowsCommand(getSchema(), getQueryName());
        insertRowsCommand.setRows(getRows());
        SaveRowsResponse response = insertRowsCommand.execute(cn, _containerPath);
        return response;
    }

    public TestDataValidator getValidator()
    {
        return new TestDataValidator(_schema, _queryName, _containerPath, _columns, _rows);
    }

    static public List<Map<String, Object>> extractFieldSetFrom(Connection cn, String schema, String queryName, String containerPath) throws IOException, CommandException
    {
        // todo: use
        SelectRowsCommand cmd = new SelectRowsCommand(schema, queryName);
        SelectRowsResponse response = cmd.execute(cn, containerPath);
        return response.getColumnModel();
    }

    // helper to generate a column or field definition
    static public Map<String, Object> simpleFieldDef(String name, String rangeURI)
    {
        Map<String, Object> fieldDef = new HashMap<>();
        fieldDef.put("name", name.toLowerCase());             // column name
        fieldDef.put("rangeURI", rangeURI);                   // column type
        return fieldDef;
    }
}
