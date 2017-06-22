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
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class StartImportPage extends LabKeyPage
{
    public StartImportPage(WebDriver test)
    {
        super(test);
    }

    public static StartImportPage startImportFromFile(BaseWebDriverTest test, File zipFile, boolean validateQueries, boolean showAdvancedImportOptions)
    {
        StartImportPage sip = new StartImportPage(test.getDriver());

        test.goToFolderManagement();
        test.clickAndWait(Locator.linkWithText("Import"));
        test.waitForElement(Locator.name("folderZip"));
        test.setFormElement(Locator.name("folderZip"), zipFile);

        sip.setValidateQueriesCheckBox(validateQueries);
        sip.setAdvancedImportOptionsCheckBox(showAdvancedImportOptions);

        test.clickButtonContainingText("Import Folder");
        test.waitForText("Select specific objects to import");

        return sip;
    }

    public static StartImportPage startImportFromPipeline(BaseWebDriverTest test, File zipFile, boolean validateQueries, boolean selectSpecificImportOptions)
    {
        StartImportPage sip = new StartImportPage(test.getDriver());
        FileBrowserHelper fileBrowserHelper = new FileBrowserHelper(test);

        test.goToFolderManagement();
        test.clickAndWait(Locator.linkWithText("Import"));
        test.waitForElement(Locator.linkWithText("Use Pipeline"));
        test.click(Locator.linkWithText("Use Pipeline"));

        fileBrowserHelper.uploadFile(zipFile);
        fileBrowserHelper.importFile(zipFile.getName(), "Import Folder");
        test.waitForText("Import Folder from Pipeline");

        sip.setValidateQueriesCheckBox(validateQueries);
        sip.setSelectSpecificImportOptions(selectSpecificImportOptions);

        return sip;
    }

    public void setValidateQueriesCheckBox(boolean check)
    {
        setInitialCheckBox(Locator.css("input[name='validateQueries']"), check);
    }

    public void setAdvancedImportOptionsCheckBox(boolean check)
    {
        setInitialCheckBox(Locator.css("input[name='advancedImportOptions']"), check);
    }

    public void setSelectSpecificImportOptions(boolean check)
    {
        setInitialCheckBox(Locator.css("input[name='specificImportOptions']"), check);
    }

    public void setApplyToMultipleFoldersCheckBox(boolean check)
    {
        setInitialCheckBox(Locator.css("input[name='applyToMultipleFolders']"), check);
    }

    public void setFailForUndefinedVisitsCheckBox(boolean check)
    {
        setInitialCheckBox(Locator.css("input[name='failForUndefinedVisits']"), check);
    }

    private void setInitialCheckBox(Locator checkBox, boolean check)
    {
        if (check)
            checkCheckbox(checkBox);
        else
            uncheckCheckbox(checkBox);
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
    public void setAdvancedOptionCheckBoxes(Map<AdvancedOptionsCheckBoxes, Boolean> checkBoxes)
    {
        for(Map.Entry<AdvancedOptionsCheckBoxes, Boolean> checkBox : checkBoxes.entrySet())
        {
            log("Setting value for checkbox: " + checkBox.getKey().toString());
            if(checkBox.getValue())
                checkCheckbox(getCheckBoxLocatorCss(checkBox.getKey()));
            else
                uncheckCheckbox(getCheckBoxLocatorCss(checkBox.getKey()));
        }
    }

    public void setAllAdvancedOptionCheckBoxes(boolean checked)
    {
        List<WebElement> chkBoxes = Locator.css("div.advanced-options-panel div.x4-column-layout-ct div.x4-panel-body input").findElements(getDriver());
        chkBoxes
                .stream()
                .forEach(chkBox -> {
                    if (!chkBox.getAttribute("value").toLowerCase().equals("study"))
                    {
                        if (checked)
                            checkCheckbox(chkBox);
                        else
                            uncheckCheckbox(chkBox);
                    }
                });
    }

    public Locator getCheckBoxLocatorCss(AdvancedOptionsCheckBoxes chkBox)
    {
        return Locator.css("input[value='" + chkBox.getValue() + "']");
    }

    public Locator getCheckBoxLocatorXpath(AdvancedOptionsCheckBoxes chkBox)
    {
        return Locator.xpath("input[value='" + chkBox.getValue() + "']");
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
        ExperimentsAndRuns("Experiments and runs"),
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
    }
}
