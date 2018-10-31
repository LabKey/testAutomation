package org.labkey.test.pages.assay;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class AssayRunsPage extends LabKeyPage<AssayRunsPage.ElementCache>
{
    public AssayRunsPage(WebDriver driver)
    {
        super(driver);
    }

    public static AssayRunsPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static AssayRunsPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("controller", containerPath, "action"));
        return new AssayRunsPage(driver.getDriver());
    }

    public DataRegionTable getTable()
    {
        return DataRegionTable.DataRegion(getDriver()).withName("Runs").findWhenNeeded(getDriver());
    }

    public AssayDataPage clickAssayIdLink(String assayId)
    {
        int rowIndex = getTable().getRowIndex("Assay Id", assayId);
        WebElement cell =  getTable().findCell(rowIndex, "Assay Id");
        WebElement link = Locator.linkWithText(assayId).waitForElement(cell, WAIT_FOR_JAVASCRIPT);
        clickAndWait(link);
        return new AssayDataPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {

    }
}
