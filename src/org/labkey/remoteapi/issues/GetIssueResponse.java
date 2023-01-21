package org.labkey.remoteapi.issues;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.HasRequiredVersion;

public class GetIssueResponse extends CommandResponse
{
    private final IssueResponseModel _issueModel;

    public GetIssueResponse(String text, int statusCode, String contentType, JSONObject json, HasRequiredVersion hasRequiredVersion)
    {
        super(text, statusCode, contentType, json, hasRequiredVersion);

        // parse json into issueModel here
        _issueModel = new IssueResponseModel(json);
    }

    // expose response data in a getter here
    public IssueResponseModel getIssueModel()
    {
        return _issueModel;
    }
}
