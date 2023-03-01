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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by iansigmon on 12/19/16.
 */
public class AuditTab extends BaseComplianceSettingsTab
{
    Elements _elements;

    public AuditTab(WebDriver driver)
    {
        super(driver);
    }

    public Elements getElements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }

    public void selectNotifyAll()
    {
        getElements().allRadio.click();
    }

    public void selectNotifyPrimary()
    {
        getElements().primaryRadio.click();
    }

    public void enableAuditNotifications()
    {
        checkCheckbox(getElements().enableCheckbox);
    }

    public void disableAuditNotifications()
    {
        uncheckCheckbox(getElements().enableCheckbox);
    }

    public boolean isAuditNotificationsEnabled()
    {
        return isChecked(Locators.enableAuditNotification);
    }

    public void assertNotifyAllSelected(String msg)
    {
        assertTrue(msg, getElements().allRadio.isSelected());
    }

    public void assertNotifyPrimarySelected(String msg)
    {
        assertTrue(msg, getElements().primaryRadio.isSelected());
    }

    public void assertRadiosDisabled()
    {
        assertFalse("Radio button is enabled when it should not be", getElements().allRadio.isEnabled());
        assertFalse("Radio button is enabled when it should not be", getElements().primaryRadio.isEnabled());
    }

    public void assertRadiosEnabled()
    {
        assertTrue("Radio button is enabled when it should not be", getElements().allRadio.isEnabled());
        assertTrue("Radio button is enabled when it should not be", getElements().primaryRadio.isEnabled());
    }

    private class Elements extends LabKeyPage.ElementCache
    {
        WebElement enableCheckbox = new LazyWebElement(Locators.enableAuditNotification, this);
        WebElement allRadio = new LazyWebElement(Locators.notifyAll, this);
        WebElement primaryRadio = new LazyWebElement(Locators.notifyPrimary, this);
    }

    public static class Locators extends org.labkey.test.Locators
    {
        protected static final Locator enableAuditNotification = Locator.checkboxById("enableAudit");
        protected static final Locator notifyAll = Locator.radioButtonByNameAndValue("notifyAllAdmin", "all");
        protected static final Locator notifyPrimary = Locator.radioButtonByNameAndValue("notifyAllAdmin", "primary");
    }
}
