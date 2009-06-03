/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

import java.io.File;

/**
 * User: jeckels
 * Date: Nov 20, 2007
 */
public class NabAssayTest extends AbstractAssayTest
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
            setFormElement("cutoff2", "80");
            setFormElement("virusName", "Nasty Virus");
            selectOptionByText("curveFitMethod", "Five Parameter");

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
            uploadFile(TEST_ASSAY_NAB_FILE2, "B", "Save and Import Another Run");
            assertTextPresent("Upload successful.");
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
            clickLinkWithText("assay");
            assertNabData(true);

            // create user with read permissions to study and dataset, but no permissions to source assay
            clickLinkWithText(TEST_ASSAY_PRJ_NAB);
            clickLinkWithText(TEST_ASSAY_FLDR_STUDY1);
            clickLinkWithText("Folder Permissions");
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
                deleteFile(getTestTempDir());
            }
            catch(Throwable T) {}
        }
    } //doCleanup()

    protected boolean isFileUploadTest()
    {
        return true;
    }
}