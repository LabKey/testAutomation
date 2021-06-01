package org.labkey.test.pages.ldap.pages.ldap;

import org.labkey.test.Locator;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.core.login.AuthDialogBase;
import org.labkey.test.pages.core.login.LoginConfigRow;
import org.labkey.test.pages.core.login.SvgCheckbox;
import org.labkey.test.pages.ldap.params.ldap.LdapAuthenticationProvider;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

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
        elementCache().searchCheckBox.set(enable);
        return this;
    }

    public boolean isSearchEnabled()
    {
        return elementCache().searchCheckBox.get();
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
        elementCache().readAttributeCheckBox.set(enable);
        return this;
    }

    public boolean isReadAttributeEnabled()
    {
        return elementCache().readAttributeCheckBox.get();
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
        Input serverUrlsInput = new Input(Locator.input("servers")
                .findWhenNeeded(this).withTimeout(2000), getDriver());
        Input domainsInput = new Input(Locator.input("domain")
                .findWhenNeeded(this).withTimeout(2000), getDriver());
        Input principalTemplateInput = new Input(Locator.input("principalTemplate")
                .findWhenNeeded(this).withTimeout(2000), getDriver());

        WebElement testButton = Locator.tagWithClass("button", "labkey-button")
                .withText("Test").findWhenNeeded(this).withTimeout(2000);
        SvgCheckbox saslCheckbox = new SvgCheckbox(Locator.tagWithClass("span", "SASL")
                .findWhenNeeded(this).withTimeout(2000), getDriver());
        SvgCheckbox searchCheckBox = new SvgCheckbox(Locator.tagWithClass("span", "search")
                .findWhenNeeded(this).withTimeout(2000), getDriver());
        SvgCheckbox readAttributeCheckBox = new SvgCheckbox(Locator.tagWithClass("span", "readAttributes")
            .findWhenNeeded(this).withTimeout(2000), getDriver());
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

        WebElement checkBox(String label)
        {
            WebElement searchContext = Locator.tagWithClass("div", "dynamicFieldSpread").withChild(Locator.tagWithText("span", label))
                    .findWhenNeeded(this).withTimeout(2000);
            return Locator.tagWithClass("span", "clickable").findWhenNeeded(searchContext).withTimeout(2000);
        }
    }

}
