package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.labkey.test.pages.core.admin.ShowAuditLogPage;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class AuditLogHelper
{
    private final WebDriverWrapper _wrapper;
    private final ConnectionSupplier _connectionSupplier;

    public AuditLogHelper(WebDriverWrapper wrapper, ConnectionSupplier connectionSupplier)
    {
        _wrapper = wrapper;
        _connectionSupplier = connectionSupplier;
    }

    public AuditLogHelper(WebDriverWrapper wrapper)
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
            ShowAdminPage.beginAt(_wrapper).clickAuditLog();
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
     * @param containerPath Path of the LK container to use for the select command.
     * @param auditEventName Name of the audit event to filter on. Example 'SamplesWorkflowAuditEvent'.
     * @param columnNames The name of the columns to return.
     * @param filters The filters to be applied
     * @return A rowResponse with the query logs.
     * @throws IOException Can be thrown by the SelectRowsCommand.
     * @throws CommandException Can be thrown by the SelectRowsCommand.
     */
    public SelectRowsResponse getAuditLogsFromLKS(String containerPath, String auditEventName, List<String> columnNames,
                                                         List<Filter> filters, @Nullable Integer maxRows) throws IOException, CommandException
    {
        SelectRowsCommand cmd = new SelectRowsCommand("auditLog", auditEventName);
        cmd.setColumns(columnNames);
        cmd.addFilter("ProjectId/Name", _wrapper.getCurrentProject(), Filter.Operator.EQUAL);
        filters.forEach(cmd::addFilter);
        if (maxRows != null)
            cmd.setMaxRows(maxRows);
        return cmd.execute(_connectionSupplier.get(), containerPath);
    }

    public List<Map<String, Object>> getAuditLogsForTransactionId(String containerPath, String auditEventName, List<String> columnNames,
                                                         Integer transactionId) throws IOException, CommandException
    {
        List<Filter> transactionFilter = List.of(new Filter("TransactionId", transactionId, Filter.Operator.EQUAL));
        return getAuditLogsFromLKS(containerPath, auditEventName, columnNames, transactionFilter, null).getRows();
    }

    public void checkAuditEventValuesForTransactionId(String containerPath, String auditEventName, Integer transactionId, int rowCount, Map<String, Object> expectedValues) throws IOException, CommandException
    {
        List<String> columnNames = expectedValues.keySet().stream().map(Object::toString).toList();
        List<Map<String, Object>> events = getAuditLogsForTransactionId(containerPath, auditEventName, columnNames, transactionId);
        assertEquals("Unexpected number of events for transactionId " + transactionId, rowCount, events.size());
        for (Map<String, Object> event : events)
        {
            for (String key : columnNames)
                assertEquals("Event value for " + key + " not as expected", expectedValues.get(key), event.get(key));
        }
    }

    /**
     * Check the number of diffs in the audit event. This is a helper function to check the number of diffs in the
     * newRecordMap for an audit entry. If a transactionId is provided, it will check all rows for that
     * transactionId. If no transactionId is provided, it will check just the latest row.
     */
    public void checkTimelineAuditEventDiffCount(String containerPath, List<Integer> expectedDiffCounts) throws IOException, CommandException
    {
        checkTimelineAuditEventDiffCount(containerPath, getAuditEventNameFromURL(), expectedDiffCounts);
    }
    public void checkTimelineAuditEventDiffCount(String containerPath, String auditEventName, List<Integer> expectedDiffCounts) throws IOException, CommandException
    {
        Integer maxRows = expectedDiffCounts.size();
        List<Map<String, Object>> events = getAuditLogsFromLKS(containerPath, auditEventName, List.of("InventoryUpdateType", "NewRecordMap"), Collections.emptyList(), maxRows).getRows();
        assertEquals("Unexpected number of events", expectedDiffCounts.size(), events.size());
        for (int i = 0; i < expectedDiffCounts.size(); i++)
        {
            Map<String, Object> event = events.get(i);
            boolean isInventoryUpdateType = event.get("InventoryUpdateType") != null;
            int expectedDiffCount = isInventoryUpdateType ? 0 : expectedDiffCounts.get(i);
            String dataChangesStr = (String) event.get("NewRecordMap");
            String[] dataChanges = dataChangesStr != null ? dataChangesStr.split("&") : new String[0];

            // filter out SampleStateLabel as that is not a change, it is added for display purposes
            dataChanges = Stream.of(dataChanges).filter(s -> !s.toLowerCase().startsWith("samplestatelabel=")).toArray(String[]::new);
            // filter out RowId as that is not a change, it is added for display purposes
            dataChanges = Stream.of(dataChanges).filter(s -> !s.toLowerCase().startsWith("rowid=")).toArray(String[]::new);

            TestLogger.log("Audit record data changes diff count check: " + dataChangesStr);
            assertEquals("Audit record data changes did not include the expected number of diffs, expected " + expectedDiffCount + " but was " + dataChanges.length + ": " + dataChangesStr,
                    expectedDiffCount, dataChanges.length);
        }
    }

    /**
     * Check for the expected number of diffs in the audit event for the last transactionId.
     * If an expectedEventCount is also provided, it will check that the number of events for that transactionId matches the expectedEventCount.
     * @return transactionId
     */
    public Integer checkTimelineAuditEventDiffCountForLastTransaction(String containerPath, String auditEventName, int expectedDiffCount, @Nullable Integer expectedEventCount) throws IOException, CommandException
    {
        Integer transactionId = (Integer) getAuditLogsFromLKS(containerPath, auditEventName, List.of("TransactionId"), Collections.emptyList(), 1)
                .getRows().get(0).get("TransactionId");
        List<Filter> transactionFilter = List.of(new Filter("TransactionId", transactionId, Filter.Operator.EQUAL));
        int eventCount = getAuditLogsFromLKS(containerPath, auditEventName, List.of("NewRecordMap"), transactionFilter, null).getRows().size();
        if (expectedEventCount != null)
            assertEquals("Unexpected number of events for transactionId " + transactionId, expectedEventCount.intValue(), eventCount);
        List<Integer> expectedChangeCounts = Collections.nCopies(eventCount, expectedDiffCount);
        checkTimelineAuditEventDiffCount(containerPath, auditEventName, expectedChangeCounts);
        return transactionId;
    }

    public String getAuditEventNameFromURL()
    {
        if (isSamplesRoute())
            return "SampleTimelineEvent";
        else if (isDataClassRoute())
            return "SourcesAuditEvent";
        return null;
    }

    public boolean isSamplesRoute()
    {
        URL url = _wrapper.getURL();
        if (url != null)
            return url.toString().toLowerCase().contains("#/samples")
                    || url.toString().toLowerCase().contains("#/media/mixturebatches")
                    || url.toString().toLowerCase().contains("#/media/rawmaterials");
        return false;
    }

    public boolean isSourcesRoute()
    {
        return Objects.requireNonNull(_wrapper.getURL().toString()).contains("#/sources");
    }

    public boolean isDataClassRoute()
    {
        if (isSourcesRoute()) return true;

        URL url = _wrapper.getURL();
        if (url != null)
            return url.toString().toLowerCase().contains("#/registry")
                    || url.toString().toLowerCase().contains("#/media/ingredients")
                    || url.toString().toLowerCase().contains("#/media/mixtures");
        return false;
    }

    public interface ConnectionSupplier
    {
        Connection get() throws IOException, CommandException;
    }
}
