package org.labkey.test.pages.core.login;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.params.login.DatabaseAuthenticationProvider;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.core.login.DbLoginUtils;
import org.labkey.test.util.core.login.DbLoginUtils.DbLoginProperties;
import org.labkey.test.util.core.login.DbLoginUtils.PasswordExpiration;
import org.labkey.test.util.core.login.DbLoginUtils.PasswordStrength;

public class DatabaseAuthConfigureDialog extends AuthDialogBase<DatabaseAuthConfigureDialog>
{

    public DatabaseAuthConfigureDialog(LoginConfigRow row)
    {
        super(new ModalDialogFinder(row.getDriver()).withTitle("Configure Database Authentication"));
        DbLoginUtils.initDbLoginConfig(this);
    }

    // get password strength
    public PasswordStrength getPasswordStrength()
    {
        return PasswordStrength.valueOf(elementCache().passwordStrengthSelect.getSelection().getValue());
    }

    public DatabaseAuthConfigureDialog setPasswordStrength(PasswordStrength newStrength)
    {
        if (getPasswordStrength().equals(newStrength))
            return this;

        elementCache().passwordStrengthSelect.selectOption(newStrength);
        WebDriverWrapper.waitFor(()-> getPasswordStrength().equals(newStrength),
                "the password strength was not set to desired state ["+ newStrength +"] in time", 1000);

        return this;
    }

    public DatabaseAuthConfigureDialog setPasswordExpiration(PasswordExpiration expiration)
    {
        elementCache().passwordExpirationSelect.selectOption(expiration);
        return this;
    }

    public PasswordExpiration getPasswordExpiration()
    {
        return PasswordExpiration.valueOf(elementCache().passwordExpirationSelect.getSelection().getValue());
    }

    @Override
    public LoginConfigRow clickApply()
    {
        Locator.findAnyElement("Finish or Apply button", this,
                Locators.dismissButton("Finish"), Locators.dismissButton("Apply")).click();
        waitForClose(4);
        return new LoginConfigRow.LoginConfigRowFinder(getDriver())
                .withDescription(new DatabaseAuthenticationProvider().getProviderDescription()).waitFor();
    }

    public void setDbLoginConfig(DbLoginProperties properties)
    {
        setDbLoginConfig(properties.strength(), properties.expiration());
    }

    @LogMethod
    public void setDbLoginConfig(@LoggedParam PasswordStrength newStrength, PasswordExpiration newExpiration)
    {
        setPasswordStrength(newStrength);
        setPasswordExpiration(newExpiration);
        clickApply();
    }

    public DbLoginProperties getDbLoginConfig()
    {
        return new DbLoginProperties(getPasswordStrength(), getPasswordExpiration());
    }

    @Override
    protected DatabaseAuthConfigureDialog getThis()
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

    protected class ElementCache extends AuthDialogBase<DatabaseAuthConfigureDialog>.ElementCache
    {
        OptionSelect<PasswordExpiration> passwordExpirationSelect = new OptionSelect<>(Locator.tagWithName("select", "expiration")
            .findWhenNeeded(this).withTimeout(2000));

        OptionSelect<PasswordStrength> passwordStrengthSelect = new OptionSelect<>(Locator.tagWithName("select", "strength")
            .findWhenNeeded(this).withTimeout(2000));
    }
}
