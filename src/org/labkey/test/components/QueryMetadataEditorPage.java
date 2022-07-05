package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.domain.DomainDesigner;
import org.labkey.test.components.query.AliasFieldDialog;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

/**
 * Automates the platform component defined in: query/src/client/QueryMetadataEditor/QueryMetadataEditor.tsx
 */
public class QueryMetadataEditorPage extends DomainDesigner<QueryMetadataEditorPage.ElementCache>
{
    public QueryMetadataEditorPage(WebDriver driver)
    {
        super(driver);
    }

    public void resetToDefault()
    {
        elementCache().resetButton.click();
        getWrapper().click(Locator.button("Reset")); // Reset confirmation on the confirm modal
        // TODO: Wait for reset
    }

    public void viewData()
    {
        getWrapper().clickAndWait(elementCache().viewDataButton);
    }

    public void editSource()
    {
        getWrapper().clickAndWait(elementCache().editSourceButton);
    }

    public AliasFieldDialog aliasField()
    {
        elementCache().aliasFieldButton.click();
        return new AliasFieldDialog(this);
    }

    @Override
    public QueryMetadataEditorPage clickSave()
    {
        elementCache().saveButton.click();
        WebDriverWrapper.waitFor(()->  Locator.tagWithClass("div", "alert-success")
                        .containingIgnoreCase("Save Successful").existsIn(this),
                "Expected success message did not appear in time", WAIT_FOR_JAVASCRIPT);
        return this;
    }

    @Override
    protected QueryMetadataEditorPage.ElementCache newElementCache()
    {
        return new QueryMetadataEditorPage.ElementCache();
    }

    public class ElementCache extends DomainDesigner.ElementCache
    {
        private final WebElement resetButton = Locator.button("Reset To Default")
                .findWhenNeeded(buttonPanel).withTimeout(WAIT_FOR_JAVASCRIPT);
        private final WebElement aliasFieldButton = Locator.button("Alias Field")
                .findWhenNeeded(buttonPanel).withTimeout(WAIT_FOR_JAVASCRIPT);
        private final WebElement viewDataButton = Locator.button("View Data")
                .findWhenNeeded(buttonPanel).withTimeout(WAIT_FOR_JAVASCRIPT);
        private final WebElement editSourceButton = Locator.button("Edit Source")
                .findWhenNeeded(buttonPanel).withTimeout(WAIT_FOR_JAVASCRIPT);

        @Override
        protected int getFieldPanelIndex()
        {
            return 0;
        }

        @Override
        protected Locator.XPathLocator buttonPanelLocator()
        {
            return Locator.byClass("query-metadata-editor-buttons");
        }
    }
}
