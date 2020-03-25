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
        scrollIntoView(newElementCache().resetButton());
        shortWait().until(ExpectedConditions.elementToBeClickable(newElementCache().resetButton()));
        click(Locator.button(newElementCache().resetButton().getText()));
        click(Locator.button("Reset")); // Reset confirmation on the confirm modal
    }

    public void viewData()
    {
        scrollIntoView(newElementCache().viewDataButton());
        shortWait().until(ExpectedConditions.elementToBeClickable(newElementCache().viewDataButton()));
        click(Locator.button(newElementCache().viewDataButton().getText()));
    }

    public void editSource()
    {
        scrollIntoView(newElementCache().editSourceButton());
        shortWait().until(ExpectedConditions.elementToBeClickable(newElementCache().editSourceButton()));
        click(Locator.button(newElementCache().editSourceButton().getText()));
    }

    public void aliasField()
    {
        scrollIntoView(newElementCache().aliasFieldButton());
        shortWait().until(ExpectedConditions.elementToBeClickable(newElementCache().aliasFieldButton()));
        click(Locator.button(newElementCache().aliasFieldButton().getText()));
        click(Locator.button("OK")); //the selected option is the first field
    }

    @Override
    protected DomainDesignerPage.ElementCache elementCache()
    {
        return super.elementCache();
    }

    @Override
    protected QueryMetadataEditorPage.ElementCache newElementCache()
    {
        return new QueryMetadataEditorPage.ElementCache();
    }

    public class ElementCache extends DomainDesignerPage.ElementCache
    {
        WebElement resetButton()
        {
            return Locator.button("Reset To Default")
                    .waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        }

        WebElement aliasFieldButton()
        {
            return Locator.button("Alias Field")
                    .waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        }

        WebElement viewDataButton()
        {
            return Locator.button("View Data")
                    .waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        }

        WebElement editSourceButton()
        {
            return Locator.button("Edit Source")
                    .waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        }
    }
}
