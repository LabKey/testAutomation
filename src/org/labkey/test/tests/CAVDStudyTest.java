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

import java.io.File;
import java.util.ArrayList;

public class CAVDStudyTest extends StudyBaseTest
{
    private static final String PROJECT_NAME = "CAVDStudyTest Project";
    private static final String FOLDER_NAME = "CAVDStudyTest Folder";
    private static final String STUDY_NAME = FOLDER_NAME + " Study";
    private ArrayList<String> _expectedVaccineDesignText = new ArrayList<String>();
    private ArrayList<String> _expectedImmunizationText = new ArrayList<String>();
    private ArrayList<String> _expectedAssayDesignText = new ArrayList<String>();

    @Override
    protected void doCreateSteps()
    {
        createProject(PROJECT_NAME, "None");
        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME, "CAVD Study", null);
    }

    @Override
    protected void  doVerifySteps()
    {
        doVerifyEmptyStudy();
        doVerifyStudyDesign();
        doVerifyAssaySchedule();
//        doVerifyDatasets();
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    private void doVerifyEmptyStudy()
    {
        log("Verifying that the study is empty.");
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

        addDataset();

        clickLinkWithText("Overview");

        assertTabPresent("Data");

        clickLinkWithText("Edit");

        waitForText("Timepoint Type");
        assertEquals(2, getXpathCount(Locator.xpath("//input[@type='radio'][@name='TimepointType'][@disabled]")));
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
        waitForText("Immunization Schedule");
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

        assertTrue("Create Study Timepoints button not disabled when no timepoints exist.", null == getButtonLocator("Create Study Timepoints"));
        
        addTimepoint("CAVDTestTimepoint", "13", TimeUnit.Days);
        waitForText("CAVDTestTimepoint: 13 days", WAIT_FOR_JAVASCRIPT);

        addStudyDesignRow(RowType.Assay, "CAVDTestAssay", "CAVDLab2");

        finishRevision();
    }

    private void doVerifyDatasets()
    {
        clickLinkWithText(STUDY_NAME);
        clickLinkWithText("Assays");

        clickUntilAlert("Create Assay Datasets", "Placeholder datasets created. Use Manage/Study Schedule to define datasets or link to assay data.");

        clickUntilAlert("Create Study Timepoints", "2 timepoints created.");

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

    private void clickUntilAlert(String buttonText, String alertText)
    {
        long startTime = System.currentTimeMillis();
        while(!isAlertPresent() && (System.currentTimeMillis() - startTime) < WAIT_FOR_JAVASCRIPT)
        {
            clickButton(buttonText, 0);
            sleep(500);
        }
        assertAlert(alertText);
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
