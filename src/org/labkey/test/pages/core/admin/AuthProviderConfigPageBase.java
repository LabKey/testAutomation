package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public abstract class AuthProviderConfigPageBase<T extends AuthProviderConfigPageBase> extends LabKeyPage<AuthProviderConfigPageBase.ElementCache>
{
    public AuthProviderConfigPageBase(WebDriver driver)
    {
        super(driver);
    }


    abstract T getThis();

    public T setDescription(String description)
    {
        elementCache().descriptionInput.setValue(description);
        return getThis();
    }
    public String getDescription()
    {
        return elementCache().descriptionInput.getValue();
    }

    public LoginConfigurePage clickSave()       // hmmm, doesn't return anywhere- just saves.  wait for page signal?
    {
        clickAndWait(elementCache().saveBtn);
        return new LoginConfigurePage(getDriver());
    }

    public LoginConfigurePage clickCancel()
    {
        clickAndWait(elementCache().cancelBtn);
        return new LoginConfigurePage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Input descriptionInput = new Input(
                Locator.input("description").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT), getDriver());
        WebElement saveBtn = Locator.lkButton("Save").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement cancelBtn = Locator.lkButton("Cancel").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
    }
}
