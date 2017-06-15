package org.labkey.test.pages;

import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Created by RyanS on 5/18/2017.
 */
public class ViewDatasetDataPage extends LabKeyPage<ViewDatasetDataPage.ElementCache>
{
    public ViewDatasetDataPage(WebDriver driver)
    {
        super(driver);
        waitForElement(DataRegionTable.Locators.dataRegion(dataRegionName));
        _dataRegionTable = new DataRegionTable(dataRegionName, getDriver());
    }
    private static final String dataRegionName = "Dataset";
    protected DataRegionTable _dataRegionTable;

    //public String getQCStates = elementCache().QCStates.getText();

    public DatasetInsertPage insertDatasetRow()
    {
        _dataRegionTable.clickInsertNewRowDropdown();
        return new DatasetInsertPage(getDriver(), elementCache().datasetNavLink.getText());
    }

    public ImportDataPage importBulkData()
    {
        _dataRegionTable.clickImportBulkDataDropdown();
        return new ImportDataPage(getDriver());
    }

    public DatasetPropertiesPage clickManageDataset()
    {
        clickAndWait(elementCache().manageDataset);
        return new DatasetPropertiesPage(getDriver());
    }

    public List<String> getColumnData(String columnName)
    {
        return _dataRegionTable.getColumnDataAsText(columnName);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement QCStates = Locator.xpath("//span[b[text()='QC States:']]").findWhenNeeded(this);
        WebElement datasetNavLink = Locator.xpath("//span[@id='navTrailAncestors']/a").findWhenNeeded(this);
        WebElement manageDataset = Locator.lkButton("Manage").findWhenNeeded(this);
    }
}
