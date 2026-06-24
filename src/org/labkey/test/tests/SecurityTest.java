/*
 * Copyright (c) 2008-2026 LabKey Corporation
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
import org.labkey.remoteapi.Connection;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestProperties;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.BVT;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.pages.LabkeyErrorPage;
import org.labkey.test.pages.admin.PermissionsPage;
import org.labkey.test.pages.user.ShowUsersPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.OptionalFeatureHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SimpleHttpResponse;
import org.labkey.test.util.UIPermissionsHelper;
import org.labkey.test.util.UIUserHelper;
import org.labkey.test.util.core.login.DbLoginUtils;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.IOException;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.WebTestHelper.buildURL;
import static org.labkey.test.util.PermissionsHelper.AUTHOR_ROLE;
import static org.labkey.test.util.PermissionsHelper.EDITOR_ROLE;
import static org.labkey.test.util.PermissionsHelper.READER_ROLE;

@Category(BVT.class)
@BaseWebDriverTest.ClassTimeout(minutes = 11)
public class SecurityTest extends BaseWebDriverTest
{
    protected static final String PROJECT_NAME = "SecurityVerifyProject";
    protected static final String ADMIN_USER_TEMPLATE = "_admin.template@security.test";
    protected static final String NORMAL_USER_TEMPLATE = "_user.template@security.test";
    protected static final String BOGUS_USER_TEMPLATE = "bogus@bogus@bogus";
    protected static final String PROJECT_ADMIN_USER = "admin_securitytest@security.test";
    protected static final String NORMAL_USER = "user_securitytest@security.test";
    private static final String ADDED_USER = "fromprojectusers@security.test";
    protected static final String TO_BE_DELETED_USER = "delete_me@security.test";
    protected static final String SITE_ADMIN_USER = "siteadmin_securitytest@security.test";
    protected static final String NOT_FOUND_ERROR = "notFound";

    protected static final String SIGN_IN_TEXT = "Sign In";

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
        ((SecurityTest) getCurrentTest()).doSetup();
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

        _userHelper.deleteUsers(false, ADMIN_USER_TEMPLATE, NORMAL_USER_TEMPLATE, PROJECT_ADMIN_USER, NORMAL_USER, SITE_ADMIN_USER, TO_BE_DELETED_USER, ADDED_USER);

        // Make sure the feature is turned off.
        Connection cn = createDefaultConnection();
        OptionalFeatureHelper.setOptionalFeature(cn, "disableGuestAccount", false);
        DbLoginUtils.resetDbLoginConfig(cn);
    }

    @Test
    public void testSecurityTxt() throws IOException
    {
        getDriver().navigate().to(WebTestHelper.getBaseURL() + "/.well-known/security.txt");

        String body = getBodyText();

        Properties props = new Properties();
        props.load(new StringReader(body));

        assertTrue("security.txt missing Contact", props.containsKey("Contact"));
        assertTrue("security.txt missing Policy", props.containsKey("Policy"));
        assertTrue("security.txt missing Expires", props.containsKey("Expires"));

        String expiresStr = props.getProperty("Expires");
        ZonedDateTime expires = ZonedDateTime.parse(expiresStr, DateTimeFormatter.ISO_INSTANT.withZone(java.time.ZoneId.of("UTC")));
        ZonedDateTime nextYear = ZonedDateTime.now(java.time.ZoneId.of("UTC")).plusYears(1);

        // If this assert fails, edit security.txt to use something more than a year in the future
        assertTrue("security.txt 'Expires' value should be more than a year in the future: " + expiresStr, expires.isAfter(nextYear));
    }

    @Test
    public void testSteps() throws IOException
    {
        if (!TestProperties.isWithoutTestModules())
        {
            enableEmailRecorder();
        }

        useReturnDuringSignInTest();
        clonePermissionsTest();
        if (!isQuickTest())
        {
            impersonationTest();
            guestTest();
            disableGuestAccountTest();
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
            dumbsterTest();
            loginSelfRegistrationEnabledTest();
            loginSelfRegistrationDisabledTest();
        }
    }

    /**
     * verify that a normal user does not get a link to the admin console or see their own history, nor
     * reach an admin-only url directly.
     */
    @LogMethod
    protected void cantReachAdminToolFromUserAccount()
    {
        final String historyTabTitle = "History:";
        final Set<String> unreachableUrls = new HashSet<>(Arrays.asList("/admin-showAdmin.view", "/user-showUsers.view",
            "/security-group.view?group=Administrators", "/security-group.view?group=Developers",
            "/user-showUsers.view", "/security-project.view?returnUrl=%2Fuser%2FshowUsers.view%3F", "/admin-createFolder.view",
            "/analytics-begin.view", "/login-configure.view", "/admin-customizeEmail.view", "/admin-filesSiteSettings.view",
            "/admin-projectSettings.view", "/flow-flowAdmin.view", "/admin-reorderFolders.view", "/admin-customizeSite.view",
            "/core-configureReportsAndScripts.view", "/audit-showAuditLog.view", "/search-admin.view",
            "/ms2-showMS2Admin.view", "/experiment-types-begin.view", "/pipeline-status-showList.view",
            "/pipeline-setup.view", "/ms2-showProteinAdmin.view", "/admin-actions.view", "/admin-caches.view",
            "/admin-dbChecker.view", "/query-dataSourceAdmin.view", "/admin-dumpHeap.view", "/admin-environmentVariables.view",
            "/admin-memTracker.view", "/admin-queries.view", "/admin-resetErrorMark.view", "/admin-showThreads.view",
            "/admin-sql-scripts.view", "/admin-systemProperties.view", "/admin-emailTest.view", "/admin-showAllErrors.view",
            "/admin-showErrorsSinceMark.view", "/admin-showPrimaryLog.view",

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
                !httpResponse.getResponseBody().contains(LabkeyErrorPage.UNAUTHORIZED_FULL_PAGE_MESSAGE)) &&
                (HttpStatus.SC_NOT_FOUND != httpResponse.getResponseCode() ||
                !httpResponse.getResponseBody().contains(NOT_FOUND_ERROR)))
        {
            // Go to page for better failure screenshot
            beginAt(url);
            fail("Url should be forbidden for non-admin: " + url);
        }
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

    private void filterUsersForEmail(String email)
    {
        DataRegionTable users = new DataRegionTable("Users", getDriver());
        users.setFilter("Email", "Equals", email);
    }

    @LogMethod
    protected void guestTest()
    {
        goToProjectHome();
        ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);
        permissionsHelper.setSiteGroupPermissions("All Site Users", AUTHOR_ROLE);
        permissionsHelper.setSiteGroupPermissions("Guests", READER_ROLE);

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
        OptionalFeatureHelper.setOptionalFeature(createDefaultConnection(), "disableGuestAccount", true);

        goToHome();
        signOut();

        // Validate that the user is shown a login screen.
        checker().withScreenshot("disableGuestAccountTest")
                .verifyTrue("Should be on login page when guest account is disabled",
                        isElementPresent(Locator.tagWithName("form", "login")));

        signIn();
        OptionalFeatureHelper.setOptionalFeature(createDefaultConnection(), "disableGuestAccount", false);
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
        _permissionsHelper.setSiteGroupPermissions("Guests", READER_ROLE);
        // Add a non-group permission
        _permissionsHelper.setUserPermissions(ADMIN_USER_TEMPLATE, EDITOR_ROLE);
        _permissionsHelper.createPermissionsGroup("Administrators");
        _permissionsHelper.clickManageGroup("Administrators");
        setFormElement(Locator.name("names"), ADMIN_USER_TEMPLATE);
        clickButton("Update Group Membership");
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setPermissions("Administrators", PermissionsHelper.PROJECT_ADMIN_ROLE);

        _permissionsHelper.createPermissionsGroup("Testers");
        _permissionsHelper.assertPermissionSetting("Testers", "No Permissions");
        _permissionsHelper.setPermissions("Testers", EDITOR_ROLE);
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
        helper.assertPermissionSetting(PROJECT_ADMIN_USER, EDITOR_ROLE);
        log("Verify that group permissions did not get assigned individually");
        _permissionsHelper.assertNoPermission(PROJECT_ADMIN_USER, READER_ROLE);
        _permissionsHelper.assertNoPermission(NORMAL_USER, READER_ROLE);
        _permissionsHelper.assertNoPermission(NORMAL_USER, EDITOR_ROLE);

        // verify permissions
        checkGroupMembership(PROJECT_ADMIN_USER, "SecurityVerifyProject/Administrators", 2);
        checkGroupMembership(NORMAL_USER, "SecurityVerifyProject/Testers", 1);
        assertNavTrail("Site Users", "User Details", "Permissions");
    }

    @Test
    public void testAddUserAsProjAdmin()
    {
        beginAt(WebTestHelper.buildURL("project", getProjectName(), "begin"));
        impersonateRoles(PermissionsHelper.PROJECT_ADMIN_ROLE);
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
        impersonateRoles(PermissionsHelper.FOLDER_ADMIN_ROLE);
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
            click(Locator.xpath("//tr[td/a[text()='" + getProjectName() + "']]//img"));
            assertElementPresent(Locator.linkWithText(groupName), expectedCount);
            return;
        }
        fail("Unable to verify group membership of cloned user privileges");
    }

    @LogMethod
    protected void useReturnDuringSignInTest()
    {
        signOut();
        clickAndWait(Locator.linkWithText(SIGN_IN_TEXT));

        waitForText("Remember my email address");

        assertElementPresent(Locator.tagWithName("form", "login"));
        setFormElement(Locator.id("email"), PasswordUtil.getUsername());

        String password = PasswordUtil.getPassword();
        WebElement input = Locator.id("password").findElement(getDriver());
        input.clear();
        input.sendKeys(password);
        input.sendKeys(Keys.ENTER);
        shortWait().until(ExpectedConditions.invisibilityOfElementLocated(Locator.byClass("signing-in-msg")));
        shortWait().until(ExpectedConditions.urlContains("/home/project-begin.view"));
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
        String createdBy = table.getDataAsText(2, "Created By");
        String impersonatedBy = table.getDataAsText(2, "Impersonated By");
        String user = table.getDataAsText(2, "User");
        String comment = table.getDataAsText(2, "Comment");

        assertTrue("Incorrect display for deleted user -- expected '<nnnn>', found '" + user + "'", user.matches("<\\d{4,}>"));
        assertEquals("Incorrect log entry for deleted user",
                siteAdminDisplayName + '|' + testUserDisplayName + '|' + user + '|' + TO_BE_DELETED_USER + " was deleted from the system",
                createdBy + '|' + impersonatedBy + '|' + user + '|' + comment);

        // 17037 Regression
        impersonate(PROJECT_ADMIN_USER);
        clickProject(PROJECT_NAME);
        PermissionsPage.enterPermissionsUI(this);
        _ext4Helper.clickTabContainingText("Project Groups");
        assertTextPresent("Total Users");
        stopImpersonating();
    }

    @LogMethod
    public void loginSelfRegistrationEnabledTest()
    {
        // prep: ensure that user does not currently exist in labkey and self register is enabled
        // Cleanup left after the addition of captcha support to ensure no lingering accounts will cause problems
        String selfRegUserEmail = "selfreg@test.labkey.local";
        _userHelper.deleteUsers(false, selfRegUserEmail);

        int getResponse = setAuthenticationParameter("SelfRegistration", true);
        assertEquals("failed to set authentication param to enable self register via http get", 200, getResponse);
        signOut();

        // test: attempt login, check if register button appears, click register
        if (!getDriver().getTitle().equals(SIGN_IN_TEXT))
        {
            clickAndWait(Locator.linkWithText(SIGN_IN_TEXT));
        }
        assertTitleContains(SIGN_IN_TEXT);
        assertElementPresent(Locator.tagWithName("form", "login"));
        clickAndWait(Locator.linkWithText("Register"));

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
        if (!getDriver().getTitle().equals(SIGN_IN_TEXT))
        {
            clickAndWait(Locator.linkWithText(SIGN_IN_TEXT));
        }
        assertTitleContains(SIGN_IN_TEXT);
        WebElement link = Locator.linkWithText("Register").findElementOrNull(getDriver());
        assertFalse("Self-registration button is visible", link != null && link.isDisplayed());

        beginAt(buildURL("login", "register"));
        waitForElement(Locators.labkeyErrorHeading.withText("Registration is not enabled."));

        // cleanup: sign admin back in
        signIn();
    }

    @LogMethod
    @Test
    public void invokeMutatingSqlAction()
    {
        // Ensure that a GET request invoking mutating SQL is flagged
        String feature = "AllowMutatingSqlViaGet";
        Connection conn = createDefaultConnection();
        OptionalFeatureHelper.disableOptionalFeature(conn, feature);
        beginAt(buildURL("test", "executeMutatingSql"));
        assertTextPresent("MUTATING SQL executed as part of handling action: GET org.labkey.devtools.TestController$ExecuteMutatingSqlAction");
        checkExpectedErrors(2);

        // Turn on the deprecated feature flag and ensure that GET request can invoke mutating SQL
        OptionalFeatureHelper.enableOptionalFeature(conn, feature);
        beginAt(buildURL("test", "executeMutatingSql"));
        assertTextPresent("UPDATE via GET was allowed!");

        // Restore flag to original value
        OptionalFeatureHelper.resetOptionalFeature(conn, feature);
    }
}