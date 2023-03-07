/*
 * Copyright (c) 2016-2017 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.pages.compliance;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ComplianceSettingsAuditPage extends BaseComplianceSettingsPage<ComplianceSettingsAuditPage.Elements>
{
    public ComplianceSettingsAuditPage(WebDriver driver)
    {
        super(driver);
    }

    public static ComplianceSettingsAuditPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        BaseComplianceSettingsPage.beginAt(webDriverWrapper, SettingsTab.Audit);
        return new ComplianceSettingsAuditPage(webDriverWrapper.getDriver());
    }

    public void selectNotifyAll()
    {
        elementCache().allRadio.click();
    }

    public void selectNotifyPrimary()
    {
        elementCache().primaryRadio.click();
    }

    public void enableAuditNotifications()
    {
        checkCheckbox(elementCache().enableCheckbox);
    }

    public void disableAuditNotifications()
    {
        uncheckCheckbox(elementCache().enableCheckbox);
    }

    public boolean isAuditNotificationsEnabled()
    {
        return elementCache().enableCheckbox.isSelected();
    }

    public void assertNotifyAllSelected(String msg)
    {
        assertTrue(msg, elementCache().allRadio.isSelected());
    }

    public void assertNotifyPrimarySelected(String msg)
    {
        assertTrue(msg, elementCache().primaryRadio.isSelected());
    }

    public void assertRadiosDisabled()
    {
        assertFalse("Radio button is enabled when it should not be", elementCache().allRadio.isEnabled());
        assertFalse("Radio button is enabled when it should not be", elementCache().primaryRadio.isEnabled());
    }

    public void assertRadiosEnabled()
    {
        assertTrue("Radio button is enabled when it should not be", elementCache().allRadio.isEnabled());
        assertTrue("Radio button is enabled when it should not be", elementCache().primaryRadio.isEnabled());
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    protected class Elements extends BaseComplianceSettingsPage<Elements>.ElementCache
    {
        WebElement enableCheckbox = Locator.checkboxById("enableAudit").findWhenNeeded(this);
        WebElement allRadio = Locator.radioButtonByNameAndValue("notifyAllAdmin", "all").findWhenNeeded(this);
        WebElement primaryRadio = Locator.radioButtonByNameAndValue("notifyAllAdmin", "primary").findWhenNeeded(this);
    }
}
