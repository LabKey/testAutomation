/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.pages.ManageDatasetsPage;
import org.labkey.test.pages.study.CreateStudyPage;
import org.labkey.test.pages.study.ManageVisitPage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.DataRegionTable.DataRegion;

public class StudyHelper
{
    protected BaseWebDriverTest _test;

    public StudyHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public CreateStudyPage startCreateStudy()
    {
        _test.clickAndWait(Locator.lkButton("Create Study"));
        return new CreateStudyPage(_test);
    }

    @LogMethod
    public void createCustomParticipantGroup(String projectName, String studyFolder, String groupName, String participantString, String... ptids)
    {
        createCustomParticipantGroup(projectName, studyFolder, groupName, participantString, null, ptids);
    }

    @LogMethod
    public void createCustomParticipantGroup(String projectName, String studyFolder, String groupName, String participantString,
                                             @Nullable Boolean shared, String... ptids)
    {
        createCustomParticipantGroup(projectName, studyFolder, groupName, participantString, null, false, shared, ptids);
    }

    @LogMethod
    public void createCustomParticipantGroup(String projectName, String studyFolder, String groupName, String participantString,
                                             @Nullable Boolean shared, Boolean demographicsPresent, String... ptids)
    {
        createCustomParticipantGroup(projectName, studyFolder, groupName, participantString, null, false, shared, demographicsPresent, ptids);
    }

    @LogMethod
    public void createCustomParticipantGroup(String projectName, String studyFolder, String groupName, String participantString,
                                             @Nullable String categoryName, boolean isCategoryNameNew, @Nullable Boolean shared, String... ptids)
    {
        createCustomParticipantGroup(projectName, studyFolder, groupName, participantString, categoryName, isCategoryNameNew, shared, true, ptids);
    }

    @LogMethod
    public void createCustomParticipantGroup(String projectName, String studyFolder, String groupName, String participantString,
                                             @Nullable String categoryName, boolean isCategoryNameNew, @Nullable Boolean shared, Boolean demographicsPresent, String... ptids)
    {
        if (!_test.isElementPresent(Locators.bodyTitle("Manage " + participantString + " Groups")) )
        {
            _test.clickProject(projectName);
            if (!projectName.equals(studyFolder))
                _test.clickFolder(studyFolder);
            _test.clickTab("Manage");
            _test.click(Locator.linkWithText("Manage " + participantString + " Groups"));
            _test.waitForText("groups allow");
        }
        _test.log("Create "+participantString+" Group: " + groupName);
        _test.clickButton("Create", 0);
        _test._extHelper.waitForExtDialog("Define "+participantString+" Group");
        _test.waitForElement(Locators.pageSignal(DataRegionTable.UPDATE_SIGNAL));
        if (demographicsPresent)
            DataRegion(_test.getDriver()).withName("demoDataRegion").waitFor();
        _test.setFormElement(Locator.name("groupLabel"), groupName);
        if( ptids.length > 0 )
        {
            String csp = ptids[0];
            for( int i = 1; i < ptids.length; i++ )
                csp += ","+ptids[i];
            _test.setFormElement(Locator.name("participantIdentifiers"), csp);
        }
        if( categoryName != null )
        {
            if (isCategoryNameNew)
                _test.setFormElement(Locator.name("participantCategory"), categoryName);
            else
                _test._ext4Helper.selectComboBoxItem(participantString + " Category:", categoryName);
            _test.pressTab(Locator.name("participantCategory"));
            _test.waitForElementToDisappear(Locator.css(".x-form-focus"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }
        if ( shared != null )
        {
            if( shared )
            {
                _test._ext4Helper.checkCheckbox("Share Category?");
            }
            else
            {
                _test._ext4Helper.uncheckCheckbox("Share Category?");
            }
        }

        _test.click(Ext4Helper.Locators.ext4Button("Save"));
        _test._ext4Helper.waitForMaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod
    public void editCustomParticipantGroup(String groupName, String participantString,
                                                  @Nullable String categoryName, Boolean isCategoryNameNew, @Nullable Boolean shared, Boolean demographicsPresent,
                                                  Boolean replaceExistingPtids,  String... newPtids)
    {
       // Caller must already be on Manage <participantString> Groups page
        // And there should be NO DEMOGRAPHICS DATASETS!

        _test.log("Edit " + participantString + " Group: " + groupName);

        // Select row
        selectParticipantCategoriesGridRow(groupName);

        String oldCategory = _test.getText(Locator.css("tr.x4-grid-row-selected > td.x4-grid-cell:nth-of-type(2)"));

        _test.clickButton("Edit Selected", 0);
        _test._extHelper.waitForExtDialog("Define " + participantString + " Group");
        _test.waitForElement(Locators.pageSignal(DataRegionTable.UPDATE_SIGNAL));
        if (demographicsPresent)
            DataRegion(_test.getDriver()).withName("demoDataRegion").waitFor();

        if (newPtids != null && newPtids.length > 0)
        {
            String csp = String.join(",", Arrays.asList(newPtids));

            if (replaceExistingPtids)
            {
                _test.setFormElement(Locator.xpath("//textarea[@name='participantIdentifiers']"), csp);
            }
            else
            {
                String currentIds = _test.getFormElement(Locator.xpath("//textarea[@name='participantIdentifiers']"));
                if (currentIds != null && currentIds.length() > 0)
                    _test.setFormElement(Locator.xpath("//textarea[@name='participantIdentifiers']"), currentIds + "," + csp);
            }
        }
        if( categoryName != null )
        {
            Locator categoryField = Locator.name("participantCategory");
            if (!"".equals(oldCategory))
                _test.waitForFormElementToEqual(categoryField, oldCategory);

            if (isCategoryNameNew)
                _test.setFormElement(categoryField, categoryName);
            else
                _test._ext4Helper.selectComboBoxItem(participantString + " Category:", categoryName);
            _test.pressTab(categoryField);
            _test.waitForElementToDisappear(Locator.css(".x-form-focus"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            assertEquals("Mouse category not set", categoryName, _test.getFormElement(categoryField));
        }
        if ( shared != null )
        {
            _test.waitForElement(Locator.css(".share-group-rendered"));
            if( shared )
            {
                _test._ext4Helper.checkCheckbox("Share Category?");
            }
            else
            {
                _test._ext4Helper.uncheckCheckbox("Share Category?");
            }
        }
        _test._ext4Helper.clickWindowButton("Define " + participantString + " Group", "Save", 0, 0);
        _test._ext4Helper.waitForMaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod
    public String getParticipantIds(String groupName, String participantString)
    {
        selectParticipantCategoriesGridRow(groupName);
        _test.clickButton("Edit Selected", 0);
        _test._extHelper.waitForExtDialog("Define " + participantString + " Group");
        DataRegion(_test.getDriver()).withName("demoDataRegion").waitFor();

        String currentIds = _test.getFormElement(Locator.xpath("//textarea[@name='participantIdentifiers']"));
        _test.click(Ext4Helper.Locators.ext4Button("Cancel"));
        _test._ext4Helper.waitForMaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        return currentIds;
    }

    @LogMethod
    public void deleteCustomParticipantGroup(String groupName, String participantString)
    {
        if( !_test.isElementPresent(Locator.xpath("id('labkey-nav-trail-current-page')[text() = 'Manage "+participantString+" Groups']")) )
        {
            _test.clickTab("Manage");
            _test.clickAndWait(Locator.linkWithText("Manage " + participantString + " Groups"));
            _test.waitForText("groups allow");
        }
        _test.log("Delete " + participantString + " Group: " + groupName);
        selectParticipantCategoriesGridRow(groupName);
        _test.clickButton("Delete Selected", 0);
        _test._extHelper.waitForExtDialog("Delete Group");
        _test._ext4Helper.clickWindowButton("Delete Group", "Yes", 0, 0);
    }

    public void selectParticipantCategoriesGridRow(String groupName)
    {
        Locator.XPathLocator participantCategoriesGrid = Locator.id("participantCategoriesGrid");
        Locator.XPathLocator categoryGridRow = participantCategoriesGrid
                .append(Locator.tagWithClass("tr", "x4-grid-data-row")
                        .withPredicate(Locator.tagWithClass("td", "x4-grid-cell-first").withText(groupName)));

        _test.waitAndClick(categoryGridRow);
        _test.waitForElement(categoryGridRow.withClass("x4-grid-row-selected"));
    }

    public void exportStudy(String folder)
    {
        exportStudy(folder, false);
    }

    @LogMethod
    public void exportStudy(String folder, boolean zipFile)
    {
        _test.clickFolder(folder);
        _test.clickTab("Manage");
        _test.clickButton("Export Study");

        _test.waitForElement(Locator.tagWithClass("table", "export-location"));
        List<String> studyObjects = Arrays.asList("Visit Map", "Cohort Settings", "QC State Settings", "CRF Datasets", "Assay Datasets", "Specimens", "Participant Comment Settings", "Participant Groups", "Protocol Documents");
        // NOTE: these have moved to the folder archive export: "Queries", "Custom Views", "Reports", "Lists"
        List<String> missingObjects = new ArrayList<>();
        for (String obj : studyObjects)
        {
            if (!_test.isElementPresent(Locator.tagWithText("label", obj).precedingSibling("input")))
                missingObjects.add(obj);
        }
        assertTrue("Missing study objects: " + String.join(", ", missingObjects), missingObjects.isEmpty());

        _test.checkRadioButton(Locator.tagWithClass("table", "export-location").index(zipFile ? 1 : 0)); // zip file vs. individual files
        _test.clickButton("Export");
    }

    @LogMethod
    public void publishStudy(String studyName, int expectedPipelineJobs, String subjectNounSingular, String subjectNounPlural, String visitNounPlural, List<String> hiddenDatasetNames)
    {
        // This method does not include all options for selecting specific datasets, views, etc. but is
        // meant to be a more general "publish study with all options selected"

        //publish the study
        _test.goToManageStudy();
        _test.clickButton("Publish Study", 0);
        _test._extHelper.waitForExtDialog("Publish Study");

        // Wizard page 1 : General Setup
        _test.waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'General Setup']"));
        _test.setFormElement(Locator.name("studyName"), studyName);
        _test.clickButton("Next", 0);

        // Wizard page 2 : Participants
        _test.waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = '" + subjectNounPlural + "']"));
        _test.checkCheckbox(Locator.radioButtonByNameAndValue("renderType", "all"));
        _test.clickButton("Next", 0);

        // Wizard page 3 : Datasets
        _test.waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Datasets']"));
        _test.click(Locator.css(".studyWizardDatasetList .x-grid3-hd-checker  div"));
        if (hiddenDatasetNames != null)
        {
            _test.assertTextPresent("Hidden Datasets");
            _test.assertTextPresent(hiddenDatasetNames);
            _test.click(Locator.css(".studyWizardHiddenDatasetList .x-grid3-hd-checker  div"));
        }
        _test.click(Locator.xpath("//input[@name='refreshType' and @value='Manual']"));
        _test.clickButton("Next", 0);

        // Wizard page 4 : Visits
        _test.waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = '" + visitNounPlural + "']"));
        _test.click(Locator.css(".studyWizardVisitList .x-grid3-hd-checker  div"));
        _test.clickButton("Next", 0);

        // Wizard page 5 : Specimens
        _test.waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Specimens']"));
        _test.clickButton("Next", 0);

        // Wizard Page 6 : Study Objects
        _test.waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Study Objects']"));
        _test.click(Locator.css(".studyObjects .x-grid3-hd-checker  div"));
        _test.clickButton("Next", 0);

        // Wizard page 7 : Lists
        _test.waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Lists']"));
        _test.click(Locator.css(".studyWizardListList .x-grid3-hd-checker  div"));
        _test.clickButton("Next", 0);

        // Wizard page 8 : Grid Views
        _test.waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Grid Views']"));
        _test.click(Locator.css(".studyWizardViewList .x-grid3-hd-checker  div"));
        _test.clickButton("Next", 0);

        // Wizard Page 9 : Reports and Charts
        _test.waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Reports and Charts']"));
        _test.click(Locator.css(".studyWizardReportList .x-grid3-hd-checker  div"));
        _test.clickButton("Next", 0);

        // Wizard page 10 : Folder Objects
        _test.waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Folder Objects']"));
        _test.click(Locator.css(".folderObjects .x-grid3-hd-checker  div"));
        _test.clickButton("Next", 0);

        // Wizard page 11 : Publish Options
        _test.waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Publish Options']"));
        _test.waitForElement(Locator.css(".studyWizardPublishOptionsList"));
        _test.waitForElement(Locator.css(".studyWizardPublishOptionsList .x-grid3-col-1")); // Make sure grid is filled in
        _test._extHelper.selectExtGridItem("name", "Use Alternate " + subjectNounSingular + " IDs", -1, "studyWizardPublishOptionsList", true);
        _test._extHelper.selectExtGridItem("name", "Shift " + subjectNounSingular + " Dates", -1, "studyWizardPublishOptionsList", true);
        _test._extHelper.selectExtGridItem("name", "Mask Clinic Names", -1, "studyWizardPublishOptionsList", true);
        _test.clickButton("Finish");
        _test.waitForPipelineJobsToComplete(expectedPipelineJobs, "Publish Study", false);
    }

    public DatasetDesignerPage defineDataset(@LoggedParam String name, String containerPath)
    {
        return defineDataset(name, containerPath, null);
    }

    @LogMethod
    public DatasetDesignerPage defineDataset(@LoggedParam String name, String containerPath, @Nullable String id)
    {
        _test.beginAt(WebTestHelper.buildURL("study", containerPath, "defineDatasetType"));

        _test.setFormElement(Locator.name("typeName"), name);

        if (id != null)
        {
            _test.uncheckCheckbox(Locator.name("autoDatasetId"));
            _test.setFormElement(Locator.id("datasetId"), id);
        }
        else
        {
            _test.checkCheckbox(Locator.name("autoDatasetId"));
        }

        _test.clickButton("Next");

        return new DatasetDesignerPage(_test.getDriver());
    }

    @LogMethod
    public void importDataset(@LoggedParam String name, String containerPath, String id, File datasetFile)
    {
        _test.beginAt(WebTestHelper.buildURL("study", containerPath, "defineDatasetType"));

        _test.setFormElement(Locator.name("typeName"), name);
        _test.uncheckCheckbox(Locator.name("autoDatasetId"));
        _test.setFormElement(Locator.id("datasetId"), id);
        _test.checkCheckbox(Locator.name("fileImport"));
        _test.clickButton("Next");

        _test.waitForElement(Locator.name("uploadFormElement"));
        _test.setFormElement(Locator.name("uploadFormElement"), datasetFile);
        _test.clickButton("Import");
    }

    public void setupRequestStatuses()
    {
        _test.clickTab("Manage");
        _test.clickAndWait(Locator.linkWithText("Manage Request Statuses"));
        _test.setFormElement(Locator.name("newLabel"), "New Request");
        _test.clickButton("Save");
        _test.setFormElement(Locator.name("newLabel"), "Processing");
        _test.clickButton("Save");
        _test.setFormElement(Locator.name("newLabel"), "Completed");
        _test.checkCheckbox(Locator.checkboxByName("newFinalState"));
        _test.clickButton("Save");
        _test.setFormElement(Locator.name("newLabel"), "Rejected");
        _test.checkCheckbox(Locator.checkboxByName("newFinalState"));
        _test.uncheckCheckbox(Locator.checkboxByName("newSpecimensLocked"));
        _test.clickButton("Done");
    }

    public ManageDatasetsPage goToManageDatasets()
    {
        _test.goToManageStudy();
        _test.waitAndClickAndWait(Locator.linkWithText("Manage Datasets"));
        return new ManageDatasetsPage(_test.getDriver());
    }

    public ManageVisitPage goToManageVisits()
    {
        _test.goToManageStudy();
        _test.waitAndClickAndWait(Locator.linkWithText("Manage Visits"));
        return new ManageVisitPage(_test.getDriver());
    }

    public PropertiesEditor goToEditSpecimenProperties()
    {
        return goToEditSpecimenProperties(SpecimenPropertyEditors.SPECIMEN_EVENT);
    }

    public PropertiesEditor goToEditSpecimenProperties(SpecimenPropertyEditors editor)
    {
        String editorTitle;
        switch(editor)
        {
            case VIAL:
                editorTitle = "Vial";
                break;
            case SPECIMEN:
                editorTitle = "Specimen";
                break;
            case SPECIMEN_EVENT:
            default:
                editorTitle = "SpecimenEvent";
        }

        _test.goToManageStudy();
        _test.waitAndClickAndWait(Locator.linkWithText("Edit specimen properties"));
        return PropertiesEditor.PropertiesEditor(_test.getDriver()).withTitleContaining(editorTitle).waitFor();
    }

    public static String getStudySampleDataPath()
    {
        return "/sampledata/study/";
    }

    public static String getPipelinePath()
    {
        return TestFileUtils.getLabKeyRoot() + getStudySampleDataPath();
    }

    public enum TimepointType
    {
        DATE,
        VISIT
    }

    public enum RepositoryType
    {
        SIMPLE,
        ADVANCED
    }

    public enum SecurityMode
    {
        BASIC_READ,
        BASIC_WRITE,
        ADVANCED_READ,
        ADVANCED_WRITE
    }

    public enum SpecimenPropertyEditors
    {
        SPECIMEN_EVENT,
        VIAL,
        SPECIMEN
    }
}
