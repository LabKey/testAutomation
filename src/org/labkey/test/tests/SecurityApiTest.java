/*
 * Copyright (c) 2009-2015 LabKey Corporation
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
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.APITestHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

// This test was moved to "InDevelopment" 2013-01-07.
@Category({InDevelopment.class})
public class SecurityApiTest extends BaseWebDriverTest
{
    protected static final String PROJECT_NAME = "Security API Test Project";
    private static final String USER_1 = "testuser1@securityapi.test";
    private static final String USER_1_PWD = "Password";
    private static final String USER_2 = "testuser2@securityapi.test";
    private static final String GROUP_1 = "testgroup1";
    private static final String GROUP_2 = "testgroup2";

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
        createUser(USER_1, null);
        createUser(USER_2, null);

        _containerHelper.createProject(PROJECT_NAME, null);
        _permissionsHelper.createPermissionsGroup(GROUP_1, USER_1);
        _permissionsHelper.createPermissionsGroup(GROUP_2, USER_1, USER_2);
        _permissionsHelper.setPermissions(GROUP_1, "Editor");
        _permissionsHelper.setPermissions(GROUP_2, "Reader");
        _permissionsHelper.exitPermissionsUI();

        // Set the password for the first test users. The API test will run with this users credentials.
        adminPasswordResetTest(USER_1, USER_1_PWD);

    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(PROJECT_NAME, afterTest);
        deleteUsersIfPresent(USER_1, USER_2);
    }

    private void adminPasswordResetTest(String username, String password)
    {
        goToSiteUsers();
        clickAndWait(Locator.linkContainingText(displayNameFromEmail(username)));
        prepForPageLoad();
        clickButtonContainingText("Reset Password", 0);
        acceptAlert();
        waitForPageToLoad();
        clickButton("Done");

        String url = getPasswordResetUrl(username);


        resetPassword(url, username, password);

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
        apiTester.runApiTests(USER_1, USER_1_PWD);
    }

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("query");
    }

}
