package org.labkey.remoteapi.security;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

public class DeletePolicyCommand extends PostCommand<CommandResponse>
{
    String resourceId;

    public DeletePolicyCommand(String resourceId)
    {
        super("security", "deletePolicy");
        this.resourceId = resourceId;
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject result = new JSONObject();

        result.put("resourceId", resourceId);
        return result;
    }

    public DeletePolicyCommand(PostCommand source)
    {
        super(source);
    }
}
