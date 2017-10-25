/*
 * Copyright (c) 2014-2017 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.labkey.test.components.PropertiesEditor.PropertiesEditor;
import static org.labkey.test.components.html.Checkbox.Checkbox;
import static org.labkey.test.components.html.Input.Input;
import static org.labkey.test.components.html.OptionSelect.OptionSelect;
import static org.labkey.test.components.html.SelectWrapper.Select;

public class AssayDesignerPage extends BaseDesignerPage<AssayDesignerPage.ElementCache>
{
    public AssayDesignerPage(WebDriver driver)
    {
        super(driver);
    }

    @Override
    public void waitForReady()
    {
        super.waitForReady();
        elementCache().descriptionInput.get();
    }

    @Override
    public AssayDesignerPage save()
    {
        return (AssayDesignerPage) super.save();
    }

    public AssayDesignerPage setName(String name)
    {
        elementCache().nameInput.set(name);
        fireEvent(elementCache().nameInput.getComponentElement(), BaseWebDriverTest.SeleniumEvent.change);
        return this;
    }

    public AssayDesignerPage setDescription(String description)
    {
        doAndExpectDirty(() ->
                elementCache().descriptionInput.set(description));
        return this;
    }

    public AssayDesignerPage setAutoCopyData(boolean checked)
    {
        elementCache().autoCopyCheckbox.set(checked);
        return this;
    }

    public AssayDesignerPage setAutoCopyTarget(String containerPath)
    {
        elementCache().autoCopyTargetSelect.selectByVisibleText(containerPath);
        return this;
    }

    public AssayDesignerPage addTransformScript(File transformScript)
    {
        int index = getElementCount(Locator.xpath("//input[starts-with(@id, 'AssayDesignerTransformScript')]"));
        click(Locator.lkButton("Add Script"));

        return setTransformScript(transformScript, index);
    }

    public AssayDesignerPage setTransformScript(File transformScript)
    {
        return setTransformScript(transformScript, 0);
    }

    public AssayDesignerPage setTransformScript(File transformScript, int index)
    {
        assertTrue("Unable to locate the transform script: " + transformScript, transformScript.exists());

        setFormElement(Locator.xpath("//input[@id='AssayDesignerTransformScript" + index + "']"), transformScript.getAbsolutePath());
        return this;
    }

    public AssayDesignerPage setPlateTemplate(String template)
    {
        elementCache().plateTemplateSelect.selectByVisibleText(template);
        return this;
    }

    public AssayDesignerPage setDetectionMethod(String method)
    {
        elementCache().detectionMethodSelect.selectByVisibleText(method);
        return this;
    }

    public AssayDesignerPage setMetaDataInputFormat(MetadataInputFormat format)
    {
        elementCache().metadataInputSelect.selectOption(format);
        return this;
    }

    public AssayDesignerPage setSaveScriptData(boolean checked)
    {
        elementCache().debugScriptCheckbox.set(checked);
        return this;
    }

    public AssayDesignerPage setEditableRuns(boolean checked)
    {
        elementCache().editableRunsCheckbox.set(checked);
        return this;
    }

    public AssayDesignerPage setEditableResults(boolean checked)
    {
        elementCache().editableResultCheckbox.set(checked);
        return this;
    }

    public AssayDesignerPage setBackgroundImport(boolean checked)
    {
        elementCache().backgroundUploadCheckbox.set(checked);
        return this;
    }

    public PropertiesEditor batchFields()
    {
        return elementCache().batchFieldsPanel;
    }

    public PropertiesEditor runFields()
    {
        return elementCache().runFieldsPanel;
    }

    public PropertiesEditor dataFields()
    {
        return elementCache().dataFieldsPanel;
    }

    public AssayDesignerPage addBatchField(String name, @Nullable String label, @Nullable String type)
    {
        return addBatchField(name, label, FieldDefinition.ColumnType.valueOf(type));
    }

    public AssayDesignerPage addBatchField(String name, @Nullable String label, @Nullable FieldDefinition.ColumnType type)
    {
        batchFields().addField(new FieldDefinition(name).setLabel(label).setType(type));
        return this;
    }

    public void removeBatchField(String name)
    {
        batchFields().selectField(name).markForDeletion();
    }

    public AssayDesignerPage addRunField(String name, @Nullable String label, @Nullable String type)
    {
        return addRunField(name, label, FieldDefinition.ColumnType.valueOf(type));
    }

    public AssayDesignerPage addRunField(String name, @Nullable String label, @Nullable FieldDefinition.ColumnType type)
    {
        runFields().addField(new FieldDefinition(name).setLabel(label).setType(type));
        return this;
    }

    public void removeRunField(String name)
    {
        runFields().selectField(name).markForDeletion();
    }

    public AssayDesignerPage addDataField(String name, @Nullable String label, @Nullable String type)
    {
        return addDataField(name, label, FieldDefinition.ColumnType.valueOf(type));
    }

    public AssayDesignerPage addDataField(String name, @Nullable String label, @Nullable FieldDefinition.ColumnType type)
    {
        dataFields().addField(new FieldDefinition(name).setLabel(label).setType(type));
        return this;
    }

    @Override
    public LabKeyPage saveAndClose()
    {
        super.saveAndClose();
        waitForElement(Locator.css("table.labkey-data-region")); // 'Runs' or 'AssayList'
        return null;
    }

    public enum MetadataInputFormat implements OptionSelect.SelectOption
    {
        MANUAL,
        FILE_BASED,
        COMBINED;

        @Override
        public String getValue()
        {
            return name();
        }

        @Override
        public String getText()
        {
            return null;
        }
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    public class ElementCache extends BaseDesignerPage.ElementCache
    {
        final Input nameInput = Input(Locator.id("AssayDesignerName"), getDriver()).findWhenNeeded(this);
        final Input descriptionInput = Input(Locator.id("AssayDesignerDescription"), getDriver()).findWhenNeeded(this);
        final Checkbox autoCopyCheckbox = Checkbox(Locator.checkboxByName("autoCopy")).findWhenNeeded(this);
        final Select autoCopyTargetSelect = Select(Locator.id("autoCopyTarget")).findWhenNeeded(this);
        final Select plateTemplateSelect = Select(Locator.id("plateTemplate")).findWhenNeeded(this);
        final Select detectionMethodSelect = Select(Locator.id("detectionMethod")).findWhenNeeded(this);
        final OptionSelect<MetadataInputFormat> metadataInputSelect = OptionSelect.finder(Locator.id("metadataInputFormat"), MetadataInputFormat.class).findWhenNeeded(this);
        final Checkbox debugScriptCheckbox = Checkbox(Locator.checkboxByName("debugScript")).findWhenNeeded(this);
        final Checkbox editableRunsCheckbox = Checkbox(Locator.checkboxByName("editableRunProperties")).findWhenNeeded(this);
        final Checkbox editableResultCheckbox = Checkbox(Locator.checkboxByName("editableResultProperties")).findWhenNeeded(this);
        final Checkbox backgroundUploadCheckbox = Checkbox(Locator.checkboxByName("backgroundUpload")).findWhenNeeded(this);


        final PropertiesEditor batchFieldsPanel = PropertiesEditor(getDriver()).withTitleContaining("Batch Fields").findWhenNeeded();
        final PropertiesEditor runFieldsPanel = PropertiesEditor(getDriver()).withTitleContaining("Run Fields").findWhenNeeded();
        final PropertiesEditor dataFieldsPanel = PropertiesEditor(getDriver()).withTitleContaining("Data Fields").findWhenNeeded();
    }
}
