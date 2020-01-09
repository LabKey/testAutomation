package org.labkey.test.pages.core.login;

import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.params.login.AuthenticationProvider;
import org.openqa.selenium.WebDriver;

public abstract class AuthDialogBase extends ModalDialog
{
    private final LoginConfigRow _row;

    protected AuthDialogBase(AuthenticationProvider provider, WebDriver driver)
    {
        super(getFinder("Configure New " + provider.getProviderName() + " Authentication", driver));
        _row = null;
    }

    protected AuthDialogBase(LoginConfigRow row)
    {
        super(getFinder("Configure " + row.getDescription(), row.getDriver()));
        _row = row;
    }

    private static ModalDialogFinder getFinder(String title, WebDriver driver)
    {
        return new ModalDialogFinder(driver).withTitle(title);
    }

    protected LoginConfigRow getRow()
    {
        return _row;
    }


    public LoginConfigRow clickApply()
    {
        dismiss("Apply");
        return getRow();
    }

    public LoginConfigRow clickCancel()
    {
        dismiss("cancel");
        return getRow();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {

    }


}
