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
package org.labkey.test.pages;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;

/**
 * Created by RyanS on 5/18/2017.
 */
public class DefineDatasetPage extends LabKeyPage<DefineDatasetPage.ElementCache>
{
    public DefineDatasetPage(WebDriver driver)
    {
        super(driver);
        waitForElement(elementCache().datasetNameText);
    }

    public void setDatasetName(String name)
    {
        setFormElement(elementCache().datasetNameText, name);
    }

    public void setIdAutomatically(boolean automatically)
    {
        if(automatically) checkCheckbox(elementCache().defineDatasetIdAutomatically);
    }

    public void importFromFile(boolean fromFile)
    {
        if(fromFile) checkCheckbox(elementCache().importFromFile);
    }

    public EditDatasetDefinitionPage clickNext()
    {
        clickAndWait(elementCache().nextButton);
        return new EditDatasetDefinitionPage(getDriver());
    }

    public void clickCancel()
    {
        clickAndWait(elementCache().cancelButton);
    }

    public void setDatasetId(String id)
    {
        setIdAutomatically(false);
        setFormElement(elementCache().datasetIdText,id);
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Locator datasetNameText = Locator.input("typeName");
        Locator defineDatasetIdAutomatically = Locator.checkboxByName("autoDatasetId");
        Locator datasetIdText = Locator.input("datasetId");
        Locator importFromFile = Locator.checkboxByName("fileImport");
        Locator nextButton = Locator.lkButton("Next");
        Locator cancelButton = Locator.lkButton("Cancel");
    }
}
