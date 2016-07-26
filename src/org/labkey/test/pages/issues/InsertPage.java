package org.labkey.test.pages.issues;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Select;
import org.openqa.selenium.WebDriver;

public class InsertPage extends UpdatePage
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
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "insert"));
        return new InsertPage(driver.getDriver());
    }
}