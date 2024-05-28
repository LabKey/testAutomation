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

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CharSequenceReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.util.TestLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TabLoader extends DataLoader
{
    public static final FileType TSV_FILE_TYPE = new TabFileType(Arrays.asList(".tsv", ".txt"), ".tsv", "text/tab-separated-values");
    public static final FileType CSV_FILE_TYPE = new TabFileType(Collections.singletonList(".csv"), ".csv", "text/comma-separated-values");

    public static class TsvFactory extends AbstractDataLoaderFactory
    {
        @NotNull
        @Override
        public DataLoader createLoader(File file, boolean hasColumnHeaders)
        {
            return new TabLoader(file, hasColumnHeaders);
        }

        /**
         * A DataLoader created with this constructor does NOT close the reader
         */
        @NotNull
        @Override
        public DataLoader createLoader(InputStream is, boolean hasColumnHeaders)
        {
            return new TabLoader(new InputStreamReader(is, StandardCharsets.UTF_8), hasColumnHeaders);
        }

        @NotNull
        @Override
        public FileType getFileType()
        {
            return TSV_FILE_TYPE;
        }
    }

    public static class CsvFactory extends AbstractDataLoaderFactory
    {
        @NotNull
        @Override
        public DataLoader createLoader(File file, boolean hasColumnHeaders) throws IOException
        {
            TabLoader loader = new TabLoader(file, hasColumnHeaders);
            loader.parseAsCSV();
            return loader;
        }

        @NotNull
        @Override
        // A DataLoader created with this constructor does NOT close the reader
        public DataLoader createLoader(InputStream is, boolean hasColumnHeaders) throws IOException
        {
            TabLoader loader = new TabLoader(new InputStreamReader(is, StandardCharsets.UTF_8), hasColumnHeaders);
            loader.parseAsCSV();
            return loader;
        }

        @Override
        public @NotNull FileType getFileType()
        {
            return CSV_FILE_TYPE;
        }
    }

    public static class CsvFactoryNoConversions extends CsvFactory
    {
        @NotNull
        @Override
        public DataLoader createLoader(File file, boolean hasColumnHeaders) throws IOException
        {

            DataLoader loader = super.createLoader(file, hasColumnHeaders);
            return loader;
        }

        @NotNull
        @Override
        // A DataLoader created with this constructor does NOT close the reader
        public DataLoader createLoader(InputStream is, boolean hasColumnHeaders) throws IOException
        {
            DataLoader loader = super.createLoader(is, hasColumnHeaders);
            return loader;
        }

        @Override
        public @NotNull FileType getFileType()
        {
            return CSV_FILE_TYPE;
        }
    }


    protected static char COMMENT_CHAR = '#';

    // source data
    private final ReaderFactory _readerFactory;
    private BufferedReader _reader = null;

    private int _commentLines = 0;
    private Map<String, String> _comments = new HashMap<>();
    private char _chDelimiter = '\t';
    private String _strDelimiter = new String(new char[]{_chDelimiter});
    private String _lineDelimiter = null;

    private String _strQuote = null;
    private String _strQuoteQuote = null;
    private boolean _parseQuotes = true;
    private boolean _unescapeBackslashes = true;
    private Filter<Map<String, Object>> _mapFilter;

    // Infer whether there are headers
    public TabLoader(File inputFile)
    {
        this(inputFile, null);
    }


    public TabLoader(final File inputFile, Boolean hasColumnHeaders)
    {
        this(() -> {
            verifyFile(inputFile);
            // Detect Charset encoding using BOM
            return Readers.getBOMDetectingReader(inputFile);
        }, hasColumnHeaders);

        setScrollable(true);
    }

    // Infer whether there are headers
    public TabLoader(CharSequence src)
    {
        this(src, null);
    }

    public TabLoader(final CharSequence src, Boolean hasColumnHeaders)
    {
        this(() -> new BufferedReader(new CharSequenceReader(src)), hasColumnHeaders);

        if (src == null)
            throw new IllegalArgumentException("src cannot be null");

        setScrollable(true);
    }

    /**
     * A TabLoader created with this constructor does NOT close the reader
     */
    public TabLoader(Reader reader, Boolean hasColumnHeaders)
    {
        this(reader, hasColumnHeaders, null);
    }

    /**
     * A TabLoader created with this constructor does NOT close the reader
     */
    public TabLoader(Reader reader, Boolean hasColumnHeaders, Boolean closeOnComplete)
    {
        this(reader, hasColumnHeaders, false);
    }

    /**
     * A TabLoader created with this constructor closes the reader only if closeOnComplete is true
     */
    public TabLoader(final Reader reader, Boolean hasColumnHeaders, final boolean closeOnComplete)
    {
        this(new ReaderFactory()
        {
            private boolean _closed = false;

            @Override
            public BufferedReader getReader()
            {
                if (_closed)
                    throw new IllegalStateException("Reader is closed");

                // Customize close() behavior to track closing and handle closeOnComplete
                return new BufferedReader(reader)
                {
                    @Override
                    public void close() throws IOException
                    {
                        _closed = true;

                        if (closeOnComplete)
                            super.close();
                    }
                };
            }
        }, hasColumnHeaders);

        setScrollable(false);
    }


    private TabLoader(ReaderFactory factory, Boolean hasColumnHeaders)
    {
        _readerFactory = factory;

        if (null != hasColumnHeaders)
            setHasColumnHeaders(hasColumnHeaders);
    }


    protected BufferedReader getReader() throws IOException
    {
        if (null == _reader)
        {
            _reader = _readerFactory.getReader();
            // Issue 23437 - use a reasonably high limit for buffering
            _reader.mark(10 * 1024 * 1024);
        }

        return _reader;
    }

    public Map<String, String> getComments() throws IOException
    {
        ensureInitialized();

        return Collections.unmodifiableMap(_comments);
    }


    /**
     * called for non-quoted strings
     * you could argue that TAB delimited string shouldn't have white space stripped, but
     * we always strip.
     */
    protected String parseValue(String value)
    {
        value = StringUtils.trimToEmpty(value);
        if ("\\N".equals(value))
            return _preserveEmptyString ? null : "";
        if (_unescapeBackslashes)
        {
            try
            {
                return StringEscapeUtils.unescapeJava(value);
            }
            catch (IllegalArgumentException e)
            {
                // Issue 16691: OctalUnescaper or UnicodeUnescaper translators will throw NumberFormatException for illegal sequences such as '\' followed by octal '9' or unicode 'zzzz'.
                // StringEscapeUtils can also throw IllegalArgumentException
                String msg = "Error reading data. Can't unescape value '" + value + "'. ";
                if (e instanceof NumberFormatException)
                    msg += "Number format error ";
                msg += e.getMessage();
                if (isThrowOnErrors())
                    throw new IllegalArgumentException(msg, e);
                else
                    TestLogger.warn(msg, e);
            }
        }
        return value;
    }

    private ArrayList<String> listParse = new ArrayList<>(30);


    private CharSequence readLine(BufferedReader r, boolean skipComments, boolean skipBlankLines)
    {
        String line = readOneTextLine(r, skipComments, skipBlankLines);
        if (null == line || null == _lineDelimiter)
            return line;
        if (line.endsWith(_lineDelimiter))
            return line.substring(0, line.length() - _lineDelimiter.length());
        StringBuilder sb = new StringBuilder(line);
        while (null != (line = readOneTextLine(r, false, false)))
        {
            sb.append("\n");
            if (line.endsWith(_lineDelimiter))
            {
                sb.append(line.substring(0, line.length() - _lineDelimiter.length()));
                return sb;
            }
            sb.append(line);
        }
        return sb;
    }


    private String readOneTextLine(BufferedReader r, boolean skipComments, boolean skipBlankLines)
    {
        try
        {
            String line;
            do
            {
                line = r.readLine();
                if (line == null)
                    return null;
            }
            while ((skipComments && line.length() > 0 && line.charAt(0) == COMMENT_CHAR) || (skipBlankLines && null == StringUtils.trimToNull(line)));
            return line;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    Pattern _replaceDoubleQuotes = null;

    private String[] readFields(BufferedReader r, @Nullable ColumnDescriptor[] columns)
    {
        if (!_parseQuotes)
        {
            CharSequence line = readLine(r, true, !isIncludeBlankLines());
            if (line == null)
                return null;
            String[] fields = StringUtils.splitByWholeSeparator(line.toString(), _strDelimiter);
            for (int i = 0; i < fields.length; i++)
                fields[i] = parseValue(fields[i]);
            return fields;
        }

        CharSequence line = readLine(r, true, !isIncludeBlankLines());
        if (line == null)
            return null;
        StringBuilder buf = line instanceof StringBuilder ? (StringBuilder) line : new StringBuilder(line);

        String field = null;
        int start = 0, colIndex = 0;
        listParse.clear();

        while (start < buf.length())
        {
            boolean loadThisColumn = null == columns || colIndex >= columns.length || columns[colIndex].load;
            int end;
            char ch = buf.charAt(start);
            char chQuote = '"';

            colIndex++;

            if (ch == _chDelimiter)
            {
                end = start;
                field = _preserveEmptyString ? null : "";
            }
            else if (ch == chQuote)
            {
                if (_strQuote == null)
                {
                    _strQuote = new String(new char[]{chQuote});
                    _strQuoteQuote = new String(new char[]{chQuote, chQuote});
                    _replaceDoubleQuotes = Pattern.compile("\\" + chQuote + "\\" + chQuote);
                }

                end = start;
                boolean hasQuotes = false;

                while (true)
                {
                    end = buf.indexOf(_strQuote, end + 1);

                    if (end == -1)
                    {
                        // XXX: limit number of lines we read
                        CharSequence nextLine = readLine(r, false, false);
                        end = buf.length();
                        if (nextLine == null)
                        {
                            // We've reached the end of the input, so there's nothing else to append
                            break;
                        }

                        buf.append('\n');
                        buf.append(nextLine);
                        continue;
                    }

                    if (end == buf.length() - 1 || buf.charAt(end + 1) != chQuote)
                        break;
                    hasQuotes = true;
                    end++; // skip double ""
                }

                field = buf.substring(start + 1, end);
                if (hasQuotes && field.contains(_strQuoteQuote))
                    field = _replaceDoubleQuotes.matcher(field).replaceAll("\"");

                // eat final "
                end++;

                //FIX: 9727
                //if not at end of line and next char is not a tab, append any chars to field up to the next tab/eol
                //note that this is a surgical quick-fix due to the proximity of release.
                //the better fix would be to parse the file character-by-character and support
                //double quotes anywhere within the field to escape delimiters
                if (end < buf.length() && buf.charAt(end) != _chDelimiter)
                {
                    start = end;
                    end = buf.indexOf(_strDelimiter, end);
                    if (-1 == end)
                        end = buf.length();
                    field = field + buf.substring(start, end);
                }
            }
            else
            {
                end = buf.indexOf(_strDelimiter, start);
                if (end == -1)
                    end = buf.length();

                // Grab and parse the field only if we're going to load it
                if (loadThisColumn)
                {
                    field = buf.substring(start, end);
                    field = parseValue(field);
                }
            }

            // Add the field value only if we're inferring columns or column.load == true
            if (loadThisColumn)
                listParse.add(field);

            // there should be a delimiter or an EOL here
            if (end < buf.length() && buf.charAt(end) != _chDelimiter)
                throw new IllegalArgumentException("Can't parse line: " + buf);

            end += _strDelimiter.length();

            while (end < buf.length() && buf.charAt(end) != _chDelimiter && Character.isWhitespace(buf.charAt(end)))
                end++;

            start = end;
        }

        return listParse.toArray(new String[listParse.size()]);
    }

    @Deprecated // Just use a CloseableFilteredIterator.  TODO: Remove
    public void setMapFilter(Filter<Map<String, Object>> mapFilter)
    {
        _mapFilter = mapFilter;
    }

    @Override
    public CloseableIterator<Map<String, Object>> iterator()
    {
        TabLoaderIterator iter;
        try
        {
            ensureInitialized();
            iter = new TabLoaderIterator();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        if (null == _mapFilter)
            return iter;
        else
            return new CloseableFilteredIterator<>(iter, _mapFilter);
    }


    public void parseAsCSV()
    {
        setDelimiterCharacter(',');
        setParseQuotes(true);
    }

    public void setDelimiterCharacter(char delimiter)
    {
        _chDelimiter = delimiter;
        _strDelimiter = new String(new char[]{_chDelimiter});
    }

    public void setDelimiters(@NotNull String field, @Nullable String line)
    {
        if (StringUtils.isEmpty(field))
            throw new IllegalArgumentException();
        _chDelimiter = field.charAt(0);
        _strDelimiter = field;
        _lineDelimiter = StringUtils.isEmpty(line) ? null : line;
    }

    public void setParseQuotes(boolean parseQuotes)
    {
        _parseQuotes = parseQuotes;
    }

    @Override
    public void close()
    {
        IOUtils.closeQuietly(_reader);
        _reader = null;
    }

    @Override
    protected void initialize() throws IOException
    {
        readComments();
        super.initialize();
    }

    private void readComments() throws IOException
    {
        BufferedReader reader = getReader();

        try
        {
            while (true)
            {
                String s = reader.readLine();

                if (null == s)
                    break;

                if (s.length() == 0 || s.charAt(0) == COMMENT_CHAR)
                {
                    _commentLines++;

                    int eq = s.indexOf('=');
                    if (eq != -1)
                    {
                        String key = s.substring(1, eq).trim();
                        String value = s.substring(eq + 1).trim();
                        if (key.length() > 0 || value.length() > 0)
                            _comments.put(key, value);
                    }
                }
                else
                {
                    break;
                }
            }
        }
        finally
        {
            reader.reset();
        }
    }

    @Override
    public String[][] getFirstNLines(int n) throws IOException
    {
        BufferedReader reader = getReader();

        try
        {
            List<String[]> lineFields = new ArrayList<>(n);
            int i;

            for (i = 0; i < n; i++)
            {
                String[] fields = readFields(reader, null);
                if (null == fields)
                    break;
                lineFields.add(fields);
            }

            if (i == 0)
                return new String[0][];

            return lineFields.toArray(new String[i][]);
        }
        finally
        {
            reader.reset();
        }
    }


    public class TabLoaderIterator extends DataLoaderIterator
    {
        private final BufferedReader reader;

        protected TabLoaderIterator() throws IOException
        {
            super(_commentLines + _skipLines);
            assert _skipLines != -1;

            reader = getReader();
            for (int i = 0; i < lineNum(); i++)
                reader.readLine();

            // make sure _columns is initialized
            ColumnDescriptor[] cols = getColumns();

            // all input starts as String, we don't need to use a String converter
            // unless a column has configured a custom converter (e.g ViabilityTsvDataHandler)
            for (ColumnDescriptor col : cols)
            {
                if (col.converter == StringConverter && col.clazz == String.class)
                    col.converter = noopConverter;
            }
        }

        @Override
        public void close() throws IOException
        {
            try
            {
                TabLoader.this.close();
            }
            finally
            {
                super.close();
            }
        }

        @Override
        protected String[] readFields()
        {
            return TabLoader.this.readFields(reader, _columns);
        }
    }
}

