package org.labkey.remoteapi.miniprofiler;

public class RecentRequestsCommand extends BaseRequestsCommand
{
    public RecentRequestsCommand(Long requestId)
    {
        super("recentRequests", requestId);
    }
}
