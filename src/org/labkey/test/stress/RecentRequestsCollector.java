package org.labkey.test.stress;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.miniprofiler.RecentRequestsCommand;
import org.labkey.remoteapi.miniprofiler.RequestInfo;
import org.labkey.test.util.Crawler;

import java.io.IOException;
import java.util.List;

public class RecentRequestsCollector
{
    private final Connection _connection;
    private long lastRequestId = 0L;

    public RecentRequestsCollector(Connection connection) throws IOException, CommandException
    {
        _connection = connection;
        getRecentRequests(); // Prime 'lastRequestId'
    }

    public List<RequestInfo> getRecentRequests() throws IOException, CommandException
    {
        List<RequestInfo> requestInfos = new RecentRequestsCommand(lastRequestId).execute(_connection, null).getRequestInfos();
        lastRequestId = requestInfos.getLast().getId();
        return requestInfos.stream().filter(requestInfo -> HarConverter.EXCLUDED_ACTIONS.contains(new Crawler.ControllerActionId(requestInfo.getUrl()))).toList();
    }
}
