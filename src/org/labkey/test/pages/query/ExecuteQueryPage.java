package org.labkey.test.pages.query;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class ExecuteQueryPage extends LabKeyPage<ExecuteQueryPage.ElementCache>
{
    public ExecuteQueryPage(WebDriver driver)
    {
        super(driver);
    }

    public static ExecuteQueryPage beginAt(WebDriverWrapper driver, String schemaName, String queryName)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), schemaName, queryName);
    }

    public static ExecuteQueryPage beginAt(WebDriverWrapper driver, String containerPath, String schemaName, String queryName)
    {
        driver.beginAt(WebTestHelper.buildURL("query", containerPath, "executeQuery", Maps.of("schemaName", schemaName, "query.queryName", queryName)));
        return new ExecuteQueryPage(driver.getDriver());
    }

    public DataRegionTable getDataRegion()
    {
        return elementCache()._dataRegionTable;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        DataRegionTable _dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").findWhenNeeded(this);
    }
}