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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.components.html.EnumSelect;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.BaseDesignerPage;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.RefindingWebElement;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.PropertiesEditor.PropertiesEditor;
import static org.labkey.test.components.html.RadioButton.RadioButton;
import static org.labkey.test.components.html.OptionSelect.OptionSelect;

public class AdminPage extends BaseDesignerPage<AdminPage.ElementCache>
{
    public AdminPage(WebDriver driver)
    {
        super(driver);
    }

    public static AdminPage beginAt(WebDriverWrapper driver, String issueDefName)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), issueDefName);
    }

    public static AdminPage beginAt(WebDriverWrapper driver, String containerPath, String issueDefName)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "admin", Maps.of("issueDefName", issueDefName.toLowerCase())));
        return new AdminPage(driver.getDriver());
    }

    public ListPage cancel()
    {
        clickButton("Cancel");
        return new ListPage(getDriver());
    }

    public LabKeyPage clickEmailTemplate()
    {
        clickAndWait(elementCache().customizeEmailButton);
        return new LabKeyPage(getDriver());
    }

    public Input singularName()
    {
        return elementCache().singularNameInput;
    }

    public Input pluralName()
    {
        return elementCache().pluralNameInput;
    }

    public EnumSelect<SortDirection> commentSortDirection()
    {
        return elementCache().commentSortSelect;
    }

    public AssignToList assignToList()
    {
        return new AssignToList();
    }

    public DefaultAssignTo defaultAssignedTo()
    {
        return new DefaultAssignTo();
    }

    public PropertiesEditor configureFields()
    {
        return elementCache().configureFieldsPanel;
    }

    @LogMethod
    public AdminPage setIssueAssignmentList(@Nullable @LoggedParam String group)
    {
        if (group != null)
            assignToList().set(AssignToListOption.specificGroup(group));
        else
            assignToList().set(AssignToListOption.allProjectUsers());
        return this;
    }

    @LogMethod
    public AdminPage setIssueAssignmentUser(@Nullable @LoggedParam String user)
    {
        if (user != null)
            defaultAssignedTo().set(DefaultAssignToOption.specificUser(user));
        else
            defaultAssignedTo().set(DefaultAssignToOption.noDefault());
        return this;
    }

    @Override
    public ListPage save()
    {
        clickButton("Save");
        return new ListPage(getDriver());
    }

    @Override
    public ListPage saveAndClose()
    {
        return save();
    }

    public enum SortDirection implements OptionSelect.SelectOption
    {
        OldestFirst("ASC", "Oldest First"),
        NewestFirst("DESC", "Newest First");

        private String _value;
        private String _text;

        SortDirection(String value, String text)
        {
            _value = value;
            _text = text;
        }

        public String getValue()
        {
            return _value;
        }

        public String getText()
        {
            return _text;
        }
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BaseDesignerPage.ElementCache
    {
        public WebElement customizeEmailButton = new RefindingWebElement(Locator.lkButton("Customize Email Template"), this);
        public Input singularNameInput = new Input(Locator.id("entrySingularName").findWhenNeeded(this), getDriver());
        public Input pluralNameInput = new Input(Locator.id("entryPluralName").findWhenNeeded(this), getDriver());
        public EnumSelect<SortDirection> commentSortSelect = EnumSelect.EnumSelect(Locator.name("sortDirection"), SortDirection.class).findWhenNeeded(this);
        public RadioButton assignedToAllProjectUsersRadio = RadioButton(Locator.css(".assigned-to-group-project > input")).findWhenNeeded(this);
        public RadioButton assignedToSpecificGroupRadio = RadioButton(Locator.css(".assigned-to-group-specific > input")).findWhenNeeded(this);
        public OptionSelect<OptionSelect.SelectOption> assignedToSpecificGroupSelect = OptionSelect(Locator.css("select.assigned-to-group")).findWhenNeeded(this);
        public RadioButton noDefaultAssignedToRadio = RadioButton(Locator.css(".assigned-to-empty > input")).findWhenNeeded(this);
        public RadioButton specificDefaultAssignedToRadio = RadioButton(Locator.css(".assigned-to-specific-user > input")).findWhenNeeded(this);
        public OptionSelect<OptionSelect.SelectOption> defaultAssignedToSelect = OptionSelect(Locator.css("select.assigned-to-user")).findWhenNeeded(this);
        public PropertiesEditor configureFieldsPanel = PropertiesEditor(getDriver()).withTitle("Configure Fields").findWhenNeeded();
    }

    public static class AssignToListOption
    {
        private OptionSelect.SelectOption option;

        public AssignToListOption(OptionSelect.SelectOption option)
        {
            this.option = option;
        }

        public static AssignToListOption specificGroup(String group)
        {
            return new AssignToListOption(OptionSelect.SelectOption.textOption(group));
        }

        public static AssignToListOption specificGroup(@NotNull Integer groupId)
        {
            return new AssignToListOption(OptionSelect.SelectOption.valueOption(groupId.toString()));
        }

        public static AssignToListOption allProjectUsers()
        {
            return null;
        }
    }

    public enum AssignedToRadioOption
    {
        AllProjectUsers,
        SpecificGroup
    }
    
    public class AssignToList
    {
        public void set(AssignToListOption from)
        {
            if (null == from)
                elementCache().assignedToAllProjectUsersRadio.check();
            else
            {
                elementCache().assignedToSpecificGroupRadio.check();
                elementCache().assignedToSpecificGroupSelect.selectOption(from.option);
            }
        }

        public OptionSelect.SelectOption get()
        {
            return elementCache().assignedToSpecificGroupSelect.getSelection();
        }

        public AssignedToRadioOption getRadioSelection()
        {
            if (elementCache().assignedToAllProjectUsersRadio.isChecked())
                return AssignedToRadioOption.AllProjectUsers;
            else
                return AssignedToRadioOption.SpecificGroup;
        }
    }
    
    public static class DefaultAssignToOption
    {
        private OptionSelect.SelectOption option;

        private DefaultAssignToOption(OptionSelect.SelectOption option)
        {
            this.option = option;
        }

        public static DefaultAssignToOption specificUser(String displayName)
        {
            return new DefaultAssignToOption(OptionSelect.SelectOption.textOption(displayName));
        }

        public static DefaultAssignToOption specificUser(@NotNull Integer userId)
        {
            return new DefaultAssignToOption(OptionSelect.SelectOption.valueOption(userId.toString()));
        }

        public static DefaultAssignToOption noDefault()
        {
            return null;
        }
    }

    public enum DefaultAssignedToRadioOption
    {
        NoDefault,
        SpecificUser
    }
    
    public class DefaultAssignTo
    {
        public void set(DefaultAssignToOption defaultUser)
        {
            if (null == defaultUser)
                elementCache().noDefaultAssignedToRadio.check();
            else
            {
                elementCache().specificDefaultAssignedToRadio.check();
                elementCache().defaultAssignedToSelect.selectOption(defaultUser.option);
            }
        }

        public OptionSelect.SelectOption get()
        {
            return elementCache().defaultAssignedToSelect.getSelection();
        }

        public DefaultAssignedToRadioOption getRadioSelection()
        {
            if (elementCache().noDefaultAssignedToRadio.isChecked())
                return DefaultAssignedToRadioOption.NoDefault;
            else
                return DefaultAssignedToRadioOption.SpecificUser;
        }
    }
}
