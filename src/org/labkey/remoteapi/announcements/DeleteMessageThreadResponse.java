package org.labkey.remoteapi.announcements;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.HasRequiredVersion;

public class DeleteMessageThreadResponse extends CommandResponse
{
    public DeleteMessageThreadResponse(String text, int statusCode, String contentType, JSONObject json,
                                       HasRequiredVersion hasRequiredVersion)
    {
        super(text, statusCode, contentType, json, hasRequiredVersion);
    }
}
