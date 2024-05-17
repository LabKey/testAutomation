package org.labkey.test.pages.assay;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfNestedElementLocatedBy;


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
        shortWait().until(presenceOfNestedElementLocatedBy(elementCache().stateSelect, Locator.tagWithAttribute("option", "value")));
        selectOptionByText(elementCache().stateSelect, state);
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

    public AssayRunsPage clickCancel()
    {
        clickButton("cancel");
        return new AssayRunsPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement stateSelect = Locator.id("stateInput").findWhenNeeded(this);
        WebElement commentInput = Locator.textarea("comment").findWhenNeeded(this);
    }
}
