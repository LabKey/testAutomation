package org.labkey.test.params.list;

public class IntListDefinition extends ListDefinition
{
    public IntListDefinition(String name, String autoIncrementKeyName)
    {
        super(name);
        setAutoIncrementKeyName(autoIncrementKeyName);
    }
}
