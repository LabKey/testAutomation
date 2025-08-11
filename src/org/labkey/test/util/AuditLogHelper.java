package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
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

    public enum AuditEvent
    {
        SAMPLE_TIMELINE_EVENT("SampleTimelineEvent"),
        SOURCES_AUDIT_EVENT("SourcesAuditEvent"),
        INVENTORY_AUDIT_EVENT("InventoryAuditEvent"),
        LIST_AUDIT_EVENT("ListAuditEvent"),
        EXPERIMENT_AUDIT_EVENT("ExperimentAuditEvent"),
        SAMPLE_WORKFLOW_AUDIT_EVENT("SamplesWorkflowAuditEvent");

        private final String _name;

        AuditEvent(String name)
        {
            _name = name;
        }

        public String getName()
        {
            return _name;
        }
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
     * @param maxRows The maximum number of rows to return. If null, all rows for the provided filters will be returned.
     * @param containerFilter The container filter to be applied. If null, default is ContainerFilter.Current.
     * @return A rowResponse with the query logs.
     * @throws IOException Can be thrown by the SelectRowsCommand.
     * @throws CommandException Can be thrown by the SelectRowsCommand.
     */
    public SelectRowsResponse getAuditLogsFromLKS(String containerPath, AuditEvent auditEventName, List<String> columnNames,
                                                         List<Filter> filters, @Nullable Integer maxRows, @Nullable ContainerFilter containerFilter) throws IOException, CommandException
    {
        SelectRowsCommand cmd = new SelectRowsCommand("auditLog", auditEventName.getName());
        cmd.setColumns(columnNames);
        cmd.addFilter("ProjectId/Name", _wrapper.getCurrentProject(), Filter.Operator.EQUAL);
        filters.forEach(cmd::addFilter);
        if (maxRows != null)
            cmd.setMaxRows(maxRows);
        if (containerFilter != null)
            cmd.setContainerFilter(containerFilter);
        return cmd.execute(_connectionSupplier.get(), containerPath);
    }

    public List<Map<String, Object>> getAuditLogsForTransactionId(String containerPath, AuditEvent auditEventName, List<String> columnNames,
                                                         Integer transactionId, @Nullable ContainerFilter containerFilter) throws IOException, CommandException
    {
        List<Filter> transactionFilter = List.of(new Filter("TransactionId", transactionId, Filter.Operator.EQUAL));
        return getAuditLogsFromLKS(containerPath, auditEventName, columnNames, transactionFilter, null, containerFilter).getRows();
    }

    public void checkAuditEventValuesForTransactionId(String containerPath, AuditEvent auditEventName, Integer transactionId, int rowCount, Map<String, Object> expectedValues) throws IOException, CommandException
    {
        List<String> columnNames = expectedValues.keySet().stream().map(Object::toString).toList();
        List<Map<String, Object>> events = getAuditLogsForTransactionId(containerPath, auditEventName, columnNames, transactionId, ContainerFilter.CurrentAndSubfolders);
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
        checkAuditEventDiffCount(containerPath, getAuditEventNameFromURL(), expectedDiffCounts);
    }

    public void checkAuditEventDiffCount(String containerPath, AuditEvent auditEventName, List<Integer> expectedDiffCounts) throws IOException, CommandException
    {
        checkAuditEventDiffCount(containerPath, auditEventName, Collections.emptyList(), expectedDiffCounts);
    }

    public void checkAuditEventDiffCount(String containerPath, AuditEvent auditEventName, List<Filter> filters, List<Integer> expectedDiffCounts) throws IOException, CommandException
    {
        Integer maxRows = expectedDiffCounts.size();
        List<Map<String, Object>> events = getAuditLogsFromLKS(containerPath, auditEventName, List.of("InventoryUpdateType", "NewRecordMap"), filters, maxRows, ContainerFilter.CurrentAndSubfolders).getRows();
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

    public Integer getLastTransactionId(String containerPath, AuditEvent auditEventName) throws IOException, CommandException
    {
        List<Map<String, Object>> events = getAuditLogsFromLKS(containerPath, auditEventName, List.of("TransactionId"), Collections.emptyList(), 1, ContainerFilter.CurrentAndSubfolders).getRows();
        return events.size() == 1 ? (Integer) events.get(0).get("TransactionId") : null;
    }

    /**
     * Check for the expected number of diffs in the audit event for the last transactionId.
     * If an expectedEventCount is also provided, it will check that the number of events for that transactionId matches the expectedEventCount.
     * @return transactionId
     */
    public Integer checkAuditEventDiffCountForLastTransaction(String containerPath, AuditEvent auditEventName, int expectedDiffCount,
                                                                      @Nullable Integer expectedEventCount) throws IOException, CommandException
    {
        Integer transactionId = getLastTransactionId(containerPath, auditEventName);
        List<Filter> transactionFilter = List.of(new Filter("TransactionId", transactionId, Filter.Operator.EQUAL));
        int eventCount = getAuditLogsFromLKS(containerPath, auditEventName, List.of("NewRecordMap"), transactionFilter, null, ContainerFilter.CurrentAndSubfolders).getRows().size();
        if (expectedEventCount != null)
            assertEquals("Unexpected number of events for transactionId " + transactionId, expectedEventCount.intValue(), eventCount);
        List<Integer> expectedChangeCounts = Collections.nCopies(eventCount, expectedDiffCount);
        checkAuditEventDiffCount(containerPath, auditEventName, transactionFilter, expectedChangeCounts);
        return transactionId;
    }

    public AuditEvent getAuditEventNameFromURL()
    {
        if (isSamplesRoute())
            return AuditEvent.SAMPLE_TIMELINE_EVENT;
        else if (isDataClassRoute())
            return AuditEvent.SOURCES_AUDIT_EVENT;
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

    public boolean validateDetailAuditLog(DetailedAuditEventRow expectedAuditDetail, DetailedAuditEventRow actualAuditDetail)
    {
        boolean pass = true;
        for (String prop : propertyAuditColumns)
        {
            String expectedValue = expectedAuditDetail.getColumn(prop);
            if (expectedValue != null)
            {
                String actualValue = actualAuditDetail.getColumn(prop);

                if (StringUtils.isEmpty(expectedValue) && actualValue == null)
                    continue;

                if (!expectedValue.equalsIgnoreCase(actualValue))
                {
                    pass = false;
                    TestLogger.log(prop + " is not as expected. Expected: " + expectedValue + ", Actual: " + actualValue);
                }
            }
        }
        return pass;
    }

    public boolean validateDomainPropertiesAuditLog(String domainName, Integer domainEventId, Map<String, DetailedAuditEventRow> expectedAuditDetails)
    {
        if (expectedAuditDetails == null)
            return true;
        Map<String, DetailedAuditEventRow> actualAuditDetails = getDomainPropertyEvents(domainName, domainEventId);
        boolean pass = true;
        if (expectedAuditDetails.size() != actualAuditDetails.size())
        {
            pass = false;
            TestLogger.log(String.format("Number of DomainPropertyAuditEvent events not as expected. Expected %d, Actual %d.", expectedAuditDetails.size(), actualAuditDetails.size()));
        }

        for (String key : expectedAuditDetails.keySet())
        {
            DetailedAuditEventRow expectedAuditDetail = expectedAuditDetails.get(key);
            DetailedAuditEventRow actualAuditDetail = actualAuditDetails.get(key);
            if (actualAuditDetail == null)
            {
                pass = false;
                TestLogger.log("Field " + key + " is missing DomainPropertyAuditEvent.");
            }
            else
                pass = pass && validateDetailAuditLog(expectedAuditDetail, actualAuditDetail);
        }

        return pass;
    }

    public boolean validateLastDomainAuditEvents(String domainName, String projectName, DetailedAuditEventRow expectedDomainEvent, Map<String, DetailedAuditEventRow> expectedDomainPropertyEvents)
    {
        DetailedAuditEventRow latestDomainEvent = getLastDomainEvent(projectName, domainName);
        if (latestDomainEvent == null)
        {
            TestLogger.log(String.format("No DomainAuditEvent found for domain '%s' in project '%s'.", domainName, projectName));
            return false;
        }

        boolean pass = validateDetailAuditLog(expectedDomainEvent, latestDomainEvent);
        return pass && validateDomainPropertiesAuditLog(domainName, latestDomainEvent.rowId, expectedDomainPropertyEvents);
    }

    public List<Integer> getDomainEventIds(String projectName, String domainName, @Nullable Collection<Integer> ignoreIds)
    {
        List<DetailedAuditEventRow> domainAuditEventAllRows = getDomainAuditEventLog(projectName, domainName, ignoreIds, null);

        List<Integer> domainEventIds = new ArrayList<>();
        domainAuditEventAllRows.forEach((event)->domainEventIds.add(event.rowId));

        TestLogger.log("Number of 'Domain Event' log entries for '" + domainName + "': " + domainEventIds.size());

        return domainEventIds;
    }

    public @Nullable DetailedAuditEventRow getLastDomainEvent(String projectName, String domainName)
    {
        List<DetailedAuditEventRow> eventLog = getDomainAuditEventLog(projectName, domainName, null, 1);
        if (eventLog.isEmpty())
            return null;
        return eventLog.get(0);
    }

    public @Nullable Integer getLastDomainEventId(String projectName, String domainName)
    {
        DetailedAuditEventRow event = getLastDomainEvent(projectName, domainName);
        if (event == null)
            return null;
        return event.rowId;
    }

    public static List<String> propertyAuditColumns = List.of("type", "comment", "usercomment", "oldvalues", "newvalues", "datachanges");
    public record DetailedAuditEventRow(Integer rowId, String keyValue, String type, String comment, String userComment, String oldValues, String newValues, String dataChanges)
    {
        public String getColumn(String columnName)
        {
            String columnname = columnName.toLowerCase();
            return switch (columnname)
            {
                case "keyvalue" -> keyValue;
                case "rowid" -> rowId + "";
                case "type" -> type;
                case "comment" -> comment;
                case "usercomment" -> userComment;
                case "oldvalues" -> oldValues;
                case "newvalues" -> newValues;
                case "datachanges" -> dataChanges;
                default -> null;
            };
        }

        public String getLogString()
        {
            return "Comment: " + comment + "\nOldValue:" + oldValues + "\nNewValue:" + newValues;
        }
    }

    public @NotNull Map<String, DetailedAuditEventRow> getDomainPropertyEvents(String domainName, Integer domainEventId)
    {
        if (domainEventId == null)
            return Collections.emptyMap();

        List<Map<String, Object>> allRows = getDomainPropertyEventLog(domainName, Collections.singletonList(domainEventId));
        Map<String, DetailedAuditEventRow> domainPropEventComments = new HashMap<>();
        allRows.forEach((event)->{
            Integer rowId = getLogColumnIntValue(event, "RowId");
            String propertyName = getLogColumnValue(event, "PropertyName");
            String action = getLogColumnValue(event, "Action");
            String comment = getLogColumnValue(event, "Comment");
            String userComment = getLogColumnValue(event, "UserComment");
            String oldValue = getLogColumnValue(event, "oldValues");
            String newValue = getLogColumnValue(event, "newValues");
            String dataChanges = getLogColumnDisplayValue(event, "dataChanges");
            domainPropEventComments.put(propertyName, new DetailedAuditEventRow(rowId, propertyName, action, comment, userComment, oldValue, newValue, dataChanges));
        });

        return domainPropEventComments;
    }

    public Map<String, DetailedAuditEventRow> getLastDomainPropertyEvents(String projectName, String domainName)
    {
        Integer lastDomainEventId = getLastDomainEventId(projectName, domainName);
        return getDomainPropertyEvents(domainName, lastDomainEventId);
    }

    public List<String> getLastDomainPropertyValues(String projectName, String domainName, String columnName)
    {
        return getLastDomainPropertyEvents(projectName, domainName).values().stream().map(values -> values.getColumn(columnName)).toList();
    }

    public List<String> getDomainEventComments(String projectName, String domainName, @Nullable Collection<Integer> ignoreIds)
    {
        return getDomainAuditEventLog(projectName, domainName, ignoreIds, null).stream().map(event -> event.comment).toList();
    }

    public Set<Integer> getDomainEventIdsFromPropertyEvents(List<Map<String, Object>> domainPropertyEventRows)
    {
        Set<Integer> domainEventIds = new HashSet<>();

        for (Map<String, Object> row : domainPropertyEventRows)
        {
            domainEventIds.add(getLogColumnIntValue(row, "domaineventid"));
        }

        return domainEventIds;
    }

    private List<DetailedAuditEventRow> getDomainAuditEventLog(String projectName, String domainName, @Nullable Collection<Integer> ignoreIds, @Nullable Integer maxRows)
    {
        TestLogger.log("Get a list of the Domain Events for project '" + projectName + "'. ");
        domainName = domainName.trim();

        Connection cn = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("auditLog", "DomainAuditEvent");
        cmd.setRequiredVersion(9.1);
        cmd.setColumns(Arrays.asList("rowid", "domainuri", "domainname", "comment", "usercomment", "oldvalues", "newvalues", "datachanges"));
        cmd.addFilter("projectid/DisplayName", projectName, Filter.Operator.EQUAL);
        cmd.addFilter("domainname", domainName, Filter.Operator.EQUAL);
        if (null != ignoreIds)
        {
            String rowIds = StringUtils.join(ignoreIds, ";");
            cmd.addFilter("rowId", rowIds, Filter.Operator.NOT_IN);
        }
        cmd.setContainerFilter(ContainerFilter.AllFolders);
        cmd.setSorts(List.of(new Sort("RowId", Sort.Direction.DESCENDING)));

        if (maxRows != null)
            cmd.setMaxRows(maxRows);

        List<Map<String, Object>> domainAuditEventAllRows = executeSelectCommand(cn, cmd);
        TestLogger.log(String.format("Number of Domain Event log entries for domain '%s' in '%s': %d", domainName, projectName, domainAuditEventAllRows.size()));

        List<DetailedAuditEventRow> domainAuditEventRows = new ArrayList<>();

        for (Map<String, Object> row : domainAuditEventAllRows)
        {
            String eventDomainName = getLogColumnValue(row, "domainname");
            Integer rowId = getLogColumnIntValue(row, "rowid");
            String comment = getLogColumnValue(row, "comment");
            String userComment = getLogColumnValue(row, "usercomment");
            String oldValue = getLogColumnValue(row, "oldvalues");
            String newValue = getLogColumnValue(row, "newvalues");
            String dataChanges = getLogColumnDisplayValue(row, "dataChanges");
            domainAuditEventRows.add(new DetailedAuditEventRow(rowId, eventDomainName, null, comment, userComment, oldValue, newValue, dataChanges));
        }

        return domainAuditEventRows;
    }

    private List<Map<String, Object>> getDomainPropertyEventLog(String domainName, @Nullable List<Integer> eventIds)
    {
        Connection cn = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("auditLog", "DomainPropertyAuditEvent");
        cmd.setRequiredVersion(9.1);
        cmd.setColumns(Arrays.asList("Created", "CreatedBy", "ImpersonatedBy", "propertyname", "action", "domainname", "domaineventid", "Comment", "UserComment", "oldvalues", "newvalues", "datachanges"));
        cmd.addFilter("domainname", domainName, Filter.Operator.EQUAL);

        if (null != eventIds)
        {
            String rowIds = StringUtils.join(eventIds, ";");
            cmd.addFilter("domaineventid/rowid", rowIds, Filter.Operator.IN);
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
        catch (IOException | CommandException ex)
        {
            // Just fail here, don't toss the exception up the stack.
            fail("There was a command exception when getting the log: " + ex);
        }

        return rowsReturned;
    }

    private Map<String, String> getDomainPropertyEventComment(Map<String, Object> row)
    {
        String comment = getLogColumnValue(row, "Comment");
        if (comment != null)
            return null;

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

    private String getLogColumnValue(Map<String, Object> rowEntry, String columnName, String valueType)
    {
        try
        {
            Map<String, Object> val = ((Map<String, Object>) rowEntry.get(columnName));
            if (val == null)
                return null;
            Object value = val.get(valueType);
            if (value == null)
                return null;
            return value.toString();
        }
        catch (JSONException je)
        {
            // Just fail here, don't toss the exception up the stack.
            throw new IllegalArgumentException(je);
        }
    }

    private String getLogColumnValue(Map<String, Object> rowEntry, String columnName)
    {
        return getLogColumnValue(rowEntry, columnName, "value");
    }

    private String getLogColumnDisplayValue(Map<String, Object> rowEntry, String columnName)
    {
        return getLogColumnValue(rowEntry, columnName, "displayValue");
    }

    private Integer getLogColumnIntValue(Map<String, Object> rowEntry, String columnName)
    {
        try
        {
            String strVal = getLogColumnValue(rowEntry, columnName);
            if (strVal == null)
                return null;
            return parseInt(strVal);
        }
        catch (JSONException je)
        {
            // Just fail here, don't toss the exception up the stack.
            throw new IllegalArgumentException(je);
        }
    }
}
