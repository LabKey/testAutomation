package org.labkey.remoteapi.announcements;

import org.json.simple.JSONObject;

public class GetMessageThreadCommand extends AbstractMessageThreadCommand
{
    private final TestAnnouncementModel _announcementModel;

    public GetMessageThreadCommand(TestAnnouncementModel params)
    {
        super("getThread");
       _announcementModel = params;
    }

    public GetMessageThreadCommand(String entityId)
    {
        super("getThread");
        _announcementModel = new TestAnnouncementModel();
        _announcementModel.setEntityId(entityId);
    }

    public GetMessageThreadCommand(Long rowId)
    {
        super("getThread");
        _announcementModel = new TestAnnouncementModel();
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
