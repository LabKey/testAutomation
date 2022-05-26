package org.labkey.test.pages.ldap;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Moved from
 * https://github.com/LabKey/ldap/blob/9c17b2fe8aa111a0b023dddee8ede5da4f6d3556/test/src/org/labkey/test/pages/ldap/LdapTestPage.java
 */
public class LdapTestPage extends LabKeyPage<LdapTestPage.ElementCache>
{
    public LdapTestPage(WebDriver driver)
    {
        super(driver);
    }

    public LdapTestPage setServerUrl(String serverURL)
    {
        setFormElement(elementCache().serverInput, serverURL);
        return this;
    }

    public String getServerUrl()
    {
        return elementCache().serverInput.getAttribute("value");
    }

    public LdapTestPage setSecurityPrincipal(String securityPrincipal)
    {
        setFormElement(elementCache().principalInput, securityPrincipal);
        return this;
    }

    public String getSecurityPrincipal()
    {
        return elementCache().principalInput.getAttribute("value");
    }

    public LdapTestPage setPassword(String password)
    {
        setFormElement(elementCache().passwordInput, password);
        return this;
    }

    public LdapTestPage clickTest()
    {
        clickAndWait(elementCache().testBtn);
        super.clearCache();
        return new LdapTestPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement serverInput = Locator.id("server").refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement principalInput = Locator.id("principal").refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement passwordInput = Locator.id("password").refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

        WebElement testBtn = Locator.lkButton("Test").refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
    }
}
