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
     * <p>
     *     Set the Study Security Type. Can be one of the values in {@link StudySecurityType}, which is either basic
     *     (read-only or editable) or custom (read-only or editable).
     * </p>
     * <p>
     *     Must be set to a 'custom' security type for the group security panel and the per dataset permissions panel
     *     to be displayed.
     * </p>
     *
     * @param securityType A {@link StudySecurityType}
     * @return A reference to this page.
     */
    public StudySecurityPage setSecurityType(StudySecurityType securityType)
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
        clickAndWait(elementCache().cancelGroupButton);
        return new ManageStudyPage(getDriver());
    }

    /**
     * Is the 'Per Dataset Permissions' panel visible.
     *
     * @return True if visible false otherwise.
     */
    boolean isDatasetPermissionPanelVisible()
    {
        return elementCache().datasetSecurityForm.isDisplayed();
    }

    private static final String DATASET_PANEL_NOT_VISIBLE = "The 'Per Dataset Permissions' panel is not displayed.\nCheck that Study Security Type is not set to a basic type.";

    /**
     * Will set the permissions for a given dataset. Will not click the 'Save' button.
     *
     * @param datasetName Name of the dataset.
     * @param permission A {@link DatasetRoles} value.
     * @return A reference to this page.
     */
    public StudySecurityPage setDatasetPermissions(String datasetName, DatasetRoles permission)
    {
        Assert.assertTrue(DATASET_PANEL_NOT_VISIBLE, isDatasetPermissionPanelVisible());
        selectOptionByText(elementCache().datasetPermissionSelect(datasetName), permission.getText());
        return this;
    }

    /**
     * Will set the permissions for a one or more datasets. Will click the 'Save' button.
     *
     * @param permissions A Map where the dataset name is the key and a value from {@link DatasetRoles}.
     * @return A reference to this page.
     */
    public StudySecurityPage setDatasetPermissionsAndSave(Map<String, DatasetRoles> permissions)
    {
        Assert.assertTrue(DATASET_PANEL_NOT_VISIBLE, isDatasetPermissionPanelVisible());

        for(Map.Entry<String, DatasetRoles> entry : permissions.entrySet())
        {
            selectOptionByText(elementCache().datasetPermissionSelect(entry.getKey()), entry.getValue().getText());
        }

        return saveDatasetPermissions();
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
        Select securityType = SelectWrapper.Select(Locator.name("securityString")).findWhenNeeded(this);
        WebElement updateTypeButton = Locator.lkButton("Update Type").findWhenNeeded(this);

        WebElement groupUpdateForm = Locator.tagWithId("form", "groupUpdateForm").findWhenNeeded(this);

        WebElement groupRadioButton(String groupName, GroupSecuritySetting setting)
        {
            return Locator.xpath(
                    String.format("//td[text()='%s']/../th/input[@value='%s']", groupName, setting.getRadioValue()))
                    .findElement(groupUpdateForm);
        }

        WebElement updateGroupButton = Locator.lkButton("Update").findWhenNeeded(groupUpdateForm);
        WebElement cancelGroupButton = Locator.lkButton("Cancel").findWhenNeeded(groupUpdateForm);

        WebElement datasetSecurityForm = Locator.tagWithId("form", "datasetSecurityForm").findWhenNeeded(this);

        private WebElement datasetPermissionSelect(String dataSet)
        {
            // Example: "//td[text()='Quality Control Report']/following-sibling::td//select"
            return Locator.tagWithText("td", dataSet).followingSibling("td").childTag("select").findElement(datasetSecurityForm);
        }

        WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(datasetSecurityForm);
        WebElement readerButton = Locator.lkButton("Set All To Reader").findWhenNeeded(datasetSecurityForm);
        WebElement editorButton = Locator.lkButton("Set All To Editor").findWhenNeeded(datasetSecurityForm);
        WebElement clearButton = Locator.lkButton("Clear All").findWhenNeeded(datasetSecurityForm);
        WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(datasetSecurityForm);
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
     */
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