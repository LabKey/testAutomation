package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.core.admin.ShowAuditLogPage;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class AuditLogHelper
{
    private final LabKeySiteWrapper _wrapper;
    private final ConnectionSupplier _connectionSupplier;

    public AuditLogHelper(LabKeySiteWrapper wrapper, ConnectionSupplier connectionSupplier)
    {
        _wrapper = wrapper;
        _connectionSupplier = connectionSupplier;
    }

    public AuditLogHelper(LabKeySiteWrapper wrapper)
    {
        this(wrapper, wrapper::createDefaultConnection);
    }

    public Integer getLatestAuditRowId(String auditTable) throws IOException, CommandException
    {
        String rowId = "rowId";

        SelectRowsCommand selectRows = new SelectRowsCommand("auditLog", auditTable);
        selectRows.setColumns(List.of(rowId));
        selectRows.setSorts(List.of(new Sort(rowId, Sort.Direction.DESCENDING)));
        selectRows.setMaxRows(1);
        selectRows.setContainerFilter(ContainerFilter.AllFolders);

        SelectRowsResponse response = selectRows.execute(_connectionSupplier.get(), null);
        List<Map<String, Object>> rows = response.getRows();
        if (rows.isEmpty())
        {
            return 0;
        }
        return (Integer) rows.get(0).get(rowId);
    }

    public DataRegionTable beginAtAuditEventView(String auditTable, Integer rowIdCutoff)
    {
        return ShowAuditLogPage.beginAt(_wrapper, auditTable, rowIdCutoff).getLogTable();
    }

    public DataRegionTable goToAuditEventView(String eventType)
    {
        if (!_wrapper.isTextPresent("Audit Log"))
        {
            _wrapper.goToAdminConsole().clickAuditLog();
        }

        if (!_wrapper.getSelectedOptionText(Locator.name("view")).equals(eventType))
        {
            _wrapper.doAndWaitForPageToLoad(() -> _wrapper.selectOptionByText(Locator.name("view"), eventType));
        }
        return new DataRegionTable("query", _wrapper);
    }

    /**
     * Get the audit logs from the LabKey server filtered to the given project.
     *
     * @param projectName Name of the LK project to filter to.
     * @param folderName Name of the LK folder to filter to.
     * @param auditEventName Name of the audit event to filter on. Example 'SamplesWorkflowAuditEvent'.
     * @param columnNames The name of the columns to return.
     * @param filters The filters to be applied
     * @return A rowResponse with the query logs.
     * @throws IOException Can be thrown by the SelectRowsCommand.
     * @throws CommandException Can be thrown by the SelectRowsCommand.
     */
    public static SelectRowsResponse getAuditLogsFromLKS(String projectName, String folderName, String auditEventName, List<String> columnNames,
                                                         List<Filter> filters, @Nullable Integer maxRows) throws IOException, CommandException
    {
        SelectRowsCommand cmd = new SelectRowsCommand("auditLog", auditEventName);
        cmd.setColumns(columnNames);
        cmd.addFilter("ProjectId/DisplayName", projectName, Filter.Operator.EQUAL);
        filters.forEach(cmd::addFilter);
        if (maxRows != null)
            cmd.setMaxRows(maxRows);

        String formattedContainerPath = projectName;
        if (!formattedContainerPath.startsWith("/"))
            formattedContainerPath = "/" + formattedContainerPath;
        if (!folderName.equalsIgnoreCase(projectName))
            formattedContainerPath = formattedContainerPath + "/" + folderName;

        return cmd.execute(WebTestHelper.getRemoteApiConnection(), formattedContainerPath);
    }

    /**
     * Check the number of diffs in the audit event. This is a helper function to check the number of diffs in the
     * newRecordMap for an audit entry. If a transactionId is provided, it will check all rows for that
     * transactionId. If no transactionId is provided, it will check just the latest row.
     */
    public static void checkTimelineAuditEventDiffCount(String projectName, String folderName, List<Integer> expectedDiffCounts) throws IOException, CommandException
    {
        checkTimelineAuditEventDiffCount(projectName, folderName, getAuditEventNameFromURL(), expectedDiffCounts);
    }
    public static void checkTimelineAuditEventDiffCount(String projectName, String folderName, String auditEventName, List<Integer> expectedDiffCounts) throws IOException, CommandException
    {
        Integer maxRows = expectedDiffCounts.size();
        List<Map<String, Object>> events = getAuditLogsFromLKS(projectName, folderName, auditEventName, List.of("NewRecordMap"), Collections.emptyList(), maxRows).getRows();
        for (int i = 0; i < expectedDiffCounts.size(); i++)
        {
            Integer expectedDiffCount = expectedDiffCounts.get(i);
            Map<String, Object> event = events.get(i);
            String dataChangesStr = (String) event.get("NewRecordMap");
            String[] dataChanges = dataChangesStr != null ? dataChangesStr.split("&") : new String[0];
            // filter out SampleStateLabel as that is not a change, it is added for display purposes
            dataChanges = Stream.of(dataChanges).filter(s -> !s.toLowerCase().startsWith("samplestatelabel=")).toArray(String[]::new);
            BaseWebDriverTest.getCurrentTest().checker().verifyEquals("Audit record data changes did not include the expected number of diffs, expected " + expectedDiffCount + " but was " + dataChanges.length + ": " + dataChangesStr,
                    expectedDiffCount, dataChanges.length);
        }
    }

    /**
     * Check for th expected number of diffs in the audit event for the last transactionId.
     * If an expectedEventCount is also provided, it will check that the number of events for that transactionId matches the expectedEventCount.
     */
    public static void checkTimelineAuditEventDiffCountForLastTransaction(String projectName, String folderName, int expectedDiffCount, @Nullable Integer expectedEventCount) throws IOException, CommandException
    {
        checkTimelineAuditEventDiffCountForLastTransaction(projectName, folderName, getAuditEventNameFromURL(), expectedDiffCount, expectedEventCount);
    }
    public static void checkTimelineAuditEventDiffCountForLastTransaction(String projectName, String folderName, String auditEventName, int expectedDiffCount, @Nullable Integer expectedEventCount) throws IOException, CommandException
    {
        Integer transactionId = (Integer) getAuditLogsFromLKS(projectName, folderName, auditEventName, List.of("TransactionId"), Collections.emptyList(), 1)
                .getRows().get(0).get("TransactionId");
        List<Filter> transactionFilter = List.of(new Filter("TransactionId", transactionId, Filter.Operator.EQUAL));
        int eventCount = getAuditLogsFromLKS(projectName, folderName, auditEventName, List.of("NewRecordMap"), transactionFilter, null).getRows().size();
        if (expectedEventCount != null)
            BaseWebDriverTest.getCurrentTest().checker().verifyEquals("Unexpected number of events for transactionId " + transactionId, expectedEventCount.intValue(), eventCount);
        List<Integer> expectedChangeCounts = Collections.nCopies(eventCount, expectedDiffCount);
        checkTimelineAuditEventDiffCount(projectName, folderName, auditEventName, expectedChangeCounts);
    }

    public static String getAuditEventNameFromURL()
    {
        if (isSamplesRoute())
            return "SampleTimelineEvent";
        else if (isDataClassRoute())
            return "SourcesAuditEvent";
        return null;
    }

    public static boolean isSamplesRoute()
    {
        return Objects.requireNonNull(BaseWebDriverTest.getCurrentTest().getURL().toString()).contains("#/samples/");
    }

    public static boolean isSourcesRoute()
    {
        return Objects.requireNonNull(BaseWebDriverTest.getCurrentTest().getURL().toString()).contains("#/sources/");
    }

    public static boolean isDataClassRoute()
    {
        return isSourcesRoute() || Objects.requireNonNull(BaseWebDriverTest.getCurrentTest().getURL().toString()).contains("#/registry/");
    }

    public interface ConnectionSupplier
    {
        Connection get() throws IOException, CommandException;
    }
}
