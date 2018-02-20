package org.labkey.test.pages.files;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class WebFilesPage extends LabKeyPage<WebFilesPage.ElementCache>
{
    public WebFilesPage(WebDriver driver)
    {
        super(driver);
    }

    public static WebFilesPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static WebFilesPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.getBaseURL() + "/_webdav/" + containerPath + "/?listing=html/");
        return new WebFilesPage(driver.getDriver());
    }

    public LabKeyPage clickButton()
    {
        clickAndWait(elementCache().standardViewButton);

        return new LabKeyPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement standardViewButton = Locator.button("Standard View").findWhenNeeded(this);
    }
}