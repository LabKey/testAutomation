package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

// TODO: Missing lots of functionality
public class ShowAdminPage extends LabKeyPage<ShowAdminPage.ElementCache>
{
    public ShowAdminPage(WebDriver driver)
    {
        super(driver);
    }

    public static ShowAdminPage beginAt(WebDriverWrapper driver)
    {
        driver.beginAt(WebTestHelper.buildURL("admin", "showAdmin"));
        return new ShowAdminPage(driver.getDriver());
    }

    public List<String> getActiveUsers()
    {
        return getTexts(elementCache().findActiveUsers());
    }

    public CustomizeSitePage clickSiteSettings()
    {
        clickAndWait(elementCache().siteSettingsLink);
        return new CustomizeSitePage(getDriver());
    }

    public ConfigureFileSystemAccessPage clickFiles()
    {
        clickAndWait(elementCache().filesLink);
        return new ConfigureFileSystemAccessPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected WebElement siteSettingsLink = Locator.linkWithText("site settings").findWhenNeeded(this);
        protected WebElement filesLink = Locator.linkWithText("files").findWhenNeeded(this);

        protected List<WebElement> findActiveUsers()
        {
            return Locator.tagWithName("table", "activeUsers").append(Locator.tag("td").position(1)).findElements(this);
        }
    }
}