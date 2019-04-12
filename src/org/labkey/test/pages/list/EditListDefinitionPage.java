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
package org.labkey.test.pages.list;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

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

    // TODO: List Properties

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
