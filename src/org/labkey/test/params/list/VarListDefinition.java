package org.labkey.test.params.list;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.params.FieldDefinition;

import java.util.List;

public class VarListDefinition extends ListDefinition
{
    private static final String DOMAIN_KIND = "VarList";

    public VarListDefinition(String name)
    {
        super(name);
    }

    @Override
    public ListDefinition addField(@NotNull FieldDefinition field)
    {
        if (getKeyName() == null)
        {
            // Use first field as key
            setKeyName(field.getName());
        }
        return super.addField(field);
    }

    @Override
    public ListDefinition setFields(List<FieldDefinition> fields)
    {
        if (!fields.isEmpty() && getKeyName() == null)
        {
            // Use first field as key
            setKeyName(fields.get(0).getName());
        }
        return super.setFields(fields);
    }

    @NotNull
    @Override
    protected String getKind()
    {
        return DOMAIN_KIND;
    }
}
