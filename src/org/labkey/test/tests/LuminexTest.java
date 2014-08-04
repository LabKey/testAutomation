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
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.RReportHelper;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
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
    protected final File TEST_ASSAY_XAR_FILE = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Luminex/" + TEST_ASSAY_XAR_NAME + ".xar");

    protected static final String TEST_ASSAY_LUM_SET_PROP_SPECIES = "testSpecies1";
    protected final File TEST_ASSAY_LUM_FILE1 = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Luminex/10JAN07_plate_1.xls");
    protected final File TEST_ASSAY_LUM_FILE2 = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Luminex/pnLINCO20070302A.xlsx");
    protected final File TEST_ASSAY_LUM_FILE3 = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Luminex/WithIndices.xls");
    protected final File TEST_ASSAY_LUM_FILE4 = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Luminex/WithAltNegativeBead.xls");
    protected final File TEST_ASSAY_LUM_FILE5 = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Luminex/Guide Set plate 1.xls");
    protected final File TEST_ASSAY_LUM_FILE6 = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Luminex/Guide Set plate 2.xls");
    protected final File TEST_ASSAY_LUM_FILE7 = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Luminex/Guide Set plate 3.xls");
    protected final File TEST_ASSAY_LUM_FILE8 = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Luminex/Guide Set plate 4.xls");
    protected final File TEST_ASSAY_LUM_FILE9 = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Luminex/Guide Set plate 5.xls");
    protected final File TEST_ASSAY_LUM_FILE10 = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Luminex/RawAndSummary.xlsx");
    protected final File TEST_ASSAY_LUM_FILE11 = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Luminex/PositivityWithBaseline.xls");
    protected final File TEST_ASSAY_LUM_FILE12 = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Luminex/PositivityWithoutBaseline.xls");
    protected final File TEST_ASSAY_LUM_FILE13 = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Luminex/PositivityThreshold.xls");

    protected final File TEST_ASSAY_MULTIPLE_STANDARDS_1 = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Luminex/plate 1_IgA-Biot (Standard2).xls");
    protected final File TEST_ASSAY_MULTIPLE_STANDARDS_2 = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Luminex/plate 2_IgA-Biot (Standard2).xls");
    protected final File TEST_ASSAY_MULTIPLE_STANDARDS_3 = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Luminex/plate 3_IgA-Biot (Standard1).xls");

    protected static final String RTRANSFORM_SCRIPT_FILE_LABKEY = "/resources/transformscripts/labkey_luminex_transform.R";
    protected static final String RTRANSFORM_SCRIPT_FILE_LAB = "/resources/transformscripts/tomaras_luminex_transform.R";

    public static final String ASSAY_ID_FIELD  = "name";
    public static final String ASSAY_DATA_FILE_LOCATION_MULTIPLE_FIELD = "__primaryFile__";

    public static final String DATA_TABLE_NAME = "Data";
    protected static final String EXCLUDE_COMMENT_FIELD = "comment";
    protected static final String MULTIPLE_CURVE_ASSAY_RUN_NAME = "multipleCurvesTestRun";
    protected static final String SAVE_CHANGES_BUTTON = "Save";

    protected DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    protected String today = null;

    protected String isotype = "IgG ></% 1";// put back TRICKY_CHARACTERS_NO_QUOTES when issue 20061 is resolved
    protected String conjugate = "PE ></% 1";// put back TRICKY_CHARACTERS_NO_QUOTES when issue 20061 is resolved


    public List<String> getAssociatedModules()
    {
        return Arrays.asList("luminex");
    }

    public String getModuleDirectory()
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
        _containerHelper.createProject(getProjectName(), null);

        //setup a pipeline for it
        setupPipeline(getProjectName());

        //create a study within this project to which we will publish
        goToProjectHome();
        addWebPart("Study Overview");
        clickButton("Create Study");
        clickButton("Create Study");
        goToProjectHome();

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
            _listHelper.addField("Batch Fields", 9, "RVersion", "R Version", ListColumnType.String);

            // add run properties for designation of which field to use for curve fit calc in transform
            _listHelper.addField("Run Fields", 8, "SubtNegativeFromAll", "Subtract Negative Bead from All Wells", ListColumnType.Boolean);
            _listHelper.addField("Run Fields", 9, "StndCurveFitInput", "Input Var for Curve Fit Calc of Standards", ListColumnType.String);
            _listHelper.addField("Run Fields", 10, "UnkCurveFitInput", "Input Var for Curve Fit Calc of Unknowns", ListColumnType.String);
            _listHelper.addField("Run Fields", 11, "CurveFitLogTransform", "Curve Fit Log Transform", ListColumnType.Boolean);
            _listHelper.addField("Run Fields", 12, "SkipRumiCalculation", "Skip Ruminex Calculations", ListColumnType.Boolean);

            // add run properties for use with the Guide Set test
            _listHelper.addField("Run Fields", 13, "NotebookNo", "Notebook Number", ListColumnType.String);
            _listHelper.addField("Run Fields", 14, "AssayType", "Assay Type", ListColumnType.String);
            _listHelper.addField("Run Fields", 15, "ExpPerformer", "Experiment Performer", ListColumnType.String);

            // add run properties for use with Calculating Positivity
            _listHelper.addField("Run Fields", 16, "CalculatePositivity", "Calculate Positivity", ListColumnType.Boolean);
            _listHelper.addField("Run Fields", 17, "BaseVisit", "Baseline Visit", ListColumnType.Double);
            _listHelper.addField("Run Fields", 18, "PositivityFoldChange", "Positivity Fold Change", ListColumnType.Integer);

            // add analyte property for tracking lot number
            _listHelper.addField("Analyte Properties", 6, "LotNumber", "Lot Number", ListColumnType.String);
            _listHelper.addField("Analyte Properties", 7, "NegativeControl", "Negative Control", ListColumnType.Boolean);

            // add the data properties for the calculated columns
            _listHelper.addField("Data Fields", 0, "FIBackgroundNegative", "FI-Bkgd-Neg", ListColumnType.Double);
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
     * click on the exclusion icon associated with the particular well
     * preconditions:  at Test Result page
     * postconditions: at Test Result Page with exclude Replicate Group From Analysis window up
     * @param wellName
     */
    protected void clickExclusionMenuIconForWell(String wellName)
    {
        waitAndClick(Locator.id("__changeExclusions__" + wellName));
        _extHelper.waitForExtDialog("Exclude Replicate Group from Analysis");
        waitForElement(Locator.xpath("//table[@id='saveBtn' and not(contains(@class, 'disabled'))]"), WAIT_FOR_JAVASCRIPT);
    }

    protected void clickExcludeAnalyteCheckBox(String analyte)
    {
        Locator l = ExtHelper.locateGridRowCheckbox(analyte);
        waitAndClick(l);
    }

    protected String[] getListOfAnalytesMultipleCurveData()
    {
        //TODO:  make this a dynamic list, acquired from the current data set, rather than hardcoded
        return new String[] {"ENV6 (97)", "ENV7 (93)", "ENV4 (26)",
                        "ENV5 (58)", "Blank (53)"};
    }


    /**
     * Goes to test run list for the common list used by all the tests
     */
    protected void goToTestRunList()
    {
        goToProjectHome();
        clickAndWait(Locator.linkContainingText(TEST_ASSAY_LUM));
    }

    protected String startCreateMultipleCurveAssayRun()
    {
        log("Creating test run with multiple standard curves");
        String name = MULTIPLE_CURVE_ASSAY_RUN_NAME;

        createNewAssayRun(name);

        uploadMultipleCurveData();

        return name;

    }

    protected void compareColumnValuesAgainstExpected(String column1, String column2, Map<String, Set<String>> column1toColumn2)
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
     * upload the three files used for the multiple curve data test
     * preconditions:  at assay run data import page
     * postconditions: at data import: analyte properties page
     */
    protected void uploadMultipleCurveData()
    {
        addFilesToAssayRun(TEST_ASSAY_MULTIPLE_STANDARDS_1, TEST_ASSAY_MULTIPLE_STANDARDS_2, TEST_ASSAY_MULTIPLE_STANDARDS_3);
        clickButton("Next");
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
            goToProjectHome();
            clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));
        }
    }

    private void setFormat(String where, int index, String formatStr)
    {
        String prefix = getPropertyXPath(where);
        _listHelper.clickRow(prefix, index);
        click(Locator.xpath(prefix + "//span[contains(@class,'x-tab-strip-text') and text()='Format']"));
        setFormElement(Locator.id("propertyFormat"), formatStr);
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

    @LogMethod
    protected void createAndImpersonateUser(String user, String perms)
    {
        goToHome();
        createUser(user, null, false);
        goToProjectHome();
        _permissionsHelper.setUserPermissions(user, perms);
        impersonate(user);
    }


    @LogMethod
    public void uploadPositivityFile(String assayName, File file, String baseVisit, String foldChange, boolean isBackgroundUpload, boolean expectDuplicateFile)
    {
        preUploadPositivityFile(assayName);
        checkCheckbox(Locator.name("calculatePositivity"));
        setFormElement(Locator.name("baseVisit"), baseVisit);
        setFormElement(Locator.name("positivityFoldChange"), foldChange);
        selectPositivityFile(file, expectDuplicateFile);
        setAnalytePropertyValues();
        finishUploadPositivityFile(assayName, isBackgroundUpload);
    }

    public void finishUploadPositivityFile(String assayName, boolean isBackgroundUpload)
    {
        clickButton("Save and Finish");
        if (!isBackgroundUpload && !isElementPresent(Locator.css(".labkey-error").containing("Error: ")))
            clickAndWait(Locator.linkWithText(assayName), 2 * WAIT_FOR_PAGE);
    }

    public void selectPositivityFile(File file, boolean expectDuplicateFile)
    {
        assertTrue("Positivity Data absent: " + file.toString(), file.exists());
        setFormElement(Locator.name("__primaryFile__"), file);
        if (expectDuplicateFile)
            waitForText("A file with name '" + file.getName() + "' already exists");
        clickButton("Next");
    }

    public void preUploadPositivityFile(String assayName)
    {
        goToTestAssayHome();
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.name("name"), assayName);
    }

    protected void setAnalytePropertyValues()
    {
        // no op, currently used by LuminexPositivityTest
    }

}