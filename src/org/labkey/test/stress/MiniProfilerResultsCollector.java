package org.labkey.test.stress;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.GetCommand;
import org.labkey.remoteapi.miniprofiler.RequestInfo;
import org.labkey.remoteapi.miniprofiler.RequestsResponse;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.Timer;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MiniProfilerResultsCollector implements Simulation.ResultCollector<RequestInfo>
{
    private final Connection _connection;
    private final AbstractScenario.TsvResultsWriter<RequestInfo> _resultsWriter;
    private final String sessionId;
    private final long initialRequestId;
    private final AtomicBoolean lock = new AtomicBoolean(false);
    private final Map<Long, RequestInfo> requestInfos = new ConcurrentHashMap<>();
    private final AtomicInteger requestCount = new AtomicInteger(0);

    public MiniProfilerResultsCollector(Connection connection, AbstractScenario.TsvResultsWriter<RequestInfo> resultsWriter)
    {
        _connection = connection;
        _resultsWriter = resultsWriter;
        // Get initial request Id
        RequestsResponse sessionRequests = getRequestInfosFromServer();
        sessionId = sessionRequests.getSessionId();
        initialRequestId = sessionRequests.getRequestInfos().stream().map(RequestInfo::getId).max(Comparator.naturalOrder()).orElse(0L);
        requestInfos.clear();
    }

    public MiniProfilerResultsCollector(Connection connection)
    {
        this(connection, null);
    }

    @Override
    public void postRequest(Simulation.RequestResult requestResult) throws InterruptedException
    {
        requestCount.incrementAndGet();

        // Only allow one thread to collect session info
        if (!lock.getAndSet(true))
        {
            try
            {
                collectSessionRequestInfos();
            }
            finally
            {
                lock.set(false);
            }
        }
    }

    @Override
    public @NotNull Collection<RequestInfo> getResults()
    {
        collectSessionRequestInfos();
        try
        {
            Timer timer = new Timer(Duration.ofSeconds(10));
            while (!timer.isTimedOut() && requestCount.get() > requestInfos.size())
            {
                // Wait to get lingering request info
                Thread.sleep(500);
                collectSessionRequestInfos();
            }
        }
        catch (InterruptedException ignore)
        {
        }
        int missingInfos = requestCount.get() - requestInfos.size();
        if (missingInfos != 0)
        {
            TestLogger.warn("Didn't find request info for %d requests".formatted(missingInfos));
        }
        return requestInfos.values();
    }

    protected final Connection getConnection()
    {
        return _connection;
    }

    private void collectSessionRequestInfos()
    {
        RequestsResponse response = getRequestInfosFromServer();
        Assert.assertEquals("Session ID has changed during simulation", sessionId, response.getSessionId());
        List<RequestInfo> responseRequestInfos = response.getRequestInfos();
        if (!responseRequestInfos.isEmpty())
        {
            responseRequestInfos.forEach(ri -> {
                requestInfos.computeIfAbsent(ri.getId(), id -> processNewRequest(ri));
            });
        }
    }

    private RequestsResponse getRequestInfosFromServer()
    {
        try
        {
            GetCommand<RequestsResponse> command = getRequestsCommand(initialRequestId);
            return command.execute(_connection, null);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    // Allow subclasses to do something else with request infos (e.g. write to a file)
    protected RequestInfo processNewRequest(RequestInfo requestInfo)
    {
        return requestInfo;
    }

    @NotNull
    protected abstract GetCommand<RequestsResponse> getRequestsCommand(long requestIdForCommand);
}
