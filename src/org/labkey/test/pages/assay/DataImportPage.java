/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.pages.assay;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebElement;

import java.io.File;


public class DataImportPage extends LabKeyPage<DataImportPage.Elements>
{
    public DataImportPage(BaseWebDriverTest test)
    {
        super(test);
    }

    public void insertTsvData(String data)
    {
        setFormElement(elementCache().inputTsvField, data);
        clickButton("Submit");
    }

    public File downloadTemplate()
    {
        return clickAndWaitForDownload(elementCache().downloadTemplateButton);
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    public class Elements extends LabKeyPage.ElementCache
    {

        final RadioButton uploadFileButton = new RadioButton(
                new LazyWebElement(Locator.radioButtonById("FileUpload"), this));
        final WebElement inputTsvField = new LazyWebElement(Locator.xpath(".//textarea[@id='tsv3']"), this);

        //final WebElement formatSelect = new LazyWebElement(Locator.xpath("//[]"))
        final WebElement importByAltKeyCheckbox = new LazyWebElement(Locator.checkboxByName("importLookupByAlternateKey"), this);
        final WebElement downloadTemplateButton = new LazyWebElement(Locator.lkButton("Download Template"), this);
        final WebElement submitButton = new LazyWebElement(Locator.lkButton("Submit"), this);
        final WebElement cancelButton = new LazyWebElement(Locator.lkButton("Cancel"), this);
    }
}
