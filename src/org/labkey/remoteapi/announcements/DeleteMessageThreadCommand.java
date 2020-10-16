package org.labkey.remoteapi.announcements;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.PostCommand;

public class DeleteMessageThreadCommand extends PostCommand<DeleteMessageThreadResponse>
{
    private String _id;
    private Long _rowId;

    public DeleteMessageThreadCommand(String id)
    {
        super("announcements", "deleteThread");
        _id = id;
    }

    public DeleteMessageThreadCommand(Long rowId)
    {
        super("announcements", "deleteThread");
        _rowId = _rowId;
    }

    @Override
    public JSONObject getJsonObject()
    {
        setRequiredVersion(0);
        JSONObject result = new JSONObject();
        if (getId() != null) result.put("rowId", _rowId);
        else if (getRowId() != null) result.put("entityId", _id);
        return result;
    }


    @Override
    protected DeleteMessageThreadResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new DeleteMessageThreadResponse(text, status, contentType, json, this);
    }

    public void setId(String id)
    {
        _id=id;
    }
    public String getId()
    {
        return _id;
    }

    public void setRowId(Long rowId)
    {
        _rowId = rowId;
    }
    public Long getRowId()
    {
        return _rowId;
    }
}
