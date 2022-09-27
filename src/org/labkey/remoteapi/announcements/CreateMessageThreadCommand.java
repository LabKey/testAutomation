package org.labkey.remoteapi.announcements;

import org.json.JSONObject;

public class CreateMessageThreadCommand extends AbstractMessageThreadCommand
{
    private final AnnouncementModel _announcementModel;
    private boolean _reply = false;


    public CreateMessageThreadCommand(AnnouncementModel params)
    {
        super("createThread");
        _announcementModel = params;
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject result = new JSONObject();

        if (_announcementModel == null)
        {
            result.put("thread", new JSONObject());
        }
        else
        {
            result.put("thread", _announcementModel.toJSON());
        }
        result.put("reply", _reply);
        return result;
    }

    public CreateMessageThreadCommand setReply(boolean reply)
    {
        _reply = reply;
        return this;
    }

    public boolean getReply()
    {
        return _reply;
    }

}
