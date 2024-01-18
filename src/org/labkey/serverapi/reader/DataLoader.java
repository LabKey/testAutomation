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
package org.labkey.serverapi.reader;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.serverapi.collections.ArrayListMap;
import org.labkey.serverapi.collections.RowMapFactory;
import org.labkey.test.util.TestLogger;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Abstract class for loading columnar data from file sources: TSVs, Excel files, etc.
public abstract class DataLoader implements Iterable<Map<String, Object>>, Loader, Closeable
{
    protected File _file = new File("Resource");

    protected ColumnDescriptor[] _columns;
    private boolean _initialized = false;
    protected int _scanAheadLineCount = 1000; // number of lines to scan trying to infer data types
    // CONSIDER: explicit flags for hasHeaders, inferHeaders, skipLines etc.
    protected int _skipLines = -1;      // -1 means infer headers
    private boolean _includeBlankLines = false;
    protected boolean _throwOnErrors = false;
    // true if the results can be scrolled by the DataIterator created in .getDataIterator()
    protected Boolean _scrollable = null;
    protected boolean _preserveEmptyString = false;

    public boolean isThrowOnErrors()
    {
        return _throwOnErrors;
    }

    public void setThrowOnErrors(boolean throwOnErrors)
    {
        _throwOnErrors = throwOnErrors;
    }

    public boolean isIncludeBlankLines()
    {
        return _includeBlankLines;
    }

    /** When false (the default), lines that have no values will be skipped. When true, a row of null values is returned instead. */
    public void setIncludeBlankLines(boolean includeBlankLines)
    {
        _includeBlankLines = includeBlankLines;
    }

    @Override
    public final ColumnDescriptor[] getColumns() throws IOException
    {
        ensureInitialized();

        return _columns;
    }

    protected void ensureInitialized() throws IOException
    {
        if (!_initialized)
        {
            initialize();
            _initialized = true;
        }
    }

    protected void initialize() throws IOException
    {
        initializeColumns();
    }

    public void setColumns(ColumnDescriptor[] columns)
    {
        _columns = columns;
    }

//    // if provided, the header row will be inspected and compared to these ColumnInfos.  if imported columns match a known
//    // ColumnInfo, the datatype of this column will be preferentially used.  this can help avoid issues such as a varchar column
//    // where incoming data looks numeric.
//    public void setKnownColumns(List<ColumnInfo> cols)
//    {
//        if (cols == null || cols.size() == 0)
//            throw new IllegalArgumentException("List of columns cannot be null or empty");
//
//        boolean useMv = _mvIndicatorContainer != null && !MvUtil.getIndicatorsAndLabels(_mvIndicatorContainer).isEmpty();
//        _columnInfoMap = ImportAliasable.Helper.createImportMap(cols, useMv);
//    }

    protected void initializeColumns() throws IOException
    {
        //Take our best guess since some columns won't map
        if (null == _columns)
            inferColumnInfo();
    }

    public void setHasColumnHeaders(boolean hasColumnHeaders)
    {
        _skipLines = hasColumnHeaders ? 1 : 0;
    }

    protected void setSource(File inputFile) throws IOException
    {
        verifyFile(inputFile);
        _file = inputFile;
    }

    protected static void verifyFile(File inputFile) throws IOException
    {
        if (!inputFile.exists())
            throw new FileNotFoundException(inputFile.getPath());
        if (!inputFile.canRead())
            throw new IOException("Can't read file: " + inputFile.getPath());
    }

    /** @return if the input to this DataLoader is entirely in-memory can be reset. */
    protected boolean isScrollable()
    {
        if (_scrollable == null)
        {
            _scrollable = true;
        }
        return _scrollable;
    }

    /** Set scrollable to true if the input to this DataLoader is entirely in-memory can be reset. */
    protected void setScrollable(boolean scrollable)
    {
        _scrollable = scrollable;
    }

    /**
     * By default, we treat empty strings as NULL values. Set true to keep them as empty strings instead.
     *
     */
    public void setPreserveEmptyString(boolean preserveEmptyString)
    {
        _preserveEmptyString = preserveEmptyString;
    }

    /**
     * Return the data for the first n lines. Note that
     * subclasses are allowed to return fewer than n lines
     * if there are fewer rows than that in the data.
     **/
    public abstract String[][] getFirstNLines(int n) throws IOException;

    /**
     * Look at first <code>scanAheadLineCount</code> lines of the file and infer col names, data types.
     * Most useful if maps are being returned, otherwise use inferColumnInfo(reader, clazz) to
     * use properties of a bean instead.
     *
     * @throws java.io.IOException
     */
    @SuppressWarnings({"ConstantConditions"})
    private void inferColumnInfo() throws IOException
    {
        int numLines = _scanAheadLineCount + Math.max(_skipLines, 0);
        String[][] lineFields = getFirstNLines(numLines);
        numLines = lineFields.length;

        if (numLines == 0)
        {
            _columns = new ColumnDescriptor[0];
            return;
        }

        int nCols = 0;
        for (String[] lineField : lineFields)
        {
            nCols = Math.max(nCols, lineField.length);
        }

        ColumnDescriptor[] colDescs = new ColumnDescriptor[nCols];
        for (int i = 0; i < nCols; i++)
            colDescs[i] = new ColumnDescriptor();

        //If first line is compatible type for all fields, then there is no header row
        if (_skipLines == -1)
        {
            boolean firstLineCompat = true;
            String[] fields = lineFields[0];
            for (int f = 0; f < nCols; f++)
            {
                //Issue 14295: **ArrayIndexOutOfBoundsException in org.labkey.api.reader.DataLoader.inferColumnInfo()
                //if you have an irregularly shaped TSV a given row can have fewer than nCols elements
                if (f >= fields.length || "".equals(fields[f]))
                    continue;

                try
                {
                    Object o = ConvertUtils.convert(fields[f], colDescs[f].clazz);
                    if (null == o)
                    {
                        firstLineCompat = false;
                        break;
                    }
                }
                catch (Exception x)
                {
                    firstLineCompat = false;
                    break;
                }
            }
            if (firstLineCompat)
                _skipLines = 0;
            else
                _skipLines = 1;
        }

        if (_skipLines > 0)
        {
            String[] headers = lineFields[_skipLines - 1];
            for (int f = 0; f < nCols; f++)
                colDescs[f].name = (f >= headers.length || StringUtils.isBlank(headers[f])) ? getDefaultColumnName(f) : headers[f].trim();
        }
        else
        {
            for (int f = 0; f < colDescs.length; f++)
            {
                ColumnDescriptor colDesc = colDescs[f];
                colDesc.name = getDefaultColumnName(f);
            }
        }

        Set<String> columnNames = new HashSet<>();
        for (ColumnDescriptor colDesc : colDescs)
        {
            if (!columnNames.add(colDesc.name) && isThrowOnErrors())
            {
                // TODO: This should be refactored to not throw this here, but rather, have the callers check themselves. It
                // is not in the interest of inferring columns that we validate duplicate columns.
                IOException e = new IOException("All columns must have unique names, but the column name '" + colDesc.name + "' appeared more than once.");

                throw e;
            }
        }

        _columns = colDescs;
    }

    protected String getDefaultColumnName(int col)
    {
        return "column" + col;
    }





    /**
     * Set the number of lines to look ahead in the file when infering the data types of the columns.
     */
    public void setScanAheadLineCount(int count)
    {
        _scanAheadLineCount = count;
    }

    /**
     * Returns an iterator over the data
     */
    @Override
    public abstract CloseableIterator<Map<String, Object>> iterator();


    /**
     * Returns a list of T records, one for each non-header row of the file.
     */
    // Caution: Using this instead of iterating directly has lead to many scalability problems in the past.
    // TODO: Migrate usages to iterator()
    @Override
    public List<Map<String, Object>> load()
    {
        return IteratorUtils.toList(iterator());
    }

    @Override
    public abstract void close();


    public static final Converter noopConverter = new Converter()
    {
        @Override
        public <T> T convert(Class<T> type, Object value)
        {
            return (T)value;
        }
    };
    public static final Converter StringConverter = ConvertUtils.lookup(String.class);

    protected abstract class DataLoaderIterator implements CloseableIterator<Map<String, Object>>
    {
        protected final ColumnDescriptor[] _activeColumns;
        private final RowMapFactory<Object> _factory;

        private Object[] _fields = null;
        private Map<String, Object> _values = null;
        private int _lineNum = 0;
        private boolean _closed = false;


        protected DataLoaderIterator(int lineNum) throws IOException
        {
            _lineNum = lineNum;

            // Figure out the active columns (load = true).  This is the list of columns we care about throughout the iteration.
            ColumnDescriptor[] allColumns = getColumns();
            ArrayList<ColumnDescriptor> active = new ArrayList<>(allColumns.length);

            for (ColumnDescriptor column : allColumns)
                if (column.load)
                    active.add(column);

            _activeColumns = active.toArray(new ColumnDescriptor[active.size()]);
            ArrayListMap.FindMap<String> colMap = new ArrayListMap.FindMap<>(new CaseInsensitiveHashMap<>());

            for (int i = 0; i < _activeColumns.length; i++)
            {
                colMap.put(_activeColumns[i].name, i);
            }

            _factory = new RowMapFactory<>(colMap);

            // find a converter for each column type
            for (ColumnDescriptor column : _activeColumns)
                if (column.converter == null)
                    column.converter = ConvertUtils.lookup(column.clazz);
        }

        public int lineNum()
        {
            return _lineNum;
        }

        protected abstract Object[] readFields() throws IOException;

        @Override
        public Map<String, Object> next()
        {
            if (_values == null)
                throw new IllegalStateException("Attempt to call next() on a finished iterator");
            Map<String, Object> next = _values;
            _values = null;
            return next;
        }

        @Override
        public boolean hasNext()
        {
            if (_fields != null)
                return true;    // throw illegalstate?

            try
            {
                while (true)
                {
                    _fields = readFields();
                    if (_fields == null)
                    {
                        close();
                        return false;
                    }
                    _lineNum++;

                    _values = convertValues();
                    if (_values == Collections.EMPTY_MAP && !isIncludeBlankLines())
                        continue;

                    return _values != null;
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        protected final Map<String, Object> convertValues()
        {
            if (_fields == null)
                return null;    // consider: throw IllegalState

            try
            {
                Object[] fields = _fields;
                _fields = null;
                Object[] values = new Object[_activeColumns.length];

                boolean foundData = false;
                for (int i = 0; i < _activeColumns.length; i++)
                {
                    ColumnDescriptor column = _activeColumns[i];
                    if (_preserveEmptyString && null == column.missingValues)
                    {
                        column.missingValues = "";
                    }
                    Object fld;
                    if (i >= fields.length)
                    {
                        fld = _preserveEmptyString ? null : "";
                    }
                    else
                    {
                        fld = fields[i];
                        if (fld instanceof String && StringUtils.containsOnly(((String) fld), ' '))
                            fld = "";
                        else if (fld == null)
                            fld = _preserveEmptyString ? null : "";
                    }
                    values[i] = ("".equals(fld)) ?
                            column.missingValues :
                            column.converter.convert(column.clazz, fld);

                    if (values[i] != null)
                        foundData = true;
                }

                if (foundData || isIncludeBlankLines())
                {
                    // This extra copy was added to AbstractTabLoader in r12810 to let DatasetDefinition.importDatasetData()
                    // modify the underlying maps. TODO: Refactor dataset import and return immutable maps.
                    ArrayList<Object> list = new ArrayList<>(_activeColumns.length);
                    list.addAll(Arrays.asList(values));
                    return _factory.getRowMap(list);
                }
                else
                {
                    // Return EMPTY_MAP to signal that we haven't reached the end yet
                    return Collections.emptyMap();
                }
            }
            catch (Exception e)
            {
                if (_throwOnErrors)
                {
                    if (e instanceof ConversionException)
                        throw ((ConversionException) e);
                    else
                        throw new RuntimeException(e);
                }

                if (null != _file)
                    TestLogger.error("failed loading file " + _file.getName() + " at line: " + _lineNum + " " + e, e);
            }

            // Return null to signals there are no more rows
            return null;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException("'remove()' is not defined for TabLoaderIterator");
        }

        @Override
        public void close() throws IOException
        {
            _closed = true;
        }

        @Override
        protected void finalize() throws Throwable
        {
            super.finalize();
            // assert _closed;  TODO: Uncomment to force all callers to close iterator.
        }
    }
}

