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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

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
    public static final String FLASK_SAMPLESET_COLS  =
    "Property\tLabel\tRangeURI\tFormat\tNotNull\tHidden\tMvEnabled\tDescription\n" +
    "PatientID\tPatient ID\thttp://www.w3.org/2001/XMLSchema#string\t\tTRUE\tFALSE\tFALSE\n" +
    "SampleID\tSample ID\thttp://www.w3.org/2001/XMLSchema#string\t\tTRUE\tFALSE\tFALSE\n" +
    "Scientist\tScientist\thttp://www.w3.org/2001/XMLSchema#string\t\tTRUE\tFALSE\tFALSE\n" +
    "Stage\tStage\thttp://www.w3.org/2001/XMLSchema#string\t\tTRUE\tFALSE\tFALSE\n" +
    "Parasitemia\t\thttp://www.w3.org/2001/XMLSchema#double\t\tTRUE\tFALSE\tFALSE\n" +
    "Gametocytemia\t\tthttp://www.w3.org/2001/XMLSchema#double\t\tTRUE\tFALSE\tFALSE\n" +
    "PatientpRBCs\tPatient pRBCs\thttp://www.w3.org/2001/XMLSchema#string\t\tTRUE\tFALSE\tFALSE\n" +
    "Hematocrit\t\thttp://www.w3.org/2001/XMLSchema#double\t\tTRUE\tFALSE\tFALSE\tHematocrit %\n" +
    "CultureMedia\tCulture Media\thttp://www.w3.org/2001/XMLSchema#string\t\tTRUE\tFALSE\tFALSE\n" +
    "SerumBatchID\tSerum Batch ID\thttp://www.w3.org/2001/XMLSchema#int\t\tFALSE\tFALSE\tFALSE\n" +
    "AlbumaxBatchID\tAlbumax Batch ID\thttp://www.w3.org/2001/XMLSchema#int\t\tFALSE\tFALSE\tFALSE\n" +
    "FoldIncrease1\tFold-Increase Test 1\thttp://www.w3.org/2001/XMLSchema#int\t\tTRUE\tFALSE\tFALSE\n" +
    "FoldIncrease2\tFold-Increase Test 2\thttp://www.w3.org/2001/XMLSchema#int\t\tTRUE\tFALSE\tFALSE\n" +
    "FoldIncrease3\tFold-Increase Test 3\thttp://www.w3.org/2001/XMLSchema#int\tTRUE\tFALSE\tFALSE\n" +
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
        enterDataPoint();
        verifyDataInAssay();
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
        Locator.XPathLocator link = Locator.linkContainingText("Upload data");
        waitAndClick(link);
        waitForElement(Locator.id("upload-diagnostic-form-body"));

        enterData();
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
