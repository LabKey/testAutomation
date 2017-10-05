package org.labkey.test.pages.study;

import org.labkey.test.Locator;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.EditDatasetDefinitionPage;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class CreateDatasetPage extends LabKeyPage<CreateDatasetPage.ElementCache>
{
    public CreateDatasetPage(WebDriver driver)
    {
        super(driver);
    }

    public CreateDatasetPage setName(String name)
    {
        elementCache().nameInput.set(name);
        return this;
    }

    public EditDatasetDefinitionPage submit()
    {
        clickAndWait(elementCache().nextButton);
        return new EditDatasetDefinitionPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Input nameInput = Input.Input(Locator.name("typeName"), getDriver()).findWhenNeeded(this);
        WebElement nextButton = Locator.lkButton("Next").findWhenNeeded(this);
    }
}