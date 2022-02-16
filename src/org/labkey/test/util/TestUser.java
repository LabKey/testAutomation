package org.labkey.test.util;

import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.openqa.selenium.WebElement;

import java.util.Map;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_PAGE;
import static org.labkey.test.util.TestLogger.log;

public class TestUser
{
    private final WebDriverWrapper _test;
    private final String _email;
    private CreateUserResponse _createUserResponse;
    private String _password;
    private final APIUserHelper _apiUserHelper;
    private Connection _impersionationConnection;

    public TestUser(WebDriverWrapper test, String email)
    {
        _test = test;
        _email = email;
        _apiUserHelper = new APIUserHelper(_test);
        create();
    }

    /**
     * Creates the user immediately via the API, and stores the createUserResponse to hang on to data like
     *
     * @return
     */
    private TestUser create()
    {
        _createUserResponse = _apiUserHelper.createUser(_email);
        return this;
    }

    public void deleteUser()
    {
        _apiUserHelper.deleteUsers(false, _email);
    }

    public Long getUserId()
    {
        return (Long) _createUserResponse.getUserId();
    }

    public String getEmail()
    {
        return _createUserResponse.getEmail();
    }

    /**
     * Uses the UI to reset the randomly-generated password a user gets when created, by following the reset link they'll
     * receive in mail. Also stores the provided password in the bean for later use.
     *  Note: this can only be done once for a given user account
     * @param password  The password
     * @return  The current instance
     */
    public TestUser setPassword(String password)
    {
        if (_password == null)  // if null, this is the initial password-
        {
            //... borrowed from LKSW's setInitialPassword - in the future, do via API
            _test.beginAt(WebTestHelper.buildURL("security", "showRegistrationEmail", Map.of("email", _email)));
            // Get setPassword URL from notification email.
            WebElement resetLink = Locator.linkWithHref("setPassword.view").findElement(_test.getDriver());

            _test.clickAndWait(resetLink, WAIT_FOR_PAGE);

            _test.setFormElement(Locator.id("password"), password);
            _test.setFormElement(Locator.id("password2"), password);

            _test.clickButton("Set Password");
        }
        else
        {
            log("Warning: user " +_email+ "has already selected a password.");
        }
        _password = password;
        return this;
    }

    public String getPassword()
    {
        return _password;
    }

    public TestUser addPermission(String role, String containerContext)
    {
        // apply permission setting right away
        new ApiPermissionsHelper(_test).addMemberToRole(getEmail(), role, PermissionsHelper.MemberType.user, containerContext);

        return this;
    }

    public void impersonate() throws Exception
    {
        if (_impersionationConnection != null)
            log("Already impersonating.");

        log("Begin impersonating as user: " + getEmail());
        _impersionationConnection = _test.createDefaultConnection();
        _impersionationConnection.impersonate(getEmail());
    }

    public void stopImpersonating() throws Exception
    {
        log("Stop impersonating uer " + getEmail());
        _impersionationConnection.stopImpersonate();
        _impersionationConnection = null;
    }
}
