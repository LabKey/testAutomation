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

package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.RReportHelper;
import static org.labkey.test.util.ListHelper.ListColumnType;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

/**
 * User: jeckels
 * Date: Nov 20, 2007
 */
public class LuminexTest extends AbstractQCAssayTest
{
    private final static String TEST_ASSAY_PRJ_LUMINEX = "Luminex Test";            //project for luminex test

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
    protected final String TEST_ASSAY_LUM_ANALYTE_PROP = "testAnalyteProp";
    private static final String THAW_LIST_NAME = "LuminexThawList";
    private static final String TEST_ASSAY_LUM_RUN_NAME4 = "testRunName4";

    private static final String RTRANSFORM_SCRIPT_FILE1 = "/resources/transformscripts/transform_v1.R";
    private static final String RTRANSFORM_SCRIPT_FILE2 = "/resources/transformscripts/blank_bead_subtraction.R";
    private static final String[] RTRANS_FIBKGDBLANK_VALUES = {"1.0", "1.0", "25031.5", "25584.5", "391.5", "336.5", "263.8", "290.8",
            "35.2", "35.2", "63.0", "71.0", "1.0", "1.0", "1.0", "1.0", "1.0", "1.0", "26430.8", "26556.2", "1.0", "1.0", "1.0",
            "1.0", "1.0", "1.0", "194.2", "198.8", "1.0", "1.0", "1.0", "1.0"};
    private static final String[] RTRANS_ESTLOGCONC_VALUES = {"-6.9", "-6.9", "4.3", "4.4", "0.5", "0.3", "-0.1", "0.0", "-6.9", "-6.9",
            "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "4.3", "4.3", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9",
            "-6.9", "-0.6", "-0.6", "-6.9", "-6.9", "-6.9", "-6.9"};

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/luminex";
    }

    /**
     * Performs Luminex designer/upload/publish.
     */
    protected void runUITests()
    {
        log("Starting Assay BVT Test");

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

        ListHelper.ListColumn participantCol = new ListHelper.ListColumn("ParticipantID", "ParticipantID", ListHelper.ListColumnType.String, "Participant ID");
        ListHelper.ListColumn visitCol = new ListHelper.ListColumn("VisitID", "VisitID", ListHelper.ListColumnType.Double, "Visit id");
        ListHelper.createList(this, TEST_ASSAY_PRJ_LUMINEX, THAW_LIST_NAME, ListHelper.ListColumnType.String, "Index", participantCol, visitCol);
        ListHelper.uploadData(this, TEST_ASSAY_PRJ_LUMINEX, THAW_LIST_NAME, "Index\tParticipantID\tVisitID\n" +
                "1\tListParticipant1\t1001.1\n" +
                "2\tListParticipant2\t1001.2\n" +
                "3\tListParticipant3\t1001.3\n" +
                "4\tListParticipant4\t1001.4");
        clickLinkWithText(TEST_ASSAY_PRJ_LUMINEX);

        clickLinkWithText("Assay List");
        clickLinkWithText(TEST_ASSAY_LUM);

        if(isFileUploadAvailable())
        {
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
            selenium.click("//input[contains(@name,'unitsOfConcentrationCheckBox')]");
            selenium.type("//input[@type='text' and contains(@name, 'unitsOfConcentration')]", "10 g/ml");
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
            assertEquals("10 g/ml", selenium.getValue("//input[@type='text' and contains(@name, 'unitsOfConcentration')]"));
            assertEquals("10 g/ml", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text' and contains(@name, 'unitsOfConcentration')]"));
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
            assertEquals("10 g/ml", selenium.getValue("//input[@type='text' and contains(@name, 'unitsOfConcentration')]"));
            assertEquals("10 g/ml", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text' and contains(@name, 'unitsOfConcentration')]"));
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

            runJavaTransformTest();
            runRTransformTest();
        }
    } //doTestSteps()


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

        clickLinkWithText(TEST_ASSAY_PRJ_LUMINEX);
        clickLinkWithText(TEST_ASSAY_LUM);
        click(Locator.linkWithText("manage assay design"));
        clickLinkWithText("edit assay design");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerTransformScript']"), WAIT_FOR_JAVASCRIPT);

        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), "/sampledata/qc/transform.jar"));
        clickNavButton("Save & Close");

        clickLinkWithText(TEST_ASSAY_PRJ_LUMINEX);
        clickLinkWithText(TEST_ASSAY_LUM);
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

    protected void runRTransformTest()
    {
        log("Uploading Luminex run with a R transform script");

        // add the R transform script to the assay
        clickLinkWithText(TEST_ASSAY_PRJ_LUMINEX);
        clickLinkWithText(TEST_ASSAY_LUM);
        click(Locator.linkWithText("manage assay design"));
        clickLinkWithText("edit assay design");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerTransformScript']"), WAIT_FOR_JAVASCRIPT);
        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE1));

        // add a run property for designation of which field to use for curve fit calc in transform
        addField("Run Fields", 5, "UnkCurveFitInput", "Input Var for Curve Fit Calc of Unknowns", ListColumnType.String);

        // add the data properties for the calculated columns
        addField("Data Fields", 0, "fiBackgroundBlank", "FI-Bkgd-Blank", ListColumnType.Double);
        addField("Data Fields", 1, "estLogConc", "Est Log Conc", ListColumnType.Double);
        addField("Data Fields", 2, "estConc", "Est Conc", ListColumnType.Double);
        addField("Data Fields", 3, "se", "SE", ListColumnType.Double);

        // set format to two decimal place for easier testing later
        setFormat("Data Fields", 0, "0.0");
        setFormat("Data Fields", 1, "0.0");
        setFormat("Data Fields", 2, "0.0");
        setFormat("Data Fields", 3, "0.0");

        // save changes to assay design
        clickNavButton("Save & Close");

        // upload the sample data file
        clickLinkWithText(TEST_ASSAY_PRJ_LUMINEX);
        clickLinkWithText(TEST_ASSAY_LUM);
        clickNavButton("Import Data");
        clickNavButton("Next");
        setFormElement("name", "r script transformed assayId");
        setFormElement("unkCurveFitInput", "FI-Bkgd-Blank");
        setFormElement("__primaryFile__", new File(TEST_ASSAY_LUM_FILE4));
        clickNavButton("Next", 60000);
        clickNavButton("Save and Finish");

        // verify that the PDF of curves was generated
        // TODO: add check for existance of PDF file in the Data Outputs section for the run

        // verify that the Excel Run Properties were successfully passed through to the transform
        // TODO: add check for the run properties that come from the excel file

        // verfiy that the calculated values were generated by the transform script
        clickLinkWithText("r script transformed assayId");
        setFilter(TEST_ASSAY_LUM + " Data", "fiBackgroundBlank", "Is Not Blank");
        assertTextPresent("1 - 32 of 32");
        // check values in the fi-bkgd-blank column
        for(int i = 0; i < RTRANS_FIBKGDBLANK_VALUES.length; i++)
        {
            assertTableCellTextEquals("dataregion_" + TEST_ASSAY_LUM + " Data",  i+2, "FI-Bkgd-Blank", RTRANS_FIBKGDBLANK_VALUES[i]);
        }
        clearFilter(TEST_ASSAY_LUM + " Data", "fiBackgroundBlank");
        setFilter(TEST_ASSAY_LUM + " Data", "estLogConc", "Is Not Blank");
        assertTextPresent("1 - 32 of 32");
        // check values in the est log conc column
        for(int i = 0; i < RTRANS_ESTLOGCONC_VALUES.length; i++)
        {
            assertTableCellTextEquals("dataregion_" + TEST_ASSAY_LUM + " Data",  i+2, "Est Log Conc", RTRANS_ESTLOGCONC_VALUES[i]);
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
