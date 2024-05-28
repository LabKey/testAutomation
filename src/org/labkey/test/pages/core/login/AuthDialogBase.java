package org.labkey.test.pages.core.login;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.ToggleButton;
import org.labkey.test.params.login.AuthenticationProvider;
import org.openqa.selenium.WebDriver;

public abstract class AuthDialogBase<T extends AuthDialogBase<T>> extends ModalDialog
{
    private final LoginConfigRow _row;

    protected AuthDialogBase(AuthenticationProvider<?> provider, WebDriver driver)
    {
        super(getFinder("Add New " + provider.getProviderName() + " Configuration", driver));
        _row = null;
    }

    protected AuthDialogBase(LoginConfigRow row)
    {
        super(getFinder("Configure " + row.getDescription(), row.getDriver()));
        _row = row;
    }

    protected AuthDialogBase(ModalDialogFinder finder)
    {
        super(finder);
        _row = null;
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
        return elementCache().enableToggle.isOn();
    }

    public T clickApplyExpectingError()
    {
        String buttonText = _row == null ? "Finish" : "Apply";
        Locators.dismissButton(buttonText).waitForElement(this, 2000).click();
        return getThis();
    }

    /**
     * for UX reasons, the wording in the button to submit an auth dialog differs when we're creating
     * a new config vs. editing an existing one.
     */
    public LoginConfigRow clickApply()
    {
        String description = getDescription();
        dismiss(_row == null ? "Finish" : "Apply");
        return new LoginConfigRow.LoginConfigRowFinder(getDriver()).withDescription(description).waitFor();
    }

    public void clickCancel()
    {
        dismiss("Cancel");
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
                .timeout(2000).findWhenNeeded(this);

        Input descriptionInput = new Input(Locator.input("description")
                .findWhenNeeded(this).withTimeout(2000), getDriver());
    }


}
