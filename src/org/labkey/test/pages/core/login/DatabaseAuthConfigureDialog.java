package org.labkey.test.pages.core.login;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.IOException;

public class DatabaseAuthConfigureDialog extends AuthDialogBase<DatabaseAuthConfigureDialog>
{

    public DatabaseAuthConfigureDialog(LoginConfigRow row)
    {
        super(getFinder("Configure Database Authentication", row.getDriver()));
        oldExpiration = getPasswordExpiration();
        oldStrength = getPasswordStrength();
    }

    private static ModalDialogFinder getFinder(String title, WebDriver driver)
    {
        return new ModalDialogFinder(driver).withTitle(title);
    }

    // get password strength
    public PasswordStrength getPasswordStrength()
    {
        if (elementCache().weakButton.getAttribute("class").contains("active"))
            return PasswordStrength.Weak;
        else return PasswordStrength.Strong;
    }

    public DatabaseAuthConfigureDialog setPasswordStrength(PasswordStrength newStrength)
    {
        if (getPasswordStrength().equals(newStrength))
            return this;

        if (newStrength.equals(PasswordStrength.Strong))
            elementCache().strongButton.click();
        else
            elementCache().weakButton.click();

        WebDriverWrapper.waitFor(()-> getPasswordStrength().equals(newStrength),
                "the password strength was not set to desired state ["+newStrength.toString()+"] in time", 1000);
        return this;
    }

    public DatabaseAuthConfigureDialog setPasswordExpiration(PasswordExpiration expiration)
    {
        elementCache().passwordExpirationSelect.selectOption(expiration);
        return this;
    }

    public PasswordExpiration getPasswordExpiration()
    {
        return  PasswordExpiration.valueOf(elementCache().passwordExpirationSelect.getSelection().getValue());
    }

    @Override
    public LoginConfigRow clickApply()
    {
        Locator.findAnyElement("Finish or Apply button", this,
                Locators.dismissButton("Finish"), Locators.dismissButton("Apply")).click();
        waitForClose(4);
        return new LoginConfigRow.LoginConfigRowFinder(getDriver()).withDescription("Standard database authentication").waitFor();
    }


    @LogMethod
    public void setDbLoginConfig(@LoggedParam PasswordStrength newStrength, PasswordExpiration newExpiration)
    {
        setPasswordStrength(newStrength);
        setPasswordExpiration(newExpiration);
        clickApply();
    }

    @LogMethod
    public static void resetDbLoginConfig(Connection connection)
    {
        if ( oldStrength != null || oldExpiration != null )
        {
            JSONObject params = new JSONObject();
            params.put("expiration", oldExpiration != null ? oldExpiration.name() : PasswordExpiration.Never.name());
            params.put("strength", oldStrength != null ? oldStrength.name() : PasswordStrength.Strong.name());
            PostCommand<?> postCommand = new PostCommand<>("login", "SaveDbLoginProperties");
            postCommand.setJsonObject(params);
            try
            {
                postCommand.execute(connection, "/");
                oldStrength = null;
                oldExpiration = null;
            }
            catch (IOException | CommandException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public enum PasswordStrength {Weak, Strong}
    public enum PasswordExpiration implements OptionSelect.SelectOption
    {
        Never, FiveSeconds, ThreeMonths, SixMonths, OneYear;

        @Override
        public String getValue()
        {
            return name();
        }
    }

    private static PasswordStrength oldStrength = null;
    private static PasswordExpiration oldExpiration = null;

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

    protected class ElementCache extends AuthDialogBase.ElementCache
    {
        OptionSelect<PasswordExpiration> passwordExpirationSelect = new OptionSelect<>(Locator.tagWithName("select", "expiration")
                .findWhenNeeded(this).withTimeout(2000));

        WebElement strongButton = Locator.button("Strong").refindWhenNeeded(this).withTimeout(4000);
        WebElement weakButton = Locator.button("Weak").refindWhenNeeded(this).withTimeout(4000);
    }


}
