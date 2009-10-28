/*
 * Copyright (c) 2009 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;

import java.io.File;

/**
 * User: jeckels
 * Date: Nov 20, 2007
 */
public class NabAssayTest extends AbstractQCAssayTest
{
    private final static String TEST_ASSAY_PRJ_NAB = "Nab Test Verify Project";            //project for nab test
    private final static String TEST_ASSAY_FLDR_NAB = "nabassay";

    protected static final String TEST_ASSAY_NAB = "TestAssayNab";
    protected static final String TEST_ASSAY_NAB_DESC = "Description for NAb assay";

    protected final static String TEST_ASSAY_USR_NAB_READER = "nabreader1@security.test";
    private final static String TEST_ASSAY_GRP_NAB_READER = "Nab Dataset Reader";   //name of Nab Dataset Readers group

    protected final String TEST_ASSAY_NAB_FILE1 = getLabKeyRoot() + "/sampledata/Nab/m0902051;3997.xls";
    protected final String TEST_ASSAY_NAB_FILE2 = getLabKeyRoot() + "/sampledata/Nab/m0902053;3999.xls";
    protected final String TEST_ASSAY_NAB_FILE3 = getLabKeyRoot() + "/sampledata/Nab/m0902055;4001.xls";
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
    private static final String CURVE_IC50_STUDY_COL_TITLE = "CurveIC50";
    private static final String CURVE_IC50_4PL_STUDY_COL_TITLE = "CurveIC50_4pl";
    private static final String CURVE_IC50_5PL_STUDY_COL_TITLE = "CurveIC50_5pl";
    private static final String CURVE_IC50_POLY_STUDY_COL_TITLE = "CurveIC50_poly";
    private static final String CURVE_IC70_STUDY_COL_TITLE = "CurveIC70";
    private static final String CURVE_IC70_4PL_STUDY_COL_TITLE = "CurveIC70_4pl";
    private static final String CURVE_IC70_5PL_STUDY_COL_TITLE = "CurveIC70_5pl";
    private static final String CURVE_IC70_POLY_STUDY_COL_TITLE = "CurveIC70_poly";
    private static final String CURVE_IC80_STUDY_COL_TITLE = "CurveIC80";
    private static final String CURVE_IC80_4PL_STUDY_COL_TITLE = "CurveIC80_4pl";
    private static final String CURVE_IC80_5PL_STUDY_COL_TITLE = "CurveIC80_5pl";
    private static final String CURVE_IC80_POLY_STUDY_COL_TITLE = "CurveIC90_poly";

    private static final boolean CONTINUE = false;

    public String getAssociatedModuleDirectory()
    {
        return "nab";
    }

    /**
     * Performs Luminex designer/upload/publish.
     */
    protected void doTestSteps()
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
            createProject(TEST_ASSAY_PRJ_NAB);

            //setup a pipeline for it
            setupPipeline(TEST_ASSAY_PRJ_NAB);

            // create a study so we can test copy-to-study later:
            clickLinkWithText(TEST_ASSAY_PRJ_NAB);
            createSubfolder(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_STUDY1, null);
            addWebPart("Study Overview");
            clickNavButton("Create Study");
            clickNavButton("Create Study");

            //add the Assay List web part so we can create a new nab assay
            createSubfolder(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_NAB, null);
            clickLinkWithText(TEST_ASSAY_PRJ_NAB);
            addWebPart("Assay List");

            //create a new nab assay
            clickLinkWithText("Manage Assays");
            clickNavButton("New Assay Design");
            checkRadioButton("providerName", "TZM-bl Neutralization (NAb)");
            clickNavButton("Next");

            waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_GWT);

            log("Setting up NAb assay");
            selenium.type("//input[@id='AssayDesignerName']", TEST_ASSAY_NAB);
            selenium.type("//textarea[@id='AssayDesignerDescription']", TEST_ASSAY_NAB_DESC);

            sleep(1000);
            clickNavButton("Save", 0);
            waitForText("Save successful.", 20000);
        }
        clickLinkWithText(TEST_ASSAY_PRJ_NAB);
        clickLinkWithText(TEST_ASSAY_FLDR_NAB);
        addWebPart("Assay List");

        clickLinkWithText("Assay List");
        clickLinkWithText(TEST_ASSAY_NAB);

        if (isFileUploadAvailable())
        {
            log("Uploading NAb Runs");
            clickNavButton("Import Data");
            clickNavButton("Next");

            setFormElement("cutoff1", "50");
            setFormElement("cutoff2", "70");
            setFormElement("virusName", "Nasty Virus");
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
            assertTextPresent("561");

            // test creating a custom details view via a "magic" named run-level view:
            clickLinkWithText("view runs");
            clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
            removeCustomizeViewColumn("Virus Name");
            setFormElement("ff_columnListName", "CustomDetailsView");
            clickNavButton("Save");

            clickLinkWithText("details", 1);
            assertNabData(true);

            clickLinkWithText("view results");

            assertAUCColumnsHidden();
            addAUCColumns();
            assertAliasedAUCCellData();

            setFilter(TEST_ASSAY_NAB + " Data", "Properties/SpecimenLsid/Property/ParticipantID", "Equals", "ptid 1 C");
            assertTextPresent("ptid 1 C");
            String ptid1c_detailsURL = getAttribute(Locator.xpath("//a[contains(text(), 'details')]"), "href");

            setFilter(TEST_ASSAY_NAB + " Data", "Properties/SpecimenLsid/Property/ParticipantID", "Equals One Of (e.g. 'a;b;c')", "ptid 1 A;ptid 1 B");
            assertTextPresent("ptid 1 A");
            assertTextPresent("ptid 1 B");
            assertTextNotPresent("ptid 1 C");
            assertTextNotPresent("ptid 2");
            checkAllOnPage(TEST_ASSAY_NAB + " Data");
            clickNavButton("Copy to Study");

            selectOptionByText("targetStudy", "/" + TEST_ASSAY_PRJ_NAB + "/" + TEST_ASSAY_FLDR_STUDY1 + " (" + TEST_ASSAY_FLDR_STUDY1 + " Study)");
            clickNavButton("Next");
            clickNavButton("Copy to Study");
            assertStudyData();

            assertAliasedAUCStudyData();
            
            clickLinkWithText("assay");
            assertNabData(true);

            // create user with read permissions to study and dataset, but no permissions to source assay
            clickLinkWithText(TEST_ASSAY_PRJ_NAB);
            clickLinkWithText(TEST_ASSAY_FLDR_STUDY1);
            enterPermissionsUI();
            createPermissionsGroup(TEST_ASSAY_GRP_NAB_READER);
            addUserToProjGroup(TEST_ASSAY_USR_NAB_READER, TEST_ASSAY_FLDR_STUDY1, TEST_ASSAY_GRP_NAB_READER);
            setSubfolderSecurity(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_STUDY1, TEST_ASSAY_GRP_NAB_READER, TEST_ASSAY_PERMS_READER);
            setStudyPerms(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_STUDY1, TEST_ASSAY_GRP_NAB_READER, TEST_ASSAY_PERMS_STUDY_READALL);

            // view dataset, click [assay] link, see assay details in nabassay container
            impersonate(TEST_ASSAY_USR_NAB_READER);
            clickLinkWithText(TEST_ASSAY_PRJ_NAB);
            assertTextNotPresent(TEST_ASSAY_FLDR_NAB); // assert no read permissions to nabassay container
            clickLinkWithText(TEST_ASSAY_FLDR_STUDY1);
            clickLinkWithText("Study Navigator");
            clickLinkWithText("2");
            assertStudyData();
            clickLinkWithText("assay");
            assertNabData(false); // CustomDetailsView not enabled for all users so "Virus Name" is present

            // no permission to details page for "ptid 1 C"; it wasn't copied to the study
            beginAt(ptid1c_detailsURL);
            assertEquals(getResponseCode(), 401);

            beginAt("/login/logout.view");  // stop impersonating

            runTransformTest();
        }

    } //doTestSteps()

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
        setFormElement("uploadedFile", file1);
        clickNavButton(finalButton, 60000);
    }

    private void assertStudyData()
    {
        assertTextPresent("Dataset: " + TEST_ASSAY_NAB);
        assertTextPresent("ptid 1 A");
        assertTextPresent("ptid 1 B");
        assertTextPresent("CurveIC50");
        assertTextPresent("1353");
        assertTextPresent("Specimen 1", 2);
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
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
        click(Locator.id("expand_Properties"));
        addCustomizeViewColumn("Properties/AUC_4pl", "AUC 4pl");
        addCustomizeViewColumn("Properties/AUC_5pl", "AUC 5pl");
        addCustomizeViewColumn("Properties/AUC_poly", "AUC Poly");
        addCustomizeViewColumn("Properties/Curve IC50_4pl", "Curve IC50 4pl");
        addCustomizeViewColumn("Properties/Curve IC50_5pl", "Curve IC50 5pl");
        addCustomizeViewColumn("Properties/Curve IC50_poly", "Curve IC50 Poly");
        addCustomizeViewColumn("Properties/Curve IC70_4pl", "Curve IC70 4pl");
        addCustomizeViewColumn("Properties/Curve IC70_5pl", "Curve IC70 5pl");
        addCustomizeViewColumn("Properties/Curve IC70_poly", "Curve IC70 Poly");
        addCustomizeViewColumn("Properties/Curve IC80_4pl", "Curve IC80 4pl");
        addCustomizeViewColumn("Properties/Curve IC80_5pl", "Curve IC80 5pl");
        addCustomizeViewColumn("Properties/Curve IC80_poly", "Curve IC80 Poly");
        clickNavButton("Save");
    }

    private void assertAliasedAUCCellData()
    {
        log("Checking data in aliased AUC columns");
        // Check that aliased AUC column show data from correct columns.  Any changes in the default location/quantity of columns will require adjustment of column indices.
        assertTableCellTextEquals("dataregion_" + TEST_ASSAY_NAB + " Data",  1, CURVE_IC80_COL_TITLE, "");//ptid 1 A, Curve IC 80. Should be blank.
        assertTableCellTextEquals("dataregion_" + TEST_ASSAY_NAB + " Data",  6, CURVE_IC70_COL_TITLE, "");//ptid 1 B, Curve IC 70. Should be blank.
        for(int i = 1; i <= 5; i++)
        {
            assertTableCellsEqual("dataregion_" + TEST_ASSAY_NAB + " Data",  i, AUC_COL_TITLE, i, AUC_POLY_COL_TITLE); //AUC = AUC_poly
            assertTableCellsEqual("dataregion_" + TEST_ASSAY_NAB + " Data",  i, CURVE_IC50_COL_TITLE, i, CURVE_IC50_POLY_COL_TITLE); //Curve IC50 = Curve_IC50_poly
            assertTableCellsEqual("dataregion_" + TEST_ASSAY_NAB + " Data",  i, CURVE_IC70_COL_TITLE, i, CURVE_IC70_POLY_COL_TITLE); //Curve IC70 = Curve_IC70_poly
        }
        for(int i = 6; i <= 10; i++)
        {
            assertTableCellsEqual("dataregion_" + TEST_ASSAY_NAB + " Data",  i, AUC_COL_TITLE, i, AUC_4PL_COL_TITLE); //AUC = AUC_4pl
            assertTableCellsEqual("dataregion_" + TEST_ASSAY_NAB + " Data",  i, CURVE_IC50_COL_TITLE, i, CURVE_IC50_4PL_COL_TITLE); //Curve IC50 = Curve_IC50_4pl
            assertTableCellsEqual("dataregion_" + TEST_ASSAY_NAB + " Data",  i, CURVE_IC80_COL_TITLE, i, CURVE_IC80_4PL_COL_TITLE); //Curve IC80 = Curve_IC80_4pl
        }
        for(int i = 11; i <= 15; i++)
        {
            assertTableCellsEqual("dataregion_" + TEST_ASSAY_NAB + " Data",  i, AUC_COL_TITLE, i, AUC_5PL_COL_TITLE); //AUC = AUC_5pl
            assertTableCellsEqual("dataregion_" + TEST_ASSAY_NAB + " Data",  i, CURVE_IC50_COL_TITLE, i, CURVE_IC50_5PL_COL_TITLE); //Curve IC50 = Curve_IC50_5pl
            assertTableCellsEqual("dataregion_" + TEST_ASSAY_NAB + " Data",  i, CURVE_IC80_COL_TITLE, i, CURVE_IC80_5PL_COL_TITLE); //Curve IC80 = Curve_IC80_5pl
        }
    }

    private void assertAliasedAUCStudyData()
    {
        log("Checking data in aliased AUC columns in Study");
        // check copied AUC data.
        assertTableCellsEqual("dataregion_Dataset",  1, AUC_STUDY_COL_TITLE, 1, AUC_POLY_STUDY_COL_TITLE); //AUC = AUC_poly
        assertTableCellsEqual("dataregion_Dataset",  2, AUC_STUDY_COL_TITLE, 2, AUC_4PL_STUDY_COL_TITLE); //AUC = AUC_4pl
        assertTableCellsEqual("dataregion_Dataset",  1, CURVE_IC50_STUDY_COL_TITLE, 1, CURVE_IC50_POLY_STUDY_COL_TITLE); //CurveIC50 = CurveIC50_poly
        assertTableCellsEqual("dataregion_Dataset",  2, CURVE_IC50_STUDY_COL_TITLE, 2, CURVE_IC50_4PL_STUDY_COL_TITLE); //CurveIC50 = CurveIC50_4pl
        assertTableCellsEqual("dataregion_Dataset",  1, CURVE_IC70_STUDY_COL_TITLE, 1, CURVE_IC70_POLY_STUDY_COL_TITLE); //CurveIC70 = CurveIC70_poly
        assertTableCellsEqual("dataregion_Dataset",  2, CURVE_IC80_STUDY_COL_TITLE, 2, CURVE_IC80_4PL_STUDY_COL_TITLE); //CurveIC80 = CurveIC80_4pl
        assertTableCellTextEquals("dataregion_Dataset",  1, CURVE_IC80_STUDY_COL_TITLE, ""); //IC80 = blank
        assertTableCellTextEquals("dataregion_Dataset",  2, CURVE_IC70_STUDY_COL_TITLE, ""); //IC70 = blank
    }

    /**
     * Cleanup entry point.
     */
    protected void doCleanup()
    {
        if (!CONTINUE)
        {
            revertToAdmin();
            try
            {
                deleteProject(TEST_ASSAY_PRJ_NAB);
                deleteEngine();
            }
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
        clickLinkWithText(TEST_ASSAY_PRJ_NAB);
        clickLinkWithText(TEST_ASSAY_NAB);
        click(Locator.linkWithText("manage assay design >>"));
        clickLinkWithText("edit assay design");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerTransformScript']"), WAIT_FOR_GWT);

        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), "/sampledata/qc/transform.jar"));
        clickNavButton("Save & Close");

        clickLinkWithText(TEST_ASSAY_FLDR_NAB);
        clickLinkWithText(TEST_ASSAY_NAB);
        clickNavButton("Import Data");
        clickNavButton("Next");

        setFormElement("name", "transformed assayId");
        setFormElement("cutoff1", "50");
        setFormElement("cutoff2", "80");
        selectOptionByText("curveFitMethod", "Polynomial");
        uploadFile(TEST_ASSAY_NAB_FILE1, "E", "Save and Finish");

        // verify the run property FileID was generated by the transform script
        clickLinkWithText("view runs");
        assertTextPresent("transformed FileID");

        // verify the fit error was generated by the transform script
        clickLinkWithText("transformed assayId");
        for(int i = 1; i <= 5; i++)
        {
            assertTableCellTextEquals("dataregion_" + TEST_ASSAY_NAB + " Data",  i, "Fit Error", "0.0");
        }
    }
}