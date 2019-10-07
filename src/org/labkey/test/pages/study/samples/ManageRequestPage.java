package org.labkey.test.pages.study.samples;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ManageRequestPage extends LabKeyPage<ManageRequestPage.ElementCache>
{
    public ManageRequestPage(WebDriver driver)
    {
        super(driver);
    }

    public static ManageRequestPage beginAt(WebDriverWrapper driver, int id)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), id);
    }

    public static ManageRequestPage beginAt(WebDriverWrapper driver, String containerPath, int id)
    {
        driver.beginAt(WebTestHelper.buildURL("study-samples", containerPath, "manageRequest", Maps.of("id", String.valueOf(id))));
        return new ManageRequestPage(driver.getDriver());
    }

    public ManageRequestPage submitRequest()
    {
        doAndAcceptUnloadAlert(() -> elementCache().submitButton.click(), "Once a request is submitted, its specimen list may no longer be modified.");
        return new ManageRequestPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement submitButton = Locator.lkButton("Submit Request").findWhenNeeded(this);
        WebElement cancelButton = Locator.lkButton("Cancel Request").findWhenNeeded(this);
        WebElement specimenSearchButton = Locator.lkButton("Specimen Search").findWhenNeeded(this);
        WebElement uploadSpecimenIdsButton = Locator.lkButton("Upload Specimen Ids").findWhenNeeded(this);
    }
}
