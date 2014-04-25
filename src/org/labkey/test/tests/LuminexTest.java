/*
 * Copyright (c) 2007-2014 LabKey Corporation
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
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.RReportHelper;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.labkey.test.util.ListHelper.ListColumnType;

import static org.junit.Assert.*;

public abstract class LuminexTest extends AbstractQCAssayTest
{
    RReportHelper _rReportHelper = new RReportHelper(this);

    private boolean _useXarImport = false;

    protected final static String TEST_ASSAY_PRJ_LUMINEX = "LuminexTest Project";            //project for luminex test

    protected static final String TEST_ASSAY_LUM =  "&TestAssayLuminex></% 1";// put back TRICKY_CHARACTERS_NO_QUOTES when issue 20061 is resolved
    protected static final String TEST_ASSAY_LUM_DESC = "Description for Luminex assay";

    protected static final String TEST_ASSAY_XAR_NAME = "TestLuminexAssay";
    protected final File TEST_ASSAY_XAR_FILE = new File(getLabKeyRoot() + "/sampledata/Luminex/" + TEST_ASSAY_XAR_NAME + ".xar");

    protected static final String TEST_ASSAY_LUM_SET_PROP_SPECIES = "testSpecies1";
    protected static final String TEST_ASSAY_LUM_RUN_NAME = "testRunName1";
    protected static final String TEST_ASSAY_LUM_SET_PROP_SPECIES2 = "testSpecies2";
    protected static final String TEST_ASSAY_LUM_RUN_NAME2 = "testRunName2";
    protected static final String TEST_ASSAY_LUM_RUN_NAME3 = "WithIndices.xls";
    protected static final String TEST_ANALYTE_LOT_NUMBER = "ABC 123";
    public static final String GUIDE_SET_5_COMMENT = "analyte 2 guide set run removed";
    protected final File TEST_ASSAY_LUM_FILE1 = new File(getLabKeyRoot() + "/sampledata/Luminex/10JAN07_plate_1.xls");
    protected final File TEST_ASSAY_LUM_FILE2 = new File(getLabKeyRoot() + "/sampledata/Luminex/pnLINCO20070302A.xlsx");
    protected final File TEST_ASSAY_LUM_FILE3 = new File(getLabKeyRoot() + "/sampledata/Luminex/WithIndices.xls");
    protected final File TEST_ASSAY_LUM_FILE4 = new File(getLabKeyRoot() + "/sampledata/Luminex/WithBlankBead.xls");
    protected final File TEST_ASSAY_LUM_FILE5 = new File(getLabKeyRoot() + "/sampledata/Luminex/Guide Set plate 1.xls");
    protected final File TEST_ASSAY_LUM_FILE6 = new File(getLabKeyRoot() + "/sampledata/Luminex/Guide Set plate 2.xls");
    protected final File TEST_ASSAY_LUM_FILE7 = new File(getLabKeyRoot() + "/sampledata/Luminex/Guide Set plate 3.xls");
    protected final File TEST_ASSAY_LUM_FILE8 = new File(getLabKeyRoot() + "/sampledata/Luminex/Guide Set plate 4.xls");
    protected final File TEST_ASSAY_LUM_FILE9 = new File(getLabKeyRoot() + "/sampledata/Luminex/Guide Set plate 5.xls");
    protected final File TEST_ASSAY_LUM_FILE10 = new File(getLabKeyRoot() + "/sampledata/Luminex/RawAndSummary.xlsx");
    protected final File TEST_ASSAY_LUM_FILE11 = new File(getLabKeyRoot() + "/sampledata/Luminex/PositivityWithBaseline.xls");
    protected final File TEST_ASSAY_LUM_FILE12 = new File(getLabKeyRoot() + "/sampledata/Luminex/PositivityWithoutBaseline.xls");
    protected final File TEST_ASSAY_LUM_FILE13 = new File(getLabKeyRoot() + "/sampledata/Luminex/PositivityThreshold.xls");

    protected final File TEST_ASSAY_MULTIPLE_STANDARDS_1 = new File(getLabKeyRoot() + "/sampledata/Luminex/plate 1_IgA-Biot (Standard2).xls");
    protected final File TEST_ASSAY_MULTIPLE_STANDARDS_2 = new File(getLabKeyRoot() + "/sampledata/Luminex/plate 2_IgA-Biot (Standard2).xls");
    protected final File TEST_ASSAY_MULTIPLE_STANDARDS_3 = new File(getLabKeyRoot() + "/sampledata/Luminex/plate 3_IgA-Biot (Standard1).xls");

    protected static final String RTRANSFORM_SCRIPT_FILE_LABKEY = "/resources/transformscripts/labkey_luminex_transform.R";
    protected static final String RTRANSFORM_SCRIPT_FILE_LAB = "/resources/transformscripts/tomaras_luminex_transform.R";
    private static final String[] RTRANS_FIBKGDBLANK_VALUES = {"-50.5", "-70.0", "25031.5", "25584.5", "391.5", "336.5", "263.8", "290.8",
            "35.2", "35.2", "63.0", "71.0", "-34.0", "-33.0", "-29.8", "-19.8", "-639.8", "-640.2", "26430.8", "26556.2", "-216.2", "-204.2", "-158.5",
            "-208.0", "-4.0", "-4.0", "194.2", "198.8", "-261.2", "-265.2", "-211.5", "-213.0"};
    private static final String[] RTRANS_ESTLOGCONC_VALUES_5PL = {"-6.9", "-6.9", "4.3", "4.3", "0.4", "0.4", "-0.0", "-0.0", "-6.9", "-6.9",
            "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "4.2", "4.2", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9",
            "-6.9", "-0.6", "-0.6", "-6.9", "-6.9", "-6.9", "-6.9"};

    private static final String[] RTRANS_ESTLOGCONC_VALUES_4PL = {"-6.9", "-6.9", "5.0", "5.0", "0.4", "0.4", "0.1", "0.1", "-6.9", "-6.9",
            "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "5.5", "5.5", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9",
            "-0.8", "-0.8", "-6.9", "-6.9", "-6.9", "-6.9"};

    public static final String ASSAY_ID_FIELD  = "name";
    public static final String ASSAY_DATA_FILE_LOCATION_MULTIPLE_FIELD = "__primaryFile__";

    public static final String DATA_TABLE_NAME = "Data";
    private static final String EXCLUDE_COMMENT_FIELD = "comment";
    public static final String EXCLUDE_SELECTED_BUTTON = "excludeselected";
    protected static final String MULTIPLE_CURVE_ASSAY_RUN_NAME = "multipleCurvesTestRun";
    protected static final String SAVE_CHANGES_BUTTON = "Save";

    private String EC50_RUN_NAME = "EC50";
    private String rum4 = "Four Parameter";
    private String rum5 = "Five Parameter";
    private String trapezoidal = "Trapezoidal";

    private String today = null;

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/luminex";
    }

    public void setUseXarImport(boolean useXarImport)
    {
        _useXarImport = useXarImport;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return TEST_ASSAY_PRJ_LUMINEX;
    }

    //potentially third "dirty" status
    protected enum  Configured {CONFIGURED, UNCONFIGURED}

    protected Configured configStatus = Configured.UNCONFIGURED;

    protected Configured getConfigStatus()
    {
        return configStatus;
    }

    protected void ensureConfigured()
    {
        if(getConfigStatus()!=Configured.CONFIGURED)
        {
            log("luminex assay not configured, configuring now");
            configure();
        }
        else
        {
            log("luminex assay already configured, skipping configuration step");
        }
    }

    @LogMethod
    protected void configure()
    {
        // setup a scripting engine to run a java transform script
        prepareProgrammaticQC();

        // fail fast if R is not configured
        _rReportHelper.ensureRConfig();

        //revert to the admin user
        revertToAdmin();

        log("Testing Luminex Assay Designer");
        //create a new test project
        _containerHelper.createProject(TEST_ASSAY_PRJ_LUMINEX, null);

        //setup a pipeline for it
        setupPipeline(TEST_ASSAY_PRJ_LUMINEX);

        //create a study within this project to which we will publish
        clickProject(TEST_ASSAY_PRJ_LUMINEX);
        addWebPart("Study Overview");
        clickButton("Create Study");
        clickButton("Create Study");
        clickProject(TEST_ASSAY_PRJ_LUMINEX);

        //add the Assay List web part so we can create a new luminex assay
        addWebPart("Assay List");

        if (_useXarImport)
        {
            // import the assay design from the XAR file
            _assayHelper.uploadXarFileAsAssayDesign(TEST_ASSAY_XAR_FILE, 1);
            // since we want to test special characters in the assay name, copy the assay design to rename
            goToManageAssays();
            clickAndWait(Locator.linkWithText(TEST_ASSAY_XAR_NAME));
            _extHelper.clickExtMenuButton(true, Locator.xpath("//a[text() = 'manage assay design']"), "copy assay design");
            clickButton("Copy to Current Folder", WAIT_FOR_PAGE);
            waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);
            setFormElement(Locator.id("AssayDesignerName"), TEST_ASSAY_LUM);
            setFormElement(Locator.id("AssayDesignerDescription"), TEST_ASSAY_LUM_DESC);
            saveAssay();
        }
        else
        {
            //create a new luminex assay
            clickButton("Manage Assays");
            clickButton("New Assay Design");

            checkCheckbox(Locator.radioButtonByNameAndValue("providerName", "Luminex"));
            clickButton("Next");

            waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

            log("Setting up Luminex assay");
            setFormElement(Locator.id("AssayDesignerName"), TEST_ASSAY_LUM);
            setFormElement(Locator.id("AssayDesignerDescription"), TEST_ASSAY_LUM_DESC);

            // add batch properties for transform and Ruminex version numbers
            _listHelper.addField("Batch Fields", 5, "Network", "Network", ListColumnType.String);
            _listHelper.addField("Batch Fields", 6, "TransformVersion", "Transform Script Version", ListColumnType.String);
            _listHelper.addField("Batch Fields", 7, "LabTransformVersion", "Lab Transform Script Version", ListColumnType.String);
            _listHelper.addField("Batch Fields", 8, "RuminexVersion", "Ruminex Version", ListColumnType.String);

            // add run properties for designation of which field to use for curve fit calc in transform
            _listHelper.addField("Run Fields", 8, "SubtBlankFromAll", "Subtract Blank Bead from All Wells", ListColumnType.Boolean);
            _listHelper.addField("Run Fields", 9, "StndCurveFitInput", "Input Var for Curve Fit Calc of Standards", ListColumnType.String);
            _listHelper.addField("Run Fields", 10, "UnkCurveFitInput", "Input Var for Curve Fit Calc of Unknowns", ListColumnType.String);
            _listHelper.addField("Run Fields", 11, "CurveFitLogTransform", "Curve Fit Log Transform", ListColumnType.Boolean);

            // add run properties for use with the Guide Set test
            _listHelper.addField("Run Fields", 12, "NotebookNo", "Notebook Number", ListColumnType.String);
            _listHelper.addField("Run Fields", 13, "AssayType", "Assay Type", ListColumnType.String);
            _listHelper.addField("Run Fields", 14, "ExpPerformer", "Experiment Performer", ListColumnType.String);

            // add run properties for use with Calculating Positivity
            _listHelper.addField("Run Fields", 15, "CalculatePositivity", "Calculate Positivity", ListColumnType.Boolean);
            _listHelper.addField("Run Fields", 16, "BaseVisit", "Baseline Visit", ListColumnType.Double);
            _listHelper.addField("Run Fields", 17, "PositivityFoldChange", "Positivity Fold Change", ListColumnType.Integer);

            // add analyte property for tracking lot number
            _listHelper.addField("Analyte Properties", 6, "LotNumber", "Lot Number", ListColumnType.String);
            _listHelper.addField("Analyte Properties", 7, "NegativeControl", "Negative Control", ListColumnType.Boolean);

            // add the data properties for the calculated columns
            _listHelper.addField("Data Fields", 0, "fiBackgroundBlank", "FI-Bkgd-Blank", ListColumnType.Double);
            _listHelper.addField("Data Fields", 1, "Standard", "Stnd for Calc", ListColumnType.String);
            _listHelper.addField("Data Fields", 2, "EstLogConc_5pl", "Est Log Conc Rumi 5 PL", ListColumnType.Double);
            _listHelper.addField("Data Fields", 3, "EstConc_5pl", "Est Conc Rumi 5 PL", ListColumnType.Double);
            _listHelper.addField("Data Fields", 4, "SE_5pl", "SE Rumi 5 PL", ListColumnType.Double);
            _listHelper.addField("Data Fields", 5, "EstLogConc_4pl", "Est Log Conc Rumi 4 PL", ListColumnType.Double);
            _listHelper.addField("Data Fields", 6, "EstConc_4pl", "Est Conc Rumi 4 PL", ListColumnType.Double);
            _listHelper.addField("Data Fields", 7, "SE_4pl", "SE Rumi 4 PL", ListColumnType.Double);
            _listHelper.addField("Data Fields", 8, "Slope_4pl", "Slope_4pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 9, "Lower_4pl", "Lower_4pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 10, "Upper_4pl", "Upper_4pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 11, "Inflection_4pl", "Inflection_4pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 12, "Slope_5pl", "Slope_5pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 13, "Lower_5pl", "Lower_5pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 14, "Upper_5pl", "Upper_5pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 15, "Inflection_5pl", "Inflection_5pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 16, "Asymmetry_5pl", "Asymmetry_5pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 17, "Positivity", "Positivity", ListColumnType.String);


            // set format to two decimal place for easier testing later
            setFormat("Data Fields", 0, "0.0");
            setFormat("Data Fields", 2, "0.0");
            setFormat("Data Fields", 3, "0.0");
            setFormat("Data Fields", 4, "0.0");
            setFormat("Data Fields", 5, "0.0");
            setFormat("Data Fields", 6, "0.0");
            setFormat("Data Fields", 7, "0.0");
            setFormat("Data Fields", 8, "0.0");
            setFormat("Data Fields", 9, "0.0");
            setFormat("Data Fields", 10, "0.0");
            setFormat("Data Fields", 11, "0.0");
            setFormat("Data Fields", 12, "0.0");
            setFormat("Data Fields", 13, "0.0");
            setFormat("Data Fields", 14, "0.0");
            setFormat("Data Fields", 15, "0.0");
            setFormat("Data Fields", 16, "0.0");

            saveAssay();

            // remove the SpecimenID field from the results grid to speed up the test
//            clickAndWait(Locator.linkWithText("Assay List"));
//            clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));
//            clickAndWait(Locator.linkWithText("view results"));
//            _customizeViewsHelper.openCustomizeViewPanel();
//            _customizeViewsHelper.removeCustomizeViewColumn("SpecimenID");
//            _customizeViewsHelper.saveDefaultView();
        }

        // Clear the success message by reopening the designer, in case downstream the test wants to do further changes
        // of the assay design and needs to be able to detect when the save is complete.
        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));
        _extHelper.clickExtMenuButton(true, Locator.xpath("//a[text() = 'manage assay design']"), "edit assay design");
        waitForElement(Locator.xpath("//textarea[@id='AssayDesignerDescription']"), WAIT_FOR_JAVASCRIPT);

        configStatus = Configured.CONFIGURED;
    }

    protected void saveAssay()
    {
        clickButton("Save", 0);
        waitForText("Save successful.", 20000);
    }


    @LogMethod
    protected void runEC50Test()
    {
        ensureRTransformPresent();
        createNewAssayRun(EC50_RUN_NAME);
        checkCheckbox(Locator.name("curveFitLogTransform"));
        uploadEC50Data();
        clickButton("Save and Finish", 2 * WAIT_FOR_PAGE);

        //add transform script
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "CurveFit");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        assertTextPresent("Four Parameter");

        waitForText("3.45399");
        
        checkEC50dataAndFailureFlag();
    }

    private void checkEC50dataAndFailureFlag()
    {
        // expect to already be viewing CurveFit query
        assertTextPresent("CurveFit");

        // quick check to see if we are using 32-bit or 64-bit R
        log("Checking R 32-bit vs 64-bit");
        pushLocation();
        clickMenuButton("Views", "Create", "R View");
        boolean is64bit = _rReportHelper.executeScript("print(.Machine$sizeof.pointer)", "[1] 8", true);
        _rReportHelper.clickSourceTab();
        _rReportHelper.saveReport("dummy");
        popLocation();
        waitForText("CurveFit");

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("TitrationId/Name");
        _customizeViewsHelper.applyCustomView();

        DataRegionTable table = new DataRegionTable("query", this, false);
        table.setFilter("TitrationId/Name", "Equals One Of (e.g. \"a;b;c\")", "Standard1;Standard2");

        List<String> analyte = table.getColumnDataAsText("Analyte");
        List<String> formula = table.getColumnDataAsText("Curve Type");
        List<String> ec50 = table.getColumnDataAsText("EC50");
        List<String> auc= table.getColumnDataAsText("AUC");
        List<String> inflectionPoint = table.getColumnDataAsText("Inflection");
        int rum5ec50count = 0;

        log("Write this");
        for(int i=0; i<formula.size(); i++)
        {
            if(formula.get(i).equals(rum4))
            {
                //ec50=populated=inflectionPoint
                assertEquals(ec50.get(i), inflectionPoint.get(i));
                //auc=unpopulated
                assertEquals(" ", auc.get(i));
            }
            else if(formula.get(i).equals(rum5))
            {
                // ec50 will be populated for well formed curves (i.e. not expected for every row, so we'll keep a count and check at the end of the loop)
                if (!ec50.get(i).equals(" ") && ec50.get(i).length() > 0)
                    rum5ec50count++;

                // auc should not be populated
                assertEquals(" ", auc.get(i));
            }
            else if(formula.get(i).equals(trapezoidal))
            {
                //ec50 should not be populated
                assertEquals(" ", ec50.get(i));
                //auc=populated (for all non-blank analytes)
                if (!analyte.get(i).startsWith("Blank"))
                    assertTrue( "AUC was unpopulated for row " + i, auc.get(i).length()>0);
            }
        }
        assertEquals("Unexpected number of Five Parameter EC50 values (expected 9 of 13).", 9, rum5ec50count);

        // check that the 5PL parameters are within the expected ranges (note: exact values can change based on R 32-bit vs R 64-bit)
        Double[] FiveParameterEC50mins = {32211.66, 44975.52, 110.24, 7826.89, 0.4199, 36465.56, 0.03962, 21075.08, 460.75};
        Double[] FiveParameterEC50maxs = {32211.67, 45012.09, 112.85, 7826.90, 0.4377, 36469.51, 0.03967, 21075.29, 480.26};
        table.setFilter("CurveType", "Equals", "Five Parameter");
        table.setFilter("EC50", "Is Not Blank", "");
        ec50 = table.getColumnDataAsText("EC50");
        assertEquals("Unexpected number of Five Parameter EC50 values (expected " + FiveParameterEC50maxs.length + ")", FiveParameterEC50maxs.length, ec50.size());
        for (int i = 0; i < ec50.size(); i++)
        {
            Double val = Double.parseDouble(ec50.get(i));
            Double min = FiveParameterEC50mins[i];
            Double max = FiveParameterEC50maxs[i];
            assertTrue("Unexpected 5PL EC50 value for " + table.getDataAsText(i, "Analyte"), min <= val && val <= max);
        }
        table.clearFilter("EC50");
        table.clearFilter("CurveType");

        // expect to already be viewing CurveFit query
        assertTextPresent("CurveFit");

        table = new DataRegionTable("query", this, false);
        table.setFilter("FailureFlag", "Equals", "true");

        // expect one 4PL curve fit failure (for Standard1 - ENV6 (97))
        table.setFilter("CurveType", "Equals", "Four Parameter");
        assertEquals("Expected one Four Parameter curve fit failure flag", 1, table.getDataRowCount());
        List<String> values = table.getColumnDataAsText("Analyte");
        assertTrue("Unexpected analyte for Four Parameter curve fit failure", values.size() == 1 && values.get(0).equals("ENV6 (97)"));
        table.clearFilter("CurveType");

        // expect four 5PL curve fit failures
        table.setFilter("CurveType", "Equals", "Five Parameter");
        assertEquals("Unexpected number of Five Parameter curve fit failure flags", 4, table.getDataRowCount());
        table.clearFilter("CurveType");

        table.clearFilter("FailureFlag");
    }

    /**
     * test of well exclusion- the ability to exclude certain wells or analytes and add ac oment as to why
     * preconditions: LUMINEX project and assay list exist.  Having the Multiple Curve data will speed up execution
     * but is not required
     * postconditions:  multiple curve data will be present, certain wells will be marked excluded
     */
    @LogMethod
    protected void runWellExclusionTest()
    {
         ensureMultipleCurveDataPresent();

         clickAndWait(Locator.linkContainingText(MULTIPLE_CURVE_ASSAY_RUN_NAME));

        //ensure multiple curve data present
        //there was a bug (never filed) that showed up with multiple curve data, so best to use that.

        String[] analytes = getListOfAnalytesMultipleCurveData();

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("ExclusionComment");
        _customizeViewsHelper.applyCustomView();

        //"all" excludes all
        String excludeAllWellName = "E1";
        excludeAllAnalytesForSingleWellTest(excludeAllWellName);

        String excludeOneWellName = "E1";
        excludeOneAnalyteForSingleWellTest(excludeOneWellName, analytes[0]);

        //excluding for one well excludes for duplicate wells
        excludeAnalyteForAllWellsTest(analytes[1]);

        // Check out the exclusion report
        clickAndWait(Locator.linkWithText("view excluded data"));
        assertTextPresent("Changed for all analytes", "exclude single analyte for single well", "ENV7 (93)", "ENV6 (97)");
        assertTextPresent("multipleCurvesTestRun", 2);
    }

    private void excludeOneAnalyteForSingleWellTest(String wellName, String excludedAnalyte)
    {
        waitForText("Well Role");
        clickExclusionMenuIconForWell(wellName);

        String exclusionComment = "exclude single analyte for single well";
        setFormElement(EXCLUDE_COMMENT_FIELD, exclusionComment);
        clickRadioButtonById(EXCLUDE_SELECTED_BUTTON);
        clickExcludeAnalyteCheckBox(excludedAnalyte);
        clickButton(SAVE_CHANGES_BUTTON, 2 * defaultWaitForPage);

        excludeForSingleWellVerify("Excluded for replicate group: " + exclusionComment, new HashSet<>((Arrays.asList(excludedAnalyte))));
    }

    /**
     * verify that a user can exclude every analyte for a single well, and that this
     * successfully applies to both the original well and its duplicates
     *
     * preconditions:  at run screen, wellName exists
     * postconditions: no change (exclusion is removed at end of test)
     * @param wellName name of well to excluse
     */
    private void excludeAllAnalytesForSingleWellTest(String wellName)
    {
        clickExclusionMenuIconForWell(wellName);

        String comment = "exclude all for single well";
        setFormElement(Locator.name(EXCLUDE_COMMENT_FIELD), comment);
        clickButton(SAVE_CHANGES_BUTTON, 2 * defaultWaitForPage);

        excludeForSingleWellVerify("Excluded for replicate group: " + comment, new HashSet<>(Arrays.asList(getListOfAnalytesMultipleCurveData())));

        //remove exclusions to leave in clean state
        clickExclusionMenuIconForWell(wellName);
        click(Locator.radioButtonById("excludeselected"));
        clickButton(SAVE_CHANGES_BUTTON, 0);
        _extHelper.waitForExtDialog("Warning");
        clickButton("Yes", 2 * defaultWaitForPage);
    }

    /**
     * go through every well.  If they match the hardcoded well, description, and type values, and one of the analyte values given
     * verify that the row has the expected comment
     *
     * @param expectedComment
     * @param analytes
     */
    private void excludeForSingleWellVerify(String expectedComment, Set<String> analytes)
    {
        for (String analyte : analytes)
        {
            setFilter("Data", "Analyte", "Equals", analyte);

            List<List<String>> vals = getColumnValues(DATA_TABLE_NAME, "Well", "Description", "Type", "Exclusion Comment", "Analyte");
            List<String> wells = vals.get(0);
            List<String> descriptions = vals.get(1);
            List<String> types = vals.get(2);
            List<String> comments = vals.get(3);
            List<String> analytesPresent = vals.get(4);

            String well;
            String description;
            String type;
            String comment;

            for(int i=0; i<wells.size(); i++)
            {
                well = wells.get(i);
                log("well: " + well);
                description= descriptions.get(i);
                log("description: " + description);
                type = types.get(i);
                log("type: " + type);
                comment = comments.get(i);
                log("Comment: "+ comment);
                String analyteVal = analytesPresent.get(i);
                log("Analyte: " + analyteVal);

                if(matchesWell(description, type, well) && analytes.contains(analyteVal))
                {
                    assertEquals(expectedComment,comment);
                }

                if(expectedComment.equals(comment))
                {
                    assertTrue(matchesWell(description, type, well));
                    assertTrue(analytes.contains(analyteVal));
                }
            }
        }
    }

    //verifies if description, type, and well match the hardcoded values
    private boolean matchesWell(String description, String type, String well)
    {
        return excludedWellDescription.equals(description) &&
                excludedWellType.equals(type) &&
                excludedWells.contains(well);
    }

    //currently hardcoded for E1
    private String[] getReplicateWells()
    {
        return new String[] {"E1", "F1"};
    }


    /**
     * verify a user can exclude a single analyte for all wells
     * preconditions:  multiple curve data imported, on assay run page
     * post conditions: specified analyte excluded from all wells, with comment "Changed for all analytes"
     * @param analyte
     */
    private void excludeAnalyteForAllWellsTest(String analyte)
    {
        String comment ="Changed for all analytes";
        excludeAnalyteForRun(analyte, true, comment);

        String exclusionPrefix = "Excluded for analyte: ";
        Map<String, Set<String>> analyteToExclusion = new HashMap<>();
        Set<String> set = new HashSet<>();
        set.add(exclusionPrefix + comment);
        analyteToExclusion.put(analyte, set);

        analyteToExclusion = createExclusionMap(set, analyte);

        compareColumnValuesAgainstExpected("Analyte", "Exclusion Comment", analyteToExclusion);
    }

    public void excludeAnalyteForRun(String analyte, boolean exclude, String comment)
    {
        clickButtonContainingText("Exclude Analytes", 0);
        _extHelper.waitForExtDialog("Exclude Analytes from Analysis");
        if (!exclude)
            waitForText("Uncheck analytes to remove exclusions");

        clickExcludeAnalyteCheckBox(analyte);
        setFormElement(Locator.id(EXCLUDE_COMMENT_FIELD), comment);
        waitForElement(Locator.xpath("//table[@id='saveBtn' and not(contains(@class, 'disabled'))]"), WAIT_FOR_JAVASCRIPT);

        if (!exclude)
        {
            clickButton(SAVE_CHANGES_BUTTON, 0);
            _extHelper.waitForExtDialog("Warning");
            _extHelper.clickExtButton("Warning", "Yes", 2 * defaultWaitForPage);
        }
        else
        {
            clickButton(SAVE_CHANGES_BUTTON, 2 * defaultWaitForPage);
        }
    }

    /**
     * return a map that, for each key, has value value
     * @param value
     * @param key
     * @return
     */
    private Map<String, Set<String>> createExclusionMap(Set<String> value, String... key)
    {
        Map<String, Set<String>> m  = new HashMap<>();

        for(String k: key)
        {
            m.put(k, value);
        }

        return m;
    }

    /**
     * click on the exclusion icon associated with the particular well
     * preconditions:  at Test Result page
     * postconditions: at Test Result Page with exclude Replicate Group From Analysis window up
     * @param wellName
     */
    private void clickExclusionMenuIconForWell(String wellName)
    {
        waitAndClick(Locator.id(getLinkIDFromWellName(wellName)));
        _extHelper.waitForExtDialog("Exclude Replicate Group from Analysis");
        waitForElement(Locator.xpath("//table[@id='saveBtn' and not(contains(@class, 'disabled'))]"), WAIT_FOR_JAVASCRIPT);
    }


    private String getLinkIDFromWellName(String wellName)
    {
        return "__changeExclusions__" + wellName;
    }



    private void clickExcludeAnalyteCheckBox(String analyte)
    {
        Locator l = ExtHelper.locateGridRowCheckbox(analyte);
        waitAndClick(l);
    }

    private String excludedWellDescription = "Sample 2";
    private String excludedWellType = "X25";
    private Set<String> excludedWells = new HashSet<>(Arrays.asList("E1", "F1"));

    private String[] getListOfAnalytesMultipleCurveData()
    {
        //TODO:  make this a dynamic list, acquired from the current data set, rather than hardcoded
        return new String[] {"ENV6 (97)", "ENV7 (93)", "ENV4 (26)",
                        "ENV5 (58)", "Blank (53)"};
    }



    /**several tests use this data.  Rather that clean and import for each
     * or take an unnecessary dependency of one to the other, this function
     * checks if the data is already present and, if it is not, adds it
     * preconditions:  Project TEST_ASSAY_PRJ_LUMINEX with Assay  TEST_ASSAY_LUM exists
     * postconditions:  assay run
     */
    protected void ensureMultipleCurveDataPresent()
    {
        goToTestRunList();

        if(!isTextPresent(MULTIPLE_CURVE_ASSAY_RUN_NAME)) //right now this is a good enough check.  May have to be
                                                    // more rigorous if tests start substantially altering data
        {
            log("multiple curve data not present, adding now");
            startCreateMultipleCurveAssayRun();
            clickButton("Save and Finish");
        }
    }

    /**
     * Goes to test run list for the common list used by all the tests
     */
    private void goToTestRunList()
    {
        goToHome();
        clickProject(TEST_ASSAY_PRJ_LUMINEX);
        clickAndWait(Locator.linkContainingText(TEST_ASSAY_LUM));
    }

    private String startCreateMultipleCurveAssayRun()
    {
        log("Creating test run with multiple standard curves");
        String name = MULTIPLE_CURVE_ASSAY_RUN_NAME;

        createNewAssayRun(name);

        uploadMultipleCurveData();

        return name;

    }
    /**
     * Test our ability to upload multiple files and set multiple standards
     *
     */
    @LogMethod
    protected void runMultipleCurveTest()
    {
        String name = startCreateMultipleCurveAssayRun();

        String[] standardsNames = {"Standard1", "Standard2"};
        checkStandardsCheckBoxesExist(standardsNames);

        String[] possibleAnalytes = getListOfAnalytesMultipleCurveData();
        String[] possibleStandards = new String[] {"Standard2", "Standard1"};

        Map<String, Set<String>> analytesAndStandardsConfig = generateAnalytesAndStandardsConfig(possibleAnalytes, possibleStandards);
        configureStandardsForAnalytes(analytesAndStandardsConfig, possibleStandards);



        clickButton("Save and Finish", 2*WAIT_FOR_PAGE);
        clickAndWait(Locator.linkWithText(name));

        //edit view to show Analyte Standard
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("Analyte/Standard");
        _customizeViewsHelper.addCustomizeViewColumn("Analyte/StdCurve");
        _customizeViewsHelper.addCustomizeViewColumn("Analyte/FitProb");
        _customizeViewsHelper.addCustomizeViewColumn("Analyte/ResVar");
        _customizeViewsHelper.applyCustomView();

        // We're OK with grabbing the footer curve fit from any of the files, under normal usage they should all share
        // the same curve fits
        assertTrue("BioPlex curve fit for ENV6 (97) in plate 1, 2, or 3",
                isTextPresent("FI = 0.465914 + (1.5417E+006 - 0.465914) / ((1 + (Conc / 122.733)^-0.173373))^7.64039") ||
                isTextPresent("FI = 0.582906 + (167.081 - 0.582906) / ((1 + (Conc / 0.531813)^-5.30023))^0.1"));
        assertTrue("BioPlex FitProb for ENV6 (97) in plate 1, 2, or 3", isTextPresent("0.9667") || isTextPresent("0.4790"));
        assertTrue("BioPlex ResVar for ENV6 (97) in plate 1, 2, 3", isTextPresent("0.1895") || isTextPresent("0.8266"));

        compareColumnValuesAgainstExpected("Analyte", "Standard", analytesAndStandardsConfig);

        // Go to the schema browser to check out the parsed curve fits
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "CurveFit");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));

        // We're OK with grabbing the footer curve fit from any of the files, under normal usage they should all share
        // the same curve fits
        assertTrue("BioPlex curve fit parameter for ENV6 (97) in plate 1, 2, or 3", isTextPresent("0.465914") || isTextPresent("0.582906"));
        assertTrue("BioPlex curve fit parameter for ENV6 (97) in plate 1, 2, or 3", isTextPresent("7.64039") || isTextPresent("0.1"));
    }

    private void compareColumnValuesAgainstExpected(String column1, String column2, Map<String, Set<String>> column1toColumn2)
    {
        Set<String> set = new HashSet<>();
        set.add(column2);
        column1toColumn2.put(column1, set); //column headers

        List<List<String>> columnVals = getColumnValues(DATA_TABLE_NAME, column1, column2);

        assertStandardsMatchExpected(columnVals, column1toColumn2);
    }

    /**
     *
     * @param columnVals two lists of equal length, with corresponding names of analytes and the standards applied to them
     * @param col1to2Map map of analyte names to the standards that should be applied to them.
     */
    private void assertStandardsMatchExpected( List<List<String>> columnVals, Map<String, Set<String>> col1to2Map)
    {
        String column1Val;
        String column2Val;
        while(columnVals.get(0).size()>0)
        {
            column1Val = columnVals.get(0).remove(0);
            column2Val = columnVals.get(1).remove(0);
            assertStandardsMatchExpected(column1Val, column2Val, col1to2Map);
        }
    }

    /**
     *
     * @param column1Val name of analyte
     * @param column2Val standard applied to analyte on server
     * @param colum1toColumn2Map map of all analytes to the appropriate standards
     */
    private void assertStandardsMatchExpected(String column1Val, String column2Val, Map<String, Set<String>> colum1toColumn2Map)
    {
//        if(analyte.equals("Analyte"))//header
//            assertEquals(actualStandards, "Exclusion Comment");
//        else
//        {
            String[] splitCol2Val = column2Val.split(",");
            Set<String> expectedCol2Vals = colum1toColumn2Map.get(column1Val);
            log("Column1: " + column1Val);
            log("Expected Column2: " + expectedCol2Vals);
            log("Column2: " + column2Val);
            if(expectedCol2Vals!=null)
            {
                assertEquals(splitCol2Val.length, expectedCol2Vals.size());

                for(String s: splitCol2Val)
                {
                    s = s.trim();
                    assertTrue("Expected " + expectedCol2Vals + " to contain" + s, expectedCol2Vals.contains(s));
                }
            }
//        }
    }

    /**
     * using the list of analytes and standards, select one or more standards for each analyte.  Expects at least three
     * analytes and exactly two standards, and returns two analytes using different standards and the rest using both.
     * This is based on the test data we're currently using, can be changed to accomodate future changes
     *
     * preconditions: none, does not interact with server
     *
     * @param possibleAnalytes list of possible analytes
     * @param possibleStandards list of possible standards
     * @return map.  Key is the name of the analyte, value is a set of standards to be used for that analyte
     */
    private Map<String,Set<String>> generateAnalytesAndStandardsConfig(String[] possibleAnalytes, String[] possibleStandards)
    {
        Map<String, Set<String>> analytesAndStandardsConfig =  new HashMap<>();


        //based on the assumption that there are five analytes and two possible standards:  update this if you need to test for more
        Set<String> firstStandard = new HashSet<>(); firstStandard.add(possibleStandards[0]);
        Set<String> secondStandard = new HashSet<>(); secondStandard.add(possibleStandards[1]);
        Set<String> bothStandard = new HashSet<>();
        bothStandard.add(possibleStandards[0]);
        bothStandard.add(possibleStandards[1]);

        analytesAndStandardsConfig.put(possibleAnalytes[0], bothStandard);
        analytesAndStandardsConfig.put(possibleAnalytes[1], firstStandard);
        analytesAndStandardsConfig.put(possibleAnalytes[2], firstStandard);
        analytesAndStandardsConfig.put(possibleAnalytes[3], secondStandard);
        analytesAndStandardsConfig.put(possibleAnalytes[4], secondStandard);

        return analytesAndStandardsConfig;
    }

    /**
     * check or uncheck the checkbox for the given standard and analyte
     *
     * @param analyte
     * @param standard
     * @param checked
     */
    protected void checkAnalyteAndStandardCheckBox(String analyte, String standard, boolean checked)
    {
        String checkboxName = "titration_" + analyte + "_" + standard;
        if(checked)
            checkCheckbox(checkboxName);
        else
            uncheckCheckbox(checkboxName);
    }


    /**
     * based on the instructions endcoded in the map, select the specified standards for each analyte
     *
     * preconditions:  on multiple curve data page.  analytes and standards must exist
     * postconditions: given check boxes checked and unchecked
     *
     * @param analytesAndTheirStandards map, where the keys are the analyte names and the values are sets of standard names,
     *      corresponding to the standards that should be used for the analyte.
     * @param standardsList list of all possible standards.  Important so that we know which boxes to uncheck when
     *      configuring which standard dto use
     */
    private void configureStandardsForAnalytes(Map<String, Set<String>> analytesAndTheirStandards, String[] standardsList)
    {
        Set<String> analytes = analytesAndTheirStandards.keySet();

        for(String analyte : analytes)
        {
            Set<String> analyteStandards = analytesAndTheirStandards.get(analyte);
            for(String standard: standardsList)
            {
                if(analyteStandards.contains(standard))
                    checkAnalyteAndStandardCheckBox(analyte, standard, true);
                else
                    checkAnalyteAndStandardCheckBox(analyte, standard, false);
            }
        }
    }

    /**
     * Verify that the "set this  as standard" checkboxes exist and can be checked for the given standard names
     * preconditions:  at analyte properties page
     * postconditions:  unchanged
     * @param standardsNames
     */
    private void checkStandardsCheckBoxesExist(String[] standardsNames)
    {
        for(int i=0; i<standardsNames.length; i++)
        {
            String s = standardsNames[i];
            Locator l = Locator.checkboxByName("_titrationRole_standard_"+s);
            checkCheckbox(l);
            assertChecked(l);
        }
    }

    /**
     * upload the three files used for the multiple curve data test
     * preconditions:  at assay run data import page
     * postconditions: at data import: analyte properties page
     */
    private void uploadMultipleCurveData()
    {
        addFilesToAssayRun(TEST_ASSAY_MULTIPLE_STANDARDS_1, TEST_ASSAY_MULTIPLE_STANDARDS_2, TEST_ASSAY_MULTIPLE_STANDARDS_3);
        clickButton("Next");
    }

    private void uploadEC50Data()
    {
        uploadMultipleCurveData();
    }

    protected void addFilesToAssayRun(File firstFile, File... additionalFiles)
    {
        setFormElement(Locator.name(ASSAY_DATA_FILE_LOCATION_MULTIPLE_FIELD), firstFile);

        int index = 1;
        for (File additionalFile : additionalFiles)
        {
            sleep(500);
            click(Locator.xpath("//a[contains(@class, 'labkey-file-add-icon-enabled')]"));

            String fieldName = ASSAY_DATA_FILE_LOCATION_MULTIPLE_FIELD + (index++);
            setFormElement(Locator.name(fieldName), additionalFile);
        }
    }

    /**
     * create new assay run with name name
     * preconditions:  can see Project Folder, assay already exists
     * postconditions: at data import screen for new test run
     * @param name name to give new assay run
     */
    protected void createNewAssayRun(String name)
    {
        goToTestRunList();
        clickButtonContainingText("Import Data");
        checkCheckbox(Locator.radioButtonByNameAndValue("participantVisitResolver", "SampleInfo"));
        clickButtonContainingText("Next");
        setFormElement(Locator.name(ASSAY_ID_FIELD), name);
    }



    /**
     * Cleanup entry point.
     * @param afterTest
     */
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        revertToAdmin();
        deleteProject(getProjectName(), afterTest);

        try{deleteEngine();}
        catch(Throwable T) {/* ignore */}

        deleteDir(getTestTempDir());
    } //doCleanup()

    //helper function to go to test assay home from anywhere the project link is visible
    protected void goToTestAssayHome()
    {
        if (!isTextPresent(TEST_ASSAY_LUM + " Runs"))
        {
            clickProject(TEST_ASSAY_PRJ_LUMINEX);
            clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));
        }
    }

    protected boolean R_TRANSFORM_SET = false;
    protected void ensureRTransformPresent()
    {
        if(!R_TRANSFORM_SET)
            runRTransformTest();
    }

    //requires drc, Ruminex and xtable packages installed in R
    @LogMethod
    protected void runRTransformTest()
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

    private void setFormat(String where, int index, String formatStr)
    {
        String prefix = getPropertyXPath(where);
        _listHelper.clickRow(prefix, index);
        click(Locator.xpath(prefix + "//span[contains(@class,'x-tab-strip-text') and text()='Format']"));
        setFormElement("propertyFormat", formatStr);
    }

    protected String isotype = "IgG ></% 1";// put back TRICKY_CHARACTERS_NO_QUOTES when issue 20061 is resolved
    protected String conjugate = "PE ></% 1";// put back TRICKY_CHARACTERS_NO_QUOTES when issue 20061 is resolved

    protected DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    protected void importLuminexRun()
    {

    }

    protected void importLuminexRunPageTwo(String name, String isotype, String conjugate, String stndCurveFitInput,
                                           String unkCurveFitInput, String notebookNo, String assayType, String expPerformer,
                                           String testDate, File file, int i)
    {
        importLuminexRunPageTwo(name, isotype, conjugate, stndCurveFitInput, unkCurveFitInput, notebookNo, assayType, expPerformer, testDate, file, i, false);
    }

    protected void importLuminexRunPageTwo(String name, String isotype, String conjugate, String stndCurveFitInput,
                                           String unkCurveFitInput, String notebookNo, String assayType, String expPerformer,
                                           String testDate, File file, int i, boolean expectDuplicateFile)
    {
            setFormElement(Locator.name("name"), name);
            setFormElement(Locator.name("isotype"), isotype);
            setFormElement(Locator.name("conjugate"), conjugate);
            setFormElement(Locator.name("stndCurveFitInput"), stndCurveFitInput);
            setFormElement(Locator.name("unkCurveFitInput"), unkCurveFitInput);
            uncheckCheckbox(Locator.name("curveFitLogTransform"));
            setFormElement(Locator.name("notebookNo"), notebookNo);
            setFormElement(Locator.name("assayType"), assayType);
            setFormElement(Locator.name("expPerformer"), expPerformer);
            setFormElement(Locator.name("testDate"), testDate);
            setFormElement(Locator.name("__primaryFile__"), file);

            if (expectDuplicateFile)
                waitForText("A file with name '" + file.getName() + "' already exists");

            clickButton("Next", 60000);
    }
    //requires drc, Ruminex, rlabkey and xtable packages installed in R
    @LogMethod
    protected void runGuideSetTest()
    {
        log("Uploading Luminex run with a R transform script for Guide Set test");
        today = df.format(Calendar.getInstance().getTime());

        File[] files = {TEST_ASSAY_LUM_FILE5, TEST_ASSAY_LUM_FILE6, TEST_ASSAY_LUM_FILE7, TEST_ASSAY_LUM_FILE8, TEST_ASSAY_LUM_FILE9};
        String[] analytes = {"GS Analyte (1)", "GS Analyte (2)"};

        // add the R transform script to the assay
        goToTestAssayHome();
        clickEditAssayDesign(false);
        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE_LABKEY), 0);
        _listHelper.addField("Batch Fields", 9, "CustomProtocol", "Protocol", ListColumnType.String);
        // save changes to assay design
        clickButton("Save & Close");

        // setup the testDate variable
        Calendar testDate = Calendar.getInstance();
        testDate.add(Calendar.DATE, -files.length);

        // upload the first set of files (2 runs)
        for (int i = 0; i < 2; i++)
        {
            goToTestAssayHome();
            clickButton("Import Data");
            setFormElement(Locator.name("network"), "NETWORK" + (i + 1));
            setFormElement(Locator.name("customProtocol"), "PROTOCOL" + (i + 1));
            clickButton("Next");

            testDate.add(Calendar.DATE, 1);
            importLuminexRunPageTwo("Guide Set plate " + (i+1), isotype, conjugate, "", "", "Notebook" + (i+1),
                        "Experimental", "TECH" + (i+1), df.format(testDate.getTime()), files[i], i);
            uncheckCheckbox(Locator.name("_titrationRole_standard_Standard1"));
            checkCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));
            clickButton("Save and Finish");

            verifyRunFileAssociations(i+1);
        }

        //verify that the uploaded runs do not have associated guide sets
        verifyGuideSetsNotApplied();

        //create initial guide sets for the 2 analytes
        goToLeveyJenningsGraphPage("Standard1");
        createInitialGuideSets();

        // check guide set IDs and make sure appropriate runs are associated to created guide sets
        Map<String, Integer> guideSetIds = getGuideSetIdMap();
        verifyGuideSetsApplied(guideSetIds, analytes, 2);

        //nav trail check
        assertElementPresent(Locator.id("navTrailAncestors").append("/a").withText("assay.Luminex." + TEST_ASSAY_LUM + " Schema"));

        // verify the guide set threshold values for the first set of runs
        int[] rowCounts = {2, 2};
        String[] ec504plAverages = {"179.78", "43426.10"};
        String[] ec504plStdDevs = {"22.21", "794.95"};
        verifyGuideSetThresholds(guideSetIds, analytes, rowCounts, ec504plAverages, ec504plStdDevs, "Four Parameter", "EC50Average", "EC50Std Dev");
        String[] aucAverages = {"8701.38", "80851.83"};
        String[] aucStdDevs = {"466.81", "6523.08"};
        verifyGuideSetThresholds(guideSetIds, analytes, rowCounts, aucAverages, aucStdDevs, "Trapezoidal", "AUCAverage", "AUCStd Dev");

        // upload the final set of runs (3 runs)
        for (int i = 2; i < files.length; i++)
        {
            goToTestAssayHome();
            clickButton("Import Data");
            setFormElement(Locator.name("network"), "NETWORK" + (i + 1));
            setFormElement(Locator.name("customProtocol"), "PROTOCOL" + (i + 1));
            clickButton("Next");

            importLuminexRunPageTwo("Guide Set plate " + (i+1), isotype, conjugate, "", "", "Notebook" + (i+1),
                        "Experimental", "TECH" + (i+1), df.format(testDate.getTime()), files[i], i);
            uncheckCheckbox(Locator.name("_titrationRole_standard_Standard1"));
            checkCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));
            clickButton("Save and Finish");

            verifyRunFileAssociations(i+1);
        }

        // verify that the newly uploaded runs got the correct guide set applied to them
        verifyGuideSetsApplied(guideSetIds, analytes, 5);

        //verify Levey-Jennings report R plots are displayed without errors
        verifyLeveyJenningsRplots();

        verifyQCFlags();
        verifyQCAnalysis();

        verifyExcludingRuns(guideSetIds, analytes);

        // test the start and end date filter for the report
        goToLeveyJenningsGraphPage("Standard1");
        applyStartAndEndDateFilter();

        // test the network and customProtocol filters for the report
        goToLeveyJenningsGraphPage("Standard1");
        applyNetworkProtocolFilter();

        excludableWellsWithTransformTest();
        applyLogYAxisScale();
        guideSetApiTest();
        verifyQCFlagUpdatesAfterWellChange();
        verifyLeveyJenningsPermissions();
        verifyHighlightUpdatesAfterQCFlagChange();
    }

    @LogMethod
    protected void importRunForTestLuminexConfig(File file, Calendar testDate, int i)
    {
        goToTestAssayHome();
        clickButton("Import Data");
        setFormElement(Locator.name("network"), "NETWORK" + (i + 1));
        clickButton("Next");

        testDate.add(Calendar.DATE, 1);
        importLuminexRunPageTwo("Guide Set plate " + (i+1), isotype, conjugate, "", "", "Notebook" + (i+1),
                    "Experimental", "TECH" + (i+1), df.format(testDate.getTime()), file, i);
        uncheckCheckbox(Locator.name("_titrationRole_standard_Standard1"));
        checkCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));
        clickButton("Save and Finish");
    }

    @LogMethod
    private void verifyHighlightUpdatesAfterQCFlagChange()
    {
        goToTestRunList();
        clickAndWait(Locator.linkWithText("Guide Set plate 4"));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        String[] newColumns = {"AnalyteTitration/MaxFIQCFlagsEnabled", "AnalyteTitration/MaxFI",
            "AnalyteTitration/Four ParameterCurveFit/EC50", "AnalyteTitration/Four ParameterCurveFit/AUC",
            "AnalyteTitration/Four ParameterCurveFit/EC50QCFlagsEnabled",
            "AnalyteTitration/Four ParameterCurveFit/AUCQCFlagsEnabled",
            "AnalyteTitration/Five ParameterCurveFit/EC50", "AnalyteTitration/Five ParameterCurveFit/AUC",
            "AnalyteTitration/Five ParameterCurveFit/EC50QCFlagsEnabled",
            "AnalyteTitration/Five ParameterCurveFit/AUCQCFlagsEnabled"};
        for(String column : newColumns)
        {
            _customizeViewsHelper.addCustomizeViewColumn(column);
        }
        _customizeViewsHelper.saveCustomView();

        String expectedHMFI=  "9173.8";
        String expectedEC50 = "36676.656";

        assertElementPresent(Locator.xpath("//span[contains(@style, 'red') and text()=" + expectedHMFI + "]"));

        clickAndWait(Locator.linkContainingText("view runs"));
        enableDisableQCFlags("Guide Set plate 4", "AUC", "HMFI");
        clickAndWait(Locator.linkContainingText("view results"));
        //turn off flags
        assertElementPresent(Locator.xpath("//td[contains(@style, 'white-space') and text()=" + expectedHMFI + "]"));
        assertElementPresent(Locator.xpath("//td[contains(@style, 'white-space') and text()=" + expectedEC50 + "]"));
    }

    @LogMethod
    private void verifyExcludingRuns(Map<String, Integer> guideSetIds, String[] analytes)
    {

        // remove a run from the current guide set
        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        clickButtonContainingText("Edit", 0);
        editRunBasedGuideSet(new String[]{"guideRunSetRow_0"}, GUIDE_SET_5_COMMENT, false);

        // create a new guide set for the second analyte so that we can test the apply guide set
        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        createGuideSet(false);
        editRunBasedGuideSet(new String[]{"allRunsRow_1", "allRunsRow_2", "allRunsRow_3"}, "create new analyte 2 guide set with 3 runs", true);

        // apply the new guide set to a run
        verifyGuideSetToRun("NETWORK5", "create new analyte 2 guide set with 3 runs");

        // verify the threshold values for the new guide set
        guideSetIds = getGuideSetIdMap();
        int[] rowCounts2 = {2, 3};
        String[] ec504plAverages2 = {"179.78", "42158.22"};
        String[] ec504plStdDevs2 = {"22.21", "4833.76"};
        verifyGuideSetThresholds(guideSetIds, analytes, rowCounts2, ec504plAverages2, ec504plStdDevs2, "Four Parameter", "EC50Average", "EC50Std Dev");
        String[] aucAverages2 = {"8701.38", "85268.04"};
        String[] aucStdDevs2 = {"466.81", "738.55"};
        verifyGuideSetThresholds(guideSetIds, analytes, rowCounts2, aucAverages2, aucStdDevs2, "Trapezoidal", "AUCAverage", "AUCStd Dev");
    }

    @LogMethod
    private void verifyLeveyJenningsPermissions()
    {
        String ljUrl = getCurrentRelativeURL();
        String editor = "editor1_luminex@luminex.test";
        String reader = "reader1_luminex@luminex.test";

        createAndImpersonateUser(editor, "Editor");

        beginAt(ljUrl);
        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        assertTextPresent("Apply Guide Set");
        stopImpersonating();
        deleteUsers(true, editor);

        createAndImpersonateUser(reader, "Reader");

        beginAt(ljUrl);
        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        assertTextPresent("Levey-Jennings Reports", "Standard1");
        assertTextNotPresent("Apply Guide Set");
        stopImpersonating();
        deleteUsers(true, reader);
    }

    @LogMethod
    private void createAndImpersonateUser(String user, String perms)
    {
        goToHome();
        createUser(user, null, false);
        goToProjectHome();
        setUserPermissions(user, perms);
        impersonate(user);
    }

    String newGuideSetPlate = "Reload guide set 5";
    @LogMethod
    private void verifyQCFlagUpdatesAfterWellChange()
    {
        importPlateFiveAgain();

        //add QC flag colum
        assertTextPresent(TEST_ASSAY_LUM + " Runs");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("QCFlags");
        _customizeViewsHelper.saveCustomView("QC Flags View");

        DataRegionTable drt = new DataRegionTable("Runs", this);

        //2. exclude wells A4, B4 from plate 5a for both analytes
        //	- the EC50 for GS Analyte (2) is changed to be under the Guide Set range so new QC Flag inserted for that
        excludeWellFromRun("Guide Set plate 5", "A4,B4");
        goBack();
        refresh();
        _extHelper.clickExtMenuButton(true, Locator.navButton("Views"), "QC Flags View");
        assertEquals("AUC, EC50-4, EC50-5, HMFI, PCV",  drt.getDataAsText(1, "QC Flags"));

        //3. un-exclude wells A4, B4 from plate 5a for both analytes
        //	- the EC50 QC Flag for GS Analyte (2) that was inserted in the previous step is removed
        includeWellFromRun("Guide Set plate 5", "A4,B4");
        goBack();
        refresh();
        _extHelper.clickExtMenuButton(true, Locator.navButton("Views"), "QC Flags View");
        assertEquals("AUC, EC50-5, HMFI, PCV",  drt.getDataAsText(1, "QC Flags"));

        //4. For GS Analyte (2), apply the non-current guide set to plate 5a
        //	- QC Flags added for EC50 and HMFI
        goToLeveyJenningsGraphPage("Standard1");
        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        String newQcFlags = "AUC, EC50-4, EC50-5, HMFI";
        assertTextNotPresent(newQcFlags);
        applyGuideSetToRun("NETWORK5", GUIDE_SET_5_COMMENT, false);
        //assert ec50 and HMFI red text present
        assertElementPresent(Locator.xpath("//div[text()='28040.51' and contains(@style,'red')]"));
        assertElementPresent(Locator.xpath("//div[text()='27950.73' and contains(@style,'red')]"));
        assertElementPresent(Locator.xpath("//div[text()='79121.90' and contains(@style,'red')]"));
        assertElementPresent(Locator.xpath("//div[text()='32145.80' and contains(@style,'red')]"));
        assertTextPresent(newQcFlags);
        //verify new flags present in run list
        goToTestRunList();
        _extHelper.clickExtMenuButton(true, Locator.navButton("Views"), "QC Flags View");
        assertTextPresent("AUC, EC50-4, EC50-5, HMFI, PCV");

        //5. For GS Analyte (2), apply the guide set for plate 5a back to the current guide set
        //	- the EC50 and HMFI QC Flags that were added in step 4 are removed
        goToLeveyJenningsGraphPage("Standard1");
        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        applyGuideSetToRun("NETWORK5", GUIDE_SET_5_COMMENT, true);
        assertTextNotPresent(newQcFlags);

        //6. Create new Guide Set for GS Analyte (2) that includes plate 5 (but not plate 5a)
        //	- the AUC QC Flag for plate 5 is removed
        Locator.XPathLocator aucLink =  Locator.xpath("//a[contains(text(),'AUC')]");
        int aucCount = getElementCount(aucLink);
        createGuideSet(false);
        editRunBasedGuideSet(new String[]{"allRunsRow_1"}, "Guide set includes plate 5", true);
        assertEquals("Wrong count for AUC flag links", aucCount-1, (getElementCount(aucLink)));

        //7. Switch to GS Analyte (1), and edit the current guide set to include plate 3
        //	- the QC Flag for plate 3 (the run included) and the other plates (4, 5, and 5a) are all removed as all values are within the guide set ranges
        setUpLeveyJenningsGraphParams("GS Analyte (1)");
        assertExpectedAnalyte1QCFlagsPresent();
        clickButtonContainingText("Edit", 0);
        editRunBasedGuideSet(new String[]{"allRunsRow_3"}, "edited analyte 1", false);
        assertEC505PLQCFlagsPresent(1);

        //8. Edit the GS Analyte (1) guide set and remove plate 3
        //	- the QC Flags for plates 3, 4, 5, and 5a return (HMFI for all 4 and AUC for plates 4, 5, and 5a)
        removePlate3FromGuideSet();
        assertExpectedAnalyte1QCFlagsPresent();
    }

    @LogMethod
    private void removePlate3FromGuideSet()
    {
        clickButtonContainingText("Edit", 0);
        _extHelper.waitForExt3Mask(WAIT_FOR_JAVASCRIPT);
        waitAndClick(Locator.id("guideRunSetRow_0"));
        clickButton("Save",0);
        waitForGuideSetExtMaskToDisappear();
    }

    private void assertExpectedAnalyte1QCFlagsPresent()
    {
        assertElementPresent(Locator.xpath("//a[contains(text(),'HMFI')]"), 4);
    }

    private void assertEC505PLQCFlagsPresent(int count)
    {
        assertEquals("Unexpected QC Flag Highlight Present", count,
                getElementCount(Locator.xpath("//div[contains(@style,'red')]")));
        assertElementPresent(Locator.xpath("//a[contains(text(),'EC50-5')]"), count);
        for(String flag : new String[] {"AUC", "HMFI", "EC50-4", "PCV"})
        {
            assertElementNotPresent(Locator.xpath("//a[contains(text(),'" + flag + "')]"));
        }
    }

    private void importPlateFiveAgain()
    {
        //1. upload plate 5 again with the same isotype and conjugate (plate 5a)
        //	- QC flags inserted for AUC for both analytes and HMFI for GS Analyte (1)

        goToTestAssayHome();
        clickButton("Import Data");
        setFormElement(Locator.name("network"), "NETWORK" + (10));
        clickButton("Next");

        importLuminexRunPageTwo(newGuideSetPlate, isotype, conjugate, "", "", "Notebook" + 11,
                    "Experimental", "TECH" + (11), "",  TEST_ASSAY_LUM_FILE9, 6, true);
        uncheckCheckbox(Locator.name("_titrationRole_standard_Standard1"));
        checkCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));
        clickButton("Save and Finish");


    }

    private void verifyQCFlags()
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));
        verifyQCFlagsInRunGrid();
        verifyQCFlagsSchema();
    }

    private void verifyQCFlagsSchema()
    {
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "QCFlags");
        waitForText("assay.Luminex." + TEST_ASSAY_LUM + ".QCFlags");
    }

    String[] expectedFlags = {"AUC, EC50-4, EC50-5, HMFI, PCV", "AUC, EC50-4, EC50-5, HMFI", "EC50-5, HMFI", "", "PCV"};

    private void verifyQCFlagsInRunGrid()
    {
        //add QC flag colum
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("QCFlags");
        _customizeViewsHelper.saveCustomView();

        //verify expected values in column
        List<String> var = getColumnValues("Runs", "QC Flags").get(0);
        String[] flags = var.toArray(new String[var.size()]);
        for(int i=0; i<flags.length; i++)
        {
            assertEquals(expectedFlags[i], flags[i].trim());
        }
        verifyQCFlagLink();
    }

    private void enableDisableQCFlags(String runName, String... flags)
    {
        Locator l = Locator.xpath("//a[text()='" + runName + "']/../../td/a[contains(@onclick,'showQCFlag')]");
        click(l);
        _extHelper.waitForExt3Mask(WAIT_FOR_JAVASCRIPT);

        sleep(1500);
        waitForText("Run QC Flags");

        for(String flag : flags)
        {
            Locator aucCheckBox = Locator.xpath("//div[text()='" + flag + "']/../../td/div/div[contains(@class, 'check')]");
            click(aucCheckBox);
        }

        clickButton("Save");
    }

    @LogMethod
    private void verifyQCFlagLink()
    {
        click(Locator.linkContainingText(expectedFlags[0], 0));
        _extHelper.waitForExt3Mask(WAIT_FOR_JAVASCRIPT);
        sleep(1500);
        assertTextPresent("CV", 4); // 3 occurances of PCV and 1 of %CV

        //verify text is in expected form
        waitForText("Standard1 GS Analyte (1) - " + isotype + " " + conjugate + " under threshold for AUC");

        //verify unchecking a box  removes the flag
        Locator aucCheckBox = Locator.xpath("//div[text()='AUC']/../../td/div/div[contains(@class, 'check')]");
        click(aucCheckBox);
        clickButton("Save", 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);

        Locator strikeoutAUC = Locator.xpath("//span[contains(@style, 'line-through') and  text()='AUC']");
        waitForElement(strikeoutAUC);

        //verify rechecking a box adds the flag back
        click(strikeoutAUC);
        _extHelper.waitForExt3Mask(WAIT_FOR_JAVASCRIPT);
        waitAndClick(aucCheckBox);
        clickButton("Save", 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        waitForText(expectedFlags[0]);
        assertElementNotPresent(strikeoutAUC);
    }

    private void verifyQCAnalysis()
    {
        goToQCAnalysisPage();
        verifyQCReport();
    }

    private void goToQCAnalysisPage()
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));

        clickAndWait(Locator.linkWithText("view results"));
        _extHelper.clickExtMenuButton(true, Locator.xpath("//a[text() = 'view qc report']"), "view titration qc report");

    }

    @LogMethod
    private void verifyQCReport()
    {
        //make sure all the columns we want are viable
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addCustomizeViewColumn("Five ParameterCurveFit/FailureFlag");
        _customizeViewsHelper.addCustomizeViewColumn("Four ParameterCurveFit/FailureFlag");
        _customizeViewsHelper.addCustomizeViewColumn("Five ParameterCurveFit/EC50");
        _customizeViewsHelper.saveCustomView();

        assertTextPresent("Titration QC Report");
        DataRegionTable drt = new DataRegionTable("AnalyteTitration", this);
        String isotype = drt.getDataAsText(0, "Isotype");
        if(isotype.length()==0)
            isotype = "[None]";
        String conjugate = drt.getDataAsText(0, "Conjugate");
        if(conjugate.length()==0)
            conjugate =  "[None]";

        log("verify the calculation failure flag");
        List<String> fourParamFlag = drt.getColumnDataAsText("Four Parameter Curve Fit Failure Flag");
        for(String flag: fourParamFlag)
        {
            assertEquals(" ", flag);
        }

        List<String> fiveParamFlag = drt.getColumnDataAsText("Five Parameter Curve Fit Failure Flag");
        List<String> fiveParamData = drt.getColumnDataAsText("Five Parameter Curve Fit EC50");

        for(int i=0; i<fiveParamData.size(); i++)
        {
            assertTrue("Row " + i + " was flagged as 5PL failure but had EC50 data", ((fiveParamFlag.get(i).equals(" ")) ^ (fiveParamData.get(i).equals(" "))));
        }


        //verify the Levey-Jennings plot
        clickAndWait(Locator.linkWithText("graph", 0));
        waitForText(" - " + isotype + " " + conjugate);
        assertTextPresent("Levey-Jennings Report", "Standard1");
    }

    protected void excludeWellFromRun(String run, String well)
    {
        clickAndWait(Locator.linkContainingText(run));

        log("Exclude well from run");
        clickExclusionMenuIconForWell(well);
        clickButton("Save");
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
    }

    //re-include an excluded well
    protected void includeWellFromRun(String run, String well)
    {
        clickAndWait(Locator.linkContainingText(run));

        log("Exclude well from from run");
        clickExclusionMenuIconForWell(well);
        click(Locator.radioButtonById("excludeselected"));
        clickButton("Save", 0);
        _extHelper.clickExtButton("Yes");
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
    }


    private void excludableWellsWithTransformTest()
    {
        goToProjectHome();
        clickAndWait(Locator.linkContainingText(TEST_ASSAY_LUM));
        excludeWellFromRun("Guide Set plate 5", "A6,B6");
        goToLeveyJenningsGraphPage("Standard1");
        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        assertTextPresent("28040.51");
    }


    @LogMethod
    private void guideSetApiTest()
    {
        goToProjectHome();
        assertTextNotPresent("GS Analyte");

        String wikiName = "LuminexGuideSetTestWiki";
        addWebPart("Wiki");
        createNewWikiPage("HTML");
        setFormElement(Locator.name("name"), wikiName);
        setWikiBody("Placeholder text.");
        saveWikiPage();
        setSourceFromFile("LuminexGuideSet.html", wikiName);

        waitAndClick(Locator.id("button_loadqwps"));
        waitForText("Done loading QWPs");
        assertTextNotPresent("Unexpected Error:");

        click(Locator.id("button_testiud"));
        waitForText("Done testing inserts, updates, and deletes");
        assertTextNotPresent("Unexpected Error:");

        click(Locator.id("button_updateCurveFit"));
        waitForText("Done with CurveFit update");
        assertTextNotPresent("Unexpected Error:");

        click(Locator.id("button_updateGuideSetCurveFit"));
        waitForText("Done with GuideSetCurveFit update");
        assertTextNotPresent("Unexpected Error:");

        // check the QWPs again to make the inserts/updates/deletes didn't affected the expected row counts
        click(Locator.id("button_loadqwps"));
        waitForText("Done loading QWPs again");
        assertTextNotPresent("Unexpected Error:");
    }

    @LogMethod
    private void verifyLeveyJenningsRplots()
    {
        goToLeveyJenningsGraphPage("Standard1");
        setUpLeveyJenningsGraphParams("GS Analyte (2)");

        // check 4PL ec50 trending R plot
        click(Locator.tagWithText("span", "EC50 - 4PL"));
        waitForLeveyJenningsTrendPlot();
        assertElementPresent( Locator.id("EC50 4PLTrendPlotDiv"));

        // check5PL  ec50 trending R plot
        click(Locator.tagWithText("span", "EC50 - 5PL Rumi"));
        waitForLeveyJenningsTrendPlot();
        assertElementPresent( Locator.id("EC50 5PLTrendPlotDiv"));

        // check auc trending R plot
        click(Locator.tagWithText("span", "AUC"));
        waitForLeveyJenningsTrendPlot();
        assertElementPresent( Locator.id("AUCTrendPlotDiv"));

        // check high mfi trending R plot
        click(Locator.tagWithText("span", "High MFI"));
        waitForLeveyJenningsTrendPlot();
        assertElementPresent( Locator.id("High MFITrendPlotDiv"));

        //verify QC flags
        //this locator finds an EC50 flag, then makes sure there's red text outlining
        Locator.XPathLocator l = Locator.xpath("//td/div[contains(@style,'red')]/../../td/div/a[contains(text(),'EC50-4')]");
        assertElementPresent(l,2);
        assertTextPresent("QC Flags");

        // Verify as much of the Curve Comparison window as we can - most of its content is in the image, so it's opaque
        // to the test
        for (int i = 1; i <= 5; i++)
        {
            click(ExtHelper.locateGridRowCheckbox("NETWORK" + i));
        }
        clickButton("View 4PL Curves", 0);
        waitForTextToDisappear("loading curves...", WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent("Error executing command");
        assertTextPresent("Export to PDF");
        clickButton("View Log Y-Axis", 0);
        waitForTextToDisappear("loading curves...", WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent("Error executing command");
        clickButton("View Linear Y-Axis", 0);
        waitForTextToDisappear("loading curves...", WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent("Error executing command");
        assertTextPresent("View Log Y-Axis");

        clickButton("Close", 0);
    }

    @LogMethod
    private void verifyGuideSetsNotApplied()
    {
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "AnalyteTitration");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        DataRegionTable table = new DataRegionTable("query", this);
        table.setFilter("GuideSet/Created", "Is Not Blank", "");
        // check that the table contains one row that reads "No data to show."
        assertEquals("Expected no guide set assignments", 0, table.getDataRowCount());
        table.clearFilter("GuideSet/Created");
    }

    @LogMethod
    private void verifyGuideSetsApplied(Map<String, Integer> guideSetIds, String[] analytes, int expectedRunCount)
    {

        // see if the 3 uploaded runs got the correct 'current' guide set applied
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "AnalyteTitration");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addCustomizeViewColumn("Analyte/RowId");
        _customizeViewsHelper.addCustomizeViewColumn("Titration/RowId");
        _customizeViewsHelper.addCustomizeViewColumn("GuideSet/RowId");
        _customizeViewsHelper.applyCustomView();
        DataRegionTable table = new DataRegionTable("query", this);
        for (String analyte : analytes)
        {
            table.setFilter("GuideSet/RowId", "Equals", guideSetIds.get(analyte).toString());
            assertEquals("Expected guide set to be assigned to " + expectedRunCount + " records", expectedRunCount, table.getDataRowCount());
            table.clearFilter("GuideSet/RowId");
        }

    }

    private Map<String, Integer> getGuideSetIdMap()
    {
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "GuideSet");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        Map<String, Integer> guideSetIds = new HashMap<>();
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addCustomizeViewColumn("RowId");
        _customizeViewsHelper.applyCustomView();
        DataRegionTable table = new DataRegionTable("query", this);
        table.setFilter("CurrentGuideSet", "Equals", "true");
        guideSetIds.put(table.getDataAsText(0, "Analyte Name"), Integer.parseInt(table.getDataAsText(0, "Row Id")));
        guideSetIds.put(table.getDataAsText(1, "Analyte Name"), Integer.parseInt(table.getDataAsText(1, "Row Id")));

        return guideSetIds;
    }

    private void createInitialGuideSets()
    {
        setUpLeveyJenningsGraphParams("GS Analyte (1)");
        createGuideSet(true);
        editRunBasedGuideSet(new String[]{"allRunsRow_1", "allRunsRow_0"}, "Analyte 1", true);

        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        createGuideSet(true);
        editRunBasedGuideSet(new String[]{"allRunsRow_1"}, "Analyte 2", true);

        //edit a guide set
        log("attempt to edit guide set after creation");
        clickButtonContainingText("Edit", 0);
        editRunBasedGuideSet(new String[]{"allRunsRow_0"}, "edited analyte 2", false);
    }

    public void setUpLeveyJenningsGraphParams(String analyte)
    {
        log("Setting Levey-Jennings Report graph parameters for Analyte " + analyte);
        waitForText(analyte);
        click(Locator.tagContainingText("span", analyte));

        _extHelper.selectComboBoxItem("Isotype:", isotype);
        _extHelper.selectComboBoxItem("Conjugate:", conjugate);
        click(Locator.extButton("Apply", 0));

        // wait for the test headers in the guide set and tracking data regions
        waitForText(analyte + " - " + isotype + " " + conjugate);
        waitForText("Standard1 Tracking Data for " + analyte + " - " + isotype + " " + conjugate);
        waitForLeveyJenningsTrendPlot();
        waitForElement(Locator.xpath("//img[starts-with(@id,'resultImage')]"));
    }

    private void addRemoveGuideSetRuns(String[] rows)
    {
        for(String row: rows)
        {
            waitForElement(Locator.id(row));
            click(Locator.tagWithId("span", row));
        }

    }
    public void createGuideSet(boolean initialGuideSet)
    {
        if (initialGuideSet)
            waitForText("No current guide set for the selected graph parameters");
        clickButtonContainingText("New", 0);
        if (!initialGuideSet)
        {
            waitForText("Creating a new guide set will set the current guide set to be inactive. Would you like to proceed?");
            clickButton("Yes", 0);
        }
    }

    public void editValueBasedGuideSet(Map<String, Double> metricInputs, String comment, boolean creating)
    {
        checkManageGuideSetHeader(creating);

        if (creating)
            checkRadioButton(Locator.radioButtonByNameAndValue("ValueBased", "true"));
        setValueBasedMetricForm(metricInputs);

        setFormElement(Locator.name("commentTextField"), comment);
        saveGuideSet(creating);

        checkLeveyJenningsGuideSetHeader(comment, "Value-based");
    }

    public void editRunBasedGuideSet(String[] rows, String comment, boolean creating)
    {
        checkManageGuideSetHeader(creating);

        addRemoveGuideSetRuns(rows);

        setFormElement(Locator.name("commentTextField"), comment);
        saveGuideSet(creating);

        checkLeveyJenningsGuideSetHeader(comment, "Run-based");
    }

    private void checkManageGuideSetHeader(boolean creating)
    {
        if (creating)
        {
            waitForText("Create Guide Set...");
            waitForText("Guide Set ID:");
            assertTextPresent("TBD", 2);
        }
        else
        {
            waitForText("Manage Guide Set...");
            waitForText("Guide Set ID:");
            assertTextPresentInThisOrder("Created:", today);
        }
    }

    private void saveGuideSet(boolean creating)
    {
        if (creating)
        {
            assertElementNotPresent(Locator.button("Save"));
            assertElementPresent(Locator.button("Create"));
            clickButton("Create",0);
            today = df.format(Calendar.getInstance().getTime());
        }
        else
        {
            assertElementNotPresent(Locator.button("Create"));
            assertElementPresent(Locator.button("Save"));
            clickButton("Save",0);
        }
        waitForGuideSetExtMaskToDisappear();
    }

    private void checkLeveyJenningsGuideSetHeader(String comment, String guideSetType)
    {
        waitForElement(Locator.tagWithText("td", today), 2*defaultWaitForPage);
        assertElementPresent(Locator.tagWithText("td", comment));
        assertElementPresent(Locator.tagWithText("td", guideSetType));
    }

    private void setValueBasedMetricForm(Map<String, Double> metricInputs)
    {
        for (Map.Entry<String, Double> metricEntry : metricInputs.entrySet())
        {
            String strVal = metricEntry.getValue() != null ? metricEntry.getValue().toString() : null;
            setFormElement(Locator.name(metricEntry.getKey()), strVal);
        }
    }

    private void waitForGuideSetExtMaskToDisappear()
    {
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        waitForLeveyJenningsTrendPlot();
    }

    private void goToLeveyJenningsGraphPage(String titrationName)
    {
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "Titration");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        clickAndWait(Locator.linkContainingText(titrationName));
        waitForText("Levey-Jennings Report");
        waitForText(titrationName);
        // Make sure we have the expected help text
        waitForText("To begin, choose an Antigen, Isotype, and Conjugate from the panel to the left and click the Apply button.");
    }

    @LogMethod
    private void verifyGuideSetThresholds(Map<String, Integer> guideSetIds, String[] analytes, int[] rowCounts, String[] averages, String[] stdDevs,
                                          String curveType, String averageColName, String stdDevColName)
    {
        // go to the GuideSetCurveFit table to verify the calculated threshold values for the EC50 and AUC
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "GuideSetCurveFit");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addCustomizeViewColumn("GuideSetId/RowId");
        _customizeViewsHelper.applyCustomView();
        DataRegionTable table = new DataRegionTable("query", this);
        for (int i = 0; i < analytes.length; i++)
        {
            // verify the row count, average, and standard deviation for the specified curve type's values
            table.setFilter("GuideSetId/RowId", "Equals", guideSetIds.get(analytes[i]).toString());
            table.setFilter("CurveType", "Equals", curveType);
            assertEquals("Unexpected row count for guide set " + guideSetIds.get(analytes[i]).toString(), rowCounts[i], Integer.parseInt(table.getDataAsText(0, "Run Count")));
            assertEquals("Unexpected average for guide set " + guideSetIds.get(analytes[i]).toString(), averages[i],table.getDataAsText(0, averageColName));
            assertEquals("Unexpected stddev for guide set " + guideSetIds.get(analytes[i]).toString(), stdDevs[i], table.getDataAsText(0, stdDevColName));
            table.clearFilter("CurveType");
            table.clearFilter("GuideSetId/RowId");
        }
    }

    @LogMethod
    protected void applyGuideSetToRun(String network, String comment, boolean useCurrent)
    {
        click(ExtHelper.locateGridRowCheckbox(network));
        clickButton("Apply Guide Set", 0);
        sleep(1000);//we need a little time even after all the elements have appeared, so waits won't work

        if(!useCurrent)
            click(ExtHelper.locateGridRowCheckbox(comment));

        waitAndClick(5000, getButtonLocator("Apply Thresholds"), 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        // verify that the plot is reloaded
        waitForLeveyJenningsTrendPlot();

    }

    @LogMethod
    private void verifyGuideSetToRun(String network, String comment)
    {
        click(ExtHelper.locateGridRowCheckbox(network));
        clickButton("Apply Guide Set", 0);
        waitForElement(ExtHelper.locateGridRowCheckbox(network));
        waitForElement(ExtHelper.locateGridRowCheckbox(comment));
        sleep(1000);
        // deselect the current guide set to test error message
        click(ExtHelper.locateGridRowCheckbox(comment));
        clickButton("Apply Thresholds", 0);
        waitForText("Please select a guide set to be applied to the selected records.");
        clickButton("OK", 0);
        // reselect the current guide set and apply it
        click(ExtHelper.locateGridRowCheckbox(comment));
        clickButton("Apply Thresholds", 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        // verify that the plot is reloaded
        waitForLeveyJenningsTrendPlot();
    }

    @LogMethod
    private void applyStartAndEndDateFilter()
    {
        String colValuePrefix = "NETWORK";

        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        // check that all 5 runs are present in the grid by clicking on them
        for (int i = 1; i <= 5; i++)
        {
            assertElementPresent(ExtHelper.locateGridRowCheckbox(colValuePrefix + i));
        }
        // set start and end date filter
        setFormElement(Locator.name("start-date-field"), "2011-03-26");
        setFormElement(Locator.name("end-date-field"), "2011-03-28");
        waitAndClick(Locator.extButtonEnabled("Apply").index(1));
        waitForLeveyJenningsTrendPlot();
        // check that only 3 runs are now present
        waitForElementToDisappear(ExtHelper.locateGridRowCheckbox(colValuePrefix + "1"), WAIT_FOR_JAVASCRIPT);
        for (int i = 2; i <= 4; i++)
        {
            assertElementPresent(ExtHelper.locateGridRowCheckbox(colValuePrefix + i));
        }
        assertElementNotPresent(ExtHelper.locateGridRowCheckbox(colValuePrefix + "5"));
    }

    @LogMethod
    private void applyNetworkProtocolFilter()
    {
        String colNetworkPrefix = "NETWORK";
        String colProtocolPrefix = "PROTOCOL";

        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        // check that all 5 runs are present in the grid by clicking on them
        for (int i = 1; i <= 5; i++)
        {
            assertElementPresent(ExtHelper.locateGridRowCheckbox(colNetworkPrefix + i));
        }
        // set network and protocol filter
        _extHelper.selectComboBoxItem(Locator.xpath("//input[@id='network-combo-box']/.."), colNetworkPrefix + "3");
        _extHelper.selectComboBoxItem(Locator.xpath("//input[@id='protocol-combo-box']/.."), colProtocolPrefix + "3");

        waitAndClick(Locator.extButtonEnabled("Apply").index(1));
        waitForLeveyJenningsTrendPlot();
        // check that only 1 runs are now present
        waitForElementToDisappear(ExtHelper.locateGridRowCheckbox(colNetworkPrefix + "1"), WAIT_FOR_JAVASCRIPT);
        assertElementPresent(ExtHelper.locateGridRowCheckbox(colNetworkPrefix + "3"));

        assertElementNotPresent(ExtHelper.locateGridRowCheckbox(colNetworkPrefix + "1"));
        assertElementNotPresent(ExtHelper.locateGridRowCheckbox(colNetworkPrefix + "2"));
        assertElementNotPresent(ExtHelper.locateGridRowCheckbox(colNetworkPrefix + "4"));
        assertElementNotPresent(ExtHelper.locateGridRowCheckbox(colNetworkPrefix + "5"));

        // Clear the filter and check that all rows reappear
        waitAndClick(Locator.extButtonEnabled("Clear"));
        waitForLeveyJenningsTrendPlot();
        for (int i = 1; i <= 5; i++)
        {
            assertElementPresent(ExtHelper.locateGridRowCheckbox(colNetworkPrefix + i));
        }
    }

    private void applyLogYAxisScale()
    {
        setUpLeveyJenningsGraphParams("GS Analyte (2)");
        _extHelper.selectComboBoxItem(Locator.xpath("//input[@id='scale-combo-box']/.."), "Log");
        waitForLeveyJenningsTrendPlot();
    }

    private void waitForLeveyJenningsTrendPlot()
    {
        waitForTextToDisappear("Loading");
        assertTextNotPresent("ScriptException");
        assertElementNotPresent(Locator.tagContainingText("pre", "Error"));
    }

    @LogMethod
    private boolean verifyRunFileAssociations(int index)
    {
        // verify that the PDF of curves file was generated along with the xls file and the Rout file
        DataRegionTable table = new DataRegionTable("Runs", this);
        table.setFilter("Name", "Equals", "Guide Set plate " + index);
        clickAndWait(Locator.tagWithAttribute("img", "src", "/labkey/Experiment/images/graphIcon.gif"));
        clickAndWait(Locator.linkWithText("Text View"));
        waitForText("Protocol Applications"); // bottom section of the "Text View" tab for the run details page
        assertElementPresent(Locator.linkWithText("Guide Set plate " + index + ".Standard1_QC_Curves_4PL.pdf"), 3);
        assertElementPresent(Locator.linkWithText("Guide Set plate " + index + ".Standard1_QC_Curves_5PL.pdf"), 3);
        assertElementPresent(Locator.linkWithText("Guide Set plate " + index + ".xls"), 4);
        assertElementPresent(Locator.linkWithText("Guide Set plate " + index + ".labkey_luminex_transform.Rout"), 3);

        return true;
    }

    @LogMethod
    public void uploadPositivityFile(String assayName, File file, String baseVisit, String foldChange, boolean isBackgroundUpload, boolean expectDuplicateFile)
    {
        goToTestAssayHome();
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.name("name"), assayName);
        checkCheckbox(Locator.name("calculatePositivity"));
        setFormElement(Locator.name("baseVisit"), baseVisit);
        setFormElement(Locator.name("positivityFoldChange"), foldChange);
        assertTrue("Positivity Data absent: " + file.toString(), file.exists());
        setFormElement(Locator.name("__primaryFile__"), file);
        if (expectDuplicateFile)
            waitForText("A file with name '" + file.getName() + "' already exists");
        clickButton("Next");
        setAnalytePropertyValues();
        clickButton("Save and Finish");
        if (!isBackgroundUpload && !isElementPresent(Locator.css(".labkey-error").containing("Error: ")))
            clickAndWait(Locator.linkWithText(assayName), 2 * WAIT_FOR_PAGE);
    }

    protected void setAnalytePropertyValues()
    {
        // no op, currently used by LuminexPositivityTest
    }

}