package org.labkey.test.components.labkey.ui.samples;

import org.labkey.test.Locator;
import org.labkey.test.components.glassLibrary.components.ReactSelect;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Automates the LabKey ui component defined in: packages/components/src/components/domainproperties/dataclasses/DataClassDesigner.tsx
 * This is a full-page component and should be wrapped by a context-specific page class
 */
public class DataClassDesigner extends EntityTypeDesigner
{
    public DataClassDesigner(WebDriver driver)
    {
        super(driver);
    }

    public DataClassDesigner setCategory(String value)
    {
         elementCache().categorySelect.select(value);
        return getThis();
    }

    public DataClassDesigner setSampleSet(String value)
    {
        elementCache().sampleSetSelect.select(value);
        return getThis();
    }

    @Override
    protected DataClassDesigner getThis()
    {
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return  (ElementCache) super.elementCache();
    }

    public void expandPropertiesPanel()
    {
        elementCache().propertiesPanelHeader.click();
    }

    protected class ElementCache extends EntityTypeDesigner.ElementCache
    {
        protected final WebElement propertiesPanelHeader = Locator.id("dataclass-properties-hdr").findWhenNeeded(this);
        protected ReactSelect categorySelect = ReactSelect.finder(getDriver()).withId("entity-category").findWhenNeeded(this);
        protected ReactSelect sampleSetSelect = ReactSelect.finder(getDriver()).withId("entity-sampleSet").findWhenNeeded(this);
    }
}
