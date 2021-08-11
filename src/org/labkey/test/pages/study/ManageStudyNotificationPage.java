package org.labkey.test.pages.study;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ManageStudyNotificationPage extends LabKeyPage<ManageStudyNotificationPage.ElementCache>
{
    public ManageStudyNotificationPage(WebDriver driver)
    {
        super(driver);
    }

    public static ManageStudyNotificationPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ManageStudyNotificationPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("reports", containerPath, "manageNotifications"));
        return new ManageStudyNotificationPage(driver.getDriver());
    }

    public ManageStudyNotificationPage selectNone()
    {
        elementCache().none.check();
        return this;
    }

    public ManageStudyNotificationPage selectAll()
    {
        elementCache().all.check();
        return this;
    }

    public ManageStudyNotificationPage selectByCategory(String name)
    {
        elementCache().byCategory.check();
        _ext4Helper.checkGridCellCheckbox(name,0);
        return this;
    }

    public ManageStudyNotificationPage selectByDataset(String name)
    {
        elementCache().byDataset.check();
        _ext4Helper.checkGridCellCheckbox(name,0);
        return this;
    }

    public ManageStudyNotificationPage save()
    {
        clickButton("Save");
        return this;
    }

    public ManageStudyNotificationPage cancel()
    {
        clickButton("Cancel");
        return this;
    }

    @Override
    protected void waitForPage()
    {
        waitForText("Manage Study Notifications");
    }

    protected ManageStudyNotificationPage.ElementCache newElementCache()
    {
        return new ManageStudyNotificationPage.ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        RadioButton none = new RadioButton.RadioButtonFinder().withLabelContaining("None.").findWhenNeeded(this);
        RadioButton all = new RadioButton.RadioButtonFinder().withLabel("All. Your daily digest will list changes and additions to all reports and datasets.")
                .findWhenNeeded(this);
        RadioButton byCategory = new RadioButton.RadioButtonFinder().withLabel("By category. Your daily digest will list changes and additions to reports and datasets in the subscribed categories.")
                .findWhenNeeded(this);
        RadioButton byDataset = new RadioButton.RadioButtonFinder().withLabel("By dataset. Your daily digest will list changes and additions to subscribed datasets.")
                .findWhenNeeded(this);
    }
}
