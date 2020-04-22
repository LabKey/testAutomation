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

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.ToggleButton;
import org.labkey.test.components.list.AdvancedListSettingsDialog;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.core.login.SvgCheckbox;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class EditListDefinitionPage extends LabKeyPage<EditListDefinitionPage.ElementCache>
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
        waitFor(()-> elementCache().listPropertiesHeaderLoc.existsIn(getDriver()),
            "The page did not render in time", WAIT_FOR_PAGE);
    }

    public EditListDefinitionPage setListName(String listName)
    {
        expandPropertiesPane();
        elementCache().nameInput().set(listName);
        return this;
    }

    public EditListDefinitionPage setDescription(String description)
    {
        expandPropertiesPane();
        elementCache().descriptionInput().set(description);
        return this;
    }

    public AdvancedListSettingsDialog openAdvancedListSettings()
    {
        expandPropertiesPane();
        elementCache().advancedSettingsBtn().click();
        return new AdvancedListSettingsDialog(this);
    }

    public void expandPropertiesPane()
    {
        if (!isListPropertiesPaneExpanded())
            elementCache().propertiesPaneToggle.click();
        waitFor(()-> isListPropertiesPaneExpanded(), "the properties pane did not expand in time", 2000);
    }

    private boolean isListPropertiesPaneExpanded()
    {
        String panelHeaderClass = elementCache().listPropertiesHeader.getAttribute("class");
        return panelHeaderClass.contains("expanded") && !panelHeaderClass.contains("collapsed");
    }

    public EditListDefinitionPage checkIndexFileAttachements(boolean checked)
    {
        openAdvancedListSettings()
                .setIndexFileAttachments(checked)
                .clickApply();
        return this;
    }


    public DomainFormPanel getFieldsPanel()
    {
        DomainFormPanel panel = new DomainFormPanel.DomainFormPanelFinder(getDriver()).withTitle("Fields").find();
        panel.expand();
        return panel;
    }
    // list properties

    public DomainFormPanel setKeyField(ListHelper.ListColumnType listKeyType, String listKeyName)
    {
        DomainFormPanel fieldsPanel = getFieldsPanel();
        if (listKeyType == ListHelper.ListColumnType.AutoInteger)
        {
            fieldsPanel.manuallyDefineFields("REMOVE_ME");
            selectAutoIntegerKeyField();
            sleep(500); // wait just a bit for the auto integer key field to be added
            fieldsPanel.getField(0).setName(listKeyName);
            fieldsPanel.removeField("REMOVE_ME");
        }
        else
        {
            DomainFieldRow keyField = fieldsPanel.manuallyDefineFields(listKeyName);
            keyField.setType(FieldDefinition.ColumnType.valueOf(listKeyType.name()));
            selectKeyField(listKeyName);
        }

        return fieldsPanel;
    }

    public DomainFormPanel selectAutoIntegerKeyField()
    {
        return selectKeyField("Auto integer key");
    }

    public DomainFormPanel selectKeyField(String keyField)
    {
        DomainFormPanel fieldsPanel = getFieldsPanel();
        selectOptionByText(Locator.name("keyField"), keyField);
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
        return elementCache().autoImportSlider().get();
    }

    public EditListDefinitionPage setColumnName(int index, String name)
    {
        DomainFormPanel fieldsPanel = getFieldsPanel();
        fieldsPanel.getField(index).setName(name);
        return this;
    }

    public EditListDefinitionPage setColumnLabel(int index, String label)
    {
        DomainFormPanel fieldsPanel = getFieldsPanel();
        fieldsPanel.getField(index).setLabel(label);
        return this;
    }

    public void setColumnPhiLevel(String name, PropertiesEditor.PhiSelectType phiLevel)
    {
        DomainFormPanel fieldsPanel = getFieldsPanel();
        fieldsPanel.getField(name).setPHILevel(phiLevel);
    }

    public EditListDefinitionPage addField(ListHelper.ListColumn newCol)
    {
        DomainFormPanel fieldsPanel = getFieldsPanel();
        fieldsPanel.addField(newCol);
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

    public void clickSaveExpectingError()
    {
        clickAndWait(Locator.button("Save").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT), 0);
    }

    public void clickSave()
    {
        clickAndWait(Locator.button("Save").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT));
    }

    public void clickCancel()
    {
        clickAndWait(Locator.button("Cancel").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT));
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Locator listPropertiesHeaderLoc = Locator.id("list-properties-hdr");
        WebElement listPropertiesHeader = listPropertiesHeaderLoc
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement propertiesPaneToggle = Locator.tagWithClass("span", "pull-right")
                .waitForElement(listPropertiesHeader, WAIT_FOR_JAVASCRIPT);

        Input nameInput()
        {
            return Input.Input(Locator.id("name"), getDriver()).waitFor();
        }
        Input descriptionInput()
        {
            return Input.Input(Locator.id("description"), getDriver()).waitFor();
        }

        SvgCheckbox checkbox(String labelText)
        {
            Locator loc = Locator.tagWithClass("div", "list__properties__checkbox-row")
                    .withChild(Locator.tagWithText("span", labelText))
                    .child(Locator.tagWithClass("span", "list__properties__checkbox--no-highlight"));
            return new SvgCheckbox(loc.waitForElement(getDriver(), 2000), getDriver());
        }

        WebElement advancedSettingsBtn()
        {
            return Locator.tagWithClass("button", "domain-field-float-right").withText("Advanced Settings")
                    .waitForElement(this, 2000);
        }

        ToggleButton autoImportSlider()
        {
            return new ToggleButton.ToggleButtonFinder(getDriver()).withState("Import Data").find();
        }
    }
}
