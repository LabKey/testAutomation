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

    /**
     * This is a short-term workaround to the fact that our 'clickAndWait' function is not app-aware, and
     * always assumes that a click will cause a page load event.  In Single-Page Apps like SM or Biologics,
     * page-load is only expected when the source and the destination URLs aren't both in the app
     * @param expectPageLoad
     */
    public void clickSave(boolean expectPageLoad)
    {
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().saveButton));
        if(expectPageLoad)
            clickSave();
        else
            elementCache().saveButton.click();
    }

    @Override
    protected CreateSampleTypePage getThis()
    {
        return this;
    }
}
