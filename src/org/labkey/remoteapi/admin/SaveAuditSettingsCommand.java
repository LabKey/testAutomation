package org.labkey.remoteapi.admin;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

public class SaveAuditSettingsCommand extends PostCommand<CommandResponse>
{
    private static final String ACTION_NAME = "saveAuditSettings";
    private static final String CONTROLLER_NAME = "audit";
    private boolean _requireUserComments = false;

    public SaveAuditSettingsCommand(boolean requireUserComments)
    {
        super(CONTROLLER_NAME, ACTION_NAME);
        _requireUserComments = requireUserComments;
    }

    public boolean isRequireUserComments()
    {
        return _requireUserComments;
    }

    public void setRequireUserComments(boolean requireUserComments)
    {
        this._requireUserComments = requireUserComments;
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject json = new JSONObject();
        json.put("requireUserComments", isRequireUserComments());

        return json;
    }
}
