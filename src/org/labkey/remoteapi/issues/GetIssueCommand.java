package org.labkey.remoteapi.issues;

import org.json.JSONObject;
import org.labkey.remoteapi.PostCommand;

public class GetIssueCommand extends PostCommand<GetIssueResponse>
{
    private final Long _issueId;

    public GetIssueCommand(Long issueId)
    {
        super("issues", "getIssue");
        _issueId = issueId;
    }

    @Override
    protected GetIssueResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new GetIssueResponse(text, status, contentType, json, this);
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject result = new JSONObject();
        result.put("issueId", _issueId);
        return result;
    }
}
