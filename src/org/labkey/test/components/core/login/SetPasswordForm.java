package org.labkey.test.components.core.login;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.PasswordUtil;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.labkey.test.components.html.Input.Input;

/**
 * core/src/org/labkey/core/login/setPassword.jsp
 */
public class SetPasswordForm extends WebDriverComponent<SetPasswordForm.ElementCache>
{
    public static final String SHORT_PASSWORD = "4asdfg!"; // Only 7 characters long. 3 character types.
    public static final String SIMPLE_PASSWORD = "3asdfghi"; // Only two character types. 8 characters long.
    public static final String GOOD_PASSWORD = "Yekbal1!"; // 8 characters long. 3+ character types.
    public static final String STRONG_PASSWORD = "We'reSo$tr0ng@yekbal1!";
    public static final String GUIDANCE_PLACEHOLDER = "Password Strength Gauge";

    private final WebElement _el;
    private final WebDriver _driver;

    public SetPasswordForm(WebDriver driver)
    {
        _el = Locator.id("setPasswordForm").waitForElement(driver, 5_000);
        _driver = driver;

        if (getWrapper().getCurrentRelativeURL().contains("changePassword.view") && PasswordUtil.getUsername().equals(getWrapper().getCurrentUser()))
            throw new IllegalArgumentException("Don't change the primary site admin user's password");
    }

    // Don't use this unless you're actually testing authentication functionality
    public static SetPasswordForm goToInitialPasswordForUser(WebDriverWrapper wrapper, String email)
    {
        wrapper.beginAt(WebTestHelper.buildURL("security", "showRegistrationEmail", Map.of("email", email)));
        // Get setPassword URL from notification email.
        WebElement resetLink = Locator.linkWithHref("setPassword.view").findElement(wrapper.getDriver());

        wrapper.clickAndWait(resetLink, WebDriverWrapper.WAIT_FOR_PAGE);

        return new SetPasswordForm(wrapper.getDriver());
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    @LogMethod
    public void assertPasswordStrengthGauge()
    {
        assertPasswordStrengthGauge("", GUIDANCE_PLACEHOLDER);
        assertPasswordStrengthGauge(SIMPLE_PASSWORD, "Very Weak");
        assertPasswordStrengthGauge(SHORT_PASSWORD, "Very Weak");
        // Password is good enough for "GOOD" strength setting; not actually rated as "Good" by entropy calculation
        assertPasswordStrengthGauge(GOOD_PASSWORD, "Weak");
        assertPasswordStrengthGauge(STRONG_PASSWORD, "Very Strong");
    }

    @LogMethod(quiet = true)
    public void assertPasswordStrengthGauge(@LoggedParam String password, @LoggedParam String expectedGuidance)
    {
        setPassword1(password);
        String strengthGuidance = elementCache().strengthGuidance.getText();
        strengthGuidance = strengthGuidance.substring(strengthGuidance.indexOf(':') + 1).trim();

        assertEquals("Strength guidance for password", expectedGuidance, strengthGuidance);
    }

    public SetPasswordForm setEmail(String email)
    {
        elementCache().email.set(email);

        return this;
    }

    public SetPasswordForm setOldPassword(String oldPassword)
    {
        elementCache().oldPassword.set(oldPassword);

        return this;
    }

    public SetPasswordForm setPassword1(String password1)
    {
        elementCache().password.set(password1);

        return this;
    }

    public SetPasswordForm setPassword2(String password2)
    {
        elementCache().password2.set(password2);

        return this;
    }

    public SetPasswordForm setNewPassword(String password)
    {
        return setPassword1(password).setPassword2(password);
    }

    public void clickSubmit()
    {
        clickSubmit(getWrapper().getDefaultWaitForPage());
    }

    public void clickSubmit(int waitForPage)
    {
        getWrapper().clickAndWait(elementCache().submitButton, waitForPage);
        getWrapper().assertNoLabKeyErrors();
    }

    public SetPasswordForm clickSubmitExpectingError(String expectedError)
    {
        getWrapper().clickAndWait(elementCache().submitButton);
        Assert.assertEquals("Wrong error message", expectedError, Locators.labkeyError.waitForElement(getDriver(), 10_000).getText());

        return new SetPasswordForm(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        // For InitialUserAction
        final Input email = Input(Locator.id("email"), getDriver()).findWhenNeeded(this);

        // For ChangePasswordAction
        final Input oldPassword = Input(Locator.id("oldPassword"), getDriver()).findWhenNeeded(this);

        final Input password = Input(Locator.id("password"), getDriver()).findWhenNeeded(this);
        final Input password2 = Input(Locator.id("password2"), getDriver()).findWhenNeeded(this);
        final WebElement strengthGuidance = Locator.id("strengthGuidance").findWhenNeeded(this);
        final WebElement submitButton = Locator.name("set").findWhenNeeded(this);
    }
}
