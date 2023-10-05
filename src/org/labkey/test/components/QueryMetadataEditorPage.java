package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.query.AliasFieldDialog;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

/**
 * Automates the platform component defined in: query/src/client/QueryMetadataEditor/QueryMetadataEditor.tsx
 */
public class QueryMetadataEditorPage extends WebDriverComponent<QueryMetadataEditorPage.ElementCache>
{
    private final WebElement el;
    private final WebDriver driver;

    public QueryMetadataEditorPage(WebDriver driver)
    {
        this.driver = driver;
        el = Locator.id("app").findElement(this.driver); // Full page component
    }

    @Override
    public WebElement getComponentElement()
    {
        return el;
    }

    @Override
    public WebDriver getDriver()
    {
        return driver;
    }

    public DomainFormPanel getFieldsPanel()
    {
        return elementCache().fieldsPanel.expand();
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

    public QueryMetadataEditorPage clickSave()
    {
        elementCache().saveButton.click();
        WebDriverWrapper.waitFor(()->  Locator.tagWithClass("div", "alert-success")
                        .containingIgnoreCase("Save Successful").existsIn(this),
                "Expected success message did not appear in time", WAIT_FOR_JAVASCRIPT);
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    public class ElementCache extends Component<?>.ElementCache
    {
        protected final DomainFormPanel fieldsPanel = new DomainFormPanel.DomainFormPanelFinder(getDriver())
                .timeout(1000).findWhenNeeded();

        protected final WebElement buttonPanel = Locator.byClass("query-metadata-editor-buttons").findWhenNeeded(this);

        private final WebElement resetButton = Locator.button("Reset To Default")
                .findWhenNeeded(buttonPanel).withTimeout(WAIT_FOR_JAVASCRIPT);
        private final WebElement aliasFieldButton = Locator.button("Alias Field")
                .findWhenNeeded(buttonPanel).withTimeout(WAIT_FOR_JAVASCRIPT);
        private final WebElement viewDataButton = Locator.button("View Data")
                .findWhenNeeded(buttonPanel).withTimeout(WAIT_FOR_JAVASCRIPT);
        private final WebElement editSourceButton = Locator.button("Edit Source")
                .findWhenNeeded(buttonPanel).withTimeout(WAIT_FOR_JAVASCRIPT);
        public final WebElement saveButton = Locator.byClass("save-button")
                .findWhenNeeded(buttonPanel).withTimeout(WAIT_FOR_JAVASCRIPT);

    }
}
