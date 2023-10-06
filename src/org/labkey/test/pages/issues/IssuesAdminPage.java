/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.pages.issues;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.domain.DomainDesigner;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_PAGE;
import static org.labkey.test.WebDriverWrapper.waitFor;

/**
 * Automates the LabKey ui components defined in: packages/components/src/components/domainproperties/issues/IssuesListDefDesignerPanels.tsx
 * Currently only exposed in LKS. Move to 'org.labkey.test.components.issues.IssuesListDesigner' once needed for LKB or LKSM
 */

public class IssuesAdminPage extends DomainDesigner<IssuesAdminPage.ElementCache>
{
    public IssuesAdminPage(WebDriver driver)
    {
        super(driver);
    }

    public static IssuesAdminPage beginAt(WebDriverWrapper driver, String issueDefName)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), issueDefName);
    }

    public static IssuesAdminPage beginAt(WebDriverWrapper driver, String containerPath, String issueDefName)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "admin", Maps.of("issueDefName", issueDefName.toLowerCase())));
        return new IssuesAdminPage(driver.getDriver());
    }

    public void waitForPage()
    {
        waitFor(() -> getFieldsPanel().getComponentElement().isDisplayed(),
                "The page did not render in time", WAIT_FOR_PAGE);
    }

    public String getSingularName()
    {
        return elementCache().singularNameInput.get();
    }

    public IssuesAdminPage setSingularName(String value)
    {
        expandPropertiesPanel();
        elementCache().singularNameInput.set(value);
        return this;
    }

    public String getPluralName()
    {
        return elementCache().pluralNameInput.get();
    }

    public IssuesAdminPage setPluralName(String value)
    {
        expandPropertiesPanel();
        elementCache().pluralNameInput.set(value);
        return this;
    }

    public SortDirection getCommentSortDirection()
    {
        return SortDirection.valueOf(elementCache().commentSortDirSelect.getValue());
    }

    public IssuesAdminPage setCommentSortDirection(SortDirection value)
    {
        elementCache().commentSortDirSelect.select(value.getText());
        return this;
    }

    public String getAssignedTo()
    {
        return elementCache().assignedToSelect.getValue();
    }

    public IssuesAdminPage setAssignedTo(String value)
    {
        if (value == null)
            elementCache().assignedToSelect.clearSelection();
        else
            elementCache().assignedToSelect.select(value);
        return this;
    }

    public List<String> getAllDefaultUserOptions()
    {
        return elementCache().defaultUserSelect.getOptions();
    }

    public String getDefaultUser()
    {
        return elementCache().defaultUserSelect.getValue();
    }

    public IssuesAdminPage setDefaultUser(String value)
    {
        if (value == null)
            elementCache().defaultUserSelect.clearSelection();
        else
            elementCache().defaultUserSelect.select(value);
        return this;
    }

    @Override
    public ListPage clickSave()
    {
        getWrapper().clickAndWait(elementCache().saveButton);
        return new ListPage(getDriver());
    }

    @Override
    public ListPage clickCancel()
    {
        getWrapper().clickAndWait(elementCache().cancelButton);
        return new ListPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    public enum SortDirection implements OptionSelect.SelectOption
    {
        OldestFirst("ASC", "Oldest first"),
        NewestFirst("DESC", "Newest first");

        private final String _value;
        private final String _text;

        SortDirection(String value, String text)
        {
            _value = value;
            _text = text;
        }

        @Override
        public String getValue()
        {
            return _value;
        }

        @Override
        public String getText()
        {
            return _text;
        }
    }

    protected class ElementCache extends DomainDesigner<?>.ElementCache
    {
        Input singularNameInput = new Input(Locator.inputById("singularItemName").findWhenNeeded(propertiesPanel), getDriver());
        Input pluralNameInput = new Input(Locator.inputById("pluralItemName").findWhenNeeded(propertiesPanel), getDriver());
        WebElement commentSortDirRow = Locator.tagWithClass("div", "row").containingIgnoreCase("Comment Sort").findWhenNeeded(this);
        ReactSelect commentSortDirSelect = ReactSelect.finder(getDriver()).findWhenNeeded(commentSortDirRow);
        WebElement assignedToRow = Locator.tagWithClass("div", "row").containingIgnoreCase("Assigned To").findWhenNeeded(this);
        ReactSelect assignedToSelect = ReactSelect.finder(getDriver()).findWhenNeeded(assignedToRow);
        WebElement defaultUserRow = Locator.tagWithClass("div", "row").containingIgnoreCase("Default User").findWhenNeeded(this);
        ReactSelect defaultUserSelect = ReactSelect.finder(getDriver()).findWhenNeeded(defaultUserRow);
    }
}
