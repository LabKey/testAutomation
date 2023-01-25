package org.labkey.remoteapi.issues;

import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;

import java.util.ArrayList;
import java.util.List;

public class IssueResponse extends CommandResponse
{
    private final List<Long> _issueIds = new ArrayList<>();

    /**
     * Constructs a new CommandResponse, initialized with the provided response text and status code.
     *
     * @param text               The response text
     * @param statusCode         The HTTP status code
     * @param contentType        The response content type
     * @param json               The parsed JSONObject (or null if JSON was not returned)
     */
    public IssueResponse(String text, int statusCode, String contentType, JSONObject json)
    {
        super(text, statusCode, contentType, json);

        JSONArray issuesArray = (JSONArray)json.get("issues");
        for (int i=0; i< issuesArray.length(); i++)
        {
            _issueIds.add(issuesArray.getLong(i));
        }
    }

    public List<Long> getIssueIds()
    {
        return _issueIds;
    }
}
