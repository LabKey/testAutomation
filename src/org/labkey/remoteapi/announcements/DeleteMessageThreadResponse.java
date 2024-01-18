package org.labkey.remoteapi.announcements;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;

public class DeleteMessageThreadResponse extends CommandResponse
{
    public DeleteMessageThreadResponse(String text, int statusCode, String contentType, JSONObject json)
    {
        super(text, statusCode, contentType, json);
    }
}
