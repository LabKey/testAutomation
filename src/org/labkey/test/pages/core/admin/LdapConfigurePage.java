package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;

public class LdapConfigurePage extends AuthProviderConfigPageBase<LdapConfigurePage>
{
    public LdapConfigurePage(WebDriver driver)
    {
        super(driver);
    }

    @Override
    LdapConfigurePage getThis()
    {
        return this;
    }

    public LdapConfigurePage setServerUrls(String serverUrls)
    {
        elementCache().serverUrlInput.setValue(serverUrls);
        return this;
    }
    public String getServerUrls()
    {
        return elementCache().serverUrlInput.getValue();
    }

    public LdapConfigurePage setLdapDomain(String ldapDomain)
    {
        elementCache().ldapDomainInput.setValue(ldapDomain);
        return this;
    }
    public String getLdapDomain()
    {
        return elementCache().ldapDomainInput.getValue();
    }

    public LdapConfigurePage setPrincipalTemplate(String principalTemplate)
    {
        elementCache().principalTemplateInput.setValue(principalTemplate);
        return this;
    }
    public String getPrincipalTemplate()
    {
        return elementCache().principalTemplateInput.getValue();
    }

    // sasl
    public LdapConfigurePage useSasl(boolean enabled)
    {
        elementCache().saslCheckbox.set(enabled);
        return this;
    }
    public boolean saslEnabled()
    {
        return elementCache().saslCheckbox.get();
    }

    // ldap search
    public LdapConfigurePage enableSearch(boolean enabled)
    {
        elementCache().enableSearchCheckbox.set(enabled);
        return this;
    }
    public boolean isSearchEnabled()
    {
        return elementCache().enableSearchCheckbox.get();
    }

    public LdapConfigurePage setUserName(String userName)
    {
        elementCache().userNameInput.setValue(userName);
        return this;
    }
    public String getUserName()
    {
        return elementCache().userNameInput.getValue();
    }

    public LdapConfigurePage setPassword(String password)
    {
        elementCache().passwordInput.setValue(password);
        return this;
    }

    public LdapConfigurePage setSearchBase(String searchBase)
    {
        elementCache().searchBaseInput.setValue(searchBase);
        return this;
    }
    public String getSearchBase()
    {
        return elementCache().searchBaseInput.getValue();
    }

    public LdapConfigurePage setLookupField(String lookupField)
    {
        elementCache().lookupFieldInput.setValue(lookupField);
        return this;
    }
    public String getLookupField()
    {
        return elementCache().lookupFieldInput.getValue();
    }

    public LdapConfigurePage setSearchTemplate(String searchTemplate)
    {
        elementCache().searchTemplateInput.setValue(searchTemplate);
        return this;
    }
    public String getSearchTemplate()
    {
        return elementCache().searchTemplateInput.getValue();
    }

    public LdapConfigurePage setEnabled(boolean enabled)
    {
        elementCache().enabledBox.set(enabled);
        return this;
    }
    public boolean getEnabled()
    {
        return elementCache().enabledBox.get();
    }

    // todo: link up test ldap settings page

    @Override
    protected ElementCache elementCache()
    {
        return(ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends AuthProviderConfigPageBase.ElementCache
    {
        public Input serverUrlInput = new Input(
                Locator.input("servers").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT),
                getDriver());
        public Input ldapDomainInput = new Input(
                Locator.input("domain").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT),
                getDriver());
        public Input principalTemplateInput = new Input(
                Locator.input("principalTemplate").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT),
                getDriver());

        public Checkbox saslCheckbox = new Checkbox(
                Locator.checkboxByName("SASL").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT));
        public Checkbox enableSearchCheckbox = new Checkbox(
                Locator.checkboxByName("search").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT));

        public Input userNameInput = new Input(
                Locator.input("username").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT),
                getDriver());
        public Input passwordInput = new Input(
                Locator.input("password").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT),
                getDriver());
        public Input searchBaseInput = new Input(
                Locator.input("searchBase").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT),
                getDriver());
        public Input lookupFieldInput = new Input(
                Locator.input("lookupField").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT),
                getDriver());
        public Input searchTemplateInput = new Input(
                Locator.input("searchTemplate").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT),
                getDriver());
        public Checkbox enabledBox = new Checkbox(
                Locator.checkboxByName("enabled").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT));
    }
}
