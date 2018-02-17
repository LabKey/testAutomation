package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class ShowAuditLogPage extends LabKeyPage<ShowAuditLogPage.ElementCache>
{
    public ShowAuditLogPage(WebDriver driver)
    {
        super(driver);
    }

    public static ShowAuditLogPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ShowAuditLogPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("audit", containerPath, "showAuditLog"));
        return new ShowAuditLogPage(driver.getDriver());
    }

    public ShowAuditLogPage selectView(String viewName)
    {
        doAndWaitForPageToLoad(()-> {
                try                 // the selection will cause a page refresh; ignore staleElementReferences in here
                {
                    new Select(elementCache().viewSelect).selectByVisibleText(viewName);
                }catch (StaleElementReferenceException stale){}
            });

        return new ShowAuditLogPage(getDriver());
    }

    public DataRegionTable getLogTable()
    {
        return new DataRegionTable("query", getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {

        WebElement viewSelect = Locator.tagWithName("select", "view")
                .findWhenNeeded(this).withTimeout(4000);


    }
}