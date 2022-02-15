/*
 * Copyright (c) 2011-2019 LabKey Corporation
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
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestFileUtils;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.pages.ManageDatasetsPage;
import org.labkey.test.pages.study.CreateStudyPage;
import org.labkey.test.pages.study.DatasetDesignerPage;
import org.labkey.test.pages.study.ManageVisitPage;
import org.labkey.test.pages.study.StudySecurityPage;
import org.openqa.selenium.interactions.Actions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.DataRegionTable.DataRegion;

public class StudyHelper
{
    public static final File SPECIMEN_ARCHIVE_A = getSpecimenArchiveFile("sample_a.specimens");
    public static final File SPECIMEN_ARCHIVE_B = getSpecimenArchiveFile("sample_b.specimens");

    private static Boolean _specimenModulePresent = null;

    protected BaseWebDriverTest _test;

    public StudyHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public boolean doesStudyExist(String containerPath)
    {
        Connection connection = _test.createDefaultConnection();
        SelectRowsCommand command = new SelectRowsCommand("study", "Datasets");
        try
        {
            SelectRowsResponse response = command.execute(connection, containerPath);
            return true;
        }
        catch (CommandException e)
        {
            return false;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public CreateStudyPage startCreateStudy()
    {
        _test.clickAndWait(Locator.lkButton("Create Study"));
        return new CreateStudyPage(_test);
    }

    public StudySecurityPage enterStudySecurity()
    {
        return _test.goToManageStudy()
                .manageSecurity();
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
            new Actions(_test.getDriver()).moveByOffset(-50, -50); // Dismiss tooltip
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
            new Actions(_test.getDriver()).moveByOffset(-50, -50); // Dismiss tooltip
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
        List<String> studyObjects = Arrays.asList("Visit Map", "Cohort Settings", "QC State Settings", "Datasets: Study Dataset Definitions", "Datasets: Study Dataset Data", "Datasets: Assay Dataset Definitions", "Datasets: Assay Dataset Data", "Participant Comment Settings", "Participant Groups", "Protocol Documents");
        if (isSpecimenModuleActive())
        {
            studyObjects = new ArrayList<>(studyObjects);
            studyObjects.add("Specimens");
        }
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

        // General Setup
        _test.waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'General Setup']"));
        _test.setFormElement(Locator.name("studyName"), studyName);
        _test.clickButton("Next", 0);

        // Participants
        _test.waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = '" + subjectNounPlural + "']"));
        _test.checkCheckbox(Locator.radioButtonByNameAndValue("renderType", "all"));
        _test.clickButton("Next", 0);

        // Datasets
        _test.waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Datasets']"));
        _test.waitForElement(Locator.css(".studyWizardDatasetList"));
        _test.click(Locator.css(".studyWizardDatasetList .x-grid3-hd-checker  div"));
        if (hiddenDatasetNames != null)
        {
            _test.assertTextPresent("Hidden Datasets");
            _test.assertTextPresent(hiddenDatasetNames);
            _test.click(Locator.css(".studyWizardHiddenDatasetList .x-grid3-hd-checker  div"));
        }
        _test.click(Locator.xpath("//input[@name='refreshType' and @value='Manual']"));
        _test.clickButton("Next", 0);

        // Visits
        _test.waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = '" + visitNounPlural + "']"));
        _test.waitForElement(Locator.css(".studyWizardVisitList"));
        _test.click(Locator.css(".studyWizardVisitList .x-grid3-hd-checker  div"));
        _test.clickButton("Next", 0);

        // Specimens, if present & active
        if (isSpecimenModuleActive())
            advanceThroughPublishStudyWizard(Panel.studySpecimens);

        // Study Objects
        advanceThroughPublishStudyWizard(Panel.studyObjects, true);

        // Lists
        advanceThroughPublishStudyWizard(Panel.studyWizardListList, true);

        // Queries
        advanceThroughPublishStudyWizard(Panel.studyWizardQueryList, true);

        // Grid Views
        advanceThroughPublishStudyWizard(Panel.studyWizardViewList, true);

        // Reports and Charts
        advanceThroughPublishStudyWizard(Panel.studyWizardReportList, true);

        // Folder Objects
        advanceThroughPublishStudyWizard(Panel.folderObjects, true);

        // Publish Options
        _test.waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Publish Options']"));
        _test.waitForElement(Locator.css(".studyWizardPublishOptionsList"));
        _test.waitForElement(Locator.css(".studyWizardPublishOptionsList .x-grid3-col-1")); // Make sure grid is filled in
        _test._extHelper.selectExtGridItem("name", "Use Alternate " + subjectNounSingular + " IDs", -1, "studyWizardPublishOptionsList", true);
        _test._extHelper.selectExtGridItem("name", "Shift " + subjectNounSingular + " Dates", -1, "studyWizardPublishOptionsList", true);
        _test._extHelper.selectExtGridItem("name", "Mask Clinic Names", -1, "studyWizardPublishOptionsList", true);
        _test.clickButton("Finish");
        _test.waitForPipelineJobsToComplete(expectedPipelineJobs, "Publish Study", false);
    }

    public void advanceThroughPublishStudyWizard(StudyHelper.IPanel wizardPanel)
    {
        advanceThroughPublishStudyWizard(wizardPanel, false);
    }

    public void advanceThroughPublishStudyWizard(StudyHelper.IPanel wizardPanel, boolean selectAll)
    {
        advanceThroughPublishStudyWizard(Arrays.asList(wizardPanel), selectAll);
    }

    public void advanceThroughPublishStudyWizard(List<StudyHelper.IPanel> wizardPanels)
    {
        advanceThroughPublishStudyWizard(wizardPanels, false);
    }

    public void advanceThroughPublishStudyWizard(List<StudyHelper.IPanel> wizardPanels, boolean selectAll)
    {
        for (IPanel panel : wizardPanels)
        {
            _test.waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = '" + panel.getPanelTitle() + "']"));
            if (panel.getLabelColumn() != null) // grids only
            {
                _test.waitForElement(Locator.css("." + panel.name()));
                if (selectAll)
                    _test.click(Locator.css("." + panel.name() + " .x-grid3-hd-checker  div"));
            }

            if (_test.isElementPresent(Locator.extButton("Finish")))
                _test.clickButton("Finish");
            else
                _test.clickButton("Next", 0);
        }
    }

    public DatasetDesignerPage defineDataset(@LoggedParam String name, String containerPath)
    {
        return defineDataset(name, containerPath, null);
    }

    @LogMethod
    public DatasetDesignerPage defineDataset(@LoggedParam String name, String containerPath, @Nullable String id)
    {
        DatasetDesignerPage page = DatasetDesignerPage.beginAt(_test, containerPath);
        page.setName(name);

        if (id != null)
        {
            page.openAdvancedDatasetSettings()
                    .setDatasetId(id)
                    .clickApply();
        }
        else
        {
            page.openAdvancedDatasetSettings()
                    .setDatasetId("")
                    .clickApply();
        }

       return page;
    }

    @LogMethod
    public void importDataset(@LoggedParam String name, String containerPath, String id, File datasetFile)
    {
        DatasetDesignerPage page = DatasetDesignerPage.beginAt(_test, containerPath);
        page.setName(name)
            .openAdvancedDatasetSettings()
            .setDatasetId(id)
            .clickApply();
        page.getFieldsPanel()
                .setInferFieldFile(datasetFile);
        // todo: 'import data' is default behavior, but is optional if the slider is toggled.
        page.clickSave();
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

    public DomainDesignerPage goToEditSpecimenProperties()
    {
        return goToEditSpecimenProperties(SpecimenPropertyEditors.SPECIMEN_EVENT);
    }

    public DomainDesignerPage goToEditSpecimenProperties(SpecimenPropertyEditors editor)
    {
        _test.goToManageStudy();

        switch(editor)
        {
            case VIAL:
                _test.waitAndClickAndWait(Locator.linkWithText("Edit Vial fields"));
                break;
            case SPECIMEN:
                _test.waitAndClickAndWait(Locator.linkWithText("Edit Specimen fields"));
                break;
            case SPECIMEN_EVENT:
            default:
                _test.waitAndClickAndWait(Locator.linkWithText("Edit Specimen Event fields"));
        }

        DomainDesignerPage designerPage = new DomainDesignerPage(_test.getDriver());

        return designerPage;
    }

    public static File getStudyTempDir()
    {
        return new File(getStudySubfolderPath(), "drt_temp");
    }

    // Return the root of the Study001 folder archive (i.e., where folder.xml lives)
    public static String getFolderArchiveRootPath()
    {
        return getFolderArchiveFile("folder.xml").getParentFile().getAbsolutePath();
    }

    // Return the root of the /study node within the Study001 folder archive (i.e., where study.xml lives)
    public static String getStudySubfolderPath()
    {
        return getFolderArchiveFile("study/study.xml").getParentFile().getAbsolutePath();
    }

    // Return the specified file from within the Study001 folder archive
    public static File getFolderArchiveFile(String relativePath)
    {
        return TestFileUtils.getSampleData("studies/Study001/" + relativePath);
    }

    // Return a specimen archive file
    public static File getSpecimenArchiveFile(String archiveName)
    {
        return TestFileUtils.getSampleData("study/specimens/" + archiveName);
    }

    // Emulates previous behavior of setting "advanced" repository type on the create study page, which is what many
    // tests want
    public void setupAdvancedRepositoryType()
    {
        setupRepositoryType(true, false, true);
    }

    @LogMethod
    public void setupRepositoryType(boolean advanced, boolean editable, boolean requests)
    {
        _test.log("Setup specimen repository type settings");
        _test.clickTab("Manage");
        _test.clickAndWait(Locator.linkWithText("Change Repository Type"));
        _test.waitForElement(Locator.tagContainingText("h3","Manage Repository Settings"));
        _test.checkRadioButton(Locator.radioButtonByName("simple").index(advanced ? 1 : 0)); // Advanced repository type?

        if (advanced)
        {
            _test.checkRadioButton(Locator.radioButtonByName("specimenDataEditable").index(editable ? 1 : 0)); // Editable specimen data?
            _test.checkRadioButton(Locator.radioButtonByName("enableRequests").index(requests ? 0 : 1)); // Enabled specimen requests?
        }

        _test.clickButton("Submit");
    }

    public boolean isSpecimenModulePresent()
    {
        if (null == _specimenModulePresent)
        {
            AbstractContainerHelper containerHelper = new APIContainerHelper(_test);
            Set<String> allModules = containerHelper.getAllModules();
            _specimenModulePresent = allModules.contains("specimen");
        }

        return _specimenModulePresent;
    }

    public boolean isSpecimenModuleActive()
    {
        if (!isSpecimenModulePresent())
            return false;

        AbstractContainerHelper containerHelper = new APIContainerHelper(_test);
        return containerHelper.getActiveModules().contains("Specimen");
    }

    public enum TimepointType
    {
        DATE,
        VISIT
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

    public interface IPanel
    {
        String name();
        String getPanelTitle();
        String getLabelColumn();
    }

    public enum Panel implements IPanel
    {
        studyGeneralSetup("General Setup", null),
        studyWizardParticipantList("Participants", "Participant Group"),
        studyWizardMouseList("Mice", "Mouse Group"),
        studyWizardDatasetList("Datasets", "Label"),
        studyWizardVisitList("Visits", "Label"),
        studySpecimens("Specimens", null),
        studyObjects("Study Objects", "Name"),
        studyWizardListList("Lists", "Name"),
        studyWizardQueryList("Queries", "Query Name"),
        studyWizardViewList("Grid Views", "Name"),
        studyWizardReportList("Reports and Charts", "Name"),
        folderObjects("Folder Objects", "Name"),
        studyWizardPublishOptionsList("Publish Options", "Name");

        private final String panelTitle;
        private final String labelColumn;

        Panel(String panelTitle, String labelColumn)
        {
            this.panelTitle = panelTitle;
            this.labelColumn = labelColumn;
        }

        public String getPanelTitle()
        {
            return panelTitle;
        }

        public String getLabelColumn()
        {
            return labelColumn;
        }
    }

    public static IPanel participantList(String singular, String plural)
    {
        return new IPanel()
        {
            @Override
            public String name()
            {
                return "studyWizardParticipantList";
            }

            @Override
            public String getPanelTitle()
            {
                return plural;
            }

            @Override
            public String getLabelColumn()
            {
                return singular + " Group";
            }
        };
    }
}
