/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.OptionSelect;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.io.File;

import static org.junit.Assert.assertTrue;
import static org.labkey.test.components.html.Checkbox.Checkbox;
import static org.labkey.test.components.html.Input.Input;
import static org.labkey.test.components.html.SelectWrapper.Select;

public class ReactAssayDesignerPage extends DomainDesignerPage
{
    public ReactAssayDesignerPage(WebDriver driver)
    {
        super(driver);
    }

    @Override
    public void waitForPage()
    {
        waitForElement(Locator.id("assay-design-description"), WAIT_FOR_JAVASCRIPT);
        elementCache().descriptionInput.get();
    }

    public ReactAssayDesignerPage setName(String name)
    {
        elementCache().nameInput.set(name);
        return this;
    }

    public String getName()
    {
        return elementCache().nameInput.get();
    }

    // I think the name field is the only field that can be enabled/disabled.
    // For example once an assay is created you can't change it's name so the field will be disabled.
    public boolean isNameEnabled()
    {
        return elementCache().nameInput.getComponentElement().isEnabled();
    }

    public ReactAssayDesignerPage setDescription(String description)
    {
        elementCache().descriptionInput.set(description);
        return this;
    }

    public String getDescription()
    {
        return elementCache().descriptionInput.get();
    }

    public ReactAssayDesignerPage setAutoCopyTarget(String containerPath)
    {
        elementCache().autoCopyTargetSelect.selectByVisibleText(containerPath);
        return this;
    }

    public ReactAssayDesignerPage setPlateTemplate(String template)
    {
        elementCache().plateTemplateSelect.selectByVisibleText(template);
        return this;
    }

    public void goToConfigureTemplates()
    {
        clickAndWait(elementCache().configureTemplatesLink);
        // todo: return the class that wraps this page
    }

    public ReactAssayDesignerPage setDetectionMethod(String method)
    {
        elementCache().detectionMethodSelect.selectByVisibleText(method);
        return this;
    }

    public ReactAssayDesignerPage setMetaDataInputFormat(MetadataInputFormat format)
    {
        elementCache().metadataInputSelect.selectOption(format);
        return this;
    }

    public ReactAssayDesignerPage setSaveScriptData(boolean checked)
    {
        elementCache().saveScriptFilesCheckbox.set(checked);
        return this;
    }

    public ReactAssayDesignerPage setEditableRuns(boolean checked)
    {
        elementCache().editableRunsCheckbox.set(checked);
        return this;
    }

    public boolean getEditableRuns()
    {
        return elementCache().editableRunsCheckbox.get();
    }

    public ReactAssayDesignerPage setEditableResults(boolean checked)
    {
        elementCache().editableResultCheckbox.set(checked);
        return this;
    }

    public boolean getEditableResults()
    {
        return elementCache().editableResultCheckbox.get();
    }

    public ReactAssayDesignerPage setBackgroundImport(boolean checked)
    {
        elementCache().backgroundUploadCheckbox.set(checked);
        return this;
    }

    public ReactAssayDesignerPage setQCStates(boolean checked)
    {
        elementCache().qcEnabledCheckbox.set(checked);
        return this;
    }

    public ReactAssayDesignerPage setPlateMetadata(boolean checked)
    {
        elementCache().plateTemplateCheckbox.set(checked);
        return this;
    }

    public ReactAssayDesignerPage addTransformScript(File transformScript)
    {
        int index = Locator.xpath("//input[starts-with(@id, 'assay-design-protocolTransformScripts')]").findElements(getDriver()).size();
        click(Locator.tagWithClass("span", "btn").containing("Add Script"));
        return setTransformScript(transformScript, index);
    }

    public ReactAssayDesignerPage setTransformScript(File transformScript)
    {
        return setTransformScript(transformScript, 0);
    }

    public ReactAssayDesignerPage setTransformScript(File transformScript, int index)
    {
        assertTrue("Unable to locate the transform script: " + transformScript, transformScript.exists());
        setFormElement(Locator.xpath("//input[@id='assay-design-protocolTransformScripts" + index + "']"), transformScript.getAbsolutePath());
        return this;
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

    public DomainFormPanel goToBatchFields()
    {
        return expandFieldsPanel("Batch");
    }

    public DomainFormPanel goToRunFields()
    {
        return expandFieldsPanel("Run");
    }

    public DomainFormPanel goToResultsFields()
    {
        return expandFieldsPanel("Results");
    }

    public DomainFormPanel expandFieldsPanel(String title)
    {
        DomainFormPanel panel = fieldsPanel(title);
        panel.expand();
        return panel;
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    public class ElementCache extends DomainDesignerPage.ElementCache
    {
        final Input nameInput = Input(Locator.id("assay-design-name"), getDriver()).findWhenNeeded(this);
        final Input descriptionInput = Input(Locator.id("assay-design-description"), getDriver()).findWhenNeeded(this);
        final Select autoCopyTargetSelect = Select(Locator.id("assay-design-autoCopyTargetContainerId")).findWhenNeeded(this);
        final Select plateTemplateSelect = Select(Locator.id("assay-design-selectedPlateTemplate")).findWhenNeeded(this);
        final WebElement configureTemplatesLink = Locator.linkContainingText("Configure Templates").findWhenNeeded(this);
        final Select detectionMethodSelect = Select(Locator.id("assay-design-selectedDetectionMethod")).findWhenNeeded(this);
        final OptionSelect<MetadataInputFormat> metadataInputSelect = OptionSelect.finder(Locator.id("assay-design-selectedMetadataInputFormat"), MetadataInputFormat.class).findWhenNeeded(this);
        final Checkbox saveScriptFilesCheckbox = Checkbox(Locator.checkboxById("assay-design-saveScriptFiles")).findWhenNeeded(this);
        final Checkbox editableRunsCheckbox = Checkbox(Locator.checkboxById("assay-design-editableRuns")).findWhenNeeded(this);
        final Checkbox editableResultCheckbox = Checkbox(Locator.checkboxById("assay-design-editableResults")).findWhenNeeded(this);
        final Checkbox backgroundUploadCheckbox = Checkbox(Locator.checkboxById("assay-design-backgroundUpload")).findWhenNeeded(this);
        final Checkbox qcEnabledCheckbox = Checkbox(Locator.checkboxById("assay-design-qcEnabled")).findWhenNeeded(this);
        final Checkbox plateTemplateCheckbox = Checkbox(Locator.checkboxById("assay-design-plateMetadata")).findWhenNeeded(this);
    }
}
