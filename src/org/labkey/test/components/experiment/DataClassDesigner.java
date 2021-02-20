package org.labkey.test.components.experiment;

import org.labkey.test.components.react.ReactSelect;
import org.openqa.selenium.WebDriver;

/**
 * Automates the LabKey ui component defined in: packages/components/src/components/domainproperties/dataclasses/DataClassDesigner.tsx
 * This is a full-page component and should be wrapped by a context-specific page class
 */
public class DataClassDesigner extends EntityTypeDesigner<DataClassDesigner>
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

    public DataClassDesigner setSampleType(String value)
    {
        elementCache().sampleTypeSelect.select(value);
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

    protected class ElementCache extends EntityTypeDesigner.ElementCache
    {
        protected ReactSelect categorySelect = ReactSelect.finder(getDriver()).withId("entity-category").findWhenNeeded(this);
        protected ReactSelect sampleTypeSelect = ReactSelect.finder(getDriver()).withId("entity-sampleSet").findWhenNeeded(this);
    }
}
