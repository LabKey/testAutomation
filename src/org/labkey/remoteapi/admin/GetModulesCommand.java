package org.labkey.remoteapi.admin;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;

public class GetModulesCommand extends Command<GetModulesResponse>
{
    public GetModulesCommand()
    {
        super("admin", "getModules");
    }

    protected GetModulesResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new GetModulesResponse(text, status, contentType, json, copy());
    }

    @Override
    public GetModulesCommand copy()
    {
        return new GetModulesCommand();
    }
}
