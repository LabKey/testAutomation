/*
 * Copyright (c) 2023-2026 LabKey Corporation
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
package org.labkey.test.pages.compliance;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ComplianceSettingsAccountsPage extends BaseComplianceSettingsPage<ComplianceSettingsAccountsPage.Elements>
{
    public ComplianceSettingsAccountsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ComplianceSettingsAccountsPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        BaseComplianceSettingsPage.beginAt(webDriverWrapper, SettingsTab.Accounts);
        return new ComplianceSettingsAccountsPage(webDriverWrapper.getDriver());
    }

    public void enableDeactivateInactiveAccounts(boolean check)
    {
        setCheckbox(elementCache().enableInactiveCheckbox, check);
    }

    public void enableExpiringAccounts(boolean check)
    {
        setCheckbox(elementCache().enableExpirationCheckbox, check);
    }

    public void setInactiveLimit(Integer limit)
    {
        setFormElement(elementCache().limitCombo, String.valueOf(limit));
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    protected class Elements extends BaseComplianceSettingsPage<Elements>.ElementCache
    {
        final WebElement enableInactiveCheckbox = Locator.checkboxById("deactivateInactives").findWhenNeeded(this);
        final WebElement limitCombo = Locator.input("inactivityLimit").findWhenNeeded(this);
        final WebElement enableExpirationCheckbox = Locator.checkboxById("expireAccounts").findWhenNeeded(this);
    }
}
