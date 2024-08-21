package org.labkey.test.pages.query;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
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

    public InsertExternalSchemaPage setEditable(boolean enable)
    {
        elementCache().editableCheckbox.set(enable);
        return this;
    }

    public InsertExternalSchemaPage setDataSource(String dataSourceName)
    {
        _extHelper.selectComboBoxItem("Data Source:", dataSourceName);
        return this;
    }

    public InsertExternalSchemaPage setSourceSchema(String sourceSchemaName)
    {
        _extHelper.selectComboBoxItem("Database Schema Name :", sourceSchemaName);
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

    public void clickUpdate()
    {
        clickAndWait(elementCache().updateButton);
    }

    public void clickDelete()
    {
        clickAndWait(elementCache().deleteButton);
        clickButton("Delete"); // Confirmation page
    }

    public void clickCancel()
    {
        clickAndWait(elementCache().cancelButton);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        Input userSchemaNameInput = Input.Input(Locator.name("userSchemaName"), getDriver()).findWhenNeeded();
        Input metaDataInput = Input.Input(Locator.name("metaData"), getDriver()).findWhenNeeded();
        Checkbox editableCheckbox = Checkbox.Checkbox(Locator.id("myeditable")).findWhenNeeded(this);
        WebElement createButton = Locator.button("Create").findWhenNeeded(this);
        WebElement updateButton = Locator.button("Update").findWhenNeeded(this);
        WebElement deleteButton = Locator.button("Delete").findWhenNeeded(this);
        WebElement cancelButton = Locator.button("Cancel").findWhenNeeded(this);
    }
}
