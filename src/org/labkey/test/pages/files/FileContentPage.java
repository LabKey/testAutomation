package org.labkey.test.pages.files;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.FileBrowserHelper;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class FileContentPage extends LabKeyPage<FileContentPage.ElementCache>
{
    private FileBrowserHelper _fileBrowserHelper;

    public FileContentPage(WebDriver driver)
    {
        super(driver);
        _fileBrowserHelper = new FileBrowserHelper(getDriver());
    }

    public static FileContentPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static FileContentPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("filecontent", containerPath, "begin"));
        return new FileContentPage(driver.getDriver());
    }

    public FileBrowserHelper fileBrowserHelper()
    {
        // todo: expose browserHelper interfaces in the page
        return _fileBrowserHelper;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement example = Locator.css("button").findWhenNeeded(this);
    }
}