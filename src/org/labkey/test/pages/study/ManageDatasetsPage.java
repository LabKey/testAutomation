package org.labkey.test.pages.study;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.dataset.EditDatasetDefinitionPage;
import org.labkey.test.util.ExperimentalFeaturesHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ManageDatasetsPage extends LabKeyPage<ManageDatasetsPage.ElementCache>
{
    public ManageDatasetsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ManageDatasetsPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ManageDatasetsPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("study", containerPath, "manageTypes"));
        return new ManageDatasetsPage(driver.getDriver());
    }

    public EditDatasetDefinitionPage clickCreateNewDataset()
    {
        ExperimentalFeaturesHelper.enableExperimentalFeature(
                createDefaultConnection(true),"experimental-reactdesigner");
        clickAndWait(elementCache().createNewDataset);

        return new EditDatasetDefinitionPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement createNewDataset = Locator.linkWithText("Create New Dataset").findWhenNeeded(this);
    }
}
