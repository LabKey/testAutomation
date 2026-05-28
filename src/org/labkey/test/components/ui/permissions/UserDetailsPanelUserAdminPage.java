/*
 * Copyright (c) 2020-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    public boolean canResetTOTPSettings()
    {
        return Locator.button("Reset TOTP Settings").isDisplayed(getDriver());
    }

    public void resetTOTPSettings()
    {
        elementCache().resetTOTP.click();
        ModalDialog resetPwdDialog = new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Reset TOTP Settings?").find();
        resetPwdDialog.dismiss("Yes, Reset TOTP Settings");
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
        private final WebElement resetTOTP = Locator.button("Reset TOTP Settings").findWhenNeeded(getComponentElement());
        private final WebElement delete = Locator.button("Delete").findWhenNeeded(getComponentElement());
        private final WebElement deactivate = Locator.button("Deactivate").findWhenNeeded(getComponentElement());
        private final WebElement reactivate = Locator.button("Reactivate").findWhenNeeded(getComponentElement());

    }
}
