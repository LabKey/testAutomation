package org.labkey.test.pages.experiment;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.labkey.ui.samples.SampleTypeDesigner;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;


public class CreateSampleSetPage extends SampleTypeDesigner<CreateSampleSetPage>
{
    public CreateSampleSetPage(WebDriver driver)
    {
        super(driver);
    }

    public static CreateSampleSetPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static CreateSampleSetPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("experiment", containerPath, "createSampleSet"));
        return new CreateSampleSetPage(driver.getDriver());
    }

    @Override
    protected CreateSampleSetPage getThis()
    {
        return this;
    }
}
