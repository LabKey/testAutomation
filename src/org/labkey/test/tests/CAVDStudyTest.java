/*
 * Copyright (c) 2012 LabKey Corporation
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

package org.labkey.test.tests;

import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class CAVDStudyTest extends StudyBaseTest
{
    private static final String PROJECT_NAME = "CAVDStudyTest Project";
    private static final String FOLDER_NAME = "CAVDStudyTest Folder";
    private static final String STUDY_NAME = FOLDER_NAME + " Study";
    private static final String FOLDER_NAME2 = "CAVDStudy2";
    private static final String FOLDER_NAME3 = "CAVDStudy3";
    private static final String FOLDER_NAME4 = "VerifyStudyList";
    private static final String CAVD_TEST_STUDY_ZIP = "/sampledata/study/CAVDTestStudy.folder.zip";
    private static Map<Integer, String> DATASETS = new TreeMap<Integer, String>();
    private ArrayList<String> _expectedVaccineDesignText = new ArrayList<String>();
    private ArrayList<String> _expectedImmunizationText = new ArrayList<String>();
    private ArrayList<String> _expectedAssayDesignText = new ArrayList<String>();

    @Override
    protected void doCreateSteps()
    {
        createProject(PROJECT_NAME, "None");
        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME, "CAVD Study", null);

        // used for doVerifyCrossContainerDatasetStatus
        configureStudy(FOLDER_NAME2);
        configureStudy(FOLDER_NAME3);
        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME4, "Collaboration", null);
    }

    @Override
    protected void  doVerifySteps()
    {
        doVerifyEmptyStudy();
        doVerifyStudyDesign();
        doVerifyAssaySchedule();
        doVerifyDatasets();
        doVerifyCrossContainerDatasetStatus();
    }

    public void configureStudy(String folderName)
    {
        createSubfolder(PROJECT_NAME, PROJECT_NAME, folderName, "Collaboration", null);
        importFolderFromZip(new File(getLabKeyRoot() + CAVD_TEST_STUDY_ZIP).getPath());
        waitForPipelineJobsToComplete(1, "Folder import", false);
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    private void doVerifyEmptyStudy()
    {
        log("Verifying that the study is empty.");
        clickLinkWithText(FOLDER_NAME);

        // Make sure a study was made.
        assertTextNotPresent("No study is active in the current container.");
        assertTextPresent(STUDY_NAME + " tracks data in");

        // Make sure Data tab is hidden
        assertTabNotPresent("Data");

        // Change timepoint type.
        clickLinkWithText("Edit");
        waitForText("Timepoint Type");
        checkRadioButton("TimepointType", "VISIT");
        clickButton("Submit");
        waitForPageToLoad();

        //Check to see if date is checked.
        clickLinkWithText("Edit");
        waitForText("Timepoint Type");
        assertRadioButtonSelected("TimepointType", "VISIT");

    }

    private void doVerifyStudyDesign()
    {
        clickLinkWithText(STUDY_NAME);

        clickLinkWithText("Vaccine Design");
        clickEditDesign();
        // clear defaults
        deleteStudyDesignRow(RowType.Immunogen, 1);
        deleteStudyDesignRow(RowType.Immunogen, 1);
        deleteStudyDesignRow(RowType.Adjuvant, 1);
        deleteStudyDesignRow(RowType.Adjuvant, 1);

        addStudyDesignRow(RowType.Immunogen, "Immunogen1", "Canarypox", "1.5e10 Ad vg", "Intramuscular (IM)");
        addStudyDesignRow(RowType.Immunogen, "gp120", "Subunit Protein", "1.6e8 Ad vg", "Intramuscular (IM)");

        saveRevision();
        assertElementPresent(Locator.xpath("//a[@class='labkey-disabled-button']/span[text()='Save']"));

        addStudyDesignRow(RowType.Adjuvant, "Adjuvant1", "1cc");
        saveRevision();
        assertElementPresent(Locator.xpath("//a[@class='labkey-disabled-button']/span[text()='Save']"));

        clickLinkWithText("Vaccine Design");
        waitForText("Immunogens");
        assertTextPresent(_expectedVaccineDesignText);

        clickEditDesign();
        addStudyDesignRow(RowType.Immunogen, "Immunogen3", "Fowlpox", "1.9e8 Ad vg", "Intramuscular (IM)");
        addStudyDesignRow(RowType.Adjuvant, "Adjuvant2");
        addAntigen(1, "Gag", "Clade B");
        addAntigen(2, "Env");
        addAntigen(3, "Tat", "Clade C");
        addAntigen(3, "Nef", "Clade D");

        finishRevision();
        waitForText("Immunogens");
        assertTextPresent(_expectedVaccineDesignText);

        clickLinkWithText("Immunizations");
        clickEditDesign();
        // clear defaults
        deleteStudyDesignRow(RowType.Immunization, 1);
        deleteStudyDesignRow(RowType.Immunization, 1);
        addStudyDesignRow(RowType.Immunization, "Vaccine", "1");
        addStudyDesignRow(RowType.Immunization, "Placebo", "2");
        saveRevision();
        addStudyDesignRow(RowType.Immunization, "Vaccine2", "3");
        finishRevision();
        waitForText("Immunization Schedule", 3, defaultWaitForPage);
        assertTextPresent(_expectedImmunizationText);
        assertElementNotPresent(Locator.tagWithText("div", "30")); // From deleted rows
    }

    private void doVerifyAssaySchedule()
    {
        clickLinkWithText(STUDY_NAME);
        clickLinkWithText("Assays");
        clickEditDesign();
        deleteStudyDesignRow(RowType.Assay, 1);
        deleteStudyDesignRow(RowType.Assay, 1);
        deleteStudyDesignRow(RowType.Assay, 1);
        deleteStudyDesignRow(RowType.Assay, 1);
        addStudyDesignRow(RowType.Assay, "ELISPOT", "Schmitz");
        addStudyDesignRow(RowType.Assay, "Neutralizing Antibodies Panel 1", "Seaman");
        addStudyDesignRow(RowType.Assay, "ICS", "McElrath");
        saveRevision();

        log("Add sample type");
        clickButton("Edit Sample Types", 0);
        waitForElement(Locator.xpath("//div[@class='Caption'][text()='Define Sample Type']"));
        clickButton("Add", 0);
        setFormElement("sampleTypeName", "Platelets");
        setFormElement("primaryType", "Blood");
        setFormElement("sampleCode", "LK");
        clickButton("Done", 0);
        waitForElementToDisappear(Locator.xpath("//div[@class='Caption'][text()='Define Sample Type']"), WAIT_FOR_JAVASCRIPT);

        log("Add assay type");
        clickButton("Edit Assay List", 0);
        waitForElement(Locator.xpath("//div[@class='Caption'][text()='Define Assay']"));
        clickButton("Add", 0);
        setFormElement("assayName", "CAVDTestAssay");
        setFormElement("assayDescription", "This Assay created by CAVD Study Test");
        setFormElement("assayLabs", "CAVDLab1\nCAVDLab2");
        setFormElement("materialAmount", "13");
        selectOptionByText("materialUnits", "ul");
        selectOptionByText("materialType", "Platelets");
        clickButton("Done", 0);
        waitForElementToDisappear(Locator.xpath("//div[@class='Caption'][text()='Define Assay']"), WAIT_FOR_JAVASCRIPT);

        clickButton("Create Study Timepoints", 0);
        assertAlert("No timepoints are defined in the assay schedule.");
        
        addTimepoint("CAVDTestTimepoint", "13", TimeUnit.Days);
        waitForText("CAVDTestTimepoint: 13 days", WAIT_FOR_JAVASCRIPT);

        addStudyDesignRow(RowType.Assay, "CAVDTestAssay", "CAVDLab2");

        finishRevision();
    }

    private void doVerifyDatasets()
    {
        clickLinkWithText(STUDY_NAME);
        clickLinkWithText("Assays", true);

        waitAndClickNavButton("Create Assay Datasets", 0);
        waitForAlert("Placeholder datasets created. Use Manage/Study Schedule to define datasets or link to assay data.", WAIT_FOR_JAVASCRIPT);

        waitAndClickNavButton("Create Study Timepoints", 0);
        waitForAlert("2 timepoints created.", WAIT_FOR_JAVASCRIPT);

        clickLinkWithText("Manage");
        clickLinkWithText("Manage Datasets");

        assertTextPresent("Placeholder", 4);
        assertLinkPresentWithText("ELISPOT");
        assertLinkPresentWithText("Neutralizing Antibodies Panel 1");
        assertLinkPresentWithText("ICS");
        assertLinkPresentWithText("CAVDTestAssay");

        clickLinkWithText("Manage");
        clickLinkWithText("Manage Timepoints");

        assertTextPresent("Study Start: 0 days");
        assertTextPresent("CAVDTestTimepoint: 13 days");
//
//        addDataset();
//
//        clickLinkWithText("Overview");
//
//        assertTabPresent("Data");
//
//        clickLinkWithText("Edit");
//
//        waitForText("Timepoint Type");
//        assertEquals(2, getXpathCount(Locator.xpath("//input[@type='radio'][@name='TimepointType'][@disabled]")));
    }

    private void doVerifyCrossContainerDatasetStatus()
    {
        // setup the dataset map (ID > Name)
        DATASETS.put(5001, "NAbTest");
        DATASETS.put(5002, "FlowTest");
        DATASETS.put(5003, "LuminexTest");
        DATASETS.put(5004, "ELISATest");

        String[][] statuses = {
            {"Draft", "/labkey/reports/icon_draft.png", "D"},
            {"Final", "/labkey/reports/icon_final.png", "F"},
            {"Locked", "/labkey/reports/icon_locked.png", "L"},
            {"Unlocked", "/labkey/reports/icon_unlocked.png", "U"}
        };

        String study2name = FOLDER_NAME2 + " Study";
        String study3name = FOLDER_NAME3 + " Study";

        log("Set study name for " + FOLDER_NAME2 + " and verify datasets exist.");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME2);
        // workaround for issue 15023: go to manage views page to initialize study dataset properties
        goToManageViews();
        waitForText("Manage Views");
        clickLinkWithText("Manage");
        clickLinkWithText("Change Study Properties");
        waitForElement(Locator.name("Label"), WAIT_FOR_JAVASCRIPT);
        setFormElement("Label", study2name);
        clickNavButton("Submit");
        waitForText("General Study Settings");
        assertTextPresent(study2name);
        clickLinkWithText("Manage Datasets");
        for (Map.Entry<Integer, String> dataset : DATASETS.entrySet())
        {
            assertTextPresentInThisOrder(dataset.getKey().toString(), dataset.getValue());
        }

        log("Set study name for " + FOLDER_NAME3 + " and verify datasets exist.");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME3);
        // workaround for issue 15023: go to manage views page to initialize study dataset properties
        goToManageViews();
        waitForText("Manage Views");
        clickLinkWithText("Manage");
        clickLinkWithText("Change Study Properties");
        waitForElement(Locator.name("Label"), WAIT_FOR_JAVASCRIPT);
        setFormElement("Label", study3name);
        clickNavButton("Submit");
        waitForText("General Study Settings");
        assertTextPresent(study3name);
        clickLinkWithText("Manage Datasets");
        for (Map.Entry<Integer, String> dataset : DATASETS.entrySet())
        {
            assertTextPresentInThisOrder(dataset.getKey().toString(), dataset.getValue());
        }

        log("Verify study list query from sibling folder contains studies and dataset status.");
        goToViscStudiesQuery(FOLDER_NAME4);
        assertLinkPresentWithText(study2name);
        assertLinkPresentWithText(study3name);
        for (String datasetName : DATASETS.values())
        {
            assertTextPresent(datasetName, 2);
        }
        // verify that there are no status icons to start
        for (String[] status : statuses)
        {
            assertElementNotPresent("Status icon not expected in studies query at this time.", Locator.tagWithAttribute("img", "src", status[1]));
        }

        log("Change study dataset status for " + FOLDER_NAME2 + " and verify changes in study list query.");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME2);
        waitForText("Study Schedule");
        // wait for the study schedule grid to load, any dataset name will do
        waitForText(DATASETS.values().iterator().next());
        int statusCounter = 0;
        for (String dataset : DATASETS.values())
        {
            setDatasetStatus(dataset, statuses[statusCounter][0]);
            statusCounter++;
        }
        
        log("Verify each status icon appears once in the studies list.");
        goToViscStudiesQuery(FOLDER_NAME4);
        for (String[] status : statuses)
        {
            assertElementPresent(Locator.tagWithAttribute("img", "src", status[1]), 1);
        }

        log("Create list in " + FOLDER_NAME4 + " with lookup to the studies list query.");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME4);
        addWebPart("Lists");
        ListHelper.ListColumn[] columns = new ListHelper.ListColumn[] {
                new ListHelper.ListColumn("MyStudyName", "MyStudyName", ListHelper.ListColumnType.String, ""),
                new ListHelper.ListColumn("StudyLookup", "StudyLookup", ListHelper.ListColumnType.String, "", new ListHelper.LookupInfo(null, "viscstudies", "studies"))
        };
        ListHelper.createList(this, FOLDER_NAME4, "AllStudiesList", ListHelper.ListColumnType.AutoInteger, "Key", columns);
        clickNavButton("Done");

        log("Add records to list for each study.");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME4);
        clickLinkWithText("AllStudiesList", true);
        clickNavButton("Insert New");
        setFormElement("quf_MyStudyName", "Something");
        selectOptionByText("quf_StudyLookup", study2name);
        clickNavButton("Submit");
        clickNavButton("Insert New");
        setFormElement("quf_MyStudyName", "TheOtherOne");
        selectOptionByText("quf_StudyLookup", study3name);
        clickNavButton("Submit");

        log("Verify that the list lookup displays dataset status values.");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME4);
        clickLinkWithText("AllStudiesList", true);
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "StudyLookup");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "StudyLookup/Dataset Status");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "StudyLookup/Label");
        CustomizeViewsHelper.applyCustomView(this);
        // verify each status icon appears once originally
        for (String[] status : statuses)
        {
            assertElementPresent(Locator.tagWithAttribute("img", "src", status[1]), 1);
        }
        log("Verify that you can navigate to study and set status from study list.");
        clickLinkWithText(study3name, true);
        // wait for the study schedule grid to load, any dataset name will do
        waitForText(DATASETS.values().iterator().next());
        // set the status to Locked for all of the datasets
        for (String dataset : DATASETS.values())
        {
            setDatasetStatus(dataset, "Locked");
        }
        assertElementPresent(Locator.tagWithAttribute("img", "src", "/labkey/reports/icon_locked.png"), DATASETS.size());
        clickButton("Save Changes", defaultWaitForPage);
        // verify that we are back on the list view
        assertTextPresent("AllStudiesList");
        assertTextPresent("Dataset Status");
        // locked icon should now appear once for study2 and for all datasets in study3
        assertElementPresent(Locator.tagWithAttribute("img", "src", "/labkey/reports/icon_locked.png"), DATASETS.size() + 1);

        log("Verify data status exports to text as expected.");
        pushLocation();
        addUrlParameter("exportAsWebPage=true");
        waitForElement(Locator.navButton("Export"), WAIT_FOR_JAVASCRIPT);
        clickExportToText();
        // verify column names
        assertTextPresentInThisOrder("myStudyName", "studyLookupLabel", "studyLookupDatasetStatus");
        // verify first study values
        assertTextPresentInThisOrder("Something", study2name);
        statusCounter = 0;
        for (String dataset : DATASETS.values())
        {
            assertTextPresent(statuses[statusCounter][2] + ": " + dataset);
            statusCounter++;
        }
        // verify second study values
        assertTextPresentInThisOrder("TheOtherOne", study3name);
        for (String dataset : DATASETS.values())
        {
            assertTextPresent("L: " + dataset);
        }
        popLocation();
    }

    private void setDatasetStatus(String dataset, String status)
    {
        clickEditDatasetIcon(dataset);
        Locator.XPathLocator comboParent = Locator.xpath("//label[contains(text(), 'Status')]/..");
        Ext4Helper.selectComboBoxItem(this, comboParent, status);
        clickNavButton("Save", 0);

        // verify that the status icon appears
        Locator statusLink = Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//div[contains(text(), '" + dataset + "')]/../../..//img[@alt='" + status + "']");
        waitForElement(statusLink, WAIT_FOR_JAVASCRIPT);
    }

    private void goToViscStudiesQuery(String folderName)
    {
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(folderName);
        goToSchemaBrowser();
        selectQuery("viscstudies", "studies");
        waitForText("view data");
        clickLinkContainingText("view data", true);
    }

    private void clickEditDatasetIcon(String dataset)
    {
        Locator editLink = Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//div[contains(text(), '" + dataset + "')]/../../..//span[contains(@class, 'edit-views-link')]");
        waitForElement(editLink, WAIT_FOR_JAVASCRIPT);
        click(editLink);

        ExtHelper.waitForExtDialog(this, dataset);
    }

    private void deleteStudyDesignRow(RowType type, int row)
    {
        int count = getXpathCount(Locator.xpath("//table[@id='"+type+"Grid']//div[starts-with(@title, 'Click to delete ')]"));
        click(Locator.xpath("//table[@id='"+type+"Grid']//div[starts-with(@title, 'Click to delete ')][text()='"+row+"']"));
        waitAndClick(Locator.xpath("//td[@role='menuitem'][starts-with(text(), 'Delete ')]"));
        waitForElementToDisappear(Locator.xpath("(//table[@id='"+type+"Grid']//div[starts-with(@title, 'Click to delete ')])["+count+"]"), WAIT_FOR_JAVASCRIPT);
        mouseDown(Locator.xpath("/html/body"));
    }

    private void addStudyDesignRow(RowType type, String... values)
    {
        waitForElement(Locator.navButton("Finished"));
        int rowCount = getXpathCount(Locator.xpath("//table[@id='"+type+"Grid']//div[starts-with(@title, 'Click to delete ')]"));
        int rowOffset = getXpathCount(Locator.xpath("//table[@id='"+type+"Grid']/tbody/tr[not(.//input or .//select)]")) + 1;
        String tablePath = "//table[@id='"+type+"Grid']";
        assertElementPresent(Locator.xpath(tablePath));
        String rowPath = tablePath + "/tbody/tr["+(rowCount+rowOffset)+"]";
        assertElementPresent(Locator.xpath(rowPath));
        for(int i = 0; i < values.length; i++)
        {
            String cellPath = rowPath + "/td["+(i+2)+"]";
            if(isElementPresent(Locator.xpath(cellPath+"/select")))
                selectOptionByText(Locator.xpath(cellPath+"/select"), values[i]);
            else if(isElementPresent(Locator.xpath(cellPath+"/input")))
                setFormElement(Locator.xpath(cellPath+"/input"), values[i]);
            else
                fail("Non input/select cell found when adding new " + type);
            switch(type)
            {
                case Adjuvant:
                    _expectedVaccineDesignText.add(values[i]);
                    break;
                case Immunogen:
                    _expectedVaccineDesignText.add(values[i]);
                    break;
                case Immunization:
                    _expectedImmunizationText.add(values[i]);
                    break;
                case Assay:
                    _expectedAssayDesignText.add(values[i]);
                    break;
            }
        }

        waitForElement(Locator.xpath("(//div[starts-with(@title, 'Click to delete ')])["+(rowCount+1)+"]"), WAIT_FOR_JAVASCRIPT);
    }

    private void addAntigen(int immunogenNumber, String... values)
    {
        String tablePath = "(//table[starts-with(@id, 'AntigenGrid')])["+immunogenNumber+"]";
        assertElementPresent(Locator.xpath(tablePath));
        int rowCount = getXpathCount(Locator.xpath(tablePath+"//div[@title='Click to delete antigen']"));
        String rowPath = tablePath + "/tbody/tr["+(rowCount+2)+"]";
        assertElementPresent(Locator.xpath(rowPath));
        for(int i = 0; i < values.length; i++)
        {
            String cellPath = rowPath + "/td["+(i+2)+"]";
            if(isElementPresent(Locator.xpath(cellPath+"/select")))
                selectOptionByText(Locator.xpath(cellPath+"/select"), values[i]);
            else if(isElementPresent(Locator.xpath(cellPath+"/input")))
                setFormElement(Locator.xpath(cellPath+"/input"), values[i]);
            else
                fail("Non input/select cell found when adding new antigen");
            _expectedVaccineDesignText.add(values[i]);
        }

        waitForElement(Locator.xpath("(//div[@title='Click to delete antigen'])["+(rowCount+1)+"]"), WAIT_FOR_JAVASCRIPT);

    }

    enum RowType
    {
        Immunogen,
        Adjuvant,
        Immunization,
        Antigen,
        Assay
    }

    enum TimeUnit
    {
        Days,
        Weeks
    }

    private void clickEditDesign()
    {
        waitAndClickNavButton("Edit");
        waitForElement(Locator.navButton("Finished"));
    }

    private void addTimepoint(String name, String count, TimeUnit unit)
    {
        click(Locator.xpath("//div[text() = 'Add Timepoint']"));
        waitForElement(Locator.id("DefineTimepointDialog"));
        setFormElement("timepointName", name);
        setFormElement("timepointCount", count);
        setFormElement("timepointUnit", unit.toString());
        clickButton("OK", 0);
        waitForElementToDisappear(Locator.id("DefineTimepointDialog"), WAIT_FOR_JAVASCRIPT);
    }

    private void addDataset()
    {
        clickLinkWithText("Manage");
        clickLinkWithText("Study Schedule");

        log("adding dataset: " + "ImportedDataset");

        clickButton("Add Dataset", 0);
        waitForElement(Locator.xpath("//span[text() = 'New Dataset']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//label[text() = 'Name:']/..//input"), "ImportedDataset");


        click(Locator.ext4Radio("Import data from file"));
        clickNavButton("Next");

        String datasetFileName = getStudySampleDataPath() + "/datasets/plate001.tsv";
        File file = new File(WebTestHelper.getLabKeyRoot(), datasetFileName);

        if (file.exists())
        {
            Locator fileUpload = Locator.xpath("//input[@name = 'uploadFormElement']");
            waitForElement(fileUpload, WAIT_FOR_JAVASCRIPT);
            setFormElement(fileUpload, file.getAbsolutePath());

            waitForElement(Locator.xpath("//div[@class = 'gwt-HTML' and contains(text(), 'Showing first 5 rows')]"), WAIT_FOR_JAVASCRIPT);
            clickNavButton("Import");
        }
        else
            fail("The dataset import .tsv file (plate001.tsv) does not exist");
    }

    private int revision = 1;
    private void saveRevision()
    {
        clickButton("Save", 0);
        waitForText("Revision "+(++revision)+" saved successfully.", WAIT_FOR_JAVASCRIPT);
    }

    private void finishRevision()
    {
        clickButton("Finished");
        revision++;
    }
}
