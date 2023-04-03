package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class AuditLogMaintenancePage extends LabKeyPage<AuditLogMaintenancePage.ElementCache>
{
    public AuditLogMaintenancePage(WebDriver driver)
    {
        super(driver);
    }

    public static AuditLogMaintenancePage beginAt(WebDriverWrapper wrapper)
    {
        wrapper.beginAt(WebTestHelper.buildURL("professional", "auditLogMaintenance"));
        return new AuditLogMaintenancePage(wrapper.getDriver());
    }

    @Override
    public void waitForPage()
    {
        waitFor(() -> {
                    try
                    {
                        return elementCache().exportData.isDisplayed();
                    }
                    catch (NoSuchElementException nse)
                    {
                        return false;
                    }
                },
                "The audit log maintenance page did not load in time.",
                5_000);
    }

    @Override
    protected AuditLogMaintenancePage.ElementCache newElementCache()
    {
        return new AuditLogMaintenancePage.ElementCache();
    }

    public String getRetentionTime()
    {
        return elementCache().retentionTime.getFirstSelectedOption().getText();
    }

    public AuditLogMaintenancePage setRetentionTime(String value)
    {
        elementCache().retentionTime.selectByVisibleText(value);
        return this;
    }

    public AuditLogMaintenancePage setExportDataBeforeDeleting(boolean value)
    {
        setCheckbox(elementCache().exportData, value);
        return this;
    }

    public void clickSave()
    {
        clickAndWait(elementCache().saveBtn);
    }

    public void clickCancel()
    {
        clickAndWait(elementCache().cancelBtn);
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Select retentionTime = SelectWrapper.Select(Locator.name("retentionTime")).findWhenNeeded(this);
        WebElement exportData = Locator.name("export").findWhenNeeded(this);

        WebElement saveBtn = Locator.lkButton("Save").findWhenNeeded(this);
        WebElement cancelBtn = Locator.lkButton("Cancel").findWhenNeeded(this);
    }
}
