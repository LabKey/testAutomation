package org.labkey.test.util;

import org.apache.commons.io.IOUtils;
import org.labkey.serverapi.reader.TabLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
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

    /**
     * Create a new list containing the contents of all the provided lists.
     * @param lists lists to be combined
     * @return combined list
     * @param <T> type of list contents
     */
    @SafeVarargs
    public static <T> List<T> merge(Collection<T>... lists)
    {
        List<T> combined = new ArrayList<>();
        for (Collection<T> list : lists)
        {
            combined.addAll(list);
        }
        return combined;
    }
}
