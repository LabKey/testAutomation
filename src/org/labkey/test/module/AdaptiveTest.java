/*
 * Copyright (c) 2010 LabKey Corporation
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

package org.labkey.test.module;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.ExtHelper;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: Trey Chadick
 * Date: May 27, 2010
 * Time: 4:19:49 PM
 */
public class AdaptiveTest extends BaseSeleniumWebTest
{
    private static final String ADAPTIVE_NOTIFY_EMAIL = "orders-notify@adaptivetcr.com";
    private static final String DATE_FORMAT = "MM/dd/yyyy";
    private static final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    private static final String PROJECT_NAME = "AdaptiveTest Project";
    private static final String CUSTOMER_A = "CustomerA";
    private static final String USERA1 = CUSTOMER_A.toLowerCase() + "_user1@adaptive.test";
    private static final String CUSTOMER_A_GROUP = CUSTOMER_A + "Group";
    private static final String CUSTOMER_A_FOLDER = CUSTOMER_A + "Folder";
    private static final String TEST_ORDER_1 = "Order Placed On " + sdf.format(new Date().getTime());
    private static final String FIRST_NAME = "Labkey";
    private static final String LAST_NAME = "Test";
    private static final String ADDRESS = "1100 Fairview Ave";
    private static final String CITY = "Seattle";
    private static final String STATE = "WA";
    private static final String ZIP = "98102";
    private static final String PHONE = "(000) 456-7890";
    private static final String PLATE_1_ID = "p789.1";
    private static String OUTGOING_TRACKING = "";
    private static String RETURN_TRACKING = "";
    private static final String SAMPLE_DATA_DIR = "/adaptive-tcr/sampleData/";
    private static final String RESOURCE_DIR = "/resources/adaptive/";
    private static final String MANIFEST_FILE_A = "manifest_789.1.xls";
    private static final String QC_FILE_A = "QC_789.1.xls";
    private static final String RUN_NAME_ALPHA = "Run Alpha";
    private static final String PCR_FILE_A = "PCR_789.1.xls";
    private static final String SAMPLES_FILE_A = "SEQ_789.1.xls";
    private static final String SEQUENCE_DATA_FILE_A = "VM-1.1.adap.txt";
    private static final String SAMPLE_A = "gemini789.1-028";
    private static final String SEQUENCE_SAMPLE_A = "VM-1.1";
    private static final String SEQUENCE_DATA_FILE_B = "VM-2.1.adap.txt";
    private static final String SAMPLE_B = "gemini789.1-009";
    private static final String SEQUENCE_SAMPLE_B = "VM-2.1";
    private static final String VIEW_NAME_A = "Test View A";
    private static final String VIEW_NAME_B = "Test View B";
    private static final String VIEW_NAME_C = "Test View C";
    private static final String VIEW_NAME_D = "Test View D";
    private static final String VIEW_NAME_E = "Test View E";
    private static final String DEMO_SITE = "Test Drive";


    protected void doCleanup()
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps()
    {
        setupAdaptiveProject();
        // TODO: Verify that customers have properly limited access.
        verifyDemoSite(); // Could go later if uploaded samples to be tested in demo
        verifySetupMessages();

        createNewOrder();
        verifyCreateOrderMessages();

        receiveOrder();
        verifyOrderAckMessages();

        shipBox();
        verifyShipBoxMessages();

        uploadSampleInformation();
        verifySampleMessages();

        receiveBox();
        verifyReceiveBoxMessages();

        QCSamples();
        PCRSamples();
        loadSequenceSamples();

        postProcessResults();
        verifyPostProcessingMessages();

        singleSampleAnalysis();
        compareSamples();
        saveViews();
        verifySavedViews();
        //editSavedViews();
    }

    protected void setupAdaptiveProject()
    {
        createProject(PROJECT_NAME, "Adaptive Home Folder");
        enableModule(PROJECT_NAME, "Dumbster");

        clickAdminMenuItem("Go To Module", "More Modules", "Dumbster");
        uncheckCheckbox("emailRecordOn");
        checkCheckbox("emailRecordOn");

        //Set up adaptive UI
        createHeader();

        //Add a customer folder and add a customer group
        createSubfolder(PROJECT_NAME, "CustomerSites", CUSTOMER_A_FOLDER, "Adaptive Customer Site", null, false);
        createPermissionsGroup(CUSTOMER_A_GROUP, USERA1);
        clickNavButton("Save and Finish");
        clickLinkWithText(CUSTOMER_A_FOLDER);
        enterPermissionsUI();
        setPermissions(CUSTOMER_A_GROUP, "Customer");
        clickNavButton("Save and Finish");

        // Setup demo site permissions
        log("Setting up " + DEMO_SITE + " permissions.");
        clickLinkWithText(DEMO_SITE);
        enterPermissionsUI();
        setSiteGroupPermissions("All Site Users", "Reader");
        clickNavButton("Save and Finish");
    }

    private void verifySetupMessages()
    {
        goToModule("Messages");
        assertTextPresent("Let's get Started!");
    }

    private void createNewOrder()
    {
        clickLinkWithText(CUSTOMER_A_FOLDER);
        pushLocation();
        impersonate(USERA1);
        popLocation();
        clickLinkWithText("Place Order");

        addItemToCart("Human", "TCRB", "1", 7);
        addItemToCart("Human", "TCRB", "2", 8);
        addItemToCart("Human", "TCRB", "3", 9);
        addItemToCart("Human", "TCRB", "4", 72);

        clickLinkWithText("Checkout");

        // Step 2
        clickLinkWithText("Continue");

        // TODO: verify that field minimums are enforced
        // TODO: verify that total price updates correctly

        // Step 3
        // Fill out customer info.
        setFormElement(Locator.xpath("//div[./span[text() = 'Ship To']]/..//input[@name = 'firstName']"), FIRST_NAME);
        setFormElement(Locator.xpath("//div[./span[text() = 'Ship To']]/..//input[@name = 'lastName']"), LAST_NAME);
        setFormElement(Locator.xpath("//div[./span[text() = 'Ship To']]/..//input[@name = 'address1']"), ADDRESS);
        setFormElement(Locator.xpath("//div[./span[text() = 'Ship To']]/..//input[@name = 'city']"), CITY);
        setFormElement(Locator.xpath("//div[./span[text() = 'Ship To']]/..//input[@name = 'state']"), STATE);
        setFormElement(Locator.xpath("//div[./span[text() = 'Ship To']]/..//input[@name = 'postalCode']"), ZIP);
        setFormElement(Locator.xpath("//div[./span[text() = 'Ship To']]/..//input[@name = 'phoneNumber']"), PHONE);
        setFormElement(Locator.xpath("//div[./span[text() = 'Bill To']]/..//input[@name = 'firstName']"), FIRST_NAME);
        setFormElement(Locator.xpath("//div[./span[text() = 'Bill To']]/..//input[@name = 'lastName']"), LAST_NAME);
        setFormElement(Locator.xpath("//div[./span[text() = 'Bill To']]/..//input[@name = 'address1']"), ADDRESS);
        setFormElement(Locator.xpath("//div[./span[text() = 'Bill To']]/..//input[@name = 'city']"), CITY);
        setFormElement(Locator.xpath("//div[./span[text() = 'Bill To']]/..//input[@name = 'state']"), STATE);
        setFormElement(Locator.xpath("//div[./span[text() = 'Bill To']]/..//input[@name = 'postalCode']"), ZIP);
        setFormElement(Locator.xpath("//div[./span[text() = 'Bill To']]/..//input[@name = 'phoneNumber']"), PHONE);
        // TODO: verify that required fields are enforced

        clickLinkWithText("Continue");

        // Step 4

        setFormElement("paymentMethod", "Purchase Order");
        setFormElement("poNumber", "1");

        // Force form to notice that it is complete.
        selenium.fireEvent("poNumber", "blur");

        clickLinkWithText("Continue");

        // Step 5
        clickLinkWithText("Complete Order");

        // Step 6
        clickLinkWithText("Client Center");

        pushLocation();
        stopImpersonating();
        popLocation();

    }

    private void verifyCreateOrderMessages()
    {
        pushLocation();
        goToAdaptiveCustomerFolder(CUSTOMER_A_FOLDER);
        assertTextPresent("We will be shipping out a box shortly");

        // Verify notification emails.
        clickAdminMenuItem("Go To Module", "More Modules", "Dumbster");
        verifyMailRecord(ADAPTIVE_NOTIFY_EMAIL, "A new Adaptive order has been created");

        popLocation();
    }

    private void addItemToCart(String species, String locus, String resolution, int quantity)
    {
        int count = countText(quantity + " " + species) + 1;

        checkRadioButton("species", species);
        checkRadioButton("locus", locus);
        checkRadioButton("productID", resolution);
        setFormElement("quantity", Integer.toString(quantity));

        click(Locator.xpath("//div[@class = 'add-to-cart-button']/a"));

        waitForElement(Locator.xpath("//label[contains(text(), 'Quantity: " + quantity + "')]"), WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.xpath("//label[contains(text(), 'Locus: " + locus + "')]"), WAIT_FOR_JAVASCRIPT);
    }

    private void receiveOrder()
    {
        clickLinkWithText(TEST_ORDER_1);

        clickOrderStatus("Receive Order", 0);
    }

    private void verifyOrderAckMessages()
    {
        pushLocation();
        goToAdaptiveCustomerFolder(CUSTOMER_A_FOLDER);
        assertTextPresent("Adaptive has acknowledged your order");
        popLocation();
    }

    private void shipBox()
    {
        clickOrderStatus("Ship Box");

        OUTGOING_TRACKING = "OutTracking_" + UUID.randomUUID().toString();
        RETURN_TRACKING = "RetTracking_" + UUID.randomUUID().toString();

        setFormElement("outgoingTrackingNumber", OUTGOING_TRACKING);
        setFormElement("returnTrackingNumber", RETURN_TRACKING);
        setFormElement("plateid0", PLATE_1_ID);

        clickNavButton("Save");

    }

    private void verifyShipBoxMessages()
    {
        goToAdaptiveCustomerFolder(CUSTOMER_A_FOLDER);
        assertTextPresent("We've sent a box to you");

        // Verify notification emails.
        pushLocation();
        clickAdminMenuItem("Go To Module", "More Modules", "Dumbster");
        verifyMailRecord(2, USERA1, "Your Adaptive order has been shipped");
        verifyMailRecord(2, ADAPTIVE_NOTIFY_EMAIL, "An Adaptive order has been shipped");
        popLocation();
    }

    private void uploadSampleInformation()
    {
        pushLocation();
        impersonate(USERA1);
        popLocation();

        //todo: check messages

        clickAdaptiveMenuButton("Samples", "Upload Samples");

        File file = new File(getAdaptiveRoot() + SAMPLE_DATA_DIR, MANIFEST_FILE_A);
        setFormElement(Locator.xpath("//input[contains(@class, 'x-form-file') and @type = 'file']"), file.toString());
        //Form submits automatically on text entry.

        //Wait for plate data to load.
        waitForText("Plate Contents", WAIT_FOR_JAVASCRIPT * 2);
        //todo: verify plate data 

        clickNavButton("Next >", 0);
        clickNavButton("OK", 0);
        clickNavButton("Next >");
        waitForElement(Locator.xpath("//button[text() = 'Done']"), WAIT_FOR_JAVASCRIPT);

        assertTextPresent(PLATE_1_ID);
        click(Locator.xpath("//button[text() = 'Done']"));
        clickNavButton("Yes");

        clickLinkWithText("Back to Home");
        pushLocation();
        stopImpersonating();
        popLocation();

    }

    private void verifySampleMessages()
    {
        goToAdaptiveCustomerFolder(CUSTOMER_A_FOLDER);
        assertTextPresent("Thank you for uploading your manifest. Please ship the box to Adaptive for processing.");
    }

    private void receiveBox()
    {
        clickLinkWithText(TEST_ORDER_1);
        clickOrderStatus("Receive Box");

        setFormElement("adaptive-tracking-number", RETURN_TRACKING);
        // TODO: Test unexpected tracking number
        clickButton("Receive Shipment", 0);

        waitForElement(Locator.name("adaptive-plate-id"), WAIT_FOR_JAVASCRIPT);
        setFormElement("adaptive-plate-id", PLATE_1_ID);
        // TODO: Test multiple plates
        clickButton("Receive Plate", 0);

        clickButton("Finished", WAIT_FOR_PAGE);

        // TODO: Verify order's state has changed.

    }

    private void verifyReceiveBoxMessages()
    {
        goToAdaptiveCustomerFolder(CUSTOMER_A_FOLDER);
        assertTextPresent("A Box shipped by you has been received at Adaptive. We'll be processing your samples and will notify you when results are ready. Thank you.");

        // Verify notification emails.
        pushLocation();
        clickAdminMenuItem("Go To Module", "More Modules", "Dumbster");
        verifyMailRecord(2, USERA1, "Your Adaptive plates have been received");
        verifyMailRecord(2, ADAPTIVE_NOTIFY_EMAIL, "Your Adaptive plates have been received");
        popLocation();
    }

    private void QCSamples()
    {
        goToAdaptiveAdmin();
        clickLinkWithText("QC Samples");
        clickLinkWithText(PLATE_1_ID);

        File file = new File(getAdaptiveRoot() + SAMPLE_DATA_DIR, QC_FILE_A);
        setFormElement(Locator.xpath("//input[contains(@class, 'x-form-file') and @type = 'file']"), file.toString());

        waitForElement(Locator.linkWithText("Quality Control Samples"), WAIT_FOR_JAVASCRIPT);

        // TODO: Verify order's state has changed.
    }

    private void PCRSamples()
    {
        goToAdaptiveAdmin();
        clickLinkWithText("PCR Samples");

        setFormElement(Locator.id("run-name"), RUN_NAME_ALPHA);
        setFormElement(Locator.id("pcr-date"), "07/08/2010");
        // species : human is now the default on the form
        //setFormElement("Species", "Human");

        File file = new File(getAdaptiveRoot() + SAMPLE_DATA_DIR, PCR_FILE_A);
        setFormElement(Locator.id("file-upload-field-file"), file.toString());
        setFormElement(Locator.id("file-upload-field"), PCR_FILE_A);

        clickButton("Submit", 0);

        waitForElement(Locator.linkWithText("PCR Samples"), WAIT_FOR_JAVASCRIPT);

        // TODO: Verify order's state has changed.
    }

    private void loadSequenceSamples()
    {
        goToAdaptiveAdmin();
        clickLinkWithText("Load Sequence Samples");

        setFormElement("FlowcellID", RUN_NAME_ALPHA + " ID");
        File file = new File(getAdaptiveRoot() + SAMPLE_DATA_DIR, SAMPLES_FILE_A);
        setFormElement(Locator.id("file-upload-field-file"), file.toString());
        setFormElement(Locator.id("file-upload-field"), SAMPLES_FILE_A);

        clickButton("Submit", 0);

        waitForElement(Locator.linkWithText("Sequence Samples"), WAIT_FOR_JAVASCRIPT);

        // TODO: Verify order's state has changed.
    }

    private void postProcessResults()
    {
        goToAdaptiveAdmin();
        clickLinkWithText("Post Process Results");

        //upload sequence data (two files)
        File fileA = new File(getAdaptiveRoot() + SAMPLE_DATA_DIR + "/sequenceData", SEQUENCE_DATA_FILE_A);
        setFormElement("fileUpload-file", fileA);
        clickNavButton("Submit", 0);
        waitForText(fileA.getName(), 10000);
        File fileB = new File(getAdaptiveRoot() + SAMPLE_DATA_DIR + "/sequenceData", SEQUENCE_DATA_FILE_B);
        setFormElement("fileUpload-file", fileB);
        clickNavButton("Submit", 0);
        waitForText(fileB.getName(), 20000);

        //import sequence data
        ExtHelper.clickFileBrowserFileCheckbox(this, fileA.getName());
        ExtHelper.clickFileBrowserFileCheckbox(this, fileB.getName());
        selectImportDataAction("Process Adaptive Files");

        waitForPipelineJobsToComplete(1, "Adaptive Sequence Post Process Job");

        //QC Sequence Results
        goToAdaptiveAdmin();
        clickLinkWithText("QC Sequence Results");
        checkAllOnPage("samples");
        clickNavButton("Publish to Customer");
        getConfirmationAndWait();
    }

    private void verifyPostProcessingMessages()
    {
        goToAdaptiveCustomerFolder(CUSTOMER_A_FOLDER);
        assertTextPresent("Post processing is complete for sample: gemini789.1-009. Your data can be viewed now.");
        assertTextPresent("Post processing is complete for sample: gemini789.1-028. Your data can be viewed now.");

        // Verify notification emails.
        pushLocation();
        clickAdminMenuItem("Go To Module", "More Modules", "Dumbster");

        // TODO: fix when Issue 67 is resolved.
        //verifyMailRecord(2, USERA1, "Your Adaptive samples have been processed", SAMPLE_B, SEQUENCE_SAMPLE_B);
        verifyMailRecord(USERA1, "Your Adaptive samples have been processed", SAMPLE_B, SEQUENCE_SAMPLE_B);
        //verifyMailRecord(3, 4, USERA1, "Your Adaptive samples have been processed", SAMPLE_A, SEQUENCE_SAMPLE_A);
        verifyMailRecord(2, 2, USERA1, "Your Adaptive samples have been processed", SAMPLE_A, SEQUENCE_SAMPLE_A);
        popLocation();
    }

    private void singleSampleAnalysis()
    {
        clickLinkWithText(PROJECT_NAME);
        goToAdaptiveCustomerFolder(CUSTOMER_A_FOLDER);
        clickAdaptiveMenuButton("Analysis", "Single Sample Analysis");

        clickLinkWithText("View Data", false);
        waitForText("Validation error", WAIT_FOR_JAVASCRIPT); // No sample selected

        setAnalysisSample(SAMPLE_A);
        setAnalysisPlotType("2dhistogram");
        clickLinkWithText("View Data", false);
        waitForText("Validation error", WAIT_FOR_JAVASCRIPT); // No column selected

        setAnalysisPlotType("tabular");
        clickLinkWithText("View Data", false);
        waitForText("Nucleotide", WAIT_FOR_JAVASCRIPT); // Wait for table to load
        waitForText("% of Total", WAIT_FOR_JAVASCRIPT);

        assertProductsTableCellEquals(2, 2, "10,739,744"); // Productive Sequences
        assertProductsTableCellEquals(3, 5, "0.116%"); // Out of frame Uniques percent
        assertProductsTableCellEquals(5, 4, "101,493"); // Total Uniques

    }

    private void compareSamples()
    {
        goToAdaptiveCustomerFolder(CUSTOMER_A_FOLDER);
        clickAdaptiveMenuButton("Analysis", "Compare Samples");

        clickLinkWithText("View Data", false);
        waitForText("Validation error", WAIT_FOR_JAVASCRIPT); // No sample selected

        setAnalysisSample(SAMPLE_A);
        setAnalysisSample(SAMPLE_A, 1);
        clickLinkWithText("View Data", false);
        waitForText("Validation error", WAIT_FOR_JAVASCRIPT); // Same sample selected twice.

        setAnalysisSample(SAMPLE_B, 1);
        clickLinkWithText("View Data", false);
        waitForText("Unique Sequences", WAIT_FOR_JAVASCRIPT); // Wait for table to load

        assertStatisticsTableCellEquals(1, 2, 2, "5,398,980");

        clickLinkWithText("Sequences only in " + SAMPLE_A, false);
        assertStatisticsTableCellEquals(2, 2, 2, "5,340,764");
        clickLinkWithText("Sequences only in " + SAMPLE_B, false);
        assertStatisticsTableCellEquals(3, 2, 3, "272,526");
    }

    private void saveViews()
    {
        // View A, from compareSamples().  Complare samples [028x009]; Tabular; Default settings.
        saveCurrentView(VIEW_NAME_A);

        refresh();
        // Setup view B.  Compare samples [009x028]; Scatter; Switched search/filter settings.
        setAnalysisSample(SAMPLE_B);
        setAnalysisSample(SAMPLE_A, 1);
        click(Locator.id("compare-scatter"));
        expandAnalysisPanel("Choose How To Search Your Data");
        click(Locator.id("dna"));
        setFormElement("searchDepth", "42");
        expandAnalysisPanel("Choose How To Filter Your Data");
        uncheckCheckbox(Locator.id("ssprod"));
        checkCheckbox(Locator.id("ssnprod"));
        checkCheckbox(Locator.id("ssscodon"));
        saveCurrentView(VIEW_NAME_B);

        refresh();
        // Setup view C.  Single sample; Tabular; Switched search settings.
        uncheckCheckbox(Locator.xpath("//input[@type='checkbox' and ../label[text()='Compare to Another Sample']]"));
        setAnalysisSample(SAMPLE_B);
        click(Locator.id("tabular"));
        expandAnalysisPanel("Choose How To Search Your Data");
        click(Locator.id("dna"));           
        setFormElement("searchDepth", "37");
        saveCurrentView(VIEW_NAME_C);

        clickAdaptiveMenuButton("Analysis", "Single Sample Analysis");
        // Setup view D.  Single sample; 2D Histogram; [JGene x Unique]; Switched filter settings.
        /*setAnalysisSample(SAMPLE_A);
        click(Locator.id("2dhistogram"));

        setFormElement(Locator.xpath("//div[./label[text()='X-Axis']]//div//input[@type = 'text']"), "JGene");
        assertFormElementEquals(Locator.xpath("//div[./label[text()='X-Axis']]//div//input[@type = 'text']"), "JGene");

        setFormElement(Locator.xpath("//div[./label[text()='Count']]//div//input[@type = 'text']"), "Unique");
        assertFormElementEquals(Locator.xpath("//div[./label[text()='Count']]//div//input[@type = 'text']"), "Unique");
        
        expandAnalysisPanel("Choose How To Filter Your Data");
        uncheckCheckbox(Locator.id("ssprod"));
        checkCheckbox(Locator.id("ssnprod"));
        checkCheckbox(Locator.id("ssscodon"));
        saveCurrentView(VIEW_NAME_D);

        refresh();
        // Setup view E.  Single sample; 3D Histogram; Switched filter settings.
        uncheckCheckbox(Locator.xpath("//input[@type='checkbox' and ../label[text()='Compare to Another Sample']]"));
        setAnalysisSample(SAMPLE_B);
        click(Locator.id("3dhistogram"));
        setFormElement(Locator.xpath("//div[./label[text()='Count']]//div//input[@type = 'text']"), "Average");
        expandAnalysisPanel("Choose How To Filter Your Data");
        uncheckCheckbox(Locator.id("ssprod"));
        checkCheckbox(Locator.id("ssnprod"));
        checkCheckbox(Locator.id("ssscodon"));
        saveCurrentView(VIEW_NAME_E);*/
    }

    private void verifySavedViews()
    {
        clickLinkWithText(PROJECT_NAME);
        goToAdaptiveCustomerFolder(CUSTOMER_A_FOLDER);
        clickAdaptiveMenuButton("Analysis", "Compare Samples");

        loadSavedView(VIEW_NAME_A); // View A, from compareSamples().  Complare samples [028x009]; Tabular; Default settings.
        assertElementContains(Locator.xpath("//div[./div/span[text() = 'Choose Your Sample']]//dl[contains(@class,'x-list-selected')]//em"), SAMPLE_A);
        assertElementContains(Locator.xpath("//div[./div/span[text() = 'Choose Your Second Sample']]//dl[contains(@class,'x-list-selected')]//em"), SAMPLE_B);
        assertElementContains(Locator.xpath("//td[contains(@class, 'view-selected')]//div[contains(@class, 'x-btn-text')]"), "Tabular");
        expandAnalysisPanel("Choose How To Search Your Data");
        assertElementPresent(Locator.xpath("//*[@id='aminoAcid' and @checked=\"\"]"));
        assertElementPresent(Locator.xpath("//*[@id='vfamily' and @checked=\"\"]"));

        loadSavedView(VIEW_NAME_B); // Setup view B.  Compare samples [009x028]; Scatter; Switched search/filter settings.
        assertElementContains(Locator.xpath("//div[./div/span[text() = 'Choose Your Sample']]//dl[contains(@class,'x-list-selected')]//em"), SAMPLE_B);
        assertElementContains(Locator.xpath("//div[./div/span[text() = 'Choose Your Second Sample']]//dl[contains(@class,'x-list-selected')]//em"), SAMPLE_A);
        assertElementContains(Locator.xpath("//td[contains(@class, 'view-selected')]//div[contains(@class, 'x-btn-text')]"), "Log Scatter");
        expandAnalysisPanel("Choose How To Search Your Data");
        assertElementPresent(Locator.xpath("//*[@id='dna' and @checked=\"\"]"));
        assertFormElementEquals("searchDepth", "42");
        expandAnalysisPanel("Choose How To Filter Your Data");
        assertNotChecked(Locator.id("ssprod"));
        assertChecked(Locator.id("ssnprod"));
        assertChecked(Locator.id("ssscodon"));

        loadSavedView(VIEW_NAME_C, false); // Setup view C.  Single sample; Tabular; Switched search settings.
        assertElementContains(Locator.xpath("//div[./div/span[text() = 'Choose Your Sample']]//dl[contains(@class,'x-list-selected')]//em"), SAMPLE_B);
        assertElementContains(Locator.xpath("//td[contains(@class, 'view-selected')]//div[contains(@class, 'x-btn-text')]"), "Tabular");
        //expandAnalysisPanel("Choose How To Search Your Data");
        //assertElementPresent(Locator.xpath("//*[@id='dna' and @checked=\"\"]"));
        //assertFormElementEquals("searchDepth", "37");

        /*loadSavedView(VIEW_NAME_D); // Setup view D.  Single sample; 2D Histogram; [JGene x Unique]; Switched filter settings.
        assertElementContains(Locator.xpath("//div[./div/span[text() = 'Choose Your Sample']]//dl[contains(@class,'x-list-selected')]//em"), SAMPLE_A);
        assertElementContains(Locator.xpath("//td[contains(@class, 'view-selected')]//div[contains(@class, 'x-btn-text')]"), "2D Histogram");
        assertFormElementEquals(Locator.xpath("//div[./label[text()='X-Axis']]//div//input[@type = 'text']"), "JGene");
        assertFormElementEquals(Locator.xpath("//div[./label[text()='Count']]//div//input[@type = 'text']"), "Unique");
        expandAnalysisPanel("Choose How To Search Your Data");
        expandAnalysisPanel("Choose How To Filter Your Data");
        assertNotChecked(Locator.id("ssprod"));
        assertChecked(Locator.id("ssnprod"));
        assertChecked(Locator.id("ssscodon"));

        /*loadSavedView(VIEW_NAME_E); // Setup view E.  Single sample; 3D Histogram; Switched filter settings.
        assertElementContains(Locator.xpath("//div[./div/span[text() = 'Choose Your Sample']]//dl[contains(@class,'x-list-selected')]//em"), SAMPLE_B);
        assertElementContains(Locator.xpath("//td[contains(@class, 'view-selected')]//div[contains(@class, 'x-btn-text')]"), "3D Histogram");
        assertFormElementEquals(Locator.xpath("//div[./label[text()='Count']]//div//input[@type = 'text']"), "Average");
        expandAnalysisPanel("Choose How To Search Your Data");
        expandAnalysisPanel("Choose How To Filter Your Data");
        assertNotChecked(Locator.id("ssprod"));
        assertChecked(Locator.id("ssnprod"));
        assertChecked(Locator.id("ssscodon"));*/
    }

    private void editSavedViews()
    {
        refresh(); // TODO: Something happens between call to this method and last method which results in mem leak? Refresh helps (possibly sampleComparisonCount.jsp)?
        
        /* Edit View A */
        editSavedView(VIEW_NAME_A);  // Change view A to Compare Sample [009x028]. Switch to scatter / linear
        setAnalysisSample(SAMPLE_B);
        setAnalysisSample(SAMPLE_A, 1);
        click(Locator.id("compare-linear"));
        clickLinkWithText("View Data", false);
    }

    private void verifyDemoSite()
    {
        clickLinkWithText(DEMO_SITE);
        pushLocation();
        impersonate(USERA1);
        popLocation();

        /* Make sure navigation has been closed down to only necessary links */
        assertTextNotPresent("New Order");
        assertTextNotPresent("Upload Samples");
        assertTextNotPresent("Administration");

        /* TODO: Test Saved Views can be loaded / not saved or edited in demo site */
        
        pushLocation();
        stopImpersonating();
        popLocation();        
    }
   
    private void expandAnalysisPanel(String panelTitle)
    {
        if ( isElementPresent(Locator.xpath("//div[../div/span[text() = '" + panelTitle + "'] and contains(@style, 'display: none')]")) )
        {
            click(Locator.xpath("//div[../span[text() = '" + panelTitle + "']]")); // Expand panel.
        }
    }

    private void collapseAnalysisPanel(String panelTitle)
    {
        if ( isElementPresent(Locator.xpath("//div[../div/span[text() = '" + panelTitle + "'] and contains(@style, 'display: block')]")) )
        {
            click(Locator.xpath("//div[../span[text() = '" + panelTitle + "']]")); // Collapse panel.
        }
    }

    private void assertProductsTableCellEquals(int row, int column, String text)
    {
        assertElementPresent(Locator.xpath("//table [@class = 'adaptive-statistics-table']/tbody[" + row + "]/tr/td[position() = " + column + " and text() = '" + text + "']"));
    }

    private void assertStatisticsTableCellEquals(int tableNum, int row, int column, String expected)
    {
        // since the multi-sample stats are in separate tabs, we have to find the active tab's table...
        Locator table = Locator.xpath("(//table [@class = 'adaptive-statistics-table'])[" + tableNum + "]//tr[" + row + "]/td[" + column + "]");
        waitForElement(table, WAIT_FOR_JAVASCRIPT);
        String actual = getText(table);
        assertEquals("Did not find expected value '" + expected + "' in \"" + tableNum + "\"." + row + "." + column + ".", expected, actual);
    }

    private void setAnalysisPlotType(String id)
    {
        click(Locator.xpath("//*[@id='" + id + "']"));
    }

    private void setAnalysisSample(String value)
    {
        setAnalysisSample(value, 0);
    }

    private void setAnalysisSample(String value, int index)
    {
        click(Locator.xpath("//div[contains(@class, 'x-list-body-inner')]/dl/dt/em[contains(@unselectable, 'on') and text()='" + value + "']").index(index));
    }

    private void saveCurrentView(String viewName)
    {
        log("Saving view: " + viewName);
        checkCheckbox("named-view-cb");
        setFormElement("reportName", viewName);
        clickNavButton("Save", 0);
        waitForText("Saved.", WAIT_FOR_JAVASCRIPT);
    }

    private void loadSavedView(String viewName)
    {
        loadSavedView(viewName, true);
    }

    private void loadSavedView(String viewName, boolean wait)
    {
        log("Loading view: " + viewName);
        setViewSelection(viewName);
        clickLinkWithText("Load View", false);
        if (wait)
        {
            waitForElement(Locator.xpath("//div[contains(@style, 'visibility: visible')]//span[text() = 'Please Wait...']"), WAIT_FOR_JAVASCRIPT);
            waitForElementToDisappear(Locator.xpath("//div[contains(@style, 'visibility: visible')]//span[text() = 'Please Wait...']"), WAIT_FOR_JAVASCRIPT * 3);
            waitForElementToDisappear(Locator.xpath("//div[contains(@style, 'visibility: visible')]//span[text() = 'Loading Views...']"), WAIT_FOR_JAVASCRIPT);
        }
        
        assertTextNotPresent("Load Error");
    }

    private void viewData(boolean wait)
    {
        clickLinkWithText("View Data", false);
        if (wait)
        {
            waitForElement(Locator.xpath("//div[contains(@style, 'visibility: visible')]//span[text() = 'Please Wait...']"), WAIT_FOR_JAVASCRIPT);
            waitForElementToDisappear(Locator.xpath("//div[contains(@style, 'visibility: visible')]//span[text() = 'Please Wait...']"), WAIT_FOR_JAVASCRIPT * 3);
            waitForElementToDisappear(Locator.xpath("//div[contains(@style, 'visibility: visible')]//span[text() = 'Loading Views...']"), WAIT_FOR_JAVASCRIPT);
        }

        assertTextNotPresent("Load Error");
    }

    private void editSavedView(String viewName)
    {
        log("Editing view: " + viewName);
        setViewSelection(viewName);
        clickLinkWithText("Edit View", false);
    }

    private void setViewSelection(String viewName)
    {
        click(Locator.xpath("//span[text() = 'Saved Views']"));
        waitForText("Your Saved Views", WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath("//em[text() = '" + viewName + "']"));
    }
    
    private void createHeader()
    {
        goToModule("Wiki");
        createNewWikiPage("HTML");
        setFormElement("name", "_header");

        // Read header info from _header.html in adaptive resources directory.
        String header = "";
        File headerFile = new File(getAdaptiveRoot() + "/resources/adaptive", "_header.html");

        try
        {
            FileReader headerHtmlReader = new FileReader(headerFile);

            int next;
            do
            {
                next = headerHtmlReader.read();
                header += (char)next;
            }while ( next != -1 );
        }
        catch ( IOException e )
        {
            fail("_header.html not found in sample data dir: " + headerFile.getParent());
        }

        setWikiBody(header);
        saveWikiPage();
    }

    private String _adaptiveRoot;

    private String getAdaptiveRoot()
    {
        if (_adaptiveRoot == null)
        {
            String _adaptiveRootRelative = System.getProperty("adaptiveRoot");
            if (_adaptiveRootRelative == null || _adaptiveRootRelative.length() == 0)
            {
                _adaptiveRoot = WebTestHelper.canonicalizePath(getLabKeyRoot() + "/../adaptive");
                // Default is assuming that Adaptive is checked out adjacent to Labkey.
                log("Using default adaptive root (" + _adaptiveRoot +
                        ").\nThis cannot be changed'.");
            }
            else
            {
                _adaptiveRoot = WebTestHelper.canonicalizePath(getLabKeyRoot() + "/" + _adaptiveRootRelative);
                log("Using adaptive root '" + _adaptiveRoot + "', as provided by system property 'adaptiveRoot'.");
            }
        }
        return _adaptiveRoot;
    }

    //TODO: Move to base class.
    public void setCustomStylesheet(String cssPath, String cssFile)
    {
        ensureAdminMode();
        
        clickAdminMenuItem("Manage Project", "Project Settings");
        clickLinkWithText("Resources");

        File file = new File(cssPath, cssFile);
        setFormElement("customStylesheet", file.toString());
        clickNavButton("Save Resources");
    }

    public void verifyMailRecord(String messageTo, String messageSubject, String... messageContents)
    {
        verifyMailRecord(1, 1, messageTo, messageSubject, messageContents);
    }

    public void verifyMailRecord(int maxRow, String messageTo, String messageSubject, String... messageContents)
    {
        verifyMailRecord(1, maxRow, messageTo, messageSubject, messageContents);
    }

    public void verifyMailRecord(int startRow, int maxRow, String messageTo, String messageSubject, String... messageContents)
    {
        // Messages are sent asynchronously, so the order of messages sent simultaneously is unpredictable.
        // startRow & maxRow determine how far down the list to look for messages.  Zero-based index for visible rows.

        int messageRow = -1;
        // Scan table for a row that matches what we are looking for.
        for ( int i = startRow; i <= maxRow; i++)
        {
            if ( messageTo.equals(getTableCellText("dataregion_EmailRecord", i + 1, 0)) &&
                 messageSubject.equals(getText(Locator.xpath("//table[@id = 'dataregion_EmailRecord']//tr[" + (i + 2) + "]//a"))) )
            {
                messageRow = i;
                i = maxRow; // exit loop.
            }
        }

        if ( messageRow == -1 )
        {
            fail("Unable to locate message [" + messageSubject + "], to [" + messageTo + "], row[" + (startRow)+ ".." + maxRow + "]");
        }

        // expand message text.
        click(Locator.xpath("//table[@id = 'dataregion_EmailRecord']//tr[" + (messageRow + 2) + "]//a"));

        // check for required message text
        for ( String snippet : messageContents)
        {
            assertElementContains(Locator.xpath("//table[@id = 'dataregion_EmailRecord']//tr[" + (messageRow + 2) + "]//div"), snippet);
        }

        // expand message text. (for better screenshots)
        click(Locator.xpath("//table[@id = 'dataregion_EmailRecord']//tr[" + (messageRow + 2) + "]//a"));

    }

    public void clickAdaptiveMenuButton(String menu, String menuItem)
    {
        mouseOver(Locator.linkWithText(menu));
        waitForElement(Locator.linkWithText(menuItem), WAIT_FOR_JAVASCRIPT);
        clickLinkWithText(menuItem);
    }

    private void clickOrderStatus(String text, int wait)
    {
        clickAndWait(Locator.xpath("//td[text() = " + Locator.xq(text) + "]"), wait);
    }

    private void clickOrderStatus(String text)
    {
        clickAndWait(Locator.xpath("//td[text() = " + Locator.xq(text) + "]"));
    }

    private void goToAdaptiveAdmin()
    {
        waitForElement(Locator.linkWithText("Administration"), 10000);
        clickAdaptiveMenuButton("Administration", "Administration");
    }

    private void goToAdaptiveCustomerFolder(String customerFolderName)
    {
        clickLinkWithText("Home");
        clickLinkWithText(customerFolderName);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    @Override
    public boolean isFileUploadTest()
    {
        return true;
    }
}
