package org.labkey.test.pages.flow.reports;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.flow.FlowReportsWebpart;
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
        driver.beginAt(WebTestHelper.buildURL("flow-reports", containerPath, "begin"));
        return new BeginPage(driver.getDriver());
    }

    public FlowReportsWebpart reportsPanel()
    {
        return elementCache().reportsWebpart;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        FlowReportsWebpart reportsWebpart = new FlowReportsWebpart(getDriver());
    }
}