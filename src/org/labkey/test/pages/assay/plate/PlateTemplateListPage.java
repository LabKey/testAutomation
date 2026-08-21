/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
package org.labkey.test.pages.assay.plate;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * Stub page class. Lacks functionality enabling interaction with the list of existing templates.
 */
public class PlateTemplateListPage extends LabKeyPage<PlateTemplateListPage.ElementCache>
{
    public PlateTemplateListPage(WebDriver driver)
    {
        super(driver);
    }

    public static PlateTemplateListPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static PlateTemplateListPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("plate", containerPath, "plateList"));
        return new PlateTemplateListPage(webDriverWrapper.getDriver());
    }

    public PlateDesignerPage clickNewPlate(PlateDesignerPage.PlateDesignerParams params)
    {
        selectOptionByText(elementCache().templateList, params.templateListOption());
        clickAndWait(Locator.lkButton("create"));

        return new PlateDesignerPage(getDriver());
    }

    public List<String> getTemplateOptions()
    {
        Select select = new Select(elementCache().templateList);
        List<WebElement> selectOptions = select.getOptions();
        return getTexts(selectOptions);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement templateTable = Locator.tagWithClass("table", "labkey-data-region-legacy").findWhenNeeded(this);
        WebElement templateList = Locator.tagWithId("select", "plate_template").findWhenNeeded(this);
    }
}
