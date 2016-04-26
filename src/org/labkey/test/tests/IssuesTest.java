/*
 * Copyright (c) 2008-2015 LabKey Corporation
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Data;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.components.dumbster.EmailRecordTable.EmailMessage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.IssuesHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@Category({DailyA.class, Data.class})
public class IssuesTest extends BaseWebDriverTest
{
    private static final String ISSUE_TITLE_0 = "A very serious issue";
    private static final String ISSUE_TITLE_1 = "Even more serious issue";
    private static final String ISSUE_TITLE_2 = "A not so serious issue";
    private static final String USER1 = "user1_issuetest@issues.test";
    private static final String USER2 = "user2_issuetest@issues.test";
    private static final String USER3 = "user3_issuetest@issues.test";

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

    @BeforeClass
    public static void doSetup() throws Exception
    {
        IssuesTest initTest = (IssuesTest)getCurrentTest();
        initTest.setupProject();
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteUsersIfPresent(USER1, USER2);
        deleteProject(getProjectName(), afterTest);
    }

    @Before
    public void returnToProject()
    {
        clickProject(getProjectName());
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
        _extHelper.clickMenuButton(true, "Grid Views", "Folder Filter", "Current folder");
    }

    public void setupProject()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(getProjectName(), null);
        _permissionsHelper.createPermissionsGroup(TEST_GROUP);
        _permissionsHelper.assertPermissionSetting(TEST_GROUP, "No Permissions");
        _permissionsHelper.setPermissions(TEST_GROUP, "Editor");
        clickButton("Save and Finish");

        _containerHelper.enableModule(getProjectName(), "Dumbster");

        clickProject(getProjectName());

        portalHelper.addWebPart("Issues Summary");
        portalHelper.addWebPart("Search");
        assertTextPresent("Open");

        enableEmailRecorder();
        checkEmptyToAssignedList();
        addProjectUsersToGroup();
        createIssues();
    }

    private void checkEmptyToAssignedList()
    {
        // InsertAction -- user isn't in any groups, so shouldn't appear in the assigned-to list yet

        goToModule("Issues");
        clickButton("New Issue");
        String assignedToText = getText(Locator.name("assignedTo"));
        assertEquals(assignedToText, "");
    }

    private void addProjectUsersToGroup()
    {
        // Add to group so user appears
        clickProject("IssuesVerifyProject");
        _permissionsHelper.addUserToProjGroup(PasswordUtil.getUsername(), getProjectName(), TEST_GROUP);
        _permissionsHelper.addUserToProjGroup(USER1, getProjectName(), TEST_GROUP);
        createUser(USER2, null, false);
    }

    private void createIssues()
    {
        clickProject(getProjectName());
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
        selectOptionByText(Locator.name("assignedTo"), getDisplayName());
        selectOptionByText(Locator.name("milestone"), "2012");
        setFormElement(Locator.name("string4"), "http://www.issues.test");
        selectOptionByText(Locator.name("string5"), "Polonium");
        clickButton("Save");

        // find issueId - parse the text from first space to :
        String issueId = getIssueId();

        // DetailsAction
        assertTextPresent("Issue " + issueId + ": " + issueTitle,
                "Milestone", "MyInteger", "MySecondInteger", "MyFirstString", "MyThirdString", "MyFourthString", "MyFifthString");
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
        searchFor(getProjectName(), "hype", 1, issueTitle);

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
            test.assertNoLabKeyErrors();
            test.assertTextPresent(value);
        }
    }

    @Test
    public void badUserNotifyList()
    {
        String badUsername = "junk";
        String errorMessage = String.format("Failed to add user %s: Invalid user display name", badUsername);

        // NOTE: re using issue but in idempotent manner!
        clickAndWait(Locator.linkWithText(ISSUE_TITLE_0));

        updateIssue();
        setFormElement(Locator.name("notifyList"), badUsername);
        clickButton("Save");

        assertTextPresent(errorMessage);

        clickButton("Cancel");
    }

    @Test
    public void testEmailTemplate()
    {
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
        assertFormElementEquals(Locator.name("emailSubject"), subject);
    }

    @Test
    public void emailTest()
    {
        goToModule("Dumbster");
        assertTextPresent("No email recorded."); // No other test should trigger notification

        goToModule("Issues");

        // EmailPrefsAction
        clickButton("Email Preferences");
        checkCheckbox(Locator.checkboxByNameAndValue("emailPreference", "8")); // self enter/edit an issue
        clickButton("Update");

        impersonate(USER1);
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("Issues Summary"));
        clickButton("Email Preferences");
        uncheckCheckbox(Locator.checkboxByNameAndValue("emailPreference", "2")); // issue assigned to me is modified
        clickButton("Update");
        stopImpersonating();

        clickProject(getProjectName());
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
        assertTitleContains(ISSUE_TITLE_2);

        goToModule("Dumbster");
        pushLocation();

        EmailRecordTable emailTable = new EmailRecordTable(this);
        EmailMessage message = emailTable.getMessage(ISSUE_TITLE_2 + ",\" has been opened and assigned to " + displayNameFromEmail(USER1));

        // Presumed to get the first message
        List<String> recipients = emailTable.getColumnDataAsText("To");
        assertTrue("User did not receive issue notification", recipients.contains(PasswordUtil.getUsername()));
        assertTrue(USER1 + " did not receieve issue notification", recipients.contains(USER1));
        assertFalse(USER2 + " receieved issue notification without container read permission", recipients.contains(USER2));

        assertTrue("Issue Message does not contain title", message.getSubject().contains(ISSUE_TITLE_2));

        assertTextNotPresent("This line shouldn't appear");
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

        assertFormElementEquals(Locator.name("entrySingularName"), "Ticket");
        assertFormElementEquals(Locator.name("entryPluralName"), "Tickets");

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
        final String subFolder = "Required Fields";
        final String[] requiredFields = {"Title", "AssignedTo", "Type", "Area", "Priority", "Milestone",
                "NotifyList", "String1", "Int1"};
        final String[] requiredFieldLabels = {"Title", "AssignedTo", "Type", "Area", "Milestone",
                "NotifyList", "Customer Name", "Contract Number"};
        Set<String> expectedErrors = new HashSet<>();

        _containerHelper.createSubfolder(getProjectName(), subFolder);

        goToModule("Issues");
        clickButton("Admin");

        addKeywordsAndVerify("type", "Type", "Type");
        addKeywordsAndVerify("area", "Area", "Area");
        addKeywordsAndVerify("milestone", "Milestone", "Milestone");

        setFormElement(Locator.name("int1"), "Contract Number");
        setFormElement(Locator.name("string1"), "Customer Name");

        updateIssue();

        for (String field : requiredFields)
            checkRequiredField(field, true);

        clickButton("Update");
        clickButton("Back to Issues");
        clickButton("New Issue");
        clickButton("Save");

        for (String label : requiredFieldLabels)
        {
            expectedErrors.add(String.format("Field %s cannot be blank.", label));
        }

        Set<String> errors = new HashSet<>(getTexts(Locators.labkeyError.findElements(getDriver())));
        errors.remove("*"); // From "Fields marked with an asterisk * are required."
        Assert.assertEquals("Wrong errors", expectedErrors, errors);
        clickButton("Cancel");

        clickButton("Admin");

        for (String field : requiredFields)
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
    }

    @LogMethod
    private void checkRequiredField(@LoggedParam String name, boolean select)
    {
        Locator checkBoxLocator = Locator.checkboxByNameAndValue("requiredFields", name);

        if (select)
            checkCheckbox(checkBoxLocator);
        else
            uncheckCheckbox(checkBoxLocator);
    }

    @LogMethod
    private void verifyFieldChecked(@LoggedParam String fieldName)
    {
        assertTrue("Checkbox not set for element: " + fieldName, isChecked(Locator.checkboxByNameAndValue("requiredFields", fieldName)));
    }

    @Test
    public void viewSelectedDetailsTest()
    {
        DataRegionTable issuesTable = new DataRegionTable("Issues", this);

        issuesTable.setFilter("Status", "Has Any Value", null);
        issuesTable.checkAll();
        clickButton("View Details");
        assertTextPresent(
                "a bright flash of light",
                "alien autopsy",
                "No big whup");
        clickAndWait(Locator.linkWithText("view grid"));
    }

    @Test
    public void lastFilterTest()
    {
        DataRegionTable issuesTable = new DataRegionTable("Issues", this);

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
        clickAndWait(Locator.linkWithText("return to grid"));
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
        clickButton("Create and Edit Source");
        _extHelper.clickExtTab("Data");

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
        final String[] issueTitles = {"This is for the subfolder test", "A sub-folder issue"};
        final String subFolder = "SubFolder";

        // NOTE: be afraid -- very afraid. this data is used other places and could lead to false+ or false-
        _issuesHelper.addIssue(Maps.of("assignedTo", getDisplayName(), "title", issueTitles[0], "priority", "2", "comment", "a bright flash of light"));

        _containerHelper.createSubfolder(getProjectName(), subFolder);
        (new PortalHelper(this)).addWebPart("Issues List");


        //Issue 15550: Better tests for view details, admin, and email preferences
        for (String button : new String[]{"Admin", "Email Preferences"})
        {
            Locator l = Locator.xpath("//span/a[span[text()='" + button + "']]");
            String href = getAttribute(l, "href");
            String containerPath = getProjectName() + "/" + subFolder;
            assertTrue("'" + href + "' did not contain '" + containerPath + "'", href.contains(containerPath));
        }

        _issuesHelper.addIssue(Maps.of("assignedTo", getDisplayName(), "title", issueTitles[1], "priority", "2", "comment", "We are in a sub-folder"));

        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("Issues Summary"));
        // Set the container filter to include subfolders
        _extHelper.clickMenuButton(true, "Grid Views", "Folder Filter", "Current folder and subfolders");

        // Verify the URL of issueTitles[0] goes to getProjectName()
        String href = getAttribute(Locator.linkContainingText(issueTitles[0]), "href");
        assertTrue("Expected issue details URL to link to project container",
                href.contains("/issues/" + getProjectName() + "/details.view") || href.contains("/" + getProjectName() + "/issues-details.view"));

        // Verify the URL of issueTitles[1] goes to getProjectName()/SUB_FOLDER_NAME
        href = getAttribute(Locator.linkContainingText(issueTitles[1]), "href");
        assertTrue("Expected issue details URL to link to sub-folder container",
                href.contains("/issues/" + getProjectName() + "/" + subFolder + "/details.view") || href.contains("/" + getProjectName() + "/" + subFolder + "/issues-details.view"));
    }

    @Test
    public void duplicatesTest()
    {
        _issuesHelper.addIssue(Maps.of("assignedTo", getDisplayName(), "title", "This Is some Issue -- let's say A"));
        String issueIdA = getIssueId();

        _issuesHelper.addIssue(Maps.of("assignedTo", getDisplayName(), "title", "This is another issue -- let's say B"));
        String issueIdB = getIssueId();

        clickAndWait(Locator.linkWithText("Resolve"));
        selectOptionByText(Locator.id("resolution"), "Duplicate");
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
        final String displayName = getDisplayName();
        final String path = String.format("/%s/%s", getProjectName(), subFolder);

        _containerHelper.createSubfolder(getProjectName(), subFolder);

        goToProjectHome();
        goToModule("Issues");

        // create a new issue to be moved
        _issuesHelper.addIssue(Maps.of("assignedTo", displayName, "title", issueTitle));

        // validate that the move button not active without desintation (here we validate details view)
        assertElementNotPresent(Locator.linkWithText("move"));

        goToModule("Issues");
        // validate that the move button not active without destination (here we validate list view)
        assertElementPresent(Locator.tag("a").withClass("labkey-disabled-button").withText("Move"));
        clickButton("Admin");

        /// attempt a bad destination
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
        String issueIdA = getIssueId();

        String issueTitleB = "Multi-Issue Move B";
        _issuesHelper.addIssue(Maps.of("assignedTo", displayName, "title", issueTitleB));
        String issueIdB = getIssueId();

        goToModule("Issues");

        DataRegionTable issuesTable = new DataRegionTable("Issues", this);
        issuesTable.checkCheckbox(issueIdA);
        issuesTable.checkCheckbox(issueIdB);
        click(Locator.linkWithText("move"));

        // handle move dialog (copy pasta)
        waitForElement(Locator.xpath("//input[@name='moveIssueCombo']"));
        _ext4Helper.selectComboBoxItem("Container:", path);
        clickAndWait(Ext4Helper.Locators.ext4Button("Move"));

        // make sure the moved issues are no longer shwoing up
        assertTextNotPresent(issueTitleA, issueTitleB);
    }

    @Test
    public void relatedIssueTest()
    {
        Locator relatedLocator = Locator.name("related");

        _issuesHelper.addIssue(Maps.of("assignedTo", getDisplayName(), "title", "A is for Apple"));
        String issueIdA = getIssueId();

        _issuesHelper.addIssue(Maps.of("assignedTo", getDisplayName(), "title", "B is for Baking"));
        String issueIdB = getIssueId();

        _issuesHelper.addIssue(Maps.of("assignedTo", getDisplayName(), "title", "C is for Cat"));
        String issueIdC = getIssueId();

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

        // issue 20699 - NPE b/c default assign to user deleted!
        String deletedUser = "deleteme@deletronia.com";
        _permissionsHelper.addUserToProjGroup(deletedUser, getProjectName(), TEST_GROUP);
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("Issues Summary"));
        clickButton("Admin");
        checkRadioButton(Locator.radioButtonByNameAndValue("assignedToUser", "SpecificUser"));
        selectOptionByText(defaultUserSelect, displayNameFromEmail(deletedUser));
        clickButton("Update");

        // taking care of some clean-up while here for the test.
        deleteUsers(true, deletedUser, user);

        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("Issues Summary"));
        clickButton("New Issue");
        // NPE
        //clickButton("Cancel");

        // TODO: extend test to check validate full user selection list based on group selection...
        // TODO: compare user dropdown list between admin and new issues page
    }

    @Test
    public void testAdminSettingInheritance()
    {
        final String subFolderA = "Folder_A"; //a folder to inherit from
        final String pathToA = String.format("/%s/%s", getProjectName(), subFolderA);

        final String subFolderA1 = "Folder_A1";
        final String pathToA1 = String.format("/%s/%s", getProjectName(), subFolderA1);

        final String subFolderB = "Folder_B"; //folder that will inherit settings from Folder_A
        final String pathToB = String.format("/%s/%s", getProjectName(), subFolderB);

        final String subFolderC = "Folder_C"; //folder that will not have an option to inherit from Folder_B, since Folder_B inherits from Folder_A
        final String pathToC = String.format("/%s/%s", getProjectName(), subFolderC);

        _containerHelper.createSubfolder(getProjectName(), subFolderA);
        _containerHelper.createSubfolder(getProjectName(), subFolderA1); // Folder of related issues list
        _containerHelper.createSubfolder(getProjectName(), subFolderB);
        _containerHelper.createSubfolder(getProjectName(), subFolderC);

        /** Start: go to Folder_A, and set admin settings**/
        goToProjectHome(getProjectName());
        clickFolder(subFolderA);
        goToModule("Issues");
        clickButton("Admin");

        // Singular item name
        setFormElement(Locator.name("entrySingularName"), "Issue w/Folder_A Admin");

        // Plural items name
        setFormElement(Locator.name("entryPluralName"), "Issues w/Folder_A Admin");

        // Comment sort direction
        selectOptionByValue(Locator.name("direction"), "DESC");

        // Populate the assigned to list from:
        selectOptionByText(Locator.name("assignedToGroup"), "Site:Users");

        // Set default assigned to user:
        checkRadioButton(Locator.radioButtonByNameAndValue("assignedToUser", "NoDefaultUser"));

        // Inherit Admin Setting from folder:
        checkRadioButton(Locator.radioButtonByNameAndValue("inheritFromContainer", "DoNotInheritFromContainer"));

        // Set move to folder:
        checkRadioButton(Locator.radioButtonByNameAndValue("moveToContainer", "NoMoveToContainer"));

        // Set folder of related issues list
        setFormElement(Locator.name("relatedIssuesList"), pathToA1);

        // set Custom Fields
        setFormElement(Locator.name("type"), "Bugs");
        setFormElement(Locator.name("area"), "Dev Area");
        setFormElement(Locator.name("priority"), "Prioritah");
        setFormElement(Locator.name("int1"), "Contract Number");
        setFormElement(Locator.name("string1"), "Development Sprint");
        checkCheckbox(Locator.checkboxByNameAndValue("pickListColumns", "string1"));
        selectOptionByValue(Locator.xpath("//*[@id=\"adminViewOfIssueList\"]/table/tbody/tr[2]/td[2]/table/tbody/tr[12]/td[3]/select"), "insert");
        clickButton("Update");

        // add keywords
        addKeywordsAndVerify("type", "Bugs", "Bad", "Not so Bad");
        addKeywordsAndVerify("area", "Dev Area", "UI", "Server", "Database");
        addKeywordsAndVerify("string1", "Development Sprint", "15.1", "15.2", "15.3", "15.4");

        // set Required Fields
        checkCheckbox(Locator.checkboxByNameAndValue("requiredFields", "Type"));
        checkCheckbox(Locator.checkboxByNameAndValue("requiredFields", "Area"));
        checkCheckbox(Locator.checkboxByNameAndValue("requiredFields", "Priority"));
        checkCheckbox(Locator.checkboxByNameAndValue("requiredFields", "Int1"));
        checkCheckbox(Locator.checkboxByNameAndValue("requiredFields", "String1"));

        clickButton("Update");

        //store option values, to later compare with inheritor's values
        List<WebElement> typeOptionsParent = Locator.xpath("//*[@id=\"formtype\"]//td[1]").findElements(getDriver());
        List<String> typeOptionsParentList = getTexts(typeOptionsParent);

        List<WebElement> string1OptionsParent = Locator.xpath("//*[@id=\"formstring1\"]").findElements(getDriver());
        List<String> string1OptionsParentList = getTexts(string1OptionsParent);

        /***** End: go to Folder_A, and set admin settings *****/

        /** Start: go to Folder_B, and inherit settings from Folder_A **/
        goToProjectHome(getProjectName());
        clickFolder(subFolderB);
        goToModule("Issues");
        clickButton("Admin");

        //inheriting from an empty folder
        checkRadioButton(Locator.radioButtonByNameAndValue("inheritFromContainer", "InheritFromSpecificContainer"));
        setFormElement(Locator.name("inheritFromContainerSelect"), "");
        clickButton("Update");

        assertTextPresent("The Inherit Admin Setting's 'Choose Folder' option was selected with a blank.");
        clickAndWait(Locator.linkWithText("back"));
        checkRadioButton(Locator.radioButtonByNameAndValue("inheritFromContainer", "DoNotInheritFromContainer"));

        //Add caption to custom field 'Type', 'String1', and 'String2'
        setFormElement(Locator.name("type"), "Probs");
        setFormElement(Locator.name("string1"), "Folder_B_Str1");
        checkCheckbox(Locator.checkboxByNameAndValue("pickListColumns", "string1"));
        setFormElement(Locator.name("string2"), "Folder_B_Str2");
        checkCheckbox(Locator.checkboxByNameAndValue("pickListColumns", "string2"));
        selectOptionByValue(Locator.xpath("//*[@id=\"adminViewOfIssueList\"]/table/tbody/tr[2]/td[2]/table/tbody/tr[13]/td[3]/select"), "admin");
        clickButton("Update");

        //Add Options to string2
        addKeywordsAndVerify("string2", "Folder_B_Str2", "B_1", "B_2", "B_3");
        checkCheckbox(Locator.checkboxByNameAndValue("requiredFields", "String2"));

        goToProjectHome(getProjectName());//REmove
        clickFolder(subFolderB);
        goToModule("Issues");
        clickButton("Admin");

        /** inherit from Folder_A - test for 'Cancel' to inherit **/
        checkRadioButton(Locator.radioButtonByNameAndValue("inheritFromContainer", "InheritFromSpecificContainer"));
        setFormElement(Locator.name("inheritFromContainerSelect"), pathToA);

        //On update, check for a popup message: "Custom Fields of current folder will get overridden".

        doAndWaitForPageToLoad(() ->
        {
            click(Locator.linkWithText("Update"));
            assertEquals("Custom Fields of current folder will get overridden.", cancelAlert());
        });

        //Test that no changes were made to the current folder since we Cancelled to inherit.
        assertFormElementEquals(Locator.name("type"), "Probs");
        assertFormElementEquals(Locator.name("string1"), "Folder_B_Str1");
        assertChecked(Locator.checkboxByNameAndValue("pickListColumns", "string1"));
        assertFormElementEquals(Locator.name("string2"), "Folder_B_Str2");
        assertChecked(Locator.checkboxByNameAndValue("pickListColumns", "string2"));

        assertOptionEquals(Locator.xpath("//*[@id=\"adminViewOfIssueList\"]/table/tbody/tr[2]/td[2]/table/tbody/tr[13]/td[3]/select"), "Admin");

        /** inherit from Folder_A - test for 'OK' to inherit **/

        checkRadioButton(Locator.radioButtonByNameAndValue("inheritFromContainer", "InheritFromSpecificContainer"));
        setFormElement(Locator.name("inheritFromContainerSelect"), pathToA);


        doAndWaitForPageToLoad(() ->
        {
            click(Locator.linkWithText("Update"));
            assertAlert("Custom Fields of current folder will get overridden.");
        });

        //check if all the inherited fields are populated and are disabled.
        assertFormElementEquals(Locator.name("type"), "Bugs");
        WebElement inputType = Locator.input("type").findElement(getDriver());
        assertFalse(inputType.getText() + " should be disabled.", inputType.isEnabled());

        assertFormElementEquals(Locator.name("area"), "Dev Area");
        WebElement inputArea = Locator.input("type").findElement(getDriver());
        assertFalse(inputArea.getText() + " should be disabled.", inputArea.isEnabled());

        assertFormElementEquals(Locator.name("priority"), "Prioritah");
        WebElement inputPri = Locator.input("type").findElement(getDriver());
        assertFalse(inputPri.getText() + " should be disabled.", inputPri.isEnabled());

        assertFormElementEquals(Locator.name("int1"), "Contract Number");
        WebElement inputInt1 = Locator.input("type").findElement(getDriver());
        assertFalse(inputInt1.getText() + " should be disabled.", inputInt1.isEnabled());

        assertFormElementEquals(Locator.name("string1"), "Development Sprint");
        WebElement inputString1 = Locator.input("type").findElement(getDriver());
        assertFalse(inputString1.getText() + " should be disabled.", inputString1.isEnabled());

        //check if inherited options are the same as the "parent"
        List<WebElement> typeOptions = Locator.xpath("//*[@id=\"formtype\"]//td[1]").findElements(getDriver());
        List<String> typeOptionsList = getTexts(typeOptions);
        assertEquals("Inherited 'type' options does not match the current options.", typeOptionsParentList, typeOptionsList);

        List<WebElement> string1Options = Locator.xpath("//*[@id=\"formstring1\"]").findElements(getDriver());
        List<String> string1OptionsList = getTexts(string1Options);
        assertEquals("Inherited 'string1' options does not match the current options.", string1OptionsParentList, string1OptionsList);

        //check if inherited options are not modifiable or are not modified
//        addKeyword("type", "Bugs", "can wait"); //TODO: should check for if it's disabled ("option/keyword" fields are not disabled in the current implementation)
//        addKeyword("string1", "Development Sprint", "15.5");//TODO: should check for if it's disabled ("option/keyword" fields are not disabled in the current implementation)

        //Check if non-inherited field still exists.
        assertFormElementEquals(Locator.name("string2"), "Folder_B_Str2");
        WebElement inputString2 = Locator.input("string2").findElement(getDriver());
        assertTrue(inputString2.getText() + " is not enabled.", inputString2.isEnabled());

        //check if non-inherited options are modifiable
        addKeyword("string2", "Folder_B_Str2", "B_4");
        WebElement str2Option = Locator.xpath("//*[@id='formstring2']/table/tbody/tr[4]/td[1]").findElement(getDriver());
        assertTrue("Element B_4 not found.", "B_4".equals(str2Option.getText()));

        /***** End: go to Folder_B, and inherit settings from Folder_A *****/

        /***** Start: Test for a message effecting inheritors in Folder_A *****/

        //go to Folder_A, Add caption to an empty custom field of Folder_A
        goToProjectHome(getProjectName());
        clickFolder("Folder_A");
        goToModule("Issues");
        clickButton("Admin");
        setFormElement(Locator.name("milestone"), "milestone_A");

        doAndWaitForPageToLoad(() ->
        {
            click(Locator.linkWithText("Update"));
            assertAlert("Found one or more folders with settings inherited from the current folder: Adding new Custom Fields will override Custom Fields of inheriting folders.");
        });

        //check if the inheritor sees the update
        goToProjectHome(getProjectName());
        clickFolder("Folder_B");
        goToModule("Issues");
        clickButton("Admin");

        assertFormElementEquals(Locator.name("milestone"), "milestone_A");
        WebElement inputMilestone = Locator.input("milestone").findElement(getDriver());
        assertFalse(inputMilestone.getText() + " should be disabled.", inputMilestone.isEnabled());

        /***** End: Test for a message effecting inheritors in Folder_A *****/

        /***** Start: Test for chaining - which is disallowed *****/
        goToProjectHome(getProjectName());
        clickFolder("Folder_C");
        goToModule("Issues");
        clickButton("Admin");

        //Under Inherit From Container: Choose Folder : Folders that have inherited settings from a different folder shouldn't be listed, in this case, Folder_B.
        checkRadioButton(Locator.radioButtonByNameAndValue("inheritFromContainer", "InheritFromSpecificContainer"));
        assertFormElementNotEquals(Locator.name("inheritFromContainerSelect"), pathToB);

        /***** End: Test for chaining - which is disallowed *****/

    }

    // NOTE: returning string here to avoid extra casting
    public String getIssueId()
    {
        String title = getDriver().getTitle();
        return title.substring(title.indexOf(' '), title.indexOf(':')).trim();
    }
}
