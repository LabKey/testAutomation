package org.labkey.test.tests;

import junit.framework.Assert;
import org.apache.commons.lang3.StringUtils;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.tests.study.DataViewsTester;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;

import java.util.Collections;
import java.util.Iterator;
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
    private static final String[] submittedRequestLabels = {"first request", "second request", "third request"};

    private static final String[] NWBT_REQUEST_CATEGORIES = {"_NWBT RC1", "_NWBT RC2", "_NWBT Repository"};
    private static final String[] NWBT_REQUEST_STATUSES = {"_NWBT Submission Review", "_NWBT Approved", "_NWBT Routed"};
    private static final String[] NWBT_DOCUMENT_TYPES = {"_NWBT IRB Approval Packet", "_NWBT Specimen Processing Protocol", "_NWBT Blank Unique Consent Form (by Study)"};

    private static final String NWBT_PRINCIPAL_INVESTIGATOR = "pi_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_STUDY_CONTACT = "sc_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_RESEARCH_COORD = "rc_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_FACULTY_CHAIR = "fc_nwbiotrust@nwbiotrust.test";
    private static final String NWBT_FACULTY_REVIEWER = "fr_nwbiotrust@nwbiotrust.test";
    private static final String[] NWBT_USERS = {NWBT_PRINCIPAL_INVESTIGATOR, NWBT_STUDY_CONTACT, NWBT_RESEARCH_COORD,
                                                NWBT_FACULTY_CHAIR, NWBT_FACULTY_REVIEWER};

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
    }

    private void verifyRequestorDashboard()
    {
        log("Verify updated reqeusts show up in Requestor Dashboard");
        clickFolder(requestorFolder1);
        waitForRequestGridToLoad(NWBT_REQUEST_STATUSES.length);
        assertElementNotPresent(getGroupingTitleLocator("Submitted"));
        for (String category : NWBT_REQUEST_STATUSES)
            assertElementPresent(getGroupingTitleLocator(category));
    }

    private void verifyResearchCoordDashboard()
    {
        log("Verify submitted requests show up in RC Dashboard");
        goToProjectHome();
        waitForRequestGridToLoad(1); // all request should still be in the Unassigned category
        assertTextNotPresent("No sample requests to show");
        assertElementPresent(getGroupingTitleLocator("Unassigned"));
        assertTextPresentInThisOrder(submittedRequestLabels);

        log("Update request status and categories");
        for (int i = 0; i < submittedRequestLabels.length; i++)
            setRequestStatusAndCategory(i, submittedRequestLabels[i], NWBT_REQUEST_STATUSES[i], NWBT_REQUEST_CATEGORIES[i]);
        clickFolder(getProjectName());
        waitForRequestGridToLoad(NWBT_REQUEST_CATEGORIES.length);
        assertElementNotPresent(getGroupingTitleLocator("Unassigned"));
        for (String category : NWBT_REQUEST_CATEGORIES)
            assertElementPresent(getGroupingTitleLocator(category));
        assertTextPresentInThisOrder(NWBT_REQUEST_STATUSES);
    }

    private void waitForRequestGridToLoad(int expectedCategories)
    {
        Locator l = Locator.xpath("//div[contains(@class, 'x4-grid-group-title')]");
        waitForElement(l);
        assertElementPresent(l, expectedCategories);
    }

    private Locator getGroupingTitleLocator(String title)
    {
        return Locator.xpath("//div[contains(@class, 'x4-grid-group-title') and contains(text(), '" + title + "')]");
    }

    private void setRequestStatusAndCategory(int index, String label, String status, String category)
    {
        Locator loc = DataViewsTester.getEditLinkLocator(label);
        click(loc);
        _extHelper.waitForExtDialog("Edit Request");
        sleep(1000); // this is tricky because there is a loading mask for the combos, but they can load very quickly so that the test misses it if we wait for the mask to disappear
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='NWBT Resource:']]"), category);
        _ext4Helper.selectComboBoxItem(Locator.xpath("//tbody[./tr/td/label[text()='Status:']]"), status);
        clickButton("Update", 0);
        waitForText(category);
        assertTextPresent(status);
    }

    private void populateSurveyDesignsAndRequests()
    {
        String designLabel = "Prospective";
        String description = "Request specimens from surgeries or clinic procedures";

        log("Create survey design in project folder and configure dashboard");
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
            submitSampleRequest(requestLabel, Collections.singletonMap("testField1", "Text Field Value"));
        }
        waitForRequestGridToLoad(1); // all request should be in the Submitted group
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
        _listHelper.addField(new ListHelper.ListColumn("testField1", "Test Field 1", ListHelper.ListColumnType.String, null));
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
        _listHelper.addField(new ListHelper.ListColumn("Category", "Category", null, null, new ListHelper.LookupInfo("", "biotrust", "RequestCategory")));
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
        goToDashboardLookupTable("Request Category", NWBT_REQUEST_CATEGORIES);
        clickButton("Import Data");
        String importData = "Category\tSortOrder\n";
        for (int index = 0; index < NWBT_REQUEST_CATEGORIES.length; index++)
            importData += NWBT_REQUEST_CATEGORIES[index] + "\t0." + index + "\n";
        _listHelper.submitTsvData(importData);

        log("Populate the Request Status dashboard lookup table");
        goToDashboardLookupTable("Request Status", NWBT_REQUEST_STATUSES);
        clickButton("Import Data");
        importData = "Status\tSortOrder\n";
        for (int index = 0; index < NWBT_REQUEST_STATUSES.length; index++)
            importData += NWBT_REQUEST_STATUSES[index] + "\t0." + index + "\n";
        _listHelper.submitTsvData(importData);

        log("Populate the Document Types lookup table");
        goToDashboardLookupTable("Document Types", NWBT_DOCUMENT_TYPES);
        clickButton("Import Data");
        importData = "Name\tAllowMultipleUpload\n";
        // set the first doc type as not allowing multiple uploads
        for (int index = 0; index < NWBT_DOCUMENT_TYPES.length; index++)
            importData += NWBT_DOCUMENT_TYPES[index] + "\t" + (index == 0 ? "false" : "true") + "\n";
        _listHelper.submitTsvData(importData);
    }

    private void goToDashboardLookupTable(String tableName, String[] valuesToBeAdded)
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(tableName));
        // verify that the values we are about to add don't already exist
        assertTextNotPresent(valuesToBeAdded);
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

    private void deleteDashboardLookupRows(String tableName, String keyColName, String[] valuesToBeDeleted)
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(tableName));
        if (isTextPresent(valuesToBeDeleted))
        {
            DataRegionTable table = new DataRegionTable("query", this);
            table.setFilter(keyColName, "Equals One Of (e.g. \"a;b;c\")", StringUtils.join(valuesToBeDeleted, ";"));
            checkAllOnPage("query");
            waitForText("Selected all " + valuesToBeDeleted.length + " rows.");
            clickButton("Delete", 0);
            assertAlert("Are you sure you want to delete the selected rows?");
            waitForText("No data to show.");
        }
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        goToHome();
        if(isElementPresent(Locator.linkWithText(getProjectName())))
        {
            deleteDashboardLookupRows("Request Category", "Category", NWBT_REQUEST_CATEGORIES);
            deleteDashboardLookupRows("Request Status", "Status", NWBT_REQUEST_STATUSES);
            deleteDashboardLookupRows("Document Types", "Name", NWBT_DOCUMENT_TYPES);
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
