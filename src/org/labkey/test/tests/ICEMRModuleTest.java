/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.labkey.test.util.ExcelHelper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: elvan
 * Date: 12/27/12
 * Time: 7:02 PM
 */
public class ICEMRModuleTest extends BaseWebDriverTest
{
    public static final String ID = "myid";
    public static final String DIAGNOSTICS_ASSAY_DESIGN = "ICEMR Diagnostics";
    public static final String ADAPTATION_ASSAY_DESIGN = "ICEMR Adaptation";
    public static final String DIAGNOSTIC_ASSAY_NAME = "Diagnostics Assay";
    public static final String ADAPTATION_ASSAY_NAME = "Adaptation Assay";
    public static final String FLASKS_SAMPLESET_NAME = "Flasks";
    public static final String FOLD_INCREASE_DEFAULT = "4";
    public static final String ADAPTATION_CRITERIA_DEFAULT = "2";
    public static final String FLASK_SAMPLESET_COLS  =
    "Property\tLabel\tRangeURI\tFormat\tNotNull\tHidden\tMvEnabled\tDescription\n" +
    "PatientID\tPatient ID\thttp://www.w3.org/2001/XMLSchema#string\t\tTRUE\tFALSE\tFALSE\n" +
    "SampleID\tSample ID\thttp://www.w3.org/2001/XMLSchema#string\t\tTRUE\tFALSE\tFALSE\n" +
    "Scientist\tScientist\thttp://www.w3.org/2001/XMLSchema#string\t\tTRUE\tFALSE\tFALSE\n" +
    "Stage\tStage\thttp://www.w3.org/2001/XMLSchema#string\t\tTRUE\tFALSE\tFALSE\n" +
    "Parasitemia\t\thttp://www.w3.org/2001/XMLSchema#double\t\tTRUE\tFALSE\tFALSE\n" +
    "Gametocytemia\t\thttp://www.w3.org/2001/XMLSchema#double\t\tTRUE\tFALSE\tFALSE\n" +
    "PatientpRBCs\tPatient pRBCs\thttp://www.w3.org/2001/XMLSchema#string\t\tTRUE\tFALSE\tFALSE\n" +
    "Hematocrit\t\thttp://www.w3.org/2001/XMLSchema#double\t\tTRUE\tFALSE\tFALSE\tHematocrit %\n" +
    "CultureMedia\tCulture Media\thttp://www.w3.org/2001/XMLSchema#string\t\tTRUE\tFALSE\tFALSE\n" +
    "SerumBatchID\tSerum Batch ID\thttp://www.w3.org/2001/XMLSchema#string\t\tFALSE\tFALSE\tFALSE\n" +
    "AlbumaxBatchID\tAlbumax Batch ID\thttp://www.w3.org/2001/XMLSchema#string\t\tFALSE\tFALSE\tFALSE\n" +
    "FoldIncrease1\tFold-Increase Test 1\thttp://www.w3.org/2001/XMLSchema#int\t\tTRUE\tFALSE\tFALSE\n" +
    "FoldIncrease2\tFold-Increase Test 2\thttp://www.w3.org/2001/XMLSchema#int\t\tTRUE\tFALSE\tFALSE\n" +
    "FoldIncrease3\tFold-Increase Test 3\thttp://www.w3.org/2001/XMLSchema#int\t\tTRUE\tFALSE\tFALSE\n" +
    "AdaptationCriteria\tAdaptation Criteria\thttp://www.w3.org/2001/XMLSchema#int\t\tTRUE\tFALSE\tFALSE\n" +
    "Comments\t\thttp://www.w3.org/2001/XMLSchema#multiLine\t\tFALSE\tFALSE\tFALSE\n" +
    "MaintenanceDate\tMaintenance Date\thttp://www.w3.org/2001/XMLSchema#dateTime\t\tFALSE\tFALSE\tFALSE\n" +
    "MaintenanceStopped\tMaintenance Stopped\thttp://www.w3.org/2001/XMLSchema#dateTime\t\tFALSE\tFALSE\tFALSE\n" +
    "StartParasitemia1\tParasitemia Test 1 Start\thttp://www.w3.org/2001/XMLSchema#double\t\tFALSE\tFALSE\tFALSE\n" +
    "FinishParasitemia1\tParasitemia Test 1 Finish\thttp://www.w3.org/2001/XMLSchema#double\t\tFALSE\tFALSE\tFALSE\n" +
    "StartParasitemia2\tParasitemia Test 2 Start\thttp://www.w3.org/2001/XMLSchema#double\t\tFALSE\tFALSE\tFALSE\n" +
    "FinishParasitemia2\tParasitemia Test 2 Finish\thttp://www.w3.org/2001/XMLSchema#double\t\tFALSE\tFALSE\tFALSE\n" +
    "StartParasitemia3\tParasitemia Test 3 Start\thttp://www.w3.org/2001/XMLSchema#double\t\tFALSE\tFALSE\tFALSE\n" +
    "FinishParasitemia3\tParasitemia Test 3 Finish\thttp://www.w3.org/2001/XMLSchema#double\t\tFALSE\tFALSE\tFALSE\n" +
    "StartDate1\tStart Date Test 1\thttp://www.w3.org/2001/XMLSchema#dateTime\t\tFALSE\tFALSE\tFALSE\n" +
    "FinishDate1\tFinish Date Test 1\thttp://www.w3.org/2001/XMLSchema#dateTime\t\tFALSE\tFALSE\tFALSE\n" +
    "AdaptationDate\tAdaptation Date\thttp://www.w3.org/2001/XMLSchema#dateTime\t\tFALSE\tFALSE\tFALSE";

    public static final String SCIENTIST = "Torruk";

    @Override
    protected String getProjectName()
    {
        return "ICEMR assay test";
    }

    @Override
    protected void doTestSteps() throws Exception
    {

        log("Create ICEMR with appropriate web parts");
        _containerHelper.createProject(getProjectName(), "ICEMR");
        createAdaptationAssay();
        createDiagnosticAssay();
        createFlasksSampleSet();
        testJavaScript();
        enterDataPoint();
        verifyDataInAssay();
        enterDataPointAdaptation();
        enterDailyAdaptationData();
        checkResultsPage();
    }

    private void verifyDataInAssay()
    {
        waitForElement(Locator.id("dataregion_Data"));
        for (String value: fieldAndValue.values())
        {
            assertElementPresent(Locator.css("#dataregion_Data td").withText(value));
        }
        goToProjectHome();
    }

    private void enterDataPoint()
    {
        Locator.XPathLocator link = Locator.linkContainingText("Diagnostics Assay");
        waitAndClick(link);
        link = Locator.navButtonContainingText("Import Data");
        waitAndClick(link);
        waitForElement(Locator.id("upload-diagnostic-form-body"));
        enterData();
    }

    private void enterDataPointAdaptation()
    {
        Locator.XPathLocator link = Locator.linkContainingText("Adaptation Assay");
        waitAndClick(link);
        link = Locator.navButtonContainingText("New Experiment");
        waitAndClick(link);
        waitForElement(Locator.id("SampleID1"));
        enterAdaptationData();
    }

    private void enterAdaptationData()
    {
        verifyError(7);

        // verify our default values for fold increase and adaptation criteria are correct
        for (int i = 1; i < 4; i++)
        {
            assertFormElementEquals(Locator.name("FoldIncrease" + i + "1"), FOLD_INCREASE_DEFAULT);
        }
        assertFormElementEquals(Locator.name("AdaptationCriteria1"), ADAPTATION_CRITERIA_DEFAULT);

        fieldAndValue = new HashMap<String, String>();

        fieldAndValue.put("PatientID", "100101");
        fieldAndValue.put("ExperimentID", "12345");
        fieldAndValue.put("SampleID1", "15243");
        fieldAndValue.put("Scientist1", "Dr. Helvetica");
        fieldAndValue.put("Gametocytemia1", "20");
        fieldAndValue.put("Hematocrit1", "24");
        fieldAndValue.put("Parasitemia1", "");
        fieldAndValue.put("SerumBatchID1", "00123");
        fieldAndValue.put("AlbumaxBatchID1", "10213");
        fieldAndValue.put("FoldIncrease11", "10");
        fieldAndValue.put("FoldIncrease21", "11");
        fieldAndValue.put("FoldIncrease31", "12");
        fieldAndValue.put("AdaptationCriteria1", "24");
        fieldAndValue.put("Comments1", "Lorem ipsum");

        for(String field : fieldAndValue.keySet())
        {
            setICEMRField(field, fieldAndValue.get(field));
        }

        //Verify a missing field (parasitemia)
        verifyError(1);

        //Verify fields out of acceptable bounds
        setICEMRField("Hematocrit1", "101");
        setICEMRField("Gametocytemia1", "101");
        setICEMRField("Parasitemia1", "101");
        verifyError(3);

        setICEMRField("Hematocrit1", "20");
        setICEMRField("Gametocytemia1", "22");
        setICEMRField("Parasitemia1", "25");

        clickButton("Add Flask", "Flask 2");

        makeAdaptationFlask(2);

        clickButtonContainingText("Submit");
        waitForElement(Locator.css(".labkey-nav-page-header").withText(ADAPTATION_ASSAY_NAME + " Runs"));
    }

    private void makeAdaptationFlask(int flaskNum)
    {
        verifyError(5);

        fieldAndValue = new HashMap<String, String>();

        fieldAndValue.put("SampleID" + flaskNum, "15258");
        fieldAndValue.put("Scientist" + flaskNum, "Dr. Helvetica");
        fieldAndValue.put("Gametocytemia" + flaskNum, "24");
        fieldAndValue.put("Hematocrit" + flaskNum, "28");
        fieldAndValue.put("Parasitemia" + flaskNum, "27");
        fieldAndValue.put("SerumBatchID"+ flaskNum, "00123");
        fieldAndValue.put("AlbumaxBatchID"+ flaskNum, "10213");
        fieldAndValue.put("FoldIncrease1"+ flaskNum, "12");
        fieldAndValue.put("FoldIncrease2"+ flaskNum, "13");
        fieldAndValue.put("FoldIncrease3"+ flaskNum, "14");
        fieldAndValue.put("AdaptationCriteria"+ flaskNum, "20");
        fieldAndValue.put("Comments"+ flaskNum, "Lorem ipsum");

        for(String field : fieldAndValue.keySet())
        {
            setICEMRField(field, fieldAndValue.get(field));
        }
        sleep(1000);
    }

    private void enterDailyAdaptationData(){

        //Navigate to Daily Upload page
        Locator.XPathLocator link = Locator.linkContainingText("Daily Maintenance");
        waitAndClick(link);

        checkTemplate();
        waitForElement(Locator.name("dailyUpload"));

        //Try to upload a form with bad columns
        setFormElement(Locator.name("dailyUpload"), new File(getLabKeyRoot(), "sampledata/icemr/missingColumns.xls"));
        clickButtonContainingText("Upload", "The data file header row does not match the daily results schema");
        _extHelper.waitForExtDialog("Daily Upload Failed");
        clickButtonContainingText("OK", "Daily Upload");
        _extHelper.waitForExtDialogToDisappear("Daily Upload Failed");

        //Try to upload a form with bad flasks (invalid IDs)
        setFormElement(Locator.name("dailyUpload"), new File(getLabKeyRoot(), "sampledata/icemr/badFlasks.xls"));
        clickButtonContainingText("Upload", "Submit");
        sleep(500);
        clickButtonContainingText("Submit", "Invalid flask specified");
        _extHelper.waitForExtDialog("Daily Maintenance Error");
        clickButtonContainingText("OK", "Result");
        _extHelper.waitForExtDialogToDisappear("Daily Maintenance Error");

        //Upload test
        refresh();
        waitForElement(Locator.name("dailyUpload"));
        setFormElement(Locator.name("dailyUpload"), new File(getLabKeyRoot(), "sampledata/icemr/dailyUploadFilled.xls"));
        clickButtonContainingText("Upload", "Scientist Name");
        sleep(500);
        clickButton("Submit");

        //Ensure that you can't add flasks if maintenance has been stopped on that flask.
        waitAndClick(link);
        waitForElement(Locator.name("dailyUpload"));
        setFormElement(Locator.name("dailyUpload"), new File(getLabKeyRoot(), "sampledata/icemr/dailyUploadFilled.xls"));
        clickButtonContainingText("Upload", "Scientist Name");
        sleep(500);
        clickButtonContainingText("Submit", "Invalid flask specified");
        _extHelper.waitForExtDialog("Daily Maintenance Error");
        clickButtonContainingText("OK", "Result");
        _extHelper.waitForExtDialogToDisappear("Daily Maintenance Error");
        clickButton("Cancel");
    }

    private void checkTemplate(){
        waitForElement(Locator.name("dailyUpload"));
        clickButtonContainingText("Get Template", "Daily Upload");
        File templateFile = new File(getDownloadDir(), "dailyUpload.xls");
        try{
            Workbook template = ExcelHelper.create(templateFile);
            Sheet sheet = template.getSheetAt(0);
            //Warnings about possible null pointers can be ignored, as all cells in question are tested for null before loading them.
            if(sheet != null){
                for(int i = 0; i < 18; i++)
                {
                    Assert.assertNotNull(ExcelHelper.getCell(sheet, i, 0));
                }
                Assert.assertEquals(ExcelHelper.getCell(sheet, 0, 0).toString(), "SampleID");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 1, 0).toString(), "MeasurementDate");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 2, 0).toString(), "Scientist");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 3, 0).toString(), "Parasitemia");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 4, 0).toString(), "Gametocytemia");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 5, 0).toString(), "Stage");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 6, 0).toString(), "Removed");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 7, 0).toString(), "RBCBatchID");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 8, 0).toString(), "SerumBatchID");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 9, 0).toString(), "AlbumaxBatchID");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 10, 0).toString(), "GrowthFoldTestInitiated");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 11, 0).toString(), "GrowthFoldTestFinished");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 12, 0).toString(), "Contamination");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 13, 0).toString(), "MycoTestResult");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 14, 0).toString(), "FreezerProIDs");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 15, 0).toString(), "FlaskMaintenanceStopped");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 16, 0).toString(), "InterestingResult");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 17, 0).toString(), "Comments");


                Assert.assertNotNull(ExcelHelper.getCell(sheet, 0, 1));
                Assert.assertNotNull(ExcelHelper.getCell(sheet, 0, 2));
                Assert.assertEquals(ExcelHelper.getCell(sheet, 0, 1).toString(), "15243");
                Assert.assertEquals(ExcelHelper.getCell(sheet, 0, 2).toString(), "15258");
            }
        }
        catch (IOException e)
        {
            Assert.fail("IOException creating the template file");
        }
        catch (InvalidFormatException e)
        {
            Assert.fail("Template file has invalid format.");
        }
    }

    private void checkResultsPage(){
        Locator.XPathLocator link = Locator.xpath("//a[text()='100101']");
        waitAndClick(link);
        //Make sure the header is there and we are in the right place
        waitForText("12345");
        //Make sure the flasks we'd expect are there
        waitForText("15243");
        waitForText("15258");
        //Hop into one of the flasks to make sure that they have data
        link = Locator.xpath("//a[text()='15243']");
        waitAndClick(link);
        waitForText("100101");
        waitForText("15243");
    }

    private void verifyError(int errorCount)
    {
        clickButton("Submit", 0);
        waitForElementToDisappear(Locator.css(".x4-form-invalid-field").index(errorCount), WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.css(".x4-form-invalid-field").index(errorCount - 1), WAIT_FOR_JAVASCRIPT);
        assertElementPresent(Locator.id("error-div").withText("> Errors in your submission. See below."));
    }

    private Map<String, String> fieldAndValue = new HashMap<String, String>();
    private void enterData()
    {
        verifyError(10);

        fieldAndValue = new HashMap<String, String>();

        fieldAndValue.put("Scientist", SCIENTIST);
        fieldAndValue.put("ParticipantID", ID);
        fieldAndValue.put("ProcessingProtocol", "1");
        fieldAndValue.put("InitParasitemia", "0.3");
        fieldAndValue.put("ParasiteDensity", "-34"); // invalid: can't have negative number
        fieldAndValue.put("InitGametocytemia", "3.5");
        fieldAndValue.put("GametocyteDensity", "3.4"); // invalid: can't have a float for an int
        fieldAndValue.put("PatientHemoglobin", "300.4");
        fieldAndValue.put("Hematocrit", "500"); // invalid: can't have percentage > 100
//        fieldAndValue.put("thinbloodsmear", "3.4");
        fieldAndValue.put("RDT", "3.4"); //this should be ignored
        fieldAndValue.put("FreezerProID", "3.4");

        for(String field : fieldAndValue.keySet())
        {
            setICEMRField(field, fieldAndValue.get(field));
        }

        // Issue 16875: decimals in certain icemr module fields causes js exception
        assertFormElementEquals(Locator.name("GametocyteDensity"), "34"); // '.' can't be entered
        fieldAndValue.put("GametocyteDensity", "34"); // update value

        // we have 2 errors total, fix one at a time
        // Issue 16876: need to screen invalid entries in ICEMR module
        verifyError(2);

        // correct negative number error
        setICEMRField("ParasiteDensity", "34");
        verifyError(1);

        // correct > 100 percent error
        // the form should submit now
        setICEMRField("Hematocrit", "5.0");
        clickButton("Submit");
        waitForElement(Locator.css(".labkey-nav-page-header").withText(DIAGNOSTIC_ASSAY_NAME + " Results"));
    }

    private void createDiagnosticAssay()
    {
        _assayHelper.createAssayWithDefaults(DIAGNOSTICS_ASSAY_DESIGN, DIAGNOSTIC_ASSAY_NAME);
    }

    private void createAdaptationAssay()
    {
        _assayHelper.createAssayWithDefaults(ADAPTATION_ASSAY_DESIGN, ADAPTATION_ASSAY_NAME);
    }
    private void createFlasksSampleSet()
    {
        clickAndWait(Locator.linkWithText(getProjectName()));
        clickButton("Import Sample Set");
        setFormElement("name", FLASKS_SAMPLESET_NAME);
        setFormElement("data", "SampleID\n" + "1");
        clickButton("Submit");

        deleteSample("1");

        // now add our real fields with rich metadata
        clickButton("Edit Fields");
        waitAndClickButton("Import Fields", 0);
        waitForElement(Locator.xpath("//textarea[@id='schemaImportBox']"), WAIT_FOR_JAVASCRIPT);

        setFormElement("schemaImportBox", FLASK_SAMPLESET_COLS);

        clickButton("Import", 0);
        waitForElement(Locator.xpath("//input[@name='ff_label3']"), WAIT_FOR_JAVASCRIPT);
        clickButton("Save");
        clickAndWait(Locator.linkWithText(getProjectName()));
    }

    private void testJavaScript()
    {
        PortalHelper ph = new PortalHelper(this);
        ph.addWebPart("ICEMR Upload Tests");
        // run the test script
        clickButton("Start Test", 0);

        waitFor(new Checker()
        {
            @Override
            public boolean check()
            {
                String s = Locator.id("log-info").findElement(_driver).getText();
                return s.contains("DONE:");
            }
        }, "Test did not finish!", WAIT_FOR_PAGE);

        Assert.assertFalse("At least one of the javascript tests failed", Locator.id("log-info").findElement(_driver).getText().contains("FAILED"));
    }

    private void deleteSample(String sample)
    {
        if (isTextPresent(sample))
        {
            checkCheckbox(Locator.xpath("//td/a[contains(text(), '" + sample + "')]/../../td/input"));
            clickButton("Delete");
            clickButton("Confirm Delete");
        }
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/customModules/icemr";
    }

    protected void setICEMRField(String field, String value)
    {
        setFormElement(Locator.name(field), value);
        fieldAndValue.put(field, value);
    }
}
