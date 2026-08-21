/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PermissionsHelper;

import java.util.Arrays;
import java.util.List;

import static org.labkey.test.util.PermissionsHelper.APP_ADMIN_ROLE;
import static org.labkey.test.util.PermissionsHelper.TROUBLESHOOTER_ROLE;

public abstract class AbstractAdminConsoleTest extends BaseWebDriverTest
{
    protected static final String APP_ADMIN_USER = "app_admin_test_user@adminconsole.test";
    protected static final String TROUBLESHOOTER_USER = "troubleshooter@adminconsole.test";

    @Override
    public String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("admin");
    }

    @Override
    public void checkQueries()
    {

    }

    @Override
    public void checkViews()
    {

    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        AbstractAdminConsoleTest initTest = getCurrentTest();
        initTest.createTestUsers();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(false, APP_ADMIN_USER);
        _userHelper.deleteUsers(false, TROUBLESHOOTER_USER);
    }

    protected void createTestUsers()
    {
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);

        int appAdminId = _userHelper.createUser(APP_ADMIN_USER, true, false).getUserId();
        setInitialPassword(appAdminId);
        apiPermissionsHelper.addMemberToRole(APP_ADMIN_USER, APP_ADMIN_ROLE, PermissionsHelper.MemberType.user, "/");

        int troubleshooterId = _userHelper.createUser(TROUBLESHOOTER_USER, true, false).getUserId();
        setInitialPassword(troubleshooterId);
        apiPermissionsHelper.addMemberToRole(TROUBLESHOOTER_USER, TROUBLESHOOTER_ROLE, PermissionsHelper.MemberType.user, "/");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
