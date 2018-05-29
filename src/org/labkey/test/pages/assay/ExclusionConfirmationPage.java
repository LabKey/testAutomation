package org.labkey.test.pages.assay;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ExclusionConfirmationPage extends LabKeyPage
{
    private DataRegionTable excludedRowsDataGrid = null;

    public ExclusionConfirmationPage(WebDriver driver)
    {
        super(driver);
        this.excludedRowsDataGrid = new DataRegionTable.DataRegionFinder(getDriver()).find();

    }

    public ExclusionConfirmationPage setComment(String comment)
    {
        WebElement input = Locator.css("textarea[name=comment]").findElement(getDriver());
        setFormElement(input, comment);
        return this;
    }

    public DataRegionTable getTrackingDataGrid()
    {
        if (excludedRowsDataGrid == null)
            excludedRowsDataGrid = new DataRegionTable.DataRegionFinder(getDriver()).find();
        return excludedRowsDataGrid;
    }

    public void verifyCountAndSave(int exclusionCount, String comment)
    {
        Assert.assertEquals("Exclusion confirmation data row count is not as expected", exclusionCount, getTrackingDataGrid().getDataRowCount());
        setComment(comment).save();
    }

    public void save()
    {
        clickButton("Confirm");
    }
}
