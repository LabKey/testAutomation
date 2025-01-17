package org.labkey.test.tests;

import org.apache.hc.core5.http.HttpStatus;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PermissionsHelper;
import org.openqa.selenium.Cookie;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.fail;
import static org.labkey.test.WebTestHelper.getCookies;

@Category({Daily.class})
public class InvalidateSessionTest extends BaseWebDriverTest
{
    private static final String USER = "pwd_change_user@invalidatesessiontest.test";

    @BeforeClass
    public static void setupProject()
    {
        InvalidateSessionTest init = getCurrentTest();
        init.doSetup();
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return null;
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _userHelper.deleteUsers(afterTest, USER);
    }

    private void doSetup()
    {
        _userHelper.createUser(USER);

        ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);
        permissionsHelper.addMemberToRole(USER, "Folder Administrator", PermissionsHelper.MemberType.user);
        setInitialPassword(USER);
    }

    /*
        Regression coverage for Secure Issue 51523: Invalidate sessions on password change
     */
    @Test
    public void testSessionInvalidatesAfterPasswordChange() throws IOException
    {
        signOut();
        signIn(USER);
        Connection cn = createDefaultConnection();
        SelectRowsResponse response;
        SelectRowsCommand selectCmd = new SelectRowsCommand("auditLog", "UserAuditEvent");
        try
        {
            response = selectCmd.execute(cn, "Home");
            Assert.assertEquals("Did not establish the database connection before the password change", 200,
                    response.getStatusCode());
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }

        log("Changing the user password");
        String newPassword = PasswordUtil.getPassword() + "&*&*";
        goToMyAccount().clickChangePassword()
                .setOldPassword(PasswordUtil.getPassword())
                .setPassword1(newPassword)
                .setPassword2(newPassword)
                .clickSubmit();

        log("Verifying the session is invalid");
        try
        {
            selectCmd.execute(cn, getProjectName());
            fail("Test should have thrown command exception");
        }
        catch (CommandException e)
        {
            Assert.assertEquals("Session should be expired", 401, e.getStatusCode());
        }
        signOut();
    }

    /*
        Regression coverage for Secure Issue 31493: Test for session and cookie persistence through login and logout
     */
    @Test
    public void testCookieAndSessionFromLogout() throws IOException
    {
        log("Capture the cookie after login");
        Cookie beforeCookie = getCookies(getCurrentUser()).get(Connection.JSESSIONID);

        log("Establish the connection");
        Connection cn = createDefaultConnection();
        SelectRowsResponse response;
        SelectRowsCommand selectCmd = new SelectRowsCommand("auditLog", "UserAuditEvent");
        try
        {
            response = selectCmd.execute(cn, getProjectName());
            Assert.assertEquals("Did not establish the database connection before the password change", 200,
                    response.getStatusCode());
        }
        catch (CommandException e)
        {
            throw new RuntimeException(e);
        }

        log("Sign out");
        signOut();

        log("Verify previously established connection be used after log out");
        try
        {
            selectCmd.execute(cn, getProjectName());
            fail("Should fail when logged out");
        }
        catch (CommandException e)
        {
            Assert.assertEquals("Command execution should fail when logged out", HttpStatus.SC_UNAUTHORIZED, e.getStatusCode());
        }

        log("Capture the cookie after logout");
        Cookie afterCookie = getCookies(getCurrentUser()).get(Connection.JSESSIONID);
        Assert.assertFalse("Before and after log out cookie should be different", beforeCookie.equals(afterCookie));
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
