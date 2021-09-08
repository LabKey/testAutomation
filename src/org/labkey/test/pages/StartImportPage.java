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
package org.labkey.test.pages;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class StartImportPage extends LabKeyPage<StartImportPage.ElementCache>
{
    public StartImportPage(WebDriver test)
    {
        super(test);
    }

    public static StartImportPage startImportFromFile(BaseWebDriverTest test, File zipFile, boolean validateQueries)
    {
        test.goToFolderManagement();
        test.clickAndWait(Locator.linkWithText("Import"));
        test.waitForElement(Locator.name("folderZip"));
        test.setFormElement(Locator.name("folderZip"), zipFile);

        StartImportPage sip = new StartImportPage(test.getDriver());
        sip.setValidateQueriesCheckBox(validateQueries);
        sip.setAdvancedImportOptionsCheckBox(true);

        test.clickButtonContainingText("Import Folder");
        test.waitForText("Select specific objects to import");

        sip.clearCache();
        return sip;
    }

    public static StartImportPage startImportFromPipeline(BaseWebDriverTest test, File zipFile, boolean validateQueries, boolean selectSpecificImportOptions)
    {
        FileBrowserHelper fileBrowserHelper = new FileBrowserHelper(test);

        test.goToFolderManagement();
        test.clickAndWait(Locator.linkWithText("Import"));
        test.waitForElement(Locator.linkWithText("Use Pipeline"));
        test.click(Locator.linkWithText("Use Pipeline"));

        fileBrowserHelper.uploadFile(zipFile);
        fileBrowserHelper.importFile(zipFile.getName(), "Import Folder");
        test.waitForText("Import Folder from Pipeline");

        StartImportPage sip = new StartImportPage(test.getDriver());
        sip.setValidateQueriesCheckBox(validateQueries);
        sip.setSelectSpecificImportOptions(selectSpecificImportOptions);

        return sip;
    }

    public void setValidateQueriesCheckBox(boolean check)
    {
        elementCache().validateQueriesCheckbox.set(check);
    }

    public void setFailForUndefinedVisitsCheckBox(boolean check)
    {
        elementCache().failForUndefinedVisitsCheckbox.set(check);
    }

    public void setAdvancedImportOptionsCheckBox(boolean check)
    {
        elementCache().advancedImportOptionsCheckbox.set(check);
    }

    public void setSelectSpecificImportOptions(boolean check)
    {
        elementCache().specificImportOptionsCheckbox.set(check);
        shortWait().until(LabKeyExpectedConditions.visibilityOf(elementCache().advancedOptionsPanel, check));
    }

    public void setApplyToMultipleFoldersCheckBox(boolean check)
    {
        elementCache().applyToMultipleFoldersCheckbox.set(check);
        shortWait().until(LabKeyExpectedConditions.visibilityOf(elementCache().applyMultiplePanel, check));
    }

    public boolean isMultipleFolderImportAvailable()
    {
        return elementCache().applyToMultipleFoldersCheckbox.isDisplayed();
    }

    public void clickStartImport()
    {
        clickButton("Start Import");
    }

    public void clickStartImport(String confirmationText)
    {
        clickButton("Start Import", 0);

        Window confirmation = new Window("Confirmation", getDriver());
        assertEquals("Wrong confirmation message", confirmationText, confirmation.getBody());
        confirmation.clickButton("Yes");
    }

    public boolean isSelectSpecificImportOptionsVisible()
    {
        return isElementVisible(Locator.css("div.advanced-options-panel"));
    }

    public void setAdvancedOptionCheckBoxes(AdvancedOptionsCheckBoxes checkBox, boolean check)
    {
        Map<AdvancedOptionsCheckBoxes, Boolean> list = new HashMap<>();
        list.put(checkBox, check);
        setAdvancedOptionCheckBoxes(list);
    }

    @LogMethod()
    public void setAdvancedOptionCheckBoxes(Map<AdvancedOptionsCheckBoxes, Boolean> options)
    {
        for(Map.Entry<AdvancedOptionsCheckBoxes, Boolean> entry : options.entrySet())
        {
            log("Setting value for checkbox: " + entry.toString());
            elementCache().dataTypesCheckbox(entry.getKey()).set(entry.getValue());
        }
    }

    public void setAllAdvancedOptionCheckBoxes(boolean checked)
    {
        Checkbox.Checkbox(Locator.checkbox())
                .findAll(elementCache().advancedOptionsPanel)
                .forEach(cb -> cb.set(checked));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        protected final Checkbox validateQueriesCheckbox = initialCheckbox("validateQueries");
        protected final Checkbox advancedImportOptionsCheckbox = initialCheckbox("advancedImportOptions");
        protected final Checkbox specificImportOptionsCheckbox = initialCheckbox("specificImportOptions");
        protected final Checkbox applyToMultipleFoldersCheckbox = initialCheckbox("applyToMultipleFolders");
        protected final Checkbox failForUndefinedVisitsCheckbox = initialCheckbox("failForUndefinedVisits");

        protected final WebElement advancedOptionsPanel = Locator.byClass("advanced-options-panel").findWhenNeeded(this);
        protected final WebElement applyMultiplePanel = Locator.byClass("apply-multiple-panel").findWhenNeeded(this);

        public ElementCache()
        {
            shortWait().until(ExpectedConditions.visibilityOf(validateQueriesCheckbox.getComponentElement()));
        }

        public Checkbox initialCheckbox(String name)
        {
            return Checkbox.Checkbox(Locator.input(name)).findWhenNeeded(this);
        }

        public Checkbox dataTypesCheckbox(AdvancedOptionsCheckBoxes value)
        {
            return Checkbox.Checkbox(Locator.tagWithAttribute("input", "value", value.getValue()))
                    .findWhenNeeded(advancedOptionsPanel);
        }
    }

    public enum AdvancedOptionsCheckBoxes
    {
        AssaySchedule("Assay Schedule"),
        Categories("Categories"),
        CohortSettings("Cohort Settings"),
        ContainerSpecificModuleProperties("Container specific module properties"),
        CustomParticipantView("Custom Participant View"),
        CustomViews("Custom Views"),
        DatasetData("Dataset Data"),
        DatasetDefinitions("Dataset Definitions"),
        ExperimentsAndRuns("Experiments, Protocols, and Runs"),
        ExternalSchemaDefinitions("External schema definitions"),
        FolderTypeAndActiveModules("Folder type and active modules"),
        FullTextSearchSettings("Full-text search settings"),
        Lists("Lists"),
        MissingValueIndicators("Missing value indicators"),
        NotificationSettings("Notification settings"),
        ParticipantCommentSettings("Participant Comment Settings"),
        ParticipantGroups("Participant Groups"),
        ProjectLevelGroupsAndMembers("Project-level groups and members"),
        ProtocolDocuments("Protocol Documents"),
        QCStateSettings("QC State Settings"),
        Queries("Queries"),
        Reports("Reports"),
        RoleAssignmentsForUsersAndGroups("Role assignments for users and groups"),
//        Study("Study"),  // Study is there but currently not visible.
        SpecimenSettings("Specimens"),
        Specimens("Specimen Settings"),
        TopLevelStudyProperties("Top-level Study Properties"),
        TreatmentData("Treatment Data"),
        VisitMap("Visit Map"),
        WebpartPropertiesAndLayout("Webpart properties and layout"),
        WikisAndTheirAttachments("Wikis and their attachments");

        private String value;

        AdvancedOptionsCheckBoxes(String value)
        {
            this.value = value;
        }

        public String getValue()
        {
            return value;
        }

        @Override
        public String toString()
        {
            return value;
        }
    }
}
