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
import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PermissionsHelper;
import org.openqa.selenium.Cookie;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.fail;
import static org.labkey.test.util.PermissionsHelper.FOLDER_ADMIN_ROLE;

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
        CreateUserResponse response = _userHelper.createUser(USER);
        ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);
        permissionsHelper.addMemberToRole(USER, FOLDER_ADMIN_ROLE, PermissionsHelper.MemberType.user);
        setInitialPassword(response.getUserId());
    }

    /*
        Regression coverage for Secure Issue 51523: Invalidate sessions on password change
     */
    @Test
    public void testSessionInvalidatesAfterPasswordChange() throws IOException, CommandException
    {
        signOut();
        signIn(USER);
        Connection cn = createDefaultConnection();
        SelectRowsResponse response;
        SelectRowsCommand selectCmd = new SelectRowsCommand("auditLog", "UserAuditEvent");
        response = selectCmd.execute(cn, "Home");
        Assert.assertEquals("Did not establish the database connection before the password change", 200,
                response.getStatusCode());

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
    public void testCookieAndSessionFromLogout() throws IOException, CommandException
    {
        log("Capture the cookie after login");
        Set<Cookie> beforeCookie = getDriver().manage().getCookies();

        log("Establish the connection");
        Connection cn = createDefaultConnection();
        SelectRowsResponse response;
        SelectRowsCommand selectCmd = new SelectRowsCommand("auditLog", "UserAuditEvent");
        response = selectCmd.execute(cn, getProjectName());
        Assert.assertEquals("Did not establish the database connection before the password change", 200,
                response.getStatusCode());

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
        Set<Cookie> afterCookie = getDriver().manage().getCookies();
        Assert.assertNotEquals("Before and after log out cookie should be different", getJSessionIdValue(beforeCookie), getJSessionIdValue(afterCookie));
    }

    private String getJSessionIdValue(Set<Cookie> cookies)
    {
        String jsession = "";
        for (Cookie c : cookies)
        {
            if(c.getName().equals(Connection.JSESSIONID))
                jsession = c.getValue();
        }
        return jsession;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
