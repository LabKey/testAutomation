package org.labkey.test.pages.assay;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;

public class AssayDataPage extends LabKeyPage<AssayDataPage.ElementCache>
{
    public AssayDataPage(WebDriver driver)
    {
        super(driver);
    }

    public static AssayDataPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static AssayDataPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("assay", containerPath, "assayResults"));
        return new AssayDataPage(driver.getDriver());
    }

    public DataRegionTable getDataTable()
    {
        return DataRegionTable.DataRegion(getDriver()).withName("Data").findWhenNeeded(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {

    }
}
