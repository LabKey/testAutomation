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
package org.labkey.test.pages.list;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.domain.DomainDesigner;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.list.AdvancedListSettingsDialog;
import org.labkey.test.components.react.ToggleButton;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_PAGE;
import static org.labkey.test.WebDriverWrapper.sleep;
import static org.labkey.test.WebDriverWrapper.waitFor;

/**
 * Automates the LabKey ui components defined in: packages/components/src/components/domainproperties/list/ListDesignerPanels.tsx
 * Currently only exposed in LKS. Move to 'org.labkey.test.components.list.ListDesigner' once needed for LKB or LKSM
 */
public class EditListDefinitionPage extends DomainDesigner<EditListDefinitionPage.ElementCache>
{
    public EditListDefinitionPage(WebDriver driver)
    {
        super(driver);
    }

    public static EditListDefinitionPage beginAt(WebDriverWrapper driver, int listId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), listId);
    }

    public static EditListDefinitionPage beginAt(WebDriverWrapper driver, String containerPath, int listId)
    {
        driver.beginAt(WebTestHelper.buildURL("list", containerPath, "editListDefinition", Maps.of("listId", String.valueOf(listId))));
        return new EditListDefinitionPage(driver.getDriver());
    }

    public static EditListDefinitionPage beginAt(WebDriverWrapper driver, String listName)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), listName);
    }

    public static EditListDefinitionPage beginAt(WebDriverWrapper driver, String containerPath, String listName)
    {
        driver.beginAt(WebTestHelper.buildURL("list", containerPath, "editListDefinition", Maps.of("name", listName)));
        return new EditListDefinitionPage(driver.getDriver());
    }

    public void waitForPage()
    {
        waitFor(()-> getFieldsPanel().getComponentElement().isDisplayed(),
            "The page did not render in time", WAIT_FOR_PAGE);
    }

    // List Properties

    public EditListDefinitionPage setName(String listName)
    {
        expandPropertiesPanel();
        elementCache().nameInput.set(listName);
        return this;
    }

    public EditListDefinitionPage setDescription(String description)
    {
        expandPropertiesPanel();
        elementCache().descriptionInput.set(description);
        return this;
    }

    public AdvancedListSettingsDialog openAdvancedListSettings()
    {
        expandPropertiesPanel();
        elementCache().advancedSettingsBtn.click();
        return new AdvancedListSettingsDialog(this);
    }

    // List Fields

    public DomainFormPanel manuallyDefineFieldsWithKey(FieldDefinition keyField)
    {
        DomainFormPanel fieldsPanel = getFieldsPanel();
        fieldsPanel.manuallyDefineFields(keyField);
        selectKeyField(keyField.getName());

        return fieldsPanel;
    }

    @NotNull
    public DomainFormPanel manuallyDefineFieldsWithAutoIncrementingKey(String listKeyName)
    {
        DomainFormPanel fieldsPanel = getFieldsPanel();
        fieldsPanel.manuallyDefineFields("REMOVE_ME"); // Switching to manual field definition mode adds a field
        selectAutoIntegerKeyField();
        sleep(500); // wait just a bit for the auto integer key field to be added
        fieldsPanel.getField(0).setName(listKeyName);
        fieldsPanel.removeField("REMOVE_ME"); // Remove automatically added field
        return fieldsPanel;
    }

    public DomainFormPanel selectAutoIntegerKeyField()
    {
        return selectKeyField("Auto integer key");
    }

    public DomainFormPanel selectKeyField(String keyField)
    {
        DomainFormPanel fieldsPanel = getFieldsPanel();
        getWrapper().selectOptionByText(Locator.name("keyField"), keyField);
        return fieldsPanel;
    }

    // note: auto-import slider is only shown when you've inferred fields from file
    public EditListDefinitionPage setAutoImport(boolean autoImport)
    {
        elementCache().autoImportSlider().set(autoImport);
        return this;
    }

    public boolean getAutoImport()
    {
        return elementCache().autoImportSlider().isEnabled();
    }

    public void setColumnPhiLevel(String name, FieldDefinition.PhiSelectType phiLevel)
    {
        DomainFormPanel fieldsPanel = getFieldsPanel();
        fieldsPanel.getField(name).setPHILevel(phiLevel);
    }

    public EditListDefinitionPage addField(FieldDefinition... fields)
    {
        DomainFormPanel domainEditor = getFieldsPanel();
        for (FieldDefinition field : fields)
        {
            domainEditor.addField(field);
        }
        return this;
    }

    public EditListDefinitionPage removeField(int index)
    {
        DomainFormPanel fieldsPanel = getFieldsPanel();
        fieldsPanel.removeField(fieldsPanel.getField(index).getName(), true);
        return this;
    }

    public List<String> getFieldNames()
    {
        DomainFormPanel fieldsPanel = getFieldsPanel();
        return fieldsPanel.fieldNames();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends DomainDesigner<?>.ElementCache
    {
        protected final Input nameInput = Input.Input(Locator.id("name"), getDriver()).findWhenNeeded(propertiesPanel);
        protected final Input descriptionInput = Input.Input(Locator.id("description"), getDriver()).findWhenNeeded(propertiesPanel);

        protected final WebElement advancedSettingsBtn = Locator.tagWithClass("button", "domain-field-float-right")
                .withText("Advanced Settings").findWhenNeeded(propertiesPanel);

        protected ToggleButton autoImportSlider()
        {
            return new ToggleButton.ToggleButtonFinder(getDriver()).withState("Yes").find(fieldsPanel);
        }
    }
}
