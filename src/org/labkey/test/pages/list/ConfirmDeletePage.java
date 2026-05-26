package org.labkey.test.pages.list;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


public class ConfirmDeletePage extends LabKeyPage<ConfirmDeletePage.ElementCache>
{
    private String _deleteBtnText;

    public ConfirmDeletePage(WebDriver driver)
    {
        this(driver, "Confirm Delete");
    }

    public ConfirmDeletePage(WebDriver driver, String deleteBtnText)
    {
        super(driver);
        _deleteBtnText = deleteBtnText;
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
        WebElement deleteButton = Locator.lkButton(_deleteBtnText == null ? "Confirm Delete" : _deleteBtnText).findWhenNeeded(this);
    }
}
