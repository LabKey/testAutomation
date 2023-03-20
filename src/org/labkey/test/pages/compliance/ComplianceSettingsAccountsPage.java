/*
 * Copyright (c) 2016-2017 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
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
