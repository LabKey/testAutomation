package org.labkey.test.pages.study.samples;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ShowCreateSampleRequestPage extends LabKeyPage<ShowCreateSampleRequestPage.ElementCache>
{
    public ShowCreateSampleRequestPage(WebDriver driver)
    {
        super(driver);
    }

    public static ShowCreateSampleRequestPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ShowCreateSampleRequestPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("study-samples", containerPath, "showCreateSampleRequest"));
        return new ShowCreateSampleRequestPage(driver.getDriver());
    }

    public ShowCreateSampleRequestPage setDetails(String... values)
    {
        for (int i = 0; i < values.length; i++)
        {
            String value = values[i];
            if (value != null)
            {
                setFormElement(Locator.id("input" + i), value);
            }
        }
        return this;
    }

    public LabKeyPage clickCreateAndReturnToSpecimens()
    {
        clickAndWait(elementCache().createAndReturnButton);

        return new LabKeyPage(getDriver());
    }

    public ManageRequestPage clickCreateAndViewDetails()
    {
        clickAndWait(elementCache().createAndViewButton);

        return new ManageRequestPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement createAndReturnButton = Locator.lkButton("Create And Return To Specimens").findWhenNeeded(this);
        WebElement createAndViewButton = Locator.lkButton("Create And View Details").findWhenNeeded(this);
        WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
    }
}
