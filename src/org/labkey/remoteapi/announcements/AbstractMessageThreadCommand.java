package org.labkey.remoteapi.announcements;

import org.json.JSONObject;
import org.labkey.remoteapi.PostCommand;

public abstract class AbstractMessageThreadCommand extends PostCommand<MessageThreadResponse>
{
    public AbstractMessageThreadCommand(String actionName)
    {
        super("announcements", actionName);
        setRequiredVersion(0);  // suppress for now
    }

    @Override
    protected MessageThreadResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new MessageThreadResponse(text, status, contentType, json);
    }
}
