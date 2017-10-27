/*
 * Copyright (c) 2013-2017 LabKey Corporation
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

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@Category({DailyB.class})
public class DrugSensitivityAssayTest extends AbstractQCAssayTest
{
    private final static String TEST_ASSAY_PROJECT = "Drug Sensitivity Test Verify Project";
    private static final String PLATE_TEMPLATE_NAME = "DrugSensitivityAssayTest Template";

    protected static final String TEST_ASSAY_NAME = "TestAssayDrugSensitivity";
    protected static final String TEST_ASSAY_DESC = "Description for Drug Sensitivity assay";

    protected final String TEST_ASSAY_FILE1 = TestFileUtils.getLabKeyRoot() + "/sampledata/DrugSensitivity/1.txt";
    protected final String TEST_ASSAY_FILE2 = TestFileUtils.getLabKeyRoot() + "/sampledata/DrugSensitivity/2.txt";
    protected final String TEST_ASSAY_FILE3 = TestFileUtils.getLabKeyRoot() + "/sampledata/DrugSensitivity/3.txt";

    protected final String TEST_ASSAY_DATA_ACQUISITION_FILE1 = TestFileUtils.getLabKeyRoot() + "/sampledata/DrugSensitivity/acquisition1.xlsx";
    protected final String TEST_ASSAY_DATA_ACQUISITION_FILE2 = TestFileUtils.getLabKeyRoot() + "/sampledata/DrugSensitivity/acquisition2.xlsx";
    protected final String TEST_ASSAY_DATA_ACQUISITION_FILE3 = TestFileUtils.getLabKeyRoot() + "/sampledata/DrugSensitivity/acquisition3.xlsx";

    @Test
    public void runUITests() throws Exception
    {
        PortalHelper portalHelper = new PortalHelper(this);

        log("Starting Drug Sensitivity Assay BVT Test");

        //revert to the admin user
        ensureSignedInAsPrimaryTestUser();

        log("Testing Drug Sensitivity Assay Designer");

        // set up a scripting engine to run a java transform script
        prepareProgrammaticQC();

        //create a new test project
        _containerHelper.createProject(getProjectName(), null);

        //setup a pipeline for it
        setupPipeline(getProjectName());

        _containerHelper.createSubfolder(getProjectName(), TEST_ASSAY_FLDR_STUDY1);
        portalHelper.addWebPart("Study Overview");
        clickButton("Create Study");
        click(Locator.radioButtonById("dateTimepointType"));
        clickButton("Create Study");

        goToProjectHome();
        portalHelper.addWebPart("Assay List");
        createTemplate();

        //create a new assay
        log("Setting up Drug Sensitivity assay");
        goToProjectHome();
        _assayHelper.createAssayAndEdit("Drug Sensitivity", TEST_ASSAY_NAME)
                .setDescription(TEST_ASSAY_DESC)
                .setPlateTemplate(PLATE_TEMPLATE_NAME)
                .save();

        goToProjectHome();
        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAME));

        log("Uploading Drug Sensitivity Runs");
        clickButton("Import Data");
        clickButton("Next");
        uploadFile(TEST_ASSAY_FILE1, null, "11223344", true);

        click(Locator.linkContainingText("Import Data"));
        clickButton("Next");
        uploadFile(TEST_ASSAY_FILE2, null, "55667788", false);

        click(Locator.linkContainingText("Import Data"));
        clickButton("Next");
        uploadFile(TEST_ASSAY_FILE3, TEST_ASSAY_DATA_ACQUISITION_FILE3, "12341234", false);

        // verify details view has a custom sample label
        assertTextPresent("Drug Treatment Information");

        clickAndWait(Locator.linkContainingText("View Runs"));
        assertElementPresent(Locator.linkContainingText("assaydata" + File.separator + "acquisition3"));
        clickAndWait(Locator.linkContainingText("3.txt"));

        DataRegionTable table = new DataRegionTable("Data", this);

        assertEquals("Wrong number of rows", 3, table.getDataRowCount());

        testCopyToStudy();
    }

    protected void uploadFile(String filePath, String acquisitionFilePath, String ptid, boolean checkRequired)
    {
        // cutoff values
        setFormElement(Locator.name("cutoff1"), "50");
        setFormElement(Locator.name("cutoff2"), "75");
        setFormElement(Locator.name("cutoff3"), "99");

        setFormElement(Locator.name("mediaType"), "serum");
        setFormElement(Locator.name("mediaFreezerProID"), "77768");
        setFormElement(Locator.name("totalEventPerWell"), "1000");

        setFormElement(Locator.name("participantID"), ptid);
        setFormElement(Locator.name("date"), "5/18/2013");
        setFormElement(Locator.name("experimentPerformer"), "John White");
        selectOptionByText(Locator.name("curveFitMethod"), "Four Parameter");

        // form values
        String[] drugs = {"GSK", "DSM-1", "Quinine"};
        String[] dilution = {"40000", "40000", "20000"};
        String[] factor = {"4", "4", "4"};
        String[] method = {"Concentration", "Concentration", "Concentration"};

        for (int i = 0; i < 3; i++)
        {
            Locator treatmentLocator = Locator.name("drug" + (i+1) + "_TreatmentName");
            Locator concentrationLocator = Locator.name("drug" + (i+1) + "_InitialDilution");
            Locator dilutionFactorLocator = Locator.name("drug" + (i+1) + "_Factor");
            Locator methodLocator = Locator.name("drug" + (i+1) + "_Method");

            setFormElement(treatmentLocator, drugs[i]);
            setFormElement(concentrationLocator, dilution[i]);
            setFormElement(dilutionFactorLocator, factor[i]);
            selectOptionByText(methodLocator, method[i]);
        }

        if (acquisitionFilePath != null)
        {
            File acquisitionFile = new File(acquisitionFilePath);
            setFormElement(Locator.name("dataAcquisitionFile"), acquisitionFile);
        }
        File file1 = new File(filePath);
        setFormElement(Locator.name("__primaryFile__"), file1);

        clickButton("Save and Finish");

        if (checkRequired)
        {
            // validate required field
            assertTextPresent("Initial Parasitemia Percent is required");
            setFormElement(Locator.name("initialParasitemia"), "1.60");

            clickButton("Save and Finish");
        }
    }

    @Override
    protected String getProjectName()
    {
        return TEST_ASSAY_PROJECT;
    }

    protected void createTemplate()
    {
        clickButton("Manage Assays");
        clickButton("Configure Plate Templates");
        clickAndWait(Locator.linkWithText("new 96 well (8x12) Drug Sensitivity default template"));
        final WebElement nameField = waitForElement(Locator.id("templateName"), WAIT_FOR_JAVASCRIPT);
        setFormElement(nameField, PLATE_TEMPLATE_NAME);
        fireEvent(nameField, SeleniumEvent.change);

        clickButton("Save & Close");
        waitForText(PLATE_TEMPLATE_NAME);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("icemr");
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    private void testCopyToStudy()
    {
        DataRegionTable table = new DataRegionTable("Data", this);
        table.checkAllOnPage();
        table.clickHeaderButtonAndWait("Copy to Study");

        selectOptionByText(Locator.name("targetStudy"), "/" + getProjectName() + "/" + TEST_ASSAY_FLDR_STUDY1 + " (" + TEST_ASSAY_FLDR_STUDY1 + " Study)");
        clickButton("Next");
        table.clickHeaderButtonAndWait("Copy to Study");

        DataRegionTable dataset = new DataRegionTable("Dataset", this);

        assertEquals("Wrong number of rows", 3, dataset.getDataRowCount());

        // verify cutoff properties are pulled through
        Set<String> cutoffColumns = new HashSet<>(Arrays.asList(
                "Fit Error",
                "Curve IC50",
                "Point IC50",
                "Curve IC75",
                "Point IC75",
                "Curve IC99",
                "Point IC99"));
        Set<String> columnHeaders = new HashSet<>(dataset.getColumnLabels());

        assertEquals("Cutoff properties not present", cutoffColumns, new HashSet<>(CollectionUtils.intersection(cutoffColumns, columnHeaders)));
    }
}
