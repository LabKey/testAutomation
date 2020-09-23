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

    // TODO: Add a waitForLoad, or some other appropriate check, that the page is loaded. If not here then in one of
    //  the parent classes.
    // Issue 41038: Add a waitForPageLoad in org.labkey.test.pages.experiment.UpdateSampleTypePage

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
