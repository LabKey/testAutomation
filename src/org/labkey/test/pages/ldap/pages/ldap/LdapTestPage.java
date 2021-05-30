package org.labkey.test.pages.ldap.pages.ldap;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

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
        WebElement serverInput = Locator.id("server").refindWhenNeeded(this);
        WebElement principalInput = Locator.id("principal").refindWhenNeeded(this);
        WebElement passwordInput = Locator.id("password").refindWhenNeeded(this);

        WebElement testBtn = Locator.lkButton("Test").refindWhenNeeded(this);
    }
}
