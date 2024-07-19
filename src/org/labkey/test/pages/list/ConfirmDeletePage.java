package org.labkey.test.pages.list;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


public class ConfirmDeletePage extends LabKeyPage<ConfirmDeletePage.ElementCache>
{
    public ConfirmDeletePage(WebDriver driver)
    {
        super(driver);
    }

    public BeginPage confirmDelete()
    {
        clickAndWait(elementCache().deleteButton);
        return new BeginPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        WebElement deleteButton = Locator.lkButton("Confirm Delete").findWhenNeeded(this);
    }
}
