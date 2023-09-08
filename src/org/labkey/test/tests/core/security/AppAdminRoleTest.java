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
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PermissionsHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@Category({Daily.class})
public class AppAdminRoleTest extends BaseWebDriverTest
{
    private static final String APP_ADMIN = "appadmin@appadmin.test";
    private static final String USER = "user@appadmin.test";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(false, APP_ADMIN, USER);
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

        new ApiPermissionsHelper(this).addUserAsAppAdmin(APP_ADMIN);
    }

    @Test
    public void testAppAdminAssignSiteAdmin()
    {
        CommandException apiException = getApiException(() -> permissionsApiAsAppAdmin().addMemberToRole(USER, "Site Admin", PermissionsHelper.MemberType.user, "/"));
        if (apiException == null)
            fail("App Admin was able to assign Site Admin role");

        assertEquals("Wrong error", "You do not have permission to modify the Site Admin role or permission.", apiException.getMessage());
    }

    @Test
    public void testAppAdminAssignPlatformDeveloper()
    {
        CommandException apiException = getApiException(() -> permissionsApiAsAppAdmin().addMemberToRole(USER, "Platform Developer", PermissionsHelper.MemberType.user, "/"));
        if (apiException == null)
            fail("App Admin was able to assign Platform Developer role");

        assertEquals("Wrong error", "You do not have permission to modify the Platform Developer role or permission.", apiException.getMessage());
    }

    @Test
    public void testAppAdminAssignReader()
    {
        permissionsApiAsAppAdmin().addMemberToRole(USER, "Reader", PermissionsHelper.MemberType.user, "home");
    }

    @Test
    public void testAppAdminCanCreateAndDeleteFolder()
    {
        goToHome();
        impersonate(APP_ADMIN);
        _containerHelper.createProject("AppAdminTestProject", "Collaboration");
        _containerHelper.deleteProject("AppAdminTestProject");
        assertEquals("Container AppAdminTestProject not deleted.", false, _containerHelper.doesContainerExist("AppAdminTestProject"));
        goToHome();
        stopImpersonating();
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
            if (ex.getCause() != null && ex.getCause() instanceof CommandException)
                return (CommandException) ex.getCause();
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
