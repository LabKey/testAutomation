package org.labkey.test.util;

import java.util.function.Supplier;

/**
 * Wraps another Supplier, invoking it lazily and caching its results for subsequent calls to get().<br>
 * Intended to be used as an alternative to null-checking and populating a member variable.<br>
 * Similar to {@link org.labkey.test.selenium.LazyWebElement} but for other types.
 * <pre>{@code
 * final CachingSupplier<T> item = new CachingSupplier<>(this::computeItem);
 * T getItem() {
 *     return item.get();
 * }
 * }</pre>
 * <pre>{@code
 * // Old pattern
 * T item;
 * T getItem() {
 *     if (item == null)
 *     {
 *         item = computeItem();
 *     }
 *     return item;
 * }
 * }</pre>
 */
public class CachingSupplier<T> implements Supplier<T>
{
    private final Supplier<T> _factory;
    private boolean _invoked = false;
    private T _value;

    public CachingSupplier(Supplier<T> factory)
    {
        _factory = factory;
    }

    @Override
    public T get()
    {
        if (!_invoked)
        {
            _value = _factory.get();
            _invoked = true;
        }
        return _value;
    }
}
