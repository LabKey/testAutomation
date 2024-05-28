package org.labkey.test.params.list;

import org.jetbrains.annotations.NotNull;

public class VarListDefinition extends ListDefinition
{
    private static final String DOMAIN_KIND = "VarList";

    public VarListDefinition(String name)
    {
        super(name);
    }

    @NotNull
    @Override
    protected String getKind()
    {
        return DOMAIN_KIND;
    }

    @Override
    protected String getKeyType()
    {
        return "Varchar";
    }
}
