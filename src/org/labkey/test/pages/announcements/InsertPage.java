package org.labkey.test.pages.announcements;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.openqa.selenium.WebDriver;

public class InsertPage extends BaseUpdatePage<InsertPage>
{
    public InsertPage(WebDriver driver)
    {
        super(driver);
    }

    public static InsertPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static InsertPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("announcements", containerPath, "insert"));
        return new InsertPage(driver.getDriver());
    }

    @Override
    protected InsertPage getThis()
    {
        return this;
    }
}