/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Luminex;
import org.labkey.test.categories.LuminexAll;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.io.File;

import static org.junit.Assert.assertEquals;

@Category({DailyA.class, LuminexAll.class, Assays.class, Luminex.class})
public class LuminexRTransformTest extends LuminexTest
{
    private static final String TEST_ANALYTE_LOT_NUMBER = "ABC 123";

    private static final String[] RTRANS_FIBKGDBLANK_VALUES = {"-50.5", "-70.0", "25031.5", "25584.5", "391.5", "336.5", "263.8", "290.8",
            "35.2", "35.2", "63.0", "71.0", "-34.0", "-33.0", "-29.8", "-19.8", "-639.8", "-640.2", "26430.8", "26556.2", "-216.2", "-204.2", "-158.5",
            "-208.0", "-4.0", "-4.0", "194.2", "198.8", "-261.2", "-265.2", "-211.5", "-213.0"};
    private static final String[] RTRANS_ESTLOGCONC_VALUES_5PL = {"-6.9", "-6.9", "4.3", "4.3", "0.4", "0.4", "-0.0", "-0.0", "-6.9", "-6.9",
            "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "4.2", "4.2", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9",
            "-6.9", "-0.6", "-0.6", "-6.9", "-6.9", "-6.9", "-6.9"};

    private static final String[] RTRANS_ESTLOGCONC_VALUES_4PL = {"-6.9", "-6.9", "5.0", "5.0", "0.4", "0.4", "0.1", "0.1", "-6.9", "-6.9",
            "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "5.5", "5.5", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9",
            "-0.8", "-0.8", "-6.9", "-6.9", "-6.9", "-6.9"};

    @Override
    protected void ensureConfigured()
    {
        setUseXarImport(true);
        super.ensureConfigured();
    }

    protected void runUITests()
    {
        runRTransformTest();
    }

    private boolean R_TRANSFORM_SET = false;
    protected void ensureRTransformPresent()
    {
        if(!R_TRANSFORM_SET)
            runRTransformTest();
    }

    //requires drc, Ruminex and xtable packages installed in R
    @LogMethod
    private void runRTransformTest()
    {
        log("Uploading Luminex run with a R transform script");

        // add the R transform script to the assay
        goToTestAssayHome();
        clickEditAssayDesign(false);
        addTransformScript(new File(TestFileUtils.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE_LABKEY), 0);
        addTransformScript(new File(TestFileUtils.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE_LAB), 1);
        clickButton("Save & Close");

        uploadRunWithoutRumiCalc();
        verifyPDFsGenerated(false);
        verifyScriptVersions();
        verifyLotNumber();
        verifyRumiCalculatedValues(false);

        reImportRunWithRumiCalc();
        verifyPDFsGenerated(true);
        verifyScriptVersions();
        verifyLotNumber();
        verifyRumiCalculatedValues(true);

        R_TRANSFORM_SET = true;
    }

    private void verifyRumiCalculatedValues(boolean hasRumiCalcData)
    {
        DataRegionTable table;
        table = new DataRegionTable("Data", this);
        table.setFilter("fiBackgroundBlank", "Is Not Blank", null);
        waitForElement(Locator.paginationText(1, 80, 80));
        table.setFilter("Type", "Equals", "C9"); // issue 20457
        assertEquals(4, table.getDataRowCount());
        for(int i = 0; i < table.getDataRowCount(); i++)
        {
            assertEquals(table.getDataAsText(i, "FI-Bkgd"), table.getDataAsText(i, "FI-Bkgd-Blank"));
        }
        table.clearFilter("Type");
        table.setFilter("Type", "Starts With", "X"); // filter to just the unknowns
        waitForElement(Locator.paginationText(1, 32, 32));
        // check values in the fi-bkgd-blank column
        for(int i = 0; i < RTRANS_FIBKGDBLANK_VALUES.length; i++)
        {
            assertEquals(RTRANS_FIBKGDBLANK_VALUES[i], table.getDataAsText(i, "FI-Bkgd-Blank"));
        }
        table.clearFilter("fiBackgroundBlank");

        table.setFilter("EstLogConc_5pl", "Is Not Blank", null);
        if (!hasRumiCalcData)
        {
            waitForText("No data to show.");
            assertEquals(0, table.getDataRowCount());
        }
        else
        {
            waitForElement(Locator.paginationText(1, 32, 32));
            // check values in the est log conc 5pl column
            for(int i = 0; i < RTRANS_ESTLOGCONC_VALUES_5PL.length; i++)
            {
                assertEquals(RTRANS_ESTLOGCONC_VALUES_5PL[i], table.getDataAsText(i, "Est Log Conc Rumi 5 PL"));
            }
        }
        table.clearFilter("EstLogConc_5pl");

        table.setFilter("EstLogConc_4pl", "Is Not Blank", null);
        if (!hasRumiCalcData)
        {
            waitForText("No data to show.");
            assertEquals(0, table.getDataRowCount());
        }
        else
        {
            waitForElement(Locator.paginationText(1, 32, 32));
            // check values in the est log conc 4pl column
            for(int i = 0; i < RTRANS_ESTLOGCONC_VALUES_4PL.length; i++)
            {
                assertEquals(RTRANS_ESTLOGCONC_VALUES_4PL[i], table.getDataAsText(i, "Est Log Conc Rumi 4 PL"));
            }
        }
        table.clearFilter("EstLogConc_4pl");

        table.clearFilter("Type");
    }

    private void verifyLotNumber()
    {
        clickAndWait(Locator.linkWithText("r script transformed assayId"));
        DataRegionTable table;
        table = new DataRegionTable("Data", this);
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("Analyte/Properties/LotNumber");
        _customizeViewsHelper.applyCustomView();
        table.setFilter("Analyte/Properties/LotNumber", "Equals", TEST_ANALYTE_LOT_NUMBER);
        waitForElement(Locator.paginationText(1, 40, 40));
        table.clearFilter("Analyte/Properties/LotNumber");
    }

    private void verifyScriptVersions()
    {
        assertTextPresent(TEST_ASSAY_LUM + " Runs");
        DataRegionTable table = new DataRegionTable("Runs", this);
        assertEquals("Unexpected Transform Script Version number", "9.0.20140716", table.getDataAsText(0, "Transform Script Version"));
        assertEquals("Unexpected Lab Transform Script Version number", "1.2.20140612", table.getDataAsText(0, "Lab Transform Script Version"));
        assertEquals("Unexpected Ruminex Version number", "0.0.9", table.getDataAsText(0, "Ruminex Version"));
    }

    private void verifyPDFsGenerated(boolean hasStandardPDFs)
    {
        click(Locator.tagWithAttribute("img", "src", "/labkey/_images/sigmoidal_curve.png"));
        assertElementPresent(Locator.linkContainingText(".Standard1_QC_Curves_4PL.pdf"));
        assertElementPresent(Locator.linkContainingText(".Standard1_QC_Curves_5PL.pdf"));
        if (hasStandardPDFs)
        {
            assertElementPresent(Locator.linkWithText("WithBlankBead.Standard1_5PL.pdf"));
            assertElementPresent(Locator.linkWithText("WithBlankBead.Standard1_4PL.pdf"));
        }
    }

    private void uploadRunWithoutRumiCalc()
    {
        clickProject(TEST_ASSAY_PRJ_LUMINEX);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));

        clickButton("Import Data");
        clickButton("Next");

        setFormElement(Locator.name("name"), "r script transformed assayId");
        checkCheckbox(Locator.name("subtBlankFromAll"));
        setFormElement(Locator.name("stndCurveFitInput"), "FI");
        setFormElement(Locator.name("unkCurveFitInput"), "FI-Bkgd-Blank");
        checkCheckbox(Locator.name("curveFitLogTransform"));
        checkCheckbox(Locator.name("skipRumiCalculation"));
        setFormElement(Locator.name("__primaryFile__"), TEST_ASSAY_LUM_FILE4);
        clickButton("Next", defaultWaitForPage * 2);

        // make sure the Standard checkboxes are checked
        checkCheckbox(Locator.name("_titrationRole_standard_Standard1"));
        checkCheckbox(Locator.name("titration_MyAnalyte (1)_Standard1"));
        checkCheckbox(Locator.name("titration_MyAnalyte (2)_Standard1"));
        checkCheckbox(Locator.name("titration_Blank (3)_Standard1"));
        // make sure that that QC Control checkbox is checked
        checkCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));
        // set LotNumber for the first analyte
        setFormElement(Locator.xpath("//input[@type='text' and contains(@name, '_LotNumber')][1]"), TEST_ANALYTE_LOT_NUMBER);
        clickButton("Save and Finish");
    }

    private void reImportRunWithRumiCalc()
    {
        clickProject(TEST_ASSAY_PRJ_LUMINEX);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));

        // all batch, run, analyte properties should be remembered from the first upload
        checkDataRegionCheckbox("Runs", 0);
        clickButton("Re-import run");
        clickButton("Next");
        uncheckCheckbox(Locator.name("skipRumiCalculation"));
        clickButton("Next", defaultWaitForPage * 2);
        clickButton("Save and Finish");
    }
}
