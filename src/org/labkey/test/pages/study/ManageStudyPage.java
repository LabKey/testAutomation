package org.labkey.test.pages.study;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ManageStudyPage extends LabKeyPage<ManageStudyPage.ElementCache>
{
    public ManageStudyPage(WebDriver driver)
    {
        super(driver);
    }

    public static ManageStudyPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ManageStudyPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("study", containerPath, "manageStudy"));
        return new ManageStudyPage(driver.getDriver());
    }

    public StudySecurityPage manageSecurity()
    {
        clickAndWait(elementCache().manageSecurity);
        return new StudySecurityPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement manageSecurity = Locator.linkWithText("Manage Security").findWhenNeeded(this);
    }
}