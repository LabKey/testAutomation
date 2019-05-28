package org.labkey.serverapi.reader;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class FilteredIterator<T> implements Iterator<T>
{
    private final Iterator<T> _iterator;
    private final Filter<T> _filter;
    private T _next;

    public FilteredIterator(Iterator<T> iterator, Filter<T> filter)
    {
        _iterator = iterator;
        _filter = filter;
        toNext();
    }

    public boolean hasNext()
    {
        return _next != null;
    }

    public T next()
    {
        if (_next == null)
            throw new NoSuchElementException();

        T returnValue = _next;
        toNext();

        return returnValue;
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    private void toNext()
    {
        _next = null;

        while (_iterator.hasNext())
        {
            T item = _iterator.next();
            if (item != null && _filter.accept(item))
            {
                _next = item;
                break;
            }
        }
    }
}
