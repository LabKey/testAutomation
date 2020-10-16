package org.labkey.remoteapi.announcements;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandResponse;

public class MessageThreadResponse extends CommandResponse
{
    private final TestAnnouncementModel _announcementModel;

    public MessageThreadResponse(String text, int statusCode, String contentType, JSONObject json, Command sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);
        _announcementModel = new TestAnnouncementModel((JSONObject) json.get("data"));
    }

    public TestAnnouncementModel getAnnouncementModel()
    {
        return _announcementModel;
    }
}
