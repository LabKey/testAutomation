package org.labkey.remoteapi.announcements;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.HasRequiredVersion;

public class MessageThreadResponse extends CommandResponse
{
    private final AnnouncementModel _announcementModel;

    public MessageThreadResponse(String text, int statusCode, String contentType, JSONObject json, HasRequiredVersion hasRequiredVersion)
    {
        super(text, statusCode, contentType, json, hasRequiredVersion);
        _announcementModel = new AnnouncementModel((JSONObject) json.get("data"));
    }

    public AnnouncementModel getAnnouncementModel()
    {
        return _announcementModel;
    }
}
