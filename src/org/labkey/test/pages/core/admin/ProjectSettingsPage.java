package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ProjectSettingsPage extends LabKeyPage<ProjectSettingsPage.ElementCache>
{
    public ProjectSettingsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ProjectSettingsPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ProjectSettingsPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("admin", containerPath, "projectSettings"));
        return new ProjectSettingsPage(driver.getDriver());
    }

    public void save()
    {
        elementCache().saveButton.click();
    }

    public Checkbox getEnableDiscussionCheckbox()
    {
        return elementCache().enableDiscussion;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected final Checkbox enableDiscussion = Checkbox.Checkbox(Locator.name("enableDiscussion")).findWhenNeeded(this);
        protected final WebElement saveButton = findButton("Save");
    }
}