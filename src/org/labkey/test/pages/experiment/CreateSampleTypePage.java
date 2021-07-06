package org.labkey.test.pages.experiment;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ui.domainproperties.samples.SampleTypeDesigner;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;

public class CreateSampleTypePage extends SampleTypeDesigner<CreateSampleTypePage>
{
    public CreateSampleTypePage(WebDriver driver)
    {
        super(driver);

        // Addressing Issue 41038: Add a waitForPageLoad in org.labkey.test.pages.experiment.UpdateSampleTypePage
        WebDriverWrapper.waitFor(()-> {
                    try
                    {
                        // There should be at least two panels for a sample type a 'general properties' panel and a 'fields' panel.
                        // Wait until the headers for these two panels are there before returning.
                        return Locator
                                .tagWithClassContaining("div", "domain-panel-header")
                                .findElements(this).size() >= 2;
                    }
                    catch(NoSuchElementException nse)
                    {
                        return false;
                    }
                },
                "Did not find the 'General Properties' and 'Fields' panels for the SampleType page.",
                1_000);


    }

    public static CreateSampleTypePage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static CreateSampleTypePage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("experiment", containerPath, "editSampleType"));
        return new CreateSampleTypePage(driver.getDriver());
    }

    @Override
    protected CreateSampleTypePage getThis()
    {
        return this;
    }
}
