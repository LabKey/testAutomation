package org.labkey.test.pages.core.login;

import org.labkey.test.params.login.AuthenticationProvider;
import org.openqa.selenium.WebDriver;

public class DatabaseAuthenticationProvider extends AuthenticationProvider<DatabaseAuthConfigureDialog>
{
    @Override
    public String getProviderName()
    {
        return "Database";
    }

    @Override
    public String getProviderDescription()
    {
        return "Standard Database authentication";
    }

    @Override
    public DatabaseAuthConfigureDialog getEditDialog(LoginConfigRow row)
    {
        return new DatabaseAuthConfigureDialog(row);
    }

    @Override
    public DatabaseAuthConfigureDialog getNewDialog(WebDriver driver)
    {
        return null;    // we shouldn't be calling this it is there by default/no way to add a new one;
    }
}
