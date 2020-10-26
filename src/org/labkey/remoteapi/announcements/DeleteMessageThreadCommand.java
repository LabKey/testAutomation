package org.labkey.remoteapi.announcements;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.PostCommand;

public class DeleteMessageThreadCommand extends PostCommand<DeleteMessageThreadResponse>
{
    private String _entityId;
    private Long _rowId;

    public DeleteMessageThreadCommand(String entityId)
    {
        super("announcements", "deleteThread");
        _entityId = entityId;
    }

    public DeleteMessageThreadCommand(Long rowId)
    {
        super("announcements", "deleteThread");
        _rowId = rowId;
    }

    @Override
    public JSONObject getJsonObject()
    {
        setRequiredVersion(0);
        JSONObject result = new JSONObject();
        if (getRowId() != null) result.put("rowId", getRowId());
        else if (getEntityId() != null) result.put("entityId", getEntityId());
        return result;
    }

    @Override
    protected DeleteMessageThreadResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new DeleteMessageThreadResponse(text, status, contentType, json, this);
    }

    public void setEntityId(String entityId)
    {
        _entityId = entityId;
    }

    public String getEntityId()
    {
        return _entityId;
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
