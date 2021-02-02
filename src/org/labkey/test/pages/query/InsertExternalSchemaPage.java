package org.labkey.test.pages.query;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class InsertExternalSchemaPage extends LabKeyPage<InsertExternalSchemaPage.ElementCache>
{
    public InsertExternalSchemaPage(WebDriver driver)
    {
        super(driver);
    }

    public static InsertExternalSchemaPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static InsertExternalSchemaPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("query", containerPath, "insertExternalSchema"));
        return new InsertExternalSchemaPage(webDriverWrapper.getDriver());
    }

    public InsertExternalSchemaPage setName(String name)
    {
        elementCache().userSchemaNameInput.set(name);
        return this;
    }

    public InsertExternalSchemaPage setDataSource(String dataSourceName)
    {
        _extHelper.selectComboBoxItem("Data Source:", dataSourceName);
        return this;
    }

    public InsertExternalSchemaPage setSourceSchema(String sourceSchemaName)
    {
        elementCache().sourceSchemaNameCombo.set(sourceSchemaName);
        return this;
    }

    public InsertExternalSchemaPage setMetadata(String metadata)
    {
        elementCache().metaDataInput.set(metadata);
        return this;
    }

    public void clickCreate()
    {
        clickAndWait(elementCache().createButton);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        Input userSchemaNameInput = Input.Input(Locator.name("userSchemaName"), getDriver()).findWhenNeeded();
        //Input dataSourceCombo = Input.Input(Locator.name("dataSource"), getDriver()).findWhenNeeded(this);
        Input sourceSchemaNameCombo = Input.Input(Locator.name("sourceSchemaName"), getDriver()).findWhenNeeded(this);
        Input metaDataInput = Input.Input(Locator.name("metaData"), getDriver()).findWhenNeeded();

        WebElement createButton = Locator.button("Create").findWhenNeeded(this);
        WebElement cancelButton = Locator.button("Cancel").findWhenNeeded(this);
    }
}
