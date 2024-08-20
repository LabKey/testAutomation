package org.labkey.test.util;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.test.WebDriverWrapper;

import java.io.IOException;

import static org.labkey.test.util.TestLogger.log;

public class TestUser
{
    private WebDriverWrapper _test;
    private final String _email;
    private CreateUserResponse _createUserResponse;
    private String _password;
    private APIUserHelper _apiUserHelper;
    private Connection _impersonationConnection;

    public TestUser(String email)
    {
        _email = email;
    }

    /**
     * Creates the user immediately via the API, and stores the createUserResponse to hang on to data like
     * the user's userId
     */
    public TestUser create(WebDriverWrapper test)
    {
        _test = test;
        _apiUserHelper = new APIUserHelper(_test);
        _createUserResponse = _apiUserHelper.createUser(_email);
        _impersonationConnection = null;
        _password = null;
        return this;
    }

    public void deleteUser()
    {
        getApiUserHelper().deleteUsers(false, _email);
    }

    public Integer getUserId()
    {
        return getCreateUserResponse().getUserId();
    }

    public String getEmail()
    {
        return _email;
    }

    public String getUserDisplayName()
    {
        return getApiUserHelper().getDisplayNameForEmail(getEmail());
    }

    /**
     * Set the initial password for a newly created user. Uses the reset link emailed to the user.
     * Stores the password in the bean for later use.
     *  Note: this can only be done once for a given user account
     * @return The current instance
     * @see #getPassword()
     */
    public TestUser setInitialPassword()
    {
        if (_password == null)  // if null, this is the initial password - we can use the UI to set it now
        {
            _password = _apiUserHelper.setInitialPassword(_email);
        }
        else
        {
            throw new IllegalStateException("User " +_email+ "has already selected a password.");
        }
        return this;
    }

    /**
     * Uses the UI to reset the randomly-generated password a user gets when created, by following the reset link they'll
     * receive in mail. Also stores the provided password in the bean for later use.
     *  Note: this can only be done once for a given user account
     * @param password  The password (ignored)
     * @return  The current instance
     * @deprecated There is no need to specify a particular password for most test scenarios.
     *  Such scenarios should not use this class.
     * @see #setInitialPassword()
     */
    @Deprecated (since = "24.6")
    public TestUser setPassword(String password)
    {
        return setInitialPassword();
    }

    public String getPassword()
    {
        if (_password == null)
        {
            throw new IllegalStateException("Password has not been set for user: " + _email);
        }
        return _password;
    }

    public TestUser addPermission(String role, String containerPath)
    {
        new ApiPermissionsHelper(getWrapper()).addMemberToRole(getEmail(), role, PermissionsHelper.MemberType.user, containerPath);

        return this;
    }

    public void impersonate() throws IOException, CommandException
    {
        impersonate(false);
    }

    /**
     * Impersonate the given test user.
     * @param refresh If the test is already within the App and isn't reloading the page via navigation, the LABKEY.user object will be out of sync with the newly impersonated user so a page refresh will help.
     */
    public void impersonate(boolean refresh) throws IOException, CommandException
    {
        log("Begin impersonating as user: " + getEmail());
        _impersonationConnection = getWrapper().createDefaultConnection();
        _impersonationConnection.impersonate(getEmail());

        if (refresh)
            getWrapper().refresh();
    }

    public void stopImpersonating() throws IOException, CommandException
    {
        stopImpersonating(false);
    }

    /**
     * Stop impersonating the test user.
     * @param refresh If the test is already within the App and isn't reloading the page via navigation, the LABKEY.user object will be out of sync with the newly impersonated user so a page refresh will help
     */
    public void stopImpersonating(boolean refresh) throws IOException, CommandException
    {
        if (_impersonationConnection == null)
        {
            throw new IllegalStateException("User " + _email + " has no impersonating connection. Impersonate using 'TestUser.impersonate'");
        }

        log("Stop impersonating uer " + getEmail());
        _impersonationConnection.stopImpersonating();
        _impersonationConnection = null;

        if (refresh)
            getWrapper().refresh();
    }

    private CreateUserResponse getCreateUserResponse()
    {
        if (_createUserResponse == null)
        {
            throw new IllegalStateException("User" + _email + " has not yet been created");
        }
        return _createUserResponse;
    }

    private APIUserHelper getApiUserHelper()
    {
        if (_apiUserHelper == null)
        {
            throw new IllegalStateException("create() must be called on this instance before attempting to reference _apiUserHelper");
        }
        return _apiUserHelper;
    }

    /**
     * checks _test for null and if so, throws
     */
    private WebDriverWrapper getWrapper()
    {
        if (_test == null)
        {
            throw new IllegalStateException("create() must be called on this instance before attempting to reference _test");
        }
        return _test;
    }
}
