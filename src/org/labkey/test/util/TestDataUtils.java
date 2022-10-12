package org.labkey.test.util;

import org.apache.commons.io.IOUtils;
import org.labkey.serverapi.reader.TabLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
}
