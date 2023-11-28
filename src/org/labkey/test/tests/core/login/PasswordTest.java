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

package org.labkey.test.tests.core.login;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.BVT;
import org.labkey.test.components.core.login.SetPasswordForm;
import org.labkey.test.pages.core.login.DatabaseAuthConfigureDialog;
import org.labkey.test.pages.core.login.LoginConfigurePage;
import org.labkey.test.params.login.DatabaseAuthenticationProvider;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.core.login.DbLoginUtils;
import org.labkey.test.util.core.login.DbLoginUtils.DbLoginProperties;
import org.labkey.test.util.core.login.DbLoginUtils.PasswordExpiration;
import org.labkey.test.util.core.login.DbLoginUtils.PasswordStrength;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.components.core.login.SetPasswordForm.GOOD_PASSWORD;
import static org.labkey.test.components.core.login.SetPasswordForm.SHORT_PASSWORD;
import static org.labkey.test.components.core.login.SetPasswordForm.SIMPLE_PASSWORD;
import static org.labkey.test.components.core.login.SetPasswordForm.STRONG_PASSWORD;

@Category(BVT.class)
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class PasswordTest extends BaseWebDriverTest
{
    private static final String USER = "user_passwordtest@password.test";

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
        _userHelper.createUser(USER);
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

        DbLoginUtils.setDbLoginConfig(connection, PasswordStrength.Weak, PasswordExpiration.SixMonths);
        DatabaseAuthConfigureDialog configDialog = configurePage
                .getPrimaryConfigurationRow(dbAuth.getProviderDescription())
                .clickEdit(dbAuth);

        DbLoginProperties dbLoginConfig = configDialog.getDbLoginConfig();
        assertEquals("Login config",
                new DbLoginProperties(PasswordStrength.Weak, PasswordExpiration.SixMonths),
                dbLoginConfig);
    }

    @Test
    public void testStrongPassword()
    {
        DbLoginUtils.setDbLoginConfig(createDefaultConnection(),
                PasswordStrength.Strong,
                PasswordExpiration.Never);

        SetPasswordForm setPasswordForm = SetPasswordForm.goToInitialPasswordForUser(this, USER);
        log("Verify strength gauge for 'SetPasswordAction'");
        setPasswordForm.assertPasswordStrengthGauge();

        setPasswordForm = setPasswordForm.setNewPassword(SIMPLE_PASSWORD)
                .clickSubmitExpectingError("Your password is not complex enough."); // fail, too simple
        setPasswordForm = setPasswordForm.setNewPassword(SHORT_PASSWORD)
                .clickSubmitExpectingError("Your password is not complex enough."); // fail, too short
        setPasswordForm = setPasswordForm.setNewPassword(GOOD_PASSWORD)
                .clickSubmitExpectingError("Your password is not complex enough."); // fail, not complex enough

        setPasswordForm.setNewPassword(STRONG_PASSWORD).clickSubmit();
        assertSignedInNotImpersonating();
        //success
        impersonate(USER);

        SetPasswordForm changePasswordForm = goToChangePassword();
        log("Verify strength gauge for 'ChangePasswordAction'");
        changePasswordForm.assertPasswordStrengthGauge();

        changePasswordForm = changePasswordForm
                .setOldPassword(STRONG_PASSWORD)
                .setNewPassword(SIMPLE_PASSWORD)// fail, too simple
                .clickSubmitExpectingError("Your password is not complex enough.");
        changePasswordForm = changePasswordForm
                .setOldPassword(STRONG_PASSWORD)
                .setNewPassword(SHORT_PASSWORD) // fail, too short
                .clickSubmitExpectingError("Your password is not complex enough.");
        changePasswordForm = changePasswordForm
                .setOldPassword(STRONG_PASSWORD)
                .setNewPassword(GOOD_PASSWORD) // fail, not complex enough
                .clickSubmitExpectingError("Your password is not complex enough.");

        String currentPassword = STRONG_PASSWORD + 0;
        changePasswordForm.setOldPassword(STRONG_PASSWORD)
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

        String currentPassword = STRONG_PASSWORD + 0;

        setInitialPassword(USER, currentPassword);
        impersonate(USER);

        int i = 1;
        for (; i <= 10; i++)
        {
            changePassword(currentPassword, STRONG_PASSWORD + i);
            currentPassword = STRONG_PASSWORD + i;
            assertTextNotPresent("Choose a new password.");
        }
        // fail, used 9 passwords ago.
        goToChangePassword()
                .setOldPassword(currentPassword)
                .setNewPassword(STRONG_PASSWORD + 1)
                .clickSubmitExpectingError("Your password must not match a recently used password.");
        changePassword(currentPassword, STRONG_PASSWORD + 0);
        assertTextNotPresent("Choose a new password.");

        stopImpersonating();
    }

    @Test
    public void testPasswordReset()
    {
        DbLoginUtils.setDbLoginConfig(createDefaultConnection(),
                PasswordStrength.Weak,
                PasswordExpiration.Never);

        //get user a password
        String username = USER;
        String password = STRONG_PASSWORD;

        password = adminPasswordResetTest(username, password+"adminReset");

        String resetUrl = userForgotPasswordWorkflowTest(username, password);

        ensureSignedOut();

        beginAt(resetUrl);

        attemptSetInvalidPassword("fooba", "fooba", "Your password must be at least six characters and cannot contain spaces.");
        attemptSetInvalidPassword("foobar", "foobar2", "Your password entries didn't match.");

        resetPassword(resetUrl, USER, STRONG_PASSWORD);

        ensureSignedInAsPrimaryTestUser();
    }

    @Test
    public void testPasswordParameter()
    {
        setInitialPassword(USER, SIMPLE_PASSWORD);

        // 31000: fail login actions if parameters present on URL
        SimplePostCommand command = new SimplePostCommand("login", "loginAPI");

        Map<String, Object> params = new HashMap<>();
        params.put("email", USER);
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
        goToSiteUsers()
                .getUsersTable()
                .setFilter("Email", "Equals", username);
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

    protected String setInitialPassword(String user, String password)
    {
        SetPasswordForm.goToInitialPasswordForUser(this, user)
                .setNewPassword(password)
                .clickSubmit();

        return password;
    }

    @LogMethod (quiet = true)
    protected void changePassword(String oldPassword, @LoggedParam String password)
    {
        goToChangePassword()
                .setOldPassword(oldPassword)
                .setNewPassword(password)
                .clickSubmit();
    }

    private SetPasswordForm goToChangePassword()
    {
        if (PasswordUtil.getUsername().equals(getCurrentUser()))
            throw new IllegalArgumentException("Don't change the primary site admin user's password");

        goToMyAccount();
        clickButton("Change Password");
        return new SetPasswordForm(getDriver());
    }

}
