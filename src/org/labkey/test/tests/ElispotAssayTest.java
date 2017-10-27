/*
 * Copyright (c) 2011-2017 LabKey Corporation
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

import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyB;
import org.labkey.test.components.CrosstabDataRegion;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.PlateSummary;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.NoSuchElementException;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.labkey.test.components.PlateSummary.Row.A;
import static org.labkey.test.components.PlateSummary.Row.C;
import static org.labkey.test.components.PlateSummary.Row.E;
import static org.labkey.test.components.PlateSummary.Row.G;

@Category({DailyB.class, Assays.class})
public class ElispotAssayTest extends AbstractQCAssayTest
{
    private final static String TEST_ASSAY_PRJ_ELISPOT = "Elispot Test Verify Project";

    protected static final String TEST_ASSAY_ELISPOT = "TestAssayElispot";
    protected static final String TEST_ASSAY_ELISPOT_DESC = "Description for Elispot assay";

    protected final String TEST_ASSAY_ELISPOT_FILE1 = TestFileUtils.getLabKeyRoot() + "/sampledata/Elispot/CTL_040A20042503-0001p.xls";
    protected final String TEST_ASSAY_ELISPOT_FILE2 = TestFileUtils.getLabKeyRoot() + "/sampledata/Elispot/AID_0161456 W4.txt";
    protected final String TEST_ASSAY_ELISPOT_FILE3 = TestFileUtils.getLabKeyRoot() + "/sampledata/Elispot/Zeiss_datafile.txt";
    protected final String TEST_ASSAY_ELISPOT_FILE4 = TestFileUtils.getLabKeyRoot() + "/sampledata/Elispot/AID_0161456 W5.txt";
    protected final String TEST_ASSAY_ELISPOT_FILE5 = TestFileUtils.getLabKeyRoot() + "/sampledata/Elispot/AID_0161456 W8.txt";
    protected final String TEST_ASSAY_ELISPOT_FILE6 = TestFileUtils.getLabKeyRoot() + "/sampledata/Elispot/AID_TNTC.txt";

    private static final String PLATE_TEMPLATE_NAME = "ElispotAssayTest Template";

    protected static final String TEST_ASSAY_FLUOROSPOT = "TestAssayFluorospot";
    protected static final String TEST_ASSAY_FLUOROSPOT_DESC = "Description for Fluorospot assay";

    protected final String TEST_ASSAY_FLUOROSPOT_FILENAME1 = "AID_fluoro2.xlsx";
    protected final String TEST_ASSAY_FLUOROSPOT_FILENAME2 = "AID_fluoro5.xlsx";
    protected final String TEST_ASSAY_FLUOROSPOT_FILE1 = TestFileUtils.getLabKeyRoot() + "/sampledata/Elispot/" + TEST_ASSAY_FLUOROSPOT_FILENAME1;
    protected final String TEST_ASSAY_FLUOROSPOT_FILE2 = TestFileUtils.getLabKeyRoot() + "/sampledata/Elispot/" + TEST_ASSAY_FLUOROSPOT_FILENAME2;
    private static final String FLUOROSPOT_DETECTION_METHOD = "fluorescent";

    public static final String STUDY_FOLDER = "Fluorospot";

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("nab");
    }

    @Override
    protected String getProjectName()
    {
        return TEST_ASSAY_PRJ_ELISPOT;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @BeforeClass
    public static void initProject() throws Exception
    {
        ElispotAssayTest init = (ElispotAssayTest)getCurrentTest();
        init.setupFolder();
    }

    @Before
    public void preTest()
    {
        // There is a jscript error that happens in at this point in the fluorospotTests if it is run immediately after the elispotTests
        // This error doesn't occur if the test is slowed down, which would imply it is a timing issue.
        // Because of that flakeyness I'm going to pause the js error checking for now.
        pauseJsErrorChecker();
        goToProjectHome();
        resumeJsErrorChecker();
    }

    /**
     * Performs ELISpot designer/upload/publish.
     */
    @Test
    public void elispotTests()
    {
        log("Starting Elispot Assay BVT Test");

        //create a new elispot assay
        clickProject(TEST_ASSAY_PRJ_ELISPOT);
        clickButton("Manage Assays");
        clickButton("New Assay Design");
        checkCheckbox(Locator.radioButtonByNameAndValue("providerName", "ELISpot"));
        clickButton("Next");

        log("Setting up Elispot assay");

        AssayDesignerPage assayDesigner = new AssayDesignerPage(this.getDriver());
        assayDesigner.setName(TEST_ASSAY_ELISPOT);
        assayDesigner.setPlateTemplate(PLATE_TEMPLATE_NAME);
        assayDesigner.setDescription(TEST_ASSAY_ELISPOT_DESC);
        assayDesigner.saveAndClose();

        clickProject(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISPOT));

        log("Uploading Elispot Runs");
        clickButton("Import Data");
        clickButton("Next");

        selectOptionByText(Locator.name("plateReader"), "Cellular Technology Ltd. (CTL)");
        uploadFile(TEST_ASSAY_ELISPOT_FILE1, "A", "Save and Import Another Run", false);
        assertTextPresent("Upload successful.");

        selectOptionByText(Locator.name("plateReader"), "AID");
        uploadFile(TEST_ASSAY_ELISPOT_FILE2, "B", "Save and Import Another Run", false);
        assertTextPresent("Upload successful.");

        selectOptionByText(Locator.name("plateReader"), "Zeiss");
        uploadFile(TEST_ASSAY_ELISPOT_FILE3, "C", "Save and Finish", false);

        assertElispotData();
        runTransformTest();
        doBackgroundSubtractionTest();
        testTNTCdata();
    }

    @Test
    public void fluorospotTests()
    {
        log("** Initialize Study Folder");
        _containerHelper.createSubfolder(getProjectName(), getProjectName(), STUDY_FOLDER, "Study", null);
        clickButton("Create Study");
        clickButton("Create Study");

        //create a new fluorospot assay
        clickProject(TEST_ASSAY_PRJ_ELISPOT);
        clickButton("Manage Assays");
        clickButton("New Assay Design");
        checkCheckbox(Locator.radioButtonByNameAndValue("providerName", "ELISpot"));
        clickButton("Next");

        log("Setting up Fluorospot assay");

        AssayDesignerPage assayDesigner = new AssayDesignerPage(this.getDriver());
        assayDesigner.setName(TEST_ASSAY_FLUOROSPOT);
        assayDesigner.setPlateTemplate(PLATE_TEMPLATE_NAME);
        assayDesigner.setDescription(TEST_ASSAY_FLUOROSPOT_DESC);
        assayDesigner.setDetectionMethod(FLUOROSPOT_DETECTION_METHOD);
        assayDesigner.saveAndClose();

        clickProject(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_FLUOROSPOT));

        log("Uploading Fluorospot Runs");
        clickButton("Import Data");
        String projectFolder = "/" + getProjectName() + "/" + STUDY_FOLDER;
        String targetStudy = projectFolder + " (" + STUDY_FOLDER + " Study)";
        selectOptionByText(Locator.name("targetStudy"), targetStudy);
        clickButton("Next");
        selectOptionByText(Locator.name("plateReader"), "AID");
        uploadFluorospotFile(TEST_ASSAY_FLUOROSPOT_FILE1, "F1", "Save and Import Another Run");
        assertTextPresent("Upload successful.");

        selectOptionByText(Locator.name("plateReader"), "AID");
        uploadFluorospotFile(TEST_ASSAY_FLUOROSPOT_FILE2, "F2", "Save and Finish");

        clickAndWait(Locator.linkContainingText("AID_fluoro2"));

        verifyDataRegion(new DataRegionTable("Data", this), SortDirection.ASC,
                Arrays.asList("0.0", "1.0", "0.0", "1.0", "0.0", "0.0", "0.0", "2.0",  "0.0", "0.0"),       // spot count
                Arrays.asList("0.0", "25.0", "0.0", "22.0", "0.0", "0.0", "0.0", "48.0",  "0.0", "0.0"),    // activity
                Arrays.asList("0.0", "84.0", "0.0", "68.0", "0.0", "0.0", "0.0", "74.0",  "0.0", "0.0"),   // intensity
                Arrays.asList("Cytokine 1","Cytokine 1","Cytokine 1","Cytokine 1","Cytokine 1","Cytokine 1","Cytokine 1","Cytokine 1","Cytokine 1","Cytokine 1")); //cytokines
        assertTextPresent("ptid 1 F1", "ptid 2 F1", "ptid 3 F1", "ptid 4 F1", "atg_1F1", "Antigen 7", "Antigen 8", "Cy3", "FITC");

        clickAndWait(Locator.linkWithText("view runs"));
        clickAndWait(Locator.linkContainingText("AID_fluoro5"));

        verifyDataRegion(new DataRegionTable("Data", this), SortDirection.DESC,
                Arrays.asList("0.0", "1.0", "0.0", "0.0", "2.0", "0.0", "0.0", "3.0", "3.0", "0.0"),        // spot count
                Arrays.asList(" ", " ", " ", " ", " ", " ", " ", " ", " ", " "),                            // activity
                Arrays.asList(" ", " ", " ", " ", " ", " ", " ", " ", " ", " "),                           // intensity
                Arrays.asList(" "," "," "," "," "," "," "," "," "," ")); //cytokines
        assertTextPresent("ptid 1 F2", "ptid 2 F2", "ptid 3 F2", "ptid 4 F2", "Antigen 5", "Antigen 6", "Cy3", "FITC");
        clickAndWait(Locator.linkWithText("view runs"));  // clickAndWait to try and avoid a js error that shows up on TC.
        waitAndClick(Locator.linkWithText("run details"));
        PlateSummary plateSummary = new PlateSummary(this, 3);
        assertEquals(Arrays.asList("244.0","544.0","210.0","449.0","333.0","429.0","393.0","689.0","400.0","159.0","130.0","94.0"), plateSummary.getRowValues(E));
        plateSummary.selectMeasurement(PlateSummary.Measurement.ACTIVITY);
        assertEquals(Arrays.asList("668.0","1610.0","1464.0","3945.0","3781.0","3703.0","8713.0","2222.0","2856.0","1208.0","880.0","1006.0"), plateSummary.getRowValues(G));
        clickAndWait(Locator.linkWithText("view runs")); // clickAndWait to try and avoid a js error that shows up on TC.
        waitAndClick(Locator.linkWithText("view results"));
        DataRegionTable results = new DataRegionTable("Data", this);
        results.ensureColumnsPresent("Wellgroup Name", "Antigen Wellgroup Name", "Antigen Name", "Cells per Well", "Wellgroup Location", "Spot Count", "Normalized Spot Count", "Spot Size", "Analyte", "Cytokine", "Activity", "Intensity", "Specimen ID", "Participant ID", "Visit ID", "Date", "Sample Description", "ProtocolName", "Plate Reader", "Target Study");
        assertEquals(Arrays.asList("Specimen 4", "Antigen 6", "atg_6F2", "150", "(7, 8)", "0.0", "0.0", " ", "FITC+Cy5", " ", " ", " ", " ", "ptid 4 F2", "4.0", " ", "blood", " ", "AID", "Fluorospot Study"), results.getRowDataAsText(0));

        DataRegionTable table = new DataRegionTable("Data", this);

        table.setFilter("WellgroupName", "Equals One Of (example usage: a;b;c)", "Specimen 4");
        table.setFilter("WellgroupLocation", "Equals One Of (example usage: a;b;c)", "(7, 8)");
        table.checkAllOnPage();

        clickButton("Copy to Study");
        assertTextPresent("All data is marked for copying to study");
        assertTextPresent(STUDY_FOLDER);
    }

    private void verifyDataRegion(DataRegionTable table, SortDirection sortDir, List<String> expectedSpotCount, List<String> expectedActivity, List<String> expectedIntensity, List<String> expectedCytokine)
    {
        log("add the analyte field to the table and adding sorts");
        CustomizeView cvHelper = table.getCustomizeView();
        cvHelper.openCustomizeViewPanel();
        cvHelper.showHiddenItems();
        cvHelper.addColumn("Analyte");

        cvHelper.removeSort("AntigenLsid/AntigenName");
        cvHelper.removeSort("Analyte");
        cvHelper.removeSort("WellgroupLocation");

        cvHelper.addSort("AntigenLsid/AntigenName", "AntigenName", sortDir);
        cvHelper.addSort("Analyte", "Analyte", sortDir);
        cvHelper.addSort("WellgroupLocation", "WellgroupLocation", sortDir);
        cvHelper.applyCustomView();

        pushLocation();
        {
            assert expectedSpotCount.size() == expectedActivity.size();
            assert expectedSpotCount.size() == expectedIntensity.size();

            table.setMaxRows(expectedSpotCount.size());
            List<String> spotCount = table.getColumnDataAsText("SpotCount");
            List<String> activity = table.getColumnDataAsText("Activity");
            List<String> intensity = table.getColumnDataAsText("Intensity");
            List<String> cytokine = table.getColumnDataAsText("Cytokine");

            assertEquals("Wrong spot count", expectedSpotCount, spotCount);
            assertEquals("Wrong activity", expectedActivity, activity);
            assertEquals("Wrong intensity", expectedIntensity, intensity);
            assertEquals("Wrong cytokine", expectedCytokine, cytokine);
        }
        popLocation();
    }

    @LogMethod
    protected void setupFolder() throws Exception
    {
        //revert to the admin user
        ensureSignedInAsPrimaryTestUser();

        log("Testing Elispot Assay Designer");

        // set up a scripting engine to run a java transform script
        prepareProgrammaticQC();

        //create a new test project
        _containerHelper.createProject(TEST_ASSAY_PRJ_ELISPOT, null);

        //setup a pipeline for it
        setupPipeline(TEST_ASSAY_PRJ_ELISPOT);

        //add the Assay List web part so we can create a new elispot assay
        clickProject(TEST_ASSAY_PRJ_ELISPOT);
        new PortalHelper(this).addWebPart("Assay List");

        //create a new elispot template
        createTemplate();
    }

    protected void uploadFluorospotFile(String filePath, String uniqueifier, String finalButton)
    {
        uploadFile(filePath, uniqueifier, finalButton, false, false, true);
    }

    protected void uploadFile(String filePath, String uniqueifier, String finalButton, boolean testPrepopulation)
    {
        uploadFile(filePath, uniqueifier, finalButton, testPrepopulation, false);
    }

    protected void uploadFile(String filePath, String uniqueifier, String finalButton, boolean testPrepopulation, boolean subtractBackground)
    {
        uploadFile(filePath, uniqueifier, finalButton, testPrepopulation, subtractBackground, false);
    }

    protected void uploadFile(String filePath, String uniqueifier, String finalButton, boolean testPrepopulation, boolean subtractBackground, boolean fluorospot)
    {
        if (subtractBackground)
            checkCheckbox(Locator.checkboxByName("subtractBackground"));
        for (int i = 0; i < 4; i++)
        {
            Locator specimenLocator = Locator.name("specimen" + (i + 1) + "_ParticipantID");

            // test for prepopulation of specimen form element values
            if (testPrepopulation)
                assertFormElementEquals(specimenLocator, "Specimen " + (i+1));
            setFormElement(specimenLocator, "ptid " + (i + 1) + " " + uniqueifier);

            setFormElement(Locator.name("specimen" + (i + 1) + "_VisitID"), "" + (i + 1));
            setFormElement(Locator.name("specimen" + (i + 1) + "_SampleDescription"), "blood");
        }

        File file1 = new File(filePath);
        setFormElement(Locator.name("__primaryFile__"), file1);
        clickButton("Next");

        for (int i = 0; i < 6; i++)
        {
            setFormElement(Locator.name("antigen" + (i + 1) + "_AntigenID"), "" + (i + 1));

            Locator antigenLocator = Locator.name("antigen" + (i + 1) + "_AntigenName");

            // test for prepopulation of antigen element values
            if (testPrepopulation)
                assertFormElementEquals(antigenLocator, "Antigen " + (i+1));

            setFormElement(antigenLocator, "atg_" + (i + 1) + uniqueifier);
            setFormElement(Locator.name("antigen" + (i + 1) + "_CellWell"), "150");
        }

        if (fluorospot)
        {
            clickButton("Next");
            setFormElement(Locator.input("cy3_CytokineName"), "Cytokine 1");
            setFormElement(Locator.input("FITC_CytokineName"), "Cytokine 2");
            setFormElement(Locator.input("FITCCy3_CytokineName"), "Cytokine 3");
        }
        clickButton(finalButton);
    }

    @LogMethod
    private void assertElispotData()
    {
        clickAndWait(Locator.linkContainingText("Zeiss_datafile"));

        assertTextPresent("ptid 1 C", "ptid 2 C", "ptid 3 C", "ptid 4 C", "atg_1C", "atg_2C", "atg_3C", "atg_4C");

        clickAndWait(Locator.linkWithText("view runs"));
        clickAndWait(Locator.linkContainingText("AID_0161456 W4"));

        assertTextPresent("ptid 1 B", "ptid 2 B", "ptid 3 B", "ptid 4 B", "atg_1B", "atg_2B", "atg_3B", "atg_4B");

        // show the normalized spot count column and verify it is calculated correctly
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("NormalizedSpotCount");
        _customizeViewsHelper.applyCustomView();

        DataRegionTable dataTable = new DataRegionTable("Data", this);
        List<String> cellWell = dataTable.getColumnDataAsText("Cells per Well");
        List<String> spotCount = dataTable.getColumnDataAsText("SpotCount");
        List<String> normalizedSpotCount = dataTable.getColumnDataAsText("NormalizedSpotCount");

        for (int i = 0; i < cellWell.size(); i++)
        {
            int cpw = NumberUtils.toInt(cellWell.get(i), 0);
            Float sc = NumberUtils.toFloat(spotCount.get(i));
            Float nsc = NumberUtils.toFloat(normalizedSpotCount.get(i));
            Float computed = sc;

            if (cpw != 0)
                computed = sc / cpw * 1000000;

            assertEquals(computed.intValue(), nsc.intValue());
        }
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.revertUnsavedView();

        clickAndWait(Locator.linkWithText("view runs"));
        clickAndWait(Locator.linkContainingText("details"));

        assertTextPresent(
                "Plate Summary Information",
                "Antigen 7",
                "Mean",
                "Median",
                "Antigen 8"
        );

        waitForElement(Locator.xpath("//label[contains(@class, 'x4-form-item-label') and text() = 'Sample Well Groups']"), WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.xpath("//label[contains(@class, 'x4-form-item-label') and text() = 'Antigen Well Groups']"), WAIT_FOR_JAVASCRIPT);

        // test color hilighting of sample and antigen well groups
        click(Locator.xpath("//label[contains(@class, 'x4-form-cb-label') and text() = 'Specimen 2']"));
        assertElementPresent(getLocatorForHilightedWell("labkey-sampleGroup-Specimen-2", "1023.0"));
        assertElementPresent(getLocatorForHilightedWell("labkey-sampleGroup-Specimen-2", "1021.0"));
        assertElementPresent(getLocatorForHilightedWell("labkey-sampleGroup-Specimen-2", "1028.0"));

        // antigen well group
        click(Locator.xpath("//label[contains(@class, 'x4-form-cb-label') and contains(text(), 'Antigen 2')]"));
        assertElementPresent(getLocatorForHilightedWell("labkey-antigenGroup-Antigen-2", "765.0"));
        assertElementPresent(getLocatorForHilightedWell("labkey-antigenGroup-Antigen-2", "591.0"));
        assertElementPresent(getLocatorForHilightedWell("labkey-antigenGroup-Antigen-2", "257.0"));

        // test the mean and median values
        DataRegionTable table = new CrosstabDataRegion("AntigenStats", this);
        String[] expectedMeans = new String[]{"15555.6", "8888.9", "122222.2", "46666.7"};
        String[] expectedMedians = new String[]{"13333.3", "13333.3", "126666.7", "40000.0"};

        int row = 0;
        int columnIdx = 9; //DataRegionTable doesn't map the subheaders so you can't use the column names
        for (String mean : expectedMeans)
            assertEquals(mean, table.getDataAsText(row++, columnIdx));

        row = 0;
        columnIdx++; //Increment column to look at median column
        for (String median : expectedMedians)
            assertEquals(median, table.getDataAsText(row++, columnIdx));
        PlateSummary plateSummary = new PlateSummary(this, 0);
        assertEquals(Arrays.asList("0.0","5.0","2.0","2.0","1.0","0.0","689.0","641.0","726.0","746.0","621.0","727.0"), plateSummary.getRowValues(A));

        // verify customization of the run details view is possible
/*
        TODO: 25924: CustomizeView doesn't handle aggregate columns in Elispot run details crosstab
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeCustomizeViewColumn("Antigen 7_Mean");
        _customizeViewsHelper.removeCustomizeViewColumn("Antigen 7_Median");
        _customizeViewsHelper.removeCustomizeViewColumn("Antigen 8_Mean");
        _customizeViewsHelper.removeCustomizeViewColumn("Antigen 8_Median");
        _customizeViewsHelper.saveCustomView("Without Antigen7&8");

        _extHelper.clickMenuButton("Views", "default");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeCustomizeViewColumn("Antigen 7_Mean");
        _customizeViewsHelper.removeCustomizeViewColumn("Antigen 7_Median");
        _customizeViewsHelper.saveDefaultView();

        _extHelper.clickMenuButton("Views", "Without Antigen7&8");
        assertTextNotPresent(
                "Antigen 7 Mean",
                "Antigen 7 Median",
                "Antigen 8 Mean",
                "Antigen 8 Median");

        _extHelper.clickMenuButton("Views", "default");
        assertTextNotPresent("Antigen 7 Mean", "Antigen 7 Median");
        assertTextPresent(
                "Antigen 8 Mean",
                "Antigen 8 Median");
*/
    }

    private Locator getLocatorForHilightedWell(String className, String count)
    {
        String xpath = String.format("//div[contains(@class, '%s') and contains(@style, 'background-color: rgb(18, 100, 149);')]//a[contains(text(), %s)]",
                className, count);
        return Locator.xpath(xpath);
    }

    @LogMethod
    protected void createTemplate()
    {
        clickButton("Manage Assays");
        clickButton("Configure Plate Templates");
        clickAndWait(Locator.linkWithText("new 96 well (8x12) ELISpot default template"));
        Locator nameField = Locator.id("templateName");
        waitForElement(nameField, WAIT_FOR_JAVASCRIPT);

        Locator.tagWithClass("*", "gwt-Label").withText("CONTROL").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT).click();

        setFormElement(nameField, PLATE_TEMPLATE_NAME);
        fireEvent(nameField, SeleniumEvent.change);

        click(Locator.xpath("//div[contains(@class, 'x-form-trigger-arrow')]"));
        Locator.tagWithClass("*", "x-combo-list-item").withText("Background Wells").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT).click();
        clickButton("Create", 0);
        waitForElement(Locator.tagWithText("label", "Background Wells"));

        highlightWells("CONTROL", "Background Wells", "A1", "B3");
        highlightWells("CONTROL", "Background Wells", "C4", "D6");
        highlightWells("CONTROL", "Background Wells", "E7", "F9");
        highlightWells("CONTROL", "Background Wells", "G10", "H12");

        Locator groupField = Locator.tagWithClass("div", "x-form-trigger-arrow").precedingSibling("input");
        setFormElement(groupField, "other control group");

        clickButton("Create", 0);
        waitForElement(Locator.tagWithName("input", "wellGroup").withAttribute("type", "radio").followingSibling("label").withText("other control group"));
        clickButton("Save & Close");
        waitForText(PLATE_TEMPLATE_NAME);
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);

        try
        {
            deleteEngine();
        }
        catch (NoSuchElementException ignore) {}
    }

    protected void runTransformTest()
    {
        // add the transform script to the assay
        log("Uploading Elispot Runs with a transform script");

        clickProject(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISPOT));


        AssayDesignerPage assayDesigner = _assayHelper.clickEditAssayDesign();
        assayDesigner.addTransformScript(new File(TestFileUtils.getLabKeyRoot(), "/sampledata/qc/transform.jar"));
        assayDesigner.saveAndClose();
        DataRegionTable.DataRegion(getDriver()).withName("Runs").waitFor();

        clickProject(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISPOT));
        clickButton("Import Data");
        clickButton("Next");

        setFormElement(Locator.name("name"), "transformed assayId");
        selectOptionByText(Locator.name("plateReader"), "AID");
        uploadFile(TEST_ASSAY_ELISPOT_FILE4, "D", "Save and Finish", false);

        // verify there is a spot count value of 747.747 and a custom column added by the transform
        clickAndWait(Locator.linkContainingText("AID_0161456 W5"));
    }

    protected void doBackgroundSubtractionTest()
    {
        removeTransformScript();
        verifyBackgroundSubtractionOnExistingRun();
        verifyBackgroundSubtractionOnNewRun();
    }

    // Unable to apply background substitution to runs imported with a transform script.
    protected void removeTransformScript()
    {
        clickProject(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISPOT));
        _assayHelper.clickEditAssayDesign();
        waitAndClick(Locator.css("div#partdelete_removeTransformScript0 span"));
        clickButton("Save & Close");
        DataRegionTable.DataRegion(getDriver()).withName("Runs").waitFor();
    }

    protected void verifyBackgroundSubtractionOnExistingRun()
    {
        clickProject(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISPOT));
        assertTextPresent("Background Subtraction");
        DataRegionTable runTable = new DataRegionTable("Runs", this);
        List<String> column = runTable.getColumnDataAsText("Background Subtraction");
        for(String item : column)
        {
            assertEquals("Background subtraction should be disabled by default.", "false", item);
        }

        runTable.checkAllOnPage();
        clickButton("Subtract Background");

        waitForTextWithRefresh(WAIT_FOR_PAGE, "COMPLETE");

        // Check well counts for TEST_ASSAY_ELISPOT_FILE4
        clickProject(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISPOT));
        clickAndWait(Locator.linkWithText("run details").index(3));
        waitForElement(Locator.css("#plate-summary-div-1 table"));

        DataRegionTable table = new CrosstabDataRegion("AntigenStats", this);
//        table.setSort("SpecimenLsid/Property/ParticipantID", SortDirection.ASC);      // TODO: we're not showing by default now

        int row = 0;
        int columnIdx = 9; //DataRegionTable doesn't map the subheaders so you can't use the column names
        String[] expectedMeans = new String[]{"0.0", "2271111.1", "1111.1", "4444.4"};
        for (String mean : expectedMeans)
            assertEquals(mean, table.getDataAsText(row++, columnIdx));

        row = 0;
        columnIdx++;
        String[] expectedMedians = new String[]{"0.0", "2376666.7", "3333.3", "6666.7"};
        for (String median : expectedMedians)
            assertEquals(median, table.getDataAsText(row++, columnIdx));
        PlateSummary plateSummary = new PlateSummary(this, 0);
        assertEquals(Arrays.asList("809.0","859.0","821.0","924.0","799.0","833.0","805.0","781.0","782.0","673.0","303.0","TNTC"), plateSummary.getRowValues(C));
        //assertEquals("Incorrect spot counts after background subtraction.", FILE4_PLATE_SUMMARY_POST_SUBTRACTION, getText(Locator.css("#plate-summary-div-1 table")));

        // Check that all runs have been subtracted
        clickProject(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISPOT));
        column = runTable.getColumnDataAsText("Background Subtraction");
        for (String item : column)
        {
            assertEquals("Background subtraction should be true for all runs.", "true", item);
        }
    }

    protected void verifyBackgroundSubtractionOnNewRun()
    {
        clickProject(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISPOT));
        clickButton("Import Data");
        clickButton("Next");

        selectOptionByText(Locator.name("plateReader"), "AID");
        uploadFile(TEST_ASSAY_ELISPOT_FILE5, "E", "Save and Finish", false, true);
        DataRegionTable runTable = new DataRegionTable("Runs", this);
        assertTextPresent("AID_0161456 W8");
        List<String> column = runTable.getColumnDataAsText("Background Subtraction");
        for (String item : column)
            assertEquals("Background subtraction should be true for all runs.", "true", item);

        clickAndWait(Locator.linkWithText("run details"));
        waitForElement(Locator.css("#plate-summary-div-1 table"));

        DataRegionTable detailsTable = new CrosstabDataRegion("AntigenStats", this);
        Map<String, String> expectedBackgroundMedians = new HashMap<>();
        expectedBackgroundMedians.put("ptid 1 E", "0.0");
        expectedBackgroundMedians.put("ptid 2 E", "0.0");
        expectedBackgroundMedians.put("ptid 3 E", "9.5");
        expectedBackgroundMedians.put("ptid 4 E", "0.0");
        for(Map.Entry<String, String> ptidMedian : expectedBackgroundMedians.entrySet())
        {
            String ptid = ptidMedian.getKey();
            String expectedBackgroundMedian = ptidMedian.getValue();
            int row = detailsTable.getRow("Participant ID", ptid);
//            assertEquals("Incorrect background value for " + ptid, expectedBackgroundMedian, detailsTable.getDataAsText(row, "Background Median"));   // TODO: crosstab
        }

        PlateSummary plateSummary = new PlateSummary(this, 0);
        assertEquals(Arrays.asList("10.0","9.0","6.0","10.0","18.0","7.0","11.0","244.0","0.0","0.0","0.0","0.0"), plateSummary.getRowValues(E));
    }

    protected void highlightWells(String type, String group, String startCell, String endCell)
    {
        Locator start = Locator.css(".Cell-"+startCell);
        Locator end = Locator.css(".Cell-"+endCell);
        if (group != null & !"".equals(group))
        {
            if (!getText(Locator.css(".gwt-TabBarItem-selected")).equals(type))
            {
                Locator.css(".gwt-Label").withText(type).findElement(getDriver()).click();
                //want for switch
            }
            if (!isChecked(Locator.xpath("//input[@name='wellGroup' and following-sibling::label[text()='"+group+"']]")))
                click(Locator.xpath("//input[@name='wellGroup' and following-sibling::label[text()='"+group+"']]"));
            if (!getAttribute(start, "style").contains("rgb(255, 255, 255)"))
                click(start);
        }
        else
        {
            Locator.tagWithClass("*", "gwt-Label").withText(type).findElement(getDriver()).click();
            //select no group in order to clear area
        }
        dragAndDrop(start, end);
    }
    private void testTNTCdata()
    {
        clickProject(TEST_ASSAY_PRJ_ELISPOT);
        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISPOT));

        log("Uploading Elispot Runs");
        clickButton("Import Data");
        clickButton("Next");

        selectOptionByText(Locator.name("plateReader"), "AID");
        uploadFile(TEST_ASSAY_ELISPOT_FILE6, "F", "Save and Finish", false);

        testMeanAndMedian();
    }

    public void testMeanAndMedian()
    {
        clickAndWait(Locator.linkContainingText("AID_TNTC"));

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("NormalizedSpotCount");
        _customizeViewsHelper.applyCustomView();

        DataRegionTable dataTable = new DataRegionTable("Data", getDriver());
        List<String> cellWell = dataTable.getColumnDataAsText("Cells per Well");
        List<String> spotCount = dataTable.getColumnDataAsText("Spot Count");
        List<String> normalizedSpotCount = dataTable.getColumnDataAsText("NormalizedSpotCount");
        for (int i=0; i < cellWell.size(); i++)
        {
            if (!"TNTC".equals(spotCount.get(i)))
            {
                int cpw = NumberUtils.toInt(cellWell.get(i), 0);
                Float sc = NumberUtils.toFloat(spotCount.get(i));
                Float nsc = NumberUtils.toFloat(normalizedSpotCount.get(i));
                Float computed = sc;

                if (cpw != 0)
                    computed = sc / cpw * 1000000;

                assertEquals(computed.intValue(), nsc.intValue());
            }
            else
            {
                assertEquals("", normalizedSpotCount.get(i).trim());
            }
        }
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.revertUnsavedView();
        clickAndWait(Locator.linkWithText("view runs"));
        clickAndWait(Locator.linkContainingText("details"));
/*                                                                  // TODO: crosstab
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("atg_2F_Mean");
        _customizeViewsHelper.addCustomizeViewColumn("atg_2F_Median");
        _customizeViewsHelper.addCustomizeViewColumn("atg_4F_Mean");
        _customizeViewsHelper.addCustomizeViewColumn("atg_4F_Median");
        _customizeViewsHelper.addCustomizeViewColumn("atg_6F_Mean");
        _customizeViewsHelper.addCustomizeViewColumn("atg_6F_Median");
        _customizeViewsHelper.applyCustomView();

        // test the mean and median values of columns that had a TNTC spot count
        DataRegionTable table = new CrosstabDataRegion("AntigenStats", this);
        List<String> expectedPtids = Arrays.asList("ptid 1 F", "ptid 2 F", "ptid 3 F", "ptid 4 F");
        List<String> expected2FMeans = Arrays.asList("4000.0", "0.0", "2222.2", "2222.2");
        List<String> expected2FMedians = Arrays.asList("6666.7", "0.0", "0.0", "0.0");
        List<String> expected4FMeans = Arrays.asList("0.0", "0.0", "444444.4", "628888.9");
        List<String> expected4FMedians = Arrays.asList("0.0", "0.0", "6666.7", "0.0");
        List<String> expected6FMeans = Arrays.asList("0.0", "0.0", "0.0", "0.0");
        List<String> expected6FMedians = Arrays.asList("0.0", "0.0", "0.0", "0.0");
        Bag<List<String>> expectedRows = new HashBag<>(DataRegionTable.collateColumnsIntoRows(
                expectedPtids,
                expected2FMeans,
                expected2FMedians,
                expected4FMeans,
                expected4FMedians,
                expected6FMeans,
                expected6FMedians));

        Bag<List<String>> actualRows = new HashBag<>(table.getRows(
                "ParticipantID",
                "Atg2FMean",
                "Atg2FMedian",
                "Atg4FMean",
                "Atg4FMedian",
                "Atg6FMean",
                "Atg6FMedian"));

        assertEquals(expectedRows, actualRows);       */
    }
}
