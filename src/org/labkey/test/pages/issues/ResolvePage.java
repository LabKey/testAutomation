package org.labkey.test.pages.issues;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class ResolvePage extends BaseUpdatePage<ResolvePage.ElementCache>
{
    public ResolvePage(WebDriver driver)
    {
        super(driver);
    }

    public static ResolvePage beginAt(WebDriverWrapper driver, int issueId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), issueId);
    }

    public static ResolvePage beginAt(WebDriverWrapper driver, String containerPath, int issueId)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "resolve", Maps.of("issueId", String.valueOf(issueId))));
        return new ResolvePage(driver.getDriver());
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

    @Override
    public OptionSelect resolution()
    {
        return (OptionSelect) super.resolution();
    }

    @Override
    public OptionSelect duplicate()
    {
        return (OptionSelect) super.duplicate();
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends UpdatePage.ElementCache
    {
        protected ElementCache()
        {
            resolution = getSelect("resolution");
            duplicate = getSelect("duplicate");
        }
    }
}