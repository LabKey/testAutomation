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

import org.apache.hc.core5.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.serverapi.reader.Readers;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestProperties;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.BVT;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.pages.PermissionsEditor;
import org.labkey.test.pages.core.login.DatabaseAuthConfigureDialog;
import org.labkey.test.pages.core.login.LoginConfigurePage;
import org.labkey.test.pages.user.ShowUsersPage;
import org.labkey.test.params.login.DatabaseAuthenticationProvider;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExperimentalFeaturesHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SimpleHttpRequest;
import org.labkey.test.util.SimpleHttpResponse;
import org.labkey.test.util.UIPermissionsHelper;
import org.labkey.test.util.UIUserHelper;
import org.openqa.selenium.WebElement;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.WebTestHelper.buildURL;

@Category(BVT.class)
@BaseWebDriverTest.ClassTimeout(minutes = 11)
public class SecurityTest extends BaseWebDriverTest
{
    protected static final String PROJECT_NAME = "SecurityVerifyProject";
    protected static final String ADMIN_USER_TEMPLATE = "_admin.template@security.test";
    protected static final String NORMAL_USER_TEMPLATE = "_user.template@security.test";
    protected static final String BOGUS_USER_TEMPLATE = "bogus@bogus@bogus";
    protected static final String PROJECT_ADMIN_USER = "admin_securitytest@security.test";
    private static final String PROJECT_ADMIN_ROLE = "Project Administrator";
    private static final String FOLDER_ADMIN_ROLE = "Folder Administrator";
    protected static final String NORMAL_USER = "user_securitytest@security.test";
    private static final String ADDED_USER = "fromprojectusers@security.test";
    private static final String STRENGTH_USER = "password_strength@security.test";
    protected static final String STRONG_PASSWORD = "We'reSo$tr0ng@yekbal1!";
    protected static final String TO_BE_DELETED_USER = "delete_me@security.test";
    protected static final String SITE_ADMIN_USER = "siteadmin_securitytest@security.test";
    protected static final String PERMISSION_ERROR = "User does not have permission to perform this operation.";
    protected static final String NOT_FOUND_ERROR = "notFound";

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

    @BeforeClass
    public static void setupProject()
    {
        ((SecurityTest)getCurrentTest()).doSetup();
    }

    protected void doSetup()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
    }

    protected boolean isQuickTest()
    {
        return false;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);

        _userHelper.deleteUsers(false, ADMIN_USER_TEMPLATE, NORMAL_USER_TEMPLATE, PROJECT_ADMIN_USER, NORMAL_USER, SITE_ADMIN_USER, TO_BE_DELETED_USER, ADDED_USER, STRENGTH_USER);

        // Make sure the feature is turned off.
        Connection cn = createDefaultConnection();
        ExperimentalFeaturesHelper.setExperimentalFeature(cn, "disableGuestAccount", false);
        DatabaseAuthConfigureDialog.resetDbLoginConfig(cn);
    }

    @Test
    public void testSteps() throws IOException
    {
        if (!TestProperties.isWithoutTestModules())
        {
            enableEmailRecorder();
        }

        clonePermissionsTest();
        displayNameTest();
        tokenAuthenticationTest();
        if (!isQuickTest())
        {
            impersonationTest();
            guestTest();
            disableGuestAccountTest();
            addRemoveSiteAdminTest();
        }

        if (!TestProperties.isWithoutTestModules())
        {
            log("Check welcome emails [6 new users]");

            EmailRecordTable table = goToEmailRecord();
            assertEquals("Notification emails.", 12, table.getEmailCount());
            // Once in the message itself, plus copies in the headers
            assertTextPresent(": Welcome", 18);
        }

        if (!isQuickTest())
        {
            cantReachAdminToolFromUserAccount();
            passwordStrengthTest();
            dumbsterTest();
            loginSelfRegistrationEnabledTest();
            loginSelfRegistrationDisabledTest();
        }
        passwordResetTest();
        passwordParameterTest();
    }

    /**
     * verify that a normal user does not get a link to the admin console or see their own history, nor
     * reach an admin-only url directly.
     */
    @LogMethod
    protected void cantReachAdminToolFromUserAccount()
    {
        final String historyTabTitle = "History:";
        final Set<String> unreachableUrls = new HashSet<>(Arrays.asList("/admin/showAdmin.view?", "/user/showUsers.view?",
                "/security/group.view?group=Administrators", "/security/group.view?group=Developers",
                "/user/showUsers.view?", "/security/project.view?returnUrl=%2Fuser%2FshowUsers.view%3F", "/admin/createFolder.view?",
                "/analytics/begin.view?", "/login/configure.view?", "/admin/customizeEmail.view?", "/admin/filesSiteSettings.view?",
                "/admin/projectSettings.view?", "/flow/flowAdmin.view?", "/admin/reorderFolders.view?", "/admin/customizeSite.view?",
                "/core/configureReportsAndScripts.view?", "/audit/showAuditLog.view?", "/search/admin.view?",
                "/ms2/showMS2Admin.view?", "/experiment-types/begin.view?", "/pipeline-status/showList.view?",
                "/pipeline/setup.view?", "/ms2/showProteinAdmin.view?", "/admin/actions.view?", "/admin/caches.view?",
                "/admin/dbChecker.view?", "/query/dataSourceAdmin.view?", "/admin/dumpHeap.view?", "/admin/environmentVariables.view?",
                "/admin/memTracker.view?", "/admin/queries.view?", "/admin/resetErrorMark.view?", "/admin/showThreads.view?",
                "/admin-sql/scripts.view?", "/admin/systemProperties.view?", "/admin/emailTest.view?", "/admin/showAllErrors.view?",
                "/admin/showErrorsSinceMark.view?", "/admin/showPrimaryLog.view?",

        /* Management actions shouldn't be reachable by non-admins */
                "/admin-missingValues.view", "/admin-manageFolders.view", "/admin-moduleProperties.view", "/admin-concepts.view",
                "/search-searchSettings.view", "/admin-notifications.view", "/admin-exportFolder.view", "/admin-importFolder.view",
                "/admin-fileRoots.view", "/admin-folderInformation.view"
        ));


        //verify that you can see the text "history" in the appropriate area as admin.  If this fails, the
        //check below is worthless

        goToMyAccount();
        assertTextPresent(historyTabTitle);

        //log in as normal user
        impersonate(NORMAL_USER);

        //admin site link not available
        assertElementNotPresent(Locator.id("adminMenuPopupText"));

        //can't reach admin urls and invalid urls directly either
        for (String url : unreachableUrls)
            assertNonReachableUrl(url);

        //shouldn't be able to view own history either
        goToMyAccount();
        assertTextNotPresent(historyTabTitle);

        stopImpersonating();
    }

    @LogMethod
    public void assertNonReachableUrl(String url)
    {
        log("Attempting to reach URL user does not have permission for:  " + url);
        SimpleHttpResponse httpResponse = WebTestHelper.getHttpResponse(url);

        if ((HttpStatus.SC_FORBIDDEN != httpResponse.getResponseCode() ||
                !httpResponse.getResponseBody().contains(PERMISSION_ERROR)) &&
                (HttpStatus.SC_NOT_FOUND != httpResponse.getResponseCode() ||
                !httpResponse.getResponseBody().contains(NOT_FOUND_ERROR)))
        {
            // Go to page for better failure screenshot
            beginAt(url);
            fail("Url should be forbidden for non-admin: " + url);
        }
    }

    /**
     *
     * preconditions:  NORMAL_USER exists with password NORMAL_USER_PASSWORD.  Currently logged in as admin
     * post conditions
     */
    @LogMethod
    public void passwordResetTest()
    {
        LoginConfigurePage.beginAt(this)
                .getPrimaryConfigurationRow("Standard database authentication")
                .clickEdit(new DatabaseAuthenticationProvider())
                .setDbLoginConfig(DatabaseAuthConfigureDialog.PasswordStrength.Weak,
                        DatabaseAuthConfigureDialog.PasswordExpiration.Never);

        //get user a password
        String username = NORMAL_USER;
        String password = STRONG_PASSWORD;

        password = adminPasswordResetTest(username, password+"adminReset");
        
        String resetUrl = userForgotPasswordWorkflowTest(username, password);
        
        userPasswordResetTest(resetUrl);

        ensureSignedInAsPrimaryTestUser();
    }

    @LogMethod
    public void passwordParameterTest()
    {
        // 31000: fail login actions if parameters present on URL
        SimplePostCommand command = new SimplePostCommand("login", "loginAPI");

        Map<String, Object> params = new HashMap<>();
        params.put("email", NORMAL_USER);
        params.put("password", STRONG_PASSWORD);
        params.put("foo", "bar");

        command.setParameters(params);
        boolean rejectedProperly = false;

        try
        {
            Connection cn = createDefaultConnection();
            command.execute(cn, null);
        }
        catch (CommandException e)
        {
            if (HttpServletResponse.SC_BAD_REQUEST == e.getStatusCode())
                rejectedProperly = true;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to connect to login-loginAPI.api action.", e);
        }

        assertTrue("Expected email/password in URL to be rejected.", rejectedProperly);
    }

    @LogMethod
    private void dumbsterTest()
    {
        assertNoDumbsterPermission(PROJECT_ADMIN_USER);
        assertNoDumbsterPermission(NORMAL_USER);
    }

    @LogMethod
    private void assertNoDumbsterPermission(@LoggedParam String user)
    {
        clickProject(PROJECT_NAME);
        goToModule("Dumbster");
        pushLocation();
        impersonate(user);
        popLocation();
        assertTextPresent("You must be a site or application administrator to view the email record.");
        stopImpersonating();
    }

    /**
     * Preconditions: able to reset user's password at resetUrl, db in weak-password mode
     */
    @LogMethod
    protected void userPasswordResetTest(String resetUrl)
    {
        ensureSignedOut();

        beginAt(resetUrl);

        attemptSetInvalidPassword("fooba", "fooba", "Your password must be at least six characters and cannot contain spaces.");
        attemptSetInvalidPassword("foobar", "foobar2", "Your password entries didn't match.");

        resetPassword(resetUrl, NORMAL_USER, STRONG_PASSWORD);
    }

    @LogMethod
    protected void attemptSetInvalidPassword(String password1, String password2, String... errors)
    {
        setFormElement(Locator.id("password"), password1);
        setFormElement(Locator.id("password2"), password2);
        clickButton("Set Password");
        assertTextPresent(errors);
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
    @LogMethod
    private String userForgotPasswordWorkflowTest(String username, String password)
    {
        ensureSignedOut();

        String resetUrl = userInitiatePasswordReset(username);

        signOut();

        //attempt sign in with old password- should succeed
        signIn(username, password);
        signOut();

        return resetUrl;
    }

    @LogMethod
    public String userInitiatePasswordReset(String username)
    {
        goToHome();
        ensureSignedOut();

        clickAndWait(Locator.linkWithText("Sign In"));
        clickAndWait(Locator.linkContainingText("Forgot password"));
        setFormElement(Locator.id("email"), username);
        clickButtonContainingText("Reset", 0);

        signIn();
        return getPasswordResetUrl(username);
    }

    String[] wrongPasswordEntered =
                new String[] {"The email address and password you entered did not match any accounts on file.",
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
    @LogMethod
    private String adminPasswordResetTest(String username, String password)
    {
        String newPassword = password +"1";
        goToSiteUsers();
        filterUsersForEmail(username);
        clickAndWait(Locator.linkContainingText(_userHelper.getDisplayNameForEmail(username)));
        clickButton("Reset Password");
        assertTextPresent("You are about to clear the user's current password");
        clickAndWait(Locator.lkButton("OK"));

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

    private void filterUsersForEmail(String email)
    {
        DataRegionTable users = new DataRegionTable("Users", getDriver());
        users.setFilter("Email", "Equals", email);
    }

    @LogMethod
    protected void addRemoveSiteAdminTest()
    {
        // test for issue 13921
        goToSiteAdmins();
        setFormElement(Locator.name("names"), NORMAL_USER);
        uncheckCheckbox(Locator.checkboxByName("sendEmail"));
        clickButton("Update Group Membership");
        assertTextPresent(NORMAL_USER);
        checkCheckbox(Locator.checkboxByNameAndValue("delete", NORMAL_USER));
        clickButton("Update Group Membership", 0);
        assertAlert("Are you sure you want to permanently remove the selected user from this group?");
        sleep(1000);
        assertElementNotPresent(Locator.checkboxByNameAndValue("delete", NORMAL_USER));
        goToProjectHome();
    }

    @LogMethod
    protected void guestTest()
    {
        goToProjectHome();
        ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);
        permissionsHelper.setSiteGroupPermissions("All Site Users", "Author");
        permissionsHelper.setSiteGroupPermissions("Guests", "Reader");

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Messages");
        waitForElement(Locator.lkButton("New"));
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

    @LogMethod
    protected void disableGuestAccountTest()
    {
        ExperimentalFeaturesHelper.setExperimentalFeature(createDefaultConnection(), "disableGuestAccount", true);

        goToHome();
        signOut();

        // Validate that the user is shown a login screen.
        checker().withScreenshot("disableGuestAccountTest")
                .verifyTrue("Should be on login page when guest account is disabled",
                        isElementPresent(Locator.tagWithName("form", "login")));

        signIn();
        ExperimentalFeaturesHelper.setExperimentalFeature(createDefaultConnection(), "disableGuestAccount", false);
    }

    @LogMethod
    protected void displayNameTest()
    {
        final UIUserHelper uiUserHelper = new UIUserHelper(this);

        String newDisplayName = "changeDisplayTest";

        uiUserHelper.setDisplayName(NORMAL_USER, newDisplayName);
        assertTextPresent(newDisplayName);

        String injectDisplayName = "displayName" + INJECT_CHARS_1;

        uiUserHelper.setDisplayName(NORMAL_USER, injectDisplayName);
        assertTextPresent(injectDisplayName);
        assertTextNotPresent(newDisplayName);
    }

    @LogMethod
    protected void clonePermissionsTest()
    {
        UIPermissionsHelper _permissionsHelper = new UIPermissionsHelper(this);
        UIUserHelper uiUserHelper = new UIUserHelper(this);
        // create admin templates, plus test bogus & duplicate email addresses
        uiUserHelper.createUser(ADMIN_USER_TEMPLATE + '\n' + NORMAL_USER_TEMPLATE + '\n' + NORMAL_USER_TEMPLATE + '\n' + BOGUS_USER_TEMPLATE, true, false);
        assertTextPresent("Failed to create user bogus@bogus@bogus: Invalid email address",
                NORMAL_USER_TEMPLATE + " was already a registered system user.");//here to see this user's profile and history.");
        //nav trail check
        assertElementPresent(Locator.tagWithClass("ol", "breadcrumb").child("li").child(Locator.tagContainingText("a", "Site Users")));

        goToProjectHome();
        // Add permissions for a site group
        _permissionsHelper.setSiteGroupPermissions("Guests", "Reader");
        // Add a non-group permission
        _permissionsHelper.setUserPermissions(ADMIN_USER_TEMPLATE, "Editor");
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

        // Issue 35282: Create a group that contains another group to assure group membership for the cloned user is
        // only to the direct group a user is not, not the recursive set of groups.
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.createPermissionsGroup("Containing Group");
        _permissionsHelper.clickManageGroup("Containing Group");
        setFormElement(Locator.name("names"), "Testers");
        clickButton("Update Group Membership");
        // make sure user that is cloned from NORMAL_USER_TEMPLATE is not added to the containing group, only to the contained one

        // create users and verify permissions
        uiUserHelper.cloneUser(PROJECT_ADMIN_USER, ADMIN_USER_TEMPLATE);
        uiUserHelper.cloneUser(SITE_ADMIN_USER, PasswordUtil.getUsername());
        uiUserHelper.cloneUser(NORMAL_USER, NORMAL_USER_TEMPLATE);
        uiUserHelper.cloneUser(TO_BE_DELETED_USER, NORMAL_USER_TEMPLATE);
        log("Verify individual (non-group) permissions were cloned");
        goToProjectHome();
        ApiPermissionsHelper helper = new ApiPermissionsHelper(this);
        helper.assertPermissionSetting(PROJECT_ADMIN_USER, "Editor");
        log("Verify that group permissions did not get assigned individually");
        _permissionsHelper.assertNoPermission(PROJECT_ADMIN_USER, "Reader");
        _permissionsHelper.assertNoPermission(NORMAL_USER, "Reader");
        _permissionsHelper.assertNoPermission(NORMAL_USER, "Editor");

        // verify permissions
        checkGroupMembership(PROJECT_ADMIN_USER, "SecurityVerifyProject/Administrators", 2);
        checkGroupMembership(NORMAL_USER, "SecurityVerifyProject/Testers", 1);
        assertNavTrail("Site Users", "User Details", "Permissions");
    }

    @Test
    public void testAddUserAsProjAdmin()
    {
        beginAt(WebTestHelper.buildURL("project", getProjectName(), "begin"));
        impersonateRoles(PROJECT_ADMIN_ROLE);
        ShowUsersPage usersPage = goToProjectUsers();

        usersPage
                .clickAddUsers()
                .setNewUsers(Arrays.asList(ADDED_USER))
                .setSendNotification(true)
                .clickAddUsers();

        assertTextPresent(ADDED_USER);
        stopImpersonating();
    }

    @Test
    public void testCantAddUserAsFolderAdmin()
    {
        beginAt(WebTestHelper.buildURL("project", getProjectName(), "begin"));
        impersonateRoles(FOLDER_ADMIN_ROLE);
        goToProjectUsers();

        assertElementNotPresent(Locator.lkButton("Add Users"));
        stopImpersonating();
    }

    @LogMethod
    protected void checkGroupMembership(String userName, String groupName, int expectedCount)
    {
        goToSiteUsers();

        Locator userAccessLink = Locator.xpath("//td[text()='" + userName + "']/..").append(Locator.linkWithText("permissions"));
        boolean isPresent = isElementPresent(userAccessLink);

        // If user is not found, filter (in case the user is on the next page)
        if (!isPresent)
        {
            filterUsersForEmail(userName);
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

    @LogMethod
    protected void tokenAuthenticationTest() throws IOException
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

        // Ensure we can POST to verify token action without CSRF token, #36450
        // Ideally, we'd POST the token in the body, but our SimpleHttpRequest doesn't support parameters
        xml = postToUrl(baseUrl + "verifyToken.view?labkeyToken=" + token);
        assertSuccessAuthenticationToken(xml, token, email, 32783);

        beginAt(baseUrl + "invalidateToken.view?labkeyToken=" + token + "&returnUrl=" + homePageUrl);
        // Make sure we redirected to the right place
        assertEquals("Redirected to wrong URL", homePageUrl, removeUrlParameters(getURL().toString()));
        // Should fail now
        xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=" + token);
        assertFailureAuthenticationToken(xml);

        // #40884 - Verify that while impersonating, token authentication still resolves to admin user
        impersonate(NORMAL_USER);

        beginAt(baseUrl + "createToken.view?returnUrl=" + homePageUrl);
        // Make sure we redirected to the right place
        assertEquals("Redirected to wrong URL", homePageUrl, removeUrlParameters(getURL().toString()));

        email = getUrlParam("labkeyEmail", true);
        assertEquals("Wrong email", userName, email);
        token = getUrlParam("labkeyToken", true);
        xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=" + token);
        assertSuccessAuthenticationToken(xml, token, email, 32783);

        // Back to the admin user
        stopImpersonating();

        // Test that LabKey Server sign out invalidates the token
        xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=" + token);
        assertFailureAuthenticationToken(xml);
    }


    @LogMethod
    public String postToUrl(String url) throws IOException
    {
        log("Posting to " + url);
        SimpleHttpRequest request = new SimpleHttpRequest(WebTestHelper.getBaseURL() + url, "POST");
        SimpleHttpResponse response = request.getResponse();

        return response.getResponseBody();
    }

    protected void assertFailureAuthenticationToken(String xml)
    {
        assertTrue(xml.startsWith("<TokenAuthentication success=\"false\" message=\"Unknown token\"/>"));
    }


    protected void assertSuccessAuthenticationToken(String xml, String token, String email, int permissions)
    {
        String correct = "<TokenAuthentication success=\"true\" token=\"" + token + "\" email=\"" + email + "\" permissions=\"" + permissions + "\"/>";
        assertTrue(xml, xml.startsWith(correct));
    }


    private String retrieveFromUrl(String relativeUrl)
    {
        log("Retrieving from " + relativeUrl);
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
        return url.substring(0, index);
    }


    @LogMethod
    protected void impersonationTest()
    {
        String testUserDisplayName = getDisplayName();

        impersonate(TO_BE_DELETED_USER);
        assertTextNotPresent("Admin Console");
        stopImpersonating();

        impersonate(SITE_ADMIN_USER);
        String siteAdminDisplayName = getDisplayName(); // Use when checking audit log, below
        goToAdminConsole();  // Site admin should be able to get to the admin console
        new UIUserHelper(this).deleteUsers(true, TO_BE_DELETED_USER);
        stopImpersonating();

        goToAdminConsole().clickAuditLog();

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
        PermissionsEditor.enterPermissionsUI(this);
        _ext4Helper.clickTabContainingText("Project Groups");
        assertTextPresent("Total Users");
        stopImpersonating();
    }


    @LogMethod
    protected void passwordStrengthTest()
    {
        String simplePassword = "3asdfghi"; // Only two character types. 8 characters long.
        String shortPassword = "4asdfg!"; // Only 7 characters long. 3 character types.
        String goodPassword = "Yekbal1!"; // 8 characters long. 3+ character types.
        LoginConfigurePage configurePage = LoginConfigurePage.beginAt(this);
        configurePage
                .getPrimaryConfigurationRow("Standard database authentication")
                .clickEdit(new DatabaseAuthenticationProvider())
                .setDbLoginConfig(DatabaseAuthConfigureDialog.PasswordStrength.Strong,
                                DatabaseAuthConfigureDialog.PasswordExpiration.Never);
        // don't click 'Save and Finish' here; setting to good/never in dbAuth doesn't require a page-level submit

        _userHelper.createUser(STRENGTH_USER);

        setInitialPassword(STRENGTH_USER, simplePassword);
        assertTextPresent("Your password is not complex enough."); // fail, too simple

        setFormElement(Locator.id("password"), shortPassword);
        setFormElement(Locator.id("password2"), shortPassword);
        clickButton("Set Password");
        assertTextPresent("Your password is not complex enough."); // fail, too short

        setFormElement(Locator.id("password"), goodPassword);
        setFormElement(Locator.id("password2"), goodPassword);
        assertTextPresent("Your password is not complex enough."); // fail, not complex enough

        setFormElement(Locator.id("password"), STRONG_PASSWORD);
        setFormElement(Locator.id("password2"), STRONG_PASSWORD);
        clickButton("Set Password");
        assertSignedInNotImpersonating();
        //success
        impersonate(STRENGTH_USER);

        changePassword(STRONG_PASSWORD, simplePassword); // fail, too simple
        assertTextPresent("Your password is not complex enough.");
        changePassword(STRONG_PASSWORD, shortPassword); // fail, too short
        assertTextPresent("Your password is not complex enough.");
        changePassword(STRONG_PASSWORD, goodPassword); // fail, not complex enough
        assertTextPresent("Your password is not complex enough.");
        String currentPassword = STRONG_PASSWORD + 0;
        changePassword(STRONG_PASSWORD, currentPassword);
        assertTextNotPresent("Choose a new password.");
        int i = 1;
        for (; i < 9; i++)
        {
            changePassword(currentPassword, STRONG_PASSWORD + i);
            currentPassword = STRONG_PASSWORD + i;
            assertTextNotPresent("Choose a new password.");
        }
        changePassword(currentPassword, STRONG_PASSWORD + 0); // fail, used 9 passwords ago.
        assertTextPresent("Your password must not match a recently used password.");
        changePassword(STRONG_PASSWORD + i, STRONG_PASSWORD);
        assertTextNotPresent("Choose a new password.");

        stopImpersonating();
        DatabaseAuthConfigureDialog.resetDbLoginConfig(createDefaultConnection());
    }

    @LogMethod
    public void loginSelfRegistrationEnabledTest()
    {
        // prep: ensure that user does not currently exist in labkey and self register is enabled
        // Cleanup left after the addition of captcha support to ensure no lingering accounts will cause problems
        String selfRegUserEmail = "selfreg@test.labkey.local";
        _userHelper.deleteUsers(false, selfRegUserEmail);

        int getResponse = setAuthenticationParameter("SelfRegistration", true);
        assertEquals("failed to set authentication param to enable self register via http get", 200, getResponse );
        signOut();

        // test: attempt login, check if register button appears, click register
        if (!getDriver().getTitle().equals("Sign In"))
        {
            clickAndWait(Locator.linkWithText("Sign In"));
        }
        assertTitleContains("Sign In");
        assertElementPresent(Locator.tagWithName("form", "login"));
        clickAndWait(Locator.lkButton("Register"));

        assertTitleContains("Register");
        assertElementPresent(Locator.tagWithName("form", "register"));
        setFormElement(Locator.id("email"), selfRegUserEmail);
        setFormElement(Locator.id("emailConfirmation"), selfRegUserEmail);
        clickButton("Register", 0);
        // Can't bypass the captcha and can't parse it from here, so just validation we get the expected error
        waitForElement(Locator.id("errors").containing("Verification text does not match"));

        // cleanup: sign admin back in
        signIn();
    }

    @LogMethod
    public void loginSelfRegistrationDisabledTest()
    {
        // prep: ensure self register is disabled
        int getResponse = setAuthenticationParameter("SelfRegistration", false);
        assertEquals("failed to set authentication param to disable self register via http get", 200, getResponse);
        signOut();

        // test: attempt login and confirm self register link is not on login screen
        if (!getDriver().getTitle().equals("Sign In"))
        {
            clickAndWait(Locator.linkWithText("Sign In"));
        }
        assertTitleContains("Sign In");
        WebElement link = Locator.button("Register").findElementOrNull(getDriver());
        assertFalse("Self-registration button is visible", link != null && link.isDisplayed());

        beginAt(buildURL("login", "register"));
        waitForElement(Locators.labkeyErrorHeading.withText("Registration is not enabled."));

        // cleanup: sign admin back in
        signIn();
    }
}
