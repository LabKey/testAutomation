package org.labkey.test.pages.study.specimen;

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

    public static ShowCreateSampleRequestPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static ShowCreateSampleRequestPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("study-samples", containerPath, "showCreateSampleRequest"));
        return new ShowCreateSampleRequestPage(webDriverWrapper.getDriver());
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
        WebElement createAndReturnButton = Locator.lkButton("Create and Return To Specimens").findWhenNeeded(this);
        WebElement createAndViewButton = Locator.lkButton("Create and View Details").findWhenNeeded(this);
        WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
    }
}
