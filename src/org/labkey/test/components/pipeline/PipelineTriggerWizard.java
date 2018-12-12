/*
 * Copyright (c) 2018 LabKey Corporation
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
package org.labkey.test.components.pipeline;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.components.labkey.LabKeyAlert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;

public class PipelineTriggerWizard extends WebDriverComponent<PipelineTriggerWizard.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public PipelineTriggerWizard(WebDriver driver)
    {
        _el = Locator.tagWithId("form", "pipelineForm").findWhenNeeded(driver);
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    public PipelineTriggerWizard setName(String value)
    {
        elementCache().nameInput.set(value);
        return this;
    }

    public String getUserName()
    {
        return elementCache().usernameInput.get();
    }

    public PipelineTriggerWizard setDescription(String value)
    {
        elementCache().descriptionInput.set(value);
        return this;
    }

    public PipelineTriggerWizard setType(String type)
    {
        elementCache().typeSelect.set(type);
        return this;
    }

    public String getTask()
    {
        return elementCache().taskSelect.get();
    }

    public PipelineTriggerWizard setTask(String task)
    {
        elementCache().taskSelect.set(task);
        return this;
    }

    public PipelineTriggerWizard setAssayProvider(String value)
    {
        elementCache().assayProviderInput.set(value);
        return this;
    }

    public PipelineTriggerWizard setUsername(String value)
    {
        elementCache().usernameInput.set(value);
        return this;
    }

    public PipelineTriggerWizard setEnabled(boolean isEnabled)
    {
        elementCache().enabledCheckbox.set(isEnabled);
        return this;
    }

    public PipelineTriggerWizard goToDetails()
    {
        elementCache().detailsLink.click();
        return this;
    }

    public PipelineTriggerWizard goToConfiguration()
    {
        elementCache().configurationLink.click();
        return this;
    }

    public PipelineTriggerWizard setLocation(String value)
    {
        elementCache().locationInput.set(value);
        return this;
    }

    public PipelineTriggerWizard setRecursive(boolean isRecursive)
    {
        elementCache().recursiveCheckbox.set(isRecursive);
        return this;
    }

    public PipelineTriggerWizard setFilePattern(String value)
    {
        elementCache().filePatternInput.set(value);
        return this;
    }

    public PipelineTriggerWizard setQuiet(Integer number)
    {
        elementCache().quietInput.set(number.toString());
        return this;
    }

    public PipelineTriggerWizard setContainerMove(String value)
    {
        elementCache().containerMoveInput.set(value);
        return this;
    }

    public PipelineTriggerWizard setSubdirectoryMove(String value)
    {
        elementCache().subdirectoryMoveInput.set(value);
        return this;
    }

    public boolean isMoveEnabled()
    {
        return elementCache().containerMoveInput.getComponentElement().getAttribute("disabled") == null;
    }

    public PipelineTriggerWizard setCopy(String value)
    {
        elementCache().copyInput.set(value);
        return this;
    }

    public PipelineTriggerWizard setParameterFunction(String function)
    {
        elementCache().paramFunctionInput.set(function);
        return this;
    }

    public PipelineTriggerWizard addCustomParamter(String key, String value, @NotNull Integer index)
    {
        elementCache().addCustomParam.click();
        Input keyInput = new Input(Locator.tagWithAttribute("input", "name", "customParamKey").findElements(this).get(index), getDriver());
        Input valueInput = new Input(Locator.tagWithAttribute("input", "name", "customParamValue").findElements(this).get(index), getDriver());

        keyInput.setValue(key);
        valueInput.setValue(value);

        return this;
    }

    public PipelineTriggerWizard removeCustomParameter(@NotNull Integer index)
    {
        WebElement deleteIcon =  Locator.id("extraParams").append(Locator.byClass("removeParamTrigger"))
                .findElements(this).get(index);

        deleteIcon.click();
        return this;
    }

    public void saveConfiguration()
    {
        goToConfiguration();
        getWrapper().clickAndWait(elementCache().saveButton);
    }

    public ModalDialog saveAndExpectError()
    {
        goToConfiguration();
        elementCache().saveButton.click();
        LabKeyAlert labKeyAlert = new LabKeyAlert(getDriver(), 10000);
        assertEquals("Pipeline Trigger Wizard did not produce an error as expected", "Error", labKeyAlert.getTitle());
        return labKeyAlert;
    }

    public void cancelEditing()
    {
        goToConfiguration();
        getWrapper().clickAndWait(elementCache().cancelButton);
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component.ElementCache
    {
        public ElementCache()
        {
            if (getWrapper().getUrlParameters().containsKey("rowId"))
                // Wait for saved config to populate form
                Locators.pageSignal("triggerConfigLoaded").waitForElement(getDriver(), 10000);
        }

        //details page elements
        Input nameInput = new Input(Locator.tagWithName("input", "name").findWhenNeeded(this), getDriver());
        Input descriptionInput =  new Input(Locator.tagWithName("textarea", "description").findWhenNeeded(this), getDriver());
        OptionSelect typeSelect = new OptionSelect(Locator.tagWithName("select", "type").findWhenNeeded(this));
        OptionSelect taskSelect = new OptionSelect(Locator.tagWithName("select", "pipelineId").findWhenNeeded(this));
        Input usernameInput =  new Input(Locator.tagWithName("input", "username").findWhenNeeded(this), getDriver());
        Input assayProviderInput =  new Input(Locator.tagWithName("input", "assay provider").findWhenNeeded(this), getDriver());
        Checkbox enabledCheckbox = new Checkbox(Locator.tagWithName("input", "enabled").findWhenNeeded(this));

        //configuration page elements
        Input locationInput =  new Input(Locator.tagWithName("input", "location").findWhenNeeded(this), getDriver());
        Checkbox recursiveCheckbox = new Checkbox(Locator.tagWithName("input", "recursive").findWhenNeeded(this));
        Input filePatternInput =  new Input(Locator.tagWithName("input", "filePattern").findWhenNeeded(this), getDriver());
        Input quietInput =  new Input(Locator.tagWithName("input", "quiet").findWhenNeeded(this), getDriver());
        Input containerMoveInput =  new Input(Locator.tagWithName("input", "containerMove").findWhenNeeded(this), getDriver());
        Input subdirectoryMoveInput = new Input(Locator.tagWithName("input", "directoryMove").findWhenNeeded(this), getDriver());
        Input copyInput =  new Input(Locator.tagWithName("input", "copy").findWhenNeeded(this), getDriver());
        Input paramFunctionInput =  new Input(Locator.tagWithName("textarea", "parameterFunction").findWhenNeeded(this), getDriver());
        WebElement addCustomParam = Locator.linkContainingText("add custom parameter").findWhenNeeded(this);

        //navgiation elements
        WebElement detailsLink = Locator.linkWithHref("#details").findWhenNeeded(this);
        WebElement configurationLink = Locator.linkWithHref("#configuration").findWhenNeeded(this);

        //page actions
        WebElement saveButton = Locator.linkContainingText("Save").findWhenNeeded(this);
        WebElement cancelButton = Locator.linkContainingText("Cancel").findWhenNeeded(this);
    }
}