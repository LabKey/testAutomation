package org.labkey.test.pages.announcements;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class UpdatePage extends BaseUpdatePage<UpdatePage>
{
    public UpdatePage(WebDriver driver)
    {
        super(driver);
    }

    public static UpdatePage beginAt(WebDriverWrapper driver, String entityId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), entityId);
    }

    public static UpdatePage beginAt(WebDriverWrapper driver, String containerPath, String entityId)
    {
        driver.beginAt(WebTestHelper.buildURL("announcements", containerPath, "update", Maps.of("entityId", entityId)));
        return new UpdatePage(driver.getDriver());
    }

    @Override
    protected UpdatePage getThis()
    {
        return this;
    }
}
