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
import org.labkey.test.drt.StudyManualTest;

import java.io.File;

/**
 * User: brittp
 * Date: Dec 8, 2006
 * Time: 4:30:24 PM
 */
public class StudyBvtTest extends StudyManualTest
{
    private static final String SPECIMEN_ARCHIVE_B = "/sampledata/study/specimens/sample_b.specimens";
    private static final String DATA_SET = "DEM-1: Demographics";
    private static final String TEST_ADD_ENTRY = "999000000";

    private final String DATASET_DATA_FILE = getLabKeyRoot() + "/sampledata/dataLoading/excel/dataset_data.xls";

    @Override
    protected void doTestSteps()
    {
        super.doTestSteps();

        // verify that we correctly warn when specimen tracking hasn't been configured
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Create New Request");
        assertTextPresent("Specimen management is not configured for this study");

        // configure specimen tracking
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Request Statuses");
        setFormElement("newLabel", "New Request");
        clickNavButton("Save");
        setFormElement("newLabel", "Pending Approval");
        clickNavButton("Save");
        setFormElement("newLabel", "Complete");
        clickNavButton("Done");
        clickLinkWithText("Manage Actors and Groups");
        setFormElement("newLabel", "Institutional Review Board");
        selectOptionByText("newPerSite", "Multiple Per Study (Location Affiliated)");
        clickNavButton("Save");
        setFormElement("newLabel", "Scientific Leadership Group");
        selectOptionByText("newPerSite", "One Per Study");
        clickNavButton("Save");
        clickLinkWithText("Update Members");
        clickLinkWithText("FHCRC - Seattle");
        assertTextPresent("Institutional Review Board, FHCRC - Seattle");
        assertTextPresent("This group currently has no members.");
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Default Requirements");
        selectOptionByText("providerActor", "Institutional Review Board");
        setFormElement("providerDescription", "To be deleted");
        clickNavButtonByIndex("Add Requirement", 1);
        assertTextPresent("To be deleted");
        clickLinkWithText("Delete");
        assertTextNotPresent("To be deleted");
        selectOptionByText("providerActor", "Institutional Review Board");
        setFormElement("providerDescription", "Providing lab approval");
        clickNavButtonByIndex("Add Requirement", 1);
        selectOptionByText("receiverActor", "Institutional Review Board");
        setFormElement("receiverDescription", "Receiving lab approval");
        clickNavButtonByIndex("Add Requirement", 2);
        selectOptionByText("generalActor", "Scientific Leadership Group");
        setFormElement("generalDescription", "SLG Request Approval");
        clickNavButtonByIndex("Add Requirement", 3);
        clickLinkWithText("manage study");

        // create specimen request
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Study Navigator");

        assertLinkNotPresentWithText("24");
        selectOptionByText("QCState", "All data");
        waitForPageToLoad();

        clickLinkWithText("24");
        //getDialog().setWorkingForm("Dataset");
        checkCheckbox(Locator.checkboxByName(".toggle"));
        clickNavButton("View Specimens");
        assertTextPresent("999320016");
        assertTextPresent("999320518");
        clickLinkWithText("Show Vial Info");
        assertTextPresent("999320016");
        checkCheckbox(Locator.checkboxByName(".toggle"));
        clickNavButton("Request Options", 0);
        clickLinkWithText("Create New Request");
        assertTextPresent("HAQ0003Y-09");
        assertTextPresent("BAQ00051-09");
        assertTextNotPresent("KAQ0003Q-01");
        selectOptionByText("destinationSite", "Duke University");
        setFormElement("inputs", new String[] { "An Assay Plan", "Duke University, NC", "My comments" });
        clickNavButton("Create and View Details");

        assertTextPresent("This request has not been submitted");
        assertNavButtonPresent("Cancel Request");
        assertNavButtonPresent("Submit Request");
        clickLinkWithText("Specimen Requests");

        assertNavButtonPresent("Submit");
        assertNavButtonPresent("Cancel");
        assertNavButtonPresent("Details");
        assertTextPresent("Not Yet Submitted");
        clickNavButton("Submit");
        selenium.getConfirmation();
        clickLinkWithText("Specimen Requests");
        assertNavButtonNotPresent("Submit");
        assertNavButtonPresent("Details");
        assertTextPresent("New Request");

        // test auto-fill:
        clickNavButton("Create New Request");
        String inputs = selenium.getValue("inputs");
        System.out.println(inputs);
        assertFormElementNotEquals(Locator.dom("document.forms[0].inputs[1]"), "Duke University, NC");
        selectOptionByText("destinationSite", "Duke University");
        assertFormElementEquals(Locator.dom("document.forms[0].inputs[1]"), "Duke University, NC");
        clickNavButton("Cancel");

        // manage new request
        clickNavButton("Details");
        assertTextNotPresent("Complete");
        assertTextNotPresent("WARNING: Missing Specimens");
        assertTextPresent("New Request");
        assertTextNotPresent("Pending Approval");
        clickLinkWithText("Update Status");
        selectOptionByText("status", "Pending Approval");
        setFormElement("comments", "Request is now pending.");
        clickNavButton("Save Changes and Send Notifications");
        assertTextNotPresent("New Request");
        assertTextPresent("Pending Approval");
        clickLinkWithText("Details", 0);
        assertTextPresent("Duke University");
        assertTextPresent("Providing lab approval");
        checkCheckbox("complete");
        setFormElement("comment", "Approval granted.");
        if (isFileUploadAvailable())
            setFormElement("formFiles[0]", new File(getLabKeyRoot() + VISIT_MAP).getPath());
        else
            log("File upload skipped.");
        clickNavButton("Save Changes and Send Notifications");
        assertTextPresent("Complete");

        clickLinkWithText("Details", 1);
        clickNavButton("Delete Requirement");
        assertTextNotPresent("Receiving lab approval");

        clickLinkWithText("Originating Location Specimen Lists");
        assertTextPresent("WARNING: The requirements for this request are incomplete");
        assertTextPresent("KCMC, Moshi, Tanzania");
        clickNavButton("Cancel");

        clickLinkWithText("View History");
        assertTextPresent("Request is now pending.");
        assertTextPresent("Approval granted.");
        assertTextPresent("Institutional Review Board (Duke University), Receiving lab approval");
        if (isFileUploadAvailable())
            assertTextPresent(VISIT_MAP.substring(VISIT_MAP.lastIndexOf("/") + 1));

        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        enterPermissionsUI();
        clickNavButton("Study Security");

        // enable advanced study security
        selectOptionByValue("securityString", "ADVANCED_READ");
        selenium.waitForPageToLoad("30000");

        click(Locator.xpath("//td[.='Users']/..//input[@value='READ']"));
        clickAndWait(Locator.id("groupUpdateButton"));

        // set the QC state 
        clickLinkWithText(getFolderName());
        clickLinkWithText(DATA_SET);
        clickMenuButton("QC State", "QCState:All data");
        checkAllOnPage("Dataset");
        clickMenuButton("QC State", "QCState:updateSelected");
        selectOptionByText("newState", "clean");
        setFormElement("comments", "This data is clean.");
        clickNavButton("Update Status");
        clickMenuButton("QC State", "QCState:clean");

        // test specimen comments
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Plasma, Unknown Processing");
        clickNavButton("Enable Comments/QC");
        checkAllOnPage("SpecimenDetail");
        clickMenuButton("Comments and QC", "Comments:Set");
        setFormElement("comments", "These vials are very important.");
        clickNavButton("Save Changes");
        assertTextPresent("These vials are very important.", 25);
        setFilter("SpecimenDetail", "ParticipantId", "Equals", "999320824");
        checkAllOnPage("SpecimenDetail");
        clickMenuButton("Comments and QC", "Comments:Clear");
        selenium.getConfirmation();
        assertTextNotPresent("These vials are very important.");
        clearFilter("SpecimenDetail", "ParticipantId");
        assertTextPresent("These vials are very important.", 23);
        clickMenuButton("Comments and QC", "Comments:Exit");

        // import second archive, verify that that data is merged:
        SpecimenImporter importer = new SpecimenImporter(new File(getPipelinePath()), new File(getLabKeyRoot(), SPECIMEN_ARCHIVE_B), new File(getLabKeyRoot(), ARCHIVE_TEMP_DIR), getStudyLabel(), 2);
        importer.importAndWaitForComplete();

        // verify that comments remain after second specimen load
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Plasma, Unknown Processing");
        assertTextPresent("These vials are very important.", 2);

        // check to see that data in the specimen archive was merged correctly:
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("By Vial");
        clickMenuButton("Page Size", "Page Size:All");
        assertTextPresent("DRT000XX-01");
        clickLinkWithText("Search");
        clickLinkWithText("Search by specimen");

//        WARNING: Using getFormElementNameByTableCaption() is dangerous... if muliple values are returned their
//        order is unpredictable, since they come back in keyset order.  The code below breaks under Java 6.
//
//        String[] globalUniqueIDCompareElems = getFormElementNameByTableCaption("Specimen Number", 0, 1);
//        String[] globalUniqueIDValueElems = getFormElementNameByTableCaption("Specimen Number", 0, 2);
//        String[] participantIDFormElems = getFormElementNameByTableCaption("Participant Id", 0, 1);
//        setFormElement(globalUniqueIDCompareElems[1], "CONTAINS");
//        setFormElement(globalUniqueIDValueElems[0], "1416");
//        setFormElement(participantIDFormElems[2], "999320528");

        // Hard-code the element names, since code above is unpredictable
        selectOptionByValue("searchParams[0].value", "999320528");
        selectOptionByValue("searchParams[1].value", "Enroll/Vacc #1");

        clickNavButton("Search");
        assertTextPresent("999320528");
        clickLinkWithText("Show Vial Info");
        // if our search worked, we'll only have six vials:
        assertTextPresent("[history]", 6);
        assertLinkPresentWithTextCount("999320528", 6);
        assertTextNotPresent("DRT000XX-01");
        clickLinkWithText("[history]");
        assertTextPresent("GAA082NH-01");
        assertTextPresent("BAD");
        assertTextPresent("1.0&nbsp;ML");
        assertTextPresent("Added Comments");
        assertTextPresent("Johannesburg, South Africa");

        clickLinkWithText(getStudyLabel());
        clickLinkWithText("View Existing Requests");
        clickNavButton("Details");
        assertTextPresent("WARNING: Missing Specimens");
        clickNavButton("Delete missing specimens");
        selenium.getConfirmation();
        assertTextNotPresent("WARNING: Missing Specimens");
        assertTextPresent("Duke University");
        assertTextPresent("An Assay Plan");
        assertTextPresent("Providing lab approval");
        assertTextPresent("HAQ0003Y-09");
        assertTextPresent("BAQ00051-09");
        assertTextNotPresent("BAQ00051-10");
        assertTextPresent("BAQ00051-11");

        log("Test editing rows in a dataset");
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());

        enterPermissionsUI();
        clickNavButton("Study Security");

        selectOptionByValue("securityString", "BASIC_WRITE");
        selenium.waitForPageToLoad("30000");

        clickLinkWithText(getFolderName());
        clickLinkWithText("DEM-1: Demographics");

        clickLinkWithText("edit");
        setFormElement("quf_DEMbdt", "2001-11-11");
        clickNavButton("Submit");
        clickMenuButton("QC State", "QCState:unknown QC");
        assertTextPresent("2001-11-11");

        log("Test adding a row to a dataset");
        clickNavButton("Insert New");
        clickNavButton("Submit");
        assertTextPresent("This field is required");
        setFormElement("quf_participantid", TEST_ADD_ENTRY);
        setFormElement("quf_SequenceNum", "123");
        clickNavButton("Submit");
        clickMenuButton("QC State", "QCState:All data");
        assertTextPresent(TEST_ADD_ENTRY);

        log("Test deleting rows in a dataset");
        checkCheckbox(Locator.raw("//input[contains(@value, '999320529')]"));
        clickNavButton("Delete");
        selenium.getConfirmation();
        assertTextNotPresent("999320529");

        // configure QC state management to show all data by default so the next steps don't have to keep changing the state:
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage QC States");
        selectOptionByText("showPrivateDataByDefault", "All data");
        clickNavButton("Save");

        // Test creating and importing a dataset from an excel file
        doTestDatasetImport();
    }

    protected boolean comparePaths(String path1, String path2)
    {
        String[] parseWith = { "/", "\\\\" };
        for (String parser1 : parseWith)
        {
            String[] path1Split = path1.split(parser1);
            for  (String parser2 : parseWith)
            {
                String[] path2Split = path2.split(parser2);
                if (path1Split.length == path2Split.length)
                {
                    int index = 0;
                    while (path1Split[index].compareTo(path2Split[index]) == 0)
                    {
                        index++;
                        if (index > path2Split.length - 1)
                            return true;
                    }
                }
            }
        }
        return false;
    }

    protected void doTestDatasetImport()
    {
        if (!isFileUploadAvailable())
            return;

        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Create New Dataset");
        setFormElement("typeName", "fileImportDataset");
        clickCheckbox("fileImport");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@name='uploadFormElement']"), WAIT_FOR_GWT);

        File datasetFile = new File(DATASET_DATA_FILE);
        setFormElement("uploadFormElement", datasetFile);

        waitForElement(Locator.xpath("//span[@id='button_Import']"), WAIT_FOR_GWT);

        clickNavButton("Import");

        waitForPageToLoad();

        assertTextPresent("kevin");
        assertTextPresent("chimpanzee");
    }
}
