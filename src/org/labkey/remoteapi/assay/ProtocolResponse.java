package org.labkey.remoteapi.assay;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandResponse;

public class ProtocolResponse extends CommandResponse
{
    private Protocol _protocol;

    public ProtocolResponse(String text, int statusCode, String contentType, JSONObject json, GetProtocolCommand sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);
        _protocol = new Protocol((JSONObject) json.get("data"));
    }

    public Protocol getProtocol()
    {
        return _protocol;
    }
}
