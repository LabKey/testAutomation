package org.labkey.remoteapi.miniprofiler;

public class SessionRequestsCommand extends BaseRequestsCommand
{
    public SessionRequestsCommand(Long requestId)
    {
        super("sessionRequests", requestId);
    }
}
