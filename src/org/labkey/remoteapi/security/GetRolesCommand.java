package org.labkey.remoteapi.security;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;

public class GetRolesCommand extends Command<GetRolesResponse>
{
    public GetRolesCommand()
    {
        super("security", "getRoles");
    }

    protected GetRolesResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new GetRolesResponse(text, status, contentType, json, copy());
    }

    @Override
    public GetRolesCommand copy()
    {
        return new GetRolesCommand();
    }
}
