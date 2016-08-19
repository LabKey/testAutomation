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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class AssayDesignerPage extends BaseDesignerPage
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
        waitForElement(Locator.id("AssayDesignerDescription"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void setName(String name)
    {
        setFormElement(Locator.id("AssayDesignerName"), name);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), BaseWebDriverTest.SeleniumEvent.change);
    }

    public void setDescription(String description)
    {
        setFormElement(Locator.id("AssayDesignerDescription"), description);
    }

    public void setAutoCopyData(boolean set)
    {
        if (set)
            checkCheckbox(Locator.checkboxByName("autoCopy"));
        else
            uncheckCheckbox(Locator.checkboxByName("autoCopy"));
    }

    public void setAutoCopyTarget(String containerPath)
    {
        selectOptionByText(Locator.id("autoCopyTarget"), containerPath);
    }

    public void addTransformScript(File transformScript)
    {
        int index = getElementCount(Locator.xpath("//input[starts-with(@id, 'AssayDesignerTransformScript')]"));
        click(Locator.lkButton("Add Script"));

        setTransformScript(transformScript, index);
    }

    public void setTransformScript(File transformScript)
    {
        setTransformScript(transformScript, 0);
    }

    public void setTransformScript(File transformScript, int index)
    {
        assertTrue("Unable to locate the transform script: " + transformScript, transformScript.exists());

        setFormElement(Locator.xpath("//input[@id='AssayDesignerTransformScript" + index + "']"), transformScript.getAbsolutePath());
    }

    public void setPlateTemplate(String template)
    {
        selectOptionByText(Locator.id("plateTemplate"), template);
    }

    public void setDetectionMethod(String method)
    {
        selectOptionByText(Locator.id("detectionMethod"), method);
    }

    public void setMetaDataInputFormat(MetadataInputFormat format)
    {
        selectOptionByValue(Locator.id("metadataInputFormat"), format.name());
    }

    public void setSaveScriptData(boolean set)
    {
        if (set)
            checkCheckbox(Locator.checkboxByName("debugScript"));
        else
            uncheckCheckbox(Locator.checkboxByName("debugScript"));
    }

    public void setEditableRuns(boolean set)
    {
        if (set)
            checkCheckbox(Locator.checkboxByName("editableRunProperties"));
        else
            uncheckCheckbox(Locator.checkboxByName("editableRunProperties"));
    }

    public void setEditableResults(boolean set)
    {
        if (set)
            checkCheckbox(Locator.checkboxByName("editableResultProperties"));
        else
            uncheckCheckbox(Locator.checkboxByName("editableResultProperties"));
    }

    public void setBackgroundImport(boolean set)
    {
        if (set)
            checkCheckbox(Locator.checkboxByName("backgroundUpload"));
        else
            uncheckCheckbox(Locator.checkboxByName("backgroundUpload"));
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

    public enum MetadataInputFormat
    {
        MANUAL,
        FILE_BASED
    }
}
