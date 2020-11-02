package org.labkey.test.params.list;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.params.FieldDefinition;

import java.util.List;

public class IntListDefinition extends ListDefinition
{
    private static final String AUTO_INCREMENT_DOMAIN_KIND = "IntList";

    public IntListDefinition(String name, String autoIncrementKeyName)
    {
        super(name);
        setKeyName(autoIncrementKeyName);
    }

    @Override
    public List<FieldDefinition> getFields()
    {
        List<FieldDefinition> fields = super.getFields();
        fields.add(0, new FieldDefinition(getKeyName(), FieldDefinition.ColumnType.Integer).setPrimaryKey(true));
        return fields;
    }

    @NotNull
    @Override
    protected String getKind()
    {
        return AUTO_INCREMENT_DOMAIN_KIND;
    }
}
