package org.labkey.remoteapi.issues;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandResponse;

import java.util.ArrayList;
import java.util.List;

public class IssueResponse extends CommandResponse
{
    private final Long _issueId;

    /**
     * Constructs a new CommandResponse, initialized with the provided
     * response text and status code.
     *
     * @param text          The response text
     * @param statusCode    The HTTP status code
     * @param contentType   The response content type
     * @param json          The parsed JSONObject (or null if JSON was not returned).
     * @param sourceCommand A copy of the command that created this response
     */
    public IssueResponse(String text, int statusCode, String contentType, JSONObject json, IssueCommand sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);

        JSONArray issuesArray = (JSONArray)json.get("issues");
        _issueId = ((Long)issuesArray.get(0));   // just take the first
    }

    public Long getIssueId()
    {
        return _issueId;
    }
}
