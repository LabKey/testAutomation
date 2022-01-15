package org.labkey.remoteapi.issues;

import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.PostCommand;

import java.util.List;

public class IssueCommand extends PostCommand<IssueResponse>
{
    private IssueModel _issue;

    public IssueCommand()
    {
        super("issues", "issues");
    }

    @Override
    protected IssueResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new IssueResponse(text, status, contentType, json, this);
    }

    public void setIssue(IssueModel issue)
    {
        _issue = issue;
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject result = new JSONObject();
        JSONArray issueArray = new JSONArray();
        issueArray.put(_issue.toJSON());
        result.put("issues", issueArray);
        return result;
    }
}


