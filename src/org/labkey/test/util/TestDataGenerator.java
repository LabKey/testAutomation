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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.labkey.api.collections.CaseInsensitiveLinkedHashMap;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.remoteapi.domain.CreateDomainCommand;
import org.labkey.remoteapi.domain.DomainResponse;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.serverapi.reader.TabLoader;
import org.labkey.serverapi.writer.PrintWriters;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.query.QueryApiHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Use this class to generate random test data for a given column schema
 */
public class TestDataGenerator
{
    // chose a Character random from this String
    private static final String ALPHANUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz";

    private final Map<String, PropertyDescriptor> _columns = new CaseInsensitiveLinkedHashMap<>();
    private final Map<String, Supplier<Object>> _dataSuppliers = new CaseInsensitiveHashMap<>();
    private final Set<String> _autoGeneratedFields = Collections.newSetFromMap(new CaseInsensitiveHashMap<>());
    // Rows generated for next insert or TSV dump
    private final List<Map<String, Object>> _rows = new ArrayList<>();
    // All rows ever generated by this generator
    private final List<Map<String, Object>> _allGeneratedRows = new ArrayList<>();

    private final String _schemaName;
    private final String _queryName;
    private final String _containerPath;


    /**
     *  use TestDataGenerator to generate data to a specific fieldSet
     */
    public TestDataGenerator(String schema, String queryName, String containerPath)
    {
        _schemaName = schema;
        _queryName = queryName;
        _containerPath = containerPath;
    }

    /**
     * @deprecated This isn't actually a lookup
     */
    @Deprecated (since = "22.4")
    public TestDataGenerator(FieldDefinition.LookupInfo lookupInfo)
    {
        this(lookupInfo.getSchema(), lookupInfo.getTable(), lookupInfo.getFolder());
    }

    public String getSchema()
    {
        return _schemaName;
    }

    public String getQueryName()
    {
        return _queryName;
    }

    /**
     *
     * @param columns   The fieldSet for the domain/sampleset/list.
     */
    public TestDataGenerator withColumns(List<? extends PropertyDescriptor> columns)
    {
        for (PropertyDescriptor fieldDef : columns)
        {
            _columns.put(fieldDef.getName(), fieldDef);
        }
        return this;
    }

    /**
     * Set fields that should be excluded from data generation. Such as intList key or sample name with nameExpression.
     * @param fieldNames fields to exclude
     * @return this
     */
    public TestDataGenerator setAutoGeneratedFields(String... fieldNames)
    {
        _autoGeneratedFields.clear();
        _autoGeneratedFields.addAll(Arrays.asList(fieldNames));

        return this;
    }

    public TestDataGenerator withGeneratedRows(int desiredRowCount)
    {
        if (getRowCount() > 0)
        {
            throw new IllegalStateException("Rows have already been generated");
        }

        generateRows(desiredRowCount);

        return this;
    }

    public TestDataGenerator addDataSupplier(String columnName, Supplier<Object> supplier)
    {
        _dataSuppliers.put(columnName, ()-> supplier.get());
        return this;
    }

    public TestDataGenerator addIntSupplier(String columnName, int min, int max)
    {
        _dataSuppliers.put(columnName, ()-> randomInt(min, max));
        return this;
    }

    public TestDataGenerator addStringSupplier(String columnName, int length)
    {
        _dataSuppliers.put(columnName, ()-> randomString(length));
        return this;
    }

    /**
     *  // helper to allow adding values as List.of(a, b, c)
     *  Note: this helper uses the indexes of the fieldSet in testDataGenerator, so you can only use this helper
     *  for fields that are explicitly part of the domain.  To add values into off-domain/lookup columns, use @addCustomRow
     * @param values values are mapped to columns in the order the columns were added
     */
    public TestDataGenerator addRow(List<Object> values)
    {
        final ArrayList<String> columns = new ArrayList<>(_columns.keySet()); // _columns remembers insertion order
        Assert.assertEquals("Did not provide the correct size row", columns.size(), values.size());

        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i < values.size(); i++)
        {
            row.put(columns.get(i), values.get(i));
        }
        addCustomRow(row);
        return this;
    }

    public TestDataGenerator addRowsFromFile(File tsv)
    {
        try (TabLoader loader = new TabLoader(tsv, true))
        {
            for (Map<String, Object> row : loader.load())
            {
                addCustomRow(row);
            }
        }
        return this;
    }

    /**
     *  Adds the specified row to the internal collection of rowMaps the object contains.
     *  To insert them to the server, use this.
     *  it is acceptable to add columns that don't exist in the destination, but be careful
     * @param customRow use Map.of(colName1, colValue1 ...)
     */
    public TestDataGenerator addCustomRow(Map<String, Object> customRow)
    {
        customRow = Collections.unmodifiableMap(new CaseInsensitiveHashMap<>(customRow));
        _rows.add(customRow);
        _allGeneratedRows.add(customRow);
        return this;
    }

    public List<Map<String, Object>> getRows()
    {
        return Collections.unmodifiableList(_rows);
    }

    public int getRowCount()
    {
        return _rows.size();
    }

    /**
     * Get all rows previously generated by this instance
     */
    public List<Map<String, Object>> getAllGeneratedRows()
    {
        return _allGeneratedRows;
    }

    public List<PropertyDescriptor> getColumns()
    {
        return new ArrayList<>(_columns.values());
    }

    public void generateRows(int numberOfRowsToGenerate)
    {
        if (_columns.keySet().size() == 0)
            throw new IllegalStateException("can't generate row data without column definitions");

        for (int i= 0; i < numberOfRowsToGenerate; i++)
        {
            Map<String, Object> newRow = generateRow();
            addCustomRow(newRow);
        }
    }

    private Map<String, Object> generateRow()
    {
        final Map<String, Object> newRow = new CaseInsensitiveHashMap<>();
        for (String columnName : _columns.keySet())
        {
            if (!_dataSuppliers.containsKey(columnName))
            {
                _dataSuppliers.put(columnName, getDefaultDataSupplier(_columns.get(columnName).getRangeURI()));
            }

            if (_autoGeneratedFields.contains(columnName))
            {
                continue;
            }
            // Generate values
            Object columnValue = _dataSuppliers.get(columnName).get();
            newRow.put(columnName, columnValue);
        }
        return newRow;
    }

    private Supplier<Object> getDefaultDataSupplier(String columnType)
    {
        switch (columnType.substring(columnType.indexOf('#') + 1).toLowerCase())
        {
            case "string":
                return ()-> randomString(20);
            case "int":
                return ()-> randomInt(0, 20);
            case "float":
                return ()-> randomFloat(0, 20);
            case "double":
                return ()-> randomDouble(0, 20);
            case "boolean":
                return ()-> randomBoolean();
            case "date":
            case "datetime":
                return ()-> randomDateString(DateUtils.addWeeks(new Date(), -39), new Date());
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

    public StringBuilder writeTsvHeaders()
    {
        StringBuilder builder = new StringBuilder();
        List<String> fieldNames = new ArrayList<>(_columns.keySet());
        fieldNames.removeAll(_autoGeneratedFields);

        builder.append(String.join("\t", fieldNames));
        builder.append("\n");

        return builder;
    }

    public String rowToString(List<String> fieldNames, Map<String, Object> row)
    {
        StringBuilder builder = new StringBuilder();
        List<String> values = new ArrayList<>();
        for (String name : fieldNames)
        {
            Object value = row.get(name);
            values.add(value != null ? String.valueOf(value) : "");
        }
        builder.append(String.join("\t", values));
        builder.append("\n");

        return builder.toString();
    }

    /**
     * generates tsv-formatted content using the rows in the current instance;
     * @return TSV formatted representation of generated rows
     */
    public String writeTsvContents()
    {
        List<String> fieldNames = new ArrayList<>(_columns.keySet());
        fieldNames.removeAll(_autoGeneratedFields);
        StringBuilder builder = writeTsvHeaders();

        for (Map<String, Object> row : _rows)
        {
            builder.append(rowToString(fieldNames, row));
        }
        return builder.toString();
    }

    public File writeGeneratedDataToFile(int numberOfRowsToGenerate, String fileName) throws IOException
    {
        String headers = writeTsvHeaders().toString();
        File file = new File(TestFileUtils.getTestTempDir(), fileName);
        FileUtils.forceMkdirParent(file);

        try(PrintWriter stream = PrintWriters.getPrintWriter(file))
        {
            List<String> fieldNames = new ArrayList<>(_columns.keySet());
            fieldNames.removeAll(_autoGeneratedFields);
            stream.write(headers);

            for (int i = 0; i < numberOfRowsToGenerate; i++)
            {
                Map<String, Object> newRow = generateRow();
                var rowMapString = rowToString(fieldNames, newRow);
                stream.write(rowMapString);
            }
        }
        return file;
    }

    public File writeGeneratedDataToExcel(int numberOfRowsToGenerate, String sheetName, String fileName) throws IOException
    {
        File file = new File(TestFileUtils.getTestTempDir(), fileName);
        FileUtils.forceMkdirParent(file);

        try(SXSSFWorkbook workbook = new SXSSFWorkbook(1000); // only holds 1000 rows in memory
            FileOutputStream out = new FileOutputStream(file))
        {
            var sheet = workbook.createSheet(sheetName);

            // write headers as row 0
            String[] columnNames = _columns.keySet().toArray(new String[0]);
            var headerRow = sheet.createRow(0);
            for (int i = 0; i < columnNames.length; i++)
            {
                headerRow.createCell(i).setCellValue(columnNames[i]);
            }

            // write content
            for (int i = 1; i < numberOfRowsToGenerate +1; i++)
            {
                Map<String, Object> row = generateRow();
                SXSSFRow currentRow = sheet.createRow(i);
                for (int j = 0; j < columnNames.length; j++)
                {
                    currentRow.createCell(j).setCellValue(row.get(columnNames[j]).toString());
                }
            }
            workbook.write(out);
        }

        return file;
    }

    /**
     * Creates a file containing the contents of the current rows, formatted in TSV.
     * The file is written to the test temp dir
     * @param fileName  the name of the file, e.g. 'testDataFileForMyTest.tsv'
     * @return File object pointing at created TSV
     */
    public File writeData(String fileName)
    {
        try
        {
            return TestFileUtils.writeTempFile(fileName, writeTsvContents());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @deprecated Use {@link org.labkey.test.params.property.DomainProps#create(Connection, String)}
     */
    @Deprecated (since = "22.4")
    public DomainResponse createDomain(Connection cn, String domainKind) throws IOException, CommandException
    {
        return createDomain(cn, domainKind, null);
    }

    /**
     * @deprecated Use {@link org.labkey.test.params.property.DomainProps#create(Connection, String)}
     */
    @Deprecated (since = "22.4")
    public DomainResponse createDomain(Connection cn, String domainKind, Map<String, Object> domainOptions) throws IOException, CommandException
    {
        CreateDomainCommand cmd = new CreateDomainCommand(domainKind, getQueryName());
        cmd.getDomainDesign().setFields(new ArrayList<>(getColumns()));

        if (domainOptions!= null)
            cmd.setOptions(domainOptions);

        return cmd.execute(cn, _containerPath);
    }

    /**
     * @deprecated Use {@link org.labkey.test.params.list.IntListDefinition#create(Connection, String)}
     */
    @Deprecated (since = "22.4")
    public DomainResponse createList(Connection cn, String keyName) throws IOException, CommandException
    {
        return createDomain(cn, "IntList", Map.of("keyName", keyName));
    }

    /**
     * @deprecated Use {@link QueryApiHelper}
     */
    @Deprecated(since = "22.4")
    public CommandResponse deleteDomain(Connection cn) throws IOException, CommandException
    {
        return getQueryHelper(cn).deleteDomain();
    }

    public SaveRowsResponse insertRows() throws IOException, CommandException
    {
        return insertRows(WebTestHelper.getRemoteApiConnection());
    }

    public SaveRowsResponse insertRows(Connection cn) throws IOException, CommandException
    {
        return insertRows(cn, getRows());
    }

    public SaveRowsResponse insertRows(Connection cn, List<Map<String, Object>> rows) throws IOException, CommandException
    {
        return getQueryHelper(cn).insertRows(rows);
    }

    /**
     * @deprecated Use {@link QueryApiHelper}
     */
    @Deprecated(since = "22.4")
    public SaveRowsResponse updateRows(Connection cn, List<Map<String, Object>> rows) throws IOException, CommandException
    {
        return getQueryHelper(cn).updateRows(rows);
    }

    /**
     * @deprecated Use {@link QueryApiHelper}
     */
    @Deprecated(since = "22.4")
    public SelectRowsResponse getRowsFromServer(Connection cn) throws IOException, CommandException
    {
        return getQueryHelper(cn).selectRows();
    }

    /**
     * @deprecated Use {@link QueryApiHelper}
     */
    @Deprecated(since = "22.4")
    public SelectRowsResponse getRowsFromServer(Connection cn, List<String> intendedColumns) throws IOException, CommandException
    {
        return getQueryHelper(cn).selectRows(intendedColumns);
    }

    /**
     * @deprecated Use {@link QueryApiHelper}
     */
    @Deprecated(since = "22.4")
    public SelectRowsResponse getRowsFromServer(Connection cn, List<String> intendedColumns, @Nullable  List<Filter> filters) throws IOException, CommandException
    {
        return getQueryHelper(cn).selectRows(intendedColumns, filters);
    }

    /**
     * @deprecated Use {@link QueryApiHelper}
     */
    @Deprecated(since = "22.4")
    public SelectRowsResponse getRowsFromServer(Connection cn, List<String> intendedColumns, @Nullable  List<Filter> filters, @Nullable List<Sort> sorts) throws IOException, CommandException
    {
        return getQueryHelper(cn).selectRows(intendedColumns, filters, sorts);
    }

    /**
     * @deprecated Use {@link QueryApiHelper}
     */
    @Deprecated(since = "22.4")
    public SaveRowsResponse deleteRows(Connection cn, List<Map<String,Object>> rowsToDelete) throws IOException, CommandException
    {
        return getQueryHelper(cn).deleteRows(rowsToDelete);
    }

    public QueryApiHelper getQueryHelper(Connection connection)
    {
        return new QueryApiHelper(connection, _containerPath, _schemaName, _queryName);
    }

    public TestDataValidator getValidator()
    {
        return new TestDataValidator(_allGeneratedRows);
    }

    /**
     * @deprecated Inline this.
     */
    @Deprecated (since = "20.4")
    static public FieldDefinition simpleFieldDef(String name, FieldDefinition.ColumnType type)
    {
        return new FieldDefinition(name, type);
    }

    /**
     * @deprecated Moved to {@link DomainUtils#deleteDomain(String, String, String)}
     */
    @Deprecated (since = "22.4")
    public static CommandResponse deleteDomain(final String containerPath, final String schema,
                                                 final String queryName)
            throws CommandException
    {
        return DomainUtils.deleteDomain(containerPath, schema, queryName);
    }

    /**
     * @deprecated Moved to {@link DomainUtils#doesDomainExist(String, String, String)}
     */
    @Deprecated (since = "22.4")
    public static boolean doesDomainExists(final String containerPath, final String schema,
                                                final String queryName)
    {
        return DomainUtils.doesDomainExist(containerPath, schema, queryName);
    }

}
