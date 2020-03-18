package org.labkey.test.params.property;

import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.domain.CreateDomainCommand;
import org.labkey.remoteapi.domain.Domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for domain properties that might be created via the 'createDomain' API
 */
public abstract class DomainProps
{
    protected abstract @NotNull Domain getDomainDesign();
    protected abstract @NotNull String getKind();
    protected abstract @NotNull Map<String, Object> getOptions();

    public final CreateDomainCommand getCreateCommand()
    {
        CreateDomainCommand command = new CreateDomainCommand(getKind(), getDomainDesign().getName());
        command.setOptions(new HashMap<>(getOptions()));
        command.setDomainDesign(getDomainDesign());
        return command;
    }
}
