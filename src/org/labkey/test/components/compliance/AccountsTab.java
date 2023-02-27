/*
 * Copyright (c) 2016-2017 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.compliance;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Created by iansigmon on 12/13/16.
 */
public class AccountsTab extends BaseComplianceSettingsTab
{
    Elements _elements;

    public AccountsTab(WebDriver driver)
    {
        super(driver);
    }

    public void enableDeactivateInactiveAccounts(boolean check)
    {
        if (check)
            checkCheckbox(getElements().enableInactiveCheckbox);
        else
            uncheckCheckbox(getElements().enableInactiveCheckbox);
    }

    public void enableExpiringAccounts(boolean check)
    {
        if (check)
            checkCheckbox(getElements().enableExpirationCheckbox);
        else
            uncheckCheckbox(getElements().enableExpirationCheckbox);
    }

    public void setInactiveLimit(Integer limit)
    {
        setFormElement(getElements().limitCombo, String.valueOf(limit));
    }

    public Elements getElements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }

    private class Elements extends LabKeyPage.ElementCache
    {
        WebElement enableInactiveCheckbox = new LazyWebElement(Locators.enableInactiveUsers, this);
        WebElement limitCombo = new LazyWebElement(Locators.inactivityLimit, this);
        WebElement enableExpirationCheckbox = new LazyWebElement(Locators.enableAccountExpiry, this);
    }

    public static class Locators extends org.labkey.test.Locators
    {
        protected static final Locator enableInactiveUsers = Locator.checkboxById("deactivateInactives");
        protected static final Locator inactivityLimit = Locator.input("inactivityLimit");
        protected static final Locator enableAccountExpiry = Locator.checkboxById("expireAccounts");
    }
}
