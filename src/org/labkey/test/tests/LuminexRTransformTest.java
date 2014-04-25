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
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.MiniTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.io.File;

import static org.junit.Assert.assertEquals;

@Category({DailyA.class, MiniTest.class, Assays.class})
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
        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE_LABKEY), 0);
        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE_LAB), 1);

        // save changes to assay design
        clickButton("Save & Close");

        // upload the sample data file
        clickProject(TEST_ASSAY_PRJ_LUMINEX);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.name("name"), "r script transformed assayId");
        setFormElement(Locator.name("stndCurveFitInput"), "FI");
        setFormElement(Locator.name("unkCurveFitInput"), "FI-Bkgd-Blank");
        checkCheckbox(Locator.name("curveFitLogTransform"));
        setFormElement(Locator.name("__primaryFile__"), TEST_ASSAY_LUM_FILE4);
        clickButton("Next", 60000);
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

        // verify that the PDF of curves was generated
        Locator l = Locator.tagWithAttribute("img", "src", "/labkey/_images/sigmoidal_curve.png");
        click(l);
        assertElementPresent(Locator.linkWithText("WithBlankBead.Standard1_5PL.pdf"));
        assertElementPresent(Locator.linkWithText("WithBlankBead.Standard1_4PL.pdf"));
        assertElementPresent(Locator.linkWithText("WithBlankBead.Standard1_QC_Curves_4PL.pdf"));
        assertElementPresent(Locator.linkWithText("WithBlankBead.Standard1_QC_Curves_5PL.pdf"));

        // verify that the transform script and ruminex versions are as expected
        assertTextPresent(TEST_ASSAY_LUM + " Runs");
        DataRegionTable table = new DataRegionTable("Runs", this);
        assertEquals("Unexpected Transform Script Version number", "7.0.20140207", table.getDataAsText(0, "Transform Script Version"));
        assertEquals("Unexpected Lab Transform Script Version number", "1.0.20140228", table.getDataAsText(0, "Lab Transform Script Version"));
        assertEquals("Unexpected Ruminex Version number", "0.0.9", table.getDataAsText(0, "Ruminex Version"));

        // verify that the lot number value are as expected
        clickAndWait(Locator.linkWithText("r script transformed assayId"));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("Analyte/Properties/LotNumber");
        _customizeViewsHelper.applyCustomView();
        setFilter("Data", "Analyte/Properties/LotNumber", "Equals", TEST_ANALYTE_LOT_NUMBER);
        waitForElement(Locator.paginationText(1, 40, 40));
        clearFilter("Data", "Analyte/Properties/LotNumber");

        // verfiy that the calculated values were generated by the transform script as expected
        table = new DataRegionTable("Data", this);
        setFilter("Data", "fiBackgroundBlank", "Is Not Blank");
        waitForElement(Locator.paginationText(1, 40, 40));
        setFilter("Data", "Type", "Starts With", "X"); // filter to just the unknowns
        waitForElement(Locator.paginationText(1, 32, 32));
        // check values in the fi-bkgd-blank column
        for(int i = 0; i < RTRANS_FIBKGDBLANK_VALUES.length; i++)
        {
            assertEquals(RTRANS_FIBKGDBLANK_VALUES[i], table.getDataAsText(i, "FI-Bkgd-Blank"));
        }
        clearFilter("Data", "fiBackgroundBlank");
        setFilter("Data", "EstLogConc_5pl", "Is Not Blank");
        waitForElement(Locator.paginationText(1, 32, 32));
        // check values in the est log conc 5pl column
        for(int i = 0; i < RTRANS_ESTLOGCONC_VALUES_5PL.length; i++)
        {
            assertEquals(RTRANS_ESTLOGCONC_VALUES_5PL[i], table.getDataAsText(i, "Est Log Conc Rumi 5 PL"));
        }
        clearFilter("Data", "EstLogConc_5pl");
        setFilter("Data", "EstLogConc_4pl", "Is Not Blank");
        waitForElement(Locator.paginationText(1, 32, 32));
        // check values in the est log conc 4pl column
        for(int i = 0; i < RTRANS_ESTLOGCONC_VALUES_4PL.length; i++)
        {
            assertEquals(RTRANS_ESTLOGCONC_VALUES_4PL[i], table.getDataAsText(i, "Est Log Conc Rumi 4 PL"));
        }
        clearFilter("Data", "EstLogConc_4pl");
        clearFilter("Data", "Type");

        R_TRANSFORM_SET = true;
    }
}
