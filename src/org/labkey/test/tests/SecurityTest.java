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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.reader.Readers;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.BVT;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.UIUserHelper;
import org.openqa.selenium.WebElement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.WebTestHelper.getHttpResponse;

@Category(BVT.class)
public class SecurityTest extends BaseWebDriverTest
{
    protected static final String PROJECT_NAME = "SecurityVerifyProject";
    protected static final String ADMIN_USER_TEMPLATE = "_admin.template@security.test";
    protected static final String NORMAL_USER_TEMPLATE = "_user.template@security.test";
    protected static final String BOGUS_USER_TEMPLATE = "bogus@bogus@bogus";
    protected static final String PROJECT_ADMIN_USER = "admin_securitytest@security.test";
    protected static final String NORMAL_USER = "user_securitytest@security.test";
    protected static final String[] PASSWORDS= {"0asdfgh!", "1asdfgh!", "2asdfgh!", "3asdfgh!", "4asdfgh!", "5asdfgh!", "6asdfgh!", "7asdfgh!", "8asdfgh!", "9asdfgh!", "10asdfgh!"};
    protected static final String NORMAL_USER_PASSWORD = PASSWORDS[0];
    protected static final String TO_BE_DELETED_USER = "delete_me@security.test";
    protected static final String SITE_ADMIN_USER = "siteadmin_securitytest@security.test";

    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.singletonList("core");
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

    protected boolean isQuickTest()
    {
        return false;
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);

        deleteUsersIfPresent(ADMIN_USER_TEMPLATE, NORMAL_USER_TEMPLATE, PROJECT_ADMIN_USER, NORMAL_USER, SITE_ADMIN_USER, TO_BE_DELETED_USER);
    }

    @Test
    public void testSteps()
    {
        enableEmailRecorder();

        clonePermissionsTest();
        displayNameTest();
        tokenAuthenticationTest();
        if (!isQuickTest())
        {
            impersonationTest();
            guestTest();
            addRemoveSiteAdminTest();
        }

        log("Check welcome emails [6 new users]");
        goToModule("Dumbster");

        // This points to a "faked up" Data Region -- cannot use DataRegionTable
        WebElement table = DataRegionTable.Locators.dataRegion("EmailRecord").findElement(this.getDriver());
        assertEquals("Expected 12 notification emails (+3 rows).", 15, getTableRowCount(table.getAttribute("id")));
        // Once in the message itself, plus copies in the headers
        assertTextPresent(": Welcome", 18);

        if (!isQuickTest())
        {
            cantReachAdminToolFromUserAccount(false);
            passwordStrengthTest();
            dumbsterTest();
            loginSelfRegistrationEnabledTest();
            loginSelfRegistrationDisabledTest();
        }
        passwordResetTest();
    }

    protected static final String HISTORY_TAB_TITLE = "History:";
    protected static final String[] unreachableUrlsShortList = {"/admin/showAdmin.view?",
            "/user/showUsers.view?",
            "/security/group.view?group=Administrators"};

    protected static final String[] unreachableUrlsExtendedList = {"/security/group.view?group=Developers",
        "user/showUsers.view?","security/project.view?returnUrl=%2Fuser%2FshowUsers.view%3F","admin/createFolder.view?",
        "/analytics/begin.view?","/login/configure.view?", "/admin/customizeEmail.view?",
        "/admin/filesSiteSettings.view?", "/admin/projectSettings.view?",
        "/flow/flowAdmin.view?","/admin/folderManagement.view?",
        "/admin/reorderFolders.view?", "/admin/customizeSite.view?",
        "/reports/configureReportsAndScripts.view?", "/audit/showAuditLog.view?",
        "/search/admin.view?", "/ms1/showAdmin.view?", "/ms2/showMS2Admin.view?",
        "/experiment-types/begin.view?", "/pipeline-status/showList.view?", "/pipeline/setup.view?",
        "/ms2/showProteinAdmin.view?", "/admin/actions.view?", "/admin/caches.view?",
        "/admin/dbChecker.view?",
        "/query/dataSourceAdmin.view?",
        "/admin/dumpHeap.view?", "/admin/environmentVariables.view?", "/admin/memTracker.view?",
        "/admin/queries.view?", "/admin/resetErrorMark.view?", "/admin/showThreads.view?",
        "/admin-sql/scripts.view?", "/admin/systemProperties.view?", "/admin/emailTest.view?",
        "/admin/showAllErrors.view?","/admin/showErrorsSinceMark.view?",  "/admin/showPrimaryLog.view?"};

    /**
     * verify that a normal user does not get a link to a the admin console or see their own history, nor
     * reach an admin-only url directly.
     *
     *
     * @param longForm if and only if longFrom = true, test against the extended list of admin urls.
     * if longForm = false, test against only a few high priority URLs.
     */
    @LogMethod protected void cantReachAdminToolFromUserAccount(boolean longForm)
    {
        //just in case, create user
//        createUserAndNotify(NORMAL_USER, null);

        //verify that you can see the text "history" in the appropriate area as admin.  If this fails, the
        //check below is worthless

        goToMyAccount();
        assertTextPresent(HISTORY_TAB_TITLE);

        //log in as normal user
        impersonate(NORMAL_USER);

        //admin site link not available
        assertElementNotPresent(Locator.id("adminMenuPopupText"));

        //can't reach admin urls directly either

        for(String url : unreachableUrlsShortList)
        {
                assertUrlUnreachableDueToPermissions(url);
        }

        if(longForm)
        {

            for(String url : unreachableUrlsExtendedList)
            {
                assertUrlUnreachableDueToPermissions(url);
            }
        }

        ///user/showUsers.view?

        //shouldn't be able to view own history either
        clickButton("Home");
        goToMyAccount();
        assertTextNotPresent(HISTORY_TAB_TITLE);

        stopImpersonating();
    }

    @LogMethod public void assertUrlUnreachableDueToPermissions(String url)
    {
        log("Attempting to reach URL user does not have permission for:  " + url);
        beginAt(url);
        assertAtUserUserLacksPermissionPage();
    }

    /**
     *
     * preconditions:  NORMAL_USER exists with password NORMAL_USER_PASSWORD.  Currently logged in as admin
     * post conditions
     */
    @LogMethod public void passwordResetTest()
    {
        //get user a password
        String username = NORMAL_USER;
        String password = NORMAL_USER_PASSWORD;

        password = adminPasswordResetTest(username, password+"adminReset");
        
        String resetUrl = userForgotPasswordWorkflowTest(username, password);
        
        userPasswordResetTest(resetUrl);

        ensureSignedInAsPrimaryTestUser();
    }

    @LogMethod private void dumbsterTest()
    {
        assertNoDumbsterPermission(PROJECT_ADMIN_USER);
        assertNoDumbsterPermission(NORMAL_USER);
    }

    @LogMethod private void assertNoDumbsterPermission(@LoggedParam String user)
    {
        clickProject(PROJECT_NAME);
        goToModule("Dumbster");
        pushLocation();
        impersonate(user);
        popLocation();
        assertTextPresent("You must be a site administrator to view the email record");
        stopImpersonating();
    }

    protected enum PasswordAlterType {RESET_PASSWORD, CHANGE_PASSWORD}

    /**
     * Precondtions:  able to reset user's password at resetUrl, db in weak-password mode
     */
    @LogMethod protected void userPasswordResetTest(String resetUrl)
    {
        ensureSignedOut();

        beginAt(resetUrl);

        String[][] passwords = {{"fooba", null}, {"foobar","foobar2"}};
        String[][] messages = {{"Your password must be six non-whitespace characters or more."}, {"Your password entries didn't match."}};
        attemptSetInvalidPasswords(PasswordAlterType.RESET_PASSWORD, passwords, messages);

        resetPassword(resetUrl, NORMAL_USER, NORMAL_USER_PASSWORD);
    }

//    protected static final int RESET_PASSWORD = 1;
//    protected static final int CHANGE_PASSWORD = 2;

    /**RESET_PASSWORD means user or admin initiated password change that
     * involves visiting a webpage specified in an e-mail to change the password,
     * without knowing the past password.
     * CHANGE_PASSWORD means a user went into their account information and initiated the change by
     * selecting "change password".  This requires the old password to work.
      */

    @LogMethod protected void attemptSetInvalidPasswords(PasswordAlterType changeType, String[][] passwords, String[][] errors)
    {
        //if reset, should already be at reset Url
        for (int i = 0; i < errors.length; i++)
        {
            switch (changeType)
            {
                case RESET_PASSWORD:
                    attemptSetInvalidPassword(changeType, passwords[i], errors[i]);
            }
        }
    }

    @LogMethod protected void attemptSetInvalidPassword(PasswordAlterType changeType, String[] passwords, String... errors)
    {
        switch (changeType)
        {
            case CHANGE_PASSWORD:
                throw new IllegalArgumentException("unsupported use of change password type");

            case RESET_PASSWORD:
                setFormElement(Locator.id("password"), passwords[0]);
                String password2 = passwords[1];
                if(password2==null) password2 = passwords[0];
                setFormElement(Locator.id("password2"), password2);
                clickButton("Set Password");
                assertTextPresent(errors);
        }
    }

    /**
     * preconditions: there exists user username with password password
     * postcondtions:  user can reset password at return value, not signed in
     *
     * @param username  user's username
     * @param password user's password
     * @return URL to use to reset user password
     */
    //issue 3876
    @LogMethod private String userForgotPasswordWorkflowTest(String username, String password)
    {
        ensureSignedOut();

        String resetUrl = userInitiatePasswordReset(username);

        signOut();

        //attempt sign in with old password- should succeed
        signIn(username, password);
        signOut();

        return resetUrl;
    }

    @LogMethod public String userInitiatePasswordReset(String username)
    {
        goToHome();
        ensureSignedOut();

        clickAndWait(Locator.linkWithText("Sign In"));
        clickAndWait(Locator.linkContainingText("forgot password"));
        setFormElement(Locator.id("EmailInput"), username);
        clickButtonContainingText("Submit", 0);

        signIn();
        return getPasswordResetUrl(username);
    }

    String[] wrongPasswordEntered =
                new String[] {"The e-mail address and password you entered did not match any accounts on file.",
                "Note: Passwords are case sensitive; make sure your Caps Lock is off."};

    /**
     *
     * preconditions: logged in as admin
     * postconditions:  not signed in, username's password is return value
     *
     * @param username username to initiate password rest for
     * @param password user's current password (before test starts)
     * @return user's new password
     */
    @LogMethod private String adminPasswordResetTest(String username, String password)
    {
        String newPassword = password +"1";
        goToSiteUsers();
        clickAndWait(Locator.linkContainingText(displayNameFromEmail(username)));
        doAndWaitForPageToLoad(() ->
        {
            clickButtonContainingText("Reset Password", 0);
            acceptAlert();
        });
        clickButton("Done");

        String url = getPasswordResetUrl(username);


        //make sure user can't log in with current password
        signOut();
        signInShouldFail(username, password, wrongPasswordEntered);

        resetPassword(url, username, newPassword);
        
        signOut();
        
        //attempt to log in with old password (should fail)
        signInShouldFail(username, password, wrongPasswordEntered);
        
        return newPassword;
    }

    @LogMethod protected void addRemoveSiteAdminTest()
    {
        // test for issue 13921
        goToSiteAdmins();
        setFormElement(Locator.name("names"), NORMAL_USER);
        uncheckCheckbox(Locator.checkboxByName("sendEmail"));
        clickButton("Update Group Membership");
        assertTextPresent(NORMAL_USER);
        checkCheckbox(Locator.checkboxByNameAndValue("delete", NORMAL_USER));
        clickButton("Update Group Membership", 0);
        assertAlert("Permanently remove selected users from this group?");
        sleep(1000);
        assertElementNotPresent(Locator.checkboxByNameAndValue("delete", NORMAL_USER));
        goToProjectHome();
    }

    @LogMethod protected void guestTest()
    {
        goToProjectHome();
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setSiteGroupPermissions("All Site Users", "Author");
        _permissionsHelper.setSiteGroupPermissions("Guests", "Reader");
        _permissionsHelper.exitPermissionsUI();

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Messages");
        assertElementPresent(Locator.lkButton("New"));
        pushLocation();
        signOut();
        popLocation();
        clickProject(PROJECT_NAME);
        assertElementNotPresent(Locator.lkButton("New"));
        signIn();
        clickProject(PROJECT_NAME);
        assertElementPresent(Locator.lkButton("New"));
        impersonate(NORMAL_USER);
        clickProject(PROJECT_NAME);
        assertElementPresent(Locator.lkButton("New"));
        stopImpersonating();
    }

    @LogMethod protected void displayNameTest()
    {
        String newDisplayName = "changeDisplayTest";

        setDisplayName(NORMAL_USER, newDisplayName);
        assertTextPresent(newDisplayName);

        String injectDisplayName = "displayNameInjection" + INJECT_CHARS_1;

        setDisplayName(NORMAL_USER, injectDisplayName); // Link crawler will do more thorough check
        assertTextPresent(injectDisplayName);
        assertTextNotPresent(newDisplayName);
    }

    @LogMethod protected void clonePermissionsTest()
    {
        // create admin templates, plus test bogus & duplicate email addresses
        createUserAndNotify(ADMIN_USER_TEMPLATE + '\n' + NORMAL_USER_TEMPLATE + '\n' + NORMAL_USER_TEMPLATE + '\n' + BOGUS_USER_TEMPLATE, null, false);
        assertTextPresent("Failed to create user bogus@bogus@bogus: Invalid email address");
        //nav trail check
        assertElementPresent(Locator.xpath("//div[@class='labkey-crumb-trail']/span[@id='navTrailAncestors']/a[text()='Site Users']"));
        assertTextPresent(NORMAL_USER_TEMPLATE + " was already a registered system user.");//here to see this user's profile and history.");

        // create the project and set permissions
        _containerHelper.createProject(PROJECT_NAME, null);
        _permissionsHelper.createPermissionsGroup("Administrators");
        _permissionsHelper.clickManageGroup("Administrators");
        setFormElement(Locator.name("names"), ADMIN_USER_TEMPLATE);
        clickButton("Update Group Membership");
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setPermissions("Administrators", "Project Administrator");

        _permissionsHelper.createPermissionsGroup("Testers");
        _permissionsHelper.assertPermissionSetting("Testers", "No Permissions");
        _permissionsHelper.setPermissions("Testers", "Editor");
        _permissionsHelper.clickManageGroup("Testers");
        setFormElement(Locator.name("names"), NORMAL_USER_TEMPLATE);
        clickButton("Update Group Membership");

        // create users and verify permissions
        createUserAndNotify(PROJECT_ADMIN_USER, ADMIN_USER_TEMPLATE);
        createUserAndNotify(SITE_ADMIN_USER, PasswordUtil.getUsername());
        createUserAndNotify(NORMAL_USER, NORMAL_USER_TEMPLATE);
        createUserAndNotify(TO_BE_DELETED_USER, NORMAL_USER_TEMPLATE);

        // verify permissions
        checkGroupMembership(PROJECT_ADMIN_USER, "SecurityVerifyProject/Administrators", 2);
        checkGroupMembership(NORMAL_USER, "SecurityVerifyProject/Testers", 1);
        assertNavTrail("Site Users", "User Details", "Permission");
//        assertTextPresent("Site Users >  User Details >  Permissions >  ");
    }

    @LogMethod protected void checkGroupMembership(String userName, String groupName, int expectedCount)
    {
        goToSiteUsers();

        Locator userAccessLink = Locator.xpath("//td[text()='" + userName + "']/..//td/a[contains(@href,'userAccess.view')]");
        boolean isPresent = isElementPresent(userAccessLink);

        // If user is not found but paging indicators are, then show all 
        if (!isPresent && isElementPresent(Locator.linkContainingText("Next")) && isElementPresent(Locator.linkContainingText("Last")))
        {
            clickButton("Page Size", 0);
            clickAndWait(Locator.linkWithText("Show All"));
            isPresent = isElementPresent(userAccessLink);
        }

        if (isPresent)
        {
            clickAndWait(userAccessLink);
            
            // check for the expected number of group membership links (note: they may be hidden by expandos)
            click(Locator.xpath("//tr[td/a[text()='" + getProjectName() + "']]//img" ));
            assertElementPresent(Locator.linkWithText(groupName), expectedCount);
            return;
        }
        fail("Unable to verify group membership of cloned user privileges");
    }

    @LogMethod protected void tokenAuthenticationTest()
    {
        beginAt("/project/SecurityVerifyProject/begin.view?");
        String homePageUrl = removeUrlParameters(getURL().toString());  // Absolute URL for redirect, get rid of '?'
        String relUrl = getCurrentRelativeURL();
        boolean newSchool = relUrl.contains("project-");
        String baseUrl = removeUrlParameters(getCurrentRelativeURL()).replaceAll("/project/", "/login/");
        baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1);
        if (newSchool)
            baseUrl += "login-";
        // Attempt to verify bogus token -- should result in failure
        String xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=ABC");
        assertFailureAuthenticationToken(xml);

        beginAt(baseUrl + "createToken.view?returnUrl=" + homePageUrl);
        // Make sure we redirected to the right place
        assertEquals("Redirected to wrong URL", homePageUrl, removeUrlParameters(getURL().toString()));

        String email = getUrlParam("labkeyEmail", true);
        String emailName;
        String userName = PasswordUtil.getUsername();
        // If we are using IE, then the email will be stripped of its @etc.
        if (!userName.contains("@"))
        {
            emailName = email.substring(0, email.indexOf("@"));
        }
        else
        {
            emailName = email;
        }
        assertEquals(userName, emailName);
        String token = getUrlParam("labkeyToken", true);
        xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=" + token);
        assertSuccessAuthenticationToken(xml, token, email, 32783);

        beginAt(baseUrl + "invalidateToken.view?labkeyToken=" + token + "&returnUrl=" + homePageUrl);
        // Make sure we redirected to the right place
        assertEquals("Redirected to wrong URL", homePageUrl, removeUrlParameters(getURL().toString()));
        // Should fail now
        xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=" + token);
        assertFailureAuthenticationToken(xml);

        impersonate(NORMAL_USER);

        beginAt(baseUrl + "createToken.view?returnUrl=" + homePageUrl);
        // Make sure we redirected to the right place
        assertEquals("Redirected to wrong URL", homePageUrl, removeUrlParameters(getURL().toString()));

        email = getUrlParam("labkeyEmail", true);
        assertEquals("Wrong email", NORMAL_USER, email);
        token = getUrlParam("labkeyToken", true);
        xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=" + token);
        assertSuccessAuthenticationToken(xml, token, email, 15);

        // Back to the admin user
        stopImpersonating();

        // Test that LabKey Server sign out invalidates the token
        xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=" + token);
        assertFailureAuthenticationToken(xml);
    }


    protected void assertFailureAuthenticationToken(String xml)
    {
        assertTrue(xml.startsWith("<TokenAuthentication success=\"false\" message=\"Unknown token\"/>"));
    }


    protected void assertSuccessAuthenticationToken(String xml, String token, String email, int permissions)
    {
        String correct = "<TokenAuthentication success=\"true\" token=\"" + token + "\" email=\"" + email + "\" permissions=\"" + permissions + "\"/>";
        assertTrue(xml.startsWith(correct));
    }


    private String retrieveFromUrl(String relativeUrl)
    {
        String newline = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();
        URL url;
        try {url = new URL(WebTestHelper.getBaseURL() +  relativeUrl);}
        catch (MalformedURLException ex) {throw new RuntimeException(ex);}

        try (InputStream is = url.openStream(); BufferedReader reader = Readers.getReader(is))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
                sb.append(newline);
            }

            return sb.toString();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failure attempting to retrieve " + relativeUrl, e);
        }
    }

    private String removeUrlParameters(String url)
    {
        int index = url.indexOf('?');

        if (-1 == index)
            return url;
        else
            return url.substring(0, index);
    }


    @LogMethod protected void impersonationTest()
    {
        String testUserDisplayName = getDisplayName();

        impersonate(TO_BE_DELETED_USER);
        assertTextNotPresent("Admin Console");
        stopImpersonating();

        impersonate(SITE_ADMIN_USER);
        String siteAdminDisplayName = getDisplayName(); // Use when checking audit log, below
        ensureAdminMode();
        goToAdminConsole();  // Site admin should be able to get to the admin console
        new UIUserHelper(this).deleteUsers(true, TO_BE_DELETED_USER);
        stopImpersonating();

        ensureAdminMode();
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("audit log"));

        doAndWaitForPageToLoad(() -> selectOptionByText(Locator.name("view"), "User events"));

        DataRegionTable table = new DataRegionTable("query", getDriver());

        table.getDataAsText(2, 2);
        String createdBy      = table.getDataAsText(2, "Created By");
        String impersonatedBy = table.getDataAsText(2, "Impersonated By");
        String user           = table.getDataAsText(2, "User");
        String comment        = table.getDataAsText(2, "Comment");

        assertTrue("Incorrect display for deleted user -- expected '<nnnn>', found '" + user + "'", user.matches("<\\d{4,}>"));
        assertEquals("Incorrect log entry for deleted user",
                siteAdminDisplayName + '|' + testUserDisplayName + '|' + user + '|' + TO_BE_DELETED_USER + " was deleted from the system",
                createdBy + '|' + impersonatedBy + '|' + user + '|' + comment);

        // 17037 Regression
        impersonate(PROJECT_ADMIN_USER);
        clickProject(PROJECT_NAME);
        _permissionsHelper.enterPermissionsUI();
        _ext4Helper.clickTabContainingText("Project Groups");
        assertTextPresent("Total Users");
        stopImpersonating();
    }


    @LogMethod protected void passwordStrengthTest()
    {
        String simplePassword = "3asdfghi"; // Only two character types. 8 characters long.
        String shortPassword = "4asdfg!"; // Only 7 characters long. 3 character types.
        setDbLoginConfig(PasswordRule.Strong, null);

        setInitialPassword(NORMAL_USER, simplePassword);
        assertTextPresent("Your password must contain three of the following"); // fail, too simple

        setFormElement(Locator.id("password"), shortPassword);
        setFormElement(Locator.id("password2"), shortPassword);
        clickButton("Set Password");
        assertTextPresent("Your password must be eight characters or more."); // fail, too short

        setFormElement(Locator.id("password"), PASSWORDS[0]);
        setFormElement(Locator.id("password2"), PASSWORDS[0]);
        clickButton("Set Password");
        assertSignedInNotImpersonating();
        //success
        impersonate(NORMAL_USER);

        changePassword(PASSWORDS[0], PASSWORDS[1]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[1], simplePassword); // fail, too simple
        assertTextPresent("Your password must contain three of the following");
        changePassword(PASSWORDS[1], shortPassword); // fail, too short
        assertTextPresent("Your password must be eight characters or more.");
        changePassword(PASSWORDS[1], PASSWORDS[2]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[2], PASSWORDS[3]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[3], PASSWORDS[4]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[4], PASSWORDS[5]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[5], PASSWORDS[6]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[6], PASSWORDS[7]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[7], PASSWORDS[8]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[8], PASSWORDS[9]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[9], PASSWORDS[10]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[10], PASSWORDS[1]); // fail, used 9 passwords ago.
        assertTextPresent("Your password must not match a recently used password.");
        changePassword(PASSWORDS[10], PASSWORDS[0]);
        assertTextNotPresent("Choose a new password.");

        stopImpersonating();
        resetDbLoginConfig();
    }

    @LogMethod public void loginSelfRegistrationEnabledTest()
    {
        // prep: ensure that user does not currently exist in labkey and  self register is enabled
        String selfRegUserEmail = "selfreg@test.labkey.local";
        deleteUsersIfPresent(selfRegUserEmail);
        int getResponse = getHttpResponse(WebTestHelper.getBaseURL() + "/login/setAuthenticationParameter.view?parameter=SelfRegistration&enabled=true").getResponseCode();
        assertEquals("failed to set authentication param to enable self register via http get", 200, getResponse );
        signOut();

        // test: attempt login, check if register button appears, click register
        if (!getDriver().getTitle().equals("Sign In"))
        {
            clickAndWait(Locator.linkWithText("Sign In"));
        }
        assertTitleEquals("Sign In");
        assertElementPresent(Locator.tagWithName("form", "login"));
        clickAndWait(Locator.linkWithText("Register for a new account"));

        assertTitleEquals("Register");
        assertElementPresent(Locator.tagWithName("form", "register"));
        setFormElement(Locator.id("email"), selfRegUserEmail);
        setFormElement(Locator.id("emailConfirmation"), selfRegUserEmail);
        clickButton("Register", 0);
        waitForText("We have sent a registration email to " + selfRegUserEmail);

        // cleanup: sign admin back in
        signIn();
    }

    @LogMethod public void loginSelfRegistrationDisabledTest()
    {
        // prep: ensure self register is disabled
        int getResponse = getHttpResponse(WebTestHelper.getBaseURL() + "/login/setAuthenticationParameter.view?parameter=SelfRegistration&enabled=false").getResponseCode();
        assertEquals("failed to set authentication param to disable self register via http get", 200, getResponse);
        signOut();

        // test: attempt login and confirm self register link is not on login screen
        if (!getDriver().getTitle().equals("Sign In"))
        {
            clickAndWait(Locator.linkWithText("Sign In"));
        }
        assertTitleEquals("Sign In");
        assertElementNotPresent(Locator.linkWithText("Register for a new account"));

        // cleanup: sign admin back in
        signIn();
    }
}
