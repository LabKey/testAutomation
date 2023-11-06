package org.labkey.test.pages.query;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.domain.BaseDomainDesigner;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public class EditMetadataPage extends BaseDomainDesigner<EditMetadataPage.ElementCache>
{
    public EditMetadataPage(WebDriver driver)
    {
        super(driver);
    }

    public static EditMetadataPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath, String schemaName, String queryName)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("query", containerPath, "metadataQuery",
                Map.of("schemaName", schemaName, "query.queryName", queryName)));
        return new EditMetadataPage (webDriverWrapper.getDriver());
    }

    public DataRegionTable clickViewData(BaseWebDriverTest test, String schemaName, String queryName)
    {
        return test.viewQueryData(schemaName, queryName);
    }

    public SourceQueryPage clickEditSource()
    {
        getWrapper().clickAndWait(elementCache().editSourceBtn);
        return new SourceQueryPage(getDriver());
    }

    @Override
    public EditMetadataPage clickSave()
    {
        elementCache().saveBtn.click();
        waitForSuccess();
        return this;
    }

    public String waitForSuccess()
    {
        return Locator.tagWithClass("div", "alert-success").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT)
                .getText();
    }

    public void resetToDefault()
    {
        WebDriverWrapper.waitFor(elementCache().resetToDefaultBtn::isEnabled,
                "The alias button did not become enabled as expected", 2000);
        elementCache().resetToDefaultBtn.click();
        new ModalDialog(new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Confirm Reset")).dismiss("Reset");
    }

    public EditMetadataPage aliasField(String fieldName)
    {
        WebDriverWrapper.waitFor(elementCache().aliasFieldBtn::isEnabled,
                "The alias button did not become enabled as expected", 2000);
        elementCache().aliasFieldBtn.click();
        ModalDialog dialog = new ModalDialog(new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Choose a field to wrap"));
        ReactSelect.finder(getDriver()).findWhenNeeded(dialog).select(fieldName);
        dialog.dismiss("OK");

        return this;
    }

    public DomainFormPanel fieldsPanel()
    {
        getWrapper().waitForElementToDisappear(Locator.tagWithText("span", "Loading..."));
        return elementCache().firstDomainFormPanel;
    }

    @Override
    protected EditMetadataPage.ElementCache newElementCache()
    {
        return new EditMetadataPage.ElementCache();
    }

    protected class ElementCache extends BaseDomainDesigner<EditMetadataPage.ElementCache>.ElementCache
    {
        protected final WebElement buttonPanel = queryMetadataButtonPanelLocator().findWhenNeeded(this)
                .withTimeout(WAIT_FOR_JAVASCRIPT);
        protected final WebElement aliasFieldBtn = Locator.button("Alias Field").findWhenNeeded(buttonPanel).withTimeout(WAIT_FOR_JAVASCRIPT);
        protected final WebElement editSourceBtn = Locator.button("Edit Source").findWhenNeeded(buttonPanel).withTimeout(WAIT_FOR_JAVASCRIPT);
        protected final WebElement saveBtn = Locator.button("Save").findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        protected final WebElement resetToDefaultBtn = Locator.button("Reset To Default").findWhenNeeded(buttonPanel)
                .withTimeout(WAIT_FOR_JAVASCRIPT);
        DomainFormPanel firstDomainFormPanel = new DomainFormPanel.DomainFormPanelFinder(getDriver())   // for situations where there's only one on the page
                .timeout(WAIT_FOR_JAVASCRIPT)
                .findWhenNeeded(this);

        protected Locator.XPathLocator queryMetadataButtonPanelLocator()
        {
            return Locator.byClass("query-metadata-editor-buttons");
        }

    }
}
