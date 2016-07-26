package org.labkey.test.components.html;

public interface FormItem<T>
{
    T get();
    void set(T value);
}
