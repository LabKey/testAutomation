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

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.domain.DomainDesigner;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.DataClassDefinition;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

/**
 * This currently wraps two pages but the product will be refactored to combine them sometime soon.
 * Will likely be structured similarly to {@link CreateSampleSetPage}
 */
public class InsertDataClassPage extends DomainDesigner<InsertDataClassPage.ElementCache>
{
    public InsertDataClassPage(WebDriver driver)
    {
        super(driver);
    }

    public static InsertDataClassPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static InsertDataClassPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("experiment", containerPath, "insertDataClass"));
        return new InsertDataClassPage(driver.getDriver());
    }

    public static void createDataClass(DataClassDefinition def, String containerPath, WebDriverWrapper driver)
    {
        InsertDataClassPage insertPage = beginAt(driver, containerPath);
        insertPage.setName(def.getName());
        if (def.getDescription() != null)
            insertPage.setDescription(def.getDescription());
        if (def.getNameExpression() != null)
            insertPage.setNameExpression(def.getNameExpression());
        if (def.getMaterialSource() != null)
            insertPage.selectMaterialSourceName(def.getMaterialSource());
        insertPage.addFields(def.getFields());
    }

    public InsertDataClassPage setName(String name)
    {
        //expandPropertiesPanel();
        elementCache().nameInput.set(name);
        return this;
    }

    public InsertDataClassPage setDescription(String desc)
    {
        //expandPropertiesPanel();
        elementCache().descriptionInput.set(desc);
        return this;
    }

    public InsertDataClassPage setNameExpression(String nameExp)
    {
        //expandPropertiesPanel();
        elementCache().nameExpressionInput.set(nameExp);
        return this;
    }

    public InsertDataClassPage selectMaterialSourceId(Integer matSrcId)
    {
        //expandPropertiesPanel();
        elementCache().materialSourceSelect.selectByValue(matSrcId.toString());
        return this;
    }

    public InsertDataClassPage selectMaterialSourceName(String matSrcName)
    {
        //expandPropertiesPanel();
        elementCache().materialSourceSelect.selectByVisibleText(matSrcName);
        return this;
    }

    public InsertDataClassPage addFields(List<FieldDefinition> fields)
    {
        clickCreate(); // TODO: Remove for single-page product implementation

        DomainFormPanel fieldsPanel = getFieldsPanel();
        for (FieldDefinition field : fields)
        {
            fieldsPanel.addField(field);
        }
        return this;
    }

    @Deprecated
    public void clickCreate()
    {
        getWrapper().clickAndWait(elementCache().createBtn);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends DomainDesigner.ElementCache
    {
        Input nameInput = Input.Input(Locator.tagWithName("input","name"), getDriver())
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);
        Input descriptionInput = Input.Input(Locator.tagWithName("input","description"), getDriver())
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);
        Input nameExpressionInput = Input.Input(Locator.tagWithName("input","nameExpression"), getDriver())
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);
        Select materialSourceSelect = SelectWrapper.Select(Locator.tagWithName("select","materialSourceId"))
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);

        WebElement createBtn = Locator.lkButton("Create").findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);

        // TODO: Remove once properties and fields are defined on the same page
        @Override
        protected int getFieldPanelIndex()
        {
            return 0;
        }
    }
}
