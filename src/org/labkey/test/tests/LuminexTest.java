/*
 * Copyright (c) 2007-2011 LabKey Corporation
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
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.RReportHelper;
import static org.labkey.test.util.ListHelper.ListColumnType;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.ExtHelper;

import java.io.File;
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

    protected static final String TEST_ASSAY_LUM = "TestAssayLuminex";
    protected static final String TEST_ASSAY_LUM_DESC = "Description for Luminex assay";

    protected static final String TEST_ASSAY_LUM_ANALYTE_PROP_NAME = "testAssayAnalyteProp";
    protected static final int TEST_ASSAY_LUM_ANALYTE_PROP_ADD = 5;
    protected static final String[] TEST_ASSAY_LUM_ANALYTE_PROP_TYPES = { "Text (String)", "Boolean", "Number (Double)", "Integer", "DateTime" };
    protected static final String TEST_ASSAY_LUM_SET_PROP_SPECIES = "testSpecies1";
    protected static final String TEST_ASSAY_LUM_RUN_NAME = "testRunName1";
    protected static final String TEST_ASSAY_LUM_SET_PROP_SPECIES2 = "testSpecies2";
    protected static final String TEST_ASSAY_LUM_RUN_NAME2 = "testRunName2";
    protected static final String TEST_ASSAY_LUM_RUN_NAME3 = "WithIndices.xls";
    protected final String TEST_ASSAY_LUM_FILE1 = getLabKeyRoot() + "/sampledata/Luminex/10JAN07_plate_1.xls";
    protected final String TEST_ASSAY_LUM_FILE2 = getLabKeyRoot() + "/sampledata/Luminex/pnLINCO20070302A.xlsx";
    protected final String TEST_ASSAY_LUM_FILE3 = getLabKeyRoot() + "/sampledata/Luminex/WithIndices.xls";
    protected final String TEST_ASSAY_LUM_FILE4 = getLabKeyRoot() + "/sampledata/Luminex/WithBlankBead.xls";

    protected final String TEST_ASSAY_MULTIPLE_STANDARDS_1 = getLabKeyRoot() + "/sampledata/Luminex/plate 1_IgA-Biot (b12 IgA std).xls";
    protected final String TEST_ASSAY_MULTIPLE_STANDARDS_2 = getLabKeyRoot() + "/sampledata/Luminex/plate 2_IgA-Biot (b12 IgA std).xls";
    protected final String TEST_ASSAY_MULTIPLE_STANDARDS_3 = getLabKeyRoot() + "/sampledata/Luminex/plate 3_IgG-Biot (HIVIG std).xls";

    protected final String TEST_ASSAY_LUM_ANALYTE_PROP = "testAnalyteProp";
    private static final String THAW_LIST_NAME = "LuminexThawList";
    private static final String TEST_ASSAY_LUM_RUN_NAME4 = "testRunName4";

    private static final String RTRANSFORM_SCRIPT_FILE1 = "/resources/transformscripts/transform_v1.R";
    private static final String RTRANSFORM_SCRIPT_FILE2 = "/resources/transformscripts/blank_bead_subtraction.R";
    private static final String[] RTRANS_FIBKGDBLANK_VALUES = {"1.0", "1.0", "25031.5", "25584.5", "391.5", "336.5", "263.8", "290.8",
            "35.2", "35.2", "63.0", "71.0", "1.0", "1.0", "1.0", "1.0", "1.0", "1.0", "26430.8", "26556.2", "1.0", "1.0", "1.0",
            "1.0", "1.0", "1.0", "194.2", "198.8", "1.0", "1.0", "1.0", "1.0"};
    private static final String[] RTRANS_ESTLOGCONC_VALUES_5PL = {"-6.9", "-6.9", "4.3", "4.3", "0.4", "0.4", "-0.0", "-0.0", "-6.9", "-6.9",
            "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "4.3", "4.3", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9",
            "-6.9", "-0.6", "-0.6", "-6.9", "-6.9", "-6.9", "-6.9"};

    private static final String[] RTRANS_ESTLOGCONC_VALUES_4PL = {"-6.9", "-6.9", "5.0", "5.0", "0.4", "0.4", "0.1", "0.1", "-6.9", "-6.9",
            "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9"};

    public static final String ASSAY_ID_FIELD  = "name";
    public static final String ASSAY_DATA_FILE_LOCATION_MULTIPLE_FIELD = "__primaryFile__";

    public static final String DATA_TABLE_NAME = "dataregion_TestAssayLuminex Data";
    private static final String EXCLUDE_COMMENT_FIELD = "comment";
    public static final String EXCLUDE_SELECTED_BUTTON = "excludeselected";
    protected static final String MULTIPLE_CURVE_ASSAY_RUN_NAME = "multipleCurvesTestRun";
    protected static final String SAVE_CHANGES_BUTTON = "Save Changes";

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

        sleep(1000);
        clickNavButton("Save", 0);
        waitForText("Save successful.", 20000);

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
        File file1 = new File(TEST_ASSAY_LUM_FILE1);
        setFormElement("__primaryFile__", file1);
        clickNavButton("Next", 60000);
        clickNavButton("Save and Import Another Run");
        clickLinkWithText(TEST_ASSAY_LUM);

        clickNavButton("Import Data");
        assertEquals(TEST_ASSAY_LUM_SET_PROP_SPECIES, selenium.getValue("species"));
        setFormElement("species", TEST_ASSAY_LUM_SET_PROP_SPECIES2);
        clickNavButton("Next");
        setFormElement("name", TEST_ASSAY_LUM_RUN_NAME2);
        setFormElement("__primaryFile__", new File(TEST_ASSAY_LUM_FILE2));
        clickNavButton("Next", 60000);
        selenium.type("//input[@type='text' and contains(@name, '_analyte_')][1]", "StandardName1b");
        selenium.type("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text']", "StandardName2");
        selenium.type("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[5]//input[@type='text']", "StandardName4");
        selenium.click("//input[contains(@name,'UnitsOfConcentrationCheckBox')]");
        selenium.type("//input[@type='text' and contains(@name, 'UnitsOfConcentration')]", "10 g/ml");
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
        setFormElement("__primaryFile__", new File(TEST_ASSAY_LUM_FILE3));
        clickNavButton("Next", 60000);
        assertEquals("StandardName1b", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]"));
        assertEquals("StandardName4", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text'][1]"));
        assertEquals("10 g/ml", selenium.getValue("//input[@type='text' and contains(@name, 'UnitsOfConcentration')]"));
        assertEquals("10 g/ml", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text' and contains(@name, 'UnitsOfConcentration')]"));
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
        setFormElement("__primaryFile__", new File(TEST_ASSAY_LUM_FILE3));
        clickNavButton("Next", 60000);
        assertEquals("StandardName1b", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]"));
        assertEquals("StandardName4", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text'][1]"));
        assertEquals("10 g/ml", selenium.getValue("//input[@type='text' and contains(@name, 'UnitsOfConcentration')]"));
        assertEquals("10 g/ml", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text' and contains(@name, 'UnitsOfConcentration')]"));
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

            runJavaTransformTest();
            runRTransformTest();
            runMultipleCurveTest();
            runWellExclusionTest();
        }
    } //doTestSteps()

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
        waitForAjaxLoad();
        waitForText("Well Role");
        clickExclusionMenuIconForWell(wellName);

        waitForAjaxLoad();
        String exclusionComment = "exclude single analyte for single well";
        setText(EXCLUDE_COMMENT_FIELD, exclusionComment);
        clickRadioButtonById(EXCLUDE_SELECTED_BUTTON);
        clickExcludeAnalyteCheckBox(excludedAnalyte, true);
        clickButton(SAVE_CHANGES_BUTTON, 0);

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
        waitForAjaxLoad();
        clickButton(SAVE_CHANGES_BUTTON, 0);
        waitForAjaxLoad();

        excludeForSingleWellVerify("Excluded for replicate group: " + comment, new HashSet<String>(Arrays.asList(getListOfAnalytesMultipleCurveData())));

        //remove exclusions to leave in clean state
        clickExclusionMenuIconForWell(wellName);
        waitForAjaxLoad();
        clickRadioButtonById("excludeselected");
        clickButton(SAVE_CHANGES_BUTTON, 0);
        waitForAjaxLoad();
        clickButton("Yes", 0);
    }

    /**
     * go through every well.  If they match the hardcoded well, description, and dilution values, and one of the analyte values given
     * verify that the row has the expected comment
     *
     * @param expectedComment
     * @param analytes
     */
    private void excludeForSingleWellVerify(String expectedComment, Set<String> analytes)
    {
        List<List<String>> vals = getColumnValues(DATA_TABLE_NAME, "Well", "Description", "Dilution", "Exclusion Comment", "Analyte");
        List<String> wells = vals.get(0);
        List<String> descriptions = vals.get(1);
        List<String> dilutions = vals.get(2);
        List<String> comments = vals.get(3);
        List<String> analytesPresent = vals.get(4);

        String well;
        String description;
        String dilution;
        String comment;
        String analyte;

        for(int i=0; i<wells.size(); i++)
        {
            well = wells.get(i);
            log("well: " + well);
            description= descriptions.get(i);
            log("description: " + description);
            dilution = dilutions.get(i);
            log("dilution: " + dilution);
            comment = comments.get(i);
            log("Comment: "+ comment);
            analyte= analytesPresent.get(i);
            log("Analyte: " + analyte);

            if(matchesWell(description, dilution, well) && analytes.contains(analyte))
            {
                assertEquals(expectedComment,comment);
            }

            if(expectedComment.equals(comment))
            {
                assertTrue(matchesWell(description, dilution, well) && analytes.contains(analyte));
            }
        }
    }

    //verifies if description, dilution, and well match the hardcoded values
    private boolean matchesWell(String description, String dilution, String well)
    {
        if(!excludedWellDescription.equals(description))
            return false;
        if(!excludedWellDilution.equals(dilution))
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
        waitForAjaxLoad();
        clickButtonContainingText("Exclude Analytes");
        waitForText("Exclude Analytes from Analysis");
        waitForAjaxLoad(); //the above wait isn't sufficient, the button still isn't ready
        clickExcludeAnalyteCheckBox(analyte, true);
        String comment = "Changed for all analytes";
        setText(EXCLUDE_COMMENT_FIELD, comment);
        waitForAjaxLoad();
        clickButton(SAVE_CHANGES_BUTTON, 0);

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
        Locator l = Locator.id(getLinkIDFromWellName(wellName));
        click(l);
        waitForAjaxLoad();
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

    private String excludedWellDescription = "Sample 6";
    private String excludedWellDilution = "10.0";
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



        clickButton("Save and Finish", 120000);
        clickLinkWithText(name);

        //edit view to show Analyte Standard
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Analyte/Standard");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Analyte/StdCurve");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Analyte/FitProb");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Analyte/ResVar");
        CustomizeViewsHelper.applyCustomView(this);

        //Issue 12943
//        assertTextPresent("BioPlex curve fit for VRC A 5304 gp140 (62) in plate 3", "FI = 0.465914 + (1.5417E+006 - 0.465914) / ((1 + (Conc / 122.733)^-0.173373))^7.64039");
//        assertTextPresent("BioPlex FitProb for VRC A 5304 gp140 (62) in plate 3", "0.9667");
//        assertTextPresent("BioPlex ResVar for VRC A 5304 gp140 (62) in plate 3", "0.1895");

        compareColumnValuesAgainstExpected("Analyte", "Analyte Standard", analytesAndStandardsConfig);

    }

    private void compareColumnValuesAgainstExpected(String column1, String column2, Map<String, Set<String>> column1toColumn2)
    {
        Set<String> set = new HashSet<String>();
        set.add(column2);
        column1toColumn2.put(column1, set); //column headers

        List<List<String>> columnVals = getColumnValues("dataregion_TestAssayLuminex Data", column1, column2);

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
     * Verify that the "set this  as standard" checkboxes exist and are checked for the given standard names
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
        String[] files = {TEST_ASSAY_MULTIPLE_STANDARDS_1, TEST_ASSAY_MULTIPLE_STANDARDS_2, TEST_ASSAY_MULTIPLE_STANDARDS_3};
        for(int i=0; i<files.length; i++)
        {
            String formName = ASSAY_DATA_FILE_LOCATION_MULTIPLE_FIELD + i;

            setFileValue(formName, files[i]);

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
        waitForPageToLoad();
        clickButtonContainingText("Next");
        waitForPageToLoad();
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
        setFormElement("__primaryFile__", new File(TEST_ASSAY_LUM_FILE1));
        clickNavButton("Next", 60000);
        clickNavButton("Save and Finish");

        // verify the description error was generated by the transform script
        clickLinkWithText("transformed assayId");
        for(int i = 3; i <= 40; i++)
        {
            assertTableCellTextEquals("dataregion_" + TEST_ASSAY_LUM + " Data",  i, "Description", "Transformed");
        }
    }

    //helper function to go to test assay home from anywhere the project link is visible
    private void goToTestAssayHome()
    {
        clickLinkWithText(TEST_ASSAY_PRJ_LUMINEX);
        clickLinkWithText(TEST_ASSAY_LUM);
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

        // add batch properties for transform and Ruminex version numbers
        addField("Batch Fields", 5, "TransformVersion", "Transform Script Version", ListColumnType.String);
        addField("Batch Fields", 6, "RuminexVersion", "Ruminex Version", ListColumnType.String);

        // add a run property for designation of which field to use for curve fit calc in transform
        addField("Run Fields", 5, "SubtBlankFromAll", "Subtract Blank Bead from All Wells", ListColumnType.Boolean);
        addField("Run Fields", 6, "StndCurveFitInput", "Input Var for Curve Fit Calc of Standards", ListColumnType.String);
        addField("Run Fields", 7, "UnkCurveFitInput", "Input Var for Curve Fit Calc of Unknowns", ListColumnType.String);

        // add the data properties for the calculated columns
        addField("Data Fields", 0, "fiBackgroundBlank", "FI-Bkgd-Blank", ListColumnType.Double);
        addField("Data Fields", 1, "Standard", "Stnd for Calc", ListColumnType.String);
        addField("Data Fields", 2, "EstLogConc_5pl", "Est Log Conc Rumi 5 PL", ListColumnType.Double);
        addField("Data Fields", 3, "EstConc_5pl", "Est Conc Rumi 5 PL", ListColumnType.Double);
        addField("Data Fields", 4, "SE_5pl", "SE Rumi 5 PL", ListColumnType.Double);
        addField("Data Fields", 5, "EstLogConc_4pl", "Est Log Conc Rumi 4 PL", ListColumnType.Double);
        addField("Data Fields", 6, "EstConc_4pl", "Est Conc Rumi 4 PL", ListColumnType.Double);
        addField("Data Fields", 7, "SE_4pl", "SE Rumi 4 PL", ListColumnType.Double);

        // set format to two decimal place for easier testing later
        setFormat("Data Fields", 0, "0.0");
        setFormat("Data Fields", 2, "0.0");
        setFormat("Data Fields", 3, "0.0");
        setFormat("Data Fields", 4, "0.0");
        setFormat("Data Fields", 5, "0.0");
        setFormat("Data Fields", 6, "0.0");
        setFormat("Data Fields", 7, "0.0");

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
        setFormElement("__primaryFile__", new File(TEST_ASSAY_LUM_FILE4));
        clickNavButton("Next", 60000);
        clickNavButton("Save and Finish");

        // verify that the PDF of curves was generated
        Locator l = Locator.tagWithAttribute("img", "src", "/labkey/_images/sigmoidal_curve.png");
        click(l);
        assertLinkPresentWithText("WithBlankBead.HIVIG_5PL.pdf");
        assertLinkPresentWithText("WithBlankBead.HIVIG_4PL.pdf");

        // verfiy that the calculated values were generated by the transform script as expected
        clickLinkWithText("r script transformed assayId");
        setFilter(TEST_ASSAY_LUM + " Data", "fiBackgroundBlank", "Is Not Blank");
        assertTextPresent("1 - 32 of 32");
        // check values in the fi-bkgd-blank column
        for(int i = 0; i < RTRANS_FIBKGDBLANK_VALUES.length; i++)
        {
            assertTableCellTextEquals("dataregion_" + TEST_ASSAY_LUM + " Data",  i+2, "FI-Bkgd-Blank", RTRANS_FIBKGDBLANK_VALUES[i]);
        }
        clearFilter(TEST_ASSAY_LUM + " Data", "fiBackgroundBlank");
        setFilter(TEST_ASSAY_LUM + " Data", "EstLogConc_5pl", "Is Not Blank");
        assertTextPresent("1 - 32 of 32");
        // check values in the est log conc 5pl column
        for(int i = 0; i < RTRANS_ESTLOGCONC_VALUES_5PL.length; i++)
        {
            assertTableCellTextEquals("dataregion_" + TEST_ASSAY_LUM + " Data",  i+2, "Est Log Conc Rumi 5 PL", RTRANS_ESTLOGCONC_VALUES_5PL[i]);
        }
        clearFilter(TEST_ASSAY_LUM + " Data", "EstLogConc_5pl");
        setFilter(TEST_ASSAY_LUM + " Data", "EstLogConc_4pl", "Is Not Blank");
        assertTextPresent("1 - 16 of 16");
        // check values in the est log conc 4pl column
        for(int i = 0; i < RTRANS_ESTLOGCONC_VALUES_4PL.length; i++)
        {
            assertTableCellTextEquals("dataregion_" + TEST_ASSAY_LUM + " Data",  i+2, "Est Log Conc Rumi 4 PL", RTRANS_ESTLOGCONC_VALUES_4PL[i]);
        }
    }

    private void setFormat(String where, int index, String formatStr)
    {
        String prefix = getPropertyXPath(where);
        ListHelper.clickRow(this, prefix, index);
        click(Locator.xpath(prefix + "//span[contains(@class,'x-tab-strip-text') and text()='Format']"));
        setFormElement("propertyFormat", formatStr);
    }
}
