package org.labkey.test.pages.issues;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class UpdatePage extends BaseUpdatePage<BaseUpdatePage.ElementCache>
{
    public UpdatePage(WebDriver driver)
    {
        super(driver);
    }

    public static UpdatePage beginAt(WebDriverWrapper driver, String issueId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), issueId);
    }

    public static UpdatePage beginAt(WebDriverWrapper driver, String containerPath, String issueId)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "update", Maps.of("issueId", issueId)));
        return new UpdatePage(driver.getDriver());
    }

    @Override
    public DetailsPage save()
    {
        clickAndWait(elementCache().saveButton);
        return new DetailsPage(getDriver());
    }

    @Override
    public OptionSelect assignedTo()
    {
        return (OptionSelect) super.assignedTo();
    }
}