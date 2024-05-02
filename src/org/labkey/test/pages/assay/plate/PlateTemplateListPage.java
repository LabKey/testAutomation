package org.labkey.test.pages.assay.plate;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * Stub page class. Lacks functionality enabling interaction with the list of existing templates.
 */
public class PlateTemplateListPage extends LabKeyPage<PlateTemplateListPage.ElementCache>
{
    public PlateTemplateListPage(WebDriver driver)
    {
        super(driver);
    }

    public static PlateTemplateListPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static PlateTemplateListPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("plate", containerPath, "plateList"));
        return new PlateTemplateListPage(webDriverWrapper.getDriver());
    }

    public PlateDesignerPage clickNewPlate(PlateDesignerPage.PlateDesignerParams params)
    {
        selectOptionByText(elementCache().templateList, params.templateListOption());
        clickAndWait(Locator.lkButton("create"));

        return new PlateDesignerPage(getDriver());
    }

    public List<String> getTemplateOptions()
    {
        Select select = new Select(elementCache().templateList);
        List<WebElement> selectOptions = select.getOptions();
        return getTexts(selectOptions);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement templateTable = Locator.tagWithClass("table", "labkey-data-region-legacy").findWhenNeeded(this);
        WebElement templateList = Locator.tagWithId("select", "plate_template").findWhenNeeded(this);
    }
}
