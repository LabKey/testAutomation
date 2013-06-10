/*
 * Copyright (c) 2009-2013 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.SortDirection;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
import java.util.regex.Pattern;

/**
 * User: jeckels
 * Date: Nov 20, 2007
 */
public class NabAssayTest extends AbstractQCAssayTest
{
    private final static String TEST_ASSAY_PRJ_NAB = "Nab Test Verify Project";            //project for nab test
    private final static String TEST_ASSAY_FLDR_NAB = "nabassay";
    private final static String TEST_ASSAY_FLDR_NAB_RENAME = "Rename" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;

    protected static final String TEST_ASSAY_NAB = "TestAssayNab";
    protected static final String TEST_ASSAY_NAB_DESC = "Description for NAb assay";

    protected final static String TEST_ASSAY_USR_NAB_READER = "nabreader1@security.test";
    private final static String TEST_ASSAY_GRP_NAB_READER = "Nab Dataset Reader";   //name of Nab Dataset Readers group

    protected final String TEST_ASSAY_NAB_FILE1 = getLabKeyRoot() + "/sampledata/Nab/m0902051;3997.xls";
    protected final String TEST_ASSAY_NAB_FILE2 = getLabKeyRoot() + "/sampledata/Nab/m0902053;3999.xls";
    protected final String TEST_ASSAY_NAB_FILE3 = getLabKeyRoot() + "/sampledata/Nab/m0902055;4001.xlsx";
    protected final String TEST_ASSAY_NAB_FILE4 = getLabKeyRoot() + "/sampledata/Nab/m0902057;4003.xls";
    protected final String TEST_ASSAY_NAB_FILE5 = getLabKeyRoot() + "/sampledata/Nab/m0902059;4005.xls";

    // AUC Column Names.
    private static final String AUC_COL_TITLE = "AUC";
    private static final String CURVE_IC50_COL_TITLE = "Curve IC50";
    private static final String CURVE_IC70_COL_TITLE = "Curve IC70";
    private static final String CURVE_IC80_COL_TITLE = "Curve IC80";
    private static final String AUC_4PL_COL_TITLE = "AUC 4pl";
    private static final String AUC_5PL_COL_TITLE = "AUC 5pl";
    private static final String AUC_POLY_COL_TITLE = "AUC Poly";
    private static final String CURVE_IC50_4PL_COL_TITLE = "Curve IC50 4pl";
    private static final String CURVE_IC50_5PL_COL_TITLE = "Curve IC50 5pl";
    private static final String CURVE_IC50_POLY_COL_TITLE = "Curve IC50 Poly";
    private static final String CURVE_IC70_4PL_COL_TITLE = "Curve IC70 4pl";
    private static final String CURVE_IC70_5PL_COL_TITLE = "Curve IC70 5pl";
    private static final String CURVE_IC70_POLY_COL_TITLE = "Curve IC70 Poly";
    private static final String CURVE_IC80_4PL_COL_TITLE = "Curve IC80 4pl";
    private static final String CURVE_IC80_5PL_COL_TITLE = "Curve IC80 5pl";
    private static final String CURVE_IC80_POLY_COL_TITLE = "Curve IC80 Poly";
    // AUC Column Names(after Copy to Study).
    private static final String AUC_STUDY_COL_TITLE = "AUC";
    private static final String AUC_4PL_STUDY_COL_TITLE = "AUC 4pl";
    private static final String AUC_5PL_STUDY_COL_TITLE = "AUC 5pl";
    private static final String AUC_POLY_STUDY_COL_TITLE = "AUC Poly";
    private static final String CURVE_IC50_STUDY_COL_TITLE = "Curve IC50";
    private static final String CURVE_IC50_4PL_STUDY_COL_TITLE = "Curve IC50 4pl";
    private static final String CURVE_IC50_5PL_STUDY_COL_TITLE = "Curve IC50 5pl";
    private static final String CURVE_IC50_POLY_STUDY_COL_TITLE = "Curve IC50 Poly";
    private static final String CURVE_IC70_STUDY_COL_TITLE = "Curve IC70";
    private static final String CURVE_IC70_4PL_STUDY_COL_TITLE = "Curve IC70 4pl";
    private static final String CURVE_IC70_5PL_STUDY_COL_TITLE = "Curve IC70 5pl";
    private static final String CURVE_IC70_POLY_STUDY_COL_TITLE = "Curve IC70 Poly";
    private static final String CURVE_IC80_STUDY_COL_TITLE = "Curve IC80";
    private static final String CURVE_IC80_4PL_STUDY_COL_TITLE = "Curve IC80 4pl";
    private static final String CURVE_IC80_5PL_STUDY_COL_TITLE = "Curve IC80 5pl";
    private static final String CURVE_IC80_POLY_STUDY_COL_TITLE = "Curve IC80 Poly";

    private static final boolean CONTINUE = false;
    private static final String PLATE_TEMPLATE_NAME = "NabAssayTest Template";
    private static final String STUDY_FOLDER = TEST_ASSAY_PRJ_NAB + "/Study 1";
    private static final String STUDY_FOLDER_ENCODED = "Nab%20Test%20Verify%20Project/Study%201";
    private static String NABJS_INCLUDE = "<script type=\"text/javascript\">" +
            "var runOnce = false;\n" +
            "function runNabAssayTest(config)\n" +
            "{\n" +
            "    if (runOnce) return;\n" +
            "    runOnce = true;\n" +
            "    var runsConfig = {\n" +
            "        assayName : 'TestAssayNab',\n" +
            "        success : function(runs)\n" +
            "        {\n" +
            "            \n" +
            "            for (var j = 0; j < runs.length; j += 1)\n" +
            "            {\n" +
            "                if (runs[j].cutoffs[0] != 50) throw 'GetNabRuns failure';\n" +
            "                var sampleIds = [];\n" +
            "                var samples = runs[j].samples;\n" +
            "                for (var k = 0; k < samples.length; k += 1)\n" +
            "                {\n" +
            "                    sampleIds[k] = samples[k].objectId;\n" +
            "                }\n" +
            "                var studyRunsConfig = {\n" +
            "                    objectIds : sampleIds,\n" +
            "                    containerPath : '" + STUDY_FOLDER + "',\n" +
            "                    success : function(runs)\n" +
            "                    {\n" +
            "                        if (runs.length == 0) return;\n" +
            "                        if (runs[0].cutoffs[0] != 50) throw 'GetStudyNabRuns failure';\n" +
            "                        var studyRunsGraphConfig = {\n" +
            "                            objectIds : sampleIds,\n" +
            "                            containerPath : '" + STUDY_FOLDER + "',\n" +
            "                            success : function(graph)\n" +
            "                            {\n" +
            "                               if (graph.length == 0) return;\n" +
            "                               if (graph.url.indexOf('" + STUDY_FOLDER_ENCODED + "') < 0) throw 'GetStudyGraphURL failure';\n" +
            "                               var done = Ext4.create(\"Ext.form.TextField\", { renderTo : config.renderTo, value : 'Success!'});\n" +
            "                            },\n" +
            "                            failure : function() {throw 'GetStudyGraphURL failure';}\n" +
            "                        }\n" +
            "                        var nabStudyGraphURl = LABKEY.Assay.getStudyNabGraphURL(studyRunsGraphConfig);\n" +
            "                    },\n" +
            "                    failure : function() {throw 'GetStudyNabRuns failure';}\n" +
            "                }\n" +
            "                var nabStudyRuns = LABKEY.Assay.getStudyNabRuns(studyRunsConfig);\n" +
            "            }\n" +
            "        },\n" +
            "        failure : function() {throw 'GetNabRuns failure';}\n" +
            "    };\n" +
            "    var nabRuns = LABKEY.Assay.getNAbRuns(runsConfig);\n" +
            "}" +
            "</script>\n";

    // TODO: taken form ClientAPITest; when NabAssayTest is moved to WebDriver, this should be factored into a common place for both tests
    private static final String TEST_DIV_NAME = "testDiv";

    protected String setSource(String srcFragment, boolean excludeTags)
    {
        assertElementPresent(Locator.linkWithText(WIKIPAGE_NAME));
        clickWebpartMenuItem(WIKIPAGE_NAME, "Edit");

        String fullSource = srcFragment;
        if (!excludeTags)
            fullSource = NABJS_INCLUDE + ClientAPITest.getFullSource(srcFragment);
        log("Setting wiki page source:");
        log(fullSource);
        setWikiBody(fullSource);
        saveWikiPage();
        return waitForDivPopulation(30);
    }

    private String waitForDivPopulation(int waitSeconds)
    {
        while (waitSeconds-- > 0)
        {
            log("Waiting for " + TEST_DIV_NAME + " div to render...");
            if (isElementPresent(Locator.id(TEST_DIV_NAME)))
            {
                String divHtml = selenium.getEval("this.browserbot.getCurrentWindow().document.getElementById('" + TEST_DIV_NAME + "').innerHTML;");
                if (divHtml.length() > 0)
                    return divHtml;
            }
            sleep(1000);
        }
        Assert.fail("Div failed to render.");
        return null;
    }
    // End TODO

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/nab";
    }

    @Override
    protected String getProjectName()
    {
        return TEST_ASSAY_PRJ_NAB;
    }

    /**
     * Performs Luminex designer/upload/publish.
     */
    protected void runUITests()
    {
        log("Starting Assay BVT Test");
        //revert to the admin user
        revertToAdmin();

        log("Testing NAb Assay Designer");

        if (!CONTINUE)
        {
            // set up a scripting engine to run a java transform script
            prepareProgrammaticQC();

            //create a new test project
            _containerHelper.createProject(TEST_ASSAY_PRJ_NAB, null);

            //setup a pipeline for it
            setupPipeline(TEST_ASSAY_PRJ_NAB);

            // create a study so we can test copy-to-study later:
            clickProject(TEST_ASSAY_PRJ_NAB);
            _containerHelper.createSubfolder(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_STUDY1, null);
            addWebPart("Study Overview");
            clickButton("Create Study");
            clickButton("Create Study");

            //add the Assay List web part so we can create a new nab assay
            _containerHelper.createSubfolder(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_NAB, null);
            clickProject(TEST_ASSAY_PRJ_NAB);
            addWebPart("Assay List");

            //create a new nab assay
            clickButton("Manage Assays");
            startCreateNabAssay(TEST_ASSAY_NAB);
            selenium.type("//textarea[@id='AssayDesignerDescription']", TEST_ASSAY_NAB_DESC);

            sleep(1000);
            clickButton("Save", 0);
            waitForText("Save successful.", 20000);

            clickAndWait(Locator.linkWithText("configure templates"));

            clickAndWait(Locator.linkWithText("new 96 well (8x12) NAb single-plate template"));

            waitForElement(Locator.xpath("//input[@id='templateName']"), WAIT_FOR_JAVASCRIPT);

            setText("templateName", PLATE_TEMPLATE_NAME);

            // select the specimen wellgroup tab
            click(Locator.tagWithText("div", "SPECIMEN"));

            // select the first specimen group
            click(Locator.tagWithText("label", "Specimen 1"));
            // set reversed dilution direction to true:
            selenium.type("//input[@id='property-ReverseDilutionDirection']", "true");

            // select the second specimen group
            click(Locator.tagWithText("label", "Specimen 2"));
            // set reversed dilution direction to false:
            selenium.type("//input[@id='property-ReverseDilutionDirection']", "false");

            // select the third specimen group
            click(Locator.tagWithText("label", "Specimen 3"));
            // set reversed dilution direction to a nonsense value:
            selenium.type("//input[@id='property-ReverseDilutionDirection']", "invalid boolean value");

            // note that we're intentionally leaving the fourth and fifth direction specifiers null, which should default to 'false'

            clickButton("Save & Close");

            assertTextPresent(PLATE_TEMPLATE_NAME);
            assertTextPresent("NAb: 5 specimens in duplicate");

            clickProject(TEST_ASSAY_PRJ_NAB);
            clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

            clickEditAssayDesign(false);
            waitForElement(Locator.xpath("//select[@id='plateTemplate']"), WAIT_FOR_JAVASCRIPT);

            selectOptionByValue(Locator.xpath("//select[@id='plateTemplate']"), PLATE_TEMPLATE_NAME);

            clickButton("Save", 0);
            waitForText("Save successful.", 20000);

            clickAndWait(Locator.linkWithText("configure templates"));

            click(Locator.linkWithText("delete", 0));

            assertConfirmation("Permanently delete this plate template?");

            waitForPageToLoad();

            assertTextPresent(PLATE_TEMPLATE_NAME);
            assertTextNotPresent("NAb: 5 specimens in duplicate");
        }

        clickProject(TEST_ASSAY_PRJ_NAB);
        clickFolder(TEST_ASSAY_FLDR_NAB);
        addWebPart("Assay List");

        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

        if (isFileUploadAvailable())
        {
            log("Uploading NAb Runs");
            clickButton("Import Data");
            clickButton("Next");

            setFormElement("cutoff1", "50");
            setFormElement("cutoff2", "70");
            setFormElement("virusName", "Nasty Virus");
            setFormElement("virusID", "5433211");
            selectOptionByText("curveFitMethod", "Polynomial");

            for (int i = 0; i < 5; i++)
            {
                setFormElement("specimen" + (i + 1) + "_ParticipantID", "ptid " + (i + 1));
                setFormElement("specimen" + (i + 1) + "_VisitID", "" + (i + 1));
                setFormElement("specimen" + (i + 1) + "_InitialDilution", "20");
                setFormElement("specimen" + (i + 1) + "_Factor", "3");
                selectOptionByText("specimen" + (i + 1) + "_Method", "Dilution");
            }

            uploadFile(TEST_ASSAY_NAB_FILE1, "A", "Save and Import Another Run");
            assertTextPresent("Upload successful.");

            setFormElement("cutoff2", "80");
            selectOptionByText("curveFitMethod", "Four Parameter");
            uploadFile(TEST_ASSAY_NAB_FILE2, "B", "Save and Import Another Run");
            assertTextPresent("Upload successful.");

            selectOptionByText("curveFitMethod", "Five Parameter");
            uploadFile(TEST_ASSAY_NAB_FILE3, "C", "Save and Finish");
            //uploadFile(TEST_ASSAY_NAB_FILE4, "D");
            //uploadFile(TEST_ASSAY_NAB_FILE5, "E");

            assertTextPresent("Virus Name");
            assertTextPresent("Nasty Virus");
            assertTextPresent("ptid 1 C, Vst 1.0");
            assertTextPresent("&lt; 20", 10);
            // check for the first dilution for the second participant:
            // Five Parameter IC50
            assertTextPresent("561");
            // Five PL AUC
            assertTextPresent("0.077");
            // Five PL posAUC
            assertTextPresent("0.081");
            // Polynomial IC50:
            assertTextNotPresent("503");
            // Four parameter IC50
            assertTextNotPresent("461");

            click(Locator.linkContainingText("Change Curve Type"));
            clickAndWait(Locator.menuItem("Four Parameter"));
            // Five Parameter IC50
            assertTextNotPresent("561");
            // Polynomial IC50:
            assertTextNotPresent("503");
            // Four parameter IC50
            assertTextPresent("461");
            // 4PL AUC/PosAUC
            assertTextPresent("0.043");
            // Five PL AUC
            assertTextNotPresent("0.077");

            click(Locator.linkContainingText("Change Curve Type"));
            clickAndWait(Locator.menuItem("Polynomial"));
            // Five Parameter IC50
            assertTextNotPresent("561");
            // Polynomial IC50:
            assertTextPresent("503");
            // Four parameter IC50
            assertTextNotPresent("461");
            // Polynomial AUC:
            assertTextPresent("0.054");
            // Polynomial posAUC:
            assertTextPresent("0.055");
            // Five PL AUC
            assertTextNotPresent("0.077");
            // 4PL AUC/PosAUC
            assertTextNotPresent("0.043");

            // Test editing runs
            // Set the design to allow editing
            clickAndWait(Locator.linkWithText("View Runs"));
            assertLinkNotPresentWithText("edit");
            click(Locator.linkWithText("manage assay design"));
            selenium.chooseOkOnNextConfirmation();
            clickAndWait(Locator.linkWithText("edit assay design"));
            assertConfirmation("This assay is defined in the /Nab Test Verify Project folder. Would you still like to edit it?");

            waitForElement(Locator.xpath("//span[@id='id_editable_run_properties']"), WAIT_FOR_JAVASCRIPT);
            checkCheckbox(Locator.xpath("//span[@id='id_editable_run_properties']/input"));
            clickButton("Save & Close");

            // Edit the first run
            clickFolder(TEST_ASSAY_FLDR_NAB);
            clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));
            clickAndWait(Locator.linkWithText("edit"));
            // Make sure that the properties that affect calculations aren't shown
            assertTextNotPresent("Cutoff");
            assertTextNotPresent("Curve Fit Method");
            setText("quf_Name", "NameEdited.xlsx");
            setText("quf_HostCell", "EditedHostCell");
            setText("quf_PlateNumber", "EditedPlateNumber");
            clickButton("Submit");
            assertLinkPresentWithText("NameEdited.xlsx");
            assertTextPresent("EditedHostCell", "EditedPlateNumber");

            // Verify that the edit was audited
            goToModule("Query");
            selectQuery("auditLog", "ExperimentAuditEvent");
            waitForElement(Locator.linkWithText("view data"), WAIT_FOR_JAVASCRIPT);
            clickAndWait(Locator.linkWithText("view data"));
            assertTextPresent("Run edited",
                    "Plate Number changed from blank to 'EditedPlateNumber'",
                    "Host Cell changed from blank to 'EditedHostCell'",
                    "Name changed from 'm0902055;4001.xlsx' to 'NameEdited.xlsx'");

            // Return to the run list
            clickFolder(TEST_ASSAY_FLDR_NAB);
            clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

            // test creating a custom details view via a "magic" named run-level view:
            _customizeViewsHelper.openCustomizeViewPanel();
            _customizeViewsHelper.removeCustomizeViewColumn("VirusName");
            _customizeViewsHelper.saveCustomView("CustomDetailsView");

            clickAndWait(Locator.linkContainingText("details", 1));
            assertNabData(true);

            clickAndWait(Locator.linkWithText("View Results"));

            assertAUCColumnsHidden();
            addAUCColumns();
            assertAliasedAUCCellData();

            setFilter("Data", "SpecimenLsid/Property/ParticipantID", "Equals", "ptid 1 C");
            assertTextPresent("ptid 1 C");
            String ptid1c_detailsURL = getAttribute(Locator.xpath("//a[contains(text(), 'details')]"), "href");
            setFilter("Data", "SpecimenLsid/Property/ParticipantID", "Equals One Of (e.g. \"a;b;c\")", "ptid 1 A;ptid 1 B;ptid 2 A;ptid 2 B;ptid 3 A;ptid 3 B;ptid 4 A;ptid 4 B");
            assertTextPresent("ptid 1 A");
            assertTextPresent("ptid 1 B");
            assertTextNotPresent("ptid 1 C");
            assertTextNotPresent("ptid 5");
            checkAllOnPage("Data");
            clickButton("Copy to Study");

            selectOptionByText("targetStudy", "/" + TEST_ASSAY_PRJ_NAB + "/" + TEST_ASSAY_FLDR_STUDY1 + " (" + TEST_ASSAY_FLDR_STUDY1 + " Study)");
            clickButton("Next");
            clickButton("Copy to Study");
            assertStudyData(4);

            assertAliasedAUCStudyData();

            clickAndWait(Locator.linkWithText("assay"));
            assertNabData(true);

            doSchemaBrowserTest();

            // create user with read permissions to study and dataset, but no permissions to source assay
            clickProject(TEST_ASSAY_PRJ_NAB);
            clickFolder(TEST_ASSAY_FLDR_STUDY1);
            pushLocation();  // Save our location because impersonatied user won't have permission to project
            createPermissionsGroup(TEST_ASSAY_GRP_NAB_READER, TEST_ASSAY_USR_NAB_READER);
            setSubfolderSecurity(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_STUDY1, TEST_ASSAY_GRP_NAB_READER, TEST_ASSAY_PERMS_READER);
            setStudyPerms(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_STUDY1, TEST_ASSAY_GRP_NAB_READER, TEST_ASSAY_PERMS_STUDY_READALL);

            // view dataset, click [assay] link, see assay details in nabassay container
            impersonate(TEST_ASSAY_USR_NAB_READER);
            popLocation();
            assertTextPresent(TEST_ASSAY_PRJ_NAB);
            assertTextNotPresent(TEST_ASSAY_FLDR_NAB); // assert no read permissions to nabassay container
            clickFolder(TEST_ASSAY_FLDR_STUDY1);
            clickAndWait(Locator.linkWithText("Study Navigator"));
            clickAndWait(Locator.linkWithText("2"));
            assertStudyData(1);
            clickAndWait(Locator.linkWithText("assay"));
            assertNabData(false); // CustomDetailsView not enabled for all users so "Virus Name" is present

            // no permission to details page for "ptid 1 C"; it wasn't copied to the study
            beginAt(ptid1c_detailsURL);
            Assert.assertEquals(getResponseCode(), 401);

            beginAt("/login/logout.view");  // stop impersonating

            doNabApiTest();

            runTransformTest();

            moveAssayFolderTest();

            directBrowserQueryTest();
        }
    } //doTestSteps()


    //Issue 17050: UnsupportedOperationException from org.labkey.nab.query.NabProtocolSchema$NabResultsQueryView.createDataView
    private void directBrowserQueryTest()
    {
        beginAt("/query/Nab%20Test%20Verify%20Project/selectRows.api?schemaName=assay&queryName=TestAssayNab%20Data");
        assertTextPresent("metaData");
    }

    /**
     * previously, assays sometimes failed to find their source files after a folder move
     * this test verifies the fix
     */
    private void moveAssayFolderTest()
    {
        log("rename assay folder and verify source file still findable");
        renameFolder(getProjectName(), TEST_ASSAY_FLDR_NAB, TEST_ASSAY_FLDR_NAB_RENAME, false);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_FLDR_NAB_RENAME));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));
        clickAndWait(Locator.linkContainingText("run details"));

        assertTextPresent("Description for NAb assay");
    }

    private static String QUERY_NAME = "Data";
    private void doSchemaBrowserTest()
    {
        clickFolder(TEST_ASSAY_FLDR_NAB);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));
        goToSchemaBrowser();
        selectQuery("assay.NAb.TestAssayNab", QUERY_NAME);
        clickButton("Create New Query");
        setFormElement(Locator.name("ff_newQueryName"), "New NabQuery");
        clickAndWait(Locator.linkWithText("Create and Edit Source"));
        setCodeEditorValue("queryText",
                "SELECT \n" +
                        QUERY_NAME + ".Properties.AUC As AUC,\n" +
                        QUERY_NAME + ".Properties.CurveIC50_4pl,\n" +
                        QUERY_NAME + ".Properties.CurveIC50_4plOORIndicator,\n" +
                        QUERY_NAME + ".Properties.SpecimenLsid.Property.ParticipantID,\n" +
                        QUERY_NAME + ".WellgroupName\n" +
                        "FROM " + QUERY_NAME + "\n"
        );
        clickButton("Save & Finish", 0);
        waitForText("Views", WAIT_FOR_JAVASCRIPT);
        assertTextPresent("AUC", "Curve IC50 4pl", "Curve IC50 4pl OOR Indicator", "Participant ID", "Wellgroup Name");
        assertTextPresent("<20.0", "ptid 1 C", "473.94");
    }

    private static String WIKIPAGE_NAME = "Nab API Wiki";
    private void doNabApiTest()
    {
        clickProject(TEST_ASSAY_PRJ_NAB);
        clickFolder(TEST_ASSAY_FLDR_NAB);
        addWebPart("Wiki");
        createNewWikiPage("HTML");
        setFormElement(Locator.name("name"), WIKIPAGE_NAME);
        setFormElement(Locator.name("title"), WIKIPAGE_NAME);
        setWikiBody("placeholder text");
        saveWikiPage();

        setSource("runNabAssayTest({renderTo : 'testDiv'})", false);
    }

    private void uploadFile(String filePath, String uniqueifier, String finalButton)
    {
        for (int i = 0; i < 5; i++)
        {
            setFormElement("specimen" + (i + 1) + "_ParticipantID", "ptid " + (i + 1) + " " + uniqueifier);
            setFormElement("specimen" + (i + 1) + "_VisitID", "" + (i + 1));
            setFormElement("specimen" + (i + 1) + "_InitialDilution", "20");
            setFormElement("specimen" + (i + 1) + "_Factor", "3");
            selectOptionByText("specimen" + (i + 1) + "_Method", "Dilution");
        }

        setFormElement("dataCollectorName", "File upload");
        File file1 = new File(filePath);
        setFormElement("__primaryFile__", file1);
        clickButton(finalButton, 60000);
    }

    private void assertStudyData(int ptidCount)
    {
        assertTextPresent("Dataset: " + TEST_ASSAY_NAB);

        if (ptidCount >= 1)
        {
            // reversed dilution direction:
            assertTextPresent("ptid 1 A");
            assertTextPresent("ptid 1 B");
            assertTextPresent("Curve IC50");
            assertTextPresent("493");
            assertTextPresent("Specimen 1", 2);
        }
        if (ptidCount >= 2)
        {
            // standard dilution direction
            assertTextPresent("ptid 2 A");
            assertTextPresent("ptid 2 B");
            assertTextPresent("134");
            assertTextPresent("Specimen 2", 2);
        }
        if (ptidCount >= 3)
        {
            // invalid dilution direction
            assertTextPresent("ptid 3 A");
            assertTextPresent("ptid 3 B");
            assertTextPresent("436");
            assertTextPresent("Specimen 3", 2);
        }
        if (ptidCount >= 4)
        {
            // unspecified dilution direction
            assertTextPresent("ptid 4 A");
            assertTextPresent("ptid 4 B");
            assertTextPresent("277.9");
            assertTextPresent("Specimen 4", 2);
        }
    }

    private void assertNabData(boolean hasCustomView)
    {
        assertTextPresent("Cutoff Dilutions");
        assertTextPresent("ptid 1");
        assertTextPresent("ptid 2");
        assertTextPresent("ptid 3");
        assertTextPresent("ptid 4");
        assertTextPresent("ptid 5");
        assertTextPresent("Virus ID");
        if (hasCustomView)
        {
            assertTextNotPresent("Virus Name");
            assertTextNotPresent("Nasty Virus");
        }
        else
        {
            assertTextPresent("Virus Name");
            assertTextPresent("Nasty Virus");
        }
    }

    private void assertAUCColumnsHidden()
    {
        log("Checking for AUC columns");
        // verify that most AUC columns are hidden by default
        assertTextPresent("AUC");
        assertTextNotPresent("AUC 4pl");
        assertTextNotPresent("AUC 5pl");
        assertTextNotPresent("AUC Poly");
        assertTextNotPresent("Curve IC50 4pl");
        assertTextNotPresent("Curve IC50 5pl");
        assertTextNotPresent("Curve IC50 Poly");
        assertTextNotPresent("Curve IC70 4pl");
        assertTextNotPresent("Curve IC70 5pl");
        assertTextNotPresent("Curve IC70 Poly");
        assertTextNotPresent("Curve IC80 4pl");
        assertTextNotPresent("Curve IC80 5pl");
        assertTextNotPresent("Curve IC80 Poly");
    }

    private void addAUCColumns()
    {
        log("Adding AUC columns to custom view");
        // add AUC columns. ORDER MATTERS!
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("AUC_4pl", "AUC 4pl");
        _customizeViewsHelper.addCustomizeViewColumn("AUC_5pl", "AUC 5pl");
        _customizeViewsHelper.addCustomizeViewColumn("AUC_Poly", "AUC Poly");
        _customizeViewsHelper.addCustomizeViewColumn("Cutoff50/IC_4pl", "Curve IC50 4pl");
        _customizeViewsHelper.addCustomizeViewColumn("Cutoff50/IC_5pl", "Curve IC50 5pl");
        _customizeViewsHelper.addCustomizeViewColumn("Cutoff50/IC_Poly", "Curve IC50 Poly");
        _customizeViewsHelper.addCustomizeViewColumn("Cutoff70/IC_4pl", "Curve IC70 4pl");
        _customizeViewsHelper.addCustomizeViewColumn("Cutoff70/IC_5pl", "Curve IC70 5pl");
        _customizeViewsHelper.addCustomizeViewColumn("Cutoff70/IC_Poly", "Curve IC70 Poly");
        _customizeViewsHelper.addCustomizeViewColumn("Cutoff80/IC_4pl", "Curve IC80 4pl");
        _customizeViewsHelper.addCustomizeViewColumn("Cutoff80/IC_5pl", "Curve IC80 5pl");
        _customizeViewsHelper.addCustomizeViewColumn("Cutoff80/IC_Poly", "Curve IC80 Poly");
        _customizeViewsHelper.applyCustomView();
    }

    private void assertAliasedAUCCellData()
    {
        log("Checking data in aliased AUC columns");
        // Check that aliased AUC column show data from correct columns.  Any changes in the default location/quantity of columns will require adjustment of column indices.
        DataRegionTable table = new DataRegionTable("Data", this);

        Assert.assertEquals("", table.getDataAsText(0, CURVE_IC80_COL_TITLE)); //ptid 1 A, Curve IC 80. Should be blank.
        Assert.assertEquals("", table.getDataAsText(5, CURVE_IC70_COL_TITLE)); //ptid 1 B, Curve IC 70. Should be blank.

        for(int i = 0; i < 5; i++)
        {
            Assert.assertEquals(table.getDataAsText(i, AUC_COL_TITLE),        table.getDataAsText(i, AUC_POLY_COL_TITLE));        //AUC = AUC_poly
            Assert.assertEquals(table.getDataAsText(i, CURVE_IC50_COL_TITLE), table.getDataAsText(i, CURVE_IC50_POLY_COL_TITLE)); //Curve IC50 = Curve_IC50_poly
            Assert.assertEquals(table.getDataAsText(i, CURVE_IC70_COL_TITLE), table.getDataAsText(i, CURVE_IC70_POLY_COL_TITLE)); //Curve IC70 = Curve_IC70_poly
        }
        for(int i = 5; i < 10; i++)
        {
            Assert.assertEquals(table.getDataAsText(i, AUC_COL_TITLE),        table.getDataAsText(i, AUC_4PL_COL_TITLE));        //AUC = AUC_4pl
            Assert.assertEquals(table.getDataAsText(i, CURVE_IC50_COL_TITLE), table.getDataAsText(i, CURVE_IC50_4PL_COL_TITLE)); //Curve IC50 = Curve_IC50_4pl
            Assert.assertEquals(table.getDataAsText(i, CURVE_IC80_COL_TITLE), table.getDataAsText(i, CURVE_IC80_4PL_COL_TITLE)); //Curve IC80 = Curve_IC80_4pl
        }
        for(int i = 10; i < 15; i++)
        {
            Assert.assertEquals(table.getDataAsText(i, AUC_COL_TITLE),        table.getDataAsText(i, AUC_5PL_COL_TITLE));        //AUC = AUC_5pl
            Assert.assertEquals(table.getDataAsText(i, CURVE_IC50_COL_TITLE), table.getDataAsText(i, CURVE_IC50_5PL_COL_TITLE)); //Curve IC50 = Curve_IC50_5pl
            Assert.assertEquals(table.getDataAsText(i, CURVE_IC80_COL_TITLE), table.getDataAsText(i, CURVE_IC80_5PL_COL_TITLE)); //Curve IC80 = Curve_IC80_5pl
        }
    }

    private void assertAliasedAUCStudyData()
    {
        log("Checking data in aliased AUC columns in Study");
        // check copied AUC data.
        setSort("Dataset", "ParticipantId", SortDirection.ASC);
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("AUC_Poly",        AUC_POLY_COL_TITLE);
        _customizeViewsHelper.addCustomizeViewColumn("AUC_4pl",         AUC_4PL_COL_TITLE);
        _customizeViewsHelper.addCustomizeViewColumn("AUC_5pl",         AUC_5PL_COL_TITLE);
        _customizeViewsHelper.addCustomizeViewColumn("Cutoff50/IC_Poly", CURVE_IC50_POLY_STUDY_COL_TITLE);
        _customizeViewsHelper.addCustomizeViewColumn("Cutoff50/IC_4pl",  CURVE_IC50_4PL_STUDY_COL_TITLE);
        _customizeViewsHelper.addCustomizeViewColumn("Cutoff70/IC_Poly", CURVE_IC70_POLY_STUDY_COL_TITLE);
        _customizeViewsHelper.addCustomizeViewColumn("Cutoff80/IC_4pl",  CURVE_IC80_4PL_STUDY_COL_TITLE);
        _customizeViewsHelper.saveCustomView();

        DataRegionTable table = new DataRegionTable("Dataset", this);
        Assert.assertEquals(table.getDataAsText(0, AUC_STUDY_COL_TITLE),        table.getDataAsText(0, AUC_POLY_STUDY_COL_TITLE));        //AUC = AUC_poly
        Assert.assertEquals(table.getDataAsText(1, AUC_STUDY_COL_TITLE),        table.getDataAsText(1, AUC_4PL_STUDY_COL_TITLE));         //AUC = AUC_4pl
        Assert.assertEquals(table.getDataAsText(0, CURVE_IC50_STUDY_COL_TITLE), table.getDataAsText(0, CURVE_IC50_POLY_STUDY_COL_TITLE)); //CurveIC50 = CurveIC50_poly
        Assert.assertEquals(table.getDataAsText(1, CURVE_IC50_STUDY_COL_TITLE), table.getDataAsText(1, CURVE_IC50_4PL_STUDY_COL_TITLE));  //CurveIC50 = CurveIC50_4pl
        Assert.assertEquals(table.getDataAsText(0, CURVE_IC70_STUDY_COL_TITLE), table.getDataAsText(0, CURVE_IC70_POLY_STUDY_COL_TITLE)); //CurveIC70 = CurveIC70_poly
        Assert.assertEquals(table.getDataAsText(1, CURVE_IC80_STUDY_COL_TITLE), table.getDataAsText(1, CURVE_IC80_4PL_STUDY_COL_TITLE));  //CurveIC80 = CurveIC80_4pl

        Assert.assertEquals("", table.getDataAsText(0, CURVE_IC80_STUDY_COL_TITLE)); //IC80 = blank
        Assert.assertEquals("", table.getDataAsText(1, CURVE_IC70_STUDY_COL_TITLE)); //IC70 = blank
    }

    /**
     * Cleanup entry point.
     * @param afterTest
     */
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        if (!CONTINUE)
        {
            revertToAdmin();
            deleteProject(getProjectName(), afterTest);
            try{deleteEngine();}
            catch(Throwable T) {}

            deleteDir(getTestTempDir());
        }
    } //doCleanup()

    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void runTransformTest()
    {
        // add the transform script to the assay
        log("Uploading NAb Runs with a transform script");
        clickProject(TEST_ASSAY_PRJ_NAB);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));
        clickEditAssayDesign(false);

        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), "/sampledata/qc/transform.jar"), 0);
        clickButton("Save & Close");

        clickFolder(TEST_ASSAY_FLDR_NAB);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));
        clickButton("Import Data");
        clickButton("Next");

        setFormElement("name", "transformed assayId");
        setFormElement("cutoff1", "50");
        setFormElement("cutoff2", "80");
        selectOptionByText("curveFitMethod", "Polynomial");
        uploadFile(TEST_ASSAY_NAB_FILE1, "E", "Save and Finish");

        // verify the run property FileID was generated by the transform script
        clickAndWait(Locator.linkWithText("View Runs"));
        assertTextPresent("transformed FileID");

        // verify the fit error was generated by the transform script
        clickAndWait(Locator.linkWithText("transformed assayId"));

        DataRegionTable table = new DataRegionTable("Data", this);
        for(int i = 0; i < 5; i++)
        {
            Assert.assertEquals("0.0", table.getDataAsText(i, "Fit Error"));
        }
    }

    @Override
    protected Pattern[] getIgnoredElements()
    {
        return new Pattern[] {
            Pattern.compile("RunProperties", Pattern.CASE_INSENSITIVE),
            Pattern.compile("RunGroups", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Input", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Batch", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Output", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Links", Pattern.CASE_INSENSITIVE),
            Pattern.compile("runId", Pattern.CASE_INSENSITIVE),
            Pattern.compile("assayId", Pattern.CASE_INSENSITIVE)
        };
    }

    @Override
    protected File[] getTestFiles()
    {
        return new File[] {
            new File(getLabKeyRoot() + "/server/test/data/api/nab-api.xml")
        };
    }
}
