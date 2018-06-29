package org.labkey.test.etl.pages;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * User: tgaluhn
 * Date: 6/21/2018
 */
public class DefinitionPage extends LabKeyPage<DefinitionPage.ElementCache>
{
    private static final String DEFINITION_ID = "etlDefinition";

    public DefinitionPage(WebDriver driver)
    {
        super(driver);
    }

    public DefinitionPage setDefinitionXml(String definitionXml)
    {
        setCodeEditorValue(DEFINITION_ID, definitionXml);
        return new DefinitionPage(getDriver());
    }

    public String getDefintionXml()
    {
        return _extHelper.getCodeMirrorValue(DEFINITION_ID);
    }

    public LabKeyPage save()
    {
        return save(null);
    }

    public LabKeyPage save(@Nullable String expectedError)
    {
        elementCache().saveButton.click();
        if (null != expectedError)
            assertTextPresent(expectedError);

        return new LabKeyPage(getDriver());
    }

    public LabKeyPage cancel()
    {
        elementCache().cancelButton.click();
        return new LabKeyPage(getDriver());
    }

    public DefinitionPage edit()
    {
        elementCache().editButton.click();
        return new DefinitionPage(getDriver());
    }

    public LabKeyPage showGrid()
    {
        elementCache().showGridButton.click();
        return new LabKeyPage(getDriver());
    }

    protected DefinitionPage.ElementCache newElementCache()
    {
        return new DefinitionPage.ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected WebElement name = Locator.id("name").findWhenNeeded(this);
        protected WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
        protected WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
        protected WebElement editButton = Locator.lkButton("Edit").findWhenNeeded(this);
        protected WebElement showGridButton = Locator.lkButton("Show Grid").findWhenNeeded(this);
    }
}
