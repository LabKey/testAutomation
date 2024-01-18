package org.labkey.remoteapi.announcements;

import org.json.JSONObject;

public class UpdateMessageThreadCommand extends AbstractMessageThreadCommand
{
    private final AnnouncementModel _announcementModel;

    public UpdateMessageThreadCommand(AnnouncementModel params)
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
