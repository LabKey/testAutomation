/*
 * Copyright (c) 2008-2017 LabKey Corporation
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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestProperties;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Hosting;
import org.labkey.test.pages.core.admin.logger.ManagerPage;
import org.labkey.test.pages.core.admin.logger.ManagerPage.LoggingLevel;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PasswordUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({DailyA.class, Hosting.class})
public class AuditLogTest extends BaseWebDriverTest
{
    public static final String USER_AUDIT_EVENT = "User events";
    public static final String GROUP_AUDIT_EVENT = "Group events";
    public static final String QUERY_UPDATE_EVENT = "Query update events";
    public static final String PROJECT_AUDIT_EVENT = "Project and Folder events";
    public static final String ASSAY_AUDIT_EVENT = "Copy-to-Study Assay events";

    private static final String AUDIT_TEST_USER = "audit_user1@auditlog.test";
    private static final String AUDIT_TEST_USER2 = "audit_user2@auditlog.test";
    private static final String AUDIT_TEST_USER3 = "audit_user3@auditlog.test";

    private static final String AUDIT_SECURITY_GROUP = "Testers";

    private static final String AUDIT_TEST_PROJECT = "AuditVerifyTest";
    private static final String AUDIT_DETAILED_TEST_PROJECT = "AuditDetailedLogTest";
    private static final String AUDIT_TEST_SUBFOLDER = "AuditVerifyTest_Subfolder";

    public static final String COMMENT_COLUMN = "Comment";

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("audit");
    }

    public enum Visibility
    {
        ParentFolder, // an only see log events in parent
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

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        // Needed for pre-clean only. User & project are deleted during test.
        if (!afterTest)
        {
            _userHelper.deleteUsers(false, AUDIT_TEST_USER);
            _containerHelper.deleteProject(getProjectName(), false);
            _containerHelper.deleteProject(AUDIT_DETAILED_TEST_PROJECT, false);
        }
    }

    @Test
    public void testSteps() throws IOException
    {
        turnOnAuditLogFile();

        userAuditTest();
        groupAuditTest();
        canSeeAuditLogTest();
    }

    protected void turnOnAuditLogFile()
    {
        goToHome();

        ManagerPage lmp = ManagerPage.beginAt(this);

        lmp.setSearchText("org.labkey.audit.event");

        log("Setting org.labkey.audit.event and org.labkey.audit.event.UserAuditEvent to ALL.");
        if (lmp.getLoggingLevel("org.labkey.audit.event") != LoggingLevel.ALL)
            lmp.setLoggingLevel("org.labkey.audit.event", LoggingLevel.ALL).clickRefresh();

        // Setting org.labkey.audit.event.UserAuditEvent because it is called out in the webapps/log4j.xml file.
        if ((lmp.isLoggerPresent("org.labkey.audit.event.UserAuditEvent")) && (lmp.getLoggingLevel("org.labkey.audit.event.UserAuditEvent") != LoggingLevel.ALL))
            lmp.setLoggingLevel("org.labkey.audit.event.UserAuditEvent", LoggingLevel.ALL).clickRefresh();

        lmp.setSearchText("").clickRefresh();

    }

    protected ArrayList<String> getAuditLogFromFile() throws IOException
    {
        ArrayList<String> auditLog = new ArrayList<>();
        File auditLogFile = new File(TestProperties.getTomcatHome(), "logs/labkey-audit.log");

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

    protected void compareAuditLogFileEntries(ArrayList<String> auditLogBefore, ArrayList<String> auditLogAfter, ArrayList<String> expectedValues)
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

        assertTrue(stringBuilder.toString(), pass);
    }

    protected void userAuditTest() throws IOException
    {
        ArrayList<String> auditLogBefore;
        ArrayList<String> auditLogAfter;

        auditLogBefore = getAuditLogFromFile();

        log("testing user audit events");
        _userHelper.createUser(AUDIT_TEST_USER);
        impersonate(AUDIT_TEST_USER);
        stopImpersonating();
        signOut();
        signInShouldFail(AUDIT_TEST_USER, "asdf"); // Bad login.  Existing User
        signInShouldFail(AUDIT_TEST_USER + "fail", "asdf"); // Bad login.  Non-existent User
        simpleSignIn();
        _userHelper.deleteUsers(true, AUDIT_TEST_USER);

        ArrayList<String> expectedLogValues = new ArrayList<>();
        expectedLogValues.add(AUDIT_TEST_USER + " was added to the system and the administrator chose not to send a verification email.");
        expectedLogValues.add(getCurrentUser() + " impersonated " + AUDIT_TEST_USER);
        expectedLogValues.add(AUDIT_TEST_USER + " was impersonated by " + getCurrentUser());
        expectedLogValues.add(AUDIT_TEST_USER + " was no longer impersonated by " + getCurrentUser());
        expectedLogValues.add(getCurrentUser() + " stopped impersonating " + AUDIT_TEST_USER);
        expectedLogValues.add(getCurrentUser() + " logged out.");
        expectedLogValues.add(AUDIT_TEST_USER + " failed to login: incorrect password");
        expectedLogValues.add(getCurrentUser() + " logged in successfully via Database authentication.");
        expectedLogValues.add(AUDIT_TEST_USER + "fail failed to login: user does not exist");
        expectedLogValues.add(AUDIT_TEST_USER + " was deleted from the system");

        for (String msg : expectedLogValues)
        {
            verifyAuditEvent(this, USER_AUDIT_EVENT, COMMENT_COLUMN, msg, 10);
        }

        // Check the file after the UI check, if the UI tests passed then we should have confidence that the entry is in the file.
        auditLogAfter = getAuditLogFromFile();

        compareAuditLogFileEntries(auditLogBefore, auditLogAfter, expectedLogValues);
    }

    protected void groupAuditTest() throws IOException
    {
        ArrayList<String> auditLogBefore;
        ArrayList<String> auditLogAfter;

        auditLogBefore = getAuditLogFromFile();

        log("testing group audit events");

        _containerHelper.createProject(AUDIT_TEST_PROJECT, null);
        _permissionsHelper.createPermissionsGroup(AUDIT_SECURITY_GROUP);
        _permissionsHelper.assertPermissionSetting(AUDIT_SECURITY_GROUP, "No Permissions");
        _permissionsHelper.setPermissions(AUDIT_SECURITY_GROUP, "Editor");

        _permissionsHelper.clickManageGroup(AUDIT_SECURITY_GROUP);
        setFormElement(Locator.name("names"), AUDIT_TEST_USER);
        uncheckCheckbox(Locator.checkboxByName("sendEmail"));
        clickButton("Update Group Membership");
        _userHelper.deleteUsers(true, AUDIT_TEST_USER);
        _containerHelper.deleteProject(AUDIT_TEST_PROJECT, true);

        ArrayList<String> expectedLogValues = new ArrayList<>();
        expectedLogValues.add("Project " + AUDIT_TEST_PROJECT + " was created");
        expectedLogValues.add("A new security group named " + AUDIT_SECURITY_GROUP + " was created.");
        expectedLogValues.add("The group Guests was removed from the security role No Permissions.");
        expectedLogValues.add("The group " + AUDIT_SECURITY_GROUP + " was assigned to the security role Editor.");
        expectedLogValues.add(AUDIT_TEST_USER + " was added to the system and the administrator chose not to send a verification email.");
        expectedLogValues.add("User: " + AUDIT_TEST_USER + " was added as a member to Group: " + AUDIT_SECURITY_GROUP);
        expectedLogValues.add(AUDIT_TEST_USER + " was deleted from the system");
        expectedLogValues.add("Project /" + AUDIT_TEST_PROJECT + " was deleted");

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
        createList(AUDIT_TEST_PROJECT, "Parent List");
        createList(AUDIT_TEST_PROJECT + "/" + AUDIT_TEST_SUBFOLDER, "Child List");

        createUserWithPermissions(AUDIT_TEST_USER, AUDIT_TEST_PROJECT, "Editor");
        clickButton("Save and Finish");
        createUserWithPermissions(AUDIT_TEST_USER2, AUDIT_TEST_PROJECT, "Project Administrator");
        clickButton("Save and Finish");

        // signed in as an admin so we should see rows here
        verifyAuditQueries(true);

        // signed in as an editor should not show any rows for audit query links
        impersonate(AUDIT_TEST_USER);
        verifyAuditQueries(false);
        stopImpersonating();

        // now grant CanSeeAuditLog permission to our audit user and verify
        // we see audit information
        _permissionsHelper.setSiteAdminRoleUserPermissions(AUDIT_TEST_USER, "See Audit Log Events");
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
        clickButton("Save and Finish");
        navigateToFolder(AUDIT_TEST_PROJECT, AUDIT_TEST_SUBFOLDER);
        _securityHelper.setProjectPerm(AUDIT_TEST_USER3, "Folder Administrator");
        impersonate(AUDIT_TEST_USER3);
        verifyListAuditLogQueries(Visibility.ChildFolder);
        stopImpersonating();

        _userHelper.deleteUsers(true, AUDIT_TEST_USER, AUDIT_TEST_USER2, AUDIT_TEST_USER3);
        _containerHelper.deleteProject(AUDIT_TEST_PROJECT, true);
    }

    @Test
    public void testDetailedQueryUpdateAuditLog() throws IOException, CommandException
    {
        _containerHelper.createProject(AUDIT_DETAILED_TEST_PROJECT, "Custom");
        _containerHelper.enableModule("simpletest");
        goToProjectHome();

        Connection cn = new Connection(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

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

    private void createList(String containerPath, String listName)
    {
        ListHelper.ListColumn lc = new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Name");
        _listHelper.createList(containerPath, listName, ListHelper.ListColumnType.AutoInteger, "Key", lc);
        goToManageLists();
        clickAndWait(Locator.linkWithText(listName));
        DataRegionTable.DataRegion(getDriver()).find().clickInsertNewRow();
        setFormElement(Locator.name("quf_Name").waitForElement(shortWait()), "Data");
        submit();
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
        if (!instance.isTextPresent("Audit Log"))
        {
            instance.ensureAdminMode();

            instance.goToAdminConsole().clickAuditLog();
        }

        if (!instance.getSelectedOptionText(Locator.name("view")).equals(eventType))
        {
            instance.doAndWaitForPageToLoad(() -> instance.selectOptionByText(Locator.name("view"), eventType));
        }
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
            assertTrue("Text '" + msg + "' was not present", findTextInDataRegion(table, i, msg, rowsToSearch + 2));
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

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
