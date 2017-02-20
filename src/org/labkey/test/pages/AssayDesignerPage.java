/*
 * Copyright (c) 2014-2016 LabKey Corporation
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

    @Deprecated
    public AssayDesignerPage(BaseWebDriverTest test)
    {
        this(test.getDriver());
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

    /**
     * File-based assay domains match the names of the xml generating them
     * So we need a generalized method for accessing
     * @param domainTitle text
     * @return
     */
    public PropertiesEditor fields(String domainTitle)
    {
        return PropertiesEditor(getDriver()).withTitle(domainTitle).findWhenNeeded();
    }

    @Deprecated
    public void addBatchField(String name, @Nullable String label, @Nullable String type)
    {
        final String xpathSection = "//tbody//td[contains(text(), 'Batch Fields')]/./parent::tr/./following-sibling::tr/./descendant::";
        addField(xpathSection, name, label, type);
    }

    @Deprecated
    public void removeBatchField(String name)
    {
        final String xpathSection = "//tbody//td[contains(text(), 'Batch Fields')]/./parent::tr/./following-sibling::tr/./descendant::";
        removeField(xpathSection, name);
    }

    @Deprecated
    public void addRunField(String name, @Nullable String label, @Nullable String type)
    {
        final String xpathSection = "//tbody//td[contains(text(), 'Run Fields')]/./parent::tr/./following-sibling::tr/./descendant::";
        addField(xpathSection, name, label, type);
    }

    @Deprecated
    public void removeRunField(String name)
    {
        final String xpathSection = "//tbody//td[contains(text(), 'Run Fields')]/./parent::tr/./following-sibling::tr/./descendant::";
        removeField(xpathSection, name);
    }

    @Deprecated
    public void addDataField(String name, @Nullable String label, @Nullable String type)
    {
        final String xpathSection = "//tbody//td[contains(text(), 'Data Fields')]/./parent::tr/./following-sibling::tr/./descendant::";
        addField(xpathSection, name, label, type);
    }

    @Deprecated
    public void removeDataField(String name)
    {
        final String xpathSection = "//tbody//td[contains(text(), 'Data Fields')]/./parent::tr/./following-sibling::tr/./descendant::";
        removeField(xpathSection, name);
    }

    @Deprecated
    private void addField(String xpathSection, String name, @Nullable String label, @Nullable String type)
    {
        List<WebElement> inputBoxes;

        Locator.xpath(xpathSection + "span[contains(@id, 'button_Add Field')]").findElement(getDriver()).click();

        inputBoxes = Locator.xpath(xpathSection + "input[contains(@id, '-input') and starts-with(@id, 'name')]").findElements(getDriver());
        setFormElement(inputBoxes.get(inputBoxes.size() - 1), name);

        if(label != null)
        {
            inputBoxes = Locator.xpath(xpathSection + "input[contains(@id, '-input') and starts-with(@id, 'label')]").findElements(getDriver());
            setFormElement(inputBoxes.get(inputBoxes.size() - 1), label);
        }

        if(type != null)
        {
            inputBoxes = Locator.xpath(xpathSection + "input[starts-with(@name, 'ff_type')]/./following-sibling::div").findElements(getDriver());
            inputBoxes.get(inputBoxes.size() - 1).click();
            click(Locator.xpath("//div[contains(@class, 'x-window')]//div[contains(@class, 'x-window-bwrap')]//table//tr//label[contains(text(), '" + type + "')]"));
            click(Locator.xpath("//button[contains(@class, 'x-btn-text')][contains(text(), 'Apply')]"));
        }
    }

    @Deprecated
    public void removeField(String xpathSection, String name)
    {
        final String xpathDelete1 = "input[contains(@id, '-input') and starts-with(@id, 'name')][";
        final String xpathDelete2 = "]/./ancestor::tr[contains(@class, 'editor-field-row')]/./descendant::div[contains(@id, 'partdelete')]";
        List<WebElement> inputBoxes;
        WebElement theBox = null;
        int index = 1;

        inputBoxes = Locator.xpath(xpathSection + "input[contains(@id, '-input') and starts-with(@id, 'name') and contains(@value, '')]").findElements(getDriver());
        for(WebElement we : inputBoxes)
        {
            if(we.getAttribute("value").trim().toLowerCase().equals(name.trim().toLowerCase()))
            {
                theBox = we;
                break;
            }
            else
            {
                index++;
            }
        }

        if(theBox != null)
        {
            Locator.xpath(xpathSection + xpathDelete1 + index + xpathDelete2).findElement(getDriver()).click();
            clickButton("OK", 0);
        }

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
        final OptionSelect<MetadataInputFormat> metadataInputSelect = OptionSelect(Locator.id("metadataInputFormat")).findWhenNeeded(this);
        final Checkbox debugScriptCheckbox = Checkbox(Locator.checkboxByName("debugScript")).findWhenNeeded(this);
        final Checkbox editableRunsCheckbox = Checkbox(Locator.checkboxByName("editableRunProperties")).findWhenNeeded(this);
        final Checkbox editableResultCheckbox = Checkbox(Locator.checkboxByName("editableResultProperties")).findWhenNeeded(this);
        final Checkbox backgroundUploadCheckbox = Checkbox(Locator.checkboxByName("backgroundUpload")).findWhenNeeded(this);


        final PropertiesEditor batchFieldsPanel = PropertiesEditor(getDriver()).withTitle("Batch Fields").findWhenNeeded();
        final PropertiesEditor runFieldsPanel = PropertiesEditor(getDriver()).withTitle("Run Fields").findWhenNeeded();
        final PropertiesEditor dataFieldsPanel = PropertiesEditor(getDriver()).withTitle("Data Fields").findWhenNeeded();
    }
}
