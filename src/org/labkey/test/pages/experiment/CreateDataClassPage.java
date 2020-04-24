/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.pages.experiment;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.labkey.ui.samples.DataClassDesigner;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class CreateDataClassPage extends LabKeyPage<CreateDataClassPage.ElementCache>
{
    public CreateDataClassPage(WebDriver driver)
    {
        super(driver);
    }

    public static CreateDataClassPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static CreateDataClassPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("experiment", containerPath, "editDataClass"));
        return new CreateDataClassPage(driver.getDriver());
    }

    public CreateDataClassPage setName(String name)
    {
        elementCache()._designer.setName(name);
        return this;
    }

    public CreateDataClassPage setDescription(String desc)
    {
        elementCache()._designer.setDescription(desc);
        return this;
    }

    public CreateDataClassPage setNameExpression(String nameExp)
    {
        elementCache()._designer.setNameExpression(nameExp);
        return this;
    }

    public CreateDataClassPage addFields(List<FieldDefinition> fields)
    {
        elementCache()._designer.addFields(fields);
        return this;
    }

    public DomainFormPanel getDomainEditor()
    {
        elementCache();
        return elementCache()._designer.getFieldsPanel();
    }

    public void clickSave()
    {
        elementCache()._designer.clickSave();
    }

    public List<String> clickSaveExpectingErrors()
    {
        return elementCache()._designer.clickSaveExpectingErrors();
    }

    public void clickCancel()
    {
        elementCache()._designer.clickCancel();
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        DataClassDesigner _designer = new DataClassDesigner(getDriver());
    }
}
