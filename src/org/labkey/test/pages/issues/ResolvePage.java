package org.labkey.test.pages.issues;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

import static org.labkey.test.components.labkey.ReadOnlyFormItem.ReadOnlyFormItem;
import static org.labkey.test.components.labkey.SelectFormItem.SelectFormItem;

public class ResolvePage extends UpdatePage<ResolvePage.ElementCache>
{
    public ResolvePage(WebDriver driver)
    {
        super(driver);
    }

    public static ResolvePage beginAt(WebDriverWrapper driver, int issueId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), issueId);
    }

    public static ResolvePage beginAt(WebDriverWrapper driver, String containerPath, int issueId)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "resolve", Maps.of("issueId", String.valueOf(issueId))));
        return new ResolvePage(driver.getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends UpdatePage.ElementCache
    {
        protected ElementCache()
        {
            resolution = SelectFormItem(getDriver()).withName("resolution").findWhenNeeded();
            duplicate = SelectFormItem(getDriver()).withName("duplicate").findWhenNeeded();
            status = ReadOnlyFormItem(getDriver()).withLabel("Status").findWhenNeeded();
        }
    }
}