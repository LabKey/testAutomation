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

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Connection;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.IssuesHelper;
import org.labkey.test.util.UIUserHelper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 9)
public class UserTest extends BaseWebDriverTest
{
    private static final String[] REQUIRED_FIELDS = {"FirstName", "LastName", "Phone", "Mobile"};
    private static final String TEST_PASSWORD = "0asdfgh!";

    /**copied from LoginController.EMAIL_PASSWORDMISMATCH_ERROR, but needs to be broken into multiple separate sentences,
     *  the search function can't handle the line breaks
     */
    private static final String[] EMAIL_PASSWORD_MISMATCH_ERROR =
            {"The email address and password you entered did not match any accounts on file.",
             "Note: Passwords are case sensitive; make sure your Caps Lock is off."};

    private static final String NORMAL_USER = "normal_user@user.test";
    private static final String BLANK_USER = "blank_user@user.test";
    private static final String DEACTIVATED_USER = "disabled_user@user.test";
    private static final String PASSWORD_RESET_USER = "pwreset_user@user.test";

    //users for change email tests.  Both included at top level so they can be included in the clean up.
    // only one should exist at any one time, but by deleting both we ensure that nothing persists even if
    // the test fails
    private static final String CHANGE_EMAIL_USER = "pre-pw_change@user.test";
    private static final String CHANGE_EMAIL_USER_ALTERNATE = "post-pw_change@user.test";
    private static final String SELF_SERVICE_EMAIL_USER = "oldaddress@user.test";
    private static final String SELF_SERVICE_EMAIL_USER_CHANGED = "newaddress@user.test";

    protected final UIUserHelper _userHelper = new UIUserHelper(this);

    @Nullable
    @Override
    protected String getProjectName()
    {
        return "UserTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("core");
    }

    @BeforeClass
    public static void setupProject()
    {
        UserTest init = (UserTest)getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        createUserWithPermissions(NORMAL_USER, getProjectName(), "Editor");
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);

        _userHelper.deleteUsers(false, CHANGE_EMAIL_USER, CHANGE_EMAIL_USER_ALTERNATE, NORMAL_USER, DEACTIVATED_USER, PASSWORD_RESET_USER, BLANK_USER, SELF_SERVICE_EMAIL_USER, SELF_SERVICE_EMAIL_USER_CHANGED);

        DomainDesignerPage domainDesignerPage = goToSiteUsers().clickChangeUserProperties();
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        for (String field : REQUIRED_FIELDS)
        {
            domainFormPanel.getField(field).setRequiredField(false);
        }

        for (String field : Arrays.asList(PROP_NAME2, PROP_NAME1))
        {
            DomainFieldRow domainFieldRow = domainFormPanel.getField(field);
            if (domainFieldRow != null)
            {
                domainFormPanel.removeField(field, true);
            }
        }
        domainDesignerPage.clickFinish();
    }

    @Before
    public void preTest()
    {
        enableEmailRecorder();
    }

    @Test
    public void testSiteUsersPermission()
    {
        goToSiteUsers();
        assertTextPresent("Email", "Display Name", "First Name", "Last Login", "Has Password");

        goToMyAccount();
        for (String label : Arrays.asList("Email", "Display Name", "First Name", "Last Login", "Has Password", "Avatar"))
            assertTrue(hasUserProfileFormLabel(label));

        impersonate(NORMAL_USER);
        goToMyAccount();
        for (String label : Arrays.asList("Display Name", "First Name", "Last Login", "Avatar"))
            assertTrue(hasUserProfileFormLabel(label));
        for (String label : Arrays.asList("Email", "Has Password"))
            assertFalse(hasUserProfileFormLabel(label));
        stopImpersonating();
    }

    private boolean hasUserProfileFormLabel(String label)
    {
        return isElementPresent(Locator.tagWithClass("td", "lk-form-label").withText(label + ":"));
    }

    @Test
    public void testChangeUserEmail()
    {
        new UIUserHelper(this).cloneUser(CHANGE_EMAIL_USER, NORMAL_USER);
        setInitialPassword(CHANGE_EMAIL_USER, TEST_PASSWORD);

        //change their email address
        changeUserEmail(CHANGE_EMAIL_USER, CHANGE_EMAIL_USER_ALTERNATE);

        signOut();

        //verify can log in with new address
        signIn(CHANGE_EMAIL_USER_ALTERNATE, TEST_PASSWORD);

        signOut();

        //verify can't log in with old address
        signInShouldFail(CHANGE_EMAIL_USER, TEST_PASSWORD, EMAIL_PASSWORD_MISMATCH_ERROR);

        simpleSignIn();

        _userHelper.deleteUsers(true, CHANGE_EMAIL_USER_ALTERNATE);
    }

    @Test
    public void testSelfServiceEmailSupport()
    {
        final String AFFIRM_CHANGE_MSG = "Email change from @1 to @2 was successful.";

        log("Make sure we are recording sent emails to dumpster");
        enableEmailRecorder();

        log("Turn on support for self service email.");
        int getResponse = setAuthenticationParameter("SelfServiceEmailChanges", true);
        assertEquals("Failed to set authentication param to enable self service email via http get", 200, getResponse);

        log("Create a new user.");
        _userHelper.createUser(SELF_SERVICE_EMAIL_USER, true, true);
        setInitialPassword(SELF_SERVICE_EMAIL_USER, TEST_PASSWORD);

        goToHome();

        log("Impersonate user: " + SELF_SERVICE_EMAIL_USER);
        impersonate(SELF_SERVICE_EMAIL_USER);

        log("Goto the account maintenance page and change the email address.");
        changeEmailAddress(SELF_SERVICE_EMAIL_USER, SELF_SERVICE_EMAIL_USER_CHANGED, TEST_PASSWORD);

        goToHome();

        log("Stop impersonating " + SELF_SERVICE_EMAIL_USER);
        stopImpersonating();

        log("Go to dumpster, make sure the expected email is there and get the url from the message.");
        URL resetUrl = getUrlFromEmail("Verification .* Web Site email change.*");

        goToHome();

        log("Validate that only the user who requested the change can use the link");
        goToURL(resetUrl, 30000);
        assertTextPresent("The current user is not the same user that initiated this request.", "Please log in with the account you used to make this email change request.");
        goToHome();

        log("Again impersonate user " + SELF_SERVICE_EMAIL_USER + " to validate the confirmation link.");
        impersonate(SELF_SERVICE_EMAIL_USER);

        goToURL(resetUrl, 30000);

        String tempStr = AFFIRM_CHANGE_MSG.replace("@1", SELF_SERVICE_EMAIL_USER);
        tempStr = tempStr.replace("@2", SELF_SERVICE_EMAIL_USER_CHANGED);
        assertTextPresent(tempStr);

        log("Stop impersonating user: " + SELF_SERVICE_EMAIL_USER);
        stopImpersonating();

        log("Go to dumpster and make sure the notification email was there.");
        assertNotNull(getEmailChangeMsgBody("Notification .* Web Site email has changed.*"));

        log("Validate that the old email address has been removed.");

        final Integer userId = new ApiPermissionsHelper(this).getUserId(SELF_SERVICE_EMAIL_USER);
        assertNull("Searching for user email '" + SELF_SERVICE_EMAIL_USER + "' did not throw the expected error.", userId);

        log("Impersonate using the new email address " + SELF_SERVICE_EMAIL_USER_CHANGED);
        goToHome();
        impersonate(SELF_SERVICE_EMAIL_USER_CHANGED);

        log("Validate that trying to use the link from the email message again will result in an error.");
        goToURL(resetUrl, 30000);
        assertTextPresent("This email address has already been verified.");
        goToHome();

        log("If you got here there were no errors using the new email account.");
        stopImpersonating();

        _userHelper.deleteUsers(false, SELF_SERVICE_EMAIL_USER, SELF_SERVICE_EMAIL_USER_CHANGED);
    }

    @Test
    public void testCustomFieldLogin()
    {
        String customFieldValue = "loginCredentials";
        setInitialPassword(NORMAL_USER, TEST_PASSWORD);

        goToSiteUsers();
        DataRegionTable table = new DataRegionTable("Users", getDriver());
        table.clickHeaderButtonAndWait("Change User Properties");

        log("Adding the custom field to core.Users");
        DomainDesignerPage domainDesignerPage = new DomainDesignerPage(getDriver());
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        domainFormPanel.addField("UID")
                .setType(FieldDefinition.ColumnType.String);
        domainDesignerPage.clickSave();

        log("Adding value to the custom field");
        navigateToUserDetails(NORMAL_USER);
        clickButton("Edit");
        setFormElement(Locator.name("quf_UID"), customFieldValue);
        clickButton("Submit");

        signOut();

        log("Sign in using custom field value");
        attemptSignIn(customFieldValue, TEST_PASSWORD);
        Assert.assertEquals("Logged in as wrong user", NORMAL_USER, getCurrentUser());
    }

    private void changeEmailAddress(String currentEmail, String newEmail, String password)
    {
        goToMyAccount();

        clickButton("Change Email");
        setFormElement(Locator.css("#requestedEmail"), newEmail);
        setFormElement(Locator.css("#requestedEmailConfirmation"), newEmail);
        clickButton("Submit");

        assertTextPresent(currentEmail);

        setFormElement(Locator.css("#password"), password);
        clickButton("Submit");
    }

    private String getEmailChangeMsgBody(String subjectRegex)
    {
        EmailRecordTable ert = new EmailRecordTable(getDriver());
        beginAt("/dumbster/begin.view?");
        EmailRecordTable.EmailMessage eMsg = ert.getMessageRegEx(subjectRegex);
        ert.clickMessage(eMsg);
        eMsg = ert.getMessageRegEx(subjectRegex);
        return eMsg.getBody();
    }

    private URL getUrlFromEmail(String subjectRegEx)
    {
        final String URL_PART = "changeEmail.view?userId=";
        String urlString = "";
        URL resetUrl;
        String[] msgLines;

        log("Find the first entry in the table with the given subject. This will be the most recent email sent.");

        String bdy = getEmailChangeMsgBody(subjectRegEx);

        if (bdy.contains(URL_PART))
        {
            msgLines = bdy.split("\n");

            log("Find the line that contains the new url to follow.");

            for(String line : msgLines)
            {
                if (line.contains(URL_PART))
                {
                    urlString = line;
                    break;
                }
            }
        }

        assertTrue("Could not find a url in the email to follow.", urlString.length() > 0);
        try
        {
            resetUrl = new URL(urlString);
        }
        catch (MalformedURLException mue)
        {
            throw new RuntimeException("The string returned: '" + urlString + "' resulted in a malformed url.", mue);
        }

        return resetUrl;
    }

    @Test
    public void testDeactivatedUser()
    {
        createUserWithPermissions(DEACTIVATED_USER, getProjectName(), "Editor");
        goToSiteUsers();
        DataRegionTable usersTable = new DataRegionTable("Users", this);
        int row = usersTable.getRowIndex("Email", DEACTIVATED_USER);
        String disabledUserId = usersTable.getDataAsText(row, "User Id");
        String normalUserId = usersTable.getDataAsText(usersTable.getRowIndex("Email", NORMAL_USER), "User Id");
        usersTable.checkCheckbox(row);
        clickButton("Deactivate");
        clickButton("Deactivate");
        assertTextNotPresent(DEACTIVATED_USER);

        log("Deactivated users shouldn't show up in issues 'Assign To' list");
        goToProjectHome();
        IssuesHelper issuesHelper = new IssuesHelper(this);
        issuesHelper.createNewIssuesList("issues", getContainerHelper());
        goToModule("Issues");
        issuesHelper.goToAdmin();
        issuesHelper.setIssueAssignmentList("Site: Users");
        clickButton("Save");
        clickAndWait(Locator.linkWithText("New Issue"));
        assertElementNotPresent(createAssignedToOptionLocator(disabledUserId));
        assertTextNotPresent(_userHelper.getDisplayNameForEmail(DEACTIVATED_USER));
        assertElementPresent(createAssignedToOptionLocator(normalUserId));
        assertTextPresent(_userHelper.getDisplayNameForEmail(NORMAL_USER));

        log("Reactivate user");
        goToSiteUsers();
        assertTextNotPresent(DEACTIVATED_USER);
        clickAndWait(Locator.linkWithText("include inactive users"));
        usersTable = new DataRegionTable("Users", this);
        row = usersTable.getRowIndex("Email", DEACTIVATED_USER);
        assertEquals(DEACTIVATED_USER + " should not be 'Active'", "false", usersTable.getDataAsText(row, "Active"));
        usersTable.checkCheckbox(row);
        clickButton("Reactivate");
        clickButton("Reactivate");
        usersTable = new DataRegionTable("Users", this);
        row = usersTable.getRowIndex("Email", DEACTIVATED_USER);
        assertEquals(DEACTIVATED_USER + " should be 'Active'", "true", usersTable.getDataAsText(row, "Active"));
    }

    private Locator createAssignedToOptionLocator(String username)
    {
        return Locator.xpath("//select[@name='assignedTo']/option[@value='" + username +  "']");
    }

    /**
     * Selects required user information fields and tests to see they are
     * enforced in the user info form.
     */
    @Test
    public void testRequiredFields()
    {
        try
        {
            ensureRequiredFieldsSet();

            _userHelper.createUserAndNotify(BLANK_USER);
            setInitialPassword(BLANK_USER, TEST_PASSWORD);

            DomainDesignerPage domainDesignerPage = goToSiteUsers().clickChangeUserProperties();
            DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
            for (String field : REQUIRED_FIELDS)
            {
                domainFormPanel.getField(field).setRequiredField(true);
            }
            domainDesignerPage.clickFinish();

            domainDesignerPage = goToSiteUsers().clickChangeUserProperties();
            domainFormPanel = domainDesignerPage.fieldsPanel();
            for (String field : REQUIRED_FIELDS)
            {
                DomainFieldRow domainFieldRow = domainFormPanel.getField(field);
                assertTrue("Field should be set to required: " + field, domainFieldRow.getRequiredField());
                domainFieldRow.setRequiredField(false);
            }
            domainDesignerPage.clickFinish();

            domainDesignerPage = goToSiteUsers().clickChangeUserProperties();
            domainFormPanel = domainDesignerPage.fieldsPanel();
            domainFormPanel.getField("FirstName").setRequiredField(true);
            domainDesignerPage.clickFinish();

            signOut();
            attemptSignIn(BLANK_USER, TEST_PASSWORD);
            waitForElement(Locator.name("quf_FirstName"));

            clickButton("Submit");
            assertTextPresent("This field is required");
            setFormElement(Locator.name("quf_FirstName"), "*");
            clickButton("Submit");
        }
        finally
        {
            // now sign out, and sign in as a user with sufficient privilege to un-set those required fields
            signOut();
            signIn();

            // go to Users page, mark 'required fields' as no longer required so other tests aren't affected
            DomainDesignerPage domainDesignerPage = goToSiteUsers().clickChangeUserProperties();
            DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
            for (String field : REQUIRED_FIELDS)
            {
                domainFormPanel.getField(field).setRequiredField(false);
            }
            domainDesignerPage.clickFinish();
        }
    }

    /**
     * Set required fields to prevent collateral failures caused by redirect during sign-in
     */
    private void ensureRequiredFieldsSet()
    {
        goToMyAccount();
        clickAndWait(Locator.lkButton("Edit"));
        for (String field : REQUIRED_FIELDS)
        {
            WebElement el = Locator.name("quf_" + field).waitForElement(new WebDriverWait(getDriver(), Duration.ofSeconds(5)));
            if (getFormElement(el).isEmpty())
                setFormElement(el, getDisplayName());
        }
        clickAndWait(Locator.lkButton("Submit"));
    }

    private void navigateToUserDetails(String userEmail)
    {
        DataRegionTable table = new DataRegionTable("Users", this);
        doAndWaitForPageToLoad(() -> table.detailsLink(table.getRowIndex("Email", userEmail)).click());
    }

    @Test
    public void testLongUserProperties()
    {
        goToSiteUsers();
        navigateToUserDetails(NORMAL_USER);
        clickButton("Edit");

        StringBuilder sb = new StringBuilder();
        final int maxFieldLength = 64;
        for (int i = 0; i < maxFieldLength + 1; i++)
            sb.append("X");
        String illegalLongProperty = sb.toString();

        log("Set illegal properties");
        setFormElement(Locator.name("quf_DisplayName"), illegalLongProperty);
        setFormElement(Locator.name("quf_FirstName"), illegalLongProperty);
        setFormElement(Locator.name("quf_LastName"), illegalLongProperty);
        setFormElement(Locator.name("quf_Phone"), illegalLongProperty);
        setFormElement(Locator.name("quf_Mobile"), illegalLongProperty);

        log("Check error messages");
        clickButton("Submit");
        WebElement errors = Locator.css(".labkey-error").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        String errorText = errors.getText();

        assertTrue("No error for 'Display Name'", errorText.contains(String.format("Value is too long for column 'DisplayName', a maximum length of %d is allowed.", maxFieldLength)));
        assertTrue("No error for 'First Name'", errorText.contains(String.format("Value is too long for column 'FirstName', a maximum length of %d is allowed.", maxFieldLength)));
        assertTrue("No error for 'Last Name'", errorText.contains(String.format("Value is too long for column 'LastName', a maximum length of %d is allowed.", maxFieldLength)));
        assertTrue("No error for 'Phone'", errorText.contains(String.format("Value is too long for column 'Phone', a maximum length of %d is allowed.", maxFieldLength)));
        assertTrue("No error for 'Mobile'", errorText.contains(String.format("Value is too long for column 'Mobile', a maximum length of %d is allowed.", maxFieldLength)));

        clickButton("Cancel");

        String userInfo = getText(DataRegionTable.Locators.form("SiteUsers"));
        assertFalse("Too-long property persists after cancel", userInfo.contains(illegalLongProperty.substring(0, 3)));
    }

    @Test
    public void testSpecialCharactersUserProperties()
    {
        goToSiteUsers();
        navigateToUserDetails(NORMAL_USER);
        clickButton("Edit");

        setFormElement(Locator.name("quf_FirstName"), TRICKY_CHARACTERS_FOR_PROJECT_NAMES);
        setFormElement(Locator.name("quf_LastName"), TRICKY_CHARACTERS_FOR_PROJECT_NAMES);

        clickButton("Submit");

        WebElement table = DataRegionTable.Locators.form("SiteUsers").findElement(this.getDriver());
        final int count = Locator.tag("td").withText(TRICKY_CHARACTERS_FOR_PROJECT_NAMES).findElements(table).size();
        assertEquals("Didn't find special characters for user first and last name", 2, count);
    }

    @Test
    public void testScriptInjectionUserProperties()
    {
        goToSiteUsers();
        navigateToUserDetails(NORMAL_USER);
        clickButton("Edit");

        setFormElement(Locator.name("quf_FirstName"), INJECT_CHARS_1);
        setFormElement(Locator.name("quf_LastName"), INJECT_CHARS_1);

        clickButton("Submit");

        WebElement table = DataRegionTable.Locators.form("SiteUsers").findElement(this.getDriver());
        final int count = Locator.tag("td").withText(INJECT_CHARS_1).findElements(table).size();
        assertEquals("Didn't find injection string for user first and last name", 2, count);
    }

    private static final String PROP_NAME1 = "Institution";
    private static final String PROP_NAME2 = "InstitutionId";

    @Test
    public void testCustomProperties()
    {
        DomainDesignerPage domainDesignerPage = goToSiteUsers().clickChangeUserProperties();
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        domainFormPanel.addField(PROP_NAME1).setType(FieldDefinition.ColumnType.String).setLabel(PROP_NAME1);
        domainFormPanel.addField(PROP_NAME2).setType(FieldDefinition.ColumnType.Integer).setLabel(PROP_NAME2);
        domainDesignerPage.clickFinish();

        assertTextPresent(PROP_NAME1, PROP_NAME2);

        navigateToUserDetails(NORMAL_USER);
        assertTextPresent(PROP_NAME1, PROP_NAME2);

        clickButton("Edit");
        assertTextPresent(PROP_NAME1, PROP_NAME2);
        clickButton("Cancel");
    }

    @Test
    public void testAddUserCSRF()
    {
        goToSiteUsers();
        clickButton("Add Users");
        setFormElement(Locator.name("newUsers"), "nocsrf@user.test");
        setFormElementJS(Locator.name(Connection.X_LABKEY_CSRF), "");

        clickButton("Add Users");
        assertElementPresent(Locators.labkeyErrorSubHeading.containing("This request has an invalid security context. You may have signed in or signed out of this session. Try again by using the 'back' and 'refresh' button in your browser."));
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
