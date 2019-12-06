package org.labkey.test.pages.assay.plate;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class PlateTemplateList extends LabKeyPage<PlateTemplateList.ElementCache>
{
    public PlateTemplateList(WebDriver driver)
    {
        super(driver);
    }

    public static PlateTemplateList beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static PlateTemplateList beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("plate", containerPath, "plateTemplateList"));
        return new PlateTemplateList(webDriverWrapper.getDriver());
    }

    public PlateDesignerPage clickNewPlate(PlateDesignerPage.PlateDesignerParams params)
    {
        clickAndWait(params.templateListLocator());

        return new PlateDesignerPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement templateTable = Locator.tagWithClass("table", "labkey-data-region-legacy").findWhenNeeded(this);
    }
}
