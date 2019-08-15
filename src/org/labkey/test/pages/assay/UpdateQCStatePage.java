package org.labkey.test.pages.assay;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


public class UpdateQCStatePage extends LabKeyPage<UpdateQCStatePage.ElementCache>
{
    public UpdateQCStatePage(WebDriver driver)
    {
        super(driver);
    }


    public DataRegionTable getHistoryTable()
    {
        return DataRegionTable.DataRegion(getDriver()).withName("auditHistory").findWhenNeeded(getDriver());
    }

    public UpdateQCStatePage selectState(String state)
    {
        setFormElement(elementCache().stateInput, state);
        return this;
    }

    public UpdateQCStatePage setComment(String comment)
    {
        setFormElement(elementCache().commentInput, comment);
        return this;
    }

    public AssayRunsPage clickUpdate()
    {
        clickButton("update");
        return new AssayRunsPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {

        WebElement stateInput = Locator.id("stateInput").findWhenNeeded(this);
        WebElement commentInput = Locator.textarea("comment").findWhenNeeded(this);
    }
}
