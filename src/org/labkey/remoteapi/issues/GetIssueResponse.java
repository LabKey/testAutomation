package org.labkey.remoteapi.issues;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;

public class GetIssueResponse extends CommandResponse
{
    private final IssueResponseModel _issueModel;

    public GetIssueResponse(String text, int statucCode, String contentType, JSONObject json, GetIssueCommand sourceCommand)
    {
        super(text, statucCode, contentType, json, sourceCommand);

        // parse json into issueModel here
        _issueModel = new IssueResponseModel(json);
    }

    // expose response data in a getter here
    public IssueResponseModel getIssueModel()
    {
        return _issueModel;
    }
}
