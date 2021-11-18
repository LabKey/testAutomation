package org.labkey.test.util;

import org.apache.tika.io.IOUtils;
import org.labkey.serverapi.reader.TabLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class TestDataUtils
{
    public static List<Map<String, Object>> rowMapsFromTsv(String tsvString) throws IOException
    {
        try (InputStream dataStream = IOUtils.toInputStream(tsvString))
        {
            return new TabLoader.TsvFactory().createLoader(dataStream, true).load();
        }
    }
}
