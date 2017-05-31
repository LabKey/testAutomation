package org.labkey.test.pages;

import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Created by RyanS on 5/18/2017.
 */
public class ManageDatasetsPage extends LabKeyPage<ManageDatasetsPage.ElementCache>
{
    public ManageDatasetsPage(WebDriver driver)
    {
        super(driver);
        waitForElement(elementCache().datasetsDataRegion);
        datasetsTable = new DataRegionTable(dataSetTableId, getDriver());
    }

    protected final static String dataSetTableId = "dataregion_datasets";
    protected DataRegionTable datasetsTable;

    public void clickStudySchedule()
    {
        clickAndWait(elementCache().studyScheduleLink);
    }

    public void clickChangeProperties()
    {
        clickAndWait(elementCache().changePropertiesLink);
    }

    public void clickDeleteMultipleDatasets()
    {
        clickAndWait(elementCache().deleteMultipleDatasetsLink);
    }

    public void clickManageDatasetSecurity()
    {
        clickAndWait(elementCache().manageDatasetSecurityLink);
    }

    public DatasetPropertiesPage clickCreateNewDataset()
    {
        clickAndWait(elementCache().createNewDatasetLink);
        return new DatasetPropertiesPage(getDriver());
    }

    public void clickProjectSettings()
    {
        clickAndWait(elementCache().projectSettingsLink);
    }

    public void clickFolderSettings()
    {
        clickAndWait(elementCache().folderSettingsLink);
    }

    public void clickChangeDisplayOrder()
    {
        clickAndWait(elementCache().changeDisplayOrderLink);
    }

    public String getDefaultDateTimeFormat()
    {
        return getText(elementCache().dateTimeFormat);
    }

    public String getDefaultNumberFormat()
    {
        return getText(elementCache().numberFormat);
    }

    public DatasetPropertiesPage selectDatasetByName(String name)
    {
        clickAndWait(findLinkInColWithText("Name",name));
        return new DatasetPropertiesPage(getDriver());
    }

    public DatasetPropertiesPage selectDatasetById(String id)
    {
        clickAndWait(findLinkInColWithText("ID", id));
        return new DatasetPropertiesPage(getDriver());
    }

    public DatasetPropertiesPage selectDatasetByLabel(String label)
    {
        clickAndWait(findLinkInColWithText("Label", label));
        return new DatasetPropertiesPage(getDriver());
    }

    private WebElement findLinkInColWithText(String column, String text)
    {
        int row = datasetsTable.getIndexWhereDataAppears(text, column);
        return datasetsTable.findCell(row,column);
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Locator studyScheduleLink = Locator.linkWithText("Study Schedule");
        Locator changeDisplayOrderLink = Locator.linkWithText("Change Display Order");
        Locator changePropertiesLink = Locator.linkWithText("Change Properties");
        Locator deleteMultipleDatasetsLink = Locator.linkWithText("Delete Multiple Datasets");
        Locator manageDatasetSecurityLink = Locator.linkWithText("Manage Dataset Security");
        Locator createNewDatasetLink = Locator.linkWithText("Create New Dataset");
        Locator folderSettingsLink = Locator.linkWithText("folder settings page");
        Locator projectSettingsLink = Locator.linkWithText("project settings page");
        Locator dateTimeFormat = Locator.xpath("//td[text()='Default date-time format:']/following-sibling::td");
        Locator numberFormat = Locator.xpath("//td[text()='Default number format:']/following-sibling::td");
        Locator datasetsDataRegion = Locator.id(dataSetTableId);
    }
}
