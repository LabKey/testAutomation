/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.test.pages.study;

import org.labkey.test.Locator;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.EditDatasetDefinitionPage;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class CreateDatasetPage extends LabKeyPage<CreateDatasetPage.ElementCache>
{
    public CreateDatasetPage(WebDriver driver)
    {
        super(driver);
    }

    public CreateDatasetPage setName(String name)
    {
        elementCache().nameInput.set(name);
        return this;
    }

    public EditDatasetDefinitionPage submit()
    {
        clickAndWait(elementCache().nextButton);
        return new EditDatasetDefinitionPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Input nameInput = Input.Input(Locator.name("typeName"), getDriver()).findWhenNeeded(this);
        WebElement nextButton = Locator.lkButton("Next").findWhenNeeded(this);
    }
}