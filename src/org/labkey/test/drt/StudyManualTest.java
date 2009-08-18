/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

package org.labkey.test.drt;

import com.thoughtworks.selenium.SeleniumException;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ExtHelper;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * User: brittp
 * Date: Mar 9, 2006
 * Time: 1:54:57 PM
 */
public class StudyManualTest extends StudyBaseTest
{
    protected final String VISIT_MAP = getSampleDataPath() + "v068_visit_map.txt";

    private final String CRF_SCHEMAS = getSampleDataPath() + "datasets/schema.tsv";

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void loadSpecimens()
    {
        // upload specimen data and verify import
        SpecimenImporter importer = new SpecimenImporter(new File(getPipelinePath()), new File(getLabKeyRoot(), SPECIMEN_ARCHIVE_A), new File(getLabKeyRoot(), ARCHIVE_TEMP_DIR), getFolderName(), 1);
        importer.importAndWaitForComplete();
    }

    protected void createStudy()
    {
        initializeFolder();

        clickNavButton("Create Study");
        click(Locator.radioButtonByNameAndValue("simpleRepository", "false"));
        clickNavButton("Create Study");

        // change study label
        clickLinkWithText("Change Label");
        setFormElement("label", getStudyLabel());
        clickNavButton("Update");
        assertTextPresent(getStudyLabel());

        // import visit map
        clickLinkWithText("Manage Visits");
        clickLinkWithText("Import Visit Map");
        String visitMapData = getFileContents(VISIT_MAP);
        setLongTextField("content", visitMapData);
        clickNavButton("Import");

        // define forms
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Define Dataset Schemas");
        clickLinkWithText("Bulk Import Schemas");
        setFormElement("typeNameColumn", "platename");
        setFormElement("labelColumn", "platelabel");
        setFormElement("typeIdColumn", "plateno");
        setLongTextField("tsv", getFileContents(CRF_SCHEMAS));
        clickNavButton("Submit", 180000);

        // setup cohorts:
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Cohorts");
        selectOptionByText("participantCohortDataSetId", "EVC-1: Enrollment Vaccination");
        waitForPageToLoad();
        selectOptionByText("participantCohortProperty", "2. Enrollment group");
        clickNavButton("Update Assignments");

        // configure QC state management so that all data is displayed by default (we'll test with hidden data later):
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage QC States");
        selectOptionByText("showPrivateDataByDefault", "All data");
        clickNavButton("Save");

        // upload data:
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Data Pipeline");
        clickNavButton("Setup");
        setFormElement("path", getPipelinePath());
        submit();
        clickLinkWithText("Pipeline");
        clickNavButton("Process and Import Data");
        waitAndClick(Locator.fileTreeByName("datasets"));
        waitForElement(Locator.navButton("Import datasets"), 5000);
        if (isNavButtonPresent("Delete log"))
            clickNavButton("Delete log");
        clickNavButton("Import datasets");
        clickNavButton("Submit");
    }

    protected void waitForInitialUpload()
    {
        // Unfortunately isLinkWithTextPresent also picks up the "Errors" link in the header,
        // and it picks up the upload of 'FPX-1: Final Complete Physical Exam' as containing complete.
        // we exclude the word 'REPLACE' to catch this case:
        startTimer();
        while ((!isLinkPresentWithText("COMPLETE") || isLinkPresentWithText("REPLACE")) &&
                !isLinkPresentWithText("ERROR") &&
                elapsedSeconds() < MAX_WAIT_SECONDS)
        {
            log("Waiting for data import");
            sleep(1000);
            refresh();
        }
        assertLinkNotPresentWithText("ERROR");  // Must be surrounded by an anchor tag.
        assertLinkPresentWithTextCount("COMPLETE", 1);
    }

    // Using old visit map format, which does not support default visibility (so we need to set it manually).
    protected void afterCreateStudy()
    {
        // Hide visits based on label -- manual create vs. import will result in different indexes for these visits
        hideVisits("Screening Cycle", "Cycle 1");

        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("DEM-1: Demographics");
        clickButtonContainingText("Edit Dataset Definition");
        waitForElement(Locator.name("description"), BaseSeleniumWebTest.WAIT_FOR_GWT);        
        checkCheckbox("demographicData");
        setFormElement("description", "This is the demographics dataset, dammit");
        clickNavButton("Save");

        createCustomAssays();
    }

    protected void hideVisits(String... visitLabel)
    {
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Visits");

        Set<String> labels = new HashSet<String>(Arrays.asList(visitLabel));
        int row = 2;  // Skip header row (row index is one-based)
        String currentLabel;

        // Loop until we find all the labels or we hit the end of the table
        while (!labels.isEmpty() && null != (currentLabel = getVisitLabel(row)))
        {
            if (labels.contains(currentLabel))
            {
                clickLinkWithText("edit", row - 2);   // Zero-based, plus the header row doesn't have an edit link
                uncheckCheckbox("showByDefault");
                clickNavButton("Save");
                labels.remove(currentLabel);
            }

            row++;
        }
    }

    // row is one-based
    private String getVisitLabel(int row)
    {
        try
        {
            return selenium.getText("//table[@id='visits']/tbody/tr[" + row + "]/th");
        }
        catch (SeleniumException e)
        {
            return null;
        }
    }

    protected void createCustomAssays()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Create New Dataset");
        setFormElement("typeName", "verifyAssay");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='DatasetDesignerName']"), WAIT_FOR_GWT);

        checkRadioButton("additionalKey", 1);

        clickNavButton("Import Schema", 0);
        waitForElement(Locator.xpath("//textarea[@id='schemaImportBox']"), WAIT_FOR_GWT);

        setFormElement("schemaImportBox", "Property\tLabel\tRangeURI\tNotNull\tDescription\n" +
                "SampleId\tSample Id\txsd:string\ttrue\tstring\n" +
                "DateField\tDateField\txsd:dateTime\tfalse\tThis is a date field\n" +
                "NumberField\tNumberField\txsd:double\ttrue\tThis is a number\n" +
                "TextField\tTextField\txsd:string\tfalse\tThis is a text field");

        clickNavButton("Import", 0);
        waitForElement(Locator.xpath("//input[@id='ff_label3']"), WAIT_FOR_GWT);

        clickRadioButtonById("button_dataField");

        clickNavButton("Save");
        waitForElement(Locator.navButton("View Dataset Data"), WAIT_FOR_GWT);
        clickNavButton("View Dataset Data");
        clickNavButton("Import Data");

        String errorRow = "\tbadvisitd\t1/1/2006\t\ttext\t";
        setFormElement("tsv", _tsv + "\n" + errorRow);
        clickNavButton("Import Data");
        assertTextPresent("Row 3 does not contain required field participantid.");
        assertTextPresent("Row 3 data type error for field SequenceNum.");
        assertTextPresent("Row 3 does not contain required field SampleId.");
        assertTextPresent("Row 3 data type error for field DateField.");
        assertTextPresent("Row 3 does not contain required field NumberField.");

        setFormElement("tsv", _tsv);
        clickNavButton("Import Data", longWaitForPage);
        assertTextPresent("1234");
        assertTextPresent("2006-02-01");
        assertTextPresent("1.2");
    }

    private void deleteLogFiles()
    {
        File dataRoot = new File(getPipelinePath());
        File[] logFiles = dataRoot.listFiles(new FilenameFilter(){
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".log");
            }
        });
        for (File f : logFiles)
            f.delete();
    }

    private void deleteAssayUploadFiles()
    {
        deleteDir(new File(getPipelinePath(), "assaydata"));
    }

    private void deleteReportFiles()
    {
        deleteDir(new File(getPipelinePath(), "Reports"));
    }

    private void deleteDir(File dir)
    {
        if (!dir.exists())
            return;

        File[] assayDataFiles = dir.listFiles();
        for (File f : assayDataFiles)
            f.delete();

        dir.delete();
    }

    protected void createReport(String reportType)
    {
        // click the create button dropdown
        String id = ExtHelper.getExtElementId(this, "btn_createView");
        click(Locator.id(id));

        id = ExtHelper.getExtElementId(this, reportType);
        click(Locator.id(id));
        waitForPageToLoad();
    }
}
