package org.labkey.test.pages.experiment;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class UpdateSampleTypePage extends CreateSampleTypePage
{
    public UpdateSampleTypePage(WebDriver driver)
    {
        super(driver);
    }

    public static UpdateSampleTypePage beginAt(WebDriverWrapper driver, Integer sampleTypeId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), sampleTypeId);
    }

    public static UpdateSampleTypePage beginAt(WebDriverWrapper driver, String containerPath, Integer sampleTypeId)
    {
        driver.beginAt(WebTestHelper.buildURL("experiment", containerPath, "editSampleType", Maps.of("RowId", String.valueOf(sampleTypeId))));
        return new UpdateSampleTypePage(driver.getDriver());
    }
}
