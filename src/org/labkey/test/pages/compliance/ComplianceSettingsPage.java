/*
 * Copyright (c) 2016-2017 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.pages.compliance;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.compliance.AccountsTab;
import org.labkey.test.components.compliance.AuditTab;
import org.openqa.selenium.WebElement;

/**
 * Created by iansigmon on 12/13/16.
 */
@Deprecated  // TODO: fold the rest of this class's usages into BaseComplianceSettingsPage
            // use Page classes derived from BaseComplianceSettingsPage
public class ComplianceSettingsPage extends BaseComplianceSettingsPage
{
    public ComplianceSettingsPage(BaseWebDriverTest test)
    {
        super(test.getDriver());
    }

    public static ComplianceSettingsPage beginAt(BaseWebDriverTest test)
    {
        test.beginAt(WebTestHelper.buildURL("compliance", null, "complianceSettings"));
        return new ComplianceSettingsPage(test);
    }

    public AccountsTab clickAccountsTab()
    {
        showTab(SettingsTab.Accounts);
        return new AccountsTab(getDriver());
    }

    public AuditTab clickAuditTab()
    {
        showTab(SettingsTab.Audit);
        return new AuditTab(getDriver());
    }

    public ComplianceLoginSettingsPage clickLoginTab()
    {
        showTab(SettingsTab.Login);
        return new ComplianceLoginSettingsPage(getDriver());
    }

    public ComplianceSessionSettingsPage clickSessionTab()
    {
        showTab(SettingsTab.Session);
        return new ComplianceSessionSettingsPage(getDriver());
    }

    public ComplianceSessionSettingsPage blurBackgroundBehindLoggedOutModal()
    {
        return clickSessionTab()
                .blurBackgroundBehindLoggedOutModal();
    }

    public ComplianceSessionSettingsPage showBackgroundBehindLoggedOutModal()
    {
        return clickSessionTab()
                .showBackgroundBehindLoggedOutModal();
    }

    public void selectSessionSettingAndCancel(boolean blur)
    {
        showTab(SettingsTab.Session);
        if(blur)
            checkRadioButton(Locator.radioButtonByNameAndValue("backgroundHideEnabled", "true"));
        else
            checkRadioButton(Locator.radioButtonByNameAndValue("backgroundHideEnabled", "false"));
        clickButton("Cancel"); // expect redirect
    }

    public ProjectLockAndReviewSettingsPage clickProjectLockingAndReview()
    {
        showTab(SettingsTab.ProjectLockingAndReview);
        return new ProjectLockAndReviewSettingsPage(getDriver());
    }

    public void save()
    {
        elementCache().saveBtn().click();
    }

    public void cancel()
    {
        elementCache().cancelBtn().click();
    }



    @Override
    protected BaseComplianceSettingsPage.ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BaseComplianceSettingsPage.ElementCache
    {
        WebElement saveBtn()
        {
            return Locators.saveBtn.waitForElement(this, 1_000);
        }

        WebElement cancelBtn()
        {
            return Locators.cancelBtn.waitForElement(this, 1_000);
        }
    }

    public static class Locators extends org.labkey.test.Locators
    {
        public static final Locator.XPathLocator activeTab = Locator.tagWithClass("div", "active-tab").childTag("a");
        public static final Locator.XPathLocator accountsTab = Locator.tagWithClass("div", "tab").childTag("a").withText("Accounts");
        public static final Locator.XPathLocator auditTab = Locator.tagWithClass("div", "tab").childTag("a").withText("Audit");


        public static final Locator.XPathLocator saveBtn = Locator.linkWithSpan("Save");
        public static final Locator.XPathLocator cancelBtn = Locator.linkWithSpan("Cancel");
    }
}