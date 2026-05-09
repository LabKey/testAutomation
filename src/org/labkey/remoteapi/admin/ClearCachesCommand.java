package org.labkey.remoteapi.admin;

import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

import java.util.HashMap;
import java.util.Map;

public class ClearCachesCommand extends PostCommand<CommandResponse>
{
    private final boolean _clearCaches;
    private final boolean _gc;

    public ClearCachesCommand(boolean clearCaches, boolean gc)
    {
        super("admin", "clearCaches");
        _clearCaches = clearCaches;
        _gc = gc;
    }

    @Override
    protected Map<String, Object> createParameterMap()
    {
        Map<String, Object> params = new HashMap<>();
        if (_clearCaches)
            params.put("clearCaches", true);
        if (_gc)
            params.put("gc", true);
        return params;
    }
}
