package org.labkey.test.pages.assay;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebElement;

/**
 * wraps the 'confirm delete' page for assay run(s)
 */
public class DeleteSelectedRunsPage extends LabKeyPage<DeleteSelectedRunsPage.Elements>
{
    public DeleteSelectedRunsPage(BaseWebDriverTest test)
    {
        super(test);
    }

    public void clickOK()
    {
        doAndWaitForPageToLoad(()-> newElementCache().okButton.click());
    }

    public void clickCancel()
    {
        doAndWaitForPageToLoad(()-> newElementCache().cancelButton.click());
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    public class Elements extends LabKeyPage.ElementCache
    {
        final WebElement okButton = new LazyWebElement(Locator.lkButton("OK"), this);
        final WebElement cancelButton = new LazyWebElement(Locator.lkButton("Cancel"), this);
    }
}
