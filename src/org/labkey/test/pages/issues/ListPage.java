package org.labkey.test.pages.issues;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ListPage extends LabKeyPage<ListPage.ElementCache>
{
    public ListPage(WebDriver driver)
    {
        super(driver);
    }

    public static ListPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), null);
    }

    public static ListPage beginAt(WebDriverWrapper driver, String issueDefName)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), issueDefName);
    }

    public static ListPage beginAt(WebDriverWrapper driver, String containerPath, String issueDefName)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "list", Maps.of("issueDefName", issueDefName)));
        return new ListPage(driver.getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement example = new LazyWebElement(Locator.css("button"), this);
    }
}