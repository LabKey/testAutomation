/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
package org.labkey.test.pages.user;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.core.login.SetPasswordForm;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.PasswordUtil;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

public class UserDetailsPage extends LabKeyPage<UserDetailsPage.ElementCache>
{
    public UserDetailsPage(WebDriver driver)
    {
        super(driver);
    }

    public static UserDetailsPage beginAt(WebDriverWrapper webDriverWrapper, Integer userId)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("user", "details", Map.of("userId", userId.toString())));
        return new UserDetailsPage(webDriverWrapper.getDriver());
    }

    public UpdateUserDetailsPage clickEdit()
    {
        clickAndWait(elementCache().editButton);
        return new UpdateUserDetailsPage(getDriver());
    }

    public ClonePermissionsPage clickClonePermission()
    {
        clickAndWait(elementCache().cloneButton);
        return new ClonePermissionsPage(getDriver());
    }

    public SetPasswordForm clickChangePassword()
    {
        if (PasswordUtil.getUsername().equals(getCurrentUser()))
            throw new IllegalArgumentException("Don't change the primary site admin user's password");

        clickAndWait(elementCache().changePwdButton);
        return new SetPasswordForm(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        WebElement editButton = Locator.lkButton("Edit").findWhenNeeded(this);
        WebElement cloneButton = Locator.lkButton("Clone Permissions").findWhenNeeded(this);
        WebElement changePwdButton = Locator.lkButton("Change Password").findWhenNeeded(this);
    }
}
