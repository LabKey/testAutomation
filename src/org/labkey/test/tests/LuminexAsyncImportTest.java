/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.MiniTest;

import java.io.File;
import java.util.Calendar;

/**
 * User: elvan
 * Date: 2/20/12
 * Time: 1:02 PM
 */
@Category({DailyA.class, MiniTest.class, Assays.class})
public class LuminexAsyncImportTest extends LuminexTest
{
    @Override
    protected void ensureConfigured()
    {
        setUseXarImport(true);
        super.ensureConfigured();
    }

    protected void runUITests()
    {
        click(Locator.name("backgroundUpload"));
        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE1), 0);
        saveAssay();

        importFirstRun();
        importSecondRun(1, Calendar.getInstance(), TEST_ASSAY_LUM_FILE5);
        reimportFirstRun(0, Calendar.getInstance(), TEST_ASSAY_LUM_FILE5);
    }

    protected void importFirstRun() {
        // IMPORT 1ST RUN
        // First file to be import which will subsequently be re-imported
        importRunForTestLuminexConfig(TEST_ASSAY_LUM_FILE5, Calendar.getInstance(), 0);
        assertTextPresent(TEST_ASSAY_LUM + " Upload Jobs");
        waitForPipelineJobsToFinish(2);
        clickAndWait(Locator.linkWithText("COMPLETE", 0));
        assertLuminexLogInfoPresent();
        assertTextNotPresent("ERROR"); //Issue 14082
        assertTextPresent("Starting assay upload", "Finished assay upload");
        clickButton("Data"); // data button links to the run results
        assertTextPresent(TEST_ASSAY_LUM + " Results");

        // test background import failure
        uploadPositivityFile("No Fold Change", TEST_ASSAY_LUM_FILE11, "1", "", true);
        assertTextPresent(TEST_ASSAY_LUM + " Upload Jobs");
        waitForPipelineJobsToFinish(3);
        clickAndWait(Locator.linkWithText("ERROR"));
        assertTextPresent("Error: No value provided for 'Positivity Fold Change'.", 3);
        checkExpectedErrors(2);
    }

    protected void importSecondRun(int index, Calendar testDate, File file) {
        // add a second run with different run values
        int i = index;
        goToTestAssayHome();
        checkCheckbox(".select");
        clickButton("Import Data");
        setFormElement("network", "NEWNET" + (i + 1));
        //assert(getFormElement(Locator.name("network")).equals("NETWORK1"));
        clickButton("Next");
        testDate.add(Calendar.DATE, 1);
        importLuminexRunPageTwo("Guide Set plate " + (i+1), "new"+isotype, "new"+conjugate, "", "", "NewNote" + (i+1),
                "new Experimental", "NewTECH" + (i+1), df.format(testDate.getTime()), file, i);
        uncheckCheckbox("_titrationRole_standard_Standard1");
        checkCheckbox("_titrationRole_qccontrol_Standard1");
        clickButton("Save and Finish");
    }

    protected void reimportFirstRun(int index, Calendar testDate, File file)
    {
        // test Luminex re-run import, check for identical values
        int i = index;
        goToTestAssayHome();
        // The 2nd run should already be selected, so we just need to click Re-import run
        clickButton("Re-import run");
        // verify that all old values from the first imported run are present
        assert(getFormElement(Locator.name("network")).equals("NETWORK1"));
        clickButton("Next");
        testDate.add(Calendar.DATE, 1);
        reimportLuminexRunPageTwo("Guide Set plate " + (i+1), isotype, conjugate, "", "", "Notebook" + (i+1),
                "Experimental", "TECH" + (i+1), df.format(testDate.getTime()), file.toString(), i);
        uncheckCheckbox("_titrationRole_standard_Standard1");
        checkCheckbox("_titrationRole_qccontrol_Standard1");
        clickButton("Save and Finish");
    }

    protected void reimportLuminexRunPageTwo(String name, String isotype, String conjugate, String stndCurveFitInput,
                                             String unkCurveFitInput, String notebookNo, String assayType, String expPerformer,
                                             String testDate, String file, int i)
    {
        // verify that all old values from the first imported run are present
        assert(getFormElement(Locator.name("name")).equals(name));
        setFormElement("name", name);
        assert(getFormElement(Locator.name("isotype")).equals(isotype));
        setFormElement("isotype", isotype);
        assert(getFormElement(Locator.name("conjugate")).equals(conjugate));
        setFormElement("conjugate", conjugate);
        assert(getFormElement(Locator.name("stndCurveFitInput")).equals(stndCurveFitInput));
        setFormElement("stndCurveFitInput", stndCurveFitInput);
        assert(getFormElement(Locator.name("unkCurveFitInput")).equals(unkCurveFitInput));
        setFormElement("unkCurveFitInput", unkCurveFitInput);
        uncheckCheckbox("curveFitLogTransform");
        assert(getFormElement(Locator.name("notebookNo")).equals(notebookNo));
        setFormElement("notebookNo", notebookNo);
        assert(getFormElement(Locator.name("assayType")).equals(assayType));
        setFormElement("assayType", assayType);
        assert(getFormElement(Locator.name("expPerformer")).equals(expPerformer));
        setFormElement("expPerformer", expPerformer);
        assert(getFormElement(Locator.name("testDate")).equals(testDate));
        setFormElement("testDate", testDate);
        //click(Locator.id("ext-gen4"));
        click(Locator.xpath("//a[contains(@class, 'labkey-file-add-icon-enabled')]"));
        setFormElement("__primaryFile__", file);
        clickButton("Next", 60000);
    }

    protected void assertLuminexLogInfoPresent()
    {
        waitForText("Finished assay upload");

        //Check for Analyte Properties
        assertTextPresentInThisOrder("----- Start Analyte Properties -----", "----- Stop Analyte Properties -----");
        assertTextPresent("Properties for GS Analyte (2)", "Properties for GS Analyte (1)");
        assertTextPresent("*LotNumber:", "*NegativeControl:", "*PositivityThreshold:");

        //Check for Well Role Properties
        assertTextPresentInThisOrder("----- Start Well Role Properties -----", "----- Stop Well Role Properties -----");
        assertTextPresent("Standard", "QC Control", "Unknown");

        //Check for Run Properties
        assertTextPresentInThisOrder("----- Start Run Properties -----", "----- End Run Properties -----");
        assertTextPresent("Uploaded Files", "Assay ID", "Isotype", "Conjugate", "Test Date", "Replaces Previous File", "Date file was modified",
                "Specimen Type", "Additive", "Derivative", "Subtract Blank Bead", "Calc of Standards", "Calc of Unknown",
                "Curve Fit Log", "Notebook Number", "Assay Type", "Experiment Performer", "Calculate Positivity",
                "Baseline Visit", "Positivity Fold Change");

        //Check for Batch Properties
        assertTextPresentInThisOrder("----- Start Batch Properties -----", "----- End Batch Properties -----");
        assertTextPresent("Participant Visit", "Target Study", "Species", "Lab ID", "Analysis", "Network", "Transform Script Version", "Ruminex Version");
    }

}
