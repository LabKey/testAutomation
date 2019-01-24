package org.labkey.serverapi.reader;

import java.io.IOException;

public class CloseableFilteredIterator<T> extends FilteredIterator<T> implements CloseableIterator<T>
{
    private final CloseableIterator<T> _iter;

    public CloseableFilteredIterator(CloseableIterator<T> iter, Filter<T> filter)
    {
        super(iter, filter);
        _iter = iter;
    }

    public void close() throws IOException
    {
        _iter.close();
    }
}
