package org.labkey.remoteapi.announcements;

import org.json.JSONObject;

public class GetMessageThreadCommand extends AbstractMessageThreadCommand
{
    private final AnnouncementModel _announcementModel;

    public GetMessageThreadCommand(AnnouncementModel params)
    {
        super("getThread");
       _announcementModel = params;
    }

    public GetMessageThreadCommand(String entityId)
    {
        super("getThread");
        _announcementModel = new AnnouncementModel();
        _announcementModel.setEntityId(entityId);
    }

    public GetMessageThreadCommand(Integer rowId)
    {
        super("getThread");
        _announcementModel = new AnnouncementModel();
        _announcementModel.setRowId(rowId);
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject result = new JSONObject();

        if (_announcementModel.getEntityId() != null)
            result.put("entityId", _announcementModel.getEntityId());
        else if (_announcementModel.getRowId() != null)
            result.put("rowId", _announcementModel.getRowId());

        return result;
    }
}
