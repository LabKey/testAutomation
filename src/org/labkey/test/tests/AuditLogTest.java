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
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.BaseRowsCommand;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.RowsResponse;
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
import org.labkey.test.components.search.SearchBodyWebPart;
import org.labkey.test.pages.core.admin.logger.ManagerPage.LoggingLevel;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.pages.query.ExecuteQueryPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.AuditLogHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Log4jUtils;
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
import static org.labkey.test.util.PermissionsHelper.AUTHOR_ROLE;
import static org.labkey.test.util.PermissionsHelper.EDITOR_ROLE;
import static org.labkey.test.util.PermissionsHelper.FOLDER_ADMIN_ROLE;
import static org.labkey.test.util.PermissionsHelper.PROJECT_ADMIN_ROLE;

@Category({Daily.class, Hosting.class})
@BaseWebDriverTest.ClassTimeout(minutes = 9)
public class AuditLogTest extends BaseWebDriverTest
{
    public static final String USER_AUDIT_EVENT = "User events";
    public static final String GROUP_AUDIT_EVENT = "Group and role events";
    public static final String QUERY_UPDATE_EVENT = "Query update events";
    public static final String PROJECT_AUDIT_EVENT = "Project and Folder events";
    public static final String ASSAY_AUDIT_EVENT = "Link to Study events";
    public static final String COMMENT_COLUMN = "Comment";

    private static final String AUDIT_TEST_USER = "audit_user1@auditlog.test";
    private static final String AUDIT_TEST_USER2 = "audit_user2@auditlog.test";
    private static final String AUDIT_TEST_USER3 = "audit_user3@auditlog.test";
    private static final String AUDIT_SECURITY_GROUP = "Testers";
    private static final String AUDIT_TEST_PROJECT = "AuditVerifyTest";
    private static final String AUDIT_DETAILED_TEST_PROJECT = "AuditDetailedLogTest";
    private static final String AUDIT_TEST_SUBFOLDER = "AuditVerifyTest_Subfolder";
    private static final String AUDIT_PROPERTY_EVENTS_PROJECT = "AuditDomainPropertyEvents";
    private static final String DOMAIN_PROPERTY_LOG_NAME = "Domain property events";
    private static final String SEARCH_TERM = "doesn't matter";

    private final ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);
    private final AuditLogHelper _auditLogHelper = new AuditLogHelper(this);

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

        // Check to see if all the expected values did show up.
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
        if (!diff.isEmpty())
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

        String usersGroup = "Users";
        impersonateGroup(usersGroup, true);
        expectedLogValues.add(getUsername() + " impersonated group: " + usersGroup);

        stopImpersonating();
        expectedLogValues.add(getUsername() + " stopped impersonating group: " + usersGroup);

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
        permissionsHelper.setPermissions(AUDIT_SECURITY_GROUP, EDITOR_ROLE);
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
        expectedLogValues.add("A new security group named Users was created.");
        expectedLogValues.add("The security group named Users was deleted.");
        expectedLogValues.add("The security group named Testers was deleted.");

        verifyAuditEvent(this, GROUP_AUDIT_EVENT, COMMENT_COLUMN, expectedLogValues.get(1), expectedLogValues.size());
        verifyAuditEvent(this, GROUP_AUDIT_EVENT, COMMENT_COLUMN, expectedLogValues.get(3), expectedLogValues.size());
        verifyAuditEvent(this, GROUP_AUDIT_EVENT, COMMENT_COLUMN, expectedLogValues.get(5), expectedLogValues.size());
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

        createUserWithPermissions(AUDIT_TEST_USER, AUDIT_TEST_PROJECT, EDITOR_ROLE);
        createUserWithPermissions(AUDIT_TEST_USER2, AUDIT_TEST_PROJECT, PROJECT_ADMIN_ROLE);

        // Do a search to ensure an audit entry in /home
        clickProject("Home");
        new SearchBodyWebPart(getDriver()).searchForm().searchFor(SEARCH_TERM);
        goToProjectHome();

        // signed in as an admin so we should see rows here
        verifyAuditQueries(true, getProjectName());

        // signed in as an editor should not show any rows for audit query links
        impersonate(AUDIT_TEST_USER);
        verifyAuditQueries(false, getProjectName());
        verifyAuditQueries(false, "Home");
        stopImpersonating();

        // Grant the "See Audit Log Events" folder role to our audit user in the project and verify we see audit
        // information in this project but not /Home. We pass the fully qualified classnames in the next few calls to
        // disambiguate the root role from the folder role.
        permissionsHelper.addMemberToRole(AUDIT_TEST_USER, "org.labkey.api.security.roles.CanSeeAuditLogFolderRole", PermissionsHelper.MemberType.user, getProjectName());
        impersonate(AUDIT_TEST_USER);
        verifyAuditQueries(true, getProjectName());
        verifyAuditQueries(false, "Home");
        stopImpersonating();
        permissionsHelper.removeUserRoleAssignment(AUDIT_TEST_USER, "org.labkey.api.security.roles.CanSeeAuditLogFolderRole", getProjectName());

        // Grant the "See Audit Log Events" root role to our audit user and verify we see audit information in this
        // project and in /Home
        permissionsHelper.setSiteRoleUserPermissions(AUDIT_TEST_USER, "org.labkey.api.security.roles.CanSeeAuditLogRole");
        impersonate(AUDIT_TEST_USER);
        verifyAuditQueries(true, getProjectName());
        ExecuteQueryPage.beginAt(this, "Home", "auditLog", "SearchAuditEvent");
        verifyAuditQueryEvent(this, "Query", SEARCH_TERM, 1);

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

        ApiPermissionsHelper ph = new ApiPermissionsHelper(this);
        ph.addMemberToRole(AUDIT_TEST_USER2,FOLDER_ADMIN_ROLE, PermissionsHelper.MemberType.user);
        impersonate(AUDIT_TEST_USER2);
        verifyListAuditLogQueries(Visibility.All);
        stopImpersonating();

        // verify issue 19832 - opposite of above.  Ensure that user who has access to child folder but not parent folder can still see
        // audit log events from the child forder if using a CurrentAndSubFolders container filter
        createUserWithPermissions(AUDIT_TEST_USER3, AUDIT_TEST_PROJECT, EDITOR_ROLE);
        permissionsHelper.addMemberToRole(AUDIT_TEST_USER3, FOLDER_ADMIN_ROLE, PermissionsHelper.MemberType.user, AUDIT_TEST_PROJECT + "/" + AUDIT_TEST_SUBFOLDER);
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
            goToProjectHome(AUDIT_DETAILED_TEST_PROJECT);

            Connection cn = WebTestHelper.getRemoteApiConnection();

            // create manufacturer (which has summary audit log level)
            InsertRowsCommand insertCmd = new InsertRowsCommand("vehicle", "manufacturers");
            Map<String, Object> rowMap = new HashMap<>();
            rowMap.put("name", "Kia");
            insertCmd.addRow(rowMap);
            RowsResponse resp1 = insertCmd.execute(cn, AUDIT_DETAILED_TEST_PROJECT);

            Integer transactionId = _auditLogHelper.checkAuditEventDiffCountForLastTransaction(AUDIT_DETAILED_TEST_PROJECT, AuditLogHelper.AuditEvent.QUERY_UPDATE_AUDIT_EVENT, 0, 1);
            Map<String, Object> expectedValues = new HashMap<>();
            expectedValues.put("Comment", "1 row(s) were inserted.");
            _auditLogHelper.checkAuditEventValuesForTransactionId(AUDIT_DETAILED_TEST_PROJECT, AuditLogHelper.AuditEvent.QUERY_UPDATE_AUDIT_EVENT, transactionId, 1, expectedValues);
            goToProjectHome(AUDIT_DETAILED_TEST_PROJECT);

            Map<String, String> auditLog = getAuditLogRow(this, "Query update events", "Query Name", "Manufacturers");
            assertEquals("Did not find expected audit log for summary log level", "1 row(s) were inserted.", auditLog.get("Comment"));

            // create manufacturer (which has summary audit log level) with api audit override to detail
            insertCmd = new InsertRowsCommand("vehicle", "manufacturers");
            insertCmd.setAuditBehavior(BaseRowsCommand.AuditBehavior.DETAILED);
            rowMap = new HashMap<>();
            rowMap.put("name", "Kia_ev");
            insertCmd.addRow(rowMap);
            insertCmd.execute(cn, AUDIT_DETAILED_TEST_PROJECT);

            goToProjectHome(AUDIT_DETAILED_TEST_PROJECT);
            transactionId = _auditLogHelper.checkAuditEventDiffCountForLastTransaction(AUDIT_DETAILED_TEST_PROJECT, AuditLogHelper.AuditEvent.QUERY_UPDATE_AUDIT_EVENT, 7, 1);
            expectedValues = new HashMap<>();
            expectedValues.put("Comment", "A row was inserted.");
            _auditLogHelper.checkAuditEventValuesForTransactionId(AUDIT_DETAILED_TEST_PROJECT, AuditLogHelper.AuditEvent.QUERY_UPDATE_AUDIT_EVENT, transactionId, 1, expectedValues);

            // create manufacturer (which has summary audit log level) with api audit override to NONE. The override should be ignored
            insertCmd = new InsertRowsCommand("vehicle", "manufacturers");
            insertCmd.setAuditBehavior(BaseRowsCommand.AuditBehavior.NONE);
            rowMap = new HashMap<>();
            rowMap.put("name", "Kia_hybrid");
            insertCmd.addRow(rowMap);
            insertCmd.execute(cn, AUDIT_DETAILED_TEST_PROJECT);

            goToProjectHome(AUDIT_DETAILED_TEST_PROJECT);
            transactionId = _auditLogHelper.checkAuditEventDiffCountForLastTransaction(AUDIT_DETAILED_TEST_PROJECT, AuditLogHelper.AuditEvent.QUERY_UPDATE_AUDIT_EVENT, 0, 1);
            expectedValues = new HashMap<>();
            expectedValues.put("Comment", "1 row(s) were inserted.");
            _auditLogHelper.checkAuditEventValuesForTransactionId(AUDIT_DETAILED_TEST_PROJECT, AuditLogHelper.AuditEvent.QUERY_UPDATE_AUDIT_EVENT, transactionId, 1, expectedValues);

            //then create model (which has detailed audit log level)
            InsertRowsCommand insertCmd2 = new InsertRowsCommand("vehicle", "models");
            rowMap = new HashMap<>();
            rowMap.put("manufacturerId", resp1.getRows().getFirst().get("rowid"));
            rowMap.put("name", "Soul");
            insertCmd2.addRow(rowMap);
            insertCmd2.execute(cn, AUDIT_DETAILED_TEST_PROJECT);

            refresh();
            auditLog = getAuditLogRow(this, "Query update events", "Query Name", "Models");
            assertEquals("Did not find expected audit log for detailed log level", "A row was inserted.", auditLog.get("Comment"));
            goToProjectHome(AUDIT_DETAILED_TEST_PROJECT);

            // create model (which has detailed audit log level), with API audit SUMMARY, effective audit should be detailed
            rowMap.put("name", "Carnival");
            insertCmd2 = new InsertRowsCommand("vehicle", "models");
            insertCmd2.setAuditBehavior(BaseRowsCommand.AuditBehavior.SUMMARY);
            insertCmd2.addRow(rowMap);
            insertCmd2.execute(cn, AUDIT_DETAILED_TEST_PROJECT);

            transactionId = _auditLogHelper.checkAuditEventDiffCountForLastTransaction(AUDIT_DETAILED_TEST_PROJECT, AuditLogHelper.AuditEvent.QUERY_UPDATE_AUDIT_EVENT, 8, 1);
            expectedValues = new HashMap<>();
            expectedValues.put("Comment", "A row was inserted.");
            _auditLogHelper.checkAuditEventValuesForTransactionId(AUDIT_DETAILED_TEST_PROJECT, AuditLogHelper.AuditEvent.QUERY_UPDATE_AUDIT_EVENT, transactionId, 1, expectedValues);

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
        ExecuteQueryPage.getPageFactory("auditLog", "ListAuditEvent")
                .addParameter("query.containerFilterName", "CurrentAndSubfolders")
                .navigate(this, getProjectName());
        verifyAuditQueryEvent(this, "List", "Parent List", 1, canSeeParent(v));
        verifyAuditQueryEvent(this, "List", "Child List", 1, canSeeChild(v));
    }

    protected void verifyAuditQueries(boolean canSeeAuditLog, String containerPath)
    {
        ExecuteQueryPage.beginAt(this, containerPath, "auditLog", "ContainerAuditEvent");
        if (canSeeAuditLog)
            verifyAuditQueryEvent(this, COMMENT_COLUMN, AUDIT_TEST_PROJECT + " was created", 1);
        else
            assertTextPresent("No data to show.");

        ExecuteQueryPage.beginAt(this, containerPath, "auditLog", "GroupAuditEvent");
        if (canSeeAuditLog)
            verifyAuditQueryEvent(this, COMMENT_COLUMN, "The user " + AUDIT_TEST_USER + " was assigned to the security role Editor.", 4);
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

        log("Validate that the expected rows are there.");

        String eventInitValue = "Name=ChangeMyColumns&AllowDelete=true&AllowUpload=true&AllowExport=true&EntireListIndexSetting=0" +
                "&EntireListBodySetting=0&EachItemBodySetting=0&EntireListIndex=false&EachItemIndex=false&FileAttachmentIndex=false";
        AuditLogHelper.DetailedAuditEventRow expectedDomainEvent = new AuditLogHelper.DetailedAuditEventRow(null, LIST_CHECK_LOG, null,
                "The domain " + LIST_CHECK_LOG + " was created. The column(s) of domain " + LIST_CHECK_LOG + " were modified.",
                null, null, eventInitValue, null);
        String field1InitValue = "Name=Field01&Label=This%20is%20Field%2001&Type=String&Scale=4000&Description=Simple%20String%20field." +
                "&PHI=Not%20PHI&DefaultScale=Linear&Required=false&Hidden=false&MvEnabled=false&Measure=false&Dimension=false" +
                "&ShownInInsert=true&ShownInDetails=true&ShownInUpdate=true&ShownInLookupView=false" +
                "&RecommendedVariable=false&ExcludedFromShifting=false&Scannable=false&DefaultValueType=Editable%20default";
        AuditLogHelper.DetailedAuditEventRow field1ExpectedEvent = new AuditLogHelper.DetailedAuditEventRow(null, FIELD01_NAME, "Created",
                null, null, null, field1InitValue, null);

        String field2InitValue = "Name=Field02&Label=This%20is%20Field%2002&Type=Integer&Description=Simple%20Integer%20field." +
                "&PHI=Not%20PHI&DefaultScale=Linear&Required=false&Hidden=false&MvEnabled=false&Measure=true&Dimension=false" +
                "&ShownInInsert=true&ShownInDetails=true&ShownInUpdate=true&ShownInLookupView=false&RecommendedVariable=false" +
                "&ExcludedFromShifting=false&Scannable=false&DefaultValueType=Editable%20default";
        AuditLogHelper.DetailedAuditEventRow field2ExpectedEvent = new AuditLogHelper.DetailedAuditEventRow(null, FIELD02_NAME, "Created",
                null, null, null, field2InitValue, null);

        AuditLogHelper.DetailedAuditEventRow keyFieldExpectedEvent = new AuditLogHelper.DetailedAuditEventRow(null, "Key", "Created",
                null, null, null, null, null);
        validateLastDomainAuditEvents(LIST_CHECK_LOG, "The values logged for the 'Created' events were not as expected.", expectedDomainEvent, Map.of("Key", keyFieldExpectedEvent, FIELD01_NAME, field1ExpectedEvent, FIELD02_NAME, field2ExpectedEvent));

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

        expectedDomainEvent = new AuditLogHelper.DetailedAuditEventRow(null, LIST_CHECK_LOG, null,
                "The column(s) of domain " + LIST_CHECK_LOG + " were modified.",
                "", eventInitValue, eventInitValue, null);

        String field1UpdateValue = "Name=Field01&Label=This%20is%20Update%20Label%20for%20Field%2001&Type=String&Scale=4000&Description=This%20should%20be%20a%20new%20description%20for%20the%20field." +
                "&PHI=Restricted%20PHI&DefaultScale=Linear&Required=true&Hidden=false&MvEnabled=false&Measure=false&Dimension=false&ShownInInsert=true&ShownInDetails=true&ShownInUpdate=true" +
                "&ShownInLookupView=false&RecommendedVariable=false&ExcludedFromShifting=false&Scannable=false&DefaultValueType=Editable%20default";
        field1ExpectedEvent = new AuditLogHelper.DetailedAuditEventRow(null, FIELD01_NAME, "Modified",
                "The following properties were updated: Label, Description, PHI, Required", "", field1InitValue, field1UpdateValue, null);
        String field2UpdateValue = "Name=Field02&Label=This%20is%20Field%2002&Type=Integer&Description=Simple%20Integer%20field.&Format=%23!&PHI=Not%20PHI" +
                "&DefaultScale=Log&Required=false&Hidden=false&MvEnabled=false&Measure=true&Dimension=false&ShownInInsert=true&ShownInDetails=true&ShownInUpdate=true" +
                "&ShownInLookupView=false&RecommendedVariable=false&ExcludedFromShifting=false&Scannable=false&DefaultValueType=Editable%20default&ConditionalFormat=format.column~eq%3D5%3A%20";
        field2ExpectedEvent = new AuditLogHelper.DetailedAuditEventRow(null, FIELD02_NAME, "Modified",
                "The following properties were updated: DefaultScale, Format, ConditionalFormat", "", field2InitValue, field2UpdateValue, null);

        log("Validate that the expected rows after the update are in the log.");
        validateLastDomainAuditEvents(LIST_CHECK_LOG, "The values logged for the 'Modified' events were not as expected.", expectedDomainEvent, Map.of(FIELD01_NAME, field1ExpectedEvent, FIELD02_NAME, field2ExpectedEvent));

        log("The modified events were logged as expected. Now add a lookup field.");
        goToProjectHome(AUDIT_PROPERTY_EVENTS_PROJECT);
        clickAndWait(Locator.linkWithText(LIST_CHECK_LOG));
        listDefinitionPage = _listHelper.goToEditDesign(LIST_CHECK_LOG);
        listDefinitionPage.getFieldsPanel()
                .addField(new FieldDefinition(FIELD03_NAME,
                        new FieldDefinition.IntLookup(null, "lists", LOOK_UP_LIST01))
                        .setLabel(FIELD03_LABEL));
        listDefinitionPage.clickSave();

        String field3CreateValue = "Name=Field03&Label=Field%2003%20Lookup&Type=Integer&PHI=Not%20PHI&DefaultScale=Linear&Required=false&Hidden=false&MvEnabled=false" +
                "&Measure=false&Dimension=true&ShownInInsert=true&ShownInDetails=true&ShownInUpdate=true&ShownInLookupView=false&RecommendedVariable=false&ExcludedFromShifting=false&Scannable=false" +
                "&DefaultValueType=Editable%20default&Lookup=%7B%22queryName%22%3A%22LookUp01%22%2C%22schemaName%22%3A%22lists%22%7D";
        AuditLogHelper.DetailedAuditEventRow field3ExpectedEvent = new AuditLogHelper.DetailedAuditEventRow(null, FIELD03_NAME, "Created",
                null, "", null, field3CreateValue, null);

        log("Validate that the expected rows after the update are in the log.");
        validateLastDomainAuditEvents(LIST_CHECK_LOG, "The values logged for the 'Created' event for the lookup field were not as expected.", expectedDomainEvent, Map.of(FIELD03_NAME, field3ExpectedEvent));

        log("The 'Created' event was logged as expected. Now modify the field to point to a new list in the lookup field.");
        goToProjectHome(AUDIT_PROPERTY_EVENTS_PROJECT);
        clickAndWait(Locator.linkWithText(LIST_CHECK_LOG));
        listDefinitionPage = _listHelper.goToEditDesign(LIST_CHECK_LOG);

        log("Change properties on field '" + FIELD03_NAME + "'.");
        listDefinitionPage.getFieldsPanel()
                .getField(FIELD03_NAME)
                .setLookup(new FieldDefinition.IntLookup(null, "lists", LOOK_UP_LIST02));
        listDefinitionPage.clickSave();

        log("Validate that the expected rows after the update are in the log.");
        String field3UpdateValue = "Name=Field03&Label=Field%2003%20Lookup&Type=Integer&PHI=Not%20PHI&DefaultScale=Linear&Required=false&Hidden=false&MvEnabled=false" +
                "&Measure=false&Dimension=true&ShownInInsert=true&ShownInDetails=true&ShownInUpdate=true&ShownInLookupView=false&RecommendedVariable=false&ExcludedFromShifting=false" +
                "&Scannable=false&DefaultValueType=Editable%20default&Lookup=%7B%22queryName%22%3A%22LookUp02%22%2C%22schemaName%22%3A%22lists%22%7D";
        field3ExpectedEvent = new AuditLogHelper.DetailedAuditEventRow(null, FIELD03_NAME, "Modified",
                "The following property was updated: Lookup", "", field3CreateValue, field3UpdateValue, null);
        boolean pass = _auditLogHelper.validateLastDomainAuditEvents(LIST_CHECK_LOG, AUDIT_PROPERTY_EVENTS_PROJECT, expectedDomainEvent, Map.of(FIELD03_NAME, field3ExpectedEvent));

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

        field3ExpectedEvent = new AuditLogHelper.DetailedAuditEventRow(null, FIELD03_NAME, "Deleted",
                "", "", null, null, null);
        validateLastDomainAuditEvents(LIST_CHECK_LOG, "The values logged for the 'Deleted' events for the lookup field were not as expected.", expectedDomainEvent, Map.of(FIELD03_NAME, field3ExpectedEvent));

        log("Ok, it looks like everything was logged as expected. Yipeee!");
    }

    private void validateLastDomainAuditEvents(String domainName, String failComment, AuditLogHelper.DetailedAuditEventRow domainEvent, Map<String, AuditLogHelper.DetailedAuditEventRow> propertyEvents)
    {
        boolean pass = _auditLogHelper.validateLastDomainAuditEvents(domainName, AUDIT_PROPERTY_EVENTS_PROJECT, domainEvent, propertyEvents);
        if(!pass)
            goToAuditEventView(this, DOMAIN_PROPERTY_LOG_NAME);

        Assert.assertTrue(failComment + " See log for details.", pass);
    }
}
