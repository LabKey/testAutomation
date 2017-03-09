package org.labkey.test.pages.list;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.list.ManageListsGrid;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;

public class BeginPage extends LabKeyPage<BeginPage.ElementCache>
{
    public BeginPage(WebDriver driver)
    {
        super(driver);
    }

    public static BeginPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static BeginPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("list", containerPath, "begin"));
        return new BeginPage(driver.getDriver());
    }

    public ManageListsGrid getGrid()
    {
        return elementCache().listsGrid;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        private final ManageListsGrid listsGrid = new ManageListsGrid(getDriver());
    }
}