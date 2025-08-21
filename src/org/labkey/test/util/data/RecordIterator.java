package org.labkey.test.util.data;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;

public class RecordIterator implements Iterator<List<Object>>
{
    private final Collection<String> headers;
    private final Iterator<Map<String, Object>> rowIterator;

    private boolean firstRow = true;

    public <T> RecordIterator(@NotNull Collection<String> headers, @NotNull Iterator<Map<String, T>> rowIterator)
    {
        this.headers = Objects.requireNonNull(headers);
        Objects.requireNonNull(rowIterator);
        this.rowIterator = new Iterator<>()
        {
            @Override
            public boolean hasNext()
            {
                return rowIterator.hasNext();
            }

            @Override
            public Map<String, Object> next()
            {
                return Collections.unmodifiableMap(rowIterator.next());
            }
        };
    }

    public RecordIterator(@NotNull List<String> headers, @NotNull Supplier<Map<String, Object>> rowSupplier, final int rowCount)
    {
        this.headers = Objects.requireNonNull((Collection<String>) headers);
        this.rowIterator = new Iterator<>()
        {
            int count = 0;

            @Override
            public boolean hasNext()
            {
                return count < rowCount;
            }

            @Override
            public Map<String, Object> next()
            {
                count++;
                return rowSupplier.get();
            }
        };
    }

    public <T> RecordIterator(@NotNull Collection<String> headers, @NotNull List<Map<String, T>> rows)
    {
        this(headers, rows.iterator());
    }

    public <T> RecordIterator(@NotNull List<Map<String, T>> rows)
    {
        this(rows.get(0).keySet(), rows);
    }

    @Override
    public boolean hasNext()
    {
        return firstRow || rowIterator.hasNext();
    }

    @Override
    public List<Object> next()
    {
        if (!hasNext())
            throw new NoSuchElementException();

        if (firstRow)
        {
            firstRow = false;
            return List.copyOf(headers);
        }
        else
        {
            return rowMapToList(rowIterator.next());
        }
    }

    private List<Object> rowMapToList(Map<String, Object> row)
    {
        return headers.stream().map(h -> row.getOrDefault(h, "")).toList();
    }
}
