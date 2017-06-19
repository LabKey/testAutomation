package org.labkey.test.pages.announcements;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class RespondPage extends BaseUpdatePage<RespondPage>
{
    public RespondPage(WebDriver driver)
    {
        super(driver);
    }

    public static RespondPage beginAt(WebDriverWrapper driver, String parentId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), parentId);
    }

    public static RespondPage beginAt(WebDriverWrapper driver, String containerPath, String parentId)
    {
        driver.beginAt(WebTestHelper.buildURL("announcements", containerPath, "respond", Maps.of("parentId", parentId)));
        return new RespondPage(driver.getDriver());
    }

    @Override
    protected RespondPage getThis()
    {
        return this;
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BaseUpdatePage<RespondPage>.ElementCache
    {
        // TODO: Add edit and delete links for thread embedded on page
    }
}