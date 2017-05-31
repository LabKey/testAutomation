package org.labkey.test.pages;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;

/**
 * Created by RyanS on 5/17/2017.
 */
public class DatasetPropertiesPage extends LabKeyPage<DatasetPropertiesPage.ElementCache>
{
    public DatasetPropertiesPage(WebDriver driver){super(driver);}

    protected DatasetPropertiesPage.ElementCache newElementCache()
    {
        return new DatasetPropertiesPage.ElementCache();
    }

    public EditDatasetDefinitionPage clickEditDefinition()
    {
        waitAndClickAndWait(elementCache().editDefinitionButton);
        return new EditDatasetDefinitionPage(getDriver());
    }

    public ViewDatasetDataPage clickViewData()
    {
        waitAndClickAndWait(elementCache().viewDataButton);
        return new ViewDatasetDataPage(getDriver());
    }

    public ManageDatasetsPage clickManageDatasets()
    {
        clickAndWait(elementCache().manageDatasetsButton);
        return new ManageDatasetsPage(getDriver());
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Locator.XPathLocator viewDataButton = Locator.lkButton("View Data");
        Locator.XPathLocator manageDatasetsButton = Locator.lkButton("Manage Datasets");
        Locator.XPathLocator deleteDatasetButton = Locator.lkButton("Delete Dataset");
        Locator.XPathLocator deleteAllRowsButton = Locator.lkButton("Delete All Rows");
        Locator.XPathLocator showImportHistoryButton = Locator.lkButton("Show Import History");
        Locator.XPathLocator editDefinitionButton = Locator.lkButton("Edit Definition");

        Locator getDatasetPropertyValue(String property)
        {
            return Locator.xpath("//*[preceding-sibling::td='"+property+"'][1]");
        }

        //TODO: Wrap Dataset Fields portion of page in convenience methods
    }
}
