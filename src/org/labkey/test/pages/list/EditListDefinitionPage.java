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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class EditListDefinitionPage extends LabKeyPage<EditListDefinitionPage.ElementCache>
{
    private boolean useNewDesigner = false;

    public EditListDefinitionPage(WebDriver driver)
    {
        super(driver);
    }

    public EditListDefinitionPage(WebDriver driver, boolean useNewDesigner)
    {
        super(driver);
        this.useNewDesigner = useNewDesigner;
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

    public DomainFormPanel getFieldsPanel()
    {
        return new DomainFormPanel.DomainFormPanelFinder(getDriver()).withTitle("Fields").find();
    }

    public DomainFormPanel expandFieldsPanel()
    {
        DomainFormPanel panel = getFieldsPanel();
        panel.expand();
        return panel;
    }

    public DomainFormPanel setKeyField(ListHelper.ListColumnType listKeyType, String listKeyName)
    {
        DomainFormPanel fieldsPanel = expandFieldsPanel();
        if (listKeyType == ListHelper.ListColumnType.AutoInteger)
        {
            fieldsPanel.startNewDesign("REMOVE_ME");
            selectOptionByText(Locator.name("keyField"), "Auto integer key");
            sleep(500); // wait just a bit for the auto integer key field to be added
            fieldsPanel.getField(0).setName(listKeyName);
            fieldsPanel.removeField("REMOVE_ME");
        }
        else
        {
            DomainFieldRow keyField = fieldsPanel.startNewDesign(listKeyName);
            keyField.setType(FieldDefinition.ColumnType.valueOf(listKeyType.name()));
            selectOptionByText(Locator.name("keyField"), listKeyName);
        }

        return fieldsPanel;
    }

    public void setColumnName(int index, String name)
    {
        if (!this.useNewDesigner)
        {
            Locator nameLoc = Locator.name("ff_name" + index);
            click(nameLoc);
            setFormElement(nameLoc, name);
            pressTab(nameLoc);
            return;
        }

        DomainFormPanel fieldsPanel = expandFieldsPanel();
        fieldsPanel.getField(index).setName(name);
    }

    public void setColumnLabel(int index, String label)
    {
        if (!this.useNewDesigner)
        {
            Locator labelLoc = Locator.name("ff_label" + index);
            click(labelLoc);
            setFormElement(labelLoc, label);
            pressTab(labelLoc);
            return;
        }

        DomainFormPanel fieldsPanel = expandFieldsPanel();
        fieldsPanel.getField(index).setLabel(label);
    }

    public void setColumnPhiLevel(String name, PropertiesEditor.PhiSelectType phiLevel)
    {
        if (!this.useNewDesigner)
        {
            ListHelper listHelper = new ListHelper(this);
            PropertiesEditor listFieldEditor = listHelper.getListFieldEditor();
            listFieldEditor.selectField(name);
            listFieldEditor.fieldProperties().selectAdvancedTab().setPhiLevel(phiLevel);
            return;
        }

        DomainFormPanel fieldsPanel = expandFieldsPanel();
        fieldsPanel.getField(name).setPHILevel(phiLevel);
    }

    public void addField(ListHelper.ListColumn newCol)
    {
        if (!this.useNewDesigner)
        {
            ListHelper listHelper = new ListHelper(this);
            listHelper.addField(newCol);
            return;
        }

        DomainFormPanel fieldsPanel = expandFieldsPanel();
        fieldsPanel.addField(newCol);
    }

    public void clickSave()
    {
        if (!this.useNewDesigner)
        {
            WebElement saveButton = Locator.lkButton("Save").waitForElement(getDriver(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            scrollToTop(); // After clicking save, sometimes the page scrolls so that the project menu is under the mouse
            saveButton.click();
            waitForElement(Locator.lkButton("Edit Design"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            waitForElement(Locator.lkButton("Done"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            return;
        }

        // TODO move this ElementCache
        clickAndWait(Locator.button("Save").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT));
    }

    public PropertiesEditor listFields()
    {
        return elementCache()._propertiesEditor;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        private final PropertiesEditor _propertiesEditor = new PropertiesEditor.PropertiesEditorFinder(getDriver()).withTitle("List Fields").findWhenNeeded();
    }
}
