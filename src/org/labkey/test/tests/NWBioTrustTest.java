/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.ModulePropertyValue;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4HelperWD;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: cnathe
 * Date: 1/25/13
 */
public class NWBioTrustTest extends SurveyTest
{
    private static final String requestorFolder1 = "Requestor 1";
    private static final String requestorFolder2 = "Requestor 2";
    private static final String provisionTableName = "Sample Request Responses";
    private static final List<Map<String, String>> designs = new ArrayList<>();
    private static final String registrationLabel = "requestor 1 study";
    private static final String[] unsubmittedRequestTypes = {"Normal tissue from patients without cancer"};
    private static final String[] submittedRequestTypes = {"Tumor from primary site", "Tumor from metastasis", "Normal tissues adjacent to primary site (same organ)"};
    private static final String[] allRequestTypes = ArrayUtils.addAll(submittedRequestTypes, unsubmittedRequestTypes);
    private static final File TEST_FILE_1 = new File( getLabKeyRoot() + "/sampledata/survey/TestAttachment.txt");
    private static final File TEST_FILE_2 = new File( getLabKeyRoot() + "/sampledata/survey/TestAttachment2.txt");

    private static final String[] NWBT_REQUEST_CATEGORIES = {"NWBT RC1", "NWBT RC2", "NWBT Repository"};
    private static final NwbtRequestStatuses[] NWBT_REQUEST_STATUSES = NwbtRequestStatuses.values();

    private enum NwbtRequestStatuses
    {
        SUBMITTED("Submitted"),
        OVERSIGHT_REVIEW("Oversight Review")
        {
            @Override
            public boolean isApproval()
            {
                return true;
            }
        },
        FEASIBILITY_REVIEW("Feasibility Review")
        {
            @Override
            public boolean isApproval()
            {
                return true;
            }
        },
        REQUEST_REVIEW("Request Review"),
        APPROVED("Approved")
        {
            @Override
            public boolean isLocked()
            {
                return true;
            }
        },
        CLOSED("Closed")
        {
            @Override
            public boolean isLocked()
            {
                return true;
            }
        };

        private String _status;

        private NwbtRequestStatuses(String status)
        {
            _status = status;
        }

        public String toString()
        {
            return _status;
        }

        public Integer sortOrder()
        {
            return this.ordinal() + 1;
        }

        public boolean isLocked()
        {
            return false;
        }

        public boolean isApproval()
        {
            return false;
        }
    }

    private static final String[] NWBT_DOCUMENT_TYPES = {
            "Proposal Summary (Specimen Request)",
            "Proposal Summary (Future Study Request)",
            "IRB Approval Packet",
            "Signed MTDUA Agreement",
            "Signed Confidentiality Pledge",
            "Specimen Processing Protocol (Tissue)",
            "Specimen Processing Protocol (Blood)",
            "Blank Unique Consent Form (by Study)",
            "Approval Reviewer Response",
            "NWBT VBD Specimen Search Export"
    };
    private static final Boolean[][] NWBT_DOCUMENT_TYPE_FLAGS = { // multiple upload allowed, expriation
            {false, false}, //Proposal Summary (Specimen Request) (multi-upload changed for test purposes)
            {true, false}, //Proposal Summary (Future Study Request)
            {true, true}, //IRB Approval Packet
            {true, false}, //Signed MTDUA Agreement(s)
            {true, false}, //Signed Confidentiality Pledge(s)
            {true, false}, //Specimen Processing Protocol (Tissue)
            {true, false}, //Specimen Processing Protocol (Blood)
            {true, true}, //Blank Unique Consent Form (by Study)
            {true, false}, //Approval Reviewer Response
            {true, false} //NWBT VBD Specimen Search Export
    };

    private static final String NWBT_PRINCIPAL_INVESTIGATOR = "pi_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_STUDY_CONTACT = "sc_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_RESEARCH_COORD = "rc_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_RC_EMAIL = "rc_notification@nwbiotrust.test";
    private static final String NWBT_FACULTY_CHAIR = "fc_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_FACULTY_REVIEWER = "fr_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_TISSUE_REVIEWER = "tr_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_BLOOD_REVIEWER = "br_nwbiotrust@nwbiotrust.test";
    private static final String[] NWBT_USERS = {NWBT_PRINCIPAL_INVESTIGATOR, NWBT_STUDY_CONTACT, NWBT_RESEARCH_COORD,
                                                NWBT_FACULTY_CHAIR, NWBT_FACULTY_REVIEWER, NWBT_TISSUE_REVIEWER, NWBT_BLOOD_REVIEWER};
    private static final String NWBT_REVIEWER_GROUP = "Approval Reviewers";
    private static final String NWBT_RESEARCH_COORD_GROUP = "Research Coordinators";
    private static final String NWBT_RESEARCH_COORD_ROLE = "NWBT Research Coordinator";

    private final File studyRegistrationJson = new File(getDownloadDir(), "study-registration.json");
    private final File sampleRequestJson = new File(getDownloadDir(), "sample-request.json");
    private final File tissueJson = new File(getDownloadDir(), "tissue-sample.json");
    private final File bloodSampleJson = new File(getDownloadDir(), "blood-sample.json");

    private int fileCount = 0;

    private final PortalHelper portalHelper = new PortalHelper(this);

    @Override
    protected String getProjectName()
    {
        return "NWBioTrustTest";
    }

    @Override
    @LogMethod protected void doTestSteps() throws Exception
    {
        enableEmailRecorder();

        setupResearchCoordAndRequstorFolders();
        setupUsersAndPermissions();
        setupProjectAdminProperties();
        setupSurveysTableDefinition();
        setupProvisionTableForResponses();
        setupSurveyDesignsAndStudy();

        verifySampleRequests();
        verifyFolderTypes();
        verifyResearchCoordDashboard();
        verifyReviewerWorkflow();
        verifyRequestorDashboard();
        verifySecondRequestorDashboard();
        verifyDocumentSetFromDashboard();
        verifyEmailNotifications();
    }

    @LogMethod
    private void deleteSurveyDesign()
    {
        if (designs.size() == 0)
            configureDesigns();

        log("Delete the survey designs for this project (which will delete the document sets and requests");
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Manage"));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("RowId");
        _customizeViewsHelper.applyCustomView();
        DataRegionTable drt = new DataRegionTable("query", this);
        int toDelete = 0;
        for (Map<String, String> design : designs)
        {
            if (drt.getRow("Label", design.get("label")) > -1)
            {
                String rowId = drt.getDataAsText(drt.getRow("Label", design.get("label")), "RowId");
                checkDataRegionCheckbox("query", rowId);
                toDelete++;
            }
        }

        if (toDelete > 0)
        {
            prepForPageLoad();
            clickButton("Delete", 0);
            if (toDelete == 1)
                assertAlert("Are you sure you want to delete this survey design and its associated surveys?");
            else
                assertAlert("Are you sure you want to delete these " + toDelete + " survey designs and their associated survey instances?");
            newWaitForPageToLoad();

            clickTab("New Sample Requests");
            waitForText("No sample requests to show", WAIT_FOR_PAGE);
            clickFolder(requestorFolder1);
            clickTab("Request Samples");
            waitForText("No sample requests to show", 3, WAIT_FOR_PAGE);
            assertTextNotPresent(submittedRequestTypes);
        }
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyDocumentSetFromDashboard()
    {
        goToProjectHome();
        populateDocumentSetForRequests();

        log("Verify documents and types via RC Dashboard");
        goToProjectHome();
        clickTab("New Sample Requests");
        waitForGridToLoad("tr", "x4-grid-row", submittedRequestTypes.length);
        waitAndClickAndWait(Locator.linkWithText("view document set"));
        waitForText(registrationLabel);
        waitForText(TEST_FILE_1.getName());
        assertElementPresent(Locator.linkWithText(TEST_FILE_1.getName()), NWBT_DOCUMENT_TYPES.length);
        assertElementPresent(Locator.linkWithText(TEST_FILE_2.getName()), NWBT_DOCUMENT_TYPES.length - 1); // one doc type doesn't allow multiple file upload
        for (int index = 0; index < NWBT_DOCUMENT_TYPES.length; index++)
            assertTextPresent(NWBT_DOCUMENT_TYPES[index], NWBT_DOCUMENT_TYPE_FLAGS[index][0] ? 2 : 1);

        log("Verify removing documents from document set");
        // verify that we navigated to the appropriate subfolder for the manage document set page
        assertTextNotPresent("RC Dashboard");
        Locator loc = getEditLinkLocator(NWBT_DOCUMENT_TYPES[0], false);
        click(loc);
        _extHelper.waitForExtDialog("Edit Document");
        assertTextPresentInThisOrder("File Name:", "Document Type:", "Created By:", "Created:");
        clickButton("Delete", 0);
        fileCount--;
        waitForTextToDisappear(NWBT_DOCUMENT_TYPES[0]);
        waitForGridToLoad("tr", "x4-grid-row", fileCount);
    }

    @LogMethod
    private void populateDocumentSetForRequests()
    {
        log("Add documents to a document set for requests");
        clickFolder(requestorFolder1);
        clickTab("My Studies");
        waitForGridToLoad("tr", "x4-grid-row", 1);
        assertElementPresent(Locator.linkWithText("Document Set (0)"), 1);
        click(Locator.linkWithText("Document Set (0)"));
        waitForText("No documents to show");
        waitForText(registrationLabel);
        clickButton("Manage");
        waitForText("No documents to show");
        waitForText(registrationLabel);
        fileCount = 0;
        for (int index = 0; index < NWBT_DOCUMENT_TYPES.length; index++)
        {
            String documentType = NWBT_DOCUMENT_TYPES[index];
            clickButton("Add Document(s)", 0);
            _extHelper.waitForExtDialog("Add Document(s)");
            _ext4Helper.selectComboBoxItem("Document Type:", documentType);
            _shortWait.until(LabKeyExpectedConditions.elementIsEnabled(Locator.name("attachmentfile0")));
            setFormElement(Locator.name("attachmentfile0"), TEST_FILE_1);
            fileCount++;
            // the first doc type was set to not allow multiple file uploads
            if (NWBT_DOCUMENT_TYPE_FLAGS[index][0])
            {
                assertElementPresent(Locator.linkContainingText("Attach a file"));
                click(Locator.linkContainingText("Attach a file"));
                _shortWait.until(LabKeyExpectedConditions.elementIsEnabled(Locator.name("attachmentfile1")));
                setFormElement(Locator.name("attachmentfile1"), TEST_FILE_2);
                fileCount++;
            }
            else
                assertElementNotPresent(Locator.linkContainingText("Attach a file"));
            sleep(500); // give the submit button a split second to enable base on form changes
            clickButton("Submit", 0);
            waitForGridToLoad("tr", "x4-grid-row", fileCount);
        }

        log("Verify file attachment links and document types exist");
        assertElementPresent(Locator.linkWithText(TEST_FILE_1.getName()), NWBT_DOCUMENT_TYPES.length);
        assertElementPresent(Locator.linkWithText(TEST_FILE_2.getName()), NWBT_DOCUMENT_TYPES.length - 1); // one doc type doesn't allow multiple file upload
        for (int index = 0; index < NWBT_DOCUMENT_TYPES.length; index++)
            assertTextPresent(NWBT_DOCUMENT_TYPES[index], NWBT_DOCUMENT_TYPE_FLAGS[index][0] ? 2 : 1);

        log("Test document type allow multiple file setting");
        clickButton("Add Document(s)", 0);
        _extHelper.waitForExtDialog("Add Document(s)");
        _ext4Helper.selectComboBoxItem("Document Type:", NWBT_DOCUMENT_TYPES[0]);
        _shortWait.until(LabKeyExpectedConditions.elementIsEnabled(Locator.name("attachmentfile0")));
        setFormElement(Locator.name("attachmentfile0"), TEST_FILE_2);
        sleep(500); // give the submit button a split second to enable base on form changes
        clickButton("Submit", 0);
        waitForText("This document type does not allow multiple files and one already exists in this document set.");
        clickButton("OK", 0);
        _shortWait.until(LabKeyExpectedConditions.elementIsEnabled(Locator.name("attachmentfile0")));
        setFormElement(Locator.name("attachmentfile0"), TEST_FILE_1);
        sleep(500); // give the submit button a split second to enable base on form changes
        clickButton("Submit", 0);
        waitForText("A document with the following name already exists for this document type: " + TEST_FILE_1.getName());
        clickButton("OK", 0);
        clickButton("Close", 0);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifySecondRequestorDashboard()
    {
        log("Verify that the 2nd requestor folder does not contain data from the first requestor");
        clickFolder(requestorFolder2);
        clickTab("My Studies");
        waitForText("No study registrations to show", 1, WAIT_FOR_PAGE);
        clickTab("Request Samples");
        waitForText("No sample requests to show", 3, WAIT_FOR_PAGE);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyRequestorDashboard()
    {
        goToProjectHome();
        log("Verify updated requests show up in Requestor Dashboard");
        clickFolder(requestorFolder1);
        clickTab("My Studies");
        waitForGridToLoad("tr", "x4-grid-row", 1);
        assertTextPresent(registrationLabel);
        clickTab("Request Samples");
        waitForGridToLoad("tr", "x4-grid-row", 4);
        waitForElement(Locator.id("pending-dashboard-2").append(Locator.linkWithText("4")));
        waitForElement(Locator.id("submitted-dashboard-3").append(Locator.linkWithText("1")));
        waitForElement(Locator.id("submitted-dashboard-3").append(Locator.linkWithText("2")));
        waitForElement(Locator.id("submitted-dashboard-3").append(Locator.linkWithText("3")));
        waitForText("No sample requests to show", 1, WAIT_FOR_PAGE);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifySampleRequests()
    {
        impersonate(NWBT_PRINCIPAL_INVESTIGATOR);
        clickProject(getProjectName());
        clickFolder(requestorFolder1);
        clickTab("Request Samples");

        log("Create sample requests - from surguries or clinic procedures");
        for (String requestType : allRequestTypes)
        {
            clickButton("Request sample collection");
            _ext4Helper.selectComboBoxItem(Locator.xpath("//tr").withPredicate(Locator.xpath("td/label").containing("Associated study")).append("/td[2]/table"), registrationLabel);
            _ext4Helper.checkCheckbox("Tissue collected at surgery");
            setFormElement(Locator.name("collectionstartdate"), "2013-03-20");
            setFormElement(Locator.name("collectionenddate"), "2013-03-21");
            clickButton("Next", 0);
            _ext4Helper.checkCheckbox("Stomach");
            clickButton("Next", 0);
            setFormElement(Locator.name("totalspecimendonors"), "6");
            setFormElement(Locator.name("genderrequirements"), "males 50%, females 50%");
            setFormElement(Locator.name("agerequirements"), ">=21yr");
            click(Ext4HelperWD.Locators.formItemWithLabel("Is cancer history and previous treatment relevant to your study?").append("//label").withText("Yes"));
            click(Ext4HelperWD.Locators.formItemWithLabel("Are patients with a prior cancer okay?").append("//label").withText("Yes"));
            click(Ext4HelperWD.Locators.formItemWithLabel("Are samples collected after neoadjuvant therapy okay?").append("//label").withText("Yes"));
            click(Ext4HelperWD.Locators.formItemWithLabelContaining("Are patients with a history of BCC").append("//label").withText("Yes"));
            clickButton("Next", 0);
            _ext4Helper.waitForMaskToDisappear();
            clickButton("Add Record", 0);
            {// Tissue Samples dialog
                _extHelper.waitForExtDialog("Add Tissue Samples");
                waitForElementToDisappear(Locator.css(".x4-mask").index(2));

                // Tissue Type Information
                _ext4Helper.selectComboBoxItem("Tissue Type:", requestType);
                _ext4Helper.selectComboBoxItem("Anatomical Site:", "Stomach");
                setFormElement(Ext4HelperWD.Locators.formItemWithLabel("Minimum Size:").append("//input"), "0.5cm x 0.4cm x 0.3cm");
                setFormElement(Ext4HelperWD.Locators.formItemWithLabel("Preferred Size:").append("//input"), "0.5cm x 0.4cm x 0.3cm");
                _ext4Helper.selectComboBoxItem("Preservation:", "Flash Frozen");

                _extHelper.clickExtButton("Add Tissue Samples", "Save", 0);
            }// done with Tissue Samples dialog

            _extHelper.waitForExtDialogToDisappear("Add Tissue Samples");
            clickButton("Next", "Please indicate all sample types needed from each specimen donor");
            clickButton("Next", "Did you request fresh");
            click(Ext4HelperWD.Locators.formItemWithLabelContaining("Did you request fresh tissue or a blood draw?").append("//label").withText("Yes"));
            click(Ext4HelperWD.Locators.formItemWithLabelContaining("Can you arrange for pick up").append("//label").withText("Yes"));
            click(Ext4HelperWD.Locators.formItemWithLabelContaining("Does your study allow fresh samples").append("//label").withText("Yes"));
            clickButton("Next", 0);
            clickButton("Save");
        }
        waitForGridToLoad("tr", "x4-grid-row", allRequestTypes.length);
        assertTextPresentInThisOrder(allRequestTypes);
        clickTab("Overview");
        waitForText("There are 4 sample requests that have not yet been submitted.");
        waitForText("There are 0 sample requests that have already been submitted.");
        waitForText("There is 1 study registration that has been created.");

        log("Submit sample requests from the requestor subfolder");
        clickTab("Request Samples");
        waitForElement(Locator.id("submitted-dashboard-3").append("//span").withText("No sample requests to show"));
        waitForElement(Locator.id("locked-dashboard-4").append("//span").withText("No sample requests to show"));
        for (int i = 0; i < submittedRequestTypes.length; i++)
        {
            // note: the Request IDs for a sample request in a new folder should now be predictable (starting at 1)
            waitAndClick(Locator.id("pending-dashboard-2").append(Locator.linkWithText(String.valueOf(i+1))));
            waitForElement(Locator.xpath("//label[text() = 'Tissue Type:']/../../td/div[text() = '" + submittedRequestTypes[i] + "']"));
            clickAndWait(Locator.linkWithText("view details", 1));
            waitForGridToLoad("tr", "x4-grid-row", 1);
            waitAndClick(Locator.xpath("//li[text()='Save / Submit']"));
            clickButton("Submit completed form");
        }

        waitForElement(Locator.id("pending-dashboard-2").append(Locator.linkWithText("4")));
        waitForElement(Locator.id("submitted-dashboard-3").append(Locator.linkWithText("1")));
        waitForElement(Locator.id("submitted-dashboard-3").append(Locator.linkWithText("2")));
        waitForElement(Locator.id("submitted-dashboard-3").append(Locator.linkWithText("3")));
        waitForElement(Locator.id("locked-dashboard-4").append("//span").withText("No sample requests to show"));

        clickTab("Overview");
        waitForText("There is 1 sample request that has not yet been submitted.");
        waitForText("There are 3 sample requests that have already been submitted.");
        waitForText("There is 1 study registration that has been created.");

        stopImpersonating();

        goToModule("Dumbster");
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyResearchCoordDashboard()
    {
        impersonateRole(NWBT_RESEARCH_COORD_ROLE);

        log("Verify submitted requests show up in RC Dashboard");
        goToProjectHome();
        clickTab("New Sample Requests");
        waitForGridToLoad("div", "x4-grid-group-title", 1); // requests grouped by study
        waitForGridToLoad("tr", "x4-grid-row", submittedRequestTypes.length);
        assertElementPresent(getGroupingTitleLocator(registrationLabel));
        assertTextPresentInThisOrder(submittedRequestTypes);
        assertTextNotPresent(unsubmittedRequestTypes);

        log("Update request status and categories");
        for (int i = 0; i < submittedRequestTypes.length; i++)
            setRequestStatusAndCategory(i, submittedRequestTypes[i], NWBT_REQUEST_STATUSES[i].toString(), NWBT_REQUEST_CATEGORIES[i]);
        goToProjectHome();
        clickTab("New Sample Requests");
        waitForGridToLoad("div", "x4-grid-group-title", 1); // requests grouped by study
        waitForGridToLoad("tr", "x4-grid-row", submittedRequestTypes.length);
        assertTextPresentInThisOrder(NWBT_REQUEST_CATEGORIES);
        assertTextPresentInThisOrder(Arrays.copyOfRange(NWBT_REQUEST_STATUSES, 0, submittedRequestTypes.length - 1));

        log("Verify audit records");
        Locator loc = getEditLinkLocator(submittedRequestTypes[0], false);
        click(loc);
        _extHelper.waitForExtDialog("Edit Sample Request : 1-1");
        click(Locator.linkContainingText("view history"));
        _extHelper.waitForExtDialog("Status Change History : 1-1");
        waitForElement(Locator.xpath("//div[text() = 'TissueRecordId']"));
        assertTextPresent("Sample Request Status Changed", "Submitted", "resource and status changed");
        clickButton("Close", 0);
        clickButton("Cancel", 0);

        loc = getEditLinkLocator(submittedRequestTypes[1], false);
        click(loc);
        _extHelper.waitForExtDialog("Edit Sample Request : 1-2");
        click(Locator.linkContainingText("view history"));
        _extHelper.waitForExtDialog("Status Change History : 1-2");
        waitForElement(Locator.xpath("//div[text() = 'TissueRecordId']"));
        assertTextPresent("Sample Request Status Changed", "Oversight Review", "Submitted", "resource and status changed");
        clickButton("Close", 0);
        clickButton("Cancel", 0);

        stopImpersonatingRole();
    }

    private void waitForGridToLoad(final String tag, final String className, final int expectedCount)
    {
        Locator l = Locator.xpath("//" + tag + "[contains(@class, '" + className + "')]");
        startTimer();
        while (getElementCount(l) < expectedCount && elapsedMilliseconds() < WAIT_FOR_JAVASCRIPT)
            sleep(1000);
        assertElementPresent(l, expectedCount);
    }

    private Locator getGroupingTitleLocator(String title)
    {
        return Locator.xpath("//div[contains(@class, 'x4-grid-group-title')]//td[contains(text(), '" + title + "')]");
    }

    private void setRequestStatusAndCategory(int index, String label, String status, String category)
    {
        Locator loc = getEditLinkLocator(label, false);
        click(loc);
        _extHelper.waitForExtDialog("Edit Sample Request : 1-" + (index+1));
        sleep(1000); // this is tricky because there is a loading mask for the combos, but they can load very quickly so that the test misses it if we wait for the mask to disappear
        _ext4Helper.selectComboBoxItem("NWBT Resource:", category);
        _ext4Helper.selectComboBoxItem("Status:", status);
        setFormElement(Locator.name("Comment"), "resource and status changed");
        sleep(500); // update button is enabled based on form state
        clickButton("Update", 0);
        waitForElement(Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner ') and contains(text(), '" + category + "')]"));
        assertTextPresent(status);
    }

    private Locator getEditLinkLocator(String label, boolean isLink)
    {
        if (isLink)
            return Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//a[contains(text(),'" + label + "')]/../../..//td//div//span[contains(@class, 'edit-views-link')]");
        else
            return Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner') and contains(text(),'" + label + "')]/../..//td//div//span[contains(@class, 'edit-views-link')]");
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupSurveyDesignsAndStudy()
    {
        if (designs.size() == 0)
            configureDesigns();

        log("Create survey designs in project folder");
        for (Map<String, String> entry : designs)
        {
            createSurveyDesign(getProjectName(), null, "Manage", entry.get("label"), entry.get("description"),
                    "biotrust", entry.get("table"), entry.get("metadataPath"));
        }

        log("Configure dashboard");
        clickTab("New Sample Requests");
        waitForText("No sample requests to show");
        customizeDashboard("RC Dashboard - New Sample Requests", designs.get(0).get("label"));
        clickFolder(requestorFolder1);
        clickTab("My Studies");
        customizeDashboard("Requestor Dashboard - My Studies", designs.get(0).get("label"));

        log("Create study registration to use for sample requests");
        pushLocation();
        impersonate(NWBT_PRINCIPAL_INVESTIGATOR);
        popLocation();
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        waitForText("No study registrations to show", 1, WAIT_FOR_PAGE);
        clickButton("Create new study registration");
        List<Map<String, String>> fields = new ArrayList<Map<String, String>>();
        fields.add(createFieldInfo("Study Information", "studydescription", "test study description: " + registrationLabel));
        fields.add(createComboFieldInfo("Study Information", "IRB approval status", "Approved Human Subjects Research"));
        fields.add(createFieldInfo("Study Information", "irbfilenumber", "TEST123"));
        fields.add(createFieldInfo("Study Information", "irbexpirationdate", "2013-03-07"));
        fields.add(createComboFieldInfo("Study Information", "Reviewing IRB", "Other"));
        fields.add(createRadioFieldInfo("Study Information", "Do you anticipate submitting data from this study to a public database (e.g. dbGAP)?", "Yes"));
        fields.add(createComboFieldInfo("Contact Information", "Study Principal Investigator", "pi nwbiotrust"));
        fields.add(createFieldInfo("Billing", "billingcomments", "test funding source comments"));
        createNewStudyRegistration(registrationLabel, fields);
        waitForGridToLoad("tr", "x4-grid-row", 1);

        stopImpersonating();
    }

    private Map<String, String> createFieldInfo(String section, String name, String value)
    {
        Map<String, String> info = new HashMap<>();
        info.put("section", section);
        info.put("name", name);
        info.put("value", value);
        return info;
    }

    private Map<String, String> createRadioFieldInfo(String section, String label, String boxLabel)
    {
        Map<String, String> info = new HashMap<>();
        info.put("section", section);
        info.put("label", label);
        info.put("boxLabel", boxLabel);
        return info;
    }

    private Map<String, String> createComboFieldInfo(String section, String label, String selection)
    {
        Map<String, String> info = new HashMap<>();
        info.put("section", section);
        info.put("label", label);
        info.put("selection", selection);
        return info;
    }

    private void createNewStudyRegistration(String label, List<Map<String, String>> fields)
    {
        waitForText("Study Name*");
        setFormElement(Locator.name("_surveyLabel_"), label);
        for (Map<String, String> field : fields)
        {
            Locator sectionLoc = Locator.xpath("//li[text()='" + field.get("section") + "']");
            if (isElementPresent(sectionLoc))
                click(sectionLoc);

            if (field.get("boxLabel") != null)
            {
                Locator l = Locator.xpath("//div[./table//label[text()='" + field.get("label") + "']]//label[text()='" + field.get("boxLabel") + "']");
                if (isElementPresent(l))
                    _ext4Helper.selectRadioButton(field.get("label"), field.get("boxLabel"));
                else // can't use the ext4helper because our label is wrapped in a <span>
                    click(Locator.xpath("//div[./table//label/span[text()='" + field.get("label") + "']]//label[text()='" + field.get("boxLabel") + "']"));
            }
            else if (field.get("selection") != null)
            {
                _ext4Helper.selectComboBoxItem(field.get("label"), field.get("selection"));
            }
            else
            {
                waitForElement(Locator.name(field.get("name")));
                setFormElement(Locator.name(field.get("name")), field.get("value"));
            }
        }
        click(Locator.xpath("//li[text()='Save / Cancel']"));
        clickButton("Save");

    }

    private void customizeDashboard(String webpartTitle, String formName)
    {
        portalHelper.clickWebpartMenuItem(webpartTitle, "Customize");
        waitForText(formName);
        _ext4Helper.checkCheckbox(formName);
        clickButton("Save");
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupProvisionTableForResponses()
    {
        log("Create provision table in biotrust schema for responses");
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Manage"));
        waitForText("Existing Request Response Schemas");
        click(Locator.linkWithText("Create new Request Response Schema"));
        _extHelper.waitForExtDialog("New Request Response Schema");
        setFormElement(Locator.name("queryName"), provisionTableName);
        sleep(500); // give the save button a split second to be enabled on form change
        clickButton("Save");
        waitForText("Edit Fields in " + provisionTableName);
        _listHelper.addField(new ListHelper.ListColumn("testfield1", "Test Field 1", ListHelper.ListColumnType.String, null));
        clickButton("Save");
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Manage"));
        assertElementPresent(Locator.linkWithText(provisionTableName));
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupSurveysTableDefinition()
    {
        log("Add fields to extensible survey.Surveys table");
        goToProjectHome();
        goToSurveysTable("create definition");
        waitForText("Extensible Table 'Surveys'");
        waitForText("No fields have been defined.");
        _listHelper.addField("Field Properties", 0, "Category", "Category", ListHelper.ListColumnType.Integer);
        _listHelper.setColumnType(0, new ListHelper.LookupInfo("", "biotrust", "RequestCategory"));
        clickButton("Save");

        log("Verify that the subfolders use the same Surveys domain");
        clickFolder(requestorFolder1);
        goToSurveysTable("view data");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("Category");
        _customizeViewsHelper.applyCustomView();
        assertTextPresentInThisOrder("Modified", "Status", "Category");
    }

    private void goToSurveysTable(String textLink)
    {
        goToSchemaBrowser();
        selectQuery("survey", "Surveys");
        waitForText(textLink);
        clickAndWait(Locator.linkContainingText(textLink));
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupProjectAdminProperties()
    {
        log("Populate the Request Category dashboard lookup table");
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Manage"));
        Object[] categoriesToInsert = checkForValuesToInsert("RequestCategory", "Category", NWBT_REQUEST_CATEGORIES);
        List<Map<String,Object>> rows = new ArrayList<>();
        for (Object category : categoriesToInsert)
        {
            Map<String, Object> rowMap = new HashMap<>();
            rowMap.put("Category", category.toString());
            rowMap.put("SortOrder", Arrays.asList(NWBT_REQUEST_CATEGORIES).indexOf(category.toString()) + 1);
            rows.add(rowMap);
        }
        insertLookupTableRecords("RequestCategory", rows);

        log("Populate the Request Status dashboard lookup table");
        Object[] statusesToInsert = checkForValuesToInsert("RequestStatus", "Status", NWBT_REQUEST_STATUSES);
        rows = new ArrayList<>();
        for (Object status : statusesToInsert)
        {
            Map<String, Object> rowMap = new HashMap<>();
            rowMap.put("Status", status.toString());
            rowMap.put("SortOrder", ((NwbtRequestStatuses)status).sortOrder());
            rowMap.put("LockedState", ((NwbtRequestStatuses)status).isLocked());
            rowMap.put("ApprovalState", ((NwbtRequestStatuses)status).isApproval());
            rows.add(rowMap);
        }
        insertLookupTableRecords("RequestStatus", rows);

        log("Populate the Document Types lookup table");
        Object[] docTypesToInsert = checkForValuesToInsert("DocumentTypes", "Name", NWBT_DOCUMENT_TYPES);
        rows = new ArrayList<>();
        for (Object docType : docTypesToInsert)
        {
            int index = Arrays.asList(NWBT_DOCUMENT_TYPES).indexOf(docType);
            Map<String, Object> rowMap = new HashMap<>();
            rowMap.put("Name", docType.toString());
            // set the first doc type as not allowing multiple uploads
            rowMap.put("AllowMultipleUpload", NWBT_DOCUMENT_TYPE_FLAGS[index][0]);
            rowMap.put("Expiration", NWBT_DOCUMENT_TYPE_FLAGS[index][1]);
            rows.add(rowMap);
        }
        insertLookupTableRecords("DocumentTypes", rows);
    }

    private void insertLookupTableRecords(String queryName, List<Map<String,Object>> rowsMap)
    {
        if (!rowsMap.isEmpty())
        {
            log("Inserting values into the lookup table via InsertRows API");
            Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
            InsertRowsCommand insertCommand = new InsertRowsCommand("biotrust", queryName);
            insertCommand.setRows(rowsMap);
            try
            {
                SaveRowsResponse saveResp = insertCommand.execute(cn, getProjectName());
                Assert.assertEquals("Problem inserting records", saveResp.getRowsAffected(), (long)rowsMap.size());
            }
            catch (Exception e)
            {
                Assert.fail(e.getMessage());
            }
        }
    }

    private Object[] checkForValuesToInsert(String queryName, String colName, Object[] values)
    {
        List<Object> valuesToInsert = new ArrayList<>(Arrays.asList(values));

        log("Checking for values to be inserted via SelectRows API");
        Filter filter = new Filter(colName, StringUtils.join(values, ";"), Filter.Operator.IN);
        SelectRowsResponse response = executeSelectRowCommand("biotrust", queryName, ContainerFilter.Current, "/" + getProjectName(), Collections.singletonList(filter));
        List<String> col = new ArrayList<>();
        for (Map<String, Object> row : response.getRows())
        {
            col.add(row.get(colName).toString());
        }

        Iterator<Object> it = valuesToInsert.iterator();
        while (it.hasNext())
        {
            Object item = it.next();
            if (col.contains(item.toString()))
                it.remove();
        }

        return valuesToInsert.toArray();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyFolderTypes()
    {
        log("Verify folder type default webparts");
        goToProjectHome();
        verifyFolderTabLinks(new String[]{"Overview", "New Sample Requests", "Active Sample Requests", "Patient ID", "Patient Consent", "Sample Orders", "Manage"});
        verifyWebpartTitleOrder(new String[]{"Pending Sample Requests"});
        clickFolder(requestorFolder1);
        verifyWebpartTitleOrder(new String[]{"Overview"});
        verifyFolderTabLinks(new String[]{"Overview", "Request Samples", "My Studies", "Contacts"});
        clickFolder(requestorFolder2);
        verifyWebpartTitleOrder(new String[]{"Overview"});
    }

    private void verifyFolderTabLinks(String[] folders)
    {
        for (String folder : folders)
            assertElementPresent(Locator.linkWithText(folder));
    }

    private void verifyWebpartTitleOrder(String[] titles)
    {
        Locator titleLoc = Locator.css(".labkey-wp-title-text");
        Iterator<WebElement> it = titleLoc.findElements(_driver).iterator();
        WebElement curEl = it.next();
        for (String expectedTitle : titles)
        {
            while (!curEl.getText().equals(expectedTitle))
            {
                if (it.hasNext())
                    curEl = it.next();
                else
                {
                    assertElementPresent(titleLoc.withText(expectedTitle));
                    org.junit.Assert.fail("Webpart found out of order: " + expectedTitle);
                }
            }
        }
    }

    private void verifyReviewerWorkflow()
    {
        log("Verify reviewer views");
        goToProjectHome();
        clickTab("New Sample Requests");
        waitForGridToLoad("div", "x4-grid-group-title", 1); // requests grouped by study
        waitForGridToLoad("tr", "x4-grid-row", submittedRequestTypes.length);
        assertElementPresent(getGroupingTitleLocator(registrationLabel));
        assertTextPresentInThisOrder(submittedRequestTypes);
        assertTextNotPresent(unsubmittedRequestTypes);

        click(Locator.linkContainingText("Oversight Review"));

        impersonateGroup(NWBT_REVIEWER_GROUP, false);

        waitForGridToLoad("div", "x4-grid-group-title", 1); // requests grouped by study
        waitForGridToLoad("tr", "x4-grid-row", 1);
        assertTextPresent(submittedRequestTypes[1]);

        click(Locator.linkContainingText("1-2"));

        waitForText("Oversight Review Assessment Details");
        _ext4Helper.selectRadioButton("Recommendation:", "Approve, no changes needed");

        if (isElementPresent(Locator.xpath("//textarea[@name='Comment']")))
            setFormElement(Locator.xpath("//textarea[@name='Comment']"), "Approved");

        waitAndClick(Locator.ext4ButtonEnabled("Submit"));
        waitForGridToLoad("div", "x4-grid-group-title", 1); // requests grouped by study
        waitForGridToLoad("tr", "x4-grid-row", 1);
        assertTextPresent(submittedRequestTypes[1]);
        stopImpersonatingGroup();

        log("Verify reviewer views");

        goToProjectHome();
        impersonateRole(NWBT_RESEARCH_COORD_ROLE);
        clickTab("New Sample Requests");
        waitForGridToLoad("div", "x4-grid-group-title", 1); // requests grouped by study
        waitForGridToLoad("tr", "x4-grid-row", submittedRequestTypes.length);

        click(Locator.linkContainingText("1-2"));
        waitForText("Sample Request Details");
        assertTextPresent("Oversight Review Approval Response");
        assertTextPresent("Oversight Review, Approved");
        clickButton("Close", 0);

        stopImpersonatingRole();
    }

    private void verifyEmailNotifications()
    {
        goToModule("Dumbster");

        waitForElement(Locator.xpath("//div[text()='Message']"));
        assertElementPresent(Locator.linkWithText("A BioTrust Sample request has been updated"), 1);
        assertElementPresent(Locator.linkWithText("A BioTrust Sample request has been submitted"), 3);
        assertElementPresent(Locator.linkWithText("A BioTrust sample request is ready for oversight review"), 1);
        click(Locator.linkWithText("A BioTrust Sample request has been updated"));
        assertTextPresent("A BioTrust sample request has been updated");
        click(Locator.linkWithText("A BioTrust Sample request has been submitted"));
        assertTextPresent("A BioTrust sample request has been submitted");
        click(Locator.linkWithText("A BioTrust sample request is ready for oversight review"));
        assertTextPresent("A BioTrust sample request of type: Tissue has been marked for oversight ");
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupUsersAndPermissions()
    {
        log("Create all of the users for this test");
        for (String user : NWBT_USERS)
            createUser(user, null);

        log("Grant the appropriate permissions for each of these users at the project level");
        goToProjectHome();
        enterPermissionsUI();
        setUserPermissions(NWBT_STUDY_CONTACT, "Reader");
        setUserPermissions(NWBT_PRINCIPAL_INVESTIGATOR, "Reader");
        setUserPermissions(NWBT_FACULTY_CHAIR, "Reader");
        setUserPermissions(NWBT_FACULTY_REVIEWER, "Reader");
        clickButton("Save and Finish");
        addUserToProjGroup(NWBT_RESEARCH_COORD, getProjectName(), NWBT_RESEARCH_COORD_GROUP);
        addUserToProjGroup(NWBT_TISSUE_REVIEWER, getProjectName(), NWBT_REVIEWER_GROUP);

        log("Grant the appropriate permissions for 1st requestor subfolder");
        //note: don't give them perm to the 2nd requestor folder so that we can test the container permissions
        clickFolder(requestorFolder1);
        enterPermissionsUI();
        setUserPermissions(NWBT_RESEARCH_COORD, "NWBT Research Coordinator");
        setUserPermissions(NWBT_STUDY_CONTACT, "NWBT Additional Study Contact");
        setUserPermissions(NWBT_PRINCIPAL_INVESTIGATOR, "NWBT Principal Investigator");
        setUserPermissions(NWBT_FACULTY_CHAIR, "NWBT Faculty Chair");
        setUserPermissions(NWBT_FACULTY_REVIEWER, "NWBT Faculty Reviewer");
        clickButton("Save and Finish");
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupResearchCoordAndRequstorFolders()
    {
        log("Create project folder and requestor subfolders");
        // use the project folder as the Research Coordinator folder, this will enable the survey and biotrust modules
        _containerHelper.createProject(getProjectName(), "NW BioTrust Research Coordinator");
        // create two requestor folders for this project
        _containerHelper.createSubfolder(getProjectName(), requestorFolder1, "NW BioTrust Specimen Requestor");
        _containerHelper.createSubfolder(getProjectName(), requestorFolder2, "NW BioTrust Specimen Requestor");

        // set up the rc email notification
        List<ModulePropertyValue> properties = new ArrayList<>();
        properties.add(new ModulePropertyValue("BioTrust", "/", "RC email address", NWBT_RC_EMAIL));

        setModuleProperties(properties);
    }

    private void deleteDashboardLookupRows(String tableName, String filterColName, String[] valuesToBeDeleted)
    {
        log("Deleting values from lookup table via DeleteRows API");
        Filter filter = new Filter(filterColName, StringUtils.join(valuesToBeDeleted, ";"), Filter.Operator.IN);
        SelectRowsResponse response = executeSelectRowCommand("biotrust", tableName, ContainerFilter.Current, "/" + getProjectName(), Collections.singletonList(filter));
        if (response.getRows().size() > 0)
        {
            Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
            DeleteRowsCommand deleteCommand = new DeleteRowsCommand("biotrust", tableName);
            deleteCommand.setRows(response.getRows());
            try
            {
                SaveRowsResponse saveResp = deleteCommand.execute(cn, getProjectName());
                Assert.assertEquals("Problem deleting records", saveResp.getRowsAffected(), (long)response.getRows().size());
            }
            catch (Exception e)
            {
               Assert.fail(e.getMessage());
            }
        }
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void configureDesigns()
    {
        log("download survey metadata");
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Manage"));
        waitForText("Metadata for Study Registration and Sample Requests");
        click(Locator.linkWithText("Study Registration"));
        downloadFileFromLink(Locator.linkWithText("Sample Request"));
        downloadFileFromLink(Locator.linkWithText("Tissue Sample Record"));
        downloadFileFromLink(Locator.linkWithText("Blood Sample Record"));

        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return studyRegistrationJson.exists();
            }
        }, "failed to download study registration json", WAIT_FOR_JAVASCRIPT);

        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return sampleRequestJson.exists();
            }
        }, "failed to download sample request json", WAIT_FOR_JAVASCRIPT);

        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return tissueJson.exists();
            }
        }, "failed to download tissue sample request json", WAIT_FOR_JAVASCRIPT);

        waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return bloodSampleJson.exists();
            }
        }, "failed to download blood sample request json", WAIT_FOR_JAVASCRIPT);

        Map<String, String> design = new HashMap<String, String>();
        design.put("label", "StudyRegistration");
        design.put("description", "");
        design.put("table", "StudyRegistrations");
        design.put("metadataPath", studyRegistrationJson.getAbsolutePath());
        designs.add(design);

        design = new HashMap<String, String>();
        design.put("label", "SampleRequest");
        design.put("description", "");
        design.put("table", "SampleRequests");
        design.put("metadataPath", sampleRequestJson.getAbsolutePath());
        designs.add(design);

        design = new HashMap<String, String>();
        design.put("label", "TissueSample");
        design.put("description", "");
        design.put("table", "TissueRecords");
        design.put("metadataPath", tissueJson.getAbsolutePath());
        designs.add(design);

        design = new HashMap<String, String>();
        design.put("label", "BloodSample");
        design.put("description", "");
        design.put("table", "TissueRecords");
        design.put("metadataPath", bloodSampleJson.getAbsolutePath());
        designs.add(design);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        goToHome();
        if(isElementPresent(Locator.linkWithText(getProjectName())))
        {
            deleteSurveyDesign();
        }
        deleteUsers(afterTest, NWBT_USERS);
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/biotrust";
    }
}
