package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
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
        if (!viewName.equals(elementCache().viewSelect.getFirstSelectedOption().getText()))
        {
            doAndWaitForPageToLoad(() -> {
                elementCache().viewSelect.selectByVisibleText(viewName);
            });
            clearCache();
        }
        return this;
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
        Select viewSelect = SelectWrapper.Select(Locator.tagWithName("select", "view")).timeout(4000)
                .findWhenNeeded(this);
    }
}