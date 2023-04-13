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
package org.labkey.test.components.pipeline;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.OptionSelect;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Map;

import static org.junit.Assert.assertTrue;

public class PipelineTriggerWizard extends WebDriverComponent<PipelineTriggerWizard.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public PipelineTriggerWizard(WebDriver driver)
    {
        _el = Locator.tagWithClass("div", "create-pipeline-trigger").findWhenNeeded(driver);
        _driver = driver;
    }

    public static PipelineTriggerWizard beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static PipelineTriggerWizard beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("pipeline", containerPath, "createPipelineTrigger"));
        return new PipelineTriggerWizard(driver.getDriver());
    }

    public static PipelineTriggerWizard beginAt(WebDriverWrapper driver, String containerPath, String pipelineTask)
    {
        driver.beginAt(WebTestHelper.buildURL("pipeline", containerPath, "createPipelineTrigger", Map.of("pipelineTask", pipelineTask)));
        return new PipelineTriggerWizard(driver.getDriver());
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
        elementCache().detailsButton.click();
        return this;
    }

    public PipelineTriggerWizard goToConfiguration()
    {
        elementCache().configurationButton.click();
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

    public PipelineTriggerWizard setAction(String action)
    {
        Locator.radioButtonByNameAndValue("insertOption", action)
                .findElement(this).click();
        return this;
    }

    public PipelineTriggerWizard setAssayProtocol(String value)
    {
        elementCache().assayProtocolInput.set(value);
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

    public PipelineTriggerWizard setConfigurationFile(String value)
    {
        elementCache().configurationFileInput.set(value);
        return this;
    }

    public boolean isMoveEnabled()
    {
        return elementCache().containerMoveInput.getComponentElement().isDisplayed() &&
                elementCache().containerMoveInput.getComponentElement().isEnabled();
    }

    public PipelineTriggerWizard setCopy(String value)
    {
        elementCache().copyInput.set(value);
        return this;
    }

    public PipelineTriggerWizard setParameterFunction(String function)
    {
        showAdvanced();

        elementCache().paramFunctionInput.set(function);
        return this;
    }

    private void showAdvanced()
    {
        if (!Locator.byClass("advanced-settings").findWhenNeeded(this).isDisplayed())
        {
            elementCache().showAdvanced.click();
        }
    }

    public PipelineTriggerWizard addCustomParameter(String key, String value)
    {
        showAdvanced();
        int initialCount = Locator.byClass("custom-parameter").findElements(this).size();
        elementCache().addCustomParam.click();
        Input keyInput = new Input(Locator.tagWithAttribute("input", "name", "custom-param-key-" + initialCount).findElement(this), getDriver());
        Input valueInput = new Input(Locator.tagWithAttribute("input", "name", "custom-param-value-" + initialCount).findElement(this), getDriver());

        keyInput.setValue(key);
        valueInput.setValue(value);

        return this;
    }

    public PipelineTriggerWizard removeCustomParameter(@NotNull Integer index)
    {
        showAdvanced();

        WebElement deleteIcon = Locator.tagWithClass("div", "custom-parameter")
                .append(Locator.tagWithClass("span", "fa-trash")).index(index)
                .findElement(this);

        deleteIcon.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(deleteIcon));
        return this;
    }

    public void saveConfiguration()
    {
        goToConfiguration();
        getWrapper().clickAndWait(elementCache().saveButton);
    }

    public void saveAndExpectError(String error)
    {
        goToConfiguration();
        elementCache().saveButton.click();
        assertTrue("Pipeline Trigger Wizard did not produce an error as expected", elementCache().error.getText().contains(error));
    }

    public void cancelEditing()
    {
        goToConfiguration();
        getWrapper().clickAndWait(elementCache().cancelButton);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {

        //details page elements
        Input nameInput = new Input(Locator.tagWithName("input", "name").findWhenNeeded(this), getDriver());
        Input descriptionInput = new Input(Locator.tagWithName("textarea", "description").findWhenNeeded(this), getDriver());
        OptionSelect typeSelect = new OptionSelect(Locator.tagWithName("select", "type").findWhenNeeded(this));
        OptionSelect taskSelect = new OptionSelect(Locator.tagWithName("select", "pipelineId").findWhenNeeded(this));
        Input usernameInput = new Input(Locator.tagWithName("input", "username").findWhenNeeded(this), getDriver());
        Input assayProviderInput = new Input(Locator.tagWithName("input", "assay provider").findWhenNeeded(this), getDriver());
        Checkbox enabledCheckbox = new Checkbox(Locator.tagWithName("input", "enabled").findWhenNeeded(this));
        //configuration page elements
        Input locationInput = new Input(Locator.tagWithName("input", "location").findWhenNeeded(this), getDriver());
        Checkbox recursiveCheckbox = new Checkbox(Locator.tagWithName("input", "recursive").findWhenNeeded(this));
        Input filePatternInput = new Input(Locator.tagWithName("input", "filePattern").findWhenNeeded(this), getDriver());
        Input quietInput = new Input(Locator.tagWithName("input", "quiet").findWhenNeeded(this), getDriver());
        Input containerMoveInput = new Input(Locator.tagWithName("input", "moveContainer").findWhenNeeded(this), getDriver());
        Input subdirectoryMoveInput = new Input(Locator.tagWithName("input", "moveDirectory").findWhenNeeded(this), getDriver());
        Input copyInput = new Input(Locator.tagWithName("input", "copy").findWhenNeeded(this), getDriver());
        Input paramFunctionInput = new Input(Locator.tagWithName("textarea", "parameterFunction").findWhenNeeded(this), getDriver());
        WebElement showAdvanced = Locator.byClass("custom-config__button").withText("Show Advanced Settings").findWhenNeeded(this);
        WebElement addCustomParam = Locator.byClass("custom-config__button").withText("Add Custom Parameter").findWhenNeeded(this);
        Input assayProtocolInput = new Input(Locator.tagWithName("input", "protocolName").findWhenNeeded(this), getDriver());
        Input configurationFileInput = new Input(Locator.name("configFileName").findWhenNeeded(this), getDriver());
        //navgiation elements
        WebElement detailsButton = Locator.buttonContainingText("Details").findWhenNeeded(this);
        WebElement configurationButton = Locator.buttonContainingText("Configuration").findWhenNeeded(this);

        //page actions
        WebElement saveButton = Locator.buttonContainingText("save").findWhenNeeded(this);
        WebElement cancelButton = Locator.linkContainingText("cancel").findWhenNeeded(this);

        WebElement error = Locator.tagWithClass("div", "alert-danger").findWhenNeeded(this);
    }

    public enum ActionOptions
    {
        MERGE("MERGE"),
        UPDATE("UPDATE"),
        APPEND("INSERT");

        private final String value;

        ActionOptions(String value)
        {
            this.value = value;
        }

        public String getValue()
        {
            return value;
        }
    }

}
