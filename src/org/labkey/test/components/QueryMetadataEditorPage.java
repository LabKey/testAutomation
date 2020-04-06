package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.components.query.AliasFieldDialog;
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
        // TODO: Wait for success
    }

    public void reset()
    {
        scrollIntoView(elementCache().resetButton);
        shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().resetButton));
        elementCache().resetButton.click();
        click(Locator.button("Reset")); // Reset confirmation on the confirm modal
        // TODO: Wait for reset
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
        clickAndWait(elementCache().editSourceButton);
    }

    public AliasFieldDialog aliasField()
    {
        elementCache().aliasFieldButton.click();
        return new AliasFieldDialog(this);
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
        final WebElement resetButton = Locator.button("Reset To Default")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

        final WebElement aliasFieldButton = Locator.button("Alias Field")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

        final WebElement viewDataButton = Locator.button("View Data")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

        final WebElement editSourceButton = Locator.button("Edit Source")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

    }
}
