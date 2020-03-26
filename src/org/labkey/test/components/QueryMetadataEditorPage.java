package org.labkey.test.components;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class QueryMetadataEditorPage extends DomainDesignerPage
{
    public QueryMetadataEditorPage(WebDriver driver)
    {
        super(driver);
    }

    @Override
    public void clickFinish()
    {
        scrollIntoView(elementCache().finishButton());
        shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().finishButton()));
        click(Locator.button(elementCache().finishButton().getText()));
    }

    public void reset()
    {
        scrollIntoView(elementCache().resetButton);
        shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().resetButton));
        elementCache().resetButton.click();
        click(Locator.button("Reset")); // Reset confirmation on the confirm modal
    }

    public void viewData()
    {
        scrollIntoView(elementCache().viewDataButton);
        shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().viewDataButton));
        clickAndWait(elementCache().viewDataButton);
    }

    public void editSource()
    {
        scrollIntoView(elementCache().editSourceButton);
        shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().editSourceButton));
        elementCache().editSourceButton.click();
    }

    public void aliasField()
    {
        scrollIntoView(newElementCache().aliasFieldButton);
        shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().aliasFieldButton));
        elementCache().aliasFieldButton.click();
        click(Locator.button("OK")); //the selected option is the first field
    }

    @Override
    protected QueryMetadataEditorPage.ElementCache elementCache()
    {
        return (QueryMetadataEditorPage.ElementCache) super.elementCache();
    }

    @Override
    protected QueryMetadataEditorPage.ElementCache newElementCache()
    {
        return new QueryMetadataEditorPage.ElementCache();
    }

    public class ElementCache extends DomainDesignerPage.ElementCache
    {
        WebElement resetButton = Locator.button("Reset To Default")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

        WebElement aliasFieldButton = Locator.button("Alias Field")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

        WebElement viewDataButton = Locator.button("View Data")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

        WebElement editSourceButton = Locator.button("Edit Source")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

    }
}
