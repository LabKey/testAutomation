/*
 * Copyright (c) 2020-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.components.ui.domainproperties.dataclasses;

import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.ui.domainproperties.EntityTypeDesigner;
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
