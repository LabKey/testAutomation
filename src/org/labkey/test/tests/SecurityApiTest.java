/*
 * Copyright (c) 2009-2017 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.APITestHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Category({DailyB.class})
public class SecurityApiTest extends BaseWebDriverTest
{
    protected static final String PROJECT_NAME = "Security API Test Project";
    private static final String USER_1 = "testuser1@securityapi.test";
    private static final String USER_2 = "testuser2@securityapi.test";
    private static final String GROUP_1 = "testgroup1";
    private static final String GROUP_2 = "testgroup2";
    private static final String ADMIN_USER = "security-api@clientapi.test";
    private static final String ADMIN_USER_PWD = "Pa$$w0rd";
    private static final String USER_CREATED_BY_API = "api-created-user@securityapi.test"; // This email value is found in the security-api.xml file for the "create new user" test.

    protected File[] getTestFiles()
    {
        return new File[]{new File(TestFileUtils.getLabKeyRoot() + "/server/test/data/api/security-api.xml")};
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @BeforeClass
    public static void initTest() throws Exception
    {
        SecurityApiTest init = (SecurityApiTest)getCurrentTest();
        init.createUsers();
    }

    public void createUsers(){

        //setup the project, users and groups
        _userHelper.createUser(USER_1);
        _userHelper.createUser(USER_2);

        _containerHelper.createProject(PROJECT_NAME, null);
        _permissionsHelper.createPermissionsGroup(GROUP_1, USER_1);
        _permissionsHelper.createPermissionsGroup(GROUP_2, USER_1, USER_2);
        _permissionsHelper.setPermissions(GROUP_1, "Editor");
        _permissionsHelper.setPermissions(GROUP_2, "Reader");
        _permissionsHelper.exitPermissionsUI();

        // Create the admin user that will be used to call the APIs.
        _userHelper.createUserAndNotify(ADMIN_USER);
        setInitialPassword(ADMIN_USER, ADMIN_USER_PWD);
        _permissionsHelper.addUserToSiteGroup(ADMIN_USER, "Site Administrators");

    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(PROJECT_NAME, afterTest);
        deleteUsersIfPresent(USER_1, USER_2, ADMIN_USER, USER_CREATED_BY_API);
    }

    protected Pattern[] getIgnoredElements()
    {
        return new Pattern[0];
    }

    @Test
    public void testApiUserRolesAndPermissions() throws Exception
    {
        APITestHelper apiTester = new APITestHelper(this);
        apiTester.setTestFiles(getTestFiles());
        apiTester.setIgnoredElements(getIgnoredElements());
        apiTester.runApiTests(ADMIN_USER, ADMIN_USER_PWD);
    }

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("query");
    }

}
