package org.labkey.serverapi.reader;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface Loader
{
    ColumnDescriptor[] getColumns() throws IOException;
    List<Map<String, Object>> load();
    CloseableIterator<Map<String, Object>> iterator();
}

