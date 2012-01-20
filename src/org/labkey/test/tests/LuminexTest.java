/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

import junit.framework.Assert;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.RReportHelper;
import static org.labkey.test.util.ListHelper.ListColumnType;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.ExtHelper;

import java.io.File;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: jeckels
 * Date: Nov 20, 2007
 */
public class LuminexTest extends AbstractQCAssayTest
{
    private final static String TEST_ASSAY_PRJ_LUMINEX = "LuminexTest Project";            //project for luminex test

    protected static final String TEST_ASSAY_LUM =  "&TestAssayLuminex" + TRICKY_CHARACTERS_NO_QUOTES;
    protected static final String TEST_ASSAY_LUM_DESC = "Description for Luminex assay";

    protected static final String TEST_ASSAY_LUM_ANALYTE_PROP_NAME = "testAssayAnalyteProp";
    protected static final int TEST_ASSAY_LUM_ANALYTE_PROP_ADD = 5;
    protected static final String[] TEST_ASSAY_LUM_ANALYTE_PROP_TYPES = { "Text (String)", "Boolean", "Number (Double)", "Integer", "DateTime" };
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

    protected final File TEST_ASSAY_MULTIPLE_STANDARDS_1 = new File(getLabKeyRoot() + "/sampledata/Luminex/plate 1_IgA-Biot (b12 IgA std).xls");
    protected final File TEST_ASSAY_MULTIPLE_STANDARDS_2 = new File(getLabKeyRoot() + "/sampledata/Luminex/plate 2_IgA-Biot (b12 IgA std).xls");
    protected final File TEST_ASSAY_MULTIPLE_STANDARDS_3 = new File(getLabKeyRoot() + "/sampledata/Luminex/plate 3_IgG-Biot (HIVIG std).xls");

    protected final String TEST_ASSAY_LUM_ANALYTE_PROP = "testAnalyteProp";
    private static final String THAW_LIST_NAME = "LuminexThawList";
    private static final String TEST_ASSAY_LUM_RUN_NAME4 = "testRunName4";

    private static final String RTRANSFORM_SCRIPT_FILE1 = "/resources/transformscripts/tomaras_luminex_transform.R";
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

    public static final String DATA_TABLE_NAME = TEST_ASSAY_LUM+" Data";
    private static final String EXCLUDE_COMMENT_FIELD = "comment";
    public static final String EXCLUDE_SELECTED_BUTTON = "excludeselected";
    protected static final String MULTIPLE_CURVE_ASSAY_RUN_NAME = "multipleCurvesTestRun";
    protected static final String SAVE_CHANGES_BUTTON = "Save";

    private String EC50_RUN_NAME = "EC50";
    private String rum4 = "Four Parameter";
    private String rum5 = "Five FourParameter";
    private String trapezoidal = "Trapezoidal";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/luminex";
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
            configure();
        }
    }

    protected void configure()
    {
        if(!isFileUploadAvailable())
            Assert.fail("Test depends on file upload ability");

        // setup a scripting engine to run a java transform script
        prepareProgrammaticQC();

        // fail fast if R is not configured
        RReportHelper.ensureRConfig(this);

        //revert to the admin user
        revertToAdmin();

        log("Testing Luminex Assay Designer");
        //create a new test project
        createProject(TEST_ASSAY_PRJ_LUMINEX);

        //setup a pipeline for it
        setupPipeline(TEST_ASSAY_PRJ_LUMINEX);

        //create a study within this project to which we will publish
        clickLinkWithText(TEST_ASSAY_PRJ_LUMINEX);
        addWebPart("Study Overview");
        clickNavButton("Create Study");
        clickNavButton("Create Study");
        clickLinkWithText(TEST_ASSAY_PRJ_LUMINEX);

        //add the Assay List web part so we can create a new luminex assay
        addWebPart("Assay List");

        //create a new luminex assay
        clickNavButton("Manage Assays");
        clickNavButton("New Assay Design");
        checkRadioButton("providerName", "Luminex");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        log("Setting up Luminex assay");
        selenium.type("//input[@id='AssayDesignerName']", TEST_ASSAY_LUM);
        selenium.type("//textarea[@id='AssayDesignerDescription']", TEST_ASSAY_LUM_DESC);

        // add batch properties for transform and Ruminex version numbers
        addField("Batch Fields", 5, "Network", "Network", ListColumnType.String);
        addField("Batch Fields", 6, "TransformVersion", "Transform Script Version", ListColumnType.String);
        addField("Batch Fields", 7, "RuminexVersion", "Ruminex Version", ListColumnType.String);

        // add run properties for designation of which field to use for curve fit calc in transform
        addField("Run Fields", 8, "SubtBlankFromAll", "Subtract Blank Bead from All Wells", ListColumnType.Boolean);
        addField("Run Fields", 9, "StndCurveFitInput", "Input Var for Curve Fit Calc of Standards", ListColumnType.String);
        addField("Run Fields", 10, "UnkCurveFitInput", "Input Var for Curve Fit Calc of Unknowns", ListColumnType.String);

        // add run properties for use with the Guide Set test
        addField("Run Fields", 11, "NotebookNo", "Notebook Number", ListColumnType.String);
        addField("Run Fields", 12, "AssayType", "Assay Type", ListColumnType.String);
        addField("Run Fields", 13, "ExpPerformer", "Experiment Performer", ListColumnType.String);

        // add analyte property for tracking lot number
        addField("Analyte Properties", 6, "LotNumber", "Lot Number", ListColumnType.String);

        // add the data properties for the calculated columns
        addField("Data Fields", 0, "fiBackgroundBlank", "FI-Bkgd-Blank", ListColumnType.Double);
        addField("Data Fields", 1, "Standard", "Stnd for Calc", ListColumnType.String);
        addField("Data Fields", 2, "EstLogConc_5pl", "Est Log Conc Rumi 5 PL", ListColumnType.Double);
        addField("Data Fields", 3, "EstConc_5pl", "Est Conc Rumi 5 PL", ListColumnType.Double);
        addField("Data Fields", 4, "SE_5pl", "SE Rumi 5 PL", ListColumnType.Double);
        addField("Data Fields", 5, "EstLogConc_4pl", "Est Log Conc Rumi 4 PL", ListColumnType.Double);
        addField("Data Fields", 6, "EstConc_4pl", "Est Conc Rumi 4 PL", ListColumnType.Double);
        addField("Data Fields", 7, "SE_4pl", "SE Rumi 4 PL", ListColumnType.Double);
        addField("Data Fields", 8, "Slope_4pl", "Slope_4pl", ListColumnType.Double);
        addField("Data Fields", 9, "Lower_4pl", "Lower_4pl", ListColumnType.Double);
        addField("Data Fields", 10, "Upper_4pl", "Upper_4pl", ListColumnType.Double);
        addField("Data Fields", 11, "Inflection_4pl", "Inflection_4pl", ListColumnType.Double);

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

        sleep(1000);
        clickNavButton("Save", 0);
        waitForText("Save successful.", 20000);

        configStatus = Configured.CONFIGURED;
    }


    /**
     * Performs Luminex designer/upload/publish.
     */
    protected void runUITests()
    {
        log("Starting Assay BVT Test");

        if(isFileUploadAvailable())
        {
            runUploadAndCopyTest();
            runJavaTransformTest();
            runRTransformTest();
            runMultipleCurveTest();
            runWellExclusionTest();
            runEC50Test();
            runGuideSetTest();
        }
    } //doTestSteps()

    protected void runUploadAndCopyTest()
    {
        ListHelper.ListColumn participantCol = new ListHelper.ListColumn("ParticipantID", "ParticipantID", ListColumnType.String, "Participant ID");
        ListHelper.ListColumn visitCol = new ListHelper.ListColumn("VisitID", "VisitID", ListColumnType.Double, "Visit id");
        ListHelper.createList(this, TEST_ASSAY_PRJ_LUMINEX, THAW_LIST_NAME, ListColumnType.String, "Index", participantCol, visitCol);
        ListHelper.uploadData(this, TEST_ASSAY_PRJ_LUMINEX, THAW_LIST_NAME, "Index\tParticipantID\tVisitID\n" +
                "1\tListParticipant1\t1001.1\n" +
                "2\tListParticipant2\t1001.2\n" +
                "3\tListParticipant3\t1001.3\n" +
                "4\tListParticipant4\t1001.4");
        clickLinkWithText(TEST_ASSAY_PRJ_LUMINEX);

        clickLinkWithText("Assay List");
        clickLinkWithText(TEST_ASSAY_LUM);
        log("Uploading Luminex Runs");
        clickNavButton("Import Data");
        setFormElement("species", TEST_ASSAY_LUM_SET_PROP_SPECIES);
        clickNavButton("Next");
        setFormElement("name", TEST_ASSAY_LUM_RUN_NAME);
        setFormElement("__primaryFile__", TEST_ASSAY_LUM_FILE1);
        clickNavButton("Next", 60000);
        clickNavButton("Save and Import Another Run");
        clickLinkWithText(TEST_ASSAY_LUM);

        clickNavButton("Import Data");
        assertEquals(TEST_ASSAY_LUM_SET_PROP_SPECIES, selenium.getValue("species"));
        setFormElement("species", TEST_ASSAY_LUM_SET_PROP_SPECIES2);
        clickNavButton("Next");
        setFormElement("name", TEST_ASSAY_LUM_RUN_NAME2);
        setFormElement("__primaryFile__", TEST_ASSAY_LUM_FILE2);
        clickNavButton("Next", 60000);
        selenium.type("//input[@type='text' and contains(@name, '_analyte_')][1]", "StandardName1b");
        selenium.type("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text']", "StandardName2");
        selenium.type("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[5]//input[@type='text']", "StandardName4");
        clickNavButton("Save and Finish");

        // Upload another run using a thaw list pasted in as a TSV
        clickNavButton("Import Data");
        assertEquals(TEST_ASSAY_LUM_SET_PROP_SPECIES2, selenium.getValue("species"));
        setFormElement("participantVisitResolver", "Lookup");
        setFormElement("ThawListType", "Text");
        setFormElement("ThawListTextArea", "Index\tSpecimenID\tParticipantID\tVisitID\n" +
                "1\tSpecimenID1\tParticipantID1\t1.1\n" +
                "2\tSpecimenID2\tParticipantID2\t1.2\n" +
                "3\tSpecimenID3\tParticipantID3\t1.3\n" +
                "4\tSpecimenID4\tParticipantID4\t1.4");
        clickNavButton("Next");
        setFormElement("__primaryFile__", TEST_ASSAY_LUM_FILE3);
        clickNavButton("Next", 60000);
        assertEquals("StandardName1b", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]"));
        assertEquals("StandardName4", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text'][1]"));
        clickNavButton("Save and Finish");

        // Upload another run using a thaw list that pointed at the list we uploaded earlier
        clickNavButton("Import Data");
        assertEquals(TEST_ASSAY_LUM_SET_PROP_SPECIES2, selenium.getValue("species"));
        assertEquals("off", selenium.getValue("//input[@name='participantVisitResolver' and @value='SampleInfo']"));
        assertEquals("on", selenium.getValue("//input[@name='participantVisitResolver' and @value='Lookup']"));
        assertEquals("on", selenium.getValue("//input[@name='ThawListType' and @value='Text']"));
        assertEquals("off", selenium.getValue("//input[@name='ThawListType' and @value='List']"));
        checkRadioButton("ThawListType", "List");
        waitForElement(Locator.id("button_Choose list..."), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Choose list...", 0);
        setFormElement("schema", "lists");
        setFormElement("table", THAW_LIST_NAME);
        clickNavButton("Close", 0);
        clickNavButton("Next");
        setFormElement("name", TEST_ASSAY_LUM_RUN_NAME4);
        setFormElement("__primaryFile__", TEST_ASSAY_LUM_FILE3);
        clickNavButton("Next", 60000);
        assertEquals("StandardName1b", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]"));
        assertEquals("StandardName4", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text'][1]"));
        clickNavButton("Save and Finish");

        log("Check that upload worked");
        clickLinkWithText(TEST_ASSAY_LUM_RUN_NAME);
        assertTextPresent("Hu IL-1b (32)");

        clickLinkWithText(TEST_ASSAY_LUM + " Runs");
        clickLinkWithText(TEST_ASSAY_LUM_RUN_NAME3);
        assertTextPresent("IL-1b (1)");
        assertTextPresent("ParticipantID1");
        assertTextPresent("ParticipantID2");
        assertTextPresent("ParticipantID3");
        setFilter(TEST_ASSAY_LUM + " Data", "ParticipantID", "Equals", "ParticipantID1");
        assertTextPresent("1.1");
        setFilter(TEST_ASSAY_LUM + " Data", "ParticipantID", "Equals", "ParticipantID2");
        assertTextPresent("1.2");

        clickLinkWithText(TEST_ASSAY_LUM + " Runs");
        clickLinkWithText(TEST_ASSAY_LUM_RUN_NAME4);
        assertTextPresent("IL-1b (1)");
        assertTextPresent("ListParticipant1");
        assertTextPresent("ListParticipant2");
        assertTextPresent("ListParticipant3");
        assertTextPresent("ListParticipant4");
        setFilter(TEST_ASSAY_LUM + " Data", "ParticipantID", "Equals", "ListParticipant1");
        assertTextPresent("1001.1");
        setFilter(TEST_ASSAY_LUM + " Data", "ParticipantID", "Equals", "ListParticipant2");
        assertTextPresent("1001.2");

        clickLinkWithText(TEST_ASSAY_LUM + " Runs");
        clickLinkWithText(TEST_ASSAY_LUM_RUN_NAME2);
        assertTextPresent("IL-1b (1)");
        assertTextPresent("9011-04");

        setFilter(TEST_ASSAY_LUM + " Data", "FI", "Equals", "20");
        selenium.click(".toggle");
        clickNavButton("Copy to Study");
        selectOptionByText("targetStudy", "/" + TEST_ASSAY_PRJ_LUMINEX + " (" + TEST_ASSAY_PRJ_LUMINEX + " Study)");
        clickNavButton("Next");
        setFormElement("participantId", "ParticipantID");
        setFormElement("visitId", "100.1");
        clickNavButton("Copy to Study");

        log("Verify that the data was published");
        assertTextPresent("ParticipantID");
        assertTextPresent("100.1");
        assertTextPresent(TEST_ASSAY_LUM_RUN_NAME2);
        assertTextPresent("LX10005314302");

        // Upload another run that has both Raw and Summary data in the same excel file
        clickLinkWithText(TEST_ASSAY_PRJ_LUMINEX);
        clickLinkWithText(TEST_ASSAY_LUM);
        clickNavButton("Import Data");
        clickNavButton("Next");
        setFormElement("name", "raw and summary");
        setFormElement("__primaryFile__", TEST_ASSAY_LUM_FILE10);
        clickNavButton("Next", 60000);
        clickNavButton("Save and Finish");

        clickLinkWithText("raw and summary");
        // make sure the Summary, StdDev, and DV columns are visible
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Summary");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "StdDev");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "CV");
        CustomizeViewsHelper.applyCustomView(this);
        // show all rows (> 100 in full data file)
        clickNavButton("Page Size", 0);
        clickLinkWithText("Show All");

        // check that both the raw and summary data were uploaded together
        DataRegionTable table = new DataRegionTable(TEST_ASSAY_LUM + " Data", this);
        assertEquals("Unexpected number of data rows for both raw and summary data", 108, table.getDataRowCount());
        // check the number of rows of summary data
        table.setFilter("Summary", "Equals", "true");
        assertEquals("Unexpected number of data rows for summary data", 36, table.getDataRowCount());
        table.clearFilter("Summary");
        // check the number of rows of raw data
        table.setFilter("Summary", "Equals", "false");
        assertEquals("Unexpected number of data rows for raw data", 72, table.getDataRowCount());
        table.clearFilter("Summary");
        // check the row count at the analyte level
        table.setFilter("Analyte", "Equals", "Analyte1");
        assertEquals("Unexpected number of data rows for Analyte1", 36, table.getDataRowCount());

        // check the StdDev and % CV for a few samples
        checkStdDevAndCV("Analyte1", "S10", 3, "0.35", "9.43%");
        checkStdDevAndCV("Analyte2", "S4", 3, "3.18", "4.80%");
        checkStdDevAndCV("Analyte3", "S8", 3, "1.77", "18.13%");
    }

    private void checkStdDevAndCV(String analyte, String type, int rowCount, String stddev, String cv)
    {
        DataRegionTable table = new DataRegionTable(TEST_ASSAY_LUM + " Data", this);
        table.setFilter("Analyte", "Equals", analyte);
        table.setFilter("Type", "Equals", type);
        assertEquals("Unexpected number of data rows for " + analyte + "/" + type, rowCount, table.getDataRowCount());
        for (int i = 0; i < rowCount; i++)  
        {
            assertEquals("Wrong StdDev", stddev, table.getDataAsText(i, "StdDev"));
            assertEquals("Wrong %CV", cv, table.getDataAsText(i, "CV"));
        }
        table.clearFilter("Type");
        table.clearFilter("Analyte");
    }

    protected void runEC50Test()
    {
//        ensureConfigured();
        ensureRTransformPresent();
        createNewAssayRun(EC50_RUN_NAME);
        uploadEC50Data();
//        ensureMultipleCurveDataPresent();
//        ensureEC50DataPresent();
        clickButton("Save and Finish", 2 * WAIT_FOR_PAGE);

        //add transform script
        goToSchemaBrowser();
        selectQuery("assay", TEST_ASSAY_LUM + " CurveFit");
        waitForText("view data");
        clickLinkContainingText("view data");
        assertTextPresent("Four Parameter");

        waitForText("3.45399");
        
        checkEC50data();
    }

    private void checkEC50data()
    {
        DataRegionTable table = new DataRegionTable("query", this);
        List<String> analyte = table.getColumnDataAsText("Analyte");
        List<String> formula = table.getColumnDataAsText("Curve Type");
        List<String> ec50 = table.getColumnDataAsText("EC50");
        List<String> auc= table.getColumnDataAsText("AUC");
        List<String> inflectionPoint = table.getColumnDataAsText("Inflection");

        log("Write this");
        for(int i=0; i<formula.size(); i++)
        {
            if(formula.get(i).equals(rum4))
            {
                //ec50=populated=inflectionPoint
                assertEquals(ec50.get(i), inflectionPoint.get(i));
                //auc=unpopulated
                assertEquals("", auc.get(i));
            }
            else if(formula.get(i).equals(rum5))
            {

                assertEquals("", ec50.get(i));
                assertEquals("", auc.get(i));
            }
            else if(formula.get(i).equals(trapezoidal))
            {
                //ec50 should not be populated
                assertEquals("", ec50.get(i));
                //auc=populated (for all non-blank analytes)
                if (!analyte.get(i).startsWith("Blank"))
                    assertTrue( "AUC was unpopulated for row " + i, ((String) auc.get(i)).length()>0);
            }
        }

//        formula = getTableColumnValues()

        //you are here
    }

    /**
     * test of well exclusion- the ability to exclude certain wells or analytes and add ac oment as to why
     * preconditions: LUMINEX project and assay list exist.  Having the Multiple Curve data will speed up execution
     * but is not required
     * postconditions:  multiple curve data will be present, certain wells will be marked excluded
     */
    protected void runWellExclusionTest()
    {
         ensureMultipleCurveDataPresent();

         clickLinkContainingText(MULTIPLE_CURVE_ASSAY_RUN_NAME);

        //ensure multiple curve data present
        //there was a bug (never filed) that showed up with multiple curve data, so best to use that.

        String[] analytes = getListOfAnalytesMultipleCurveData();

        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "ExclusionComment");
        CustomizeViewsHelper.applyCustomView(this);

        //"all" excludes all
        String excludeAllWellName = "E1";
        excludeAllAnalytesForSingleWellTest(excludeAllWellName);

        String excludeOneWellName = "E1";
        excludeOneAnalyteForSingleWellTest(excludeOneWellName, analytes[0]);

        //excluding for one well excludes for duplicate wells
        excludeAnalyteForAllWellsTest(analytes[1]);

    }

    private void excludeOneAnalyteForSingleWellTest(String wellName, String excludedAnalyte)
    {
        waitForText("Well Role");
        clickExclusionMenuIconForWell(wellName);

        String exclusionComment = "exclude single analyte for single well";
        setText(EXCLUDE_COMMENT_FIELD, exclusionComment);
        clickRadioButtonById(EXCLUDE_SELECTED_BUTTON);
        clickExcludeAnalyteCheckBox(excludedAnalyte, true);
        clickButton(SAVE_CHANGES_BUTTON, 2 * defaultWaitForPage);

        excludeForSingleWellVerify("Excluded for replicate group: " + exclusionComment, new HashSet<String>((Arrays.asList(excludedAnalyte))));
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
        setText(EXCLUDE_COMMENT_FIELD, comment);
        clickButton(SAVE_CHANGES_BUTTON, 2 * defaultWaitForPage);

        excludeForSingleWellVerify("Excluded for replicate group: " + comment, new HashSet<String>(Arrays.asList(getListOfAnalytesMultipleCurveData())));

        //remove exclusions to leave in clean state
        clickExclusionMenuIconForWell(wellName);
        clickRadioButtonById("excludeselected");
        clickButton(SAVE_CHANGES_BUTTON, 0);
        ExtHelper.waitForExtDialog(this, "Warning");
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
        String analyte;

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
            analyte= analytesPresent.get(i);
            log("Analyte: " + analyte);

            if(matchesWell(description, type, well) && analytes.contains(analyte))
            {
                assertEquals(expectedComment,comment);
            }

            if(expectedComment.equals(comment))
            {
                assertTrue(matchesWell(description, type, well));
                assertTrue(analytes.contains(analyte));
            }
        }
    }

    //verifies if description, type, and well match the hardcoded values
    private boolean matchesWell(String description, String type, String well)
    {
        if(!excludedWellDescription.equals(description))
            return false;
        if(!excludedWellType.equals(type))
            return false;
        return excludedWells.contains(well);
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
        clickButtonContainingText("Exclude Analytes", 0);
        ExtHelper.waitForExtDialog(this, "Exclude Analytes from Analysis");
        clickExcludeAnalyteCheckBox(analyte, true);
        String comment = "Changed for all analytes";
        setText(EXCLUDE_COMMENT_FIELD, comment);
        waitForElement(Locator.xpath("//table[@id='saveBtn' and not(contains(@class, 'disabled'))]"), WAIT_FOR_JAVASCRIPT);
        clickButton(SAVE_CHANGES_BUTTON, 2 * defaultWaitForPage);

        String exclusionPrefix = "Excluded for analyte: ";
        Map<String, Set<String>> analyteToExclusion = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(exclusionPrefix + comment);
        analyteToExclusion.put(analyte, set);

        analyteToExclusion = createExclusionMap(set, analyte);

        compareColumnValuesAgainstExpected("Analyte", "Exclusion Comment", analyteToExclusion);
    }

    /**
     * return a map that, for each key, has value value
     * @param value
     * @param key
     * @return
     */
    private Map<String, Set<String>> createExclusionMap(Set<String> value, String... key)
    {
        Map<String, Set<String>> m  = new HashMap<String, Set<String>>();

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
        ExtHelper.waitForExtDialog(this, "Exclude Replicate Group from Analysis");
        waitForElement(Locator.xpath("//table[@id='saveBtn' and not(contains(@class, 'disabled'))]"), WAIT_FOR_JAVASCRIPT);
    }


    private String getLinkIDFromWellName(String wellName)
    {
        return "__changeExclusions__" + wellName;
    }



    private void clickExcludeAnalyteCheckBox(String analyte, boolean b)
    {
        if(b)
            ExtHelper.prevClickFileBrowserFileCheckbox(this, analyte);
        else
            fail("not supported at this time");
    }

    private String excludedWellDescription = "Sample 2";
    private String excludedWellType = "X25";
    private Set<String> excludedWells = new HashSet<String>(Arrays.asList("E1", "F1"));

    private String[] getListOfAnalytesMultipleCurveData()
    {
        //TODO:  make this a dynamic list, acquired from the current data set, rather than hardcoded
        return new String[] {"VRC A 5304 gp140 (62)", "VRC B gp140 (63)", "B.con.env03 140 CF (65)",
                        "JRFL gp140 (66)", "Blank (53)"};
    }



    /**several tests use this data.  Rather that clean and import for each
     * or take an unnecessary dependency of one to the other, this function
     * checks if the data is already present and, if it is not, adds it
     * preconditions:  Project TEST_ASSAY_PRJ_LUMINEX with Assay  TEST_ASSAY_LUM exists
     * postconditions:  assay run
     */
    private void ensureMultipleCurveDataPresent()
    {
        goToTestRunList();

        if(!isTextPresent(MULTIPLE_CURVE_ASSAY_RUN_NAME)) //right now this is a good enough check.  May have to be
                                                    // more rigorous if tests start substantially altering data
        {
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
        clickLinkContainingText(TEST_ASSAY_PRJ_LUMINEX);
        clickLinkContainingText(TEST_ASSAY_LUM);
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
    protected void runMultipleCurveTest()
    {
        String name = startCreateMultipleCurveAssayRun();

        String[] standardsNames = {"HIVIG", "b12 IgA"};
        checkStandardsCheckBoxesExist(standardsNames);

        String[] possibleAnalytes = getListOfAnalytesMultipleCurveData();
        String[] possibleStandards = new String[] {"b12 IgA", "HIVIG"};

        Map<String, Set<String>> analytesAndStandardsConfig = generateAnalytesAndStandardsConfig(possibleAnalytes, possibleStandards);
        configureStandardsForAnalytes(analytesAndStandardsConfig, possibleStandards);



        clickButton("Save and Finish", 2*WAIT_FOR_PAGE);
        clickLinkWithText(name);

        //edit view to show Analyte Standard
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Analyte/Standard");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Analyte/StdCurve");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Analyte/FitProb");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Analyte/ResVar");
        CustomizeViewsHelper.applyCustomView(this);

        // We're OK with grabbing the footer curve fit from any of the files, under normal usage they should all share
        // the same curve fits
        assertTrue("BioPlex curve fit for VRC A 5304 gp140 (62) in plate 1, 2, or 3",
                isTextPresent("FI = 0.465914 + (1.5417E+006 - 0.465914) / ((1 + (Conc / 122.733)^-0.173373))^7.64039") ||
                isTextPresent("FI = 0.582906 + (167.081 - 0.582906) / ((1 + (Conc / 0.531813)^-5.30023))^0.1"));
        assertTrue("BioPlex FitProb for VRC A 5304 gp140 (62) in plate 1, 2, or 3", isTextPresent("0.9667") || isTextPresent("0.4790"));
        assertTrue("BioPlex ResVar for VRC A 5304 gp140 (62) in plate 1, 2, 3", isTextPresent("0.1895") || isTextPresent("0.8266"));

        compareColumnValuesAgainstExpected("Analyte", "Standard", analytesAndStandardsConfig);

        // Go to the schema browser to check out the parsed curve fits
        goToSchemaBrowser();
        selectQuery("assay", TEST_ASSAY_LUM + " CurveFit");
        waitForText("view data");
        clickLinkContainingText("view data");

        // We're OK with grabbing the footer curve fit from any of the files, under normal usage they should all share
        // the same curve fits
        assertTrue("BioPlex curve fit parameter for VRC A 5304 gp140 (62) in plate 1, 2, or 3", isTextPresent("0.465914") || isTextPresent("0.582906"));
        assertTrue("BioPlex curve fit parameter for VRC A 5304 gp140 (62) in plate 1, 2, or 3", isTextPresent("7.64039") || isTextPresent("0.1"));
    }

    private void compareColumnValuesAgainstExpected(String column1, String column2, Map<String, Set<String>> column1toColumn2)
    {
        Set<String> set = new HashSet<String>();
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
        Map<String, Set<String>> analytesAndStandardsConfig =  new HashMap<String, Set<String>>();


        //based on the assumption that there are five analytes and two possible standards:  update this if you need to test for more
        Set<String> firstStandard = new HashSet<String>(); firstStandard.add(possibleStandards[0]);
        Set<String> secondStandard = new HashSet<String>(); secondStandard.add(possibleStandards[1]);
        Set<String> bothStandard = new HashSet<String>();
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
        File[] files = {TEST_ASSAY_MULTIPLE_STANDARDS_1, TEST_ASSAY_MULTIPLE_STANDARDS_2, TEST_ASSAY_MULTIPLE_STANDARDS_3};
        addFilesToAssayRun(files);
    }

    private void uploadEC50Data()
    {
        uploadMultipleCurveData();
    }

    private void addFilesToAssayRun(File[] files)
    {
        for(int i=0; i<files.length; i++)
        {
            String formName = ASSAY_DATA_FILE_LOCATION_MULTIPLE_FIELD + i;

            setFormElement(formName, files[i]);

            sleep(500);

            click(Locator.id("file-upload-add" + i));
        }
         clickButton("Next");

    }

    /**
     * create new assay run with name name
     * preconditions:  can see Project Folder, assay already exists
     * postconditions: at data import screen for new test run
     * @param name name to give new assay run
     */
    private void createNewAssayRun(String name)
    {
        goToTestRunList();
        clickButtonContainingText("Import Data");
        checkRadioButton("participantVisitResolver", "SampleInfo");
        clickButtonContainingText("Next");
        setFormElement(ASSAY_ID_FIELD, name);
    }



    /**
     * Cleanup entry point.
     */
    protected void doCleanup()
    {
        revertToAdmin();
        try
        {
            deleteProject(TEST_ASSAY_PRJ_LUMINEX);
            deleteEngine();
        }
        catch(Throwable T) {/* ignore */}

        deleteDir(getTestTempDir());
    } //doCleanup()

    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void runJavaTransformTest()
    {
        // add the transform script to the assay
        log("Uploading Luminex Runs with a transform script");


        //TODO:  goToTestRunList
        clickLinkWithText(TEST_ASSAY_PRJ_LUMINEX);
        clickLinkWithText(TEST_ASSAY_LUM);
        click(Locator.linkWithText("manage assay design"));
        clickLinkWithText("edit assay design");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerTransformScript']"), WAIT_FOR_JAVASCRIPT);

        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), "/sampledata/qc/transform.jar"));
        clickNavButton("Save & Close");

        goToTestAssayHome();
        clickNavButton("Import Data");
        setFormElement("species", TEST_ASSAY_LUM_SET_PROP_SPECIES);
        clickNavButton("Next");
        setFormElement("name", "transformed assayId");
        setFormElement("__primaryFile__", TEST_ASSAY_LUM_FILE1);
        clickNavButton("Next", 60000);
        clickNavButton("Save and Finish");

        // verify the description error was generated by the transform script
        clickLinkWithText("transformed assayId");
        DataRegionTable table = new DataRegionTable(TEST_ASSAY_LUM + " Data", this);
        for(int i = 1; i <= 40; i++)
        {
            assertEquals("Transformed", table.getDataAsText(i, "Description"));
        }
    }

    //helper function to go to test assay home from anywhere the project link is visible
    private void goToTestAssayHome()
    {
        clickLinkWithText(TEST_ASSAY_PRJ_LUMINEX);
        clickLinkWithText(TEST_ASSAY_LUM);
    }

    protected boolean R_TRANSFORM_SET = false;
    protected void ensureRTransformPresent()
    {
        if(!R_TRANSFORM_SET)
            runRTransformTest();
    }

    //requires drc, Ruminex and xtable packages installed in R
    protected void runRTransformTest()
    {
        log("Uploading Luminex run with a R transform script");


        // add the R transform script to the assay
        goToTestAssayHome();
        click(Locator.linkWithText("manage assay design"));
        clickLinkWithText("edit assay design");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerTransformScript']"), WAIT_FOR_JAVASCRIPT);
        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE1));

        // save changes to assay design
        clickNavButton("Save & Close");

        // upload the sample data file
        clickLinkWithText(TEST_ASSAY_PRJ_LUMINEX);
        clickLinkWithText(TEST_ASSAY_LUM);
        clickNavButton("Import Data");
        clickNavButton("Next");
        setFormElement("name", "r script transformed assayId");
        setFormElement("stndCurveFitInput", "FI");
        setFormElement("unkCurveFitInput", "FI-Bkgd-Blank");
        setFormElement("__primaryFile__", TEST_ASSAY_LUM_FILE4);
        clickNavButton("Next", 60000);
        // make sure the Standard checkboxes are checked
        checkCheckbox("_titrationRole_standard_HIVIG");
        checkCheckbox("titration_CN54 (1)_HIVIG");
        checkCheckbox("titration_Con S (2)_HIVIG");
        checkCheckbox("titration_Blank (3)_HIVIG");
        // make sure that that QC Control checkbox is checked
        checkCheckbox("_titrationRole_qccontrol_HIVIG");
        // set LotNumber for the first analyte
        selenium.type("//input[@type='text' and contains(@name, '_LotNumber')][1]", TEST_ANALYTE_LOT_NUMBER);
        clickNavButton("Save and Finish");

        // verify that the PDF of curves was generated
        Locator l = Locator.tagWithAttribute("img", "src", "/labkey/_images/sigmoidal_curve.png");
        click(l);
        assertLinkPresentWithText("WithBlankBead.HIVIG_5PL.pdf");
        assertLinkPresentWithText("WithBlankBead.HIVIG_4PL.pdf");
        assertLinkPresentWithText("WithBlankBead.HIVIG_QC_Curves.pdf");

        // verify that the lot number value are as expected
        clickLinkWithText("r script transformed assayId");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Analyte/Properties/LotNumber");
        CustomizeViewsHelper.applyCustomView(this);
        setFilter(TEST_ASSAY_LUM + " Data", "Analyte/Properties/LotNumber", "Equals", TEST_ANALYTE_LOT_NUMBER);
        assertTextPresent("1 - 40 of 40");
        clearFilter(TEST_ASSAY_LUM + " Data", "Analyte/Properties/LotNumber");

        // verfiy that the calculated values were generated by the transform script as expected
        DataRegionTable table = new DataRegionTable(TEST_ASSAY_LUM + " Data", this);
        setFilter(TEST_ASSAY_LUM + " Data", "fiBackgroundBlank", "Is Not Blank");
        assertTextPresent("1 - 40 of 40");
        setFilter(TEST_ASSAY_LUM + " Data", "Type", "Starts With", "X"); // filter to just the unknowns
        assertTextPresent("1 - 32 of 32");
        // check values in the fi-bkgd-blank column
        for(int i = 0; i < RTRANS_FIBKGDBLANK_VALUES.length; i++)
        {
            assertEquals(RTRANS_FIBKGDBLANK_VALUES[i], table.getDataAsText(i, "FI-Bkgd-Blank"));
        }
        clearFilter(TEST_ASSAY_LUM + " Data", "fiBackgroundBlank");
        setFilter(TEST_ASSAY_LUM + " Data", "EstLogConc_5pl", "Is Not Blank");
        assertTextPresent("1 - 32 of 32");
        // check values in the est log conc 5pl column
        for(int i = 0; i < RTRANS_ESTLOGCONC_VALUES_5PL.length; i++)
        {
            assertEquals(RTRANS_ESTLOGCONC_VALUES_5PL[i], table.getDataAsText(i, "Est Log Conc Rumi 5 PL"));
        }
        clearFilter(TEST_ASSAY_LUM + " Data", "EstLogConc_5pl");
        setFilter(TEST_ASSAY_LUM + " Data", "EstLogConc_4pl", "Is Not Blank");
        assertTextPresent("1 - 32 of 32");
        // check values in the est log conc 4pl column
        for(int i = 0; i < RTRANS_ESTLOGCONC_VALUES_4PL.length; i++)
        {
            assertEquals(RTRANS_ESTLOGCONC_VALUES_4PL[i], table.getDataAsText(i, "Est Log Conc Rumi 4 PL"));
        }
        clearFilter(TEST_ASSAY_LUM + " Data", "EstLogConc_4pl");
        clearFilter(TEST_ASSAY_LUM + " Data", "Type");

        R_TRANSFORM_SET = true;
    }

    private void setFormat(String where, int index, String formatStr)
    {
        String prefix = getPropertyXPath(where);
        ListHelper.clickRow(this, prefix, index);
        click(Locator.xpath(prefix + "//span[contains(@class,'x-tab-strip-text') and text()='Format']"));
        setFormElement("propertyFormat", formatStr);
    }

    protected String isotype = "IgG " + TRICKY_CHARACTERS_NO_QUOTES;
    protected String conjugate = "PE " + TRICKY_CHARACTERS_NO_QUOTES;

    protected DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    protected void importLuminexRun()
    {

    }

    protected void importLuminexRunPageTwo(String name, String isotype, String conjugate, String stndCurveFitInput,
                                           String unkCurveFitInput, String notebookNo, String assayType, String expPerformer,
                                           String testDate, String file, int i)
    {
            setFormElement("name", name);
            setFormElement("isotype", isotype);
            setFormElement("conjugate", conjugate);
            setFormElement("stndCurveFitInput", stndCurveFitInput);
            setFormElement("unkCurveFitInput", unkCurveFitInput);
            setFormElement("notebookNo", notebookNo);
            setFormElement("assayType", assayType);
            setFormElement("expPerformer", expPerformer);
            setFormElement("testDate", testDate);
            setFormElement("__primaryFile__", file);
            clickNavButton("Next", 60000);
    }
    //requires drc, Ruminex, rlabkey and xtable packages installed in R
    protected void runGuideSetTest()
    {
        log("Uploading Luminex run with a R transform script for Guide Set test");

        File[] files = {TEST_ASSAY_LUM_FILE5, TEST_ASSAY_LUM_FILE6, TEST_ASSAY_LUM_FILE7, TEST_ASSAY_LUM_FILE8, TEST_ASSAY_LUM_FILE9};
        String[] analytes = {"GS Analyte (1)", "GS Analyte (2)"};

        // add the R transform script to the assay
        goToTestAssayHome();
        click(Locator.linkWithText("manage assay design"));
        clickLinkWithText("edit assay design");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerTransformScript']"), WAIT_FOR_JAVASCRIPT);
        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE1));
        // save changes to assay design
        clickNavButton("Save & Close");

        // setup the testDate variable
        Calendar testDate = Calendar.getInstance();
        testDate.add(Calendar.DATE, -files.length);

        // upload the first set of files (2 runs)
        boolean displayingRowId = false;
        for (int i = 0; i < 2; i++)
        {
            goToTestAssayHome();
            clickNavButton("Import Data");
            setFormElement("network", "NETWORK" + (i + 1));
            clickNavButton("Next");

            testDate.add(Calendar.DATE, 1);
            importLuminexRunPageTwo("Guide Set plate " + (i+1), isotype, conjugate, "", "", "Notebook" + (i+1),
                        "Experimental", "TECH" + (i+1), df.format(testDate.getTime()), files[i].toString(), i);
            uncheckCheckbox("_titrationRole_standard_HIVIG");
            checkCheckbox("_titrationRole_qccontrol_HIVIG");
            clickNavButton("Save and Finish");

            displayingRowId = verifyRunFileAssociations(displayingRowId, (i+1));
        }

        //verify that the uploaded runs do not have associated guide sets
        verifyGuideSetsNotApplied();

        //create initial guide sets for the 2 analytes
        goToLeveyJenningsGraphPage("HIVIG");
        createInitialGuideSets();

        // check guide set IDs and make sure appropriate runs are associated to created guide sets
        Map<String, Integer> guideSetIds = getGuideSetIdMap();
        verifyGuideSetsApplied(guideSetIds, analytes, 2);

        //navil trail check
        assertTextPresent("assay Schema >  ");

        // verify the guide set threshold values for the first set of runs
        int[] rowCounts = {2, 2};
        double[] ec50Averages = {179.78, 43426.10};
        double[] ec50StdDevs = {22.21, 794.95};
        double[] aucAverages = {8701.38, 80851.83};
        double[] aucStdDevs = {466.81, 6523.08};
        verifyGuideSetThresholds(guideSetIds, analytes, rowCounts, ec50Averages, ec50StdDevs, aucAverages, aucStdDevs);

        // upload the final set of runs (2 runs)
        for (int i = 2; i < files.length; i++)
        {
            goToTestAssayHome();
            clickNavButton("Import Data");
            setFormElement("network", "NETWORK" + (i + 1));
            clickNavButton("Next");

            importLuminexRunPageTwo("Guide Set plate " + (i+1), isotype, conjugate, "", "", "Notebook" + (i+1),
                        "Experimental", "TECH" + (i+1), df.format(testDate.getTime()), files[i].toString(), i);
            uncheckCheckbox("_titrationRole_standard_HIVIG");
            checkCheckbox("_titrationRole_qccontrol_HIVIG");
            clickNavButton("Save and Finish");

            displayingRowId = verifyRunFileAssociations(displayingRowId, (i+1));
        }

        // verify that the newly uploaded runs got the correct guide set applied to them
        verifyGuideSetsApplied(guideSetIds, analytes, 5);

        //verify Levey-Jennings report R plots are displayed without errors
        verifyLeveyJenningsRplots();
        verifyQCFlags();
        verifyQCAnalysis();

        // remove a run from the current guide set
        setUpGuideSet("GS Analyte (2)");
        clickButtonContainingText("Edit", 0);
        editGuideSet(new String[] {"guideRunSetRow_0"}, GUIDE_SET_5_COMMENT, false);

        // create a new guide set for the second analyte so that we can test the apply guide set
        setUpGuideSet("GS Analyte (2)");
        createGuideSet("GS Analyte (2)", false);
        editGuideSet(new String[] {"allRunsRow_1", "allRunsRow_2", "allRunsRow_3"}, "create new analyte 2 guide set with 3 runs", true);

        // apply the new guide set to a run
        verifyGuideSetToRun("NETWORK5", 2, "create new analyte 2 guide set with 3 runs", 4);

        // verify the threshold values for the new guide set
        guideSetIds = getGuideSetIdMap();
        int[] rowCounts2 = {2, 3};
        double[] ec50Averages2 = {179.78, 42158.22};
        double[] ec50StdDevs2 = {22.21, 4833.76};
        double[] aucAverages2 = {8701.38, 85268.04};
        double[] aucStdDevs2 = {466.81, 738.55};
        verifyGuideSetThresholds(guideSetIds, analytes, rowCounts2, ec50Averages2, ec50StdDevs2, aucAverages2, aucStdDevs2);

        // test the start and end date filter for the report
        goToLeveyJenningsGraphPage("HIVIG");
        applyStartAndEndDateFilter();

        excludableWellsWithTransformTest();
        // test the y-axis scale
        applyLogYAxisScale();
        guideSetApiTest();
        verifyQCFlagUpdates();
    }

    String newGuideSetPlate = "Reload guide set 5";
    private void verifyQCFlagUpdates()
    {
        importPlateFiveAgain();
         DataRegionTable drt = new DataRegionTable(TEST_ASSAY_LUM + " Runs", this);
        //
        //2. exclude wells A4, B4 from plate 5a for both analytes
        //	- the EC50 for GS Analyte (2) is changed to be under the Guide Set range so new QC Flag inserted for that
        //
        excludeWellFromRun("Guide Set plate 5", "A4,B4");
        goBack();
        refresh();
        assertEquals("AUC, EC50, HMFI, PCV",  drt.getDataAsText(1, "QC Flags"));

        //3. un-exclude wells A4, B4 from plate 5a for both analytes
        //	- the EC50 QC Flag for GS Analyte (2) that was inserted in the previous step is removed

        includeWellFromRun("Guide Set plate 5", "A4,B4");
        goBack();
        refresh();
        assertEquals("AUC, HMFI, PCV",  drt.getDataAsText(1, "QC Flags"));


//4. For GS Analyte (2), apply the non-current guide set to plate 5a
//	- QC Flags added for EC50 and HMFI
        goToLeveyJenningsGraphPage("HIVIG");
        setUpGuideSet("GS Analyte (2)");
        String newQcFlags = "AUC, EC50, HMFI";
        assertTextNotPresent(newQcFlags);
      applyGuideSetToRun("NETWORK5", 2, GUIDE_SET_5_COMMENT,2 );
        //assert ec50 and HMFI red text present
        assertElementPresent(Locator.xpath("//div[text()='28040.51' and contains(@style,'red')]"));
        assertElementPresent(Locator.xpath("//div[text()='32145.80' and contains(@style,'red')]"));
        assertTextPresent(newQcFlags);
        //verify new flags present in run list
        goToTestRunList();
        assertTextPresent("AUC, EC50, HMFI, PCV");

//5. For GS Analyte (2), apply the guide set for plate 5a back to the current guide set
//	- the EC50 and HMFI QC Flags that were added in step 4 are removed
        goToLeveyJenningsGraphPage("HIVIG");
        setUpGuideSet("GS Analyte (2)");
        applyGuideSetToRun("NETWORK5", 2, GUIDE_SET_5_COMMENT, -1);
        assertTextNotPresent(newQcFlags);
//
//6. Create new Guide Set for GS Analyte (2) that includes plate 5 (but not plate 5a)
//	- the AUC QC Flag for plate 5 is removed
//
        Locator.XPathLocator aucLink =  Locator.xpath("//a[contains(text(),'AUC')]");
        int aucCount = getXpathCount(aucLink);
        createGuideSet("GS Analyte (2)", false);
        editGuideSet(new String[]{"allRunsRow_1"}, "Guide set includes plate 5", true);
        assertEquals("Wrong count for AUC flag links", aucCount-1, (getXpathCount(aucLink)));


//7. Switch to GS Analyte (1), and edit the current guide set to include plate 3
//	- the QC Flag for plate 3 (the run included) and the other plates (4, 5, and 5a) are all removed as all values are within the guide set ranges
        setUpGuideSet("GS Analyte (1)");
        assertExpectedAnalyte1QCFlagsPresent();
        clickButtonContainingText("Edit", 0);
        editGuideSet(new String[]{"allRunsRow_3"}, "edited analyte 1", false);
        assertNoQCFlagsPresent();

//8. Edit the GS Analyte (1) guide set and remove plate 3
//	- the QC Flags for plates 3, 4, 5, and 5a return (HMFI for all 4 and AUC for plates 4, 5, and 5a)
        removePlate3FromGuideSet();
        assertExpectedAnalyte1QCFlagsPresent();
    }

    private void removePlate3FromGuideSet()
    {
        clickButtonContainingText("Edit", 0);
        waitForExtMask();
        Locator l = Locator.id("guideRunSetRow_0");
        waitForElement(l, defaultWaitForPage);
        click(l);
        clickButton("Save",0);
        waitForGuideSetExtMaskToDisappear();
    }

    private void assertExpectedAnalyte1QCFlagsPresent()
    {

            assertElementPresent(Locator.xpath("//a[contains(text(),'HMFI')]"), 4);
    }

    private void assertNoQCFlagsPresent()
    {
        assertEquals("Unexpected QC Flag Highlight Present", 0,
                    getXpathCount(Locator.xpath("//div[contains(@style,'red')]")));
        for(String flag : new String[] {"AUC", "HMFI", "EC50", "PCV"})
        {
            assertElementNotPresent(Locator.xpath("//a[contains(text(),'" + flag + "')]"));
        }
    }

    private void importPlateFiveAgain()
    {
        //1. upload plate 5 again with the same isotype and conjugate (plate 5a)
        //	- QC flags inserted for AUC for both analytes and HMFI for GS Analyte (1)

        goToTestAssayHome();
        clickNavButton("Import Data");
        setFormElement("network", "NETWORK" + (10));
        clickNavButton("Next");

        importLuminexRunPageTwo(newGuideSetPlate, isotype, conjugate, "", "", "Notebook" + 11,
                    "Experimental", "TECH" + (11), "",  TEST_ASSAY_LUM_FILE9.toString(), 6);
        uncheckCheckbox("_titrationRole_standard_HIVIG");
        checkCheckbox("_titrationRole_qccontrol_HIVIG");
        clickNavButton("Save and Finish");


    }

    private void verifyQCFlags()
    {
        goToProjectHome();
        clickLinkWithText(TEST_ASSAY_LUM);
        verifyQCFlagsInRunGrid();
        verifyQCFlagsSchema();
    }

    private void verifyQCFlagsSchema()
    {
        goToSchemaBrowser();
        selectQuery("assay", TEST_ASSAY_LUM + " QCFlags");
        waitForText("assay." + TEST_ASSAY_LUM + " QCFlags");
    }

    String[] expectedFlags = {"AUC, EC50, HMFI, PCV","AUC, EC50, HMFI", "HMFI", "", "PCV"};

    private void verifyQCFlagsInRunGrid()
    {
        //add QC flag colum
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "QCFlags");
        CustomizeViewsHelper.saveCustomView(this);

        //verify expected values in column
        String[] flags = getColumnValues(TEST_ASSAY_LUM + " Runs", "QC Flags").get(0).toArray(new String[] {});
        for(int i=0; i<flags.length; i++)
        {
            assertEquals(expectedFlags[i], flags[i]);
        }
        verifyQCFlagLink();
    }

    private void verifyQCFlagLink()
    {
        clickLinkContainingText(expectedFlags[0], 0, false);
        waitForExtMask();
         sleep(500);
        assertTextPresent("CV", 4); // 3 occurances of PCV and 1 of %CV

        //verify text is in expected form
        waitForText("HIVIG GS Analyte (1) - " + isotype + " " + conjugate + " under threshold for AUC");

        //verify unchecking a box  removes the flag
        Locator aucCheckBox = Locator.xpath("//div[text()='AUC']/../../td/div/div[contains(@class, 'check')]");
        clickAt(aucCheckBox,  "1,1");
        clickButton("Save", 0);
        waitForExtMaskToDisappear();
        waitForPageToLoad();

        Locator strikeoutAUC = Locator.xpath("//span[contains(@style, 'line-through') and  text()='AUC']");
        isElementPresent(strikeoutAUC);

        //verify rechecking a box adds the flag back
        waitForText(expectedFlags[0]);
        clickAtAndWait(strikeoutAUC, 0, "1,1");
        waitForExtMask();
//        waitForPageToLoad();
        waitForElement(aucCheckBox, defaultWaitForPage);
        clickAt(aucCheckBox,  "1,1");

        clickButton("Save", 0);
        waitForExtMaskToDisappear();
        waitForPageToLoad();
        assertElementNotPresent(Locator.xpath("//span[contains(@style, 'line-through') and  text()='AUC']"));
    }

    private void verifyQCAnalysis()
    {
        goToQCAnalysisPage();
        verifyQCReport();
    }

    private void goToQCAnalysisPage()
    {
        clickLinkWithText(getProjectName());
        clickLinkWithText(TEST_ASSAY_LUM);

        String qcUrlInRuns = getQCLink();
        clickLinkWithText("view results");
        String qcUrlInResults = getQCLink();
        clickLinkContainingText("view qc report");

    }

    private void verifyQCReport()
    {
        assertTextPresent(TEST_ASSAY_LUM + " QC Report");
        DataRegionTable drt = new DataRegionTable(TEST_ASSAY_LUM + " AnalyteTitration", this);
        String isotype = drt.getDataAsText(0, "Isotype");
        if(isotype.length()==0)
            isotype = "[None]";
        String conjugate = drt.getDataAsText(0, "Conjugate");
        if(conjugate.length()==0)
            conjugate =  "[None]";
        clickLinkWithText("graph",0);
        waitForText(" - " + isotype + " " + conjugate);
        assertTextPresent( "HIVIG Levey-Jennings Report");
    }

    private String getQCLink()
    {
        Locator l = Locator.tagContainingText("a", "view qc report");
        assertElementPresent(l);
        return getAttribute(l,  "href");
    }

    protected void excludeWellFromRun(String run, String well)
    {
        clickLinkContainingText(run);

        log("Exclude well from from run");
        clickExclusionMenuIconForWell(well);
        clickButton("Save");
        waitForExtMaskToDisappear();
    }

    //re-include an excluded well
    protected void includeWellFromRun(String run, String well)
    {
        clickLinkContainingText(run);

        log("Exclude well from from run");
        clickExclusionMenuIconForWell(well);
        clickRadioButtonById("excludeselected");
        clickButton("Save", 0);
//        sleep(1000);
        ExtHelper.clickExtButton(this, "Yes");
        waitForExtMaskToDisappear();
    }


    private void excludableWellsWithTransformTest()
    {
        clickLinkContainingText(getProjectName());
        clickLinkContainingText(TEST_ASSAY_LUM);
        excludeWellFromRun("Guide Set plate 5", "A6,B6");
        goToLeveyJenningsGraphPage("HIVIG");
        setUpGuideSet("GS Analyte (2)");
        assertTextPresent("28040.51");


    }


    private void guideSetApiTest()
    {
        clickLinkContainingText(getProjectName());
        assertTextNotPresent("GS Analyte");
        addWebPart("Wiki");
        createNewWikiPage("HTML");
        setWikiBody(getFileContents("server/test/data/api/LuminexGuideSet.html"));
        saveWikiPage();

        click(Locator.id("button_loadqwps"));
        waitForText("Done loading QWPs");
        assertTextNotPresent("Error:");

        click(Locator.id("button_testiud"));
        waitForText("Done testing inserts, updates, and deletes");
        assertTextNotPresent("Error:");

        click(Locator.id("button_updateCurveFit"));
        waitForText("Done with CurveFit update");
        assertTextNotPresent("Error:");

        click(Locator.id("button_updateGuideSetCurveFit"));
        waitForText("Done with GuideSetCurveFit update");
        assertTextNotPresent("Error:");

        // check the QWPs again to make the inserts/updates/deletes didn't affected the expected row counts
        click(Locator.id("button_loadqwps"));
        waitForText("Done loading QWPs again");
        assertTextNotPresent("Error:");        
    }

    private void verifyLeveyJenningsRplots()
    {
        goToLeveyJenningsGraphPage("HIVIG");
        setUpGuideSet("GS Analyte (2)");

        // check ec50 trending R plot
        click(Locator.tagWithText("span", "EC50"));
        waitForTextToDisappear("Loading");
        assertTextNotPresent("Error");
        assertElementPresent( Locator.id("EC50TrendPlotDiv"));

        // check auc trending R plot
        click(Locator.tagWithText("span", "AUC"));
        waitForTextToDisappear("Loading");
        assertTextNotPresent("Error");
        assertElementPresent( Locator.id("AUCTrendPlotDiv"));

        // check high mfi trending R plot
        click(Locator.tagWithText("span", "High MFI"));
        waitForTextToDisappear("Loading");
        assertTextNotPresent("Error");
        assertElementPresent( Locator.id("High MFITrendPlotDiv"));

        //verify QC flags
        //this locator finds an EC50 flag, then makes sure there's red text outlining
        Locator.XPathLocator l = Locator.xpath("//td/div[contains(@style,'red')]/../../td/div/a[contains(text(),'EC50')]");
        assertElementPresent(l,2);
        assertTextPresent("QC Flags"); //should update to qc flags
    }

    private void verifyGuideSetsNotApplied()
    {
        goToSchemaBrowser();
        selectQuery("assay", TEST_ASSAY_LUM + " AnalyteTitration");
        waitForText("view data");
        clickLinkContainingText("view data");
        DataRegionTable table = new DataRegionTable("query", this);
        table.setFilter("GuideSet/Created", "Is Not Blank", "");
        // check that the table contains one row that reads "No data to show."
        assertEquals("Expected no guide set assignments", 0, table.getDataRowCount());
        table.clearFilter("GuideSet/Created");
    }

    private void verifyGuideSetsApplied(Map<String, Integer> guideSetIds, String[] analytes, int expectedRunCount)
    {

        // see if the 3 uploaded runs got the correct 'current' guide set applied
        goToSchemaBrowser();
        selectQuery("assay", TEST_ASSAY_LUM + " AnalyteTitration");
        waitForText("view data");
        clickLinkContainingText("view data");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Analyte/RowId");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Titration/RowId");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "GuideSet/RowId");
        CustomizeViewsHelper.applyCustomView(this);
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
        selectQuery("assay", TEST_ASSAY_LUM + " GuideSet");
        waitForText("view data");
        clickLinkContainingText("view data");
        Map<String, Integer> guideSetIds = new HashMap<String, Integer>();
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "RowId");
        CustomizeViewsHelper.applyCustomView(this);
        DataRegionTable table = new DataRegionTable("query", this);
        table.setFilter("CurrentGuideSet", "Equals", "true");
        guideSetIds.put(table.getDataAsText(0, "Analyte Name"), Integer.parseInt(table.getDataAsText(0, "Row Id")));
        guideSetIds.put(table.getDataAsText(1, "Analyte Name"), Integer.parseInt(table.getDataAsText(1, "Row Id")));

        return guideSetIds;
    }

    private void createInitialGuideSets()
    {
        setUpGuideSet("GS Analyte (1)");
        createGuideSet("GS Analyte (1)", true);
        editGuideSet(new String[] {"allRunsRow_1", "allRunsRow_0"}, "Analyte 1", true);

        setUpGuideSet("GS Analyte (2)");
        createGuideSet("GS Analyte (2)", true);
        editGuideSet(new String[] {"allRunsRow_1"}, "Analyte 2", true);

        //edit a guide set
        log("attempt to edit guide set after creation");
        clickButtonContainingText("Edit", 0);
        editGuideSet(new String[] {"allRunsRow_0"}, "edited analyte 2", false);
    }

    private void setIsoAndConjugate()
    {
        log("unimplemented");
        ExtHelper.selectComboBoxItem(this,"Isotype", isotype);

        ExtHelper.selectComboBoxItem(this,"Conjugate", conjugate);

    }

    private void setUpGuideSet(String analyte)
    {
        waitForText(analyte);
        Locator l = Locator.tagContainingText("span", analyte);
        clickAt(l, "1,1");

        setIsoAndConjugate();

        l = Locator.extButton("Apply", 0);
        clickAt(l,  "1,1");

        // wait for the test headers in the guide set and tracking data regions
        waitForText(analyte + " - " + isotype + " " + conjugate);
        waitForText("HIVIG Tracking Data for " + analyte + " - " + isotype + " " + conjugate);
        waitForTextToDisappear("Loading");
        assertTextNotPresent("Error");
    }

    private void addRemoveGuideSetRuns(String[] rows)
    {
        for(String row: rows)
        {
            Locator l = Locator.id(row);
            waitForElement(l, defaultWaitForPage);
            click(Locator.tagWithId("span", row));
        }

    }
    private void createGuideSet(String analyte, boolean initialGuideSet)
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

    private void editGuideSet(String[] rows, String comment, boolean creating)
    {
        String today = df.format(Calendar.getInstance().getTime());

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


        addRemoveGuideSetRuns(rows);
        setText("commentTextField", comment);

        if (creating)
        {
            assertElementNotPresent(Locator.button("Save"));
            assertElementPresent(Locator.button("Create"));
            clickButton("Create",0);
        }
        else
        {
            assertElementNotPresent(Locator.button("Create"));
            assertElementPresent(Locator.button("Save"));
            clickButton("Save",0);
        }
        waitForGuideSetExtMaskToDisappear();

        waitForText("Created: " + today + "; Comment: " + comment);
    }

    private void waitForGuideSetExtMaskToDisappear()
    {

        waitForExtMaskToDisappear();
        waitForTextToDisappear("Loading");
        assertTextNotPresent("Error");
    }

    private void goToLeveyJenningsGraphPage(String titrationName)
    {
        goToSchemaBrowser();
        selectQuery("assay", TEST_ASSAY_LUM + " Titration");
        waitForText("view data");
        clickLinkContainingText("view data");
        clickLinkContainingText(titrationName);
        waitForText(titrationName + " Levey-Jennings Report");
    }

    private void verifyGuideSetThresholds(Map<String, Integer> guideSetIds, String[] analytes, int[] rowCounts, double[] ec50Averages, double[] ec50StdDevs, double[] aucAverages, double[] aucStdDevs)
    {
        // go to the GuideSetCurveFit table to verify the calculated threshold values for the EC50 and AUC
        goToSchemaBrowser();
        selectQuery("assay", TEST_ASSAY_LUM + " GuideSetCurveFit");
        waitForText("view data");
        clickLinkContainingText("view data");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "GuideSetId/RowId");
        CustomizeViewsHelper.applyCustomView(this);
        DataRegionTable table = new DataRegionTable("query", this);
        for (int i = 0; i < analytes.length; i++)
        {
            // verify the row count, average, and standard deviation for the EC50 values
            table.setFilter("GuideSetId/RowId", "Equals", guideSetIds.get(analytes[i]).toString());
            table.setFilter("CurveType", "Equals", "Four Parameter");
            assertEquals("Unexpected row count for guide set " + guideSetIds.get(analytes[i]).toString(), rowCounts[i], Integer.parseInt(table.getDataAsText(0, "Run Count")));
            assertEquals("Unexpected EC50 average for guide set " + guideSetIds.get(analytes[i]).toString(), ec50Averages[i], Double.parseDouble(table.getDataAsText(0, "EC50Average")));
            assertEquals("Unexpected EC50 StdDev for guide set " + guideSetIds.get(analytes[i]).toString(), ec50StdDevs[i], Double.parseDouble(table.getDataAsText(0, "EC50Std Dev")));
            table.clearFilter("CurveType");
            // verify the row count, average, and standard deviation for the AUC values
            table.setFilter("CurveType", "Equals", "Trapezoidal");
            assertEquals("Unexpected row count for guide set " + guideSetIds.get(analytes[i]).toString(), rowCounts[i], Integer.parseInt(table.getDataAsText(0, "Run Count")));
            assertEquals("Unexpected AUC average for guide set " + guideSetIds.get(analytes[i]).toString(), aucAverages[i]	, Double.parseDouble(table.getDataAsText(0, "AUCAverage")));
            assertEquals("Unexpected AUC StdDev for guide set " + guideSetIds.get(analytes[i]).toString(), aucStdDevs[i], Double.parseDouble(table.getDataAsText(0, "AUCStd Dev")));
            table.clearFilter("CurveType");
            table.clearFilter("GuideSetId/RowId");
        }
    }

    private void applyGuideSetToRun(String network, int runRowIndex, String comment, int guideSetIndex)
    {
        clickAt(ExtHelper.locateBrowserFileCheckbox(network), "1," + runRowIndex);
        clickButton("Apply Guide Set", 0);
        sleep(1000);//we need a little time even after all the elements have appeared, so waits won't work

        if(guideSetIndex!=-1) //not clicking anything will apply the current guide set
            clickAt(ExtHelper.locateBrowserFileCheckbox(comment), "1," + guideSetIndex);

        waitAndClick(5000, getButtonLocator("Apply Thresholds"), 0);
        waitForExtMaskToDisappear();
        // verify that the plot is reloaded
        waitForTextToDisappear("Loading");
        assertTextNotPresent("Error");

    }
    private void verifyGuideSetToRun(String network, int networkColIndex, String comment, int commentColIndex)
    {
        clickAt(ExtHelper.locateBrowserFileCheckbox(network), "1," + networkColIndex);
        clickButton("Apply Guide Set", 0);
        waitForElement(ExtHelper.locateBrowserFileCheckbox(network), defaultWaitForPage);
        waitForElement(ExtHelper.locateBrowserFileCheckbox(comment), defaultWaitForPage);
        // deselect the current guide set to test error message
        clickAt(ExtHelper.locateBrowserFileCheckbox(comment), "1," + commentColIndex);
        clickButton("Apply Thresholds", 0);
        waitForText("Please select a guide set to be applied to the selected records.");
        clickButton("OK", 0);
        // reselect the current guide set and apply it
        clickAt(ExtHelper.locateBrowserFileCheckbox(comment), "1," + commentColIndex);
        waitAndClick(5000, getButtonLocator("Apply Thresholds"), 0); 
        waitForExtMaskToDisappear();
        // verify that the plot is reloaded
        waitForTextToDisappear("Loading");
        assertTextNotPresent("Error");
    }

    private void applyStartAndEndDateFilter()
    {
        String colValuePrefix = "NETWORK";
        int columnIndex = 2;

        setUpGuideSet("GS Analyte (2)");
        // check that all 5 runs are present in the grid by clicking on them
        for (int i = 5; i > 0; i--)
        {
            clickAt(ExtHelper.locateBrowserFileCheckbox(colValuePrefix + i), i+","+columnIndex);
        }
        // set start and end date filter
        setFormElement("start-date-field", "2011-03-26");
        setFormElement("end-date-field", "2011-03-28");
        // click a different element on the page to trigger the date change event
        clickAt(ExtHelper.locateBrowserFileCheckbox(colValuePrefix + "5"), "1,"+columnIndex);
        Locator l = Locator.extButton("Apply", 1);
        clickAt(l,  "1,1");
        waitForTextToDisappear("Loading");
        assertTextNotPresent("Error");
        // check that only 3 runs are now present
        for (int i = 4; i > 1; i--)
        {
            clickAt(ExtHelper.locateBrowserFileCheckbox(colValuePrefix + i), (i-3)+","+columnIndex);
        }
        assertElementNotPresent(ExtHelper.locateBrowserFileCheckbox(colValuePrefix + "5"));
        assertElementNotPresent(ExtHelper.locateBrowserFileCheckbox(colValuePrefix + "1"));
    }

    private void applyLogYAxisScale()
    {
        setUpGuideSet("GS Analyte (2)");
        ExtHelper.selectComboBoxItem(this, Locator.xpath("//input[@id='scale-combo-box']/.."), "Log");
        waitForTextToDisappear("Loading");
        assertTextNotPresent("Error");
    }

    private boolean verifyRunFileAssociations(boolean displayingRowId, int index)
    {
        // verify that the PDF of curves file was generated along with the xls file and the Rout file
        if (!displayingRowId)
        {
            CustomizeViewsHelper.openCustomizeViewPanel(this);
            CustomizeViewsHelper.addCustomizeViewColumn(this, "RowId");
            CustomizeViewsHelper.applyCustomView(this);
            displayingRowId = true;
        }
        DataRegionTable table = new DataRegionTable(TEST_ASSAY_LUM + " Runs", this);
        clickLinkWithText(table.getDataAsText(0, "Row Id"));
        assertLinkPresentWithTextCount("Guide Set plate " + index + ".HIVIG_QC_Curves.pdf", 3);
        assertLinkPresentWithTextCount("Guide Set plate " + index + ".xls", 3);
        assertLinkPresentWithTextCount("Guide Set plate " + index + ".tomaras_luminex_transform.Rout", 3);

        return true;
    }
}
