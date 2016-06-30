package org.labkey.test.pages.issues;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

import static org.labkey.test.components.labkey.ReadOnlyFormItem.ReadOnlyFormItem;

public class ClosePage extends UpdatePage<ClosePage.ElementCache>
{
    public ClosePage(WebDriver driver)
    {
        super(driver);
    }

    public static ClosePage beginAt(WebDriverWrapper driver, int issueId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), issueId);
    }

    public static ClosePage beginAt(WebDriverWrapper driver, String containerPath, int issueId)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "close", Maps.of("issueId", String.valueOf(issueId))));
        return new ClosePage(driver.getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends UpdatePage.ElementCache
    {
        protected ElementCache()
        {
            assignedTo = ReadOnlyFormItem(getDriver()).withLabel("Assigned To").findWhenNeeded();
            status = ReadOnlyFormItem(getDriver()).withLabel("Status").findWhenNeeded();
        }
    }
}