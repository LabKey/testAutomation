package org.labkey.test.params.list;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.util.DomainUtils.DomainKind;

public class VarListDefinition extends ListDefinition
{

    public VarListDefinition(String name)
    {
        super(name);
    }

    @NotNull
    @Override
    protected String getKind()
    {
        return DomainKind.VarList.name();
    }

    @Override
    protected String getKeyType()
    {
        return "Varchar";
    }
}
