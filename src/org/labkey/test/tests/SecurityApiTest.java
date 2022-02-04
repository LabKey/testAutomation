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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.APITestHelper;
import org.labkey.test.util.ApiPermissionsHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
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
        return new File[]{TestFileUtils.getSampleData("api/security-api.xml")};
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
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.createPermissionsGroup(GROUP_1, USER_1);
        apiPermissionsHelper.createPermissionsGroup(GROUP_2, USER_1, USER_2);
        apiPermissionsHelper.setPermissions(GROUP_1, "Editor");
        apiPermissionsHelper.setPermissions(GROUP_2, "Reader");

        // Create the admin user that will be used to call the APIs.
        _userHelper.createUserAndNotify(ADMIN_USER);
        setInitialPassword(ADMIN_USER, ADMIN_USER_PWD);
        apiPermissionsHelper.addUserToSiteGroup(ADMIN_USER, "Site Administrators");

    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(PROJECT_NAME, afterTest);
        _userHelper.deleteUsers(false, USER_1, USER_2, ADMIN_USER, USER_CREATED_BY_API);
    }

    protected Pattern[] getIgnoredElements()
    {
        return new Pattern[0];
    }

    /**
     * A Note About This Test
     *
     * This is "one of those tests" that are seldom change and very rarely fail. However, when it does fail you can spend
     * more time than you want just trying to get it to run. Hopefully the following comments can help you run the test.
     *
     * If you cannot get this test to run in IntelliJ try running it from the command line. Running in IntelliJ can throw
     * a ClassNotFoundException. You are welcome to try and track it down and correct it if you want, but running from the
     * command line (gradlew uiTests) does appear to be more reliable.
     *
     * The failed output from the test can be difficult to read. Especially if the failure was between expected and actual
     * roles and permissions. If that is the type of failure you are seeing the failure message is a rough diff between
     * the expected JSON and the JSON that was returned. If you run the test locally from the command line you might be
     * able to better understand what the differences are. After running locally you should see a message at the end of
     * the run output that looks like this:
     *
     * There were failing tests. See the report at: file:///Users/janedoe/labkey/trunk/build/modules/testAutomation/test/logs/reports/html/index.html
     *
     * Open this file in a browser, click on the test name, then click on the "Standard output" button. This presents the
     * output in a more readable way (basically a log). Near the top is an "Expected:" comment with the expected output.
     * A little ways down is a comment "Actual:" with the actual output (go figure). It might be easier to compare the
     * expected vs. actual results as it appears in this log.
     *
     * The expected output comes from the security-api.xml file. If this is a role and/or permission error odds are you
     * are going to have to change that file in some way. Try to make the smallest change possible. If you are changing
     * permissions, either adding or removing, you will need to update the value(s) in the effectivePermissions collection
     * for a given role.
     *
     * If you still have a hard time getting the test to pass locally try running with a bootstrapped database. That is
     * how the test is run on TeamCity and it may address any assumptions the test makes about the environment.
     *
     * Vaya con Dios
     */
    @Test
    public void testApiUserRolesAndPermissions() throws Exception
    {
        APITestHelper apiTester = new APITestHelper(this);
        apiTester.setTestFiles(getTestFiles());
        apiTester.setIgnoredElements(getIgnoredElements());
        apiTester.runApiTests(ADMIN_USER, ADMIN_USER_PWD);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("query");
    }

}
