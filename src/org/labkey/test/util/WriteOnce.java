package org.labkey.test.util;

import java.util.Objects;

/**
 * Simple class to store effectively <code>final</code> objects generated during tests' <code>@BeforeClass</code>
 * @param <T> type to be stashed
 */
public class WriteOnce<T>
{
    private T _o;

    public T get()
    {
        return _o;
    }

    public synchronized void set(T o)
    {
        if (_o != null)
        {
            if (!Objects.equals(_o, o))
            {
                throw new IllegalStateException("Shared data already stored.");
            }
        }
        else
        {
            _o = o;
        }
    }
}
