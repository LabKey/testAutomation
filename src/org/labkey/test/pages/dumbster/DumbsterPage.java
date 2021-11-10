package org.labkey.test.pages.dumbster;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class DumbsterPage extends LabKeyPage<DumbsterPage.ElementCache>
{
    public DumbsterPage(WebDriver driver)
    {
        super(driver);
    }

    public static DumbsterPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("dumbster", containerPath, "begin"));
        return new DumbsterPage(webDriverWrapper.getDriver());
    }

    @Override
    protected void waitForPage()
    {
        waitFor(()-> elementCache()._emailRecordTable.getComponentElement().isDisplayed(),
                "the page did not render in time", WAIT_FOR_JAVASCRIPT);
    }

    public EmailRecordTable emailTable()
    {
       return elementCache()._emailRecordTable;
    }

    public void clickTitleLink()
    {
        clickAndWait(elementCache().bodyTitleLink);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        EmailRecordTable _emailRecordTable = new EmailRecordTable(getDriver());
        WebElement bodyTitleLink = Locator.tagWithClass("a", "lk-body-title-folder").findWhenNeeded(this);
    }
}
