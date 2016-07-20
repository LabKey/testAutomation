package org.labkey.test.pages.issues;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.BaseDesignerPage;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.RefindingWebElement;
import org.labkey.test.components.html.Select;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.PropertiesEditor.PropertyEditor;
import static org.labkey.test.components.html.RadioButton.RadioButton;
import static org.labkey.test.components.html.Select.Select;

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
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "admin", Maps.of("issueDefName", issueDefName)));
        return new AdminPage(driver.getDriver());
    }

    public LabKeyPage customizeEmailTemplate()
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

    public Select<SortDirection> commentSortDirection()
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

    public enum SortDirection implements Select.SelectOption
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
        public Select<SortDirection> commentSortSelect = Select(Locator.name("sortDirection")).findWhenNeeded(this);
        public RadioButton assignedToAllProjectUsersRadio = RadioButton(Locator.css(".assigned-to-group-project > input")).findWhenNeeded(this);
        public RadioButton assignedToSpecificGroupRadio = RadioButton(Locator.css(".assigned-to-group-specific > input")).findWhenNeeded(this);
        public Select assignedToSpecificGroupSelect = Select(Locator.name("assigned-to-group")).findWhenNeeded(this);
        public RadioButton noDefaultAssignedToRadio = RadioButton(Locator.css(".assigned-to-empty > input")).findWhenNeeded(this);
        public RadioButton specificDefaultAssignedToRadio = RadioButton(Locator.css(".assigned-to-specific-user > input")).findWhenNeeded(this);
        public Select defaultAssignedToSelect = Select(Locator.name("sortDirection")).findWhenNeeded(this);
        public PropertiesEditor configureFieldsPanel = PropertyEditor(getDriver()).withTitle("Configure Fields").findWhenNeeded();
    }

    public static class AssignToListOption
    {
        private Select.SelectOption option;

        public AssignToListOption(Select.SelectOption option)
        {
            this.option = option;
        }

        public static AssignToListOption specificUser(String specificUser)
        {
            return new AssignToListOption(Select.SelectOption.textOption(specificUser));
        }

        public static AssignToListOption specificUser(@NotNull Integer specificUserId)
        {
            return new AssignToListOption(Select.SelectOption.valueOption(specificUserId.toString()));
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
        public void setAssignedToList(AssignToListOption from)
        {
            if (null == from)
                elementCache().assignedToAllProjectUsersRadio.check();
            else
            {
                elementCache().assignedToSpecificGroupRadio.check();
                elementCache().assignedToSpecificGroupSelect.selectOption(from.option);
            }
        }

        public Select.SelectOption getAssignToGroup()
        {
            return elementCache().assignedToSpecificGroupSelect.getSelection();
        }

        public AssignedToRadioOption getAssignedToRadioSelection()
        {
            if (elementCache().assignedToAllProjectUsersRadio.isChecked())
                return AssignedToRadioOption.AllProjectUsers;
            else
                return AssignedToRadioOption.SpecificGroup;
        }
    }
    
    public static class DefaultAssignToOption
    {
        private Select.SelectOption option;

        private DefaultAssignToOption(Select.SelectOption option)
        {
            this.option = option;
        }

        public static DefaultAssignToOption specificUser(String specificUser)
        {
            return new DefaultAssignToOption(Select.SelectOption.textOption(specificUser));
        }

        public static DefaultAssignToOption specificUser(@NotNull Integer specificUserId)
        {
            return new DefaultAssignToOption(Select.SelectOption.valueOption(specificUserId.toString()));
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
        public void setDefaultAssignedTo(DefaultAssignToOption defaultUser)
        {
            if (null == defaultUser)
                elementCache().noDefaultAssignedToRadio.check();
            else
            {
                elementCache().specificDefaultAssignedToRadio.check();
                elementCache().defaultAssignedToSelect.selectOption(defaultUser.option);
            }
        }

        public Select.SelectOption getDefaultAssignToUser()
        {
            return elementCache().defaultAssignedToSelect.getSelection();
        }

        public DefaultAssignedToRadioOption getDefaultAssignedToRadioSelection()
        {
            if (elementCache().noDefaultAssignedToRadio.isChecked())
                return DefaultAssignedToRadioOption.NoDefault;
            else
                return DefaultAssignedToRadioOption.SpecificUser;
        }
    }
}