package org.labkey.test.util;

import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.openqa.selenium.WebElement;

import java.util.Map;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_PAGE;

public class TestUser
{
    private final WebDriverWrapper _test;
    private final String _email;
    private CreateUserResponse _createUserResponse;
    private String _password;
    private Map<String, String> _permissions;

    public TestUser(WebDriverWrapper test, String email)
    {
        _test = test;
        _email = email;
    }

    /**
     * If this instance has not been used to create a user on the server, will no-op
     * @return
     */
    public TestUser createIfNeeded()
    {
        var apiUserHelper = new APIUserHelper(_test);
        if (apiUserHelper.getUserId(_email) == null)    // or alternately, could check _createUserResponse for null
        {
            _createUserResponse = apiUserHelper.createUser(_email);
            if (_password != null)
            {
                // reset the initial password here, somehow
                // note that the _createUserResponse has a link to the password reset URL containing a hash
                // that the login-setPassword.view action might accept...

                //... or we could borrow this from LKSW's setInitialPassword
                _test.beginAt(WebTestHelper.buildURL("security", "showRegistrationEmail", Map.of("email", _email)));
                // Get setPassword URL from notification email.
                WebElement resetLink = Locator.linkWithHref("setPassword.view").findElement(_test.getDriver());

                _test.clickAndWait(resetLink, WAIT_FOR_PAGE);

                _test.setFormElement(Locator.id("password"), _password);
                _test.setFormElement(Locator.id("password2"), _password);

                _test.clickButton("Set Password");
            }
            if (_permissions != null)
            {
                for (String key : _permissions.keySet())
                {
                    new ApiPermissionsHelper(_test).addMemberToRole(getEmail(), key, PermissionsHelper.MemberType.user, _permissions.get(key));
                }
            }
        }
        return this;
    }

    public boolean isCreated()
    {
        return _createUserResponse != null;
    }

    public void deleteUser()
    {
        if (_createUserResponse != null)
        {
            new APIUserHelper(_test).deleteUser(_email);
        }
    }

    public Long getUserId()
    {
        if (_createUserResponse == null)
            createIfNeeded();
        return (Long) _createUserResponse.getUserId();
    }

    public String getEmail()
    {
        return _email;
    }

    public TestUser setPassword(String password)
    {
        _password = password;
        return this;
    }

    public String getPassword()
    {
        return _password;
    }

    public TestUser addPermission(String role, String containerContext)
    {
        if (_permissions == null)
            _permissions = new CaseInsensitiveHashMap();
        _permissions.put(role, containerContext);
        return this;
    }
}
