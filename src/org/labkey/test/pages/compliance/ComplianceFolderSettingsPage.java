/*
 * Copyright (c) 2017-2018 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.pages.compliance;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.admin.FolderManagementPage;
import org.labkey.test.util.compliance.ComplianceUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.util.compliance.ComplianceUtils.PhiColumnBehavior.HIDE;
import static org.labkey.test.util.compliance.ComplianceUtils.PhiColumnBehavior.SHOW;

public class ComplianceFolderSettingsPage extends FolderManagementPage
{
    public ComplianceFolderSettingsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ComplianceFolderSettingsPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("compliance", containerPath, "settings"));
        return new ComplianceFolderSettingsPage(driver.getDriver());
    }

    public ComplianceFolderSettingsPage setInheritTermsOfUse(boolean check)
    {
        setCheckbox(elementCache().inheritTermsOfUse, check);
        return this;
    }

    public ComplianceFolderSettingsPage setRequireActivity(boolean check)
    {
        setCheckbox(elementCache().requireActivity, check);
        return this;
    }

    public ComplianceFolderSettingsPage setPhiRolesRequired(boolean check)
    {
        setCheckbox(elementCache().phiRolesRequired, check);
        return this;
    }

    public ComplianceFolderSettingsPage setPhiRolesRequiredAndColumnBehavior(boolean check, ComplianceUtils.PhiColumnBehavior behavior)
    {
        setCheckbox(elementCache().phiRolesRequired, check);
        if (check)
        {
            if (!SHOW.equals(behavior))
            {
                WebElement radio = HIDE.equals(behavior) ? elementCache().hidePhiColumn : elementCache().blankPhiColumn;
                shortWait().until(driver -> radio.isEnabled());
                radio.click();
            }
        }
        return this;
    }

    public ComplianceFolderSettingsPage setColumnBehavior(ComplianceUtils.PhiColumnBehavior behavior)
    {
        // Can't directly SHOW; must uncheck phiRolesRequired to SHOW
        switch(behavior)
        {
            case HIDE:
                elementCache().hidePhiColumn.click();
                break;
            case BLANK:
            default:
                elementCache().blankPhiColumn.click();
                break;
        }
        return this;
    }

    public ComplianceFolderSettingsPage setQueryLoggingBehavior(ComplianceUtils.QueryLoggingBehavior behavior)
    {
        switch(behavior)
        {
            case NONE:
                elementCache().noLogging.click();
                break;
            case PHI:
                elementCache().phiOnlyLogging.click();
                break;
            case ALL:
            default:
                elementCache().allLogging.click();
                break;
        }
        return this;
    }

    public void save()
    {
        clickAndWait(elementCache().saveButton);
        waitForElement(Locator.tagWithClass("b", "labkey-message").withText("Save successful."));
    }

    @Override
    protected ComplianceFolderSettingsPage.ElementCache elementCache()
    {
        return (ComplianceFolderSettingsPage.ElementCache) super.elementCache();
    }

    @Override
    protected ComplianceFolderSettingsPage.ElementCache newElementCache()
    {
        return new ComplianceFolderSettingsPage.ElementCache();
    }

    protected class ElementCache extends FolderManagementPage.ElementCache
    {
        protected final WebElement inheritTermsOfUse = Locator.checkboxByName("inheritTermsOfUse").findWhenNeeded(this);
        protected final WebElement requireActivity = Locator.checkboxById("overrideSetting").findWhenNeeded(this);
        protected final WebElement phiRolesRequired = Locator.checkboxById("phiRolesRequired").findWhenNeeded(this);
//        protected final WebElement showPhiColumn = Locator.radioButtonByNameAndValue("phiColumnBehavior", "show").findWhenNeeded(this);
        protected final WebElement hidePhiColumn = Locator.radioButtonByNameAndValue("phiColumnBehavior", "hide").findWhenNeeded(this);
        protected final WebElement blankPhiColumn = Locator.radioButtonByNameAndValue("phiColumnBehavior", "blank").findWhenNeeded(this);
        protected final WebElement noLogging = Locator.radioButtonByNameAndValue("loggingBehavior", "none").findWhenNeeded(this);
        protected final WebElement phiOnlyLogging = Locator.radioButtonByNameAndValue("loggingBehavior", "phi").findWhenNeeded(this);
        protected final WebElement allLogging = Locator.radioButtonByNameAndValue("loggingBehavior", "all").findWhenNeeded(this);
        protected final WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
    }

}
