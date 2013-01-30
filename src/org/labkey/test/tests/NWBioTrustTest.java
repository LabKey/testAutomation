package org.labkey.test.tests;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.ArrayList;
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
    private static final String designLabel = "ProspectiveTest";
    private static final String description = "Request specimens from surgeries or clinic procedures";
    private static final String[] submittedRequestLabels = {"first request", "second request", "third request"};
    private static final File TEST_FILE_1 = new File( getLabKeyRoot() + "/sampledata/survey/TestAttachment.txt");
    private static final File TEST_FILE_2 = new File( getLabKeyRoot() + "/sampledata/survey/TestAttachment2.txt");

    private static final String[] NWBT_REQUEST_CATEGORIES = {"_NWBT RC1", "_NWBT RC2", "_NWBT Repository"};
    private static final String[] NWBT_REQUEST_STATUSES = {"_NWBT Submission Review", "_NWBT Approved", "_NWBT Routed"};
    private static final String[] NWBT_DOCUMENT_TYPES = {"_NWBT Blank Unique Consent Form (by Study)", "_NWBT IRB Approval Packet", "_NWBT Specimen Processing Protocol"};

    private static final String NWBT_PRINCIPAL_INVESTIGATOR = "pi_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_STUDY_CONTACT = "sc_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_RESEARCH_COORD = "rc_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_FACULTY_CHAIR = "fc_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_FACULTY_REVIEWER = "fr_nwbiotrust@nwbiotrust.test";
    private static final String[] NWBT_USERS = {NWBT_PRINCIPAL_INVESTIGATOR, NWBT_STUDY_CONTACT, NWBT_RESEARCH_COORD,
                                                NWBT_FACULTY_CHAIR, NWBT_FACULTY_REVIEWER};

    private int fileCount = 0;

    private final PortalHelper portalHelper = new PortalHelper(this);

    @Override
    protected String getProjectName()
    {
        return "NWBioTrustTest";
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setupResearchCoordAndRequstorFolders();
        //setupUsersAndPermissions();
        verifyFolderTypes();
        setupProjectAdminProperties();
        setupSurveysTableDefinition();
        setupProvisionTableForResponses();
        populateSurveyDesignsAndRequests();
        verifyResearchCoordDashboard();
        verifyRequestorDashboard();
        verifySecondRequestorDashboard();
        populateDocumentSetForReqeusts();
        verifyDocumentSetFromDashboard();
        deleteSurveyDesign();
    }

    private void deleteSurveyDesign()
    {
        log("Delete the survey design for this project (which will delete the document set and requests");
        goToProjectHome();
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("RowId");
        _customizeViewsHelper.applyCustomView();
        DataRegionTable drt = new DataRegionTable("query", this);
        String rowId = drt.getDataAsText(drt.getRow("Label", designLabel), "RowId");
        checkDataRegionCheckbox("query", rowId);
        clickButton("Delete", 0);
        assertAlert("Are you sure you want to delete this survey design and its associated surveys?");
        waitForText("No sample requests to show", WAIT_FOR_PAGE);
        assertTextNotPresent(designLabel);
        clickFolder(requestorFolder1);
        waitForText("No sample requests to show");
        assertTextPresent("No sample requests to show", 2);
        assertTextNotPresent(submittedRequestLabels);
    }

    private void verifyDocumentSetFromDashboard()
    {
        log("Verify documents and types via RC Dashboard");
        goToProjectHome();
        waitForGridToLoad("div", "x4-grid-group-title", NWBT_REQUEST_CATEGORIES.length);
        assertElementPresent(Locator.linkWithText("Document Set (0)"), 2);
        assertElementPresent(Locator.linkWithText("Document Set (3)"), 1);
        click(Locator.linkWithText("Document Set (3)")); // link for the first request
        waitForText(TEST_FILE_1.getName());
        assertElementPresent(Locator.linkWithText(TEST_FILE_1.getName()), NWBT_DOCUMENT_TYPES.length);
        assertElementPresent(Locator.linkWithText(TEST_FILE_2.getName()), NWBT_DOCUMENT_TYPES.length - 1); // first doc type doesn't allow multiple file upload
        for (int index = 0; index < NWBT_DOCUMENT_TYPES.length; index++)
            assertTextPresent(NWBT_DOCUMENT_TYPES[index], index == 0 ? 1 : 2);

        log("Verify removing documents from document set");
        clickButton("Manage");
        waitForGridToLoad("tr", "x4-grid-row", fileCount);
        // verify that we navigated to the appropriate subfolder for the manage document set page
        assertTextPresent("NW BioTrust Specimen Requestor Dashboard");
        assertTextNotPresent("NW BioTrust Research Coordinator Dashboard");
        Locator loc = getEditLinkLocator(TEST_FILE_1.getName());
        click(loc);
        _extHelper.waitForExtDialog("Edit Document");
        assertTextPresentInThisOrder("File Name:", "Document Type:", "Created By:", "Created:");
        clickButton("Remove", 0);
        fileCount--;
        waitForTextToDisappear(NWBT_DOCUMENT_TYPES[0]);
        waitForGridToLoad("tr", "x4-grid-row", fileCount);
    }

    private void populateDocumentSetForReqeusts()
    {
        log("Add documents to a document set for requests");
        clickFolder(requestorFolder1);
        waitForGridToLoad("div", "x4-grid-group-title", NWBT_REQUEST_STATUSES.length);
        assertElementPresent(Locator.linkWithText("Document Set (0)"), 3);
        click(Locator.linkWithText("Document Set (0)")); // link for the first request
        waitForText("No documents to show");
        assertTextPresent(submittedRequestLabels[0], NWBT_REQUEST_STATUSES[0]);
        clickButton("Manage");
        waitForText("No documents to show");
        assertTextPresent(submittedRequestLabels[0], NWBT_REQUEST_STATUSES[0]);
        fileCount = 0;
        for (int index = 0; index < NWBT_DOCUMENT_TYPES.length; index++)
        {
            String documentType = NWBT_DOCUMENT_TYPES[index];
            clickButton("Add Document(s)", 0);
            _extHelper.waitForExtDialog("Add Document(s)");
            _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Document Type:']]"), documentType);
            waitForElement(Locator.name("attachmentfile0"));
            setFormElement(Locator.name("attachmentfile0"), TEST_FILE_1.toString());
            fileCount++;
            // the first doc type was set to not allow multiple file uploads
            if (index > 0)
            {
                assertElementPresent(Locator.linkContainingText("Attach a file"));
                click(Locator.linkContainingText("Attach a file"));
                waitForElement(Locator.name("attachmentfile1"));
                setFormElement(Locator.name("attachmentfile1"), TEST_FILE_2.toString());
                fileCount++;
            }
            else
                assertElementNotPresent(Locator.linkContainingText("Attach a file"));
            clickButton("Submit", 0);
            waitForGridToLoad("tr", "x4-grid-row", fileCount);
        }

        log("Verify file attachment links and document types exist");
        assertElementPresent(Locator.linkWithText(TEST_FILE_1.getName()), NWBT_DOCUMENT_TYPES.length);
        assertElementPresent(Locator.linkWithText(TEST_FILE_2.getName()), NWBT_DOCUMENT_TYPES.length - 1); // first doc type doesn't allow multiple file upload
        for (int index = 0; index < NWBT_DOCUMENT_TYPES.length; index++)
            assertTextPresent(NWBT_DOCUMENT_TYPES[index], index == 0 ? 1 : 2);

        log("Test document type allow multiple file setting");
        clickButton("Add Document(s)", 0);
        _extHelper.waitForExtDialog("Add Document(s)");
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Document Type:']]"), NWBT_DOCUMENT_TYPES[0]);
        waitForElement(Locator.name("attachmentfile0"));
        setFormElement(Locator.name("attachmentfile0"), TEST_FILE_2.toString());
        clickButton("Submit", 0);
        waitForText("This document type does not allow multiple files and one already exists in this document set.");
        clickButton("OK", 0);
        setFormElement(Locator.name("attachmentfile0"), TEST_FILE_1.toString());
        clickButton("Submit", 0);
        waitForText("A document with the following name already exists for this document type: " + TEST_FILE_1.getName());
        clickButton("OK", 0);
        clickButton("Close", 0);
    }

    private void verifySecondRequestorDashboard()
    {
        // TODO: this will be put to better use once we implement the NWBT security roles/permissions
        log("Verify that the 2nd requestor folder does not contain data from the first requestor");
        clickFolder(requestorFolder2);
        waitForText("No sample requests to show");
        assertTextPresent("No sample requests to show", 2);
        assertElementNotPresent(Locator.linkWithText("Click Here"));
    }

    private void verifyRequestorDashboard()
    {
        log("Verify updated reqeusts show up in Requestor Dashboard");
        clickFolder(requestorFolder1);
        waitForGridToLoad("div", "x4-grid-group-title", NWBT_REQUEST_STATUSES.length);
        assertElementNotPresent(getGroupingTitleLocator("Submitted"));
        for (String category : NWBT_REQUEST_STATUSES)
            assertElementPresent(getGroupingTitleLocator(category));
    }

    private void verifyResearchCoordDashboard()
    {
        log("Verify submitted requests show up in RC Dashboard");
        goToProjectHome();
        waitForGridToLoad("div", "x4-grid-group-title", 1); // all request should still be in the Unassigned category
        assertTextNotPresent("No sample requests to show");
        assertElementPresent(getGroupingTitleLocator("Unassigned"));
        assertTextPresentInThisOrder(submittedRequestLabels);

        log("Update request status and categories");
        for (int i = 0; i < submittedRequestLabels.length; i++)
            setRequestStatusAndCategory(i, submittedRequestLabels[i], NWBT_REQUEST_STATUSES[i], NWBT_REQUEST_CATEGORIES[i]);
        clickFolder(getProjectName());
        waitForGridToLoad("div", "x4-grid-group-title", NWBT_REQUEST_CATEGORIES.length);
        assertElementNotPresent(getGroupingTitleLocator("Unassigned"));
        for (String category : NWBT_REQUEST_CATEGORIES)
            assertElementPresent(getGroupingTitleLocator(category));
        assertTextPresentInThisOrder(NWBT_REQUEST_STATUSES);
    }

    private void waitForGridToLoad(String tag, String className, int expectedCount)
    {
        Locator l = Locator.xpath("//" + tag + "[contains(@class, '" + className + "')]");
        startTimer();
        while (getElementCount(l) < expectedCount && elapsedSeconds() < WAIT_FOR_JAVASCRIPT)
            sleep(1000);
        assertElementPresent(l, expectedCount);
    }

    private Locator getGroupingTitleLocator(String title)
    {
        return Locator.xpath("//div[contains(@class, 'x4-grid-group-title') and contains(text(), '" + title + "')]");
    }

    private void setRequestStatusAndCategory(int index, String label, String status, String category)
    {
        Locator loc = getEditLinkLocator(label);
        click(loc);
        _extHelper.waitForExtDialog("Edit Request");
        sleep(1000); // this is tricky because there is a loading mask for the combos, but they can load very quickly so that the test misses it if we wait for the mask to disappear
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='NWBT Resource:']]"), category);
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Status:']]"), status);
        clickButton("Update", 0);
        waitForElement(Locator.xpath("//div[contains(@class, 'x4-grid-group-title') and contains(text(), '" + category + "')]"));
        assertTextPresent(status);
    }

    private Locator getEditLinkLocator(String label)
    {
        return Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner')]//a[contains(text(),'" + label + "')]/../../..//td//div//span[contains(@class, 'edit-views-link')]");
    }

    private void populateSurveyDesignsAndRequests()
    {
        log("Create survey design in project folder and configure dashboard");
        assertTextNotPresent(designLabel);
        createSurveyDesign(getProjectName(), designLabel, description, "biotrust", provisionTableName);
        waitForText("No sample requests to show");
        customizeDashboard("RC Dashboard - Study Registrations", designLabel);

        log("Submit requests from the requestor subfolder");
        clickFolder(requestorFolder1);
        customizeDashboard("Requestor Dashboard - Study Registrations", designLabel);
        assertTextPresentInThisOrder(designLabel, description, "Draft Requests", "Submitted Requests");
        assertTextPresent("No sample requests to show", 2); // both draft and submitted should be empty
        for (String requestLabel : submittedRequestLabels)
        {
            clickAndWait(Locator.linkWithText("Click Here"));
            submitSampleRequest(requestLabel, Collections.singletonMap("testfield1", "Text Field Value"));
        }
        waitForGridToLoad("div", "x4-grid-group-title", 1); // all request should be in the Submitted group
        assertTextPresent("No sample requests to show", 1); // draft grid should still be empty
        assertTextPresentInThisOrder(submittedRequestLabels);
    }

    private void submitSampleRequest(String label, Map<String, String> fields)
    {
        waitForText("Survey Label*");
        setFormElement(Locator.name("_surveyLabel_"), label);
        for (Map.Entry<String, String> field : fields.entrySet())
        {
            setFormElement(Locator.name(field.getKey()), field.getValue());
        }
        clickButton("Submit completed form");
    }

    private void customizeDashboard(String webpartTitle, String formName)
    {
        portalHelper.clickWebpartMenuItem(webpartTitle, "Customize");
        waitForText(formName);
        _ext4Helper.checkCheckbox(formName);
        clickButton("Save");
    }

    private void setupProvisionTableForResponses()
    {
        log("Create provision table in biotrust schema for responses");
        goToProjectHome();
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
        assertElementPresent(Locator.linkWithText(provisionTableName));
    }

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

    private void setupProjectAdminProperties()
    {
        // NOTE: these tables are site wide (i.e. no container field), so we have to track which ones we add so we can delete them

        log("Populate the Request Category dashboard lookup table");
        goToProjectHome();
        checkForValuesToInsert("RequestCategory", "Category", NWBT_REQUEST_CATEGORIES);
        List<Map<String,Object>> rows = new ArrayList<Map<String, Object>>();
        for (int index = 0; index < NWBT_REQUEST_CATEGORIES.length; index++)
        {
            Map<String, Object> rowMap = new HashMap<String, Object>();
            rowMap.put("Category", NWBT_REQUEST_CATEGORIES[index]);
            rowMap.put("SortOrder", index / 10.0);
            rows.add(rowMap);
        }
        insertLookupTableRecords("RequestCategory", rows);

        log("Populate the Request Status dashboard lookup table");
        checkForValuesToInsert("RequestStatus", "Status", NWBT_REQUEST_STATUSES);
        rows = new ArrayList<Map<String, Object>>();
        for (int index = 0; index < NWBT_REQUEST_STATUSES.length; index++)
        {
            Map<String, Object> rowMap = new HashMap<String, Object>();
            rowMap.put("Status", NWBT_REQUEST_STATUSES[index]);
            rowMap.put("SortOrder", index / 10.0);
            rows.add(rowMap);
        }
        insertLookupTableRecords("RequestStatus", rows);

        log("Populate the Document Types lookup table");
        checkForValuesToInsert("DocumentTypes", "Name", NWBT_DOCUMENT_TYPES);
        rows = new ArrayList<Map<String, Object>>();
        for (int index = 0; index < NWBT_DOCUMENT_TYPES.length; index++)
        {
            Map<String, Object> rowMap = new HashMap<String, Object>();
            rowMap.put("Name", NWBT_DOCUMENT_TYPES[index]);
            // set the first doc type as not allowing multiple uploads
            rowMap.put("AllowMultipleUpload", index != 0);
            rows.add(rowMap);
        }
        insertLookupTableRecords("DocumentTypes", rows);
    }

    private void insertLookupTableRecords(String queryName, List<Map<String,Object>> rowsMap)
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

    private void checkForValuesToInsert(String queryName, String colName, String[] values)
    {
        log("Checking for values to be inserted via SelectRows API");
        Filter filter = new Filter(colName, StringUtils.join(values, ";"), Filter.Operator.IN);
        SelectRowsResponse response = executeSelectRowCommand("biotrust", queryName, ContainerFilter.Current, "/" + getProjectName(), Collections.singletonList(filter));
        if (response.getRows().size() > 0)
            Assert.fail("The " + colName + " to be inserted already exist in the biotrust." + queryName + " table.");
    }

    private void verifyFolderTypes()
    {
        log("Verify folder type default webparts");
        goToProjectHome();
        verifyWebpartTitleOrder(new String[]{"NW BioTrust Administration", "Survey Designs", "RC Dashboard - Study Registrations"});
        clickFolder(requestorFolder1);
        verifyWebpartTitleOrder(new String[]{"Requestor Dashboard - Study Registrations"});
        clickFolder(requestorFolder2);
        verifyWebpartTitleOrder(new String[]{"Requestor Dashboard - Study Registrations"});
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

    private void setupUsersAndPermissions()
    {
        log("Create all of the users for this test");
        for (String user : NWBT_USERS)
            createUser(user, null);

        log("Grant the appropriate permissions for each of these users at the project level");
        goToProjectHome();
        enterPermissionsUI();
        setUserPermissions(NWBT_RESEARCH_COORD, "NWBT Research Coordinator");
        setUserPermissions(NWBT_STUDY_CONTACT, "Reader");
        setUserPermissions(NWBT_PRINCIPAL_INVESTIGATOR, "Reader");
        setUserPermissions(NWBT_FACULTY_CHAIR, "Reader");
        setUserPermissions(NWBT_FACULTY_REVIEWER, "Reader");
        clickButton("Save and Finish");

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

        // TODO: more to be done here once we implement the various permissions/roles
        // TODO: we could use the webpart permissions to hide/show the RC dashboard, etc. from the project folder
    }

    private void setupResearchCoordAndRequstorFolders()
    {
        log("Create project folder and requestor subfolders");
        // use the project folder as the Research Coordinator folder, this will enable the survey and biotrust modules
        _containerHelper.createProject(getProjectName(), "NW BioTrust Research Coordinator");
        // create two requestor folders for this project
        _containerHelper.createSubfolder(getProjectName(), requestorFolder1, "NW BioTrust Specimen Requestor");
        _containerHelper.createSubfolder(getProjectName(), requestorFolder2, "NW BioTrust Specimen Requestor");
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

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        goToHome();
        if(isElementPresent(Locator.linkWithText(getProjectName())))
        {
            deleteDashboardLookupRows("RequestCategory", "Category", NWBT_REQUEST_CATEGORIES);
            deleteDashboardLookupRows("RequestStatus", "Status", NWBT_REQUEST_STATUSES);
            deleteDashboardLookupRows("DocumentTypes", "Name", NWBT_DOCUMENT_TYPES);
        }
        //deleteUsers(afterTest, NWBT_USERS);
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/biotrust";
    }
}
