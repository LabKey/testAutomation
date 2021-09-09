/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

package org.labkey.test.tests.issues;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.Issues;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.components.dumbster.EmailRecordTable.EmailMessage;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.issues.ClosePage;
import org.labkey.test.pages.issues.DetailsPage;
import org.labkey.test.pages.issues.EmailPrefsPage;
import org.labkey.test.pages.issues.IssuesAdminPage;
import org.labkey.test.pages.issues.ListPage;
import org.labkey.test.pages.issues.ResolvePage;
import org.labkey.test.pages.issues.UpdatePage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.IssuesHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.PasswordUtil.getUsername;

@Category({Issues.class, Daily.class, Data.class})
@BaseWebDriverTest.ClassTimeout(minutes = 20)
public class IssuesTest extends BaseWebDriverTest
{
    private static final String ISSUE_TITLE_0 = "A very serious issue";
    private static final String ISSUE_TITLE_1 = "Even more serious issue";
    private static final String USER1 = "user1_issuetest@issues.test";
    private static final String USER2 = "user2_issuetest@issues.test";
    private static final String USER3 = "user3_issuetest@issues.test";
    private static final String user = "reader@issues.test";
    private static final Map<String, String> ISSUE_0 = new HashMap<>(Maps.of("title", ISSUE_TITLE_0, "priority", "2", "comment", "a bright flash of light"));
    private static final Map<String, String> ISSUE_1 = new HashMap<>(Maps.of("title", ISSUE_TITLE_1, "priority", "1", "comment", "alien autopsy"));
    private static final String ISSUE_SUMMARY_WEBPART_NAME = "Issues Summary";
    private static final String ISSUE_LIST_REGION_NAME = "issues-issues";
    private static final String TEST_GROUP = "testers";
    private static final String TEST_EMAIL_TEMPLATE =
            "You can review this issue here: ^detailsURL^\n" +
                    "Modified by: ^user^\n" +
                    "^modifiedFields^\n" +
                    "^string2|This line shouldn't appear: %s^\n" +
                    "^string3|This line shouldn't appear: %s^\n" +
                    "^string5|Customized template line: %s^\n" +
                    "^comment^";
    private static final String TEST_EMAIL_TEMPLATE_BAD = TEST_EMAIL_TEMPLATE +
            "\n'^asdf|The current date is: %1$tb %1$te, %1$tY^"; // Single quote for regression: 11389
    private static String NAME;
    private final IssuesHelper _issuesHelper = new IssuesHelper(this);
    private final ApiPermissionsHelper _permissionsHelper = new ApiPermissionsHelper(this);

    @BeforeClass
    public static void doSetup()
    {
        IssuesTest initTest = (IssuesTest) getCurrentTest();
        initTest.doInit();
    }

    private static String getLookupTableName(String issueDefName, String field)
    {
        return issueDefName + "-" + field.toLowerCase() + "-lookup";
    }

    @LogMethod
    public static void addLookupValues(BaseWebDriverTest test, String issueDefName, String fieldName, Collection<String> values)
    {
        if (!test.isElementPresent(Locator.tagWithText("h3", getLookupTableName(issueDefName, fieldName))))
        {
            test.goToSchemaBrowser();
            test.viewQueryData("lists", getLookupTableName(issueDefName, fieldName));
        }

        StringBuilder tsv = new StringBuilder();
        tsv.append("value");
        for (String value : values)
        {
            tsv.append("\n");
            tsv.append(value);
        }
        test._listHelper.uploadData(tsv.toString());
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("issues");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "IssuesVerifyProject";
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(false, USER1, USER2);
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    public void doInit()
    {
        NAME = getDisplayName();
        ISSUE_0.put("assignedTo", NAME);
        ISSUE_1.put("assignedTo", NAME);
        _containerHelper.createProject(getProjectName(), null);
        _permissionsHelper.createPermissionsGroup(TEST_GROUP);
        _permissionsHelper.assertPermissionSetting(TEST_GROUP, "No Permissions");
        _permissionsHelper.setPermissions(TEST_GROUP, "Editor");

        _issuesHelper.createNewIssuesList("issues", _containerHelper);
        waitAndClickAndWait(Locator.linkContainingText(ISSUE_SUMMARY_WEBPART_NAME));
        _issuesHelper.goToAdmin();
        _issuesHelper.setIssueAssignmentList(null);
        clickButton("Save");

        // Add to group so user appears
        _userHelper.createUser(USER1);
        _userHelper.createUser(USER2);
        _userHelper.createUser(USER3);
        _permissionsHelper.addUserToProjGroup(getUsername(), getProjectName(), TEST_GROUP);
        _permissionsHelper.addUserToProjGroup(USER1, getProjectName(), TEST_GROUP);
        _permissionsHelper.addUserToProjGroup(USER3, getProjectName(), TEST_GROUP);

        // Create issues
        clickProject(getProjectName());
        waitAndClickAndWait(Locator.linkContainingText(ISSUE_SUMMARY_WEBPART_NAME));
        _issuesHelper.addIssue(ISSUE_0);
        _issuesHelper.addIssue(ISSUE_1);
    }

    @Before
    public void returnToProject()
    {
        enableEmailRecorder();
        clickProject(getProjectName());
        waitAndClickAndWait(Locator.linkContainingText(ISSUE_SUMMARY_WEBPART_NAME));
        DataRegionTable issuesTable = new DataRegionTable(ISSUE_LIST_REGION_NAME, getDriver());

        // clear region selection and filters
        issuesTable.uncheckAll();
        issuesTable.clearAllFilters();

        // reset folder filter
        issuesTable.setContainerFilter(DataRegionTable.ContainerFilterType.CURRENT_FOLDER);
    }

    public void validateQueries()
    {
        // TODO: Fix broken query validation
    }

    @Test
    public void generalTest()
    {
        final String issueTitle = "A general issue";

        Locator newIssueButton = Locator.lkButton("New Issue");
        assertElementPresent(newIssueButton);

        // quick security test
        saveLocation();
        signOut();
        recallLocation();                          // try open issues as guest
        assertElementNotPresent(newIssueButton);
        signIn();
        recallLocation();                          // and logged in again
        assertElementPresent(newIssueButton);

        addLookupValues("issues", "area", Arrays.asList("Area51", "Fremont", "Downtown"));
        addLookupValues("issues", "type", Arrays.asList("UFO", "SPEC", "AAA"));
        addLookupValues("issues", "milestone", Arrays.asList("2012", "2013", "15.3", "15.3modules", "16.1", "16.1modules", "16.2", "16.2modules", "16.3", "16.3Modules",
                "17.1", "17.1modules", "17.2", "17.2modules", "17.3", "17.3modules", "18.1", "18.1modules", "18.2", "18.2modules", "18.3", "18.3modules",
                "19.1", "19.1modules", "19.2", "19.2modules", "19.3", "19.3modules", "20.1", "20.1modules", "20.2", "20.2modules", "20.3", "20.3modules",
                "TBD"));

        // create lookups for new custom fields
        createLookupList("issues", "MyFirstString", Arrays.asList("North", "South"));
        createLookupList("issues", "MyFifthString", Arrays.asList("Cadmium", "Polonium"));
        createLookupList("issues", "Client", Arrays.asList("Grey alien", "Green alien", "Aubergine alien"));
        createLookupList("issues", "UserStory", Arrays.asList("Alien landing investigation", "Alien diet study", "What is with all the circular symbols"));
        createLookupList("issues", "Triage", Arrays.asList("Approved", "Investigate", "Review Requested"));
        createLookupList("issues", "Note", Arrays.asList("Blocked", "Pending Doctor Action", "Take Me To Your Leader"));
        createLookupList("issues", "module", Arrays.asList("Unity", "Zvezda", "Kibo", "Columbus"));

        // SetCustomColumnConfigurationAction
        List<ListHelper.ListColumn> fields = new ArrayList<>();

        fields.add(new ListHelper.ListColumn("MyInteger", "MyInteger", ListHelper.ListColumnType.Integer, ""));
        fields.add(new ListHelper.ListColumn("MySecondInteger", "MySecondInteger", ListHelper.ListColumnType.Integer, ""));
        fields.add(new ListHelper.ListColumn("MyFirstString", "MyFirstString", ListHelper.ListColumnType.String, "", new ListHelper.LookupInfo(null, "lists", getLookupTableName("issues", "MyFirstString"))));
        fields.add(new ListHelper.ListColumn("MyThirdString", "MyThirdString", ListHelper.ListColumnType.String, ""));
        fields.add(new ListHelper.ListColumn("MyFourthString", "MyFourthString", ListHelper.ListColumnType.String, ""));
        fields.add(new ListHelper.ListColumn("MyFifthString", "MyFifthString", ListHelper.ListColumnType.String, "", new ListHelper.LookupInfo(null, "lists", getLookupTableName("issues", "MyFifthString"))));

        fields.add(new ListHelper.ListColumn("Client", "Client", ListHelper.ListColumnType.String, "", new ListHelper.LookupInfo(null, "lists", getLookupTableName("issues", "Client"))));
        fields.add(new ListHelper.ListColumn("UserStory", "User Story", ListHelper.ListColumnType.String, "", new ListHelper.LookupInfo(null, "lists", getLookupTableName("issues", "UserStory"))));
        fields.add(new ListHelper.ListColumn("SupportTicket", "Support Ticket", ListHelper.ListColumnType.Integer, ""));
        fields.add(new ListHelper.ListColumn("TeamCity", "TeamCity Note", ListHelper.ListColumnType.String, ""));
        fields.add(new ListHelper.ListColumn("Triage", "Triage", ListHelper.ListColumnType.String, "", new ListHelper.LookupInfo(null, "lists", getLookupTableName("issues", "Triage"))));
        fields.add(new ListHelper.ListColumn("Note", "Note", ListHelper.ListColumnType.String, "", new ListHelper.LookupInfo(null, "lists", getLookupTableName("issues", "Note"))));
        fields.add(new ListHelper.ListColumn("Module", "Module", ListHelper.ListColumnType.String, "", new ListHelper.LookupInfo(null, "lists", getLookupTableName("issues", "Module"))));

        clickProject(getProjectName());
        waitAndClickAndWait(Locator.linkContainingText(ISSUE_SUMMARY_WEBPART_NAME));
        IssuesAdminPage adminPage = _issuesHelper.goToAdmin();

        for (ListHelper.ListColumn col : fields)
        {
            adminPage.getFieldsPanel().addField(col);
        }
        clickButton("Save");

        clickProject(getProjectName());
        waitAndClickAndWait(Locator.linkContainingText(ISSUE_SUMMARY_WEBPART_NAME));

        // InsertAction
        clickButton("New Issue");
        setFormElement(Locator.name("title"), issueTitle);
        selectOptionByText(Locator.name("type"), "UFO");
        selectOptionByText(Locator.name("area"), "Area51");
        selectOptionByText(Locator.name("module"), "Zvezda");
        selectOptionByText(Locator.name("priority"), "2");
        setFormElement(Locator.name("comment"), ISSUE_0.get("comment"));
        selectOptionByText(Locator.name("assignedTo"), NAME);
        selectOptionByText(Locator.name("milestone"), "2012");

        Locator fouthStringLocator = Locator.name("myFourthString");
        if (!isElementPresent(fouthStringLocator))
            fouthStringLocator = Locator.name("myfourthstring");
        setFormElement(fouthStringLocator, "http://www.issues.test");

        Locator fifthStringLocator = Locator.name("myFifthString");
        if (!isElementPresent(fifthStringLocator))
            fifthStringLocator = Locator.name("myfifthstring");
        selectOptionByText(fifthStringLocator, "Polonium");
        clickButton("Save");

        // find issueId - parse the text from first space to :
        String issueId = getIssueId();

        // DetailsAction
        assertTextPresent("Issue " + issueId + ": " + issueTitle,
                "Milestone", "MyInteger", "MySecondInteger", "MyFirstString", "MyThirdString", "MyFourthString", "MyFifthString");
        assertTextNotPresent("MySecondString");
        assertElementPresent(Locator.linkWithText("http://www.issues.test"));

        // ListAction
        clickAndWait(Locator.linkWithText("Issues List"));

        // Click the issue id based on the text issue title
        clickAndWait(Locator.linkWithText("" + issueId));

        // UpdateAction
        updateIssue();
        setFormElement(Locator.name("comment"), "don't believe the hype");
        clickButton("Save");
        searchFor(getProjectName(), "hype", 1, issueTitle);

        // ResolveAction
        clickButton("Resolve");
        clickButton("Save");

        // ReopenAction
        clickButton("Reopen");
        clickButton("Save");

        // ResolveAction
        clickButton("Resolve");
        clickButton("Save");

        // CloseAction
        clickButton("Close");
        clickButton("Save");
        assertTextPresent("Issues List"); //we should be back at the issues list now

        // JumpToIssueAction
        setFormElement(Locator.name("issueId"), "" + issueId);
        clickAndWait(Locator.tagWithAttribute("a", "data-original-title", "Search"));
        assertTextPresent(issueTitle);
        assertTextNotPresent("Invalid");

        // SearchAction
        clickAndWait(Locator.linkWithText("Issues List"));
        pushLocation();
        String index = WebTestHelper.getContextPath() + "/search/" + getProjectName() + "/index.view?wait=1";
        log(index);
        beginAt(index, 5 * defaultWaitForPage);
        popLocation();
        // UNDONE: test grid search box

        // SearchWebPart
        searchFor(getProjectName(), "hype", 1, issueTitle);
        // SearchWebPart
        searchFor(getProjectName(), "2012", 1, issueTitle);

        // UNDONE test these actions
        // CompleteUserAction
        // ExportTsvAction
        // PurgeAction
        // RssAction
    }

    @LogMethod
    private void setRequiredFields(IssuesAdminPage adminPage, int[] positions, boolean selected)
    {
        for (int pos : positions)
        {
            adminPage.getFieldsPanel().getField(pos).setRequiredField(selected);
        }
    }

    private void addLookupValues(String issueDefName, String fieldName, Collection<String> values)
    {
        addLookupValues(this, issueDefName, fieldName, values);
    }

    private void createLookupList(String issueDefName, String fieldName, Collection<String> values)
    {
        _listHelper.createList(getProjectName(), getLookupTableName(issueDefName, fieldName), ListHelper.ListColumnType.String, "value");
        addLookupValues(issueDefName, fieldName, values);
    }

    @Test
    public void badUserNotifyList()
    {
        String badUsername = "junk";
        String errorMessage = String.format("Invalid user '%s'", badUsername);

        // NOTE: re using issue but in idempotent manner!
        clickAndWait(Locator.linkWithText(ISSUE_TITLE_0));

        updateIssue();
        setFormElement(Locator.name("notifyList"), badUsername);
        clickButton("Save");

        assertElementPresent(Locators.labkeyError.withText(errorMessage));

        clickButton("Cancel");
    }

    @Test
    public void testEmailTemplate()
    {
        // CustomizeEmailAction
        goToModule("Issues");
        clickButton("Customize Email Template");
        Assert.assertEquals("Wrong email template class", "org.labkey.issue.IssueUpdateEmailTemplate", getFormElement(Locator.name("templateClass")));
        setFormElement(Locator.name("emailMessage"), TEST_EMAIL_TEMPLATE_BAD);
        clickButton("Save");
    }

    @Test
    public void emailTest()
    {
        Map<String, String> ISSUE = Maps.of("assignedTo", _userHelper.getDisplayNameForEmail(USER1), "title", "A not so serious issue", "priority", "4", "comment", "No big whup", "notifyList", USER2);

        _issuesHelper.goToAdmin();
        _issuesHelper.setIssueAssignmentList(null);
        clickButton("Save");
        goToModule("Issues");

        // EmailPrefsAction
        EmailPrefsPage emailPrefsPage = new ListPage(getDriver()).clickEmailPreferences();
        emailPrefsPage.notifyOnMyChanges().check();
        emailPrefsPage.clickUpdate();

        impersonate(USER1);
        {
            emailPrefsPage = EmailPrefsPage.beginAt(this, getProjectName());
            emailPrefsPage.notifyOnAssignedIsModified().uncheck();
            emailPrefsPage.clickUpdate();
        }
        stopImpersonating();

        clickProject(getProjectName());
        _issuesHelper.addIssue(ISSUE);
        clickAndWait(Locator.linkWithText("Issues List"));

        // need to make change that will message current admin
        clickAndWait(Locator.linkWithText(ISSUE.get("title")));
        updateIssue();
        setFormElement(Locator.name("comment"), "Sup with this issue!");
        clickButton("Save");
        clickAndWait(Locator.linkWithText("Issues List"));

        //Issue 16238: From close issue screen: "Save" goes back to issue, "cancel" goes to issue list. This is the opposite of what I want
        log("verify cancelling returns to the same issue page");
        clickAndWait(Locator.linkWithText(ISSUE.get("title")));
        updateIssue();
        clickButton("Cancel");
        assertTitleContains(ISSUE.get("title"));

        //("Dumbster");
        goToModule("Dumbster");
        pushLocation();

        EmailRecordTable emailTable = new EmailRecordTable(this);
        EmailMessage message = emailTable.getMessageWithSubjectContaining(ISSUE.get("title") + ",\" has been opened and assigned to " + _userHelper.getDisplayNameForEmail(USER1));

        // Presumed to get the first message
        List<String> recipients = emailTable.getColumnDataAsText("To");
        assertTrue("User did not receive issue notification", recipients.contains(getUsername()));
        assertTrue(USER1 + " did not receive issue notification", recipients.contains(USER1));
        assertFalse(USER2 + " received issue notification without container read permission", recipients.contains(USER2));

        assertTrue("Issue Message does not contain title", message.getSubject().contains(ISSUE.get("title")));

        assertTextNotPresent("This line shouldn't appear");
    }

    private void updateIssue()
    {
        clickButton("Update");
    }

    @Test
    public void requiredFieldsTest()
    {
        final String subFolder = "Required Fields";
        final int[] requiredFieldPos = {0, 1, 2, 3, 4, 5, 6, 8, 9};
        final String[] requiredFieldLabels = {"title", "assignedto", "type", "area", "milestone",
                "notifylist", "customername", "contractnumber"};
        Set<String> expectedErrors = new HashSet<>();

        _containerHelper.createSubfolder(getProjectName(), subFolder);
        _issuesHelper.createNewIssuesList("required-fields", _containerHelper);

        List<ListHelper.ListColumn> fields = new ArrayList<>();

        fields.add(new ListHelper.ListColumn("ContractNumber", "Contract Number", ListHelper.ListColumnType.Integer, ""));
        fields.add(new ListHelper.ListColumn("CustomerName", "Customer Name", ListHelper.ListColumnType.String, ""));

        clickFolder(subFolder);
        waitAndClickAndWait(Locator.linkContainingText(ISSUE_SUMMARY_WEBPART_NAME));
        IssuesAdminPage adminPage = _issuesHelper.goToAdmin();

        for (ListHelper.ListColumn col : fields)
        {
            adminPage.getFieldsPanel().addField(col);
        }
        setRequiredFields(adminPage, requiredFieldPos, true);
        adminPage.clickSave();
        clickButton("New Issue");
        clickButton("Save");

        for (String label : requiredFieldLabels)
        {
            expectedErrors.add(String.format("Missing value for required property: %s", label));
        }

        Set<String> errors = getTexts(Locators.labkeyError.findElements(getDriver())).stream().map(String::toLowerCase).collect(Collectors.toSet());
        expectedErrors = expectedErrors.stream().map(String::toLowerCase).collect(Collectors.toSet());
        errors.remove("*"); // From "Fields marked with an asterisk * are required."
        Assert.assertEquals("Wrong errors", expectedErrors, errors);
        clickButton("Cancel");

        adminPage = _issuesHelper.goToAdmin();
        // clear all required selections except title
        setRequiredFields(adminPage, requiredFieldPos, false);
        setRequiredFields(adminPage, new int[]{0}, true);
        adminPage.clickSave();

        clickButton("New Issue");
        clickButton("Save");

        assertTextPresentCaseInsensitive("Missing value for required property: title");
        clickButton("Cancel");
    }

    @Test
    public void viewSelectedDetailsTest()
    {
        DataRegionTable issuesTable = new DataRegionTable(ISSUE_LIST_REGION_NAME, getDriver());

        issuesTable.setFilter("Status", "Has Any Value", null);
        issuesTable.checkAll();
        clickButton("View Details");
        assertTextPresent(
                ISSUE_0.get("comment"),
                ISSUE_1.get("comment"));
        clickAndWait(Locator.linkWithText("Issues List"));
    }

    @Test
    public void lastFilterTest()
    {
        DataRegionTable issuesTable = new DataRegionTable(ISSUE_LIST_REGION_NAME, getDriver());

        // assert both issues are present
        issuesTable.clearAllFilters("IssueId");
        assertTextPresent(ISSUE_TITLE_0, ISSUE_TITLE_1);

        // Filter out all pri-1 bugs; assert newly created issue is filtered out
        issuesTable.setFilter("Priority", "Does Not Equal", "1");
        assertTextPresent(ISSUE_TITLE_0);
        assertTextNotPresent(ISSUE_TITLE_1);

        // view an issue
        clickAndWait(Locator.linkWithText(ISSUE_TITLE_0));

        // assert .lastFilter is applied
        clickAndWait(Locator.linkWithText("Issues List"));
        assertTextPresent(ISSUE_TITLE_0);
        assertTextNotPresent(ISSUE_TITLE_1);

        issuesTable.clearAllFilters("IssueId");
    }

    @Test
    public void queryTest()
    {
        clickProject(getProjectName());
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addQueryWebPart("issues");

        clickAndWait(Locator.linkWithText("Issues Queries"));
        createNewQuery("issues");
        setFormElement(Locator.name("ff_newQueryName"), "xxyzzy");
        selectOptionByText(Locator.name("ff_baseTableName"), "issues");
        clickButton("Create and Edit Source");
        _ext4Helper.clickExt4Tab("Data");

        waitForText(WAIT_FOR_JAVASCRIPT, ISSUE_TITLE_0);
        waitForText(WAIT_FOR_JAVASCRIPT, ISSUE_TITLE_1);

        clickProject(getProjectName());

        // remove query which is broken now because requiredFieldsTest() renames MyFirstString
        deleteQuery(getProjectName(), "issues", "xxyzzy");
    }

    @LogMethod
    private void deleteQuery(String container, String schemaName, String queryName)
    {
        String deleteQueryURL = "query/" + container + "/deleteQuery.view?schemaName=" + schemaName + "&query.queryName=" + queryName;
        beginAt(deleteQueryURL);
        clickButton("OK");
    }

    @Test
    // Test issues grid with issues in a sub-folder
    public void subFolderIssuesTest()
    {
        final Map<String, String> issue0 = new HashMap<>(ISSUE_0);
        issue0.put("title", "This is for the subfolder test");
        final Map<String, String> issue1 = Maps.of("assignedTo", NAME, "title", "A sub-folder issue", "priority", "2", "comment", "We are in a sub-folder");
        final String subFolder = "SubFolder";

        // NOTE: be afraid -- very afraid. this data is used other places and could lead to false+ or false-
        _issuesHelper.addIssue(issue0);

        _containerHelper.createSubfolder(getProjectName(), subFolder);
        _issuesHelper.createNewIssuesList("issues", _containerHelper);

        _issuesHelper.addIssue(issue1);

        clickProject(getProjectName());
        waitAndClickAndWait(Locator.linkContainingText(ISSUE_SUMMARY_WEBPART_NAME));

        DataRegionTable issuesTable = new DataRegionTable(ISSUE_LIST_REGION_NAME, getDriver());
        issuesTable.setContainerFilter(DataRegionTable.ContainerFilterType.CURRENT_AND_SUBFOLDERS);

        // Verify the URL of issueTitles[0] goes to getProjectName()
        String href = getAttribute(Locator.linkContainingText(issue0.get("title")), "href");
        assertTrue("Expected issue details URL to link to project container",
                href.contains("/issues/" + getProjectName() + "/details.view") || href.contains("/" + getProjectName() + "/issues-details.view"));

        // Verify the URL of issueTitles[1] goes to getProjectName()/SUB_FOLDER_NAME
        href = getAttribute(Locator.linkContainingText(issue1.get("title")), "href");
        assertTrue("Expected issue details URL to link to sub-folder container",
                href.contains("/issues/" + getProjectName() + "/" + subFolder + "/details.view") || href.contains("/" + getProjectName() + "/" + subFolder + "/issues-details.view"));
    }

    @Test
    public void duplicatesTest()
    {
        _issuesHelper.addIssue(Maps.of("assignedTo", NAME, "title", "This Is some Issue -- let's say A"));
        String issueIdA = getIssueId();

        _issuesHelper.addIssue(Maps.of("assignedTo", NAME, "title", "This is another issue -- let's say B"));
        String issueIdB = getIssueId();

        clickButton("Resolve");
        selectOptionByText(Locator.name("resolution"), "Duplicate");
        setFormElement(Locator.name("duplicate"), issueIdA);
        clickButton("Save");

        assertElementPresent(Locator.linkWithText(issueIdA));
        assertTextPresent("resolve as Duplicate of " + issueIdA);

        clickAndWait(Locator.linkWithText(issueIdA));

        assertElementPresent(Locator.linkWithText(issueIdB));
        assertTextPresent(String.format("Issue %s marked as duplicate of this issue.", issueIdB), "Duplicates");
    }

    @Test
    public void moveIssueTest()
    {
        final String subFolder = "Move Folder";
        final String issueTitle = "This issue will be moved";
        final String displayName = NAME;
        final String path = String.format("/%s/%s", getProjectName(), subFolder);

        _containerHelper.createSubfolder(getProjectName(), subFolder);
        _issuesHelper.createNewIssuesList("issues", _containerHelper);

        goToProjectHome();
        goToModule("Issues");

        // create a new issue to be moved
        _issuesHelper.addIssue(Maps.of("assignedTo", displayName, "title", issueTitle));

        // move the created issue
        goToModule("Issues");
        clickAndWait(Locator.linkWithText(issueTitle));
        BootstrapMenu.find(getDriver(), "More")
                .clickSubMenu(false, "Move");

        // handle move dialog
        waitForElement(Locator.xpath("//input[@name='moveIssueCombo']"));
        _ext4Helper.selectComboBoxItem("Target folder:", path);
        clickAndWait(Ext4Helper.Locators.ext4Button("Move"));

        // validate new container
        assertEquals(path, getCurrentContainerPath());

        // perform multi-issue move
        returnToProject();
        goToModule("Issues");

        String issueTitleA = "Multi-Issue Move A";
        _issuesHelper.addIssue(Maps.of("assignedTo", displayName, "title", issueTitleA));
        String issueIdA = getIssueId();

        String issueTitleB = "Multi-Issue Move B";
        _issuesHelper.addIssue(Maps.of("assignedTo", displayName, "title", issueTitleB));
        String issueIdB = getIssueId();

        goToModule("Issues");

        DataRegionTable issuesTable = new DataRegionTable(ISSUE_LIST_REGION_NAME, getDriver());
        issuesTable.checkCheckboxByPrimaryKey(issueIdA);
        issuesTable.checkCheckboxByPrimaryKey(issueIdB);
        click(Locator.tagWithText("span", "Move").parent());
        // handle move dialog (copy pasta)
        waitForElement(Locator.xpath("//input[@name='moveIssueCombo']"));
        _ext4Helper.selectComboBoxItem("Target folder:", path);
        clickAndWait(Ext4Helper.Locators.ext4Button("Move"));

        // make sure the moved issues are no longer shwoing up
        assertTextNotPresent(issueTitleA, issueTitleB);
    }

    @Test
    public void relatedIssueTest()
    {
        Locator relatedLocator = Locator.name("related");

        String issueIdA = _issuesHelper.addIssue(Maps.of("assignedTo", NAME, "title", "A is for Apple", "priority", "0")).getIssueId();
        String issueIdB = _issuesHelper.addIssue(Maps.of("assignedTo", NAME, "title", "B is for Baking", "priority", "0")).getIssueId();
        // related C to A
        String issueIdC = _issuesHelper.addIssue(Maps.of("assignedTo", NAME, "title", "C is for Cat", "priority", "0", "related", issueIdA)).getIssueId();

        clickAndWait(Locator.linkWithText(issueIdA));
        assertElementPresent(Locator.linkWithText(issueIdC)); // Should link back to related issue

        // try to link to non-existent issue
        updateIssue();
        setFormElement(relatedLocator, "0");
        clickButton("Save");
        assertTextPresent("Invalid issue id in related string");

        // try to double link (reverse order to validate re-ordering)
        setFormElement(relatedLocator, String.format("%s,%s", issueIdC, issueIdB));
        clickButton("Save");

        assertElementPresent(Locator.linkWithText(issueIdC));
        assertElementPresent(Locator.linkWithText(issueIdB));
        assertTextPresent(String.format("%s,%s", issueIdC, issueIdB)); // Issue 39945: this is matching the comment 'Related' diff in change summary

        // show related comments
        final Locator.XPathLocator related = Locator.tagWithClass("div", "relatedIssue");
        assertElementNotVisible(related);
        BootstrapMenu.find(getDriver(), "More")
                .clickSubMenu(false, "Show related comments");
        assertElementVisible(related);

        // hide related comments
        BootstrapMenu.find(getDriver(), "More")
                .clickSubMenu(false, "Hide related comments");
        assertElementNotVisible(related);

        // NOTE: still need to test for case where user doesn't have permission to related issue...
    }

    @Test
    public void defaultAssignedToTest()
    {
        Locator.XPathLocator specificUserSelect = Locator.tagWithClass("select", "assigned-to-user");

        // create reader user (issue 20598)
        _userHelper.createUser(user);
        _permissionsHelper.createPermissionsGroup("Readers", user);
        _permissionsHelper.setPermissions("Readers", "Reader");

        String user1DisplayName = _userHelper.getDisplayNameForEmail(USER1);

        _issuesHelper.goToAdmin();
        _issuesHelper.setIssueAssignmentList(null);
        clickButton("Save");
        goToModule("Issues");

        // check for no default
        clickButton("New Issue");
        assertEquals(getSelectedOptionText(Locator.name("assignedTo")), "");
        clickButton("Cancel");

        /// check reader cannot be set as default user (issue 20598)
        _issuesHelper.goToAdmin();
        assertElementNotPresent(specificUserSelect.append(Locator.tagWithText("option", user)));

        // set default

        _issuesHelper.setIssueAssignmentUser(user1DisplayName);
        clickButton("Save");

        // verify
        clickButton("New Issue");
        assertEquals(getSelectedOptionText(Locator.name("assignedTo")), user1DisplayName);
        clickButton("Cancel");

        // set default group and user
        _issuesHelper.goToAdmin();
        _issuesHelper.setIssueAssignmentList("Site: Users");
        _issuesHelper.setIssueAssignmentUser(NAME);
        clickButton("Save");

        // verify
        clickButton("New Issue");
        assertEquals(getSelectedOptionText(Locator.name("assignedTo")), NAME);
        clickButton("Cancel");

        // set no default user and return to project users assign list
        _issuesHelper.goToAdmin();
        _issuesHelper.setIssueAssignmentList(null);
        _issuesHelper.setIssueAssignmentUser(null);
        clickButton("Save");

        // check for no default
        clickButton("New Issue");
        assertEquals(getSelectedOptionText(Locator.name("assignedTo")), "");
        clickButton("Cancel");

        // issue 20699 - NPE b/c default assign to user deleted!
        String deletedUser = "deleteme@issues.test";
        _userHelper.createUser(deletedUser);
        _permissionsHelper.addUserToProjGroup(deletedUser, getProjectName(), TEST_GROUP);
        clickProject(getProjectName());
        waitAndClickAndWait(Locator.linkContainingText(ISSUE_SUMMARY_WEBPART_NAME));
        _issuesHelper.goToAdmin();
        _issuesHelper.setIssueAssignmentUser(_userHelper.getDisplayNameForEmail(deletedUser));
        clickButton("Save");

        // taking care of some clean-up while here for the test.
        _userHelper.deleteUsers(true, deletedUser, user);

        //No NPE when the default user is selected.
        clickProject(getProjectName());
        waitAndClickAndWait(Locator.linkContainingText(ISSUE_SUMMARY_WEBPART_NAME));
        clickButton("New Issue");
        clickButton("Cancel");

        // TODO: compare user dropdown list between admin and new issues page
        goToProjectHome();
        waitAndClickAndWait(Locator.linkContainingText(ISSUE_SUMMARY_WEBPART_NAME));
        clickButton("New Issue");
        List<WebElement> assignedToUserOptionWebElement = new Select(Locator.name("assignedTo").findElement(getDriver())).getOptions();
        List<String> assignedToUserOptions = new ArrayList<>();
        for (WebElement e : assignedToUserOptionWebElement)
            if (!e.getText().isBlank())
                assignedToUserOptions.add(e.getText());

        goToProjectHome();
        waitAndClickAndWait(Locator.linkContainingText(ISSUE_SUMMARY_WEBPART_NAME));
        IssuesAdminPage issuesAdminPage = _issuesHelper.goToAdmin();
        List<String> defaultUserOptions = issuesAdminPage.getAllDefaultUserOptions();

        checker().verifyEquals("Assigned too and Default user assignment options dont match", defaultUserOptions, assignedToUserOptions);
    }

    @Test
    public void testAssignedToOnResolveAndClose() throws Exception
    {
        final String title = "assignmentTest";
        final String openTo = _userHelper.getDisplayNameForEmail(USER1);
        final String updateTo = _userHelper.getDisplayNameForEmail(USER3);
        final String resolveTo = NAME;
        final String closeTo = "Guest";

        DetailsPage detailsPage = _issuesHelper.addIssue(title, openTo);
        assertEquals("Wrong assignedTo after issue creation.", openTo, detailsPage.assignedTo().get());

        UpdatePage updatePage = detailsPage.clickUpdate();
        updatePage.assignedTo().set(updateTo);
        assertEquals("Wrong assignedTo after issue update.", updateTo, updatePage.assignedTo().get());
        detailsPage = updatePage.save();
        assertEquals("Wrong assignedTo after issue update.", updateTo, detailsPage.assignedTo().get());

        ResolvePage resolvePage = detailsPage.clickResolve();
        assertEquals("Wrong assignedTo after issue resolve.", resolveTo, resolvePage.assignedTo().get());
        detailsPage = resolvePage.save();
        assertEquals("Wrong assignedTo after issue resolve.", resolveTo, detailsPage.assignedTo().get());

        ClosePage closePage = detailsPage.clickClose();
        assertEquals("Wrong assignedTo after issue close.", closeTo, closePage.assignedTo().get());
        ListPage listPage = closePage.save();
        assertEquals("Wrong assignedTo after issue close.", closeTo, listPage.dataRegion().getDataAsText(0, "Assigned To"));
    }

    // NOTE: returning string here to avoid extra casting
    public String getIssueId()
    {
        URL url = getURL();
        Map<String, String> urlParameters = WebTestHelper.parseUrlQuery(url);
        return urlParameters.get("issueId");
    }
}
