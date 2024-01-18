package org.labkey.remoteapi.announcements;

import org.json.JSONObject;
import org.labkey.remoteapi.PostCommand;

public class GetDiscussionsCommand extends PostCommand<GetDiscussionsResponse>
{
    private final String _discussionSrcIdentifier;

    public GetDiscussionsCommand(String discussionSrcIdentifier)
    {
        super("announcements", "getDiscussions");
        setRequiredVersion(0); // suppress version check; announcements does not grok this
        _discussionSrcIdentifier = discussionSrcIdentifier;
    }

    @Override
    protected GetDiscussionsResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new GetDiscussionsResponse(text, status, contentType, json);
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject result = new JSONObject();
        result.put("discussionSrcIdentifier", getDiscussionSrcIdentifier());
        return result;
    }

    public String getDiscussionSrcIdentifier()
    {
        return _discussionSrcIdentifier;
    }
}
