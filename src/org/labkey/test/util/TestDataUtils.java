package org.labkey.test.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.labkey.serverapi.reader.TabLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestDataUtils
{
    private TestDataUtils()
    {
        // Utility class. Do not instantiate.
    }

    public static List<Map<String, Object>> rowMapsFromTsv(String tsvString) throws IOException
    {
        try (InputStream dataStream = IOUtils.toInputStream(tsvString, StandardCharsets.UTF_8))
        {
            return new TabLoader.TsvFactory().createLoader(dataStream, true).load();
        }
    }

    public static List<Map<String, Object>> rowMapsFromCsv(String tsvString) throws IOException
    {
        try (InputStream dataStream = IOUtils.toInputStream(tsvString, StandardCharsets.UTF_8))
        {
            return new TabLoader.CsvFactory().createLoader(dataStream, true).load();
        }
    }

    public static String tsvStringFromRowMaps(List<Map<String, Object>> rowMaps, List<String> columns,
                                              boolean includeHeaders)
    {
        return toTabular(rowMaps, columns, '\t', includeHeaders);
    }

    public static String csvStringFromRowMaps(List<Map<String, Object>> rowMaps, List<String> columns,
                                              boolean includeHeaders)
    {
        return toTabular(rowMaps, columns, ',', includeHeaders);
    }


    public static List<List<String>> rowListsFromMaps(List<Map<String, Object>> rowMaps, List<String> columns)
    {
        return rowListsFromMaps(rowMaps, columns, false, true);
    }

    /**
     * convert a List of Map<String, Object> to a list of List<String>
     * @param rowMaps   Source data
     * @param columns   keys contained in each map, will copy values associated with them to the resulting list
     * @return A List<List<String>> containing values
     * @throws IOException
     */
    public static List<List<String>> rowListsFromMaps(List<Map<String, Object>> rowMaps, List<String> columns, boolean includeHeaders, boolean preserveEmptyValues)
    {
        List<List<String>> lists = new ArrayList<>();

        if (includeHeaders)
        {
            List<String> headers = new ArrayList<>();
            for(String col : columns)
                headers.add(col);

            lists.add(headers);
        }

        for (int i=0; i<rowMaps.size(); i++)
        {
            List<String> rowList = new ArrayList<>();
            var rowMap = rowMaps.get(i);
            for(String column : columns)
            {
                var value = (String) rowMap.get(column);
                if (value == null && preserveEmptyValues)
                    rowList.add("");
                else
                    rowList.add(value);
            }
            lists.add(rowList);
        }
        return lists;
    }

    /**
     * Convert a list of Map<String, Object>> to tabluar (tsv, csv) format
     * (assumes the rowMaps all share the same keyset/schema)
     * can be used to generate edit-grid paste data, if delimiter is \t and includeHeaders is false
     *
     * @param rowMaps data to be written into tabular format
     * @param columns the fields (in order) from the rowMaps to include in tabular output
     * @param delimiter comma [,] for csv tab [\t] for tsv
     * @param includeHeaders    whether to write the keys as column names on the first line of the output string
     * @return
     */
    private static String toTabular(List<Map<String, Object>> rowMaps, List<String> columns,
                                    char delimiter, boolean includeHeaders)
    {
        StringBuilder builder = new StringBuilder();

        if (includeHeaders)
        {
            builder.append(String.join(String.valueOf(delimiter), columns));
            builder.append("\n");
        }

        TsvQuoter q = new TsvQuoter(delimiter);

        for (Map<String, Object> row : rowMaps)
        {
            List<String> values = new ArrayList<>();
            for (String name : columns)
            {
                String value = q.quoteValue(row.get(name));
                values.add(value);
            }
            builder.append(String.join(String.valueOf(delimiter), values));
            builder.append("\n");
        }
        return builder.toString();
    }

    /**
     * Used to quote values to be written to a TSV file
     * @see org.labkey.api.data.TSVWriter
     */
    public static class TsvQuoter
    {
        private static final char _chQuote = '"';
        private final char[] _escapedChars;

        public TsvQuoter(char delimiterChar)
        {
            _escapedChars = new char[] {'\r', '\n', _chQuote, delimiterChar};
        }

        public TsvQuoter()
        {
            this('\t');
        }

        public String quoteValue(Object o)
        {
            if (o == null)
                return "";

            String value = o.toString();

            String escaped = value;
            if (shouldQuote(value))
            {
                StringBuilder sb = new StringBuilder(value.length() + 10);
                sb.append(_chQuote);
                int i;
                int lastMatch = 0;

                while (-1 != (i = value.indexOf(_chQuote, lastMatch)))
                {
                    sb.append(value, lastMatch, i);
                    sb.append(_chQuote).append(_chQuote);
                    lastMatch = i + 1;
                }

                if (lastMatch < value.length())
                    sb.append(value.substring(lastMatch));

                sb.append(_chQuote);
                escaped = sb.toString();
            }

            return escaped;
        }

        protected boolean shouldQuote(String value)
        {
            int len = value.length();
            if (len == 0)
                return false;
            char firstCh = value.charAt(0);
            char lastCh = value.charAt(len - 1);
            if (Character.isSpaceChar(firstCh) || Character.isSpaceChar(lastCh))
                return true;
            return StringUtils.containsAny(value, _escapedChars);
        }
    }
}
