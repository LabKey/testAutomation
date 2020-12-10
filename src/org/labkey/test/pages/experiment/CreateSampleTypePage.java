package org.labkey.test.pages.experiment;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.labkey.ui.samples.SampleTypeDesigner;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;


public class CreateSampleTypePage extends SampleTypeDesigner<CreateSampleTypePage>
{
    public CreateSampleTypePage(WebDriver driver)
    {
        super(driver);
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
