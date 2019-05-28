package org.labkey.serverapi.reader;

public interface Filter<T>
{
    boolean accept(T object);
}
