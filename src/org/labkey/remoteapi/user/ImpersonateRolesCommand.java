package org.labkey.remoteapi.user;

import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ImpersonateRolesCommand extends PostCommand<CommandResponse>
{
    private final Map<String, Object> _parameters = new HashMap<>();

    public ImpersonateRolesCommand(String... roleNames)
    {
        super("user", "impersonateRoles.api");
        _parameters.put("roleNames", Arrays.asList(roleNames));
    }

    @Override
    protected Map<String, Object> createParameterMap()
    {
        return new HashMap<>(_parameters); // Return a copy
    }
}
