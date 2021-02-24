/*
 * Copyright (c) 2015-2017 LabKey Corporation
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
package org.labkey.test.pages.query;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapperImpl;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

public class ExecuteQueryParameterized extends LabKeyPage<ExecuteQueryParameterized.Elements>
{
    public ExecuteQueryParameterized(WebDriver driver)
    {
        super(driver);
    }

    public ExecuteQueryParameterized setParameters(Map<String, String> data)
    {
        for(String key : data.keySet())
        {
            setFormElement(elementCache().findInputField(key), data.get(key));
        }
        return this;
    }

    public DataRegionTable submit()
    {
        clickAndWait(elementCache().submitButton);
        return new DataRegionTable("query", new WebDriverWrapperImpl(getDriver()));
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    protected class Elements extends LabKeyPage<?>.ElementCache
    {
        WebElement findInputField(String fieldKey)
        {
            return Locator.tag("input").attributeEndsWith("name", ".param." + fieldKey).findElement(this);
        }
        WebElement submitButton = new LazyWebElement(Ext4Helper.Locators.ext4Button("Submit"), this);
    }
}
