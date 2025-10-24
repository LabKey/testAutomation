package org.labkey.test.tests.list;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.BaseRowsCommand;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.MoveRowsCommand;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.TruncateTableCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.Hosting;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.params.list.VarListDefinition;
import org.labkey.test.util.AuditLogHelper;
import org.labkey.test.util.DomainUtils;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.TestUser;
import org.labkey.test.util.query.MoveRowsResponse;
import org.labkey.test.util.query.QueryApiHelper;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.labkey.test.util.AuditLogHelper.AuditEvent.LIST_AUDIT_EVENT;
import static org.labkey.test.util.PermissionsHelper.AUTHOR_ROLE;
import static org.labkey.test.util.PermissionsHelper.EDITOR_ROLE;
import static org.labkey.test.util.PermissionsHelper.READER_ROLE;

@Category({Daily.class, Data.class, Hosting.class}) // Matches ListTest for now
public class ListMoveRowsTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ListMoveRowsTest";
    private static final String PROJECT_PATH = "/" + PROJECT_NAME;
    private static final String SUBFOLDER_A_NAME = "SubfolderA";
    private static final String SUBFOLDER_A_PATH = "/" + PROJECT_NAME + "/" + SUBFOLDER_A_NAME;
    private static final String SUBFOLDER_MINOR_A_NAME = "MinorA";
    private static final String SUBFOLDER_MINOR_A_PATH = SUBFOLDER_A_PATH + "/" + SUBFOLDER_MINOR_A_NAME;
    private static final String SUBFOLDER_B_NAME = "SubfolderB";
    private static final String SUBFOLDER_B_PATH = "/" + PROJECT_NAME + "/" + SUBFOLDER_B_NAME;
    private static final String LIST_SCHEMA = "lists";

    private static final TestUser AUTHOR_USER = new TestUser("author@listmoverows.test");
    private static final TestUser READER_USER = new TestUser("reader@listmoverows.test");
    private static final TestUser SUBFOLDER_EDITOR_USER = new TestUser("sub.editor@listmoverows.test");

    private static final String attachmentFieldName = TestDataGenerator.randomFieldName("Attachment", null, DomainUtils.DomainKind.IntList);
    private static final String booleanFieldName = TestDataGenerator.randomFieldName("Boolean", null, DomainUtils.DomainKind.IntList);
    private static final String dateFieldName = TestDataGenerator.randomFieldName("Date", null, DomainUtils.DomainKind.IntList);
    private static final String dateTimeFieldName = TestDataGenerator.randomFieldName("DateTime", null, DomainUtils.DomainKind.IntList);
    private static final String decimalFieldName = TestDataGenerator.randomFieldName("Decimal", null, DomainUtils.DomainKind.IntList);
    private static final String integerFieldName = TestDataGenerator.randomFieldName("Integer", null, DomainUtils.DomainKind.IntList);

    private static final String autoIncrementKeyFieldName = TestDataGenerator.randomFieldName("AutoIncrementKey", null, DomainUtils.DomainKind.IntList);
    private static final String integerKeyFieldName = TestDataGenerator.randomFieldName("IntegerKey", null, DomainUtils.DomainKind.IntList);
    private static final String stringKeyFieldName = TestDataGenerator.randomFieldName("StringKey", null, DomainUtils.DomainKind.VarList);

    private static final File SAMPLE_DATA_FILE = TestFileUtils.getSampleData("lists/ListImportFields.txt");

    private static IntListDefinition AUTO_INCREMENT_LIST;
    private static IntListDefinition INTEGER_KEY_LIST;
    private static VarListDefinition STRING_KEY_LIST;

    private record UpdateCounts(int fileAttachmentsMoved, int listAuditEventsCreated, int listAuditEventsMoved, int listRecords, int queryAuditEventsMoved) { }
    private static final UpdateCounts NO_UPDATE = new UpdateCounts(0, 0, 0, 0, 0);

    private final AuditLogHelper _auditLogHelper = new AuditLogHelper(this);

    @BeforeClass
    public static void setupProject() throws Exception
    {
        ListMoveRowsTest init = getCurrentTest();
        init.doSetup();
    }

    private void doSetup() throws Exception
    {
        // Create folders
        {
            _containerHelper.createProject(getProjectName(), null);
            _containerHelper.createSubfolder(getProjectName(), SUBFOLDER_A_NAME);
            _containerHelper.createSubfolder(getProjectName(), SUBFOLDER_B_NAME);
            _containerHelper.createSubfolder(SUBFOLDER_A_PATH, SUBFOLDER_MINOR_A_NAME);
        }

        // Configure users
        {
            AUTHOR_USER.create(this)
                    .setInitialPassword()
                    .addPermission(AUTHOR_ROLE, PROJECT_PATH)
                    .addPermission(AUTHOR_ROLE, SUBFOLDER_A_PATH)
                    .addPermission(AUTHOR_ROLE, SUBFOLDER_B_PATH)
                    .addPermission(AUTHOR_ROLE, SUBFOLDER_MINOR_A_PATH);
            READER_USER.create(this)
                    .setInitialPassword()
                    .addPermission(READER_ROLE, PROJECT_PATH);
            SUBFOLDER_EDITOR_USER.create(this)
                    .setInitialPassword()
                    .addPermission(READER_ROLE, PROJECT_PATH)
                    .addPermission(EDITOR_ROLE, SUBFOLDER_A_PATH)
                    .addPermission(EDITOR_ROLE, SUBFOLDER_B_PATH)
                    .addPermission(EDITOR_ROLE, SUBFOLDER_MINOR_A_PATH);
        }

        // Create lists
        {
            var conn = createDefaultConnection();
            var fields = getFields();

            // List keyed by auto-increment
            var autoKeyListName = DomainUtils.DomainKind.IntList.randomName("AUTO");
            AUTO_INCREMENT_LIST = (IntListDefinition) new IntListDefinition(autoKeyListName, autoIncrementKeyFieldName)
                    .setFields(fields);
            AUTO_INCREMENT_LIST.getCreateCommand().execute(conn, PROJECT_PATH);
            AUTO_INCREMENT_LIST.setListId(resolveListId(conn, autoKeyListName, PROJECT_PATH));

            // List keyed by integer (not auto-increment)
            var intKeyField = new FieldDefinition(integerKeyFieldName, FieldDefinition.ColumnType.Integer);
            var intListFields = new ArrayList<FieldDefinition>();
            intListFields.add(intKeyField);
            intListFields.addAll(fields);
            var intKeyListName = DomainUtils.DomainKind.IntList.randomName("INT");
            INTEGER_KEY_LIST = (IntListDefinition) new IntListDefinition(intKeyListName)
                    .setKeyName(intKeyField.getName())
                    .setFields(intListFields);
            INTEGER_KEY_LIST.getCreateCommand().execute(conn, PROJECT_PATH);
            INTEGER_KEY_LIST.setListId(resolveListId(conn, intKeyListName, PROJECT_PATH));

            // List keyed by string
            var stringKeyField = new FieldDefinition(stringKeyFieldName);
            var varListFields = new ArrayList<FieldDefinition>();
            varListFields.add(stringKeyField);
            varListFields.addAll(fields);
            var stringKeyListName = DomainUtils.DomainKind.VarList.randomName("STR");
            STRING_KEY_LIST = (VarListDefinition) new VarListDefinition(stringKeyListName)
                    .setFields(varListFields)
                    .setKeyName(stringKeyField.getName());
            STRING_KEY_LIST.getCreateCommand().execute(conn, PROJECT_PATH);
            STRING_KEY_LIST.setListId(resolveListId(conn, stringKeyListName, PROJECT_PATH));
        }
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        _userHelper.deleteUsers(afterTest, AUTHOR_USER, READER_USER, SUBFOLDER_EDITOR_USER);
    }

    @Test
    public void testAutoIntListMove() throws Exception
    {
        truncateList(AUTO_INCREMENT_LIST);

        int totalRows = 100;
        int numRowsWithAttachmentValue = 5;
        var response = addRows(AUTO_INCREMENT_LIST, getProjectName(), totalRows, numRowsWithAttachmentValue);

        successfullyMoveRows(AUTO_INCREMENT_LIST, response.getRows());
    }

    @Test
    public void testIntListMove() throws Exception
    {
        truncateList(INTEGER_KEY_LIST);

        int totalRows = 100;
        int numRowsWithAttachmentValue = 5;
        var response = addRows(INTEGER_KEY_LIST, getProjectName(), totalRows, numRowsWithAttachmentValue);

        successfullyMoveRows(INTEGER_KEY_LIST, response.getRows());
    }

    @Test
    public void testVarListMove() throws Exception
    {
        truncateList(STRING_KEY_LIST);

        int totalRows = 100;
        int numRowsWithAttachmentValue = 5;
        var response = addRows(STRING_KEY_LIST, getProjectName(), totalRows, numRowsWithAttachmentValue);

        successfullyMoveRows(STRING_KEY_LIST, response.getRows());
    }

    @Test
    public void testPermissions() throws Exception
    {
        truncateList(AUTO_INCREMENT_LIST);
        var projectResponse = addRows(AUTO_INCREMENT_LIST, PROJECT_PATH, 10, 0);
        var subfolderRows = 10;
        var subfolderFileAttachments = 2;
        var subfolderResponse = addRows(AUTO_INCREMENT_LIST, SUBFOLDER_A_PATH, subfolderRows, subfolderFileAttachments);

        // Verify author permissions
        {
            impersonate(AUTHOR_USER.getEmail());

            // Author should not be able to move rows from subfolder to project
            String expectedError = "User does not have permission to perform this operation.";
            moveRowsExpectingError(AUTO_INCREMENT_LIST, SUBFOLDER_A_PATH, PROJECT_PATH, subfolderResponse.getRows(), expectedError);

            // Author should not be able to move rows from a project to a subfolder
            moveRowsExpectingError(AUTO_INCREMENT_LIST, PROJECT_PATH, SUBFOLDER_A_PATH, projectResponse.getRows(), expectedError);

            stopImpersonating();
        }

        // Verify reader permissions
        {
            impersonate(READER_USER.getEmail());

            // Reader should not be able to move rows from project to subfolder
            String expectedError = String.format("You do not have permission to move rows from '%s' to the target container: %s.", AUTO_INCREMENT_LIST.getName(), SUBFOLDER_A_PATH);
            moveRowsExpectingError(AUTO_INCREMENT_LIST, PROJECT_PATH, SUBFOLDER_A_PATH, projectResponse.getRows(), expectedError);

            // Reader should not be able to move rows from subfolder to project
            expectedError = "User does not have permission to perform this operation.";
            moveRowsExpectingError(AUTO_INCREMENT_LIST, SUBFOLDER_A_PATH, PROJECT_PATH, subfolderResponse.getRows(), expectedError);

            stopImpersonating();
        }

        // Verify editor permissions
        {
            impersonate(SUBFOLDER_EDITOR_USER.getEmail());

            // Subfolder editor should not be able to move rows from subfolder to project where they can only read
            String expectedError = String.format("You do not have permission to move rows from '%s' to the target container: %s.", AUTO_INCREMENT_LIST.getName(), PROJECT_PATH);
            moveRowsExpectingError(AUTO_INCREMENT_LIST, SUBFOLDER_A_PATH, PROJECT_PATH, subfolderResponse.getRows(), expectedError);

            // Subfolder editor should not be able to move rows from a project where they can only read to a subfolder
            expectedError = "User does not have permission to perform this operation.";
            moveRowsExpectingError(AUTO_INCREMENT_LIST, PROJECT_PATH, SUBFOLDER_A_PATH, projectResponse.getRows(), expectedError);

            // Subfolder editor should be able to move rows from subfolder to subfolder
            MoveRowsResponse moveResponse = moveRows(AUTO_INCREMENT_LIST, SUBFOLDER_A_PATH, SUBFOLDER_B_PATH, subfolderResponse.getRows());

            stopImpersonating();

            // We have to navigate to the project to ensure requests are made from the correct folder context.
            // This is due to the audit log helper hardcoding what project it filters the audit logs by.
            goToProjectHome();

            var expectedCounts = new UpdateCounts(subfolderFileAttachments, subfolderRows + 2, subfolderRows, subfolderRows, subfolderRows - subfolderFileAttachments);
            verifySuccessfulMove(AUTO_INCREMENT_LIST, moveResponse, SUBFOLDER_A_PATH, SUBFOLDER_B_PATH, expectedCounts);
        }
    }

    @Test
    public void testListInSubfolder() throws Exception
    {
        // Create a list domain in a subfolder
        var subfolderListName = DomainUtils.DomainKind.IntList.randomName("SUBFOLDER");
        var subfolderList = (IntListDefinition) new IntListDefinition(subfolderListName, autoIncrementKeyFieldName)
                .setFields(getFields());
        var conn = createDefaultConnection();
        subfolderList.getCreateCommand().execute(conn, SUBFOLDER_A_PATH);
        subfolderList.setListId(resolveListId(conn, subfolderListName, SUBFOLDER_A_PATH));

        var subARows = addRows(subfolderList, SUBFOLDER_A_PATH, 10, 0);

        // Cannot move rows from a list defined in a subfolder to the project
        String expectedError = String.format("List '%s' is not accessible from folder %s.", subfolderList.getName(), PROJECT_PATH);
        moveRowsExpectingError(subfolderList, SUBFOLDER_A_PATH, PROJECT_PATH, subARows.getRows(), expectedError);

        // Cannot move rows from a list defined in a subfolder to a folder further down the hierarchy
        expectedError = String.format("List '%s' is not accessible from folder %s.", subfolderList.getName(), SUBFOLDER_MINOR_A_PATH);
        moveRowsExpectingError(subfolderList, SUBFOLDER_A_PATH, SUBFOLDER_MINOR_A_PATH, subARows.getRows(), expectedError);
    }

    @Test
    public void testAuditDetailsNone() throws Exception
    {
        truncateList(STRING_KEY_LIST);

        // Arrange
        int totalRows = 5;
        int numRowsWithAttachmentValue = 2;
        var response = addRows(STRING_KEY_LIST, SUBFOLDER_A_PATH, totalRows, numRowsWithAttachmentValue);

        QueryApiHelper queryApiHelper = getQueryApiHelper(SUBFOLDER_A_PATH, STRING_KEY_LIST);
        MoveRowsCommand command = queryApiHelper.createMoveRowsCommand(response.getRows(), SUBFOLDER_B_PATH);
        command.setAuditBehavior(BaseRowsCommand.AuditBehavior.NONE);

        // Act
        MoveRowsResponse moveRowsResponse = queryApiHelper.moveRows(command);

        // Assert
        UpdateCounts updateCount = new UpdateCounts(numRowsWithAttachmentValue, 0, totalRows, totalRows, totalRows - numRowsWithAttachmentValue);
        checker().verifyNull("Expected a null transaction audit Id", moveRowsResponse.getTransactionAuditId());
        verifyUpdateCounts(updateCount, moveRowsResponse.getUpdateCounts());
    }

    @Test
    public void testInvalidArguments() throws Exception
    {
        truncateList(AUTO_INCREMENT_LIST);
        var response = addRows(AUTO_INCREMENT_LIST, getProjectName(), 1, 0);
        var validId = response.getRows().get(0).get(autoIncrementKeyFieldName);

        moveRowsExpectingError(AUTO_INCREMENT_LIST, getProjectName(), SUBFOLDER_B_PATH, List.of(), "No 'rows' array supplied.");
        moveRowsExpectingError(AUTO_INCREMENT_LIST, getProjectName(), SUBFOLDER_B_PATH, List.of(Map.of("InvalidKey", validId)), "Key field value required for moving list rows.");
        moveRowsExpectingError(AUTO_INCREMENT_LIST, getProjectName(), null, response.getRows(), "A target container must be specified for the move operation.");
        moveRowsExpectingError(AUTO_INCREMENT_LIST, getProjectName(), "/Shared", response.getRows(), "Invalid target container for the move operation: /Shared.");
    }

    private void successfullyMoveRows(ListDefinition list, List<Map<String, Object>> rows) throws Exception
    {
        var rowsSize = rows.size();
        var rowAttachmentSize = attachmentCount(rows);

        var mid = rowsSize / 2;
        var firstRows = rows.subList(0, mid);
        var firstRowsSize = firstRows.size();
        var firstRowAttachmentSize = attachmentCount(firstRows);

        var secondRows = rows.subList(mid, rowsSize);
        var secondRowsSize = secondRows.size();
        var secondRowAttachmentSize = attachmentCount(secondRows);

        // Act
        var moveResponse = moveRows(list, getProjectName(), SUBFOLDER_A_PATH, rows);
        var expectedCounts = new UpdateCounts(rowAttachmentSize, rowsSize + 2, rowsSize, rowsSize, rowsSize - rowAttachmentSize);
        verifySuccessfulMove(list, moveResponse, getProjectName(), SUBFOLDER_A_PATH, expectedCounts);
        if (checker().errorsSinceMark() > 0)
            return;

        // Do it again with the same rows expecting no changes
        moveResponse = moveRows(list, getProjectName(), SUBFOLDER_A_PATH, rows);
        verifySuccessfulMove(list, moveResponse, getProjectName(), SUBFOLDER_A_PATH, NO_UPDATE);
        if (checker().errorsSinceMark() > 0)
            return;

        // Now move them between subfolders
        moveResponse = moveRows(list, SUBFOLDER_A_PATH, SUBFOLDER_B_PATH, firstRows);
        expectedCounts = new UpdateCounts(firstRowAttachmentSize, firstRowsSize + 2, firstRowsSize + firstRowsSize, firstRowsSize, firstRowsSize - firstRowAttachmentSize);
        verifySuccessfulMove(list, moveResponse, SUBFOLDER_A_PATH, SUBFOLDER_B_PATH, expectedCounts);
        if (checker().errorsSinceMark() > 0)
            return;

        // Now move them between subfolders
        moveResponse = moveRows(list, SUBFOLDER_A_PATH, SUBFOLDER_B_PATH, secondRows);
        expectedCounts = new UpdateCounts(secondRowAttachmentSize, secondRowsSize + 2, secondRowsSize + secondRowsSize, secondRowsSize, secondRowsSize - secondRowAttachmentSize);
        verifySuccessfulMove(list, moveResponse, SUBFOLDER_A_PATH, SUBFOLDER_B_PATH, expectedCounts);
        if (checker().errorsSinceMark() > 0)
            return;

        // Now move them further down the hierarchy
        moveResponse = moveRows(list, SUBFOLDER_B_PATH, SUBFOLDER_MINOR_A_PATH, secondRows);
        expectedCounts = new UpdateCounts(secondRowAttachmentSize, secondRowsSize + 2, secondRowsSize * 3, secondRowsSize, secondRowsSize - secondRowAttachmentSize);
        verifySuccessfulMove(list, moveResponse, SUBFOLDER_B_PATH, SUBFOLDER_MINOR_A_PATH, expectedCounts);
        if (checker().errorsSinceMark() > 0)
            return;

        // Now move all rows back to the project
        moveResponse = moveRows(list, PROJECT_PATH, PROJECT_PATH, rows);
        expectedCounts = new UpdateCounts(rowAttachmentSize, rowsSize + 4, (rowsSize * 3) + secondRowsSize, rowsSize, rowsSize - rowAttachmentSize);
        verifySuccessfulMove(list, moveResponse, Map.of(SUBFOLDER_B_PATH, secondRowsSize, SUBFOLDER_MINOR_A_PATH, secondRowsSize), PROJECT_PATH, expectedCounts);
    }

    private int attachmentCount(List<Map<String, Object>> rows)
    {
        return (int) rows.stream().filter(row -> row.get(attachmentFieldName) != null).count();
    }

    private void verifySuccessfulMove(
        ListDefinition list,
        MoveRowsResponse response,
        String sourceContainerPath,
        String targetContainerPath,
        UpdateCounts expectedCounts
    ) throws IOException, CommandException
    {
        verifySuccessfulMove(list, response, Map.of(sourceContainerPath, expectedCounts.listRecords), targetContainerPath, expectedCounts);
    }

    private void verifySuccessfulMove(
        ListDefinition list,
        MoveRowsResponse response,
        Map<String, Integer> sourceContainerPathCounts,
        String targetContainerPath,
        UpdateCounts expectedCounts
    ) throws IOException, CommandException
    {
        final String targetPath = targetContainerPath.startsWith("/") ? targetContainerPath : "/" + targetContainerPath;

        // Verify response
        checker().verifyTrue("Expected successful move rows response", response.getSuccess());
        checker().verifyTrue("Expected a transaction audit Id", response.getTransactionAuditId() > 0);
        checker().verifyEquals("Unexpected container path", targetPath, response.getContainerPath());

        // Verify update counts
        verifyUpdateCounts(expectedCounts, response.getUpdateCounts());

        // Verify audit logs
        var decimalFormat = new DecimalFormat("#,##0");
        var hasUpdates = expectedCounts != NO_UPDATE;
        var listAuditEvents = _auditLogHelper.getAuditLogsForTransactionId(getProjectName(), LIST_AUDIT_EVENT, List.of("Comment", "Container/Path", "ListId"), response.getTransactionAuditId(), ContainerFilter.CurrentAndSubfolders);
        checker().verifyEquals("Unexpected number of list audit events", expectedCounts.listAuditEventsCreated, listAuditEvents.size());

        for (var entry : sourceContainerPathCounts.entrySet())
        {
            String sourcePath = entry.getKey().startsWith("/") ? entry.getKey() : "/" + entry.getKey();
            Integer expectedListRecordCount = entry.getValue();

            checker().wrapAssertion(() -> {
                var comment = String.format("Moved %s rows from %s", decimalFormat.format(expectedListRecordCount), sourcePath);
                var matches = auditEventMatches(listAuditEvents, targetPath, list.getListId(), comment);
                Assertions.assertThat(matches)
                        .as(String.format("Expected audit summary event in target container \"%s\"", comment))
                        .hasSize(hasUpdates ? 1 : 0);
            });

            checker().wrapAssertion(() -> {
                var comment = String.format("Moved %s rows to %s", decimalFormat.format(expectedListRecordCount), targetPath);
                var matches = auditEventMatches(listAuditEvents, sourcePath, list.getListId(), comment);
                Assertions.assertThat(matches)
                        .as(String.format("Expected audit summary event in source container \"%s\"", comment))
                        .hasSize(hasUpdates ? 1 : 0);
            });
        }

        checker().wrapAssertion(() -> {
            var matches = auditEventMatches(listAuditEvents, targetPath, list.getListId(), "An existing list record was moved");
            Assertions.assertThat(matches)
                    .as("Expected each list record move to be audited in the target container")
                    .hasSize(expectedCounts.listRecords);
        });
    }

    private void verifyUpdateCounts(UpdateCounts expectedCounts, Map<String, Object> responseCounts)
    {
        checker().wrapAssertion(() -> Assertions.assertThat(responseCounts.keySet())
                .as("Expect list moveRows response to contain specific update counts")
                .containsExactlyInAnyOrder("fileAttachmentsMoved", "listAuditEventsCreated", "listAuditEventsMoved", "listRecords", "queryAuditEventsMoved"));
        checker().verifyEquals("Unexpected number of file attachments moved", expectedCounts.fileAttachmentsMoved, responseCounts.get("fileAttachmentsMoved"));
        checker().verifyEquals("Unexpected number of list audit events created", expectedCounts.listAuditEventsCreated, responseCounts.get("listAuditEventsCreated"));
        checker().verifyEquals("Unexpected number of list audit events moved", expectedCounts.listAuditEventsMoved, responseCounts.get("listAuditEventsMoved"));
        checker().verifyEquals("Unexpected number of list records moved", expectedCounts.listRecords, responseCounts.get("listRecords"));
        checker().verifyEquals("Unexpected number of query audit events moved", expectedCounts.queryAuditEventsMoved, responseCounts.get("queryAuditEventsMoved"));
    }

    private List<Map<String, Object>> auditEventMatches(List<Map<String, Object>> events, String containerPath, Integer listId, String comment)
    {
        return events.stream()
                .filter((event) ->
                    comment.equals(event.get("Comment")) &&
                    containerPath.equals(event.get("Container/Path")) &&
                    listId.equals(event.get("ListId")))
                .toList();
    }

    private QueryApiHelper getQueryApiHelper(String containerPath, ListDefinition list)
    {
        return new QueryApiHelper(createDefaultConnection(), containerPath, LIST_SCHEMA, list.getName());
    }

    private MoveRowsResponse moveRows(ListDefinition list, String sourceContainerPath, String targetContainerPath, List<Map<String, Object>> rows) throws Exception
    {
        return getQueryApiHelper(sourceContainerPath, list)
                .moveRows(rows, targetContainerPath);
    }

    private void moveRowsExpectingError(
        ListDefinition list,
        String sourceContainerPath,
        String targetContainerPath,
        List<Map<String, Object>> rows,
        String expectedError
    )
    {
        String error = null;

        try
        {
            moveRows(list, sourceContainerPath, targetContainerPath, rows);
        }
        catch (Exception e)
        {
            error = e.getMessage();
        }

        checker().verifyEquals("Unexpected error message", expectedError, error);
    }

    private SelectRowsResponse addRows(ListDefinition list, String containerPath, int numRows, int numRowsWithAttachmentValue) throws IOException, CommandException
    {
        // At this time, the only way to provide values for attachment fields on lists is via the UI. The following
        // adds an initial set of rows without attachment values, then adds a second set of rows with attachment values.
        // Each row with attachment values is entered manually through the UI form.
        var dataGenerator = list.getTestDataGenerator(containerPath)
                .addDataSupplier(stringKeyFieldName, () -> TestDataGenerator.randomString(TestDataGenerator.randomInt(21, 57)))
                .addDataSupplier(attachmentFieldName, () -> null);

        dataGenerator.withGeneratedRows(numRows - numRowsWithAttachmentValue)
            .insertRows();

        var attachmentRows = dataGenerator.withGeneratedRows(numRowsWithAttachmentValue)
                .getRows();

        _listHelper.beginAtList(containerPath, list.getName());
        for (var row : attachmentRows)
        {
            var newRow = new CaseInsensitiveHashMap<>();
            newRow.putAll(row);
            newRow.put(attachmentFieldName, SAMPLE_DATA_FILE);
            _listHelper.insertNewRow(newRow, false);
        }

        return getQueryApiHelper(containerPath, list)
                .selectRows();
    }

    private List<FieldDefinition> getFields()
    {
        return List.of(
            new FieldDefinition(attachmentFieldName, FieldDefinition.ColumnType.Attachment),
            new FieldDefinition(booleanFieldName, FieldDefinition.ColumnType.Boolean),
            new FieldDefinition(dateFieldName, FieldDefinition.ColumnType.Date),
            new FieldDefinition(dateTimeFieldName, FieldDefinition.ColumnType.DateAndTime),
            new FieldDefinition(decimalFieldName, FieldDefinition.ColumnType.DateAndTime),
            new FieldDefinition(integerFieldName, FieldDefinition.ColumnType.Integer)
        );
    }

    private Integer resolveListId(Connection conn, String listName, String containerPath) throws IOException, CommandException
    {
        var cmd = new SelectRowsCommand("ListManager", "ListManager");
        cmd.setFilters(List.of(new Filter("Name", listName)));
        cmd.setColumns(List.of("ListId"));
        var response = cmd.execute(conn, containerPath);

        return (Integer) response.getRows().get(0).get("ListId");
    }

    private void truncateList(ListDefinition list) throws IOException, CommandException
    {
        var listName = list.getName();
        var connection = createDefaultConnection();
        var cmd = new TruncateTableCommand(LIST_SCHEMA, listName);

        cmd.execute(connection, PROJECT_PATH);
        cmd.execute(connection, SUBFOLDER_A_PATH);
        cmd.execute(connection, SUBFOLDER_MINOR_A_PATH);
        cmd.execute(connection, SUBFOLDER_B_PATH);
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return List.of("list");
    }
}
