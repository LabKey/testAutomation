/*
 * Copyright (c) 2013-2018 LabKey Corporation
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
package org.labkey.test.util;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;

public class SchemaHelper
{
    protected WebDriverWrapper _test;
    private int _queryLoadTimeOut = 0;

    public SchemaHelper(WebDriverWrapper test)
    {
        _test = test;
    }

    @LogMethod
    public void createLinkedSchema(String projectName, String targetFolder, String name, String sourceContainerPath, String schemaTemplate, String sourceSchemaName, String tables, String metadata)
    {
        _editLinkedSchema(true, projectName, targetFolder, name, sourceContainerPath, schemaTemplate, sourceSchemaName, tables, metadata);
    }

    @LogMethod
    public void updateLinkedSchema(String projectName, String targetFolder, String name, String sourceContainerPath, String schemaTemplate, String sourceSchemaName, String tables, String metadata)
    {
        _editLinkedSchema(false, projectName, targetFolder, name, sourceContainerPath, schemaTemplate, sourceSchemaName, tables, metadata);
    }

    public void setQueryLoadTimeout(int timeout)
    {
        _queryLoadTimeOut = timeout;
    }

    public void _editLinkedSchema(boolean create, String projectName, String targetFolder, String name, String sourceContainerPath, String schemaTemplate, String sourceSchemaName, String tables, String metadata)
    {
        _test.beginAt("/query/" + projectName + (targetFolder == null ? "" : "/" + targetFolder) + "/admin.view");

        // Click the create new or edit existing link.
        Locator link;
        if (create)
            link = Locator.xpath("//a[text()='new linked schema']");
        else
            link = Locator.xpath("//td[text()='" + name + "']/..//a[text()='edit']");
        _test.waitAndClickAndWait(link);

        _test.waitForElement(Locator.xpath("//input[@name='userSchemaName']"));
        _test.setFormElement(Locator.xpath("//input[@name='userSchemaName']"), name);

        _test.waitForElement(Locator.css(".containers-loaded-marker"));
        _test._ext4Helper.selectComboBoxItem("Source Container:", sourceContainerPath);

        if (schemaTemplate != null)
        {
            _test.shortWait().until(LabKeyExpectedConditions.elementIsEnabled(Locator.xpath("//input[@name='schemaTemplate']")));
            _test._ext4Helper.selectComboBoxItem("Schema Template:?", schemaTemplate);
            _test._ext4Helper.waitForMaskToDisappear(WebDriverWrapper.WAIT_FOR_PAGE);
        }

        if (sourceSchemaName != null)
        {
            if (schemaTemplate != null)
            {
                // click "Override template value" widget
                _test.click(Locator.xpath("id('sourceSchemaOverride')/span[text()='Override template value']"));
                _test.waitForElement(Locator.xpath("id('sourceSchemaOverride')/span[text()='Revert to template value']"));
            }
            _test.shortWait().until(LabKeyExpectedConditions.elementIsEnabled(Locator.xpath("//input[@name='sourceSchemaName']")));

            // There are multiple Ext form elements on this row, so the label for the actual combo box is empty
            _test._ext4Helper.selectComboBoxItem("", sourceSchemaName);

            if(_queryLoadTimeOut > 0)
                _test.waitForElement(Locator.css(".query-loaded-marker"), _queryLoadTimeOut);
            else
                _test.waitForElement(Locator.css(".query-loaded-marker"));
        }

        if (tables != null)
        {
            if (schemaTemplate != null)
            {
                // click "Override template value" widget
                _test.click(Locator.xpath("id('tablesOverride')/span[text()='Override template value']"));
                _test.waitForElement(Locator.xpath("id('tablesOverride')/span[text()='Revert to template value']"));
            }
            _test.shortWait().until(LabKeyExpectedConditions.elementIsEnabled(Locator.xpath("//input[@name='tables']")));

            _test._ext4Helper.selectComboBoxItem(Ext4Helper.Locators.formItemWithLabel("Published Tables:?"), Ext4Helper.TextMatchTechnique.CONTAINS, tables.split(","));
        }

        if (metadata != null)
        {
            if (schemaTemplate != null)
            {
                // click "Override template value" widget
                _test.click(Locator.xpath("id('metadataOverride')/span[text()='Override template value']"));
                _test.waitForElement(Locator.xpath("id('metadataOverride')/span[text()='Revert to template value']"));
            }
            _test.shortWait().until(LabKeyExpectedConditions.elementIsEnabled(Locator.xpath("//textarea[@name='metaData']")));

            _test.setFormElement(Locator.xpath("//textarea[@name='metaData']"), metadata);
        }

        // If the tables variable is null, and create is false, the code will not wait for the table value to be loaded
        // and will click save. The result is that table value is then set to empty.
        // Wait for the mask to disappear before clicking create or update.
        if (schemaTemplate == null)
            _test._ext4Helper.waitForMaskToDisappear();

        if (create)
        {
            _test.waitForText("will be published"); // added as a part of Issue 37078
            _test.clickButton("Create");
        }
        else
            _test.clickButton("Update");

        // Back on schema admin page, check the linked schema was created/updated.
        _test.waitForElement(Locator.xpath("//td[text()='" + name + "']"));
    }
}
