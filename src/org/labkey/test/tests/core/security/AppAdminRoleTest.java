/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.tests.core.security;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.PermissionsEditor;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PermissionsHelper.MemberType;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.labkey.test.util.PermissionsHelper.APP_ADMIN_ROLE;
import static org.labkey.test.util.PermissionsHelper.DEVELOPER_ROLE;
import static org.labkey.test.util.PermissionsHelper.IMP_TROUBLESHOOTER_ROLE;
import static org.labkey.test.util.PermissionsHelper.SITE_ADMIN_ROLE;

@Category({Daily.class})
public class AppAdminRoleTest extends BaseWebDriverTest
{
    private static final String APP_ADMIN = "appadmin@appadmin.test";
    private static final String USER = "user@appadmin.test";
    private static final String ADMIN_GROUP = "Custom Admin Group";
    private static final String DEV_GROUP = "Custom Developer Group";
    private static final String IT_GROUP = "Custom IT Group";
    private static final String SITE_GROUP = "Custom Site Group";
    private static final String APP_ADMIN_TEST_PROJECT = "AppAdminTestProject";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(APP_ADMIN_TEST_PROJECT, false);
        _userHelper.deleteUsers(false, APP_ADMIN, USER);
        deleteSiteGroups(new ApiPermissionsHelper(this));
    }

    @BeforeClass
    public static void setupProject()
    {
        AppAdminRoleTest init = (AppAdminRoleTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _userHelper.createUser(USER);
        _userHelper.createUserAndNotify(APP_ADMIN, true);
        setInitialPassword(APP_ADMIN);

        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.addUserAsAppAdmin(APP_ADMIN);
        createSiteGroups(apiPermissionsHelper);
    }

    private void deleteSiteGroups(ApiPermissionsHelper apiPermissionsHelper)
    {
        apiPermissionsHelper.deleteGroup(ADMIN_GROUP, "/", false);
        apiPermissionsHelper.deleteGroup(SITE_GROUP, "/", false);
        apiPermissionsHelper.deleteGroup(DEV_GROUP, "/", false);
        apiPermissionsHelper.deleteGroup(IT_GROUP, "/", false);
    }

    @LogMethod
    private void createSiteGroups(ApiPermissionsHelper apiPermissionsHelper)
    {
        deleteSiteGroups(apiPermissionsHelper);
        apiPermissionsHelper.createGlobalPermissionsGroup(SITE_GROUP);
        apiPermissionsHelper.createGlobalPermissionsGroup(ADMIN_GROUP);
        apiPermissionsHelper.addMemberToRole(ADMIN_GROUP, SITE_ADMIN_ROLE, MemberType.group, "/");
        apiPermissionsHelper.createGlobalPermissionsGroup(DEV_GROUP);
        apiPermissionsHelper.addMemberToRole(DEV_GROUP, DEVELOPER_ROLE, MemberType.group, "/");
        apiPermissionsHelper.createGlobalPermissionsGroup(IT_GROUP);
        apiPermissionsHelper.addMemberToRole(IT_GROUP, IMP_TROUBLESHOOTER_ROLE, MemberType.group, "/");
    }

    @Test
    public void testAppAdminAssignSiteAdmin()
    {
        CommandException apiException = getApiException(() -> permissionsApiAsAppAdmin().addMemberToRole(USER, "Site Admin", MemberType.user, "/"));
        if (apiException == null)
            fail("App Admin was able to assign Site Admin role");

        assertEquals("Wrong error", "You do not have permission to modify the Site Administrator role.", apiException.getMessage());
    }

    @Test
    public void testAssignGroupSiteAdmin()
    {
        CommandException apiException = getApiException(() -> permissionsApiAsAppAdmin().addMemberToRole(SITE_GROUP, "Site Admin", MemberType.group, "/"));
        if (apiException == null)
            fail("App Admin was able to assign group to Site Admin role");

        assertEquals("Wrong error", "You do not have permission to modify the Site Administrator role.", apiException.getMessage());
    }

    @Test
    public void testAppAdminAssignPlatformDeveloper()
    {
        CommandException apiException = getApiException(() -> permissionsApiAsAppAdmin().addMemberToRole(USER, "Platform Developer", MemberType.user, "/"));
        if (apiException == null)
            fail("App Admin was able to assign Platform Developer role");

        assertEquals("Wrong error", "You do not have permission to modify the Platform Developer role.", apiException.getMessage());
    }

    @Test
    public void testAssignGroupPlatformDeveloper()
    {
        CommandException apiException = getApiException(() -> permissionsApiAsAppAdmin().addMemberToRole(SITE_GROUP, "Platform Developer", MemberType.group, "/"));
        if (apiException == null)
            fail("App Admin was able to assign group to Platform Developer role");

        assertEquals("Wrong error", "You do not have permission to modify the Platform Developer role.", apiException.getMessage());
    }

    @Test
    public void testAppAdminAssignReader()
    {
        permissionsApiAsAppAdmin().addMemberToRole(USER, "Reader", MemberType.user, "home");
    }

    @Test
    public void testAppAdminCanCreateAndDeleteFolder()
    {
        impersonate(APP_ADMIN);
        _containerHelper.createProject(APP_ADMIN_TEST_PROJECT, "Collaboration");
        _containerHelper.deleteProject(APP_ADMIN_TEST_PROJECT);
        assertFalse("Container AppAdminTestProject not deleted.", _containerHelper.doesContainerExist(APP_ADMIN_TEST_PROJECT));
    }

    @Test
    public void testModifyPrivilegedGroup()
    {
        CommandException apiException = getApiException(() -> permissionsApiAsAppAdmin().addUserToSiteGroup(USER, ADMIN_GROUP));
        if (apiException == null)
            fail("App Admin was able to modify privileged group");

        assertEquals("Wrong error", "Can not update members of a group assigned a privileged role: " + ADMIN_GROUP, apiException.getMessage());
    }

    @Test
    public void testPermissionsUi()
    {
        impersonate(APP_ADMIN);
        PermissionsEditor permissionsEditor;

        log("Test adding roles");
        permissionsEditor = PermissionsEditor.beginAt(this, "/");
        for (String role : List.of(SITE_ADMIN_ROLE, DEVELOPER_ROLE, IMP_TROUBLESHOOTER_ROLE))
        {
            checker().verifyFalse("App admin user shouldn't be able to modify " + role + " role", permissionsEditor.isRoleEditable(role));
        }

        checker().verifyTrue("App admin user should be able to modify " + APP_ADMIN_ROLE + " role", permissionsEditor.isRoleEditable(APP_ADMIN_ROLE));
    }

    private ApiPermissionsHelper permissionsApiAsAppAdmin()
    {
        return new ApiPermissionsHelper(this, () -> new Connection(WebTestHelper.getBaseURL(), APP_ADMIN, PasswordUtil.getPassword()));
    }

    private CommandException getApiException(Runnable runnable)
    {
        try
        {
            runnable.run();
            return null;
        }
        catch (RuntimeException ex)
        {
            if (ex.getCause() instanceof CommandException ce)
                return ce;
            throw ex;
        }
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "AppAdminRoleTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
