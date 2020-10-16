package org.labkey.remoteapi.announcements;

import org.json.simple.JSONObject;

public class UpdateMessageThreadCommand extends AbstractMessageThreadCommand
{
    private final TestAnnouncementModel _announcementModel;

    public UpdateMessageThreadCommand(TestAnnouncementModel params)
    {
        super("updateThread");
        _announcementModel = params;
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject result = new JSONObject();
        result.put("thread", _announcementModel.toJSON());
        return result;
    }
}
