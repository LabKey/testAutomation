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
     * This is "one of those tests" that are seldom change and very rarely fail. However, when they do you spend more time
     * than you want trying to just get it to run. Hopefully the following comments can help you if you are one of those
     * poor souls who now finds themselves in this position.
     *
     * If you cannot get this test to run in IntelliJ try running it from the command line. Running in IntelliJ can throw
     * a ClassNotFoundException. You are welcome to try and track it down and correct it if you want, but running from the
     * command line (gradlew uiTests) does appear to be more reliable.
     *
     * The failed output from the test, especially on TeamCity, can be difficult to read. It is kind of a diff but not
     * really. However, fear not! When you run the test locally from the command line you should see a line at the end of
     * the output that looks like this:
     *
     * There were failing tests. See the report at: file:///Users/janedoe/labkey/trunk/build/modules/testAutomation/test/logs/reports/html/index.html
     *
     * Open this file in a browser, click on the test name, then click on the "Standard output" button. This presents the
     * output in a more readable way (basically a log). Near the top is an "Expected:" comment with the expected output.
     * A little ways down is a comment "Actual:" with the actual output (go figure).
     * Looking at the expected vs. actual output in this log might be easier to compare and see what the difference is.
     *
     * The expected output comes from the security-api.xml file. Odds are you are going to have to change that file in
     * some way. You didn't hear it from me but the easiest fix could be to replace the contents of the security-api.xml
     * file with the actual output from the log. Obviously the smaller the change the better, but as a last resort you
     * could try that as a fix.
     *
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
