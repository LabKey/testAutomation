/*
 * Copyright (c) 2008-2014 LabKey Corporation
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
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;

import static org.junit.Assert.*;

/**
 * User: Karl Lum
 * Date: Mar 13, 2008
 */
@Category({DailyA.class})
public class AuditLogTest extends BaseWebDriverTest
{
    public static final String USER_AUDIT_EVENT = "User events";
    public static final String GROUP_AUDIT_EVENT = "Group events";
    public static final String PROJECT_AUDIT_EVENT = "Project and Folder events";
    public static final String ASSAY_AUDIT_EVENT = "Copy-to-Study Assay events";

    private static final String AUDIT_TEST_USER = "audit_user1@auditlog.test";
    private static final String AUDIT_TEST_USER2 = "audit_user2@auditlog.test";

    private static final String AUDIT_TEST_PROJECT = "AuditVerifyTest";
    private static final String AUDIT_TEST_SUBFOLDER = "AuditVerifyTest_Subfolder";

    public static final String COMMENT_COLUMN = "Comment";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/audit";
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
            deleteProject(getProjectName(), false);
        }
    }

    protected void doTestSteps() throws Exception
    {
        userAuditTest();
        groupAuditTest();
        canSeeAuditLogTest();
    }

    protected void userAuditTest() throws Exception
    {
        log("testing user audit events");
        createUser(AUDIT_TEST_USER, null);
        impersonate(AUDIT_TEST_USER);
        stopImpersonating();
        signOut();
        signIn(AUDIT_TEST_USER, "asdf", false); // Bad login.  Existing User
        signIn(AUDIT_TEST_USER + "fail", "asdf", false); // Bad login.  Non-existent User
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

    protected void groupAuditTest() throws Exception
    {
        log("testing group audit events");

        _containerHelper.createProject(AUDIT_TEST_PROJECT, null);
        createPermissionsGroup("Testers");
        assertPermissionSetting("Testers", "No Permissions");
        setPermissions("Testers", "Editor");

        clickManageGroup("Testers");
        setFormElement(Locator.name("names"), AUDIT_TEST_USER);
        uncheckCheckbox("sendEmail");
        clickButton("Update Group Membership");
        deleteUsers(true, AUDIT_TEST_USER);
        deleteProject(AUDIT_TEST_PROJECT, true);

        verifyAuditEvent(this, GROUP_AUDIT_EVENT, COMMENT_COLUMN, "A new security group named Testers was created", 10);
        verifyAuditEvent(this, GROUP_AUDIT_EVENT, COMMENT_COLUMN, "The user/group Testers was assigned to the security role Editor.", 10);
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
        _containerHelper.createSubfolder(AUDIT_TEST_PROJECT, AUDIT_TEST_SUBFOLDER, null);
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
        setSiteAdminRoleUserPermissions(AUDIT_TEST_USER, "See Audit Log Events");
        impersonate(AUDIT_TEST_USER);
        verifyAuditQueries(true);

        // cleanup
        stopImpersonating();

        // verify issue 19515 - ensure that container filters are respected (i.e., a project admin without sub-folder admin access
        // should not see audit log events for that sub folder
        // verify our audit log only shows the row for the parent list since our user does not have project admin
        // permissions on the sub-folder
        impersonate(AUDIT_TEST_USER2);
        verifyListAuditLogQueries(false);
        stopImpersonating();
        // now give access to the sub-folder
        clickProject(AUDIT_TEST_PROJECT);
        clickFolder(AUDIT_TEST_SUBFOLDER);
        _securityHelper.setProjectPerm(AUDIT_TEST_USER2, "Folder Administrator");
        impersonate(AUDIT_TEST_USER2);
        verifyListAuditLogQueries(true);
        stopImpersonating();

        deleteUsers(true, AUDIT_TEST_USER);
        deleteUsers(true, AUDIT_TEST_USER2);
        deleteProject(AUDIT_TEST_PROJECT, true);
    }

    private void createList(String folderName, String listName)
    {
        ListHelper.ListColumn  lc = new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Name");
        _listHelper.createList(folderName, listName, ListHelper.ListColumnType.AutoInteger, "Key", lc);
        clickAndWait(Locator.linkWithText(folderName));
        clickAndWait(Locator.linkWithText(listName));
        clickButton("Insert New");
        setFormElement(Locator.name("quf_Name"), "Data");
        submit();
    }

    protected void verifyListAuditLogQueries(boolean canSeeSubFolders)
    {
        beginAt("/query/" + getProjectName() + "/executeQuery.view?schemaName=auditLog&query.queryName=ListAuditEvent&query.containerFilterName=CurrentAndSubfolders");
        verifyAuditQueryEvent(this, "List", "Parent List", 1);
        verifyAuditQueryEvent(this, "List", "Child List", 1, canSeeSubFolders);
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
            verifyAuditQueryEvent(this, COMMENT_COLUMN, "The user/group " + AUDIT_TEST_USER + " was assigned to the security role Editor.", 1);
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
            instance.prepForPageLoad();
            instance.selectOptionByText(Locator.name("view"), eventType);
            instance.newWaitForPageToLoad();
        }

        verifyAuditQueryEvent(instance, column, msg, rowsToSearch);
    }

    public static void verifyAuditQueryEvent(BaseWebDriverTest instance, String column, String msg, int rowsToSearch)
    {
        verifyAuditQueryEvent(instance, column, msg, rowsToSearch, true);
    }

    public static void verifyAuditQueryEvent(BaseWebDriverTest instance, String column, String msg, int rowsToSearch, boolean shouldFindText)
    {
        instance.log("searching for audit entry: " + msg);
        DataRegionTable table = new DataRegionTable("query", instance, false);
        int i = table.getColumn(column);
        if (shouldFindText)
            assertTrue("Text '" + msg + "' was not present", findTextInDataRegion(table, i, msg, rowsToSearch + 2));
        else
            assertFalse("Text '" + msg + "' was present", findTextInDataRegion(table, i, msg, rowsToSearch + 2));
    }

    public static boolean findTextInDataRegion(DataRegionTable table, int column, String txt, int rowsToSearch)
    {
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
