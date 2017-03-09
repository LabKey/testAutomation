package org.labkey.test.pages.list;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class GridPage extends LabKeyPage<GridPage.ElementCache>
{
    public GridPage(WebDriver driver)
    {
        super(driver);
    }

    public static GridPage beginAt(WebDriverWrapper driver, String listId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), listId);
    }

    public static GridPage beginAt(WebDriverWrapper driver, String containerPath, String listId)
    {
        driver.beginAt(WebTestHelper.buildURL("list", containerPath, "grid",  Maps.of("listId", listId)));
        return new GridPage(driver.getDriver());
    }

    public DataRegionTable getGrid()
    {
        return elementCache().table;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        private final DataRegionTable table = new DataRegionTable("query", getDriver());
    }
}