package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.pipeline.PipelineStatusDetailsPage;
import org.openqa.selenium.WebDriver;

public class ConfigureSystemMaintenancePage extends LabKeyPage<ConfigureSystemMaintenancePage.ElementCache>
{
    public ConfigureSystemMaintenancePage(WebDriver driver)
    {
        super(driver);
    }

    public static ConfigureSystemMaintenancePage beginAt(WebDriverWrapper webDriverWrapper)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("admin", "configureSystemMaintenance"));
        return new ConfigureSystemMaintenancePage(webDriverWrapper.getDriver());
    }

    /**
     * Run the specified maintenance task and switch to the window that opens
     * @param description task description
     */
    public PipelineStatusDetailsPage runMaintenanceTask(String description)
    {
        doAndWaitForWindow(() -> click(Locator.tagWithAttribute("input", "type", "checkbox")
                .followingSibling("a").withText(description)), "systemMaintenance");

        PipelineStatusDetailsPage pipelineStatusDetailsPage = new PipelineStatusDetailsPage(getDriver());
        pipelineStatusDetailsPage.waitForComplete();
        return pipelineStatusDetailsPage;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<ElementCache>.ElementCache
    {
    }
}
