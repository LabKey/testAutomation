/*
 * Copyright (c) 2014-2015 LabKey Corporation
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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class AssayDomainEditor extends DomainEditor
{
    public AssayDomainEditor(BaseWebDriverTest test)
    {
        super(test);
    }

    @Override
    public void waitForReady()
    {
        super.waitForReady();
        _test.waitForElement(Locator.id("AssayDesignerDescription"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void setName(String name)
    {
        _test.setFormElement(Locator.id("AssayDesignerName"), name);
        _test.fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), BaseWebDriverTest.SeleniumEvent.change);
    }

    public void setDescription(String description)
    {
        _test.setFormElement(Locator.id("AssayDesignerDescription"), description);
    }

    public void setAutoCopyData(boolean set)
    {
        if (set)
            _test.checkCheckbox(Locator.checkboxByName("autoCopy"));
        else
            _test.uncheckCheckbox(Locator.checkboxByName("autoCopy"));
    }

    public void setAutoCopyTarget(String containerPath)
    {
        _test.selectOptionByText(Locator.id("autoCopyTarget"), containerPath);
    }

    public void addTransformScript(File transformScript)
    {
        int index = _test.getElementCount(Locator.xpath("//input[starts-with(@id, 'AssayDesignerTransformScript')]"));
        _test.click(Locator.lkButton("Add Script"));

        setTransformScript(transformScript, index);
    }

    public void setTransformScript(File transformScript)
    {
        setTransformScript(transformScript, 0);
    }

    public void setTransformScript(File transformScript, int index)
    {
        assertTrue("Unable to locate the transform script: " + transformScript, transformScript.exists());

        _test.setFormElement(Locator.xpath("//input[@id='AssayDesignerTransformScript" + index + "']"), transformScript.getAbsolutePath());
    }

    public void setPlateTemplate(String template)
    {
        _test.selectOptionByText(Locator.id("plateTemplate"), template);
    }

    public void setDetectionMethod(String method)
    {
        _test.selectOptionByText(Locator.id("detectionMethod"), method);
    }

    public void setMetaDataInputFormat(MetadataInputFormat format)
    {
        _test.selectOptionByValue(Locator.id("metadataInputFormat"), format.name());
    }

    public void setSaveScriptData(boolean set)
    {
        if (set)
            _test.checkCheckbox(Locator.checkboxByName("debugScript"));
        else
            _test.uncheckCheckbox(Locator.checkboxByName("debugScript"));
    }

    public void setEditableRuns(boolean set)
    {
        if (set)
            _test.checkCheckbox(Locator.checkboxByName("editableRunProperties"));
        else
            _test.uncheckCheckbox(Locator.checkboxByName("editableRunProperties"));
    }

    public void setEditableResults(boolean set)
    {
        if (set)
            _test.checkCheckbox(Locator.checkboxByName("editableResultProperties"));
        else
            _test.uncheckCheckbox(Locator.checkboxByName("editableResultProperties"));
    }

    public void setBackgroundImport(boolean set)
    {
        if (set)
            _test.checkCheckbox(Locator.checkboxByName("backgroundUpload"));
        else
            _test.uncheckCheckbox(Locator.checkboxByName("backgroundUpload"));
    }

    public void addBatchField(String name, @Nullable String label, @Nullable String type)
    {
        final String xpathSection = "//tbody//td[contains(text(), 'Batch Fields')]/./parent::tr/./following-sibling::tr/./descendant::";
        addField(xpathSection, name, label, type);
    }

    public void removeBatchField(String name)
    {
        final String xpathSection = "//tbody//td[contains(text(), 'Batch Fields')]/./parent::tr/./following-sibling::tr/./descendant::";
        removeField(xpathSection, name);
    }

    public void addRunField(String name, @Nullable String label, @Nullable String type)
    {
        final String xpathSection = "//tbody//td[contains(text(), 'Run Fields')]/./parent::tr/./following-sibling::tr/./descendant::";
        addField(xpathSection, name, label, type);
    }

    public void removeRunField(String name)
    {
        final String xpathSection = "//tbody//td[contains(text(), 'Run Fields')]/./parent::tr/./following-sibling::tr/./descendant::";
        removeField(xpathSection, name);
    }

    public void addDataField(String name, @Nullable String label, @Nullable String type)
    {
        final String xpathSection = "//tbody//td[contains(text(), 'Data Fields')]/./parent::tr/./following-sibling::tr/./descendant::";
        addField(xpathSection, name, label, type);
    }

    public void removeDataField(String name)
    {
        final String xpathSection = "//tbody//td[contains(text(), 'Data Fields')]/./parent::tr/./following-sibling::tr/./descendant::";
        removeField(xpathSection, name);
    }

    private void addField(String xpathSection, String name, @Nullable String label, @Nullable String type)
    {
        List<WebElement> inputBoxes;

        Locator.xpath(xpathSection + "span[contains(@id, 'button_Add Field')]").findElement(_test.getDriver()).click();

        inputBoxes = Locator.xpath(xpathSection + "input[contains(@id, '-input') and starts-with(@id, 'name')]").findElements(_test.getDriver());
        _test.setFormElement(inputBoxes.get(inputBoxes.size() - 1), name);

        if(label != null)
        {
            inputBoxes = Locator.xpath(xpathSection + "input[contains(@id, '-input') and starts-with(@id, 'label')]").findElements(_test.getDriver());
            _test.setFormElement(inputBoxes.get(inputBoxes.size() - 1), label);
        }

        if(type != null)
        {
            inputBoxes = Locator.xpath(xpathSection + "input[starts-with(@name, 'ff_type')]/./following-sibling::div").findElements(_test.getDriver());
            inputBoxes.get(inputBoxes.size() - 1).click();
            _test.click(Locator.xpath("//div[contains(@class, 'x-window')]//div[contains(@class, 'x-window-bwrap')]//table//tr//label[contains(text(), '" + type + "')]"));
            _test.click(Locator.xpath("//button[contains(@class, 'x-btn-text')][contains(text(), 'Apply')]"));
        }
    }

    public void removeField(String xpathSection, String name)
    {
        final String xpathDelete1 = "input[contains(@id, '-input') and starts-with(@id, 'name')][";
        final String xpathDelete2 = "]/./ancestor::tr[contains(@class, 'editor-field-row')]/./descendant::div[contains(@id, 'partdelete')]";
        List<WebElement> inputBoxes;
        WebElement theBox = null;
        int index = 1;

        inputBoxes = Locator.xpath(xpathSection + "input[contains(@id, '-input') and starts-with(@id, 'name') and contains(@value, '')]").findElements(_test.getDriver());
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
            Locator.xpath(xpathSection + xpathDelete1 + index + xpathDelete2).findElement(_test.getDriver()).click();
            _test.clickButton("OK", 0);
        }

    }

    @Override
    public void saveAndClose()
    {
        super.saveAndClose();
        _test.waitForElement(Locator.css("table.labkey-data-region")); // 'Runs' or 'AssayList'
    }

    public enum MetadataInputFormat
    {
        MANUAL,
        FILE_BASED
    }
}
