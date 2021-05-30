package org.labkey.test.pages.ldap.params.ldap;

import org.labkey.test.pages.core.login.LoginConfigRow;
import org.labkey.test.pages.ldap.pages.ldap.LdapConfigureDialog;
import org.labkey.test.params.login.AuthenticationProvider;
import org.openqa.selenium.WebDriver;

public class LdapAuthenticationProvider extends AuthenticationProvider<LdapConfigureDialog>
{
    @Override
    public String getProviderName()
    {
        return "LDAP";
    }

    @Override
    public String getProviderDescription()
    {
        return "Uses the LDAP protocol to authenticate against an institution's directory server";
    }

    @Override
    public LdapConfigureDialog getEditDialog(LoginConfigRow row)
    {
        return new LdapConfigureDialog(row);
    }

    @Override
    public LdapConfigureDialog getNewDialog(WebDriver driver)
    {
        return new LdapConfigureDialog(driver);
    }
}
