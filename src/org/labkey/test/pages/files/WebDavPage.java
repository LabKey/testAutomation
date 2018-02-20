package org.labkey.test.pages.files;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.FileBrowserHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class WebDavPage extends LabKeyPage<WebDavPage.ElementCache>
{
    private FileBrowserHelper _fileBrowserHelper;
    public WebDavPage(WebDriver driver)
    {
        super(driver);
        _fileBrowserHelper = new FileBrowserHelper(driver);
    }

    public static WebDavPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static WebDavPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.getBaseURL() + "/_webdav/" + containerPath + "/@files/");
        return new WebDavPage(driver.getDriver());
    }

    public FileBrowserHelper getFileBrowserHelper()
    {
        return _fileBrowserHelper;
    }

    public WebFilesPage goToWebFiles()
    {
        clickAndWait(elementCache().htmlViewButton);

        return new WebFilesPage(getDriver());
    }

    public String getWebDavUrl()
    {
        return elementCache().webDavUrlElement.getText();
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement htmlViewButton = Locator.button("HTML View").findWhenNeeded(this);

        WebElement fbDetailsTable = Locator.tagWithClass("table", "fb-details").findWhenNeeded(this);
        WebElement webDavUrlElement = Locator.tagWithText("th", "WebDav URL:").followingSibling("a")
                .findWhenNeeded(fbDetailsTable);
    }
}