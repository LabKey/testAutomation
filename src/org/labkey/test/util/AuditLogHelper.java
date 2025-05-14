package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.labkey.test.pages.core.admin.ShowAuditLogPage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.Integer.parseInt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
        List<Map<String, Object>> events = getAuditLogsFromLKS(containerPath, auditEventName, List.of("NewRecordMap"), Collections.emptyList(), maxRows).getRows();
        assertEquals("Unexpected number of events", expectedDiffCounts.size(), events.size());
        for (int i = 0; i < expectedDiffCounts.size(); i++)
        {
            int expectedDiffCount = expectedDiffCounts.get(i);
            Map<String, Object> event = events.get(i);
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
     * Check for th expected number of diffs in the audit event for the last transactionId.
     * If an expectedEventCount is also provided, it will check that the number of events for that transactionId matches the expectedEventCount.
     */
    public void checkTimelineAuditEventDiffCountForLastTransaction(String containerPath, String auditEventName, int expectedDiffCount, @Nullable Integer expectedEventCount) throws IOException, CommandException
    {
        Integer transactionId = (Integer) getAuditLogsFromLKS(containerPath, auditEventName, List.of("TransactionId"), Collections.emptyList(), 1)
                .getRows().get(0).get("TransactionId");
        List<Filter> transactionFilter = List.of(new Filter("TransactionId", transactionId, Filter.Operator.EQUAL));
        int eventCount = getAuditLogsFromLKS(containerPath, auditEventName, List.of("NewRecordMap"), transactionFilter, null).getRows().size();
        if (expectedEventCount != null)
            assertEquals("Unexpected number of events for transactionId " + transactionId, expectedEventCount.intValue(), eventCount);
        List<Integer> expectedChangeCounts = Collections.nCopies(eventCount, expectedDiffCount);
        checkTimelineAuditEventDiffCount(containerPath, auditEventName, expectedChangeCounts);
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

    public boolean validateExpectedRowInDomainPropertyAuditLog(List<Map<String, Object>> domainPropertyEventRows, String propertyName, Map<String, String> expectedColumns, @Nullable Map<String, String> expectedComment)
    {
        boolean pass = true;

        for(Map<String, Object> row : domainPropertyEventRows)
        {

            if(getLogColumnValue(row, "propertyname").equals(propertyName))
            {
                TestLogger.log("Validate the columns for property '" + propertyName + "'.");
                for(String fieldName : expectedColumns.keySet())
                {
                    if(!getLogColumnValue(row, fieldName).equals(expectedColumns.get(fieldName)))
                    {
                        pass = false;
                        TestLogger.log("************** For field '" + fieldName + "' expected value '" + expectedColumns.get(fieldName) + "' found '" + row.get(fieldName) + "' **************");
                    }
                }

                if(null != expectedComment)
                {
                    TestLogger.log("Validate that the Comment field is as expected.");
                    Map<String, String> commentFieldValues = getDomainPropertyEventComment(row);
                    pass = validateCommentHasExpectedValues(commentFieldValues, expectedComment) && pass;
                }
            }

        }

        return pass;
    }

    private boolean validateCommentHasExpectedValues(Map<String, String> comment, Map<String, String> expected)
    {
        boolean pass = true;

        for(String key : expected.keySet())
        {
            if(!expected.get(key).equals(comment.get(key)))
            {
                TestLogger.log("************** Comment value does not contain expected value for field '" + key + "'. Expected '" + expected.get(key) + "' found '" + comment.get(key) + "'.  **************");
                pass = false;
            }
        }

        return pass;
    }

    public List<Map<String, Object>> getDomainPropertyEventsFromDomainEvents(String projectName, String domainName, @Nullable Set<Integer> ignoreIds)
    {
        List<Integer> domainEventIds = getDomainEventIds(projectName, domainName, ignoreIds);

        TestLogger.log("Get all of the Domain Property Events for '" + domainName + "' that are linked to the domain events.");
        List<Map<String, Object>> domainPropertyEventRows = getDomainPropertyEventLog(domainName, domainEventIds);
        TestLogger.log("Number of 'Domain Property Event' log entries: " + domainPropertyEventRows.size());

        return domainPropertyEventRows;
    }

    public List<Integer> getDomainEventIds(String projectName, String domainName, @Nullable Collection<Integer> ignoreIds)
    {
        List<Map<String, Object>> domainAuditEventAllRows = getDomainEventLog(projectName, domainName, ignoreIds);

        List<Integer> domainEventIds = new ArrayList<>();
        domainAuditEventAllRows.forEach((event)->domainEventIds.add(getLogColumnIntValue(event, "rowid")));

        TestLogger.log("Number of 'Domain Event' log entries for '" + domainName + "': " + domainEventIds.size());

        return domainEventIds;
    }

    public Map<String, Object> getLastDomainEvent(String projectName, String domainName)
    {
        return getDomainEventLog(projectName, domainName, null).get(0);
    }

    public Integer getLastDomainEventId(String projectName, String domainName)
    {
        return getLogColumnIntValue(getLastDomainEvent(projectName, domainName), "rowid");
    }

    public String getLastDomainEventComment(String projectName, String domainName)
    {
        return getLogColumnValue(getLastDomainEvent(projectName, domainName), "comment");
    }

    public List<String> getLastDomainPropertyValues(String projectName, String domainName, String columnName)
    {
        Integer lastDomainEventId = getLastDomainEventId(projectName, domainName);
        List<Map<String, Object>> allRows = getDomainPropertyEventLog(domainName, Collections.singletonList(lastDomainEventId));
        List<String> domainPropEventComments = new ArrayList<>();
        allRows.forEach((event)->domainPropEventComments.add(getLogColumnValue(event, columnName)));
        return domainPropEventComments;
    }

    public List<String> getLastDomainPropertyComment(String projectName, String domainName)
    {
        return getLastDomainPropertyValues(projectName, domainName, "comment");
    }

    public List<String> getDomainEventComments(String projectName, String domainName, @Nullable Collection<Integer> ignoreIds)
    {
        List<Map<String, Object>> domainAuditEventAllRows = getDomainEventLog(projectName, domainName, ignoreIds);

        List<String> domainEventComments = new ArrayList<>();
        domainAuditEventAllRows.forEach((event)->domainEventComments.add(getLogColumnValue(event, "comment")));
        return domainEventComments;
    }

    public Set<Integer> getDomainEventIdsFromPropertyEvents(List<Map<String, Object>> domainPropertyEventRows)
    {
        Set<Integer> domainEventIds = new HashSet<>();

        for(Map<String, Object> row : domainPropertyEventRows)
        {
            domainEventIds.add(getLogColumnIntValue(row, "domaineventid"));
        }

        return domainEventIds;
    }

    private List<Map<String, Object>> getDomainEventLog(String projectName, String domainName, @Nullable Collection<Integer> ignoreIds)
    {
        TestLogger.log("Get a list of the Domain Events for project '" + projectName + "'. ");

        Connection cn = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("auditLog", "DomainAuditEvent");
        cmd.setRequiredVersion(9.1);
        cmd.setColumns(Arrays.asList("rowid", "created", "createdby", "impersonatedby", "projectid", "domainuri", "domainname", "comment"));
        cmd.addFilter("projectid/DisplayName", projectName, Filter.Operator.EQUAL);
        if(null != ignoreIds)
        {
            StringBuilder stringBuilder = new StringBuilder();
            ignoreIds.forEach((id)->{
                if(!stringBuilder.isEmpty())
                    stringBuilder.append(";");
                stringBuilder.append(id);
            });
            cmd.addFilter("rowId", stringBuilder, Filter.Operator.NOT_IN);
        }
        cmd.setContainerFilter(ContainerFilter.AllFolders);
        cmd.setSorts(Arrays.asList(new Sort("RowId", Sort.Direction.DESCENDING)));

        List<Map<String, Object>> domainAuditEventAllRows = executeSelectCommand(cn, cmd);
        TestLogger.log("Number of 'Domain Event' log entries for '" + projectName + "': " + domainAuditEventAllRows.size());

        TestLogger.log("Filter the list to look only at '" + domainName + "'.");
        List<Map<String, Object>> domainAuditEventRows = new ArrayList<>();

        for(Map<String, Object> row : domainAuditEventAllRows)
        {
            if(getLogColumnValue(row, "domainname").toLowerCase().trim().equals(domainName.toLowerCase().trim()))
                domainAuditEventRows.add(row);
        }

        return domainAuditEventRows;
    }

    private List<Map<String, Object>> getDomainPropertyEventLog(String domainName, @Nullable List<Integer> eventIds)
    {
        Connection cn = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("auditLog", "DomainPropertyAuditEvent");
        cmd.setRequiredVersion(9.1);
        cmd.setColumns(Arrays.asList("Created", "CreatedBy", "ImpersonatedBy", "propertyname", "action", "domainname", "domaineventid", "Comment"));
        cmd.addFilter("domainname", domainName, Filter.Operator.EQUAL);

        if(null != eventIds)
        {
            StringBuilder stringBuilder = new StringBuilder();
            eventIds.forEach((id)->{
                if(!stringBuilder.isEmpty())
                    stringBuilder.append(";");
                stringBuilder.append(id);
            });
            cmd.addFilter("domaineventid/rowid", stringBuilder, Filter.Operator.IN);
        }

        cmd.setContainerFilter(ContainerFilter.AllFolders);

        return executeSelectCommand(cn, cmd);
    }

    private List<Map<String, Object>> executeSelectCommand(Connection cn, SelectRowsCommand cmd)
    {
        List<Map<String, Object>> rowsReturned = new ArrayList<>();
        try
        {
            SelectRowsResponse response = cmd.execute(cn, "/");
            TestLogger.log("Number of rows: " + response.getRowCount());
            rowsReturned.addAll(response.getRows());
        }
        catch(IOException | CommandException ex)
        {
            // Just fail here, don't toss the exception up the stack.
            fail("There was a command exception when getting the log: " + ex);
        }

        return rowsReturned;
    }

    private Map<String, String> getDomainPropertyEventComment(Map<String, Object> row)
    {
        String comment = getLogColumnValue(row, "Comment");

        String[] commentAsArray = comment.split(";");

        Map<String, String> fieldComments = new HashMap<>();

        for (String s : commentAsArray)
        {
            String[] fieldValue = s.split(":");

            // If the split on the ':' produced more than two entries in the array it most likely means that the
            // comment for that property had a : in it. So treat the first entry as the field name and then concat the
            // other fields together.
            // For example the ConditionalFormats field will log the following during an update:
            // ConditionalFormats: old: <none>, new: 1;
            // And a create of a Lookup will log as:
            // Lookup: [Schema: lists, Query: LookUp01];
            StringBuilder sb = new StringBuilder();
            sb.append(fieldValue[1].trim());

            for (int j = 2; j < fieldValue.length; j++)
            {
                sb.append(":");
                sb.append(fieldValue[j]);
            }

            fieldComments.put(fieldValue[0].trim(), sb.toString());
        }

        return fieldComments;
    }

    private String getLogColumnValue(Map<String, Object> rowEntry, String columnName)
    {
        try
        {
            return ((Map<String, Object>) rowEntry.get(columnName)).get("value").toString();
        }
        catch(JSONException je)
        {
            // Just fail here, don't toss the exception up the stack.
            throw new IllegalArgumentException(je);
        }
    }

    private Integer getLogColumnIntValue(Map<String, Object> rowEntry, String columnName)
    {
        try
        {
            return parseInt(getLogColumnValue(rowEntry, columnName));
        }
        catch(JSONException je)
        {
            // Just fail here, don't toss the exception up the stack.
            throw new IllegalArgumentException(je);
        }
    }
}
