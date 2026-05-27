/*
 * Copyright (c) 2023-2026 LabKey Corporation
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

package org.labkey.test.tests.core.login;

import org.apache.hc.core5.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.remoteapi.security.EnsureLoginCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.BVT;
import org.labkey.test.components.core.login.SetPasswordForm;
import org.labkey.test.pages.core.login.DatabaseAuthConfigureDialog;
import org.labkey.test.pages.core.login.LoginConfigurePage;
import org.labkey.test.params.login.DatabaseAuthenticationProvider;
import org.labkey.test.util.APIUserHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.core.login.DbLoginUtils;
import org.labkey.test.util.core.login.DbLoginUtils.DbLoginProperties;
import org.labkey.test.util.core.login.DbLoginUtils.PasswordExpiration;
import org.labkey.test.util.core.login.DbLoginUtils.PasswordStrength;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.components.core.login.SetPasswordForm.SHORT_PASSWORD;
import static org.labkey.test.components.core.login.SetPasswordForm.VERY_STRONG_PASSWORD;
import static org.labkey.test.components.core.login.SetPasswordForm.VERY_WEAK_PASSWORD;
import static org.labkey.test.components.core.login.SetPasswordForm.WEAK_PASSWORD;

@Category(BVT.class)
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class PasswordTest extends BaseWebDriverTest
{
    private static final String USER = "user_passwordtest@password.test";

    private int _userId;

    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.singletonList("core");
    }

    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(false, USER);

        Connection cn = createDefaultConnection();
        DbLoginUtils.resetDbLoginConfig(cn);
    }

    @Before
    public void resetUser()
    {
        _userHelper.deleteUsers(false, USER);
        _userId = _userHelper.createUser(USER).getUserId();
    }

    @Test
    public void testLoginConfigurationForm()
    {
        LoginConfigurePage configurePage = LoginConfigurePage.beginAt(this);
        DatabaseAuthenticationProvider dbAuth = new DatabaseAuthenticationProvider();
        configurePage
                .getPrimaryConfigurationRow(dbAuth.getProviderDescription())
                .clickEdit(dbAuth)
                .setDbLoginConfig(PasswordStrength.Good, PasswordExpiration.OneYear);

        Connection connection = createDefaultConnection();
        assertEquals("Login config", new DbLoginProperties(PasswordStrength.Good, PasswordExpiration.OneYear),
                DbLoginUtils.getDbLoginConfig(connection));

        DbLoginUtils.setDbLoginConfig(connection, PasswordStrength.Strong, PasswordExpiration.SixMonths);
        DatabaseAuthConfigureDialog configDialog = configurePage
                .getPrimaryConfigurationRow(dbAuth.getProviderDescription())
                .clickEdit(dbAuth);

        DbLoginProperties dbLoginConfig = configDialog.getDbLoginConfig();
        assertEquals("Login config",
                new DbLoginProperties(PasswordStrength.Strong, PasswordExpiration.SixMonths),
                dbLoginConfig);
    }

    @Test
    public void testStrongPassword()
    {
        String displayName = "D1spl&yN&m3";
        _userHelper.setDisplayName(USER, displayName);

        DbLoginUtils.setDbLoginConfig(createDefaultConnection(),
                PasswordStrength.Strong,
                PasswordExpiration.Never);

        SetPasswordForm setPasswordForm = SetPasswordForm.goToInitialPasswordForUser(this, _userId);
        log("Verify strength gauge for 'SetPasswordAction'");
        setPasswordForm.verifyPasswordStrengthGauge(USER);

        setPasswordForm = setPasswordForm.setNewPassword(VERY_WEAK_PASSWORD)
                .clickSubmitExpectingError("Your password is not complex enough."); // fail, too simple
        setPasswordForm = setPasswordForm.setNewPassword(SHORT_PASSWORD)
                .clickSubmitExpectingError("Your password is not complex enough."); // fail, too short
        setPasswordForm = setPasswordForm.setNewPassword(WEAK_PASSWORD)
                .clickSubmitExpectingError("Your password is not complex enough."); // fail, not complex enough

        setPasswordForm.setNewPassword(VERY_STRONG_PASSWORD).clickSubmit();
        assertSignedInNotImpersonating();
        impersonate(USER);

        SetPasswordForm changePasswordForm = goToMyAccount().clickChangePassword();
        log("Verify strength gauge for 'ChangePasswordAction'");
        changePasswordForm.verifyPasswordStrengthGauge(USER, displayName);

        changePasswordForm = changePasswordForm
                .setOldPassword(VERY_STRONG_PASSWORD)
                .setNewPassword(VERY_WEAK_PASSWORD)// fail, too simple
                .clickSubmitExpectingError("Your password is not complex enough.");
        changePasswordForm = changePasswordForm
                .setOldPassword(VERY_STRONG_PASSWORD)
                .setNewPassword(SHORT_PASSWORD) // fail, too short
                .clickSubmitExpectingError("Your password is not complex enough.");
        changePasswordForm = changePasswordForm
                .setOldPassword(VERY_STRONG_PASSWORD)
                .setNewPassword(WEAK_PASSWORD) // fail, not complex enough
                .clickSubmitExpectingError("Your password is not complex enough.");

        String currentPassword = VERY_STRONG_PASSWORD + 0;
        changePasswordForm.setOldPassword(VERY_STRONG_PASSWORD)
                .setNewPassword(currentPassword)
                .clickSubmit();
        assertTextNotPresent("Choose a new password.");
        assertEquals("Signed in as", USER, getCurrentUser());
    }

    @Test
    public void testReusePassword()
    {
        DbLoginUtils.setDbLoginConfig(createDefaultConnection(),
                PasswordStrength.Strong,
                PasswordExpiration.Never);

        String currentPassword = VERY_STRONG_PASSWORD + 0;

        setInitialPassword(_userId, currentPassword);
        impersonate(USER);

        int i = 1;
        for (; i <= 10; i++)
        {
            changePassword(currentPassword, VERY_STRONG_PASSWORD + i);
            currentPassword = VERY_STRONG_PASSWORD + i;
            assertTextNotPresent("Choose a new password.");
        }
        // fail, used 9 passwords ago.
        goToMyAccount().clickChangePassword()
                .setOldPassword(currentPassword)
                .setNewPassword(VERY_STRONG_PASSWORD + 1)
                .clickSubmitExpectingError("Your password must not match a recently used password.");
        changePassword(currentPassword, VERY_STRONG_PASSWORD + 0);
        assertTextNotPresent("Choose a new password.");

        stopImpersonating();
    }

    @Test
    public void testPasswordReset()
    {
        DbLoginUtils.setDbLoginConfig(createDefaultConnection(),
                PasswordStrength.Good,
                PasswordExpiration.Never);

        //get user a password
        String username = USER;
        String password = VERY_STRONG_PASSWORD;

        password = adminPasswordResetTest(username, password+"adminReset");

        String resetUrl = userForgotPasswordWorkflowTest(username, password);
        beginAt(resetUrl);

        attemptSetInvalidPassword("fooba", "fooba", "Your password must be at least eight characters and cannot contain spaces.");
        attemptSetInvalidPassword("foobar", "foobar2", "Your password entries didn't match.");

        resetPassword(resetUrl, USER, VERY_STRONG_PASSWORD);

        ensureSignedInAsPrimaryTestUser();
    }

    @Test
    public void testPasswordParameter()
    {
        setInitialPassword(_userId, WEAK_PASSWORD);

        // 31000: fail login actions if parameters present on URL
        SimplePostCommand command = new SimplePostCommand("login", "loginAPI");

        Map<String, Object> params = new HashMap<>();
        params.put("email", USER);
        params.put("password", VERY_STRONG_PASSWORD);
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
            if (HttpStatus.SC_BAD_REQUEST == e.getStatusCode())
                rejectedProperly = true;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to connect to login-loginAPI.api action.", e);
        }

        assertTrue("Expected email/password in URL to be rejected.", rejectedProperly);
    }

    @Test
    public void testChooseNewPasswordMessages() throws IOException
    {
        // Test bad API key
        signOut();
        signInShouldFailUiAndApi("apikey", "abc123", "The API key you provided is invalid.");

        // Hold an admin API connection open, allowing us to reset the config without the browser session interfering
        Connection adminConnection = WebTestHelper.getRemoteApiConnection();
        DbLoginProperties savedProperties = DbLoginUtils.getDbLoginConfig(adminConnection);

        try
        {
            DbLoginUtils.setDbLoginConfig(adminConnection,
                PasswordStrength.Good,
                PasswordExpiration.Never
            );

            // Set a weak password
            String resetUrl = userInitiatePasswordReset(USER);
            beginAt(resetUrl);
            new SetPasswordForm(getDriver())
                .setNewPassword(WEAK_PASSWORD)
                .clickSubmit();

            // Test bogus password
            signOut();
            signInShouldFailUiAndApi(USER, "bogus", "The email address and password you entered did not match any accounts on file.");

            // Test deactivated user
            APIUserHelper helper = new APIUserHelper(() -> adminConnection);
            helper.deactivateUsers(_userId);
            signInShouldFailUiAndApi(USER, WEAK_PASSWORD, "Your account has been deactivated.");
            helper.activateUsers(_userId);

            // Change the configuration and test password that no longer meets complexity requirements
            DbLoginUtils.setDbLoginConfig(adminConnection,
                PasswordStrength.Strong,
                PasswordExpiration.Never
            );
            signInShouldFailUiAndApi(USER, WEAK_PASSWORD, "Your password does not meet the complexity requirements; please choose a new password.");
            String strongPassword = VERY_STRONG_PASSWORD + "!";
            changeInvalidPassword(WEAK_PASSWORD, strongPassword);

            // Change the configuration and test expired password
            DbLoginUtils.setDbLoginConfig(adminConnection,
                PasswordStrength.Strong,
                PasswordExpiration.FiveSeconds
            );
            // Wait six seconds for expiration
            sleep(6000);
            signInShouldFailUiAndApi(USER, strongPassword, "Your password has expired; please choose a new password.");
            changeInvalidPassword(strongPassword, VERY_STRONG_PASSWORD + "@");
        }
        finally
        {
            DbLoginUtils.setDbLoginConfig(adminConnection, savedProperties);
        }
    }

    // Attempt to sign in via UI and API, expecting both to fail with the specified message
    private void signInShouldFailUiAndApi(String email, String password, String expectedMessage) throws IOException
    {
        signInShouldFail(email, password, expectedMessage);
        Connection userConnection = new Connection(WebTestHelper.getBaseURL(), email, password);
        EnsureLoginCommand ensureLoginCommand = new EnsureLoginCommand();
        try
        {
            ensureLoginCommand.execute(userConnection, "/");
            fail("Expected execute() to throw an exception.");
        }
        catch (CommandException e)
        {
            assertEquals(HttpStatus.SC_UNAUTHORIZED, e.getStatusCode());
            assertEquals(expectedMessage, e.getMessage());
        }
    }

    @LogMethod
    private void resetPassword(String password)
    {
        signOut();
        String resetUrl = userInitiatePasswordReset(USER);
        beginAt(resetUrl);
        new SetPasswordForm(getDriver())
            .setNewPassword(password)
            .clickSubmit();
    }

    @LogMethod
    private void changeInvalidPassword(String oldPassword, String newPassword)
    {
        new SetPasswordForm(getDriver())
            .setOldPassword(oldPassword)
            .setNewPassword(newPassword)
            .clickSubmit();
        signOut();
    }

    @LogMethod
    protected void attemptSetInvalidPassword(String password1, String password2, String error)
    {
        new SetPasswordForm(getDriver())
            .setPassword1(password1)
            .setPassword2(password2)
            .clickSubmitExpectingError(error);
    }

    /**
     * preconditions: there exists user username with password
     * postcondtions:  user can reset password at return value, not signed in
     *
     * @param username  user's username
     * @param password user's password
     * @return URL to use to reset user password
     */
    // Issue 3876
    @LogMethod
    private String userForgotPasswordWorkflowTest(String username, String password)
    {
        String resetUrl = userInitiatePasswordReset(username);
        signOut();

        //attempt sign in with old password - should succeed
        signIn(username, password);
        signOut();

        return resetUrl;
    }

    @LogMethod
    public String userInitiatePasswordReset(String username)
    {
        signOut();
        goToHome();
        clickAndWait(Locator.linkWithText("Sign In"));
        clickAndWait(Locator.linkContainingText("Forgot password"));
        setFormElement(Locator.id("email"), username);
        clickButtonContainingText("Reset", 0);

        // Need to sign in as admin to see the email
        signIn();
        return getPasswordResetUrl(_userId);
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
        goToSiteUsers()
                .getUsersTable()
                .setFilter("Email", "Equals", username);
        clickAndWait(Locator.linkContainingText(_userHelper.getDisplayNameForEmail(username)));
        int userId = Integer.valueOf(getUrlParam("userId"));
        clickButton("Reset Password");
        assertTextPresent("You are about to clear the user's current password");
        clickAndWait(Locator.lkButton("OK"));

        String url = getPasswordResetUrl(userId);

        //make sure user can't log in with current password
        signOut();
        signInShouldFail(username, password, wrongPasswordEntered);

        resetPassword(url, username, newPassword);

        signOut();

        //attempt to log in with old password (should fail)
        signInShouldFail(username, password, wrongPasswordEntered);

        return newPassword;
    }

    protected String setInitialPassword(int userId, String password)
    {
        SetPasswordForm.goToInitialPasswordForUser(this, userId)
                .setNewPassword(password)
                .clickSubmit();

        return password;
    }

    @LogMethod (quiet = true)
    protected void changePassword(String oldPassword, @LoggedParam String password)
    {
        goToMyAccount().clickChangePassword()
                .setOldPassword(oldPassword)
                .setNewPassword(password)
                .clickSubmit();
    }
}
