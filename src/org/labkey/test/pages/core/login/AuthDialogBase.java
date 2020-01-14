package org.labkey.test.pages.core.login;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.ToggleButton;
import org.labkey.test.params.login.AuthenticationProvider;
import org.openqa.selenium.WebDriver;

public abstract class AuthDialogBase<T extends AuthDialogBase> extends ModalDialog
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

    public T setDescription(String description)
    {
        elementCache().descriptionInput.set(description);
        return getThis();
    }

    public String getDescription()
    {
        return elementCache().descriptionInput.get();
    }

    public T setEnabled(boolean enabled)
    {
        elementCache().enableToggle.set(enabled);
        return getThis();
    }

    public boolean isEnabled()
    {
        return elementCache().enableToggle.get();
    }

    public T clickButtonExpectingError(String buttonText)
    {
        Locators.dismissButton(buttonText).waitForElement(this, 2000).click();
        return getThis();
    }

    /**
     * for UX reasons, the wording in the button to submit an auth dialog differs when we're creating
     * a new config vs. editing an existing one.
     * @return
     */
    public LoginConfigRow clickApply()
    {
        String description= getDescription();
        Locator.findAnyElement("Finish or Apply button", this,
                Locators.dismissButton("Finish"), Locators.dismissButton("Apply")).click();
        waitForClose(4);
        return new LoginConfigRow.LoginConfigRowFinder(getDriver()).withDescription(description).waitFor();
    }

    public void clickCancel()
    {
        dismiss("cancel");
    }

    protected abstract T getThis();

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return  (ElementCache) super.elementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        ToggleButton enableToggle = new ToggleButton.ToggleButtonFinder(getDriver())
            .withState("Enabled").timeout(2000).findWhenNeeded(this);

        Input descriptionInput = new Input(Locator.input("description")
                .findWhenNeeded(this).withTimeout(2000), getDriver());
    }


}
