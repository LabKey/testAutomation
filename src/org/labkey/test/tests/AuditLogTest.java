/*
 * Copyright (c) 2008-2016 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Category({DailyA.class})
public class AuditLogTest extends BaseWebDriverTest
{
    public static final String USER_AUDIT_EVENT = "User events";
    public static final String GROUP_AUDIT_EVENT = "Group events";
    public static final String PROJECT_AUDIT_EVENT = "Project and Folder events";
    public static final String ASSAY_AUDIT_EVENT = "Copy-to-Study Assay events";

    private static final String AUDIT_TEST_USER = "audit_user1@auditlog.test";
    private static final String AUDIT_TEST_USER2 = "audit_user2@auditlog.test";
    private static final String AUDIT_TEST_USER3 = "audit_user3@auditlog.test";

    private static final String AUDIT_TEST_PROJECT = "AuditVerifyTest";
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
    protected void checkQueries(){} // Skip.  Project is deleted as part of test

    @Override
    protected void checkViews(){} // Skip.  Project is deleted as part of test

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        // Needed for pre-clean only. User & project are deleted during test.
        if (!afterTest)
        {
            deleteUsers(false, AUDIT_TEST_USER);
            _containerHelper.deleteProject(getProjectName(), false);
        }
    }

    @Test
    public void testSteps()
    {
        userAuditTest();
        groupAuditTest();
        canSeeAuditLogTest();
    }

    protected void userAuditTest()
    {
        log("testing user audit events");
        createUser(AUDIT_TEST_USER, null);
        impersonate(AUDIT_TEST_USER);
        stopImpersonating();
        signOut();
        signInShouldFail(AUDIT_TEST_USER, "asdf"); // Bad login.  Existing User
        signInShouldFail(AUDIT_TEST_USER + "fail", "asdf"); // Bad login.  Non-existent User
        simpleSignIn();
        deleteUsers(true, AUDIT_TEST_USER);

        verifyAuditEvent(this, USER_AUDIT_EVENT, COMMENT_COLUMN, AUDIT_TEST_USER + " was added to the system", 10);
        verifyAuditEvent(this, USER_AUDIT_EVENT, COMMENT_COLUMN, AUDIT_TEST_USER + " was impersonated by", 10);
        verifyAuditEvent(this, USER_AUDIT_EVENT, COMMENT_COLUMN, "impersonated " + AUDIT_TEST_USER, 10);
        verifyAuditEvent(this, USER_AUDIT_EVENT, COMMENT_COLUMN, AUDIT_TEST_USER + " was no longer impersonated by", 10);
        verifyAuditEvent(this, USER_AUDIT_EVENT, COMMENT_COLUMN, "stopped impersonating " + AUDIT_TEST_USER, 10);
        verifyAuditEvent(this, USER_AUDIT_EVENT, COMMENT_COLUMN, AUDIT_TEST_USER + " failed to login: incorrect password", 10);
        verifyAuditEvent(this, USER_AUDIT_EVENT, COMMENT_COLUMN, AUDIT_TEST_USER + "fail failed to login: user does not exist", 10);
        verifyAuditEvent(this, USER_AUDIT_EVENT, COMMENT_COLUMN, AUDIT_TEST_USER + " was deleted from the system", 10);
    }

    protected void groupAuditTest()
    {
        log("testing group audit events");

        _containerHelper.createProject(AUDIT_TEST_PROJECT, null);
        _permissionsHelper.createPermissionsGroup("Testers");
        _permissionsHelper.assertPermissionSetting("Testers", "No Permissions");
        _permissionsHelper.setPermissions("Testers", "Editor");

        _permissionsHelper.clickManageGroup("Testers");
        setFormElement(Locator.name("names"), AUDIT_TEST_USER);
        uncheckCheckbox(Locator.checkboxByName("sendEmail"));
        clickButton("Update Group Membership");
        deleteUsers(true, AUDIT_TEST_USER);
        _containerHelper.deleteProject(AUDIT_TEST_PROJECT, true);

        verifyAuditEvent(this, GROUP_AUDIT_EVENT, COMMENT_COLUMN, "A new security group named Testers was created", 10);
        verifyAuditEvent(this, GROUP_AUDIT_EVENT, COMMENT_COLUMN, "The group Testers was assigned to the security role Editor.", 10);
        verifyAuditEvent(this, GROUP_AUDIT_EVENT, COMMENT_COLUMN, "User: " + AUDIT_TEST_USER + " was added as a member to Group: Testers", 10);
        verifyAuditEvent(this, USER_AUDIT_EVENT, COMMENT_COLUMN, AUDIT_TEST_USER + " was deleted from the system", 10);

        log("testing project audit events");
        verifyAuditEvent(this, PROJECT_AUDIT_EVENT, COMMENT_COLUMN, AUDIT_TEST_PROJECT + " was created", 5);
        verifyAuditEvent(this, PROJECT_AUDIT_EVENT, COMMENT_COLUMN, AUDIT_TEST_PROJECT + " was deleted", 5);
    }

    protected void canSeeAuditLogTest()
    {
        log("testing CanSeeAuditLog permission");
        simpleSignIn();
        _containerHelper.createProject(AUDIT_TEST_PROJECT, null);
        _containerHelper.createSubfolder(AUDIT_TEST_PROJECT, AUDIT_TEST_SUBFOLDER);
        createList(AUDIT_TEST_PROJECT, "Parent List");
        createList(AUDIT_TEST_SUBFOLDER, "Child List");

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
        clickProject(AUDIT_TEST_PROJECT);
        clickFolder(AUDIT_TEST_SUBFOLDER);
        _securityHelper.setProjectPerm(AUDIT_TEST_USER2, "Folder Administrator");
        impersonate(AUDIT_TEST_USER2);
        verifyListAuditLogQueries(Visibility.All);
        stopImpersonating();

        // verify issue 19832 - opposite of above.  Ensure that user who has access to child folder but not parent folder can still see
        // audit log events from the child forder if using a CurrentAndSubFolders container filter
        createUserWithPermissions(AUDIT_TEST_USER3, AUDIT_TEST_PROJECT, "Editor");
        clickButton("Save and Finish");
        clickProject(AUDIT_TEST_PROJECT);
        clickFolder(AUDIT_TEST_SUBFOLDER);
        _securityHelper.setProjectPerm(AUDIT_TEST_USER3, "Folder Administrator");
        impersonate(AUDIT_TEST_USER3);
        verifyListAuditLogQueries(Visibility.ChildFolder);
        stopImpersonating();

        deleteUsers(true, AUDIT_TEST_USER);
        deleteUsers(true, AUDIT_TEST_USER2);
        deleteUsers(true, AUDIT_TEST_USER3);
        _containerHelper.deleteProject(AUDIT_TEST_PROJECT, true);
    }

    private void createList(String folderName, String listName)
    {
        ListHelper.ListColumn  lc = new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Name");
        _listHelper.createList(folderName, listName, ListHelper.ListColumnType.AutoInteger, "Key", lc);
        clickAndWait(Locator.linkWithText(folderName));
        clickAndWait(Locator.linkWithText(listName));
        DataRegionTable.findDataRegion(this).clickInsertNewRowDropdown();
        waitForElement(Locator.name("quf_Name"));
        setFormElement(Locator.name("quf_Name"), "Data");
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

    public static void verifyAuditEvent(BaseWebDriverTest instance, String eventType, String column, String msg, int rowsToSearch)
    {
        if (!instance.isTextPresent("Audit Log"))
        {
            instance.ensureAdminMode();

            instance.goToAdminConsole();
            instance.clickAndWait(Locator.linkWithText("audit log"));
        }

        if (!instance.getSelectedOptionText(Locator.name("view")).equals(eventType))
        {
            instance.doAndWaitForPageToLoad(() -> instance.selectOptionByText(Locator.name("view"), eventType));
        }

        verifyAuditQueryEvent(instance, column, msg, rowsToSearch);
    }

    public static void verifyAuditEvent(BaseWebDriverTest instance, String eventType, List<String> columns, List<String> msgs, int rowsToSearch)
    {
        if (!instance.isTextPresent("Audit Log"))
        {
            instance.ensureAdminMode();

            instance.goToAdminConsole();
            instance.clickAndWait(Locator.linkWithText("audit log"));
        }

        if (!instance.getSelectedOptionText(Locator.name("view")).equals(eventType))
        {
            instance.doAndWaitForPageToLoad(() -> instance.selectOptionByText(Locator.name("view"), eventType));
        }

        assertEquals(columns.size(), msgs.size());
        for (int i = 0; i < columns.size(); i += 1)
            verifyAuditQueryEvent(instance, columns.get(i), msgs.get(i), rowsToSearch);
    }

    public static void verifyAuditQueryEvent(BaseWebDriverTest instance, String column, String msg, int rowsToSearch)
    {
        verifyAuditQueryEvent(instance, column, msg, rowsToSearch, true);
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

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
