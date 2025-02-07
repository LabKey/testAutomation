package org.labkey.test.stress;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.miniprofiler.RequestInfo;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.TestLogger;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

public class ActivityRecorder
{
    private final RecentRequestsCollector recentRequestsCollector;

    public ActivityRecorder(Connection connection) throws IOException, CommandException
    {
        recentRequestsCollector = new RecentRequestsCollector(connection);
    }

    @LogMethod
    public <R> R recordActivity(@LoggedParam String description, Supplier<R> activity) throws IOException, CommandException
    {
        R result = activity.get();

        List<RequestInfo> recentRequests = recentRequestsCollector.getRecentRequests();
        TestLogger.log("%s triggered %s requests".formatted(description, recentRequests.size()));

        return result;
    }

    public void recordActivity(String description, Runnable activity) throws IOException, CommandException
    {
        recordActivity(description, () -> {
            activity.run();
            return null;
        });
    }

    public List<RequestInfo> skipRecentRequests() throws IOException, CommandException
    {
        List<RequestInfo> recentRequests = recentRequestsCollector.getRecentRequests();
        TestLogger.log("Skipping %s requests".formatted(recentRequests.size()));

        return recentRequests;
    }
}
