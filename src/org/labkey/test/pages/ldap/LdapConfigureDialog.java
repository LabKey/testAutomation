package org.labkey.test.pages.ldap;

import org.labkey.test.Locator;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.ReactCheckBox;
import org.labkey.test.pages.core.login.AuthDialogBase;
import org.labkey.test.pages.core.login.LoginConfigRow;
import org.labkey.test.params.ldap.LdapAuthenticationProvider;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * moved from
    https://github.com/LabKey/ldap/blob/9c17b2fe8aa111a0b023dddee8ede5da4f6d3556/test/src/org/labkey/test/pages/ldap/LdapConfigureDialog.java
 */

public class LdapConfigureDialog extends AuthDialogBase<LdapConfigureDialog>
{
    public LdapConfigureDialog(LoginConfigRow row)
    {
        super(row);
    }

    public LdapConfigureDialog(WebDriver driver)
    {
        super(new LdapAuthenticationProvider(), driver);
    }

    public String getServerUrls()
    {
        return elementCache().serverUrlsInput.get();
    }

    public LdapConfigureDialog setServerUrls(String serverUrls)
    {
        elementCache().serverUrlsInput.set(serverUrls);
        return this;
    }

    public String getLdapDomain()
    {
        return elementCache().domainsInput.get();
    }

    public LdapConfigureDialog setLdapDomain(String domain)
    {
        elementCache().domainsInput.set(domain);
        return this;
    }

    public String getPrincipalTemplate()
    {
        return elementCache().principalTemplateInput.get();
    }

    public LdapConfigureDialog setPrincipalTemplate(String principalTemplate)
    {
        elementCache().principalTemplateInput.set(principalTemplate);
        return this;
    }

    // search settings

    public String getUserName()
    {
        return elementCache().userInput.get();
    }

    public LdapConfigureDialog setUserName(String userName)     // label is 'search dn'
    {
        elementCache().userInput.set(userName);
        return this;
    }

    public LdapConfigureDialog setPassword(String password)
    {
        elementCache().passwordInput.set(password);
        return this;
    }

    public String getSearchBase()
    {
        return elementCache().searchBaseInput.get();
    }

    public LdapConfigureDialog setSearchBase(String searchBase)
    {
        elementCache().searchBaseInput.set(searchBase);
        return this;
    }

    public String getLookupField()
    {
        return elementCache().lookupFieldInput.get();
    }

    public LdapConfigureDialog setLookupField(String lookupField)
    {
        elementCache().lookupFieldInput.set(lookupField);
        return this;
    }

    public String getSearchTemplate()
    {
        return elementCache().searchTemplateInput.get();
    }

    public LdapConfigureDialog setSearchTemplate(String searchTemplate)
    {
        elementCache().searchTemplateInput.set(searchTemplate);
        return this;
    }

    public LdapConfigureDialog enableSearch(boolean enable)
    {
        elementCache().searchCheckbox.set(enable);
        return this;
    }

    public boolean isSearchEnabled()
    {
        return elementCache().searchCheckbox.get();
    }

    public LdapConfigureDialog enableSasl(boolean enable)
    {
        elementCache().saslCheckbox.set(enable);
        return this;
    }

    public boolean isSaslEnabled()
    {
        return elementCache().saslCheckbox.get();
    }

    public LdapConfigureDialog enableReadAttribute(boolean enable)
    {
        elementCache().readAttributeCheckbox.set(enable);
        return this;
    }

    public boolean isReadAttributeEnabled()
    {
        return elementCache().readAttributeCheckbox.get();
    }

    public LdapTestPage openLdapTestWindow()
    {
        elementCache().testButton.click();

        getWrapper().switchToWindow(1);
        return new LdapTestPage(getDriver());
    }

    @Override
    protected LdapConfigureDialog getThis()
    {
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends AuthDialogBase.ElementCache
    {
        ReactCheckBox checkbox(String inputId)
        {
            return new ReactCheckBox(Locator.tagWithId("input", inputId)
                    .findWhenNeeded(this));
        }

        Input serverUrlsInput = new Input(Locator.input("servers")
                .findWhenNeeded(this).withTimeout(2000), getDriver());
        Input domainsInput = new Input(Locator.input("domain")
                .findWhenNeeded(this).withTimeout(2000), getDriver());
        Input principalTemplateInput = new Input(Locator.input("principalTemplate")
                .findWhenNeeded(this).withTimeout(2000), getDriver());

        WebElement testButton = Locator.tagWithClass("button", "labkey-button")
                .withText("Test").findWhenNeeded(this).withTimeout(2000);
        ReactCheckBox saslCheckbox = checkbox("sasl");
        ReactCheckBox searchCheckbox = checkbox("search");
        ReactCheckBox readAttributeCheckbox = checkbox("readAttributes");
        Input userInput = new Input(Locator.input("username")
                .findWhenNeeded(this).withTimeout(2000), getDriver());
        Input passwordInput = new Input(Locator.input("password")
                .findWhenNeeded(this).withTimeout(2000), getDriver());
        Input searchBaseInput = new Input(Locator.input("searchBase")
                .findWhenNeeded(this).withTimeout(2000), getDriver());
        Input lookupFieldInput = new Input(Locator.input("lookupField")
                .findWhenNeeded(this).withTimeout(2000), getDriver());
        Input searchTemplateInput = new Input(Locator.input("searchTemplate")
                .findWhenNeeded(this).withTimeout(2000), getDriver());
    }

}
