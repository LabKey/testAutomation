/*
 * Copyright (c) 2011-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.test.tests;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Hosting;
import org.labkey.test.components.domain.ConditionalFormatDialog;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.core.admin.logger.ManagerPage.LoggingLevel;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.AuditLogHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Log4jUtils;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.UIUserHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.util.PasswordUtil.getUsername;

@Category({Daily.class, Hosting.class})
@BaseWebDriverTest.ClassTimeout(minutes = 9)
public class AuditLogTest extends BaseWebDriverTest
{
    public static final String USER_AUDIT_EVENT = "User events";
    public static final String GROUP_AUDIT_EVENT = "Group and role events";
    public static final String QUERY_UPDATE_EVENT = "Query update events";
    public static final String PROJECT_AUDIT_EVENT = "Project and Folder events";
    public static final String ASSAY_AUDIT_EVENT = "Link to Study events";

    private static final String AUDIT_TEST_USER = "audit_user1@auditlog.test";
    private static final String AUDIT_TEST_USER2 = "audit_user2@auditlog.test";
    private static final String AUDIT_TEST_USER3 = "audit_user3@auditlog.test";

    private static final String AUDIT_SECURITY_GROUP = "Testers";

    private static final String PROJECT_ADMIN_ROLE = "Project Administrator";
    private static final String AUTHOR_ROLE = "Author";

    private static final String AUDIT_TEST_PROJECT = "AuditVerifyTest";
    private static final String AUDIT_DETAILED_TEST_PROJECT = "AuditDetailedLogTest";
    private static final String AUDIT_TEST_SUBFOLDER = "AuditVerifyTest_Subfolder";
    private static final String AUDIT_PROPERTY_EVENTS_PROJECT = "AuditDomainPropertyEvents";

    public static final String COMMENT_COLUMN = "Comment";

    private final ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("audit");
    }

    public enum Visibility
    {
        ParentFolder, // can only see log events in parent
        ChildFolder, // can only see log event in children
        All, // can see all events
        None // can see no events
    }

    public boolean canSeeParent(Visibility v)
    {
        return v == Visibility.ParentFolder || v == Visibility.All;
    }

    public boolean canSeeChild(Visibility v)
    {
        return v == Visibility.ChildFolder || v == Visibility.All;
    }

    @Override
    protected String getProjectName()
    {
        return AUDIT_TEST_PROJECT;
    }

    @Override
    protected void checkQueries()
    {
    } // Skip.  Project is deleted as part of test

    @Override
    protected void checkViews()
    {
    } // Skip.  Project is deleted as part of test

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(false, AUDIT_TEST_USER);
        _containerHelper.deleteProject(getProjectName(), false);
        _containerHelper.deleteProject(AUDIT_DETAILED_TEST_PROJECT, false);
        _containerHelper.deleteProject(AUDIT_PROPERTY_EVENTS_PROJECT, false);
        Log4jUtils.resetAllLogLevels();
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Test
    public void testSteps() throws IOException, CommandException
    {
        turnOnAuditLogFile();

        userAuditTest();
        groupAuditTest();
        canSeeAuditLogTest();
    }

    protected void turnOnAuditLogFile()
    {
        Log4jUtils.setLogLevel("org.labkey.audit.event", LoggingLevel.ALL);
        Log4jUtils.setLogLevel("org.labkey.audit.event.UserAuditEvent", LoggingLevel.ALL);
        Log4jUtils.setLogLevel("org.labkey.core.Login.LoginController", LoggingLevel.ALL);
    }

    protected ArrayList<String> getAuditLogFromFile() throws IOException
    {
        ArrayList<String> auditLog = new ArrayList<>();
        File auditLogFile = new File(TestFileUtils.getServerLogDir(), "labkey-audit.log");

        try (FileReader fileReader = new FileReader(auditLogFile))
        {
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;

            while ((line = bufferedReader.readLine()) != null)
            {
                auditLog.add(line);
            }
        }

        return auditLog;
    }

    protected void compareAuditLogFileEntries(List<String> auditLogBefore, List<String> auditLogAfter, List<String> expectedValues) throws IOException
    {
        boolean pass = true;
        StringBuilder stringBuilder = new StringBuilder();

        log("Validating entries in the Audit Log file.");

        ArrayList<String> diff = new ArrayList<>(auditLogAfter);
        diff.removeAll(auditLogBefore);

        // First check if the count is right.
        if (expectedValues.size() != diff.size())
        {
            stringBuilder.append("Number of audit logs recorded in the file not as expected. Expected: ")
                    .append(expectedValues.size())
                    .append(" found: ")
                    .append(diff.size())
                    .append("\n");
            pass = false;
        }

        // Check to see if all of the expected values did show up.
        for (String expectedValue : expectedValues)
        {
            log("Searching Audit Log file for entry: '" + expectedValue + "'.");
            boolean found = false;
            for (int j = 0; (!found) && (j < diff.size()); j++)
            {
                if (diff.get(j).contains(expectedValue))
                {
                    // If we found the expected message remove it from the list and stop checking.
                    found = true;
                    diff.remove(j);
                }
            }

            if (!found)
            {
                stringBuilder.append("Did not find '")
                        .append(expectedValue)
                        .append("' in log file\n");
                pass = false;
            }

        }

        // If there is anything left in the list it means there was an log message recorded that we weren't expecting.
        if (diff.size() > 0)
        {
            pass = false;
            for (String extraLog : diff)
                stringBuilder.append("Found this unexpected log in the file: ")
                        .append(extraLog)
                        .append("\n");
        }

        if (!pass)
        {
            File dumpDir = new File(getArtifactCollector().ensureDumpDir(), "audit_logs");
            dumpDir.mkdir();
            TestFileUtils.writeFile(new File(dumpDir, "audit_log_before.log"), String.join("\n", auditLogBefore));
            TestFileUtils.writeFile(new File(dumpDir, "audit_log_after.log"), String.join("\n", auditLogAfter));
            fail(stringBuilder.toString());
        }
    }

    protected void userAuditTest() throws IOException
    {
        List<String> auditLogBefore;
        List<String> auditLogAfter;
        List<String> expectedLogValues = new ArrayList<>();

        auditLogBefore = getAuditLogFromFile();
        // Use UI helper to avoid unexpected events from API authentication
        UIUserHelper userHelper = new UIUserHelper(this);
        log("testing user audit events");
        userHelper.createUser(AUDIT_TEST_USER);
        expectedLogValues.add(AUDIT_TEST_USER + " was added to the system and the administrator chose not to send a verification email.");

        impersonate(AUDIT_TEST_USER);
        expectedLogValues.add(getUsername() + " impersonated " + AUDIT_TEST_USER);
        expectedLogValues.add(AUDIT_TEST_USER + " was impersonated by " + getUsername());

        stopImpersonating();
        expectedLogValues.add(AUDIT_TEST_USER + " was no longer impersonated by " + getUsername());
        expectedLogValues.add(getUsername() + " stopped impersonating " + AUDIT_TEST_USER);

        impersonateRoles(PROJECT_ADMIN_ROLE, AUTHOR_ROLE);
        expectedLogValues.add(getUsername() + " impersonated roles: " + PROJECT_ADMIN_ROLE + ", " + AUTHOR_ROLE);

        stopImpersonating();
        expectedLogValues.add(getUsername() + " stopped impersonating roles: " + PROJECT_ADMIN_ROLE + ", " + AUTHOR_ROLE);

        String adminGroup = "Administrator";
        impersonateGroup(adminGroup, true);
        expectedLogValues.add(getUsername() + " impersonated group: " + adminGroup);

        stopImpersonating();
        expectedLogValues.add(getUsername() + " stopped impersonating group: " + adminGroup);

        navBar().userMenu().signOut();
        expectedLogValues.add(getUsername() + " logged out.");

        signInShouldFail(AUDIT_TEST_USER, "asdf"); // Bad login.  Existing User
        expectedLogValues.add(AUDIT_TEST_USER + " failed to login: incorrect password");

        signInShouldFail(AUDIT_TEST_USER + "fail", "asdf"); // Bad login.  Non-existent User
        expectedLogValues.add(AUDIT_TEST_USER + "fail failed to login: user does not exist");

        simpleSignIn();
        expectedLogValues.add(getUsername() + " logged in successfully via the \"Standard database authentication\" configuration.");

        userHelper.deleteUsers(true, AUDIT_TEST_USER);
        expectedLogValues.add(AUDIT_TEST_USER + " was deleted from the system");

        Collections.reverse(expectedLogValues);
        for (int i = 0; i < expectedLogValues.size(); i++)
        {
            String msg = expectedLogValues.get(i);
            verifyAuditEvent(this, USER_AUDIT_EVENT, COMMENT_COLUMN, msg, i + 1);
        }

        // Check the file after the UI check, if the UI tests passed then we should have confidence that the entry is in the file.
        auditLogAfter = getAuditLogFromFile();

        compareAuditLogFileEntries(auditLogBefore, auditLogAfter, expectedLogValues);
    }

    protected void groupAuditTest() throws IOException
    {
        List<String> auditLogBefore;
        List<String> auditLogAfter;

        auditLogBefore = getAuditLogFromFile();

        log("testing group audit events");

        _containerHelper.createProject(AUDIT_TEST_PROJECT, null);
        permissionsHelper.createPermissionsGroup(AUDIT_SECURITY_GROUP);
        permissionsHelper.setPermissions(AUDIT_SECURITY_GROUP, "Editor");
        _userHelper.createUser(AUDIT_TEST_USER, false, true);
        permissionsHelper.addUserToProjGroup(AUDIT_TEST_USER, getProjectName(), AUDIT_SECURITY_GROUP);
        _userHelper.deleteUsers(true, AUDIT_TEST_USER);
        _containerHelper.deleteProject(AUDIT_TEST_PROJECT, true);

        List<String> expectedLogValues = new ArrayList<>();
        expectedLogValues.add("Project " + AUDIT_TEST_PROJECT + " was created");
        expectedLogValues.add("A new security group named " + AUDIT_SECURITY_GROUP + " was created.");
        expectedLogValues.add("The group Guests was removed from the security role No Permissions.");
        expectedLogValues.add("The group " + AUDIT_SECURITY_GROUP + " was assigned to the security role Editor.");
        expectedLogValues.add(AUDIT_TEST_USER + " was added to the system and the administrator chose not to send a verification email.");
        expectedLogValues.add("User: " + AUDIT_TEST_USER + " was added as a member to Group: " + AUDIT_SECURITY_GROUP);
        expectedLogValues.add(AUDIT_TEST_USER + " was deleted from the system");
        expectedLogValues.add("Project /" + AUDIT_TEST_PROJECT + " was deleted");
        expectedLogValues.add("A new security policy was established for " + AUDIT_TEST_PROJECT + ". It will no longer inherit permissions from /");
        expectedLogValues.add("The group Guests was assigned to the security role No Permissions.");

        verifyAuditEvent(this, GROUP_AUDIT_EVENT, COMMENT_COLUMN, expectedLogValues.get(1), 10);
        verifyAuditEvent(this, GROUP_AUDIT_EVENT, COMMENT_COLUMN, expectedLogValues.get(3), 10);
        verifyAuditEvent(this, GROUP_AUDIT_EVENT, COMMENT_COLUMN, expectedLogValues.get(5), 10);
        verifyAuditEvent(this, USER_AUDIT_EVENT, COMMENT_COLUMN, expectedLogValues.get(6), 10);

        log("testing project audit events");
        verifyAuditEvent(this, PROJECT_AUDIT_EVENT, COMMENT_COLUMN, expectedLogValues.get(0), 10);
        verifyAuditEvent(this, PROJECT_AUDIT_EVENT, COMMENT_COLUMN, expectedLogValues.get(7), 10);

        // Check the file after the UI check, if the UI tests passed then we should have confidence that the entry is in the file.
        auditLogAfter = getAuditLogFromFile();

        compareAuditLogFileEntries(auditLogBefore, auditLogAfter, expectedLogValues);
    }

    protected void canSeeAuditLogTest()
    {
        log("testing CanSeeAuditLog permission");
        simpleSignIn();
        _containerHelper.createProject(AUDIT_TEST_PROJECT, null);
        _containerHelper.createSubfolder(AUDIT_TEST_PROJECT, AUDIT_TEST_SUBFOLDER);
        createList(AUDIT_TEST_PROJECT, "Parent List", "Name\nData", new FieldDefinition("Name", ColumnType.String).setDescription("Name") );
        createList(AUDIT_TEST_PROJECT + "/" + AUDIT_TEST_SUBFOLDER, "Child List", "Name\nData", new FieldDefinition("Name", ColumnType.String).setDescription("Name"));

        createUserWithPermissions(AUDIT_TEST_USER, AUDIT_TEST_PROJECT, "Editor");
        createUserWithPermissions(AUDIT_TEST_USER2, AUDIT_TEST_PROJECT, "Project Administrator");

        // signed in as an admin so we should see rows here
        verifyAuditQueries(true);

        // signed in as an editor should not show any rows for audit query links
        impersonate(AUDIT_TEST_USER);
        verifyAuditQueries(false);
        stopImpersonating();

        // now grant CanSeeAuditLog permission to our audit user and verify
        // we see audit information
        permissionsHelper.setSiteAdminRoleUserPermissions(AUDIT_TEST_USER, "See Audit Log Events");
        impersonate(AUDIT_TEST_USER);
        verifyAuditQueries(true);

        // cleanup
        stopImpersonating();

        // verify issue 19515 - ensure that container filters are respected (i.e., a project admin without sub-folder admin access
        // should not see audit log events for that sub folder
        // verify our audit log only shows the row for the parent list since our user does not have project admin
        // permissions on the sub-folder
        impersonate(AUDIT_TEST_USER2);
        verifyListAuditLogQueries(Visibility.ParentFolder);
        stopImpersonating();
        // now give access to the sub-folder
        navigateToFolder(AUDIT_TEST_PROJECT, AUDIT_TEST_SUBFOLDER);
        _securityHelper.setProjectPerm(AUDIT_TEST_USER2, "Folder Administrator");
        impersonate(AUDIT_TEST_USER2);
        verifyListAuditLogQueries(Visibility.All);
        stopImpersonating();

        // verify issue 19832 - opposite of above.  Ensure that user who has access to child folder but not parent folder can still see
        // audit log events from the child forder if using a CurrentAndSubFolders container filter
        createUserWithPermissions(AUDIT_TEST_USER3, AUDIT_TEST_PROJECT, "Editor");
        permissionsHelper.addMemberToRole(AUDIT_TEST_USER3, "Folder Administrator", PermissionsHelper.MemberType.user, AUDIT_TEST_PROJECT + "/" + AUDIT_TEST_SUBFOLDER);
        impersonate(AUDIT_TEST_USER3);
        verifyListAuditLogQueries(Visibility.ChildFolder);
        stopImpersonating();

        _userHelper.deleteUsers(true, AUDIT_TEST_USER, AUDIT_TEST_USER2, AUDIT_TEST_USER3);
        _containerHelper.deleteProject(AUDIT_TEST_PROJECT, true);
    }

    @Test
    public void testDetailedQueryUpdateAuditLog() throws IOException, CommandException
    {
        // This test class is run as part of the Distribution Suites. The distributions are production build and have
        // a minimal feature set and some may not include the "simpletest" module. If it is not there make this a no-op test.
        if(_containerHelper.getAllModules().contains("simpletest"))
        {
            _containerHelper.createProject(AUDIT_DETAILED_TEST_PROJECT, "Custom");
            _containerHelper.enableModule("simpletest");
            goToProjectHome();

            Connection cn = WebTestHelper.getRemoteApiConnection();

            // create manufacturer (which has summary audit log level)
            InsertRowsCommand insertCmd = new InsertRowsCommand("vehicle", "manufacturers");
            Map<String, Object> rowMap = new HashMap<>();
            rowMap.put("name", "Kia");
            insertCmd.addRow(rowMap);
            SaveRowsResponse resp1 = insertCmd.execute(cn, AUDIT_DETAILED_TEST_PROJECT);

            Map<String, String> auditLog = getAuditLogRow(this, "Query update events", "Query Name", "Manufacturers");
            assertEquals("Did not find expected audit log for summary log level", "1 row(s) were inserted.", auditLog.get("Comment"));

            //then create model (which has detailed audit log level)
            InsertRowsCommand insertCmd2 = new InsertRowsCommand("vehicle", "models");
            rowMap = new HashMap<>();
            rowMap.put("manufacturerId", resp1.getRows().get(0).get("rowid"));
            rowMap.put("name", "Soul");
            insertCmd2.addRow(rowMap);
            insertCmd2.execute(cn, AUDIT_DETAILED_TEST_PROJECT);

            refresh();
            auditLog = getAuditLogRow(this, "Query update events", "Query Name", "Models");
            assertEquals("Did not find expected audit log for detailed log level", "A row was inserted.", auditLog.get("Comment"));
            _containerHelper.deleteProject(AUDIT_DETAILED_TEST_PROJECT, false);
        }
        else
        {
            // Would like to mark the test are ignore, but unfortunately that can only be done by annotation and once in
            // the test it can't be marked as ignore.
            log("The 'simpletest' module was not present, nothing was tested.");
        }
    }

    private void createList(String containerPath, String listName, @Nullable String tsvData, FieldDefinition... listColumns)
    {
        _listHelper.createList(containerPath, listName, "Key", listColumns);
        if(null != tsvData)
        {
            _listHelper.goToList(listName);
            _listHelper.clickImportData()
                    .setText(tsvData)
                    .submit();
        }
    }

    protected void verifyListAuditLogQueries(Visibility v)
    {
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=auditLog&query.queryName=ListAuditEvent&query.containerFilterName=CurrentAndSubfolders");
        verifyAuditQueryEvent(this, "List", "Parent List", 1, canSeeParent(v));
        verifyAuditQueryEvent(this, "List", "Child List", 1, canSeeChild(v));
    }

    protected void verifyAuditQueries(boolean canSeeAuditLog)
    {
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=auditLog&query.queryName=ContainerAuditEvent");
        if (canSeeAuditLog)
            verifyAuditQueryEvent(this, COMMENT_COLUMN, AUDIT_TEST_PROJECT + " was created", 1);
        else
            assertTextPresent("No data to show.");

        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=auditLog&query.queryName=GroupAuditEvent");
        if (canSeeAuditLog)
            verifyAuditQueryEvent(this, COMMENT_COLUMN, "The user " + AUDIT_TEST_USER + " was assigned to the security role Editor.", 1);
        else
            assertTextPresent("No data to show.");
    }

    public static void goToAuditEventView(BaseWebDriverTest instance, String eventType)
    {
        new AuditLogHelper(instance).goToAuditEventView(eventType);
    }

    public static void verifyAuditEvent(BaseWebDriverTest instance, String eventType, String column, String msg, int rowsToSearch)
    {
        goToAuditEventView(instance, eventType);

        verifyAuditQueryEvent(instance, column, msg, rowsToSearch);
    }

    public static void verifyAuditQueryEvent(BaseWebDriverTest instance, String column, String msg, int rowsToSearch)
    {
        verifyAuditQueryEvent(instance, column, msg, rowsToSearch, true);
    }

    public static Map<String, String> getAuditLogRow(BaseWebDriverTest instance, String eventType, String column, String msg)
    {
        goToAuditEventView(instance, eventType);
        instance.log("searching for entry " + column + " = " + msg);
        DataRegionTable table = new DataRegionTable("query", instance);
        return table.getRowDataAsMap(column, msg);
    }

    public static void verifyAuditQueryEvent(BaseWebDriverTest instance, String column, String msg, int rowsToSearch, boolean shouldFindText)
    {
        instance.log("searching for audit entry: " + msg);
        DataRegionTable table = new DataRegionTable("query", instance);
        int i = table.getColumnIndex(column);
        if (shouldFindText)
        {
            instance.waitForTextWithRefresh(10000, msg);
            assertTrue("Text '" + msg + "' was not present", findTextInDataRegion(table, i, msg, rowsToSearch + 2));
        }
        else
            assertFalse("Text '" + msg + "' was present", findTextInDataRegion(table, i, msg, rowsToSearch + 2));
    }

    public static boolean findTextInDataRegion(DataRegionTable table, int column, String txt, int rowsToSearch)
    {
        rowsToSearch = Math.min(table.getDataRowCount(), rowsToSearch);
        for (int row = 0; row < rowsToSearch; row++)
        {
            String value = table.getDataAsText(row, column);
            if (StringUtils.isNotEmpty(value) && value.contains(txt))
                return true;
        }
        return false;
    }

    @Test
    public void testDomainPropertyEvents()
    {
        final String LOOK_UP_LIST01 = "LookUp01";
        final String LOOK_UP_LIST02 = "LookUp02";
        final String LIST01_TSV = "id\tvalue\n1\tA\n2\tB\n3\tC";
        final String LIST02_TSV = "id\tvalue\n4\tX\n5\tY\n6\tZ";

        // This is the list who's log entries will be validated.
        final String LIST_CHECK_LOG = "ChangeMyColumns";

        final String FIELD01_NAME = "Field01";
        final String FIELD01_LABEL = "This is Field 01";
        final String FIELD01_UPDATED_LABEL = "This is Update Label for Field 01";
        final ColumnType FIELD01_TYPE = ColumnType.String;
        final String FIELD01_DESCRIPTION = "Simple String field.";
        final String FIELD01_UPDATED_DESCRIPTION = "This should be a new description for the field.";

        final String FIELD02_NAME = "Field02";
        final String FIELD02_LABEL = "This is Field 02";
        final ColumnType FIELD02_TYPE = ColumnType.Integer;
        final String FIELD02_DESCRIPTION = "Simple Integer field.";

        final String FIELD03_NAME = "Field03";
        final String FIELD03_LABEL = "Field 03 Lookup";
        final ColumnType FIELD03_TYPE = ColumnType.Integer;

        final String DOMAIN_PROPERTY_LOG_NAME = "Domain property events";

        _containerHelper.createProject(AUDIT_PROPERTY_EVENTS_PROJECT, null);

        PortalHelper portalHelper = new PortalHelper(getDriver());

        portalHelper.addWebPart("Lists");

        FieldDefinition[] listColumns = new FieldDefinition[]{
                new FieldDefinition("id", ColumnType.Integer).setLabel("id").setDescription("Simple integer index."),
                new FieldDefinition("value", ColumnType.String).setLabel("value").setDescription("Value of the look up.")};

        log("Create a couple of lists to be used as lookups.");
        createList(AUDIT_PROPERTY_EVENTS_PROJECT, LOOK_UP_LIST01, LIST01_TSV, listColumns);
        createList(AUDIT_PROPERTY_EVENTS_PROJECT, LOOK_UP_LIST02, LIST02_TSV, listColumns);

        log("Create the list that will have it's column attributes modified.");
        listColumns = new FieldDefinition[]{
                new FieldDefinition(FIELD01_NAME, FIELD01_TYPE).setLabel(FIELD01_LABEL).setDescription(FIELD01_DESCRIPTION),
                new FieldDefinition(FIELD02_NAME, FIELD02_TYPE).setLabel(FIELD02_LABEL).setDescription(FIELD02_DESCRIPTION)};

        createList(AUDIT_PROPERTY_EVENTS_PROJECT, LIST_CHECK_LOG, null, listColumns);

        List<Map<String, Object>> domainPropertyEventRows = getDomainPropertyEventsFromDomainEvents(AUDIT_PROPERTY_EVENTS_PROJECT, LIST_CHECK_LOG, null);

        // Add the list of the event ids to an ignore list so future tests don't look at them again.
        List<String> ignoreIds = new ArrayList<>();
        ignoreIds.addAll(getDomainEventIdsFromPropertyEvents(domainPropertyEventRows));

        if(domainPropertyEventRows.size() != 3)
        {
            // We are going to fail, so navigate to the Domain Property Events Audit Log so the screen shot shows the log.
            // I do the navigation because the log validation is happening by the API, so if there is a failure in the log
            // we may be on a page that will add no value to the screen shot artifact.
            goToAuditEventView(this, DOMAIN_PROPERTY_LOG_NAME);
            Assert.assertEquals("The number of entries in the domain audit log were not as expected.", 3, domainPropertyEventRows.size());
        }

        log("Validate that the expected rows are there.");
        Map<String, String> field01ExpectedColumns = Maps.of("action", "Created");
        Map<String, String> field01ExpectedComment = Maps.of("Name", FIELD01_NAME,"Label", FIELD01_LABEL,"Type", "String","Description", FIELD01_DESCRIPTION);
        boolean pass = validateExpectedRowInDomainPropertyAuditLog(domainPropertyEventRows, FIELD01_NAME, field01ExpectedColumns, field01ExpectedComment);

        Map<String, String> field02ExpectedColumns = Maps.of("action", "Created");
        Map<String, String> field02ExpectedComment = Maps.of("Name", FIELD02_NAME,"Label", FIELD02_LABEL,"Type", FIELD02_TYPE.getLabel(),"Description", FIELD02_DESCRIPTION);
        pass = validateExpectedRowInDomainPropertyAuditLog(domainPropertyEventRows, FIELD02_NAME, field02ExpectedColumns, field02ExpectedComment) && pass;

        // We are going to fail, so navigate to the Domain Property Events Audit Log.
        if(!pass)
            goToAuditEventView(this, DOMAIN_PROPERTY_LOG_NAME);

        Assert.assertTrue("The values logged for the 'Created' events were not as expected. See log for details.", pass);

        log("Looks like the created events were as expected. Now modify some column/field attributes.");
        goToProjectHome(AUDIT_PROPERTY_EVENTS_PROJECT);
        clickAndWait(Locator.linkWithText(LIST_CHECK_LOG));
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(LIST_CHECK_LOG);
        DomainFormPanel fieldsPanel = listDefinitionPage.getFieldsPanel();

        log("Change properties on field '" + FIELD01_NAME + "'.");
        fieldsPanel.getField(FIELD01_NAME)
                .setPHILevel(FieldDefinition.PhiSelectType.Restricted)
                .setRequiredField(true)
                .setDescription(FIELD01_UPDATED_DESCRIPTION)
                .setLabel(FIELD01_UPDATED_LABEL);

        log("Change properties on field '" + FIELD02_NAME + "'.");
        DomainFieldRow field2 = fieldsPanel.getField(FIELD02_NAME)
                .setScaleType(FieldDefinition.ScaleType.LOG)
                .setNumberFormat("#!");
        ConditionalFormatDialog formatDlg = field2.clickConditionalFormatButton();
        formatDlg.getOpenFormatPanel().setFirstValue("5");
        formatDlg.clickApply();

        listDefinitionPage.clickSave();

        log("Get a list of ids from the Domain Events Audit Log again but this time remove from the list the ids from the created events.");
        domainPropertyEventRows = getDomainPropertyEventsFromDomainEvents(AUDIT_PROPERTY_EVENTS_PROJECT, LIST_CHECK_LOG, ignoreIds);

        // Add the list of the event ids to an ignore list so future tests don't look at them again.
        ignoreIds.addAll(getDomainEventIdsFromPropertyEvents(domainPropertyEventRows));

        if(domainPropertyEventRows.size() != 2)
        {
            // We are going to fail, so navigate to the Domain Property Events Audit Log.
            goToAuditEventView(this, DOMAIN_PROPERTY_LOG_NAME);
            Assert.assertEquals("The number of entries in the domain audit log were not as expected.", 2, domainPropertyEventRows.size());
        }

        log("Validate that the expected rows after the update are in the log.");
        field01ExpectedColumns = Maps.of("action", "Modified");
        field01ExpectedComment = Maps.of("Label", FIELD01_LABEL + " -> " + FIELD01_UPDATED_LABEL,
                "Description", FIELD01_DESCRIPTION + " -> " + FIELD01_UPDATED_DESCRIPTION,
                "PHI", "Not PHI -> Restricted PHI",
                "Required", "false -> true");
        pass = validateExpectedRowInDomainPropertyAuditLog(domainPropertyEventRows, FIELD01_NAME, field01ExpectedColumns, field01ExpectedComment);

        field02ExpectedColumns = Maps.of("action", "Modified");
        field02ExpectedComment = Maps.of("ConditionalFormats", "old: <none>, new: 1",
                "DefaultScale", "Linear -> Log");
        pass = validateExpectedRowInDomainPropertyAuditLog(domainPropertyEventRows, FIELD02_NAME, field02ExpectedColumns, field02ExpectedComment) && pass;

        // We are going to fail, so navigate to the Domain Property Events Audit Log.
        if(!pass)
            goToAuditEventView(this, DOMAIN_PROPERTY_LOG_NAME);

        Assert.assertTrue("The values logged for the 'Modified' events were not as expected. See log for details.", pass);

        log("The modified events were logged as expected. Now add a lookup field.");
        goToProjectHome(AUDIT_PROPERTY_EVENTS_PROJECT);
        clickAndWait(Locator.linkWithText(LIST_CHECK_LOG));
        listDefinitionPage = _listHelper.goToEditDesign(LIST_CHECK_LOG);
        listDefinitionPage.getFieldsPanel()
                .addField(new FieldDefinition(FIELD03_NAME,
                        new FieldDefinition.LookupInfo(null, "lists", LOOK_UP_LIST01)
                                .setTableType(ColumnType.Integer))
                        .setLabel(FIELD03_LABEL));
        listDefinitionPage.clickSave();

        log("Validate that a 'Create' event was logged for the new filed.");
        domainPropertyEventRows = getDomainPropertyEventsFromDomainEvents(AUDIT_PROPERTY_EVENTS_PROJECT, LIST_CHECK_LOG, ignoreIds);

        // Add the list of the event ids to an ignore list so future tests don't look at them again.
        ignoreIds.addAll(getDomainEventIdsFromPropertyEvents(domainPropertyEventRows));

        if(domainPropertyEventRows.size() != 1)
        {
            // We are going to fail, so navigate to the Domain Property Events Audit Log.
            goToAuditEventView(this, DOMAIN_PROPERTY_LOG_NAME);
            Assert.assertEquals("The number of entries in the domain audit log were not as expected.", 1, domainPropertyEventRows.size());
        }

        log("Validate that the expected row is there for the newly created field.");
        Map<String, String> field03ExpectedColumns = Maps.of("action", "Created");
        Map<String, String> field03ExpectedComment = Maps.of("Name", FIELD03_NAME,
                "Label", FIELD03_LABEL,
                "Type", FIELD03_TYPE.getLabel(),
                "Lookup", "[Schema: lists, Query: " + LOOK_UP_LIST01 + "]");
        pass = validateExpectedRowInDomainPropertyAuditLog(domainPropertyEventRows, FIELD03_NAME, field03ExpectedColumns, field03ExpectedComment);

        // We are going to fail, so navigate to the Domain Property Events Audit Log.
        if(!pass)
            goToAuditEventView(this, DOMAIN_PROPERTY_LOG_NAME);

        Assert.assertTrue("The values logged for the 'Created' event for the lookup field were not as expected. See log for details.", pass);

        log("The 'Created' event was logged as expected. Now modify the field to point to a new list in the lookup field.");
        goToProjectHome(AUDIT_PROPERTY_EVENTS_PROJECT);
        clickAndWait(Locator.linkWithText(LIST_CHECK_LOG));
        listDefinitionPage = _listHelper.goToEditDesign(LIST_CHECK_LOG);

        log("Change properties on field '" + FIELD03_NAME + "'.");
        listDefinitionPage.getFieldsPanel()
                .getField(FIELD03_NAME)
                .setLookup(new FieldDefinition.IntLookup(null, "lists", LOOK_UP_LIST02));
        listDefinitionPage.clickSave();

        log("Validate that the expected row is there for the after modifying the Lookup field.");
        field03ExpectedColumns = Maps.of("action", "Modified");
        field03ExpectedComment = Maps.of("Lookup", "[Query: old: " + LOOK_UP_LIST01 + ", new: " + LOOK_UP_LIST02 + "]");

        log("Get a list of ids from the Domain Events Audit Log again but remove from the list the ids from all of the previous events.");
        domainPropertyEventRows = getDomainPropertyEventsFromDomainEvents(AUDIT_PROPERTY_EVENTS_PROJECT, LIST_CHECK_LOG, ignoreIds);

        // Add the list of the event ids to an ignore list so future tests don't look at them again.
        ignoreIds.addAll(getDomainEventIdsFromPropertyEvents(domainPropertyEventRows));

        pass = validateExpectedRowInDomainPropertyAuditLog(domainPropertyEventRows, FIELD03_NAME, field03ExpectedColumns, field03ExpectedComment);

        // We are going to fail, so navigate to the Domain Property Events Audit Log.
        if(!pass)
            goToAuditEventView(this, DOMAIN_PROPERTY_LOG_NAME);

        Assert.assertTrue("The values logged for the 'Modified' events for the lookup field were not as expected. See log for details.", pass);

        log("The 'Modified' event was logged as expected for the lookup. Now delete the field.");
        goToProjectHome(AUDIT_PROPERTY_EVENTS_PROJECT);
        clickAndWait(Locator.linkWithText(LIST_CHECK_LOG));
        listDefinitionPage = _listHelper.goToEditDesign(LIST_CHECK_LOG);
        listDefinitionPage.getFieldsPanel().getField(3).clickRemoveField(true);
        listDefinitionPage.clickSave();

        log("Validate that the expected row is there after deleting the Lookup field.");
        field03ExpectedColumns = Maps.of("action", "Deleted");

        log("Get a list of ids from the Domain Events Audit Log again but remove from the list the ids from all of the previous events.");
        domainPropertyEventRows = getDomainPropertyEventsFromDomainEvents(AUDIT_PROPERTY_EVENTS_PROJECT, LIST_CHECK_LOG, ignoreIds);

        pass = validateExpectedRowInDomainPropertyAuditLog(domainPropertyEventRows, FIELD03_NAME, field03ExpectedColumns, null);

        // We are going to fail, so navigate to the Domain Property Events Audit Log.
        if(!pass)
            goToAuditEventView(this, DOMAIN_PROPERTY_LOG_NAME);

        Assert.assertTrue("The values logged for the 'Deleted' events for the lookup field were not as expected. See log for details.", pass);

        log("Ok, it looks like everything was logged as expected. Yipeee!");
    }

    private boolean validateExpectedRowInDomainPropertyAuditLog(List<Map<String, Object>> domainPropertyEventRows, String propertyName, Map<String, String> expectedColumns, @Nullable Map<String, String> expectedComment)
    {
        boolean pass = true;

        for(Map<String, Object> row : domainPropertyEventRows)
        {

            if(getLogColumnValue(row, "propertyname").equals(propertyName))
            {
                log("Validate the columns for property '" + propertyName + "'.");
                for(String fieldName : expectedColumns.keySet())
                {
                    if(!getLogColumnValue(row, fieldName).equals(expectedColumns.get(fieldName)))
                    {
                        pass = false;
                        log("************** For field '" + fieldName + "' expected value '" + expectedColumns.get(fieldName) + "' found '" + row.get(fieldName) + "' **************");
                    }
                }

                if(null != expectedComment)
                {
                    log("Validate that the Comment field is as expected.");
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
                log("************** Comment value does not contain expected value for field '" + key + "'. Expected '" + expected.get(key) + "' found '" + comment.get(key) + "'.  **************");
                pass = false;
            }
        }

        return pass;
    }

    private List<Map<String, Object>> getDomainPropertyEventsFromDomainEvents(String projectName, String domainName, @Nullable List<String> ignoreIds)
    {
        List<String> domainEventIds = getDomainEventIds(projectName, domainName);

        if(null != ignoreIds)
        {
            log("Removing the ignore ids from the list.");
            domainEventIds.removeAll(ignoreIds);
        }

        log("Get all of the Domain Property Events for '" + domainName + "' that are linked to the domain events.");
        List<Map<String, Object>> domainPropertyEventRows = getDomainPropertyEventLog(domainName, domainEventIds);
        log("Number of 'Domain Property Event' log entries: " + domainPropertyEventRows.size());

        return domainPropertyEventRows;
    }

    private List<String> getDomainEventIds(String projectName, String domainName)
    {
        log("Get a list of the Domain Events for project '" + projectName + "'. ");
        List<Map<String, Object>> domainAuditEventAllRows = getDomainEventLog(projectName);
        log("Number of 'Domain Event' log entries for '" + projectName + "': " + domainAuditEventAllRows.size());

        log("Filter the list to look only at '" + domainName + "'.");
        List<Map<String, Object>> domainAuditEventRows = new ArrayList<>();

        for(Map<String, Object> row : domainAuditEventAllRows)
        {
            if(getLogColumnValue(row, "domainname").toLowerCase().trim().equals(domainName.toLowerCase().trim()))
                domainAuditEventRows.add(row);
        }

        List<String> domainEventIds = new ArrayList<>();
        domainAuditEventRows.forEach((event)->domainEventIds.add(getLogColumnValue(event, "rowid")));

        log("Number of 'Domain Event' log entries for '" + domainName + "': " + domainEventIds.size());

        return domainEventIds;
    }

    private List<String> getDomainEventIdsFromPropertyEvents(List<Map<String, Object>> domainPropertyEventRows)
    {
        List<String> domainEventIds = new ArrayList<>();

        for(Map<String, Object> row : domainPropertyEventRows)
        {
            domainEventIds.add(getLogColumnValue(row, "domaineventid"));
        }

        return domainEventIds;
    }

    private List<Map<String, Object>> getDomainEventLog(String projectName)
    {
        Connection cn = WebTestHelper.getRemoteApiConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("auditLog", "DomainAuditEvent");
        cmd.setRequiredVersion(9.1);
        cmd.setColumns(Arrays.asList("rowid", "created", "createdby", "impersonatedby", "projectid", "domainuri", "domainname", "comment"));
        cmd.addFilter("projectid/DisplayName", projectName, Filter.Operator.EQUAL);
        cmd.setContainerFilter(ContainerFilter.AllFolders);

        return executeSelectCommand(cn, cmd);
    }

    private List<Map<String, Object>> getDomainPropertyEventLog(String domainName, @Nullable List<String> eventIds)
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
                if(stringBuilder.length() != 0)
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
            log("Number of rows: " + response.getRowCount());
            rowsReturned.addAll(response.getRows());
        }
        catch(IOException | CommandException ex)
        {
            // Just fail here, don't toss the exception up the stack.
            Assert.assertTrue("There was a command exception when getting the log: " + ex.toString(), false);
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
}
