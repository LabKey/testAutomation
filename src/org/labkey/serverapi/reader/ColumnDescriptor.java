package org.labkey.serverapi.reader;

import org.apache.commons.beanutils.Converter;

import java.lang.reflect.Method;

public class ColumnDescriptor
{
    public ColumnDescriptor()
    {
    }

    public ColumnDescriptor(String name)
    {
        this.name = name;
        this.clazz = String.class;
    }

    public ColumnDescriptor(String name, Class type)
    {
        this.name = name;
        this.clazz = type;
    }

    public ColumnDescriptor(String name, Class type, Object defaultValue)
    {
        this.name = name;
        this.clazz = type;
        this.missingValues = defaultValue;
    }

    public Class clazz = String.class;
    public String name = null;
    public String propertyURI = null;
    public boolean load = true;
    public boolean isProperty = false; //Load as a class property
    public Object missingValues = null;
    public Object errorValues = null;
    public Converter converter = null;
    public Method setter = null;



    public String toString()
    {
        return name + ":" + clazz.getSimpleName();
    }

    public String getColumnName()
    {
        return name;
    }
}