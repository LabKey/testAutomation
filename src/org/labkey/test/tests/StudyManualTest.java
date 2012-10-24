/*
 * Copyright (c) 2008-2012 LabKey Corporation
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

import com.thoughtworks.selenium.SeleniumException;
import org.labkey.test.Locator;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.StudyHelper;

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
    private final String CRF_SCHEMAS = getStudySampleDataPath() + "datasets/schema.tsv";

    protected final String VISIT_MAP = getStudySampleDataPath() + "v068_visit_map.txt";

    protected final StudyHelper _studyHelper = new StudyHelper(this);

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
        waitForPipelineJobsToComplete(1, "study import", false);
        afterManualCreate();
    }

    protected void createStudyManually()
    {
        initializeFolder();

        clickButton("Create Study");
        click(Locator.radioButtonByNameAndValue("simpleRepository", "false"));
        setText("subjectNounSingular", "Mouse");
        setText("subjectNounPlural", "Mice");
        setText("subjectColumnName", "MouseId");
        clickButton("Create Study");

        // change study label
        clickLinkWithText("Change Study Properties");
        waitForElement(Locator.name("Label"), WAIT_FOR_JAVASCRIPT);
        setFormElement("Label", getStudyLabel());
        clickButton("Submit");
        assertTextPresent(getStudyLabel());

        // import visit map
        clickTab("Manage");
        clickLinkWithText("Manage Visits");
        clickLinkWithText("Import Visit Map");
        String visitMapData = getFileContents(VISIT_MAP);
        setLongTextField("content", visitMapData);
        clickButton("Import");

        // import custom visit mapping
        importCustomVisitMappingAndVerify();

        // define forms
        clickTab("Manage");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Define Dataset Schemas");
        clickLinkWithText("Bulk Import Schemas");
        setFormElement("typeNameColumn", "platename");
        setFormElement("labelColumn", "platelabel");
        setFormElement("typeIdColumn", "plateno");
        setFormElement(Locator.name("tsv"), getFileContents(CRF_SCHEMAS));
        clickButton("Submit", 180000);

        // setup cohorts:
        clickTab("Manage");
        clickLinkWithText("Manage Cohorts");
        selectOptionByText("participantCohortDataSetId", "EVC-1: Enrollment Vaccination");
        waitForPageToLoad();
        selectOptionByText("participantCohortProperty", "2. Enrollment group");
        clickButton("Update Assignments");

        // configure QC state management so that all data is displayed by default (we'll test with hidden data later):
        clickLinkWithText(getStudyLabel());
        clickTab("Manage");
        clickLinkWithText("Manage Dataset QC States");
        selectOptionByText("showPrivateDataByDefault", "All data");
        clickButton("Save");

        // upload datasets:
        clickLinkWithText(getStudyLabel());
        setPipelineRoot(getPipelinePath());
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Files");
        clickButton("Process and Import Data");
        _extHelper.selectFileBrowserItem("datasets/Study001.dataset");
        if (isNavButtonPresent("Delete log"))
            clickButton("Delete log");
        selectImportDataAction("Import Datasets");
        clickButton("Start Import");
        waitForPipelineJobsToComplete(1, "study import", false);

    }


    // Using old visit map format, which does not support default visibility, etc. (so we need to set these manually).
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


    protected void importCustomVisitMappingAndVerify()
    {
        // Import custom mapping
        importCustomVisitMapping();

        // Test clearing the custom mapping
        clickLinkWithText("Clear Custom Mapping");
        clickLinkWithText("OK");
        assertTextPresent("The custom mapping is currently empty");
        assertNavButtonPresent("Import Custom Mapping");
        assertNavButtonNotPresent("Replace Custom Mapping");
        assertNavButtonNotPresent("Clear Custom Mapping");
        assertTextNotPresent("Vaccine 1");
        assertTextNotPresent("Vaccination 1");
        assertTextNotPresent("Cycle 10");
        assertTextNotPresent("All Done");

        // Import custom mapping again
        importCustomVisitMapping();
    }


    protected void importCustomVisitMapping()
    {
        if (!isLinkPresentContainingText("Visit Import Mapping"))
        {
            clickTab("Manage");
            clickLinkWithText("Manage Visits");
        }

        clickLinkWithText("Visit Import Mapping");
        clickButton("Import Custom Mapping");
        setFormElement(Locator.id("tsv"), VISIT_IMPORT_MAPPING);
        clickButton("Submit");

        assertTextPresentInThisOrder("Cycle 10", "Vaccine 1", "Vaccination 1", "All Done");
    }


    protected void setDemographicsDescription()
    {
        clickLinkWithText(getFolderName());
        clickTab("Manage");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("DEM-1: Demographics");
        clickButtonContainingText("Edit Definition");
        waitForElement(Locator.name("description"), WAIT_FOR_JAVASCRIPT);
        setFormElement("description", DEMOGRAPHICS_DESCRIPTION);
        clickButton("Save");
    }


    protected void setDemographicsBit()
    {
        clickLinkWithText(getFolderName());
        setDemographicsBit("DEM-1: Demographics", true);
    }


    // Hide visits based on label -- manual create vs. import will result in different indexes for these visits
    protected void hideVisits(String... visitLabel)
    {
        clickTab("Manage");
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
                clickButton("Save");
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
        clickTab("Manage");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Create New Dataset");
        setFormElement("typeName", "verifyAssay");
        clickButton("Next");

        waitForElement(Locator.xpath("//input[@id='DatasetDesignerName']"), WAIT_FOR_JAVASCRIPT);

        checkRadioButton("additionalKey", 1);

        clickButton("Import Fields", 0);
        waitForElement(Locator.xpath("//textarea[@id='schemaImportBox']"), WAIT_FOR_JAVASCRIPT);

        setFormElement("schemaImportBox", "Property\tLabel\tRangeURI\tNotNull\tDescription\n" +
                "SampleId\tSample Id\txsd:string\ttrue\tstring\n" +
                "DateField\tDateField\txsd:dateTime\tfalse\tThis is a date field\n" +
                "NumberField\tNumberField\txsd:double\ttrue\tThis is a number\n" +
                "TextField\tTextField\txsd:string\tfalse\tThis is a text field");

        clickButton("Import", 0);
        waitForElement(Locator.xpath("//input[@name='ff_label3']"), WAIT_FOR_JAVASCRIPT);

        clickRadioButtonById("button_dataField");

        addField("Dataset Fields", 4, "otherData", "Other Data", ListHelper.ListColumnType.String);
        click(Locator.xpath("//span[contains(@class,'x-tab-strip-text') and text()='Advanced']"));
        waitForElement(Locator.id("importAliases"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.id("importAliases"), "aliasedColumn");

        clickButton("Save");
        waitForElement(Locator.navButton("View Data"), WAIT_FOR_JAVASCRIPT);
        clickButton("View Data");
        clickButton("Import Data");

        String errorRow = "\tbadvisitd\t1/1/2006\t\ttext\t";
        setFormElement("text", _tsv + "\n" + errorRow);
        _listHelper.submitImportTsv_error("Could not convert 'badvisitd'");
        assertTextPresent("Could not convert 'text'");

        _listHelper.submitTsvData(_tsv);
        assertTextPresent("1234");
        assertTextPresent("2006-02-01");
        assertTextPresent("1.2");
        assertTextPresent("aliasedData");
    }
}
