package org.labkey.test.components.ui.permissions;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class UserDetailsPanelUserAdminPage extends UserDetailsPanel
{
    protected UserDetailsPanelUserAdminPage(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    public static SimpleWebDriverComponentFinder<UserDetailsPanelUserAdminPage> finder(WebDriver driver)
    {
        return new SimpleWebDriverComponentFinder<>(driver, LOC, UserDetailsPanelUserAdminPage::new);
    }

    public void resetPassword()
    {
        elementCache().resetPwd.click();
        ModalDialog resetPwdDialog = new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Reset Password?").find();
        resetPwdDialog.dismiss("Yes, Reset Password");
    }

    public void deactivateUser()
    {
        elementCache().deactivate.click();
        ModalDialog deactivateUser = new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Deactivate 1 User?").find();
        deactivateUser.dismiss("Yes, Deactivate");
    }

    public void activateUser()
    {
        elementCache().reactivate.click();
        ModalDialog deactivateUser = new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Reactivate 1 User?").find();
        deactivateUser.dismiss("Yes, Reactivate");
    }

    public void deleteUser()
    {
        elementCache().delete.click();
        ModalDialog deleteUser = new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Delete 1 User?").find();
        deleteUser.dismiss("Yes, Permanently Delete");
    }

    @Override
    protected UserDetailsPanelUserAdminPage.ElementCache elementCache()
    {
        return new UserDetailsPanelUserAdminPage.ElementCache();
    }

    protected class ElementCache extends Component.ElementCache
    {
        private final WebElement resetPwd = Locator.button("Reset Password").findWhenNeeded(getComponentElement());
        private final WebElement delete = Locator.button("Delete").findWhenNeeded(getComponentElement());
        private final WebElement deactivate = Locator.button("Deactivate").findWhenNeeded(getComponentElement());
        private final WebElement reactivate = Locator.button("Reactivate").findWhenNeeded(getComponentElement());

    }
}
