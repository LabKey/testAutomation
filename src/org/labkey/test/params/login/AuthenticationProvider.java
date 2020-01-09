package org.labkey.test.params.login;

import org.labkey.test.pages.core.login.AuthDialogBase;
import org.labkey.test.pages.core.login.LoginConfigRow;
import org.openqa.selenium.WebDriver;

public abstract class AuthenticationProvider<D extends AuthDialogBase>
{
    public abstract String getProviderName();
    public abstract String getProviderDescription();
    public abstract D getEditDialog(LoginConfigRow row);
    public abstract D getNewDialog(WebDriver driver);
}
