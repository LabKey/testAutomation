/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.test.pages;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ConfigureDbLoginPage extends LabKeyPage<ConfigureDbLoginPage.ElementCache>
{
    public ConfigureDbLoginPage(WebDriver driver)
    {
        super(driver);
    }

    public static ConfigureDbLoginPage beginAt(WebDriverWrapper driver)
    {
        driver.beginAt(WebTestHelper.buildURL("login", "configureDbLogin"));
        return new ConfigureDbLoginPage(driver.getDriver());
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

    @LogMethod
    public void setDbLoginConfig(@LoggedParam PasswordStrength newStrength, PasswordExpiration newExpiration)
    {
        PasswordStrength curStrength = null;
        PasswordExpiration curExpiration = null;

        if (oldStrength == null || oldExpiration == null)
        {
            curStrength = getCurrentPasswordStrength();
            curExpiration = getCurrentPasswordExpiration();
        }

        if ( newStrength != null && curStrength != newStrength)
        {
            if (oldStrength == null)
                oldStrength = curStrength;
            elementCache().getStrengthRadio(newStrength).check();
        }

        if ( newExpiration != null && curExpiration != newExpiration)
        {
            if (oldExpiration == null)
                oldExpiration = curExpiration;
            elementCache().expirationSelect.selectOption(newExpiration);
        }

        clickButton("Save");
    }

    @LogMethod
    public static void resetDbLoginConfig(LabKeySiteWrapper siteWrapper)
    {
        if ( oldStrength != null || oldExpiration != null )
        {
            siteWrapper.ensureSignedInAsPrimaryTestUser();
            beginAt(siteWrapper).resetDbLoginConfig();
        }
    }

    private void resetDbLoginConfig()
    {
        if (oldStrength != null)
            elementCache().getStrengthRadio(oldStrength).check();
        if (oldExpiration != null)
            elementCache().expirationSelect.selectOption(oldExpiration);

        clickButton("Save");

        if (oldStrength != null && oldStrength != getCurrentPasswordStrength())
            TestLogger.log("Failed to reset password strength to: " + oldStrength);
        if (oldExpiration != null && oldExpiration != getCurrentPasswordExpiration())
            TestLogger.log("Failed to reset password expiration to: " + oldExpiration);

        // Back to default.
        oldStrength = null;
        oldExpiration = null;
    }

    @NotNull
    public ConfigureDbLoginPage.PasswordExpiration getCurrentPasswordExpiration()
    {
        return PasswordExpiration.valueOf(getFormElement(Locator.name("expiration")));
    }

    @NotNull
    public ConfigureDbLoginPage.PasswordStrength getCurrentPasswordStrength()
    {
        return PasswordStrength.valueOf(Locator.css("input[name=strength]:checked").findElement(getDriver()).getAttribute("value"));
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected RadioButton getStrengthRadio(PasswordStrength strength)
        {
            return RadioButton.finder().withNameAndValue("strength", strength.toString()).find(this);
        }

        protected final OptionSelect<PasswordExpiration> expirationSelect = OptionSelect.finder(Locator.name("expiration"), PasswordExpiration.class).findWhenNeeded(this);
        protected final WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
        protected final WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
    }
}