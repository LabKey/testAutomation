/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.test.pages.test;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.ui.grids.EditableGrid;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;

public class CoreComponentsTestPage extends LabKeyPage<CoreComponentsTestPage.ElementCache>
{
    public CoreComponentsTestPage(WebDriver driver)
    {
        super(driver);
    }

    public static CoreComponentsTestPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static CoreComponentsTestPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("core", containerPath, "components"));
        return new CoreComponentsTestPage(webDriverWrapper.getDriver());
    }

    private ReactSelect getComponentSelect()
    {
        return ReactSelect.finder(getDriver()).waitFor();
    }

    public EditableGrid getEditableGrid(String schema, String query)
    {
        getComponentSelect().select("EditableGridPanel");
        applySchemaQuery(schema, query);
        return new EditableGrid.EditableGridFinder(getDriver()).waitFor();
    }

    /**
     * Configures the current page to show a GridPanel containing the contents of the specified query
     * @return  a QueryGrid to wrap the GridPanel, once it is found
     */
    public QueryGrid getGridPanel(String schema, String query)
    {
        getComponentSelect().select("GridPanel");
        applySchemaQuery(schema, query);
        return new QueryGrid.QueryGridFinder(getDriver()).inPanelWithHeaderText("GridPanel").waitFor();
    }

    private void applySchemaQuery(String schema, String query)
    {
        Input.Input(Locator.input("schemaName"), getDriver()).waitFor().set(schema);
        Input.Input(Locator.input("queryName"), getDriver()).waitFor().set(query);
        Locator.button("Apply").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT).click();
    }

    /**
     * clears the selected component, if there is one, so that the current page can be re-used
     * to test a different component
     */
    public CoreComponentsTestPage clearSelectedComponent()
    {
        getComponentSelect().clearSelection();
        return new CoreComponentsTestPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {


    }
}
