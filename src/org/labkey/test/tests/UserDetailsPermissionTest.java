/*
 * Copyright (c) 2012-2026 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimpleGetCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.query.ExecuteQueryPage;
import org.labkey.test.pages.user.ShowUsersPage;
import org.labkey.test.pages.user.UpdateUserDetailsPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.DataRegionTable.DataRegionFinder;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.labkey.test.util.PermissionsHelper.PROJECT_ADMIN_ROLE;
import static org.labkey.test.util.PermissionsHelper.READER_ROLE;
import static org.labkey.test.util.PermissionsHelper.SITE_ADMIN_ROLE;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class UserDetailsPermissionTest extends BaseWebDriverTest
{
    private static final String TEST_GROUP = "HiddenEmail Test group";
    private static final String ADMIN_USER = "admin@usertable.test";
    private static final String USER_INFO_VIEWER = "user_info_viewer@usertable.test";
    private static final String IMPERSONATED_USER = "impersonated_user@usertable.test";
    private static final String CHECKED_USER = "checked_user@usertable.test";
    private static final String PROJECT_ADMIN = "project_admin@usertable.test";
    private static final String NON_MEMBER = "non_member@usertable.test";
    private static final String EMAIL_TEST_LIST = "My Users";
    private static final String CUSTOM_USER_COLUMN = "UserTablePermTest";
    private static final String HIDDEN_COL_VIEW = "hiddenColView";
    private static final String HIDDEN_STRING = "HIDDEN_VALUE";

    @Override
    protected String getProjectName()
    {
        return "Hidden Email Test";
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);

        _userHelper.deleteUsers(false, USER_INFO_VIEWER, IMPERSONATED_USER, CHECKED_USER, ADMIN_USER, PROJECT_ADMIN, NON_MEMBER);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @BeforeClass
    public static void setup()
    {
        UserDetailsPermissionTest initTest = getCurrentTest();
        initTest.doSetup();
    }

    private void doSetup()
    {
        DomainDesignerPage domainDesignerPage = goToSiteUsers().clickChangeUserProperties();
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        if (domainFormPanel.getField(CUSTOM_USER_COLUMN) == null)
            domainFormPanel.addField(CUSTOM_USER_COLUMN).setType(FieldDefinition.ColumnType.String);
        domainDesignerPage.clickFinish();

        int adminId = _userHelper.createUser(ADMIN_USER, true, true).getUserId();
        int userInfoId = _userHelper.createUser(USER_INFO_VIEWER, true, true).getUserId();
        int impersonatedId = _userHelper.createUser(IMPERSONATED_USER, true, true).getUserId();
        _userHelper.createUser(CHECKED_USER, true, true);
        setInitialPassword(adminId);
        setInitialPassword(userInfoId);
        setInitialPassword(impersonatedId);

        _containerHelper.createProject(getProjectName(), null);

        new ApiPermissionsHelper(this).setSiteRoleUserPermissions(ADMIN_USER, SITE_ADMIN_ROLE);
        // Use created user to ensure we have a known 'Modified by' column for created users
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this,
                () -> new Connection(WebTestHelper.getBaseURL(), ADMIN_USER, PasswordUtil.getPassword()));

        apiPermissionsHelper.createPermissionsGroup(TEST_GROUP, USER_INFO_VIEWER, IMPERSONATED_USER, CHECKED_USER);
        apiPermissionsHelper.setPermissions(TEST_GROUP, READER_ROLE);
        apiPermissionsHelper.setSiteRoleUserPermissions(USER_INFO_VIEWER, "See User and Group Details");

        // A project administrator (no site-wide User Management permission) is used to verify that user details
        // render from the Project Users page.
        _userHelper.createUser(PROJECT_ADMIN, true, true);
        new ApiPermissionsHelper("/" + getProjectName()).setUserPermissions(PROJECT_ADMIN, PROJECT_ADMIN_ROLE);

        // A user with no role in the project (no read access) that should never appear on the Project Users page.
        _userHelper.createUser(NON_MEMBER, true, true);

        impersonate(ADMIN_USER);
        {
            UpdateUserDetailsPage page = goToMyAccount().clickEdit();
            page.setField("Phone", HIDDEN_STRING);
            page.setField(CUSTOM_USER_COLUMN, HIDDEN_STRING);
            page.clickSubmit();
        }
        stopImpersonating();
        impersonate(CHECKED_USER);
        {
            UpdateUserDetailsPage page = goToMyAccount().clickEdit();
            page.setField("Phone", HIDDEN_STRING);
            page.setField(CUSTOM_USER_COLUMN, HIDDEN_STRING);
            page.clickSubmit();
        }
        stopImpersonating();
    }

    @Test
    public void testUserVisibilityViaLookup()
    {
        final String displayName = _userHelper.getDisplayNameForEmail(CHECKED_USER);

        createHiddenEmailList();

        impersonate(IMPERSONATED_USER);
        goToProjectHome();

        log("Verify that emails cannot be seen in list via lookup");
        clickAndWait(Locator.linkWithText(EMAIL_TEST_LIST));
        new DataRegionFinder(getDriver()).find().goToView(HIDDEN_COL_VIEW);
        assertTextPresent(displayName);
        // This user does not have permission to see user details, so no link
        assertElementNotPresent(Locator.linkWithText(displayName));
        assertTextNotPresent(CHECKED_USER, ADMIN_USER, HIDDEN_STRING);

        stopImpersonating();
        impersonate(USER_INFO_VIEWER);
        goToProjectHome();

        log("Verify that user table info can be seen with permission");
        clickAndWait(Locator.linkWithText(EMAIL_TEST_LIST));
        new DataRegionFinder(getDriver()).find().goToView(HIDDEN_COL_VIEW);
        assertTextPresent(CHECKED_USER, ADMIN_USER, HIDDEN_STRING);
    }

    @Test
    public void testUserVisibilityViaQuery()
    {
        final String displayName = _userHelper.getDisplayNameForEmail(CHECKED_USER);
        createUsersTableView();

        impersonate(IMPERSONATED_USER);
        ExecuteQueryPage.beginAt(this, "core", "Users");

        log("Verify that emails cannot be seen in query webpart");
        new DataRegionFinder(getDriver()).find().goToView(HIDDEN_COL_VIEW);
        assertElementPresent(Locator.linkWithText(displayName));
        assertTextNotPresent(CHECKED_USER, ADMIN_USER, HIDDEN_STRING);

        stopImpersonating();
        impersonate(USER_INFO_VIEWER);
        ExecuteQueryPage.beginAt(this, "core", "Users");

        log("Verify that user table info can be seen with permission");
        new DataRegionFinder(getDriver()).find().goToView(HIDDEN_COL_VIEW);
        assertTextPresent(CHECKED_USER, ADMIN_USER, HIDDEN_STRING);
    }

    @Test
    public void testUserVisibilityViaContactsWebPart()
    {
        final String displayName = _userHelper.getDisplayNameForEmail(CHECKED_USER);

        goToProjectHome();

        new PortalHelper(this).addBodyWebPart("Contacts");

        impersonate(IMPERSONATED_USER);

        log("Verify that user information cannot be seen in contacts webpart");
        assertElementPresent(Locator.linkWithText(displayName));
        assertTextNotPresent(CHECKED_USER, ADMIN_USER, HIDDEN_STRING, TEST_GROUP);

        stopImpersonating();
        impersonate(USER_INFO_VIEWER);
        goToProjectHome();

        log("Verify that user table info can be seen with permission");
        assertTextPresent(CHECKED_USER, ADMIN_USER, HIDDEN_STRING, TEST_GROUP);
    }

    @Test
    public void testProjectUsers() throws Exception
    {
        // The list of project users will be all users with read access to the folder.
        Set<String> projectUsers = getUsersWithAccess(getProjectName(), ReadPermission.class);
        assertFalse("A user with no project role must not have read access",
                projectUsers.contains(NON_MEMBER));

        // Both a site administrator and a project administrator should see the project's users and be able to open
        // the details view for every one of them.
        for (String admin : List.of(ADMIN_USER, PROJECT_ADMIN))
        {
            impersonate(admin);
            goToProjectHome();

            log("Verify the Project Users page, impersonating " + admin);
            ShowUsersPage projectUsersPage = goToProjectUsers();
            assertEquals("Project Users page displaying the wrong set of users.",
                    projectUsers, new HashSet<>(projectUsersPage.getUsersTable().getColumnDataAsText("Email")));

            log("Verify " + admin + " can view the details page of every project user");
            for (String userEmail : projectUsers)
            {
                DataRegionTable usersTable = goToProjectUsers().getUsersTable();
                clickAndWait(usersTable.detailsLink(usersTable.getRowIndexStrict("Email", userEmail)));
                assertTextPresent(userEmail, _userHelper.getDisplayNameForEmail(userEmail));
            }

            stopImpersonating();
        }
    }

    /**
     * Returns the set of user emails who have permission to the folder
     */
    private Set<String> getUsersWithAccess(String folderPath, Class<? extends Permission> perm) throws IOException, CommandException
    {
        SimpleGetCommand command = new SimpleGetCommand("user", "getUsersWithPermissions");
        command.setParameters(Map.of("permissions", perm.getName()));
        CommandResponse response = command.execute(createDefaultConnection(), folderPath);
        List<Map<String, Object>> users = response.getProperty("users");

        return users.stream().
                map(m -> (String) m.get("email")).collect(Collectors.toSet());
    }

    @Test
    public void testUserVisibilityAutoCompleteApi() throws Exception
    {
        List<Map<String, String>> adminResponse = getAutoCompleteResponse(ADMIN_USER, getProjectName());
        assertThat("Sanity check failed for auto-complete API as admin",
                adminResponse.toString(), containsString(CHECKED_USER));
        List<Map<String, String>> viewerResponse = getAutoCompleteResponse(USER_INFO_VIEWER, getProjectName());
        assertThat("Auto-complete API should return email adresses for user with permission",
                viewerResponse.toString(), containsString(CHECKED_USER));
        List<Map<String, String>> maskedResponse = getAutoCompleteResponse(IMPERSONATED_USER, getProjectName());
        assertThat("Auto-complete API should not return email address without permission",
                maskedResponse.toString(), allOf(
                        not(containsString(CHECKED_USER)),
                        containsString(_userHelper.getDisplayNameForEmail(CHECKED_USER))));
    }

    private List<Map<String, String>> getAutoCompleteResponse(String user, String containerPath) throws IOException
    {
        Connection connection = new Connection(WebTestHelper.getBaseURL(), user, PasswordUtil.getPassword());
        SimpleGetCommand command = new SimpleGetCommand("security", "CompleteUserRead");

        try
        {
            return command.execute(connection, containerPath).getProperty("completions");
        }
        catch (CommandException e)
        {
            throw new RuntimeException(e.getResponseText());
        }
    }

    @LogMethod
    private void createHiddenEmailList()
    {
        // Create list
        impersonate(ADMIN_USER);
        FieldDefinition userColumn = new FieldDefinition("user",
                new FieldDefinition.IntLookup(getProjectName(), "core", "Users"));

        _listHelper.createList(getProjectName(), EMAIL_TEST_LIST, "Key", userColumn);
        goToManageLists();
        clickAndWait(Locator.linkWithText(EMAIL_TEST_LIST));
        new DataRegionFinder(getDriver()).find().clickInsertNewRow();
        selectOptionByText(Locator.name("quf_user"), _userHelper.getDisplayNameForEmail(CHECKED_USER));
        clickButton("Submit");
        new DataRegionFinder(getDriver()).find().clickInsertNewRow();
        selectOptionByText(Locator.name("quf_user"), _userHelper.getDisplayNameForEmail(ADMIN_USER));
        clickButton("Submit");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("user/Phone");
        _customizeViewsHelper.addColumn("user/" + CUSTOM_USER_COLUMN);
        _customizeViewsHelper.addColumn("user/Email");
        _customizeViewsHelper.addColumn("user/ModifiedBy/Email");
        _customizeViewsHelper.addColumn("user/ModifiedBy/ModifiedBy/Email");
        _customizeViewsHelper.addColumn("ModifiedBy/Email");
        _customizeViewsHelper.saveCustomView(HIDDEN_COL_VIEW, true);

        assertTextPresent(CHECKED_USER, ADMIN_USER, HIDDEN_STRING); // Ensure subsequent check is valid
        stopImpersonating();
    }

    @LogMethod
    private void createUsersTableView()
    {
        ExecuteQueryPage.beginAt(this, "core", "Users");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("ModifiedBy/Email");
        _customizeViewsHelper.saveCustomView(HIDDEN_COL_VIEW, true);

        assertTextPresent(CHECKED_USER, ADMIN_USER, HIDDEN_STRING); // Ensure subsequent check is valid
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
