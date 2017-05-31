package org.labkey.test.pages;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;

/**
 * Created by RyanS on 5/18/2017.
 */

//Probably not needed, may be a dupe of DatasetPropertiesPage

public class ManageDatasetProperties extends LabKeyPage<ManageDatasetProperties.ElementCache>
{
    public ManageDatasetProperties(WebDriver driver)
    {
        super(driver);
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Locator viewDataBtn = Locator.lkButton("View Data");
        Locator editAssociatedVisitsBtn = Locator.lkButton("Edit Associated Visits");
        Locator manageDatasetsBtn = Locator.lkButton("Manage Datasets");
        Locator deleteDatasetBtn = Locator.lkButton("Delete Dataset");
        Locator deleteAllRowsBtn = Locator.lkButton("Delete All Rows");
    }
}
