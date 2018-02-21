package org.labkey.test.pages.user;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ShowUsersPage extends LabKeyPage<ShowUsersPage.ElementCache>
{
    public ShowUsersPage(WebDriver driver)
    {
        super(driver);
    }

    public static ShowUsersPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ShowUsersPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("user", containerPath, "showUsers"));
        return new ShowUsersPage(driver.getDriver());
    }

    public DataRegionTable getUsersTable()
    {
        return elementCache().usersTable;
    }

    public PropertiesEditor clickChangeUserProperties()
    {
        waitAndClickAndWait(Locator.linkWithSpan( "Change User Properties"));
        return new PropertiesEditor.PropertiesEditorFinder(getDriver()).withTitle("Field Properties").waitFor();
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        DataRegionTable usersTable = DataRegionTable.DataRegion(getDriver()).withName("Users").findWhenNeeded(getDriver());
    }
}