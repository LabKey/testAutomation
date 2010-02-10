/*
 * Copyright (c) 2008-2010 LabKey Corporation
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * User: brittp
 * Date: Mar 9, 2006
 * Time: 1:54:57 PM
 */
public class StudyManualTest extends StudyTest
{
    private final String CRF_SCHEMAS = getSampleDataPath() + "datasets/schema.tsv";

    protected final String VISIT_MAP = getSampleDataPath() + "v068_visit_map.txt";

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void doCreateSteps()
    {
        createStudyManually();
        startSpecimenImport(2);

        // wait for datasets (but not specimens) to finish
        waitForPipelineJobsToComplete(1, "study import");
        afterManualCreate();
    }

    protected void createStudyManually()
    {
        initializeFolder();

        clickNavButton("Create Study");
        click(Locator.radioButtonByNameAndValue("simpleRepository", "false"));
        setText("subjectNounSingular", "Mouse");
        setText("subjectNounPlural", "Mice");
        setText("subjectColumnName", "MouseId");
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

        // upload datasets:
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Data Pipeline");
        clickNavButton("Setup");
        setPipelineRoot(getPipelinePath());
        clickLinkWithText("Pipeline");
        clickNavButton("Process and Import Data");
        waitAndClick(Locator.fileTreeByName("datasets"));
        ExtHelper.waitForImportDataEnabled(this);
        ExtHelper.selectFileBrowserFile(this, "Study001.dataset");
        //waitForElement(Locator.navButton("Import datasets"), WAIT_FOR_JAVASCRIPT);
        if (isNavButtonPresent("Delete log"))
            clickNavButton("Delete log");
        selectImportDataAction("Import Datasets");
        waitForPageToLoad();
        clickNavButton("Start Import");
    }

    // Using old visit map format, which does not support default visibility (so we need to set it manually).
    protected void afterManualCreate()
    {
        hideSceeningVisit();
        setDemographicsDescription();
        setDemographicsBit();
        createCustomAssays();
    }


    protected void hideSceeningVisit()
    {
        clickLinkWithText(getFolderName());
        hideVisits("Screening Cycle", "Cycle 1");
    }


    protected void setDemographicsDescription()
    {
        clickLinkWithText(getFolderName());
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("DEM-1: Demographics");
        clickButtonContainingText("Edit Dataset Definition");
        waitForElement(Locator.name("description"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        setFormElement("description", DEMOGRAPHICS_DESCRIPTION);
        clickNavButton("Save");
    }


    protected void setDemographicsBit()
    {
        clickLinkWithText(getFolderName());
        setDemographicsBit("DEM-1: Demographics", true);
    }


    // Hide visits based on label -- manual create vs. import will result in different indexes for these visits
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
        clickLinkWithText(getFolderName());
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Create New Dataset");
        setFormElement("typeName", "verifyAssay");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='DatasetDesignerName']"), WAIT_FOR_JAVASCRIPT);

        checkRadioButton("additionalKey", 1);

        clickNavButton("Import Fields", 0);
        waitForElement(Locator.xpath("//textarea[@id='schemaImportBox']"), WAIT_FOR_JAVASCRIPT);

        setFormElement("schemaImportBox", "Property\tLabel\tRangeURI\tNotNull\tDescription\n" +
                "SampleId\tSample Id\txsd:string\ttrue\tstring\n" +
                "DateField\tDateField\txsd:dateTime\tfalse\tThis is a date field\n" +
                "NumberField\tNumberField\txsd:double\ttrue\tThis is a number\n" +
                "TextField\tTextField\txsd:string\tfalse\tThis is a text field");

        clickNavButton("Import", 0);
        waitForElement(Locator.xpath("//input[@id='ff_label3']"), WAIT_FOR_JAVASCRIPT);

        clickRadioButtonById("button_dataField");

        addField("Dataset Fields", 4, "otherData", "Other Data", "Text (String)");
        setFormElement(Locator.id("importAliases"), "aliasedColumn");

        clickNavButton("Save");
        waitForElement(Locator.navButton("View Dataset Data"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("View Dataset Data");
        clickNavButton("Import Data");

        String errorRow = "\tbadvisitd\t1/1/2006\t\ttext\t";
        setFormElement("tsv", _tsv + "\n" + errorRow);
        clickNavButton("Import Data");
        assertTextPresent("Row 3 does not contain required field MouseId.");
        assertTextPresent("Row 3 data type error for field SequenceNum.");
        assertTextPresent("Row 3 does not contain required field SampleId.");
        assertTextPresent("Row 3 data type error for field DateField.");
        assertTextPresent("Row 3 does not contain required field NumberField.");

        setFormElement("tsv", _tsv);
        clickNavButton("Import Data", longWaitForPage);
        assertTextPresent("1234");
        assertTextPresent("2006-02-01");
        assertTextPresent("1.2");
        assertTextPresent("aliasedData");
    }
}
