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
package org.labkey.test.pages.study;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StudySecurityPage extends LabKeyPage<StudySecurityPage.ElementCache>
{
    public StudySecurityPage(WebDriver driver)
    {
        super(driver);
    }

    @Override
    protected void waitForPage()
    {
        waitFor(()-> elementCache().updateTypeButton.isDisplayed(),
                "The 'Manage Study Security' page did not load in time.",
                1_000);
    }

    public static StudySecurityPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static StudySecurityPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("study-security", containerPath, "begin"));
        return new StudySecurityPage(driver.getDriver());
    }

    /**
     *
     * Set the Study Security Type to a value from {@link StudySecurityType}. Does not click the update button.
     *
     * @param securityType Value from {@link StudySecurityType}.
     * @return A reference to this page.
     */
    public StudySecurityPage setSecurityType(StudySecurityType securityType)
    {
        elementCache().securityType.selectByValue(securityType.toString());
        return this;
    }

    /**
     * <p>
     *     Set the Study Security Type and click the 'Update Type' button. Can be one of the values in
     *     {@link StudySecurityType}, which is either basic (read-only or editable) or custom (read-only or editable).
     * </p>
     * <p>
     *     Must be set to a 'custom' security type for the group security panel and the per dataset permissions panel
     *     to be displayed.
     * </p>
     *
     * @param securityType A {@link StudySecurityType}
     * @return A reference to this page.
     */
    public StudySecurityPage setSecurityTypeAndUpdate(StudySecurityType securityType)
    {
        elementCache().securityType.selectByValue(securityType.toString());
        clickAndWait(elementCache().updateTypeButton);

        // Identify how many web parts should be present.
        int webPartCount = switch (securityType)
                {
                    case ADVANCED_READ, ADVANCED_WRITE -> 3;
                    default -> 1;
                };

        waitForElements(Locator.tagWithName("div", "webpart"), webPartCount);

        clearCache();
        return this;
    }

    /**
     * Get the value set in the Study Security Type selection box.
     *
     * @return A {@link StudySecurityType}.
     */
    public StudySecurityType getSecurityType()
    {
        return StudySecurityType.valueOf(getSelectedOptionValue(elementCache().securityType.getWrappedElement()));
    }

    /**
     * Is the 'Study Security' panel for groups visible.
     *
     * @return True if visible false otherwise.
     */
    public boolean isGroupStudySecurityVisible()
    {
        return elementCache().groupUpdateForm.isDisplayed();
    }

    private static final String GROUP_PANEL_NOT_VISIBLE = "The 'Study Security' panel for groups is not displayed.\nCheck that Study Security Type is not set to a basic type.";

    /**
     * Set the security level for a specific group. Will not click the 'Update' button.
     *
     * @param groupName Name of the group.
     * @param setting Permission setting (Read All, None, etc...)
     * @return A reference to this page.
     */
    public StudySecurityPage setGroupStudySecurity(String groupName, GroupSecuritySetting setting)
    {
        Assert.assertTrue(GROUP_PANEL_NOT_VISIBLE, isGroupStudySecurityVisible());
        elementCache().groupRadioButton(groupName, setting).click();
        return this;
    }

    /**
     * Set the security levels for one or more groups. Will click the 'Update' button.
     *
     * @param settings A Map with the group name as the key and a value from {@link GroupSecuritySetting}.
     * @return A reference to this page.
     */
    public StudySecurityPage setGroupStudySecurityAndUpdate(Map<String, GroupSecuritySetting> settings)
    {
        Assert.assertTrue(GROUP_PANEL_NOT_VISIBLE, isGroupStudySecurityVisible());
        for(Map.Entry<String, GroupSecuritySetting> entry : settings.entrySet())
        {
            elementCache().groupRadioButton(entry.getKey(), entry.getValue()).click();
        }
        return updateGroupSecurity();
    }

    /**
     * Click the 'Update' button on the 'Study Security' panel for groups.
     *
     * @return A reference to this page.
     */
    public StudySecurityPage updateGroupSecurity()
    {
        Assert.assertTrue(GROUP_PANEL_NOT_VISIBLE, isGroupStudySecurityVisible());
        clickAndWait(elementCache().updateGroupButton);
        clearCache();
        return this;
    }

    /**
     * Click the 'Cancel' button on the 'Study Security' panel for groups.
     *
     * @return Will send you back to the Manage Studies page {@link ManageStudyPage}.
     */
    public ManageStudyPage cancelGroupSecurity()
    {
        Assert.assertTrue(GROUP_PANEL_NOT_VISIBLE, isGroupStudySecurityVisible());
        clickAndWait(elementCache().cancelGroupButton);
        return new ManageStudyPage(getDriver());
    }

    /**
     * Get the displayed security roles that a group can be set to. These are the 'EDIT ALL', 'READ ALL' etc... roles.
     * If Study Security Type is read-only then the 'READ-ALL' role should not be present.
     *
     * @return List of the roles displayed.
     */
    public List<GroupSecuritySetting> getAllowedGroupRoles()
    {
        Assert.assertTrue(GROUP_PANEL_NOT_VISIBLE, isGroupStudySecurityVisible());

        // Unfortunately everything in the group roles table is in a 'th' tag. The roles are in the first row (tr).
        WebElement firstTr = Locator.tag("tr").findElement(elementCache().groupUpdateForm);
        List<WebElement> thElements = Locator.tag("th").findElements(firstTr);

        // Yes I know I could call getTexts to get the text of the elements, but the text needs to be cleaned up.
        // The first text entry can be ignored because it is the empty column header above the group names.
        // All the remaining text/roles need to have the trailing '?' removed, which is the help icon after each role.

        List<GroupSecuritySetting> roles = new ArrayList<>();

        for(WebElement th : thElements)
        {
            String text = th.getText();

            if(!text.isBlank())
            {
                text = text.trim().substring(0, text.lastIndexOf("?"));

                if(text.equalsIgnoreCase("EDIT ALL"))
                    roles.add(GroupSecuritySetting.EDIT_ALL);
                else if(text.equalsIgnoreCase("READ ALL"))
                    roles.add(GroupSecuritySetting.READ_ALL);
                else if(text.equalsIgnoreCase("PER DATASET"))
                    roles.add(GroupSecuritySetting.PER_DATASET);
                else
                    roles.add(GroupSecuritySetting.NONE);
            }
        }

        return roles;
    }

    /**
     * Get the list of groups displayed.
     *
     * @return List of the displayed groups.
     */
    public List<String> getGroups()
    {
        Assert.assertTrue(GROUP_PANEL_NOT_VISIBLE, isGroupStudySecurityVisible());

        List<WebElement> tds = Locator.tag("td").withoutAttribute("style").findElements(elementCache().groupUpdateForm);
        return getTexts(tds);
    }

    /**
     * Return the role that the given group is set to.
     *
     * @param groupName Name of the group.
     * @return A {@link GroupSecuritySetting} value, null if not set (unlikely).
     */
    public GroupSecuritySetting getRoleForGroup(String groupName)
    {
        Assert.assertTrue(GROUP_PANEL_NOT_VISIBLE, isGroupStudySecurityVisible());

        Assert.assertTrue(String.format("There is no group named '%s' listed. Please check the group name.", groupName),
                getGroups().contains(groupName));

        WebElement tr = Locator.tagWithText("td", groupName).parent().findElement(elementCache().groupUpdateForm);
        String value = Locator.tagWithAttribute("input", "checked").findElement(tr).getAttribute("value");

        if(value.equals(GroupSecuritySetting.EDIT_ALL.getRadioValue()))
            return GroupSecuritySetting.EDIT_ALL;

        if(value.equals(GroupSecuritySetting.READ_ALL.getRadioValue()))
            return GroupSecuritySetting.READ_ALL;

        if(value.equals(GroupSecuritySetting.PER_DATASET.getRadioValue()))
            return GroupSecuritySetting.PER_DATASET;

        if(value.equals(GroupSecuritySetting.NONE.getRadioValue()))
            return GroupSecuritySetting.NONE;

        return null;
    }

    /**
     * Is the 'Per Dataset Permissions' panel visible.
     *
     * @return True if visible false otherwise.
     */
    public boolean isDatasetPermissionPanelVisible()
    {
        return elementCache().datasetSecurityForm.isDisplayed();
    }

    private static final String DATASET_PANEL_NOT_VISIBLE = "The 'Per Dataset Permissions' panel is not displayed.\nCheck that Study Security Type is not set to a basic type.";

    /**
     * Will set the permissions for a given dataset. Will not click the 'Save' button.
     *
     * @param groupName  Name of the group that will have the dataset permission.
     * @param datasetName Name of the dataset.
     * @param role Label for the desired role.
     * @return A reference to this page.
     */
    public StudySecurityPage setDatasetPermissions(String groupName, String datasetName, String role)
    {
        int position = getGroupColumnIndex(groupName);
        WebElement selectElement = elementCache().datasetPermissionSelect(datasetName, position);
        scrollToMiddle(selectElement);
        selectOptionByText(selectElement, role);
        return this;
    }

    @Deprecated
    public StudySecurityPage setDatasetPermissions(String groupName, String datasetName, DatasetRoles permission)
    {
        return setDatasetPermissions(groupName, datasetName, permission.getText());
    }

    /**
     * Will set the permissions for a one or more datasets. Will click the 'Save' button.
     *
     * @param groupName  Name of the group that will have the dataset permission.
     * @param permissions A Map where the dataset name is the key and a value from {@link DatasetRoles}.
     * @return A reference to this page.
     */
    @Deprecated
    public StudySecurityPage setDatasetPermissionsAndSave(String groupName, Map<String, DatasetRoles> permissions)
    {
        Assert.assertTrue(DATASET_PANEL_NOT_VISIBLE, isDatasetPermissionPanelVisible());

        int position = getGroupColumnIndex(groupName);

        for(Map.Entry<String, DatasetRoles> entry : permissions.entrySet())
        {
            selectOptionByText(elementCache().datasetPermissionSelect(entry.getKey(), position), entry.getValue().getText());
        }

        return saveDatasetPermissions();
    }

    /**
     * For a group set the permissions for all datasets.
     *
     * @param groupName Name of the group.
     * @param role The role to apply
     * @return A reference to this page.
     */
    public StudySecurityPage setRoleForAllDatasets(String groupName, String role)
    {
        Assert.assertTrue(DATASET_PANEL_NOT_VISIBLE, isDatasetPermissionPanelVisible());

        selectOptionByText(elementCache().allDatasetPermissionSelect(groupName), role);

        return this;
    }

    /**
     * Return the per-dataset permission for a group.
     *
     * @param groupName The name of the group.
     * @param datasetName The name of the dataset.
     * @return The {@link DatasetRoles} permission.
     */
    public String getDatasetRole(String groupName, String datasetName)
    {
        int position = getGroupColumnIndex(groupName);

        return getSelectedOptionText(elementCache().datasetPermissionSelect(datasetName, position));
    }

    /**
     * Return the per-dataset permission for a group.
     *
     * @param groupName The name of the group.
     * @param datasetName The name of the dataset.
     * @return The {@link DatasetRoles} permission.
     */
    @Deprecated
    public DatasetRoles getDatasetPermission(String groupName, String datasetName)
    {
        String text = getDatasetRole(groupName, datasetName);

        if(text.equals(DatasetRoles.AUTHOR.getText()))
            return DatasetRoles.AUTHOR;

        if(text.equals(DatasetRoles.EDITOR.getText()))
            return DatasetRoles.EDITOR;

        if(text.equals(DatasetRoles.NONE.getText()))
            return DatasetRoles.NONE;

        if(text.equals(DatasetRoles.READER.getText()))
            return DatasetRoles.READER;

        return null;
    }

    // The place holder text in the dropdown used to set permissions for a dataset.
    private static final String PER_DS_PLACE_HOLDER = "set all to";

    /**
     * Get the role options that are available for 'Per Dataset Permissions'. This will look only at the select control
     * at the top of the column for a given group.
     *
     * @param groupName Name of the a group.
     * @return A list of available roles defined by {@link DatasetRoles}.
     */
    @Deprecated
    public List<DatasetRoles> getAllowedDatasetPermissions(String groupName)
    {
        Assert.assertTrue(DATASET_PANEL_NOT_VISIBLE, isDatasetPermissionPanelVisible());

        List<String> stringRoles = getSelectOptions(elementCache().allDSPermissionsLocator(groupName));
        List<DatasetRoles> roles = new ArrayList<>();

        for(String role : stringRoles)
        {
            if(!role.toLowerCase().contains(PER_DS_PLACE_HOLDER.toLowerCase()))
            {

                if (role.equals(DatasetRoles.NONE.getText()))
                    roles.add(DatasetRoles.NONE);
                if (role.equals(DatasetRoles.READER.getText()))
                    roles.add(DatasetRoles.READER);
                if (role.equals(DatasetRoles.EDITOR.getText()))
                    roles.add(DatasetRoles.EDITOR);
                if (role.equals(DatasetRoles.AUTHOR.getText()))
                    roles.add(DatasetRoles.AUTHOR);
            }
        }

        return roles;
    }

    /**
     * Get a list of the datasets that can have individual permissions set.
     *
     * @return List of the displayed datasets.
     */
    public List<String> getDatasetsListed()
    {
        Assert.assertTrue(DATASET_PANEL_NOT_VISIBLE, isDatasetPermissionPanelVisible());

        List<String> dataClasses = new ArrayList<>();

        // Get the first td of each row in the Per Dataset Permissions grid. This will contain the list of datasets.
        List<WebElement> tdElements = Locator.tag("table")
                .childTag("tbody")
                .childTag("tr")
                .childTag("td")
                .position(1).findElements(elementCache().datasetSecurityForm);


        // Need to ignore some of the non-dataset rows, in this case the first two rows.
        // One is a blank td (column header) and the other is a select box that is for all datasets.
        for(WebElement td : tdElements)
        {
            String text = td.getText();

            if(!text.isBlank() && !text.contains(PER_DS_PLACE_HOLDER))
            {
                dataClasses.add(td.getText());
            }
        }

        return dataClasses;
    }

    /**
     * <p>
     *     A dataset will be highlighted in bold if it needs special considerations. For example datasets that are
     *     alt-ids may require that the user has read access if they want to insert new records in another dataset that
     *     uses the alt-ids.
     * </p>
     * <p>
     *     This will return true if a dataset is in bold in the per-dataset permissions grid.
     * </p>
     *
     * @param datasetName Name of the dataset.
     * @return True if the dataset name is in bold, false otherwise.
     */
    public boolean isDatasetHighlighted(String datasetName)
    {

        Assert.assertTrue(DATASET_PANEL_NOT_VISIBLE, isDatasetPermissionPanelVisible());

        return isElementPresent(Locator.tag("table")
                .childTag("tbody")
                .childTag("tr")
                .childTag("td").withText(datasetName).childTag("b"));

    }

    /**
     * Return the list of groups shown in the 'Per Dataset Permissions' panel.
     *
     * @return A list of group names.
     */
    public List<String> getGroupsListedInPerDatasets()
    {
        Assert.assertTrue(DATASET_PANEL_NOT_VISIBLE, isDatasetPermissionPanelVisible());

        List<WebElement> thElements = Locator.tag("table")
                .childTag("tbody")
                .childTag("tr").position(1)
                .childTag("th").findElements(elementCache().datasetSecurityForm);

        return getTexts(thElements);
    }

    /**
     * Private function. Get the column index of the given group in the Per Dataset Permissions table. Asserts that
     * the group has per-dataset permissions. Basically this is used to identify which combo-box should be used when
     * setting a per-dataset permission for a group.
     *
     * @param groupName Name of the group.
     * @return The 0 based column index.
     */
    private int getGroupColumnIndex(String groupName)
    {
        int index = -1;

        List<String> groups = getGroupsListedInPerDatasets();

        for(int i = 0; i < groups.size(); i++)
        {
            if(groups.get(i).equals(groupName))
            {
                index = i;
                break;
            }
        }

        Assert.assertNotEquals(
                String.format("It doesn't look like group '%s' has per-dataset permissions.", groupName),
                -1, index);

        // Need to subtract 1 because column position is not the same as the position of the combobox in the collection.
        return index - 1;
    }

    /**
     * Click the 'Save' button on the Per Dataset Permissions panel.
     *
     * @return A reference to this page.
     */
    public StudySecurityPage saveDatasetPermissions()
    {
        clickAndWait(elementCache().saveButton);
        clearCache();
        return this;
    }

    /**
     * Will set the permissions for all the datasets to 'Reader'.
     *
     * @return A reference to this page.
     */
    public StudySecurityPage setAllToReader()
    {
        elementCache().readerButton.click();
        return this;
    }

    /**
     * Will set the permissions for all the datasets to 'Editor'.
     *
     * @return A reference to this page.
     */
    public StudySecurityPage setAllToEditor()
    {
        elementCache().editorButton.click();
        return this;
    }

    /**
     * Will set the permissions for all the datasets to 'None'.
     *
     * @return A reference to this page.
     */
    public StudySecurityPage clearAll()
    {
        elementCache().clearButton.click();
        return this;
    }

    /**
     * Click the 'Cancel' button on the 'Per Dataset Permissions' panel.
     *
     * @return Will send you back to the Manage Studies page {@link ManageStudyPage}.
     */
    public ManageStudyPage cancel()
    {
        clickAndWait(elementCache().cancelButton);
        return new ManageStudyPage(getDriver());
    }

    //TODO Need to add Import/Export Policy panel.

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        final Select securityType = SelectWrapper.Select(Locator.name("securityString")).findWhenNeeded(this);
        final WebElement updateTypeButton = Locator.lkButton("Update Type").findWhenNeeded(this);

        final WebElement groupUpdateForm = Locator.tagWithId("form", "groupUpdateForm").findWhenNeeded(this);

        final WebElement groupRadioButton(String groupName, GroupSecuritySetting setting)
        {
            return Locator.xpath(
                    String.format("//td[text()='%s']/../th/input[@value='%s']", groupName, setting.getRadioValue()))
                    .findElement(groupUpdateForm);
        }

        final WebElement updateGroupButton = Locator.lkButton("Update").findWhenNeeded(groupUpdateForm);
        final WebElement cancelGroupButton = Locator.lkButton("Cancel").findWhenNeeded(groupUpdateForm);

        final WebElement datasetSecurityForm = Locator.tagWithId("form", "datasetSecurityForm").findWhenNeeded(this);

        final WebElement datasetPermissionSelect(String dataSet, int position)
        {
            // Example: "//td[text()='Quality Control Report']/following-sibling::td//select"
            // Multiple groups could have the "Per Dataset" set so there will be multiple dropdowns for a dataset,
            // position identifies which one to get.
            return Locator.tagWithText("td", dataSet)
                    .followingSibling("td")
                    .childTag("select").findElements(datasetSecurityForm).get(position);
        }

        final private Locator allDSPermissionsLocator(String groupName)
        {
            return Locator.tagWithName("select", groupName);
        }

        final WebElement allDatasetPermissionSelect(String groupName)
        {
            return allDSPermissionsLocator(groupName).findElement(datasetSecurityForm);
        }

        final WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(datasetSecurityForm);
        final WebElement readerButton = Locator.lkButton("Set All To Reader").findWhenNeeded(datasetSecurityForm);
        final WebElement editorButton = Locator.lkButton("Set All To Editor").findWhenNeeded(datasetSecurityForm);
        final WebElement clearButton = Locator.lkButton("Clear All").findWhenNeeded(datasetSecurityForm);
        final WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(datasetSecurityForm);
    }

    /**
     * Values for the Study Security Type.
     */
    public enum StudySecurityType
    {
        BASIC_READ,
        BASIC_WRITE,
        ADVANCED_READ,
        ADVANCED_WRITE
    }

    /**
     * Values for the 'Study Security' group panel.
     */
    public enum GroupSecuritySetting
    {
        EDIT_ALL("UPDATE"),
        READ_ALL("READ"),
        PER_DATASET("READOWN"),
        NONE("NONE");

        private final String _value;

        GroupSecuritySetting(String value)
        {
            _value = value;
        }

        public String getRadioValue()
        {
            return _value;
        }
    }

    /**
     * Permissions that can be set per dataset.
     * @deprecated Available roles are not static
     */
    @Deprecated
    public enum DatasetRoles
    {
        NONE("None"),
        READER("Reader"),
        AUTHOR("Author"),
        EDITOR("Editor");

        private final String _text;

        DatasetRoles(String text)
        {
            _text = text;
        }

        public String getText()
        {
            return _text;
        }
    }

}
