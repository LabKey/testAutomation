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
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.domain.DomainPanel;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.assay.plate.PlateTemplateListPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.labkey.test.components.html.Checkbox.Checkbox;
import static org.labkey.test.components.html.Input.Input;
import static org.labkey.test.components.html.SelectWrapper.Select;

/**
 * Automates the LabKey ui component defined in: packages/components/src/components/domainproperties/assay/AssayDesignerPanels.tsx
 */
public class ReactAssayDesignerPage extends DomainDesignerPage
{
    public ReactAssayDesignerPage(WebDriver driver)
    {
        super(driver);
    }

    public ReactAssayDesignerPage setName(String name)
    {
        expandPropertiesPanel();
        elementCache().nameInput.set(name);
        return this;
    }

    public String getName()
    {
        expandPropertiesPanel();
        return elementCache().nameInput.get();
    }

    // I think the name field is the only field that can be enabled/disabled.
    // For example once an assay is created you can't change it's name so the field will be disabled.
    public boolean isNameEnabled()
    {
        expandPropertiesPanel();
        return elementCache().nameInput.getComponentElement().isEnabled();
    }

    public ReactAssayDesignerPage setDescription(String description)
    {
        expandPropertiesPanel();
        elementCache().descriptionInput.set(description);
        return this;
    }

    public String getDescription()
    {
        expandPropertiesPanel();
        return elementCache().descriptionInput.get();
    }

    public ReactAssayDesignerPage setAutoLinkTarget(String containerPath)
    {
        expandPropertiesPanel();
        WebDriverWrapper.waitFor(() -> elementCache().autoLinkTargetSelect.getOptions().size() > 1,
                "No Auto-link targets available", 5_000);
        elementCache().autoLinkTargetSelect.selectByVisibleText(containerPath);
        return this;
    }

    public ReactAssayDesignerPage setAutoLinkCategory(String categoryName)
    {
        expandPropertiesPanel();
        elementCache().autoLinkDatasetCategory.set(categoryName);
        return this;
    }

    public String getAutoLinkCategory()
    {
        expandPropertiesPanel();
        return elementCache().autoLinkDatasetCategory.get();
    }

    public ReactAssayDesignerPage setPlateTemplate(String template)
    {
        expandPropertiesPanel();
        elementCache().plateTemplateSelect.selectByVisibleText(template);
        return this;
    }

    public PlateTemplateListPage goToConfigureTemplates()
    {
        expandPropertiesPanel();
        getWrapper().clickAndWait(elementCache().configureTemplatesLink);
        return new PlateTemplateListPage(getDriver());
    }

    public ReactAssayDesignerPage setDetectionMethod(String method)
    {
        expandPropertiesPanel();
        elementCache().detectionMethodSelect.selectByVisibleText(method);
        return this;
    }

    public boolean isMetadataInputFormatSelectPresent()
    {
        expandPropertiesPanel();
        return elementCache().metadataInputSelect().isPresent();
    }

    public ReactAssayDesignerPage setMetaDataInputFormat(MetadataInputFormat format)
    {
        expandPropertiesPanel();
        elementCache().metadataInputSelect().get().selectOption(format);
        return this;
    }

    public String getMetadataInputFormat()
    {
        expandPropertiesPanel();
        return elementCache().metadataInputSelect().get().get();
    }

    public ReactAssayDesignerPage setSaveScriptData(boolean checked)
    {
        expandPropertiesPanel();
        elementCache().saveScriptFilesCheckbox.set(checked);
        return this;
    }

    public ReactAssayDesignerPage setEditableRuns(boolean checked)
    {
        expandPropertiesPanel();
        elementCache().editableRunsCheckbox.set(checked);
        return this;
    }

    public boolean getEditableRuns()
    {
        expandPropertiesPanel();
        return elementCache().editableRunsCheckbox.get();
    }

    public ReactAssayDesignerPage setEditableResults(boolean checked)
    {
        expandPropertiesPanel();
        elementCache().editableResultCheckbox.set(checked);
        return this;
    }

    public boolean getEditableResults()
    {
        expandPropertiesPanel();
        return elementCache().editableResultCheckbox.get();
    }

    public ReactAssayDesignerPage setBackgroundImport(boolean checked)
    {
        expandPropertiesPanel();
        elementCache().backgroundUploadCheckbox.set(checked);
        return this;
    }

    public ReactAssayDesignerPage setQCStates(boolean checked)
    {
        expandPropertiesPanel();
        elementCache().qcEnabledCheckbox.set(checked);
        return this;
    }

    public ReactAssayDesignerPage setPlateMetadata(boolean checked)
    {
        expandPropertiesPanel();
        elementCache().plateTemplateCheckbox.set(checked);
        return this;
    }

    public ReactAssayDesignerPage setStatus(boolean checked)
    {
        expandPropertiesPanel();
        elementCache().activeStatusCheckBox.set(checked);
        return this;
    }

    public boolean getStatus()
    {
        expandPropertiesPanel();
        return elementCache().activeStatusCheckBox.get();
    }

    public ReactAssayDesignerPage addTransformScript(File transformScript)
    {
        expandPropertiesPanel();
        int index = Locator.xpath("//input[starts-with(@id, 'assay-design-protocolTransformScripts')]").findElements(getDriver()).size();
        getWrapper().click(Locator.tagWithClass("span", "btn").containing("Add Script"));
        return setTransformScript(transformScript, index);
    }

    public ReactAssayDesignerPage setTransformScript(File transformScript)
    {
        expandPropertiesPanel();
        return setTransformScript(transformScript, 0);
    }

    public ReactAssayDesignerPage setTransformScript(File transformScript, int index)
    {
        expandPropertiesPanel();
        assertTrue("Unable to locate the transform script: " + transformScript, transformScript.exists());
        getWrapper().setFormElement(Locator.xpath("//input[@id='assay-design-protocolTransformScripts" + index + "']"), transformScript.getAbsolutePath());
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

    protected void expandPropertiesPanel()
    {
        elementCache().propertiesPanel.expand();
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
        protected final DomainPanel<?, ?> propertiesPanel = new DomainPanel.DomainPanelFinder(getDriver()).index(0).timeout(5000).findWhenNeeded(this);
        final Input nameInput = Input(Locator.id("assay-design-name"), getDriver()).findWhenNeeded(propertiesPanel);
        final Input descriptionInput = Input(Locator.id("assay-design-description"), getDriver()).findWhenNeeded(propertiesPanel);
        final Input autoLinkDatasetCategory = Input(Locator.id("assay-design-autoLinkCategory"), getDriver()).findWhenNeeded(propertiesPanel);
        final Select autoLinkTargetSelect = Select(Locator.id("assay-design-autoCopyTargetContainerId")).findWhenNeeded(propertiesPanel);
        final Select plateTemplateSelect = Select(Locator.id("assay-design-selectedPlateTemplate")).findWhenNeeded(propertiesPanel);
        final WebElement configureTemplatesLink = Locator.linkContainingText("Configure Templates").findWhenNeeded(propertiesPanel);
        final Select detectionMethodSelect = Select(Locator.id("assay-design-selectedDetectionMethod")).findWhenNeeded(propertiesPanel);
        final Optional<OptionSelect<MetadataInputFormat>> metadataInputSelect()
        {
            return OptionSelect.finder(Locator.id("assay-design-selectedMetadataInputFormat"), MetadataInputFormat.class)
                    .findOptional(propertiesPanel);
        }
        final Checkbox saveScriptFilesCheckbox = Checkbox(Locator.checkboxById("assay-design-saveScriptFiles")).findWhenNeeded(propertiesPanel);
        final Checkbox editableRunsCheckbox = Checkbox(Locator.checkboxById("assay-design-editableRuns")).findWhenNeeded(propertiesPanel);
        final Checkbox editableResultCheckbox = Checkbox(Locator.checkboxById("assay-design-editableResults")).findWhenNeeded(propertiesPanel);
        final Checkbox backgroundUploadCheckbox = Checkbox(Locator.checkboxById("assay-design-backgroundUpload")).findWhenNeeded(propertiesPanel);
        final Checkbox qcEnabledCheckbox = Checkbox(Locator.checkboxById("assay-design-qcEnabled")).findWhenNeeded(propertiesPanel);
        final Checkbox plateTemplateCheckbox = Checkbox(Locator.checkboxById("assay-design-plateMetadata")).findWhenNeeded(propertiesPanel);
        final Checkbox activeStatusCheckBox = Checkbox(Locator.checkboxById("assay-design-status")).findWhenNeeded(propertiesPanel);
    }
}
