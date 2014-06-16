/*
 * Copyright (c) 2008-2014 LabKey Corporation
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Data;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EmailRecordTable;
import org.labkey.test.util.EmailRecordTable.EmailMessage;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.IssuesHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;

import java.util.List;

import static org.junit.Assert.*;

@Category({DailyA.class, Data.class})
public class IssuesTest extends BaseWebDriverTest
{
    protected static final String PROJECT_NAME = "IssuesVerifyProject";
    protected static final String SUB_FOLDER_NAME = "SubFolder";
    private static final String ISSUE_TITLE_0 = "A very serious issue";
    private static final String ISSUE_TITLE_1 = "Even more serious issue";
    private static final String ISSUE_TITLE_2 = "A not so serious issue";
    private static final String USER1 = "user1_issuetest@issues.test";
    private static final String USER2 = "user2_issuetest@issues.test";
    private static final String USER3 = "user3_issuetest@issues.test";

    private static final String[] REQUIRED_FIELDS = {"Title", "AssignedTo", "Type", "Area", "Priority", "Milestone",
            "NotifyList", "String1", "Int1"};
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
            "\n\'^asdf|The current date is: %1$tb %1$te, %1$tY^"; // Single quote for regression: 11389

    private IssuesHelper _issuesHelper = new IssuesHelper(this);

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/issues";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        IssuesTest initTest = new IssuesTest();
        initTest.doCleanup(false);
        initTest.setupProject();
        currentTest = initTest;
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteUsers(afterTest, USER1, USER2);
        deleteProject(PROJECT_NAME, afterTest);
    }

    @Before
    public void returnToProject()
    {
        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText("Issues Summary"));
        readyState();
    }

    public void readyState()
    {
        DataRegionTable issuesTable = new DataRegionTable("Issues", this);

        // clear region selection and filters
        issuesTable.uncheckAll();
        issuesTable.clearAllFilters("IssueId");

        // reset folder filter
        _extHelper.clickMenuButton(true, "Views", "Folder Filter", "Current folder");
    }

    public void setupProject()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(PROJECT_NAME, null);
        _permissionsHelper.createPermissionsGroup(TEST_GROUP);
        _permissionsHelper.assertPermissionSetting(TEST_GROUP, "No Permissions");
        _permissionsHelper.setPermissions(TEST_GROUP, "Editor");
        clickButton("Save and Finish");

        enableModule(PROJECT_NAME, "Dumbster");

        clickProject(PROJECT_NAME);

        portalHelper.addWebPart("Issues Summary");
        portalHelper.addWebPart("Search");
        assertTextPresent("Open");

        _containerHelper.createSubfolder(PROJECT_NAME, SUB_FOLDER_NAME, null);
        portalHelper.addWebPart("Issues List");

        enableEmailRecorder();
        checkEmptyToAssignedList();
        addProjectUsersToGroup();
        createIssues();
    }

    private void checkEmptyToAssignedList()
    {
        // InsertAction -- user isn't in any groups, so shouldn't appear in the assigned-to list yet

        clickButton("New Issue");
        String assignedToText = getText(Locator.name("assignedTo"));
        assertEquals(assignedToText, "");
    }

    private void addProjectUsersToGroup()
    {
        // Add to group so user appears
        clickProject("IssuesVerifyProject");
        _permissionsHelper.addUserToProjGroup(PasswordUtil.getUsername(), PROJECT_NAME, TEST_GROUP);
        _permissionsHelper.addUserToProjGroup(USER1, PROJECT_NAME, TEST_GROUP);
        createUser(USER2, null, false);
    }

    private void createIssues()
    {
        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText("Issues Summary"));
        _issuesHelper.addIssue(Maps.of("assignedTo", getDisplayName(), "title", ISSUE_TITLE_0, "priority", "2", "comment", "a bright flash of light"));
        _issuesHelper.addIssue(Maps.of("assignedTo", getDisplayName(), "title", ISSUE_TITLE_1, "priority", "1", "comment", "alien autopsy"));
        _issuesHelper.addIssue(Maps.of("assignedTo", displayNameFromEmail(USER1), "title", ISSUE_TITLE_2, "priority", "4", "comment", "No big whup", "notifyList", USER2));
    }

    public void validateQueries()
    {
        // TODO: Fix broken query validation
    }

    @Test
    public void generalTest()
    {
        final String issueTitle = "A general issue";

        assertButtonPresent("New Issue");

        // quick security test
        // TODO push lots of locations as we go and move this test to end
        pushLocation();
        pushLocation();
        signOut();
        popLocation();                          // try open issues as guest
        assertButtonNotPresent("New Issue");
        assertElementPresent(Locator.tagWithName("form", "login"));
        signIn();
        popLocation();                          // and logged in again
        assertButtonPresent("New Issue");

        // AdminAction
        clickButton("Admin");

        // AddKeywordAction
        addKeywordsAndVerify("area", "Area", "Area51", "Fremont", "Downtown");
        addKeywordsAndVerify("type", "Type", "UFO", "SPEC", "TODO", "AAA");

        //SetKeywordDefaultAction
        clickAndWait(Locator.linkWithText("set"));
        // check that AAA is bold and [clear] link is on that row
        assertElementContains(Locator.xpath("id('formtype')/table/tbody/tr[1]/td[1]/b"), "AAA");
        assertElementContains(Locator.xpath("id('formtype')/table/tbody/tr[1]/td[2]/a[2]"), "CLEAR");
        //SetKeywordDefaultAction
        clickAndWait(Locator.linkWithText("clear"));
        // check that AAA is not bold and [set] link is now on that row
        assertElementNotPresent(Locator.xpath("id('formtype')/table/tbody/tr[1]/td[1]/b"));
        assertElementContains(Locator.xpath("id('formtype')/table/tbody/tr[1]/td[2]/a[2]"), "SET");
        clickAndWait(Locator.linkWithText("delete"));
        assertTextNotPresent("AAA");

        // Check that non-integer priority results in an error message
        addKeyword("priority", "Priority", "ABC");
        assertElementPresent(Locator.css(".labkey-error").withText("Priority must be an integer"));
        assertElementNotPresent(Locator.css("#formPriority td").withText("ABC"));
        addKeyword("priority", "Priority", "1.2");
        assertElementPresent(Locator.css(".labkey-error").withText("Priority must be an integer"));
        assertElementNotPresent(Locator.css("#formPriority td").withText("1.2"));

        // SetCustomColumnConfigurationAction
        setFormElement(Locator.name("int1"), "MyInteger");
        setFormElement(Locator.name("int2"), "MySecondInteger");
        setFormElement(Locator.name("string1"), "MyFirstString");
        // Omit string2 to test using it in email template.
        setFormElement(Locator.name("string3"), "MyThirdString");
        setFormElement(Locator.name("string4"), "MyFourthString");
        setFormElement(Locator.name("string5"), "MyFifthString");
        checkCheckbox(Locator.checkboxByNameAndValue("pickListColumns", "string1"));
        checkCheckbox(Locator.checkboxByNameAndValue("pickListColumns", "string5"));
        clickButton("Update");

        // AddKeywordAction
        addKeywordsAndVerify("milestone", "Milestone", "2012", "2013");
        addKeywordsAndVerify("string1", "MyFirstString", "North", "South");
        addKeywordsAndVerify("string5", "MyFifthString", "Cadmium", "Polonium");

        // UpdateRequiredFieldsAction
        checkCheckbox(Locator.checkboxByNameAndValue("requiredFields", "Milestone"));
        checkCheckbox(Locator.checkboxByNameAndValue("requiredFields", "String4"));
        checkCheckbox(Locator.checkboxByNameAndValue("requiredFields", "String5"));
        clickButton("Update");

        // ListAction (empty)
        clickButton("Back to Issues");

        // InsertAction
        clickButton("New Issue");
        String customStringText = getText(Locator.name("string5"));
        assertEquals(customStringText, "Cadmium\nPolonium");
        setFormElement(Locator.name("title"), issueTitle);
        selectOptionByText(Locator.name("type"), "UFO");
        selectOptionByText(Locator.name("area"), "Area51");
        selectOptionByText(Locator.name("priority"), "2");
        setFormElement(Locator.name("comment"), "a bright flash of light");
        clickButton("Save");

        // test validate
        assertTextPresent("Field AssignedTo cannot be blank");
        selectOptionByText(Locator.name("assignedTo"), getDisplayName());
        clickButton("Save");
        assertTextPresent("Field Milestone cannot be blank");
        selectOptionByText(Locator.name("milestone"), "2012");
        clickButton("Save");
        assertTextPresent("Field MyFourthString cannot be blank");
        setFormElement(Locator.name("string4"), "http://www.issues.test");
        clickButton("Save");
        assertTextPresent("Field MyFifthString cannot be blank");
        selectOptionByText(Locator.name("string5"), "Polonium");
        clickButton("Save");

        // find issueId - parse the text from first space to :
        String issueId = getLastPageIssueId();

        // DetailsAction
        assertTextPresent("Issue " + issueId + ": " + issueTitle);
        assertTextPresent("Milestone", "MyInteger", "MySecondInteger", "MyFirstString", "MyThirdString", "MyFourthString", "MyFifthString");
        assertTextNotPresent("MySecondString");
        assertElementPresent(Locator.linkWithText("http://www.issues.test"));

        // ListAction
        clickAndWait(Locator.linkWithText("return to grid"));

        // Click the issue id based on the text issue title
        clickAndWait(Locator.linkWithText("" + issueId));

        // UpdateAction
        updateIssue();
        setFormElement(Locator.name("comment"), "don't believe the hype");
        clickButton("Save");
        searchFor(PROJECT_NAME, "2012", 1, issueTitle);

        // ResolveAction
        clickAndWait(Locator.linkWithText("resolve"));
        clickButton("Save");

        // ReopenAction
        clickAndWait(Locator.linkWithText("reopen"));
        clickButton("Save");

        // ResolveAction
        clickAndWait(Locator.linkWithText("resolve"));
        clickButton("Save");

        // CloseAction
        clickAndWait(Locator.linkWithText("close"));
        clickButton("Save");
        assertTextPresent("Issues List"); //we should be back at the issues list now

        // JumpToIssueAction
        setFormElement(Locator.name("issueId"), "" + issueId);
        clickButton("Jump to Issue");
        assertTextPresent(issueTitle);
        assertTextNotPresent("Invalid");

        // SearchAction
        clickAndWait(Locator.linkWithText("return to grid"));
        pushLocation();
        String index = getContextPath() + "/search/" + PROJECT_NAME + "/index.view?wait=1";
        log(index);
        beginAt(index, 5 * defaultWaitForPage);
        popLocation();
        // UNDONE: test grid search box

        // SearchWebPart
        searchFor(PROJECT_NAME, "hype", 1, issueTitle);
        // SearchWebPart
        searchFor(PROJECT_NAME, "2012", 1, issueTitle);

        // clean up required fields
        clickAndWait(Locator.linkWithText("return to grid"));
        clickButton("Admin");
        uncheckCheckbox(Locator.checkboxByNameAndValue("requiredFields", "Milestone"));
        uncheckCheckbox(Locator.checkboxByNameAndValue("requiredFields", "String4"));
        uncheckCheckbox(Locator.checkboxByNameAndValue("requiredFields", "String5"));
        clickButton("Update");

        // UNDONE test these actions
        // CompleteUserAction
        // ExportTsvAction
        // PurgeAction
        // RssAction
    }

    private void addKeyword(String fieldName, String caption, String value)
    {
        addKeyword(this, fieldName, caption, value);
    }

    private void addKeywordsAndVerify(String fieldName, String caption, String... values)
    {
        addKeywordsAndVerify(this, fieldName, caption, values);
    }

    // Add a keyword to the given field, without verifying the operation.  Need to be on the issues admin page already.
    @LogMethod(quiet = true)
    private static void addKeyword(BaseWebDriverTest test, @LoggedParam String fieldName, String caption, String value)
    {
        test.setFormElement(Locator.xpath("//form[@name='add" + fieldName + "']/input[@name='keyword']"), value);
        test.clickButton("Add " + caption);
    }

    // Add new keyword(s) to the given field and verify they were added without error.  Need to be on the issues admin page already.
    @LogMethod
    public static void addKeywordsAndVerify(BaseWebDriverTest test, @LoggedParam String fieldName, String caption, String... values)
    {
        for (String value : values)
        {
            addKeyword(test, fieldName, caption, value);
            test.assertNoLabkeyErrors();
            test.assertTextPresent(value);
        }
    }

    @Test
    public void emailTest()
    {
        goToModule("Dumbster");
        assertTextPresent("No email recorded.");

        // CustomizeEmailAction 
        goToModule("Issues");
        clickButton("Admin");
        clickButton("Customize Email Template");
        String subject = getFormElement(Locator.name("emailSubject"));
        setFormElement(Locator.name("emailMessage"), TEST_EMAIL_TEMPLATE_BAD);
        clickButton("Save");
        assertTextPresent("Invalid template");
        setFormElement(Locator.name("emailMessage"), TEST_EMAIL_TEMPLATE);
        clickButton("Save");
        assertTextNotPresent("Invalid template");
        assertFormElementEquals("emailSubject", subject); // regression check for issue #11389
        goToModule("Portal");

        // EmailPrefsAction
        clickAndWait(Locator.linkWithText("Issues Summary"));
        clickButton("Email Preferences");
        checkCheckbox(Locator.checkboxByNameAndValue("emailPreference", "8")); // self enter/edit an issue
        clickButton("Update");

        impersonate(USER1);
        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText("Issues Summary"));
        clickButton("Email Preferences");
        uncheckCheckbox(Locator.checkboxByNameAndValue("emailPreference", "2")); // issue assigned to me is modified
        clickButton("Update");
        stopImpersonating();

        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText("Issues Summary"));

        // need to make change that will message current admin
        clickAndWait(Locator.linkWithText(ISSUE_TITLE_2));
        updateIssue();
        setFormElement(Locator.name("comment"), "Sup with this issue!");
        clickButton("Save");
        clickAndWait(Locator.linkWithText("return to grid"));

        //Issue 16238: From close issue screen: "Save" goes back to issue, "cancel" goes to issue list. This is the opposite of what I want
        log("verify cancelling returns to the same issue page");
        clickAndWait(Locator.linkWithText(ISSUE_TITLE_2));
        updateIssue();
        clickButton("Cancel");
        assertTextPresent(ISSUE_TITLE_2);

        goToModule("Dumbster");
        pushLocation();

        EmailRecordTable emailTable = new EmailRecordTable(this);
        EmailMessage message = emailTable.getMessage(ISSUE_TITLE_2 + ",\" has been opened and assigned to " + displayNameFromEmail(USER1));

        // Presumed to get the first message
        List<String> recipients = emailTable.getColumnDataAsText("To");
        assertTrue("User did not receive issue notification", recipients.contains(PasswordUtil.getUsername()));
        assertTrue(USER2 + " did not receieve issue notification", recipients.contains(USER2));
        assertTrue(USER1 + " did not receieve issue notification", recipients.contains(USER1));

        assertTrue("Issue Message does not contain title", message.getSubject().contains(ISSUE_TITLE_2));

        assertTextNotPresent("This line shouldn't appear");

        impersonate(USER1);
        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText("Issues Summary"));
        clickAndWait(Locator.linkWithText(ISSUE_TITLE_2));
        updateIssue();
        selectOptionByText(Locator.name("priority"), "0");
        setFormElement(Locator.name("notifyList"), USER3);
        setFormElement(Locator.name("comment"), "Oh Noez!");
        clickButton("Save");
        stopImpersonating();

        popLocation();

        emailTable = new EmailRecordTable(this);
        message = emailTable.getMessage(ISSUE_TITLE_2 + ",\" has been updated");

        // issue 17637 : inactive users as well as users not in the system should not receive emails
        //assertTrue(USER3 + " did not receieve updated issue notification" + message.getTo()[0],
        //        USER3.equals(emailTable.getDataAsText(0, "To")) || USER3.equals(emailTable.getDataAsText(1, "To")));
        assertTrue("User did not receive updated issue notification",
                PasswordUtil.getUsername().equals(emailTable.getDataAsText(0, "To")) || PasswordUtil.getUsername().equals(emailTable.getDataAsText(1, "To")));
    }

    private void updateIssue()
    {
        clickAndWait(Locator.linkWithText("update"));
    }

    @Test
    public void entryTypeNameTest()
    {
        goToModule("Issues");
        clickButton("Admin");
        setFormElement(Locator.name("entrySingularName"), "Ticket");
        setFormElement(Locator.name("entryPluralName"), "Tickets");
        clickButton("Update");

        assertFormElementEquals("entrySingularName", "Ticket");
        assertFormElementEquals("entryPluralName", "Tickets");

        assertTextPresent("Tickets Admin Page");
        clickAndWait(Locator.linkWithText("Back to Tickets"));

        assertTextPresent("Tickets List");
        assertTextNotPresent("Issues List");
        assertButtonPresent("New Ticket");
        assertButtonPresent("Jump to Ticket");
        assertTextPresent("Ticket ID");
        assertTextNotPresent("Issue ID");

        clickButton("Admin");
        setFormElement(Locator.name("entrySingularName"), "Issue");
        setFormElement(Locator.name("entryPluralName"), "Issues");
        clickButton("Update");
    }

    @Test
    public void requiredFieldsTest()
    {
        goToModule("Issues");
        clickButton("Admin");
        setFormElement(Locator.name("int1"), "Contract Number");
        setFormElement(Locator.name("string1"), "Customer Name");

        updateIssue();

        for (String field : REQUIRED_FIELDS)
            checkRequiredField(field, true);

        clickButton("Update");
        clickButton("Back to Issues");
        clickButton("Admin");

        for (String field : REQUIRED_FIELDS)
        {
            verifyFieldChecked(field);
            checkRequiredField(field, false);
        }

        checkRequiredField("Title", true);
        clickButton("Update");
        clickButton("Back to Issues");
        clickButton("New Issue");
        clickButton("Save");

        assertTextPresent("Field Title cannot be blank.");
        clickButton("Cancel");

        clickButton("Admin");
        checkCheckbox(Locator.checkboxByNameAndValue("requiredFields", "AssignedTo"));
        clickButton("Update");
    }

    @LogMethod
    private void checkRequiredField(@LoggedParam String name, boolean select)
    {
        Locator checkBoxLocator = Locator.checkboxByNameAndValue("requiredFields", name);

        if (select)
            checkCheckbox(checkBoxLocator);
        else
        {
            if (isChecked(checkBoxLocator))
                click(checkBoxLocator);
        }
    }

    @LogMethod
    private void verifyFieldChecked(@LoggedParam String fieldName)
    {
        if (isChecked(Locator.checkboxByNameAndValue("requiredFields", fieldName)))
            return;

        assertFalse("Checkbox not set for element: " + fieldName, false);
    }

    @Test
    public void viewSelectedDetailsTest()
    {
        DataRegionTable issuesTable = new DataRegionTable("Issues", this);

        issuesTable.setFilter("Status", "Has Any Value", null);
        issuesTable.checkAll();
        clickButton("View Details");
        assertTextPresent("a bright flash of light");
        assertTextPresent("alien autopsy");
        assertTextPresent("No big whup");
        clickAndWait(Locator.linkWithText("view grid"));
    }

    @Test
    public void lastFilterTest()
    {
        // assert both issues are present
        clearAllFilters("Issues", "IssueId");
        assertTextPresent(ISSUE_TITLE_0);
        assertTextPresent(ISSUE_TITLE_1);

        // Filter out all pri-1 bugs; assert newly created issue is filtered out
        DataRegionTable issuesTable = new DataRegionTable("Issues", this);
        issuesTable.setFilter("Priority", "Does Not Equal", "1");
        assertTextPresent(ISSUE_TITLE_0);
        assertTextNotPresent(ISSUE_TITLE_1);

        // view an issue
        clickAndWait(Locator.linkWithText(ISSUE_TITLE_0));

        // assert .lastFilter is applied
        clickAndWait(Locator.linkWithText("return to grid"));
        assertTextPresent(ISSUE_TITLE_0);
        assertTextNotPresent(ISSUE_TITLE_1);

        clearAllFilters("Issues", "IssueId");
    }

    @Test
    public void queryTest()
    {
        clickProject(PROJECT_NAME);
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addQueryWebPart("issues");

        clickAndWait(Locator.linkWithText("Issues Queries"));
        createNewQuery("issues");
        setFormElement(Locator.name("ff_newQueryName"), "xxyzzy");
        clickButton("Create and Edit Source");
        _extHelper.clickExtTab("Data");

        waitForText(ISSUE_TITLE_0, WAIT_FOR_JAVASCRIPT);
        waitForText(ISSUE_TITLE_1, WAIT_FOR_JAVASCRIPT);

        clickProject(PROJECT_NAME);

        // remove query which is broken now because requiredFieldsTest() renames MyFirstString
        deleteQuery(PROJECT_NAME, "issues", "xxyzzy");
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
        String[] issueTitles = {"This is for the subfolder test", "A sub-folder issue"};

        // NOTE: be afraid -- very afraid. this data is used other places and could lead to false+ or false-
        _issuesHelper.addIssue(Maps.of("assignedTo", getDisplayName(), "title", issueTitles[0], "priority", "2", "comment", "a bright flash of light"));

        clickFolder(SUB_FOLDER_NAME);

        //Issue 15550: Better tests for view details, admin, and email preferences
        for (String button : new String[]{"Admin", "Email Preferences"})
        {
            Locator l = Locator.xpath("//span/a[span[text()='" + button + "']]");
            assertTrue(getAttribute(l, "href").contains(PROJECT_NAME + "/" + SUB_FOLDER_NAME));
        }

        _issuesHelper.addIssue(Maps.of("assignedTo", getDisplayName(), "title", issueTitles[1], "priority", "2", "comment", "We are in a sub-folder"));

        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText("Issues Summary"));
        // Set the container filter to include subfolders
        _extHelper.clickMenuButton(true, "Views", "Folder Filter", "Current folder and subfolders");

        // Verify the URL of issueTitles[0] goes to PROJECT_NAME
        String href = getAttribute(Locator.linkContainingText(issueTitles[0]), "href");
        assertTrue("Expected issue details URL to link to project container",
                href.contains("/issues/" + PROJECT_NAME + "/details.view") || href.contains("/" + PROJECT_NAME + "/issues-details.view"));

        // Verify the URL of issueTitles[1] goes to PROJECT_NAME/SUB_FOLDER_NAME
        href = getAttribute(Locator.linkContainingText(issueTitles[1]), "href");
        assertTrue("Expected issue details URL to link to sub-folder container",
                href.contains("/issues/" + PROJECT_NAME + "/" + SUB_FOLDER_NAME + "/details.view") || href.contains("/" + PROJECT_NAME + "/" + SUB_FOLDER_NAME + "/issues-details.view"));
    }

    @Test
    public void duplicatesTest()
    {
        _issuesHelper.addIssue(Maps.of("assignedTo", getDisplayName(), "title", "This Is some Issue -- let's say A"));
        String issueIdA = getLastPageIssueId();

        _issuesHelper.addIssue(Maps.of("assignedTo", getDisplayName(), "title", "This is another issue -- let's say B"));
        String issueIdB = getLastPageIssueId();

        clickAndWait(Locator.linkWithText("Resolve"));
        selectOptionByText(Locator.id("resolution"), "Duplicate");
        setFormElement(Locator.name("duplicate"), issueIdA);
        clickButton("Save");

        assertElementPresent(Locator.linkWithText(issueIdA));
        assertTextPresent("resolve as Duplicate of " + issueIdA);

        clickAndWait(Locator.linkWithText(issueIdA));

        assertElementPresent(Locator.linkWithText(issueIdB));
        assertTextPresent(String.format("Issue %s marked as duplicate of this issue.", issueIdB));
        assertTextPresent("Duplicates");
    }

    @Test
    public void moveIssueTest()
    {
        String issueTitle = "This issue will be moved";
        String displayName = getDisplayName();
        String path = String.format("/%s/%s", PROJECT_NAME, SUB_FOLDER_NAME);

        goToModule("Issues");

        DataRegionTable issuesTable = new DataRegionTable("Issues", this);
        issuesTable.uncheckAll();

        // validate that the move button not active without destination (here we validate list view)
        assertElementPresent(Locator.xpath("//a[@class=' labkey-disabled-button']/span[text()='Move']"));

        // create a new issue to be moved
        _issuesHelper.addIssue(Maps.of("assignedTo", displayName, "title", issueTitle));

        // validate that the move button not active without desintation (here we validate details view)
        assertElementNotPresent(Locator.linkWithText("move"));

        goToModule("Issues");
        clickButton("Admin");

        // attempt a bad destination
        checkRadioButton(Locator.radioButtonByNameAndValue("moveToContainer", "SpecificMoveToContainer"));
        setFormElement(Locator.name("moveToContainerSelect"), "/this/is/a/bad/path");
        updateIssue();
        assertTextPresent("Container does not exist!");
        clickAndWait(Locator.linkWithText("back"));

        // this is needed in FF as after going back, the browser does not remember the form state.
        checkRadioButton(Locator.radioButtonByNameAndValue("moveToContainer", "SpecificMoveToContainer"));

        // attempt an empty destination
        setFormElement(Locator.name("moveToContainerSelect"), "");
        updateIssue();
        assertTextPresent("The move to specific container option was selected with a blank.");
        clickAndWait(Locator.linkWithText("back"));

        // this is needed in FF as after going back, the browser does not remember the form state.
        checkRadioButton(Locator.radioButtonByNameAndValue("moveToContainer", "SpecificMoveToContainer"));

        // setup a good destination
        setFormElement(Locator.name("moveToContainerSelect"), path);
        updateIssue();

        // move the created issue
        goToModule("Issues");
        clickAndWait(Locator.linkWithText(issueTitle));
        click(Locator.linkWithText("move"));

        // handle move dialog
        waitForElement(Locator.xpath("//input[@name='moveIssueCombo']"));
        _ext4Helper.selectComboBoxItem("Container:", path);
        clickAndWait(Ext4Helper.Locators.ext4Button("Move"));

        // validate new container
        assertEquals(path, getCurrentContainerPath());

        // perform multi-issue move
        returnToProject();
        goToModule("Issues");


        String issueTitleA = "Multi-Issue Move A";
        _issuesHelper.addIssue(Maps.of("assignedTo", displayName, "title", issueTitleA));
        String issueIdA = getLastPageIssueId();

        String issueTitleB = "Multi-Issue Move B";
        _issuesHelper.addIssue(Maps.of("assignedTo", displayName, "title", issueTitleB));
        String issueIdB = getLastPageIssueId();

        goToModule("Issues");
        issuesTable.checkCheckbox(issueIdA);
        issuesTable.checkCheckbox(issueIdB);
        click(Locator.linkWithText("move"));

        // handle move dialog (copy pasta)
        waitForElement(Locator.xpath("//input[@name='moveIssueCombo']"));
        _ext4Helper.selectComboBoxItem("Container:", path);
        clickAndWait(Ext4Helper.Locators.ext4Button("Move"));

        // make sure the moved issues are no longer shwoing up
        assertTextNotPresent(issueTitleA);
        assertTextNotPresent(issueTitleB);
    }

    @Test
    public void relatedIssueTest()
    {
        Locator relatedLocator = Locator.name("related");

        _issuesHelper.addIssue(Maps.of("assignedTo", getDisplayName(), "title", "A is for Apple"));
        String issueIdA = getLastPageIssueId();

        _issuesHelper.addIssue(Maps.of("assignedTo", getDisplayName(), "title", "B is for Baking"));
        String issueIdB = getLastPageIssueId();

        _issuesHelper.addIssue(Maps.of("assignedTo", getDisplayName(), "title", "C is for Cat"));
        String issueIdC = getLastPageIssueId();

        // related C to A
        updateIssue();
        setFormElement(relatedLocator, issueIdA);
        clickButton("Save");

        assertElementPresent(Locator.linkWithText(issueIdA));
        clickAndWait(Locator.linkWithText(issueIdA));

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
        assertTextPresent(String.format("%s, %s", issueIdB, issueIdC));

        // NOTE: still need to test for case where user doesn't have permission to related issue...
    }

    @Test
    public void defaultAssignedToTest()
    {
        String user = "reader@email.com";
        Locator.XPathLocator defaultUserSelect = Locator.tagWithName("select", "defaultUser");

        // create reader user (issue 20598)
        _permissionsHelper.createPermissionsGroup("Readers");
        _permissionsHelper.assertPermissionSetting("Readers", "No Permissions");
        _permissionsHelper.setPermissions("Readers", "Reader");
        _permissionsHelper.clickManageGroup("Readers");
        setFormElement(Locator.name("names"), user);
        clickButton("Update Group Membership");

        String user1DisplayName = displayNameFromEmail(USER1);

        goToModule("Issues");

        // check for no default
        clickButton("New Issue");
        assertEquals(getSelectedOptionText(Locator.id("assignedTo")), "");
        clickButton("Cancel");

        /// check reader cannot be set as default user (issue 20598)
        clickButton("Admin");
        assertElementNotPresent(defaultUserSelect.append(Locator.tagWithText("option", user)));

        // set default
        checkRadioButton(Locator.radioButtonByNameAndValue("assignedToUser", "SpecificUser"));
        selectOptionByText(defaultUserSelect, user1DisplayName);
        clickButton("Update");
        clickButton("Back to Issues");

        // verify
        clickButton("New Issue");
        assertEquals(getSelectedOptionText(Locator.id("assignedTo")), user1DisplayName);
        clickButton("Cancel");

        // set default group and user
        clickButton("Admin");
        checkRadioButton(Locator.radioButtonByNameAndValue("assignedToMethod", "Group"));
        selectOptionByText(Locator.name("assignedToGroup"), "Site:Users");
        selectOptionByText(defaultUserSelect, getDisplayName());
        clickButton("Update");
        clickButton("Back to Issues");

        // verify
        clickButton("New Issue");
        assertEquals(getSelectedOptionText(Locator.id("assignedTo")), getDisplayName());
        clickButton("Cancel");

        // set no default user and return to project users assign list
        clickButton("Admin");
        checkRadioButton(Locator.radioButtonByNameAndValue("assignedToMethod", "ProjectUsers"));
        checkRadioButton(Locator.radioButtonByNameAndValue("assignedToUser", "NoDefaultUser"));
        clickButton("Update");
        clickButton("Back to Issues");

        // check for no default
        clickButton("New Issue");
        assertEquals(getSelectedOptionText(Locator.id("assignedTo")), "");
        clickButton("Cancel");

        // TODO: extend test to check validate full user selection list based on group selection...
        // TODO: compare user dropdown list between admin and new issues page
    }

    // NOTE: returning string here to avoid extra casting
    public String getLastPageIssueId()
    {
        String title = getLastPageTitle();
        return title.substring(title.indexOf(' '), title.indexOf(':')).trim();
    }

}
