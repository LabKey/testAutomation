package org.labkey.test.pages.experiment;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class UpdateSampleSetPage extends CreateSampleSetPage
{
    public UpdateSampleSetPage(WebDriver driver)
    {
        super(driver);
    }

    public static UpdateSampleSetPage beginAt(WebDriverWrapper driver, Integer sampleSetId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), sampleSetId);
    }

    public static UpdateSampleSetPage beginAt(WebDriverWrapper driver, String containerPath, Integer sampleSetId)
    {
        driver.beginAt(WebTestHelper.buildURL("experiment", containerPath, "editSampleSet", Maps.of("RowId", String.valueOf(sampleSetId))));
        return new UpdateSampleSetPage(driver.getDriver());
    }
}
