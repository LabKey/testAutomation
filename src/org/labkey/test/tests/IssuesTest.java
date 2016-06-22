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
import org.labkey.api.collections.CaseInsensitiveHashSet;
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
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

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
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Before
    public void returnToProject()
    {
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("Issue Summary"));
        readyState();
    }

    public void readyState()
    {
        DataRegionTable issuesTable = new DataRegionTable("issues-issues", getDriver());

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
        _issuesHelper.createNewIssuesList("issues", _containerHelper);

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
        _userHelper.createUser(USER2, false);
    }

    private void createIssues()
    {
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("Issue Summary"));
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

        Locator newIssueButton = Locator.lkButton("New Issue");
        assertElementPresent(newIssueButton);

        // quick security test
        // TODO push lots of locations as we go and move this test to end
        pushLocation();
        pushLocation();
        signOut();
        popLocation();                          // try open issues as guest
        assertElementNotPresent(newIssueButton);
        assertElementPresent(Locator.tagWithName("form", "login"));
        signIn();
        popLocation();                          // and logged in again
        assertElementPresent(newIssueButton);

        addLookupValues("issues", "area", Arrays.asList("Area51", "Fremont", "Downtown"));
        addLookupValues("issues", "type", Arrays.asList("UFO", "SPEC", "AAA"));
        addLookupValues("issues", "milestone", Arrays.asList("2012", "2013"));

        // create lookups for new custom fields
        createLookupList("issues", "MyFirstString", Arrays.asList("North", "South"));
        createLookupList("issues", "MyFifthString", Arrays.asList("Cadmium", "Polonium"));

        // SetCustomColumnConfigurationAction
        List<ListHelper.ListColumn> fields = new ArrayList<>();

        fields.add(new ListHelper.ListColumn("MyInteger", "MyInteger", ListHelper.ListColumnType.Integer, ""));
        fields.add(new ListHelper.ListColumn("MySecondInteger", "MySecondInteger", ListHelper.ListColumnType.Integer, ""));
        fields.add(new ListHelper.ListColumn("MyFirstString", "MyFirstString", ListHelper.ListColumnType.String, "", new ListHelper.LookupInfo(null, "lists", getLookupTableName("issues", "MyFirstString"))));
        fields.add(new ListHelper.ListColumn("MyThirdString", "MyThirdString", ListHelper.ListColumnType.String, ""));
        fields.add(new ListHelper.ListColumn("MyFourthString", "MyFourthString", ListHelper.ListColumnType.String, ""));
        fields.add(new ListHelper.ListColumn("MyFifthString", "MyFifthString", ListHelper.ListColumnType.String, "", new ListHelper.LookupInfo(null, "lists", getLookupTableName("issues", "MyFifthString"))));

        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("Issue Summary"));
        _issuesHelper.goToAdmin();

        for (ListHelper.ListColumn col : fields)
        {
            _listHelper.addField(col);
        }
        clickButton("Save");

        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("Issue Summary"));

        // InsertAction
        clickButton("New Issue");
        setFormElement(Locator.name("title"), issueTitle);
        selectOptionByText(Locator.name("type"), "UFO");
        selectOptionByText(Locator.name("area"), "Area51");
        selectOptionByText(Locator.name("priority"), "2");
        setFormElement(Locator.name("comment"), "a bright flash of light");
        selectOptionByText(Locator.name("assignedTo"), getDisplayName());
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

    private void setDefaultValues(Map<String, String> defaultValues)
    {
        clickAndWait(Locator.linkWithText("Issue Summary"));
        _issuesHelper.goToAdmin();
        click(Locator.tag("div").withClass("gwt-Label").withText("Area"));
        click(Locator.tag("span").withClass("x-tab-strip-text").withText("Advanced"));
        clickAndWait(Locator.linkWithText("set value"));

        for (Map.Entry<String, String> field : defaultValues.entrySet())
        {
            if ("select".equals(Locator.name(field.getKey()).findElement(getDriver()).getTagName()))
                selectOptionByText(Locator.name(field.getKey()), field.getValue());
            else
                setFormElement(Locator.id(field.getKey()), field.getValue());
        }
        clickButton("Save Defaults");
    }

    @LogMethod
    private void setRequiredFields(int[] positions, boolean selected)
    {
        for (int pos : positions)
        {
            click(Locator.tag("div").withAttribute("id", "label" + pos));
            click(Locator.tag("span").withClass("x-tab-strip-text").withText("Validators"));

            if (selected)
                checkCheckbox(Locator.checkboxByName("required"));
            else
                uncheckCheckbox(Locator.checkboxByName("required"));
        }
    }

    private static String getLookupTableName(String issueDefName, String field)
    {
        return issueDefName + "-" + field.toLowerCase() + "-lookup";
    }

    private void addLookupValues(String issueDefName, String fieldName, Collection<String> values)
    {
        addLookupValues(this, issueDefName, fieldName, values);
    }

    @LogMethod
    public static void addLookupValues(BaseWebDriverTest test, String issueDefName, String fieldName, Collection<String> values)
    {
        if (!test.isElementPresent(Locator.lkButton("Import Data")))
        {
            test.goToSchemaBrowser();
            test.selectQuery("lists", getLookupTableName(issueDefName, fieldName));
            test.click(Locator.linkWithText("view data"));
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

    private void goToIssuesAdmin()
    {
        clickButton("Admin");
        waitForText("Configure Fields");
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

        // no longer relevant, the email template is non-restrictive to allow
        // custom fields
/*
        assertTextPresent("Invalid template");
        setFormElement(Locator.name("emailMessage"), TEST_EMAIL_TEMPLATE);
        clickButton("Save");
        assertTextNotPresent("Invalid template");
        assertEquals(subject, getFormElement(Locator.name("emailSubject")));
*/
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
        clickAndWait(Locator.linkWithText("Issue Summary"));
        clickButton("Email Preferences");
        uncheckCheckbox(Locator.checkboxByNameAndValue("emailPreference", "2")); // issue assigned to me is modified
        clickButton("Update");
        stopImpersonating();

        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("Issue Summary"));

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
        _issuesHelper.goToAdmin();

        setFormElement(Locator.name("entrySingularName"), "Ticket");
        setFormElement(Locator.name("entryPluralName"), "Tickets");
        clickButton("Save");

        _issuesHelper.goToAdmin();
        assertEquals("Ticket", getFormElement(Locator.name("entrySingularName")));
        assertEquals("Tickets", getFormElement(Locator.name("entryPluralName")));

        assertTextPresent("Tickets Admin Page");
        clickButton("Cancel");

        assertTextPresent("Tickets List");
        assertTextNotPresent("Issues List");
        assertButtonPresent("New Ticket");
        assertButtonPresent("Jump to Ticket");
        assertTextPresent("Ticket ID");
        assertTextNotPresent("Issue ID");

        _issuesHelper.goToAdmin();
        setFormElement(Locator.name("entrySingularName"), "Issue");
        setFormElement(Locator.name("entryPluralName"), "Issues");
        clickButton("Save");
    }

    @Test
    public void requiredFieldsTest()
    {
        final String subFolder = "Required Fields";
        final int[] requiredFieldPos = {0,1,2,3,4,5,6,8,9};
        final String[] requiredFieldLabels = {"title", "assignedto", "type", "area", "milestone",
                "notifylist", "customername", "contractnumber"};
        Set<String> expectedErrors = new CaseInsensitiveHashSet();

        _containerHelper.createSubfolder(getProjectName(), subFolder);
        _issuesHelper.createNewIssuesList("required-fields", _containerHelper);

        List<ListHelper.ListColumn> fields = new ArrayList<>();

        fields.add(new ListHelper.ListColumn("ContractNumber", "Contract Number", ListHelper.ListColumnType.Integer, ""));
        fields.add(new ListHelper.ListColumn("CustomerName", "Customer Name", ListHelper.ListColumnType.String, ""));

        clickFolder(subFolder);
        clickAndWait(Locator.linkWithText("Issue Summary"));
        _issuesHelper.goToAdmin();

        for (ListHelper.ListColumn col : fields)
        {
            _listHelper.addField(col);
        }
        setRequiredFields(requiredFieldPos, true);
        clickButton("Save");
        clickButton("New Issue");
        clickButton("Save");

        for (String label : requiredFieldLabels)
        {
            expectedErrors.add(String.format("Field %s cannot be blank.", label));
        }

        Set<String> errors = new CaseInsensitiveHashSet(getTexts(Locators.labkeyError.findElements(getDriver())));
        errors.remove("*"); // From "Fields marked with an asterisk * are required."
        Assert.assertEquals("Wrong errors", expectedErrors, errors);
        clickButton("Cancel");

        _issuesHelper.goToAdmin();
        // clear all required selections except title
        setRequiredFields(requiredFieldPos, false);
        setRequiredFields(new int[]{1}, true);
        clickButton("Save");

        clickButton("New Issue");
        clickButton("Save");

        assertTextPresentCaseInsensitive("Field title cannot be blank.");
        clickButton("Cancel");
    }

    @Test
    public void viewSelectedDetailsTest()
    {
        DataRegionTable issuesTable = new DataRegionTable("issues-issues", getDriver());

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
        DataRegionTable issuesTable = new DataRegionTable("issues-issues", getDriver());

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
        _issuesHelper.createNewIssuesList("issues", _containerHelper);

        _issuesHelper.addIssue(Maps.of("assignedTo", getDisplayName(), "title", issueTitles[1], "priority", "2", "comment", "We are in a sub-folder"));

        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("Issue Summary"));
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
        final String displayName = getDisplayName();
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

        DataRegionTable issuesTable = new DataRegionTable("issues-issues", getDriver());
        issuesTable.checkCheckboxByPrimaryKey(issueIdA);
        issuesTable.checkCheckboxByPrimaryKey(issueIdB);
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
        Locator.XPathLocator specificUserSelect = Locator.tagWithClass("select", "assigned-to-user");

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
        _issuesHelper.setIssueAssignmentList("Site:Users");
        _issuesHelper.setIssueAssignmentUser(getDisplayName());
        clickButton("Save");

        // verify
        clickButton("New Issue");
        assertEquals(getSelectedOptionText(Locator.name("assignedTo")), getDisplayName());
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
        String deletedUser = "deleteme@deletronia.com";
        _permissionsHelper.addUserToProjGroup(deletedUser, getProjectName(), TEST_GROUP);
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("Issue Summary"));
        _issuesHelper.goToAdmin();
        _issuesHelper.setIssueAssignmentUser(displayNameFromEmail(deletedUser));
        clickButton("Save");

        // taking care of some clean-up while here for the test.
        deleteUsers(true, deletedUser, user);

        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("Issue Summary"));
        clickButton("New Issue");
        // NPE
        //clickButton("Cancel");

        // TODO: extend test to check validate full user selection list based on group selection...
        // TODO: compare user dropdown list between admin and new issues page
    }

    /**
     * Obsolete, legacy inheritance is not supported and is replaced by shared domain plus scoping. Consider deleting this
     * test and adding an up to date test for the new code.
     */
    @Deprecated
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
        addLookupValues("issues", "type", Collections.singletonList("Not so Bad"));
        addLookupValues("issues", "area", Arrays.asList("Server", "Database"));
        addLookupValues("issues", "Development Sprint", Arrays.asList("15.1", "15.2", "15.3", "15.4"));

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
        addLookupValues("issues", "Folder_B_Str2", Arrays.asList("B_1", "B_2", "B_3"));
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
        assertEquals("Probs", getFormElement(Locator.name("type")));
        assertEquals("Folder_B_Str1", getFormElement(Locator.name("string1")));
        assertChecked(Locator.checkboxByNameAndValue("pickListColumns", "string1"));
        assertEquals("Folder_B_Str2", getFormElement(Locator.name("string2")));
        assertChecked(Locator.checkboxByNameAndValue("pickListColumns", "string2"));

        assertEquals("Admin", getSelectedOptionText(Locator.tagWithName("select", "permissions").index(1)));

        /** inherit from Folder_A - test for 'OK' to inherit **/

        checkRadioButton(Locator.radioButtonByNameAndValue("inheritFromContainer", "InheritFromSpecificContainer"));
        setFormElement(Locator.name("inheritFromContainerSelect"), pathToA);


        doAndWaitForPageToLoad(() ->
        {
            click(Locator.linkWithText("Update"));
            assertAlert("Custom Fields of current folder will get overridden.");
        });

        //check if all the inherited fields are populated and are disabled.
        assertEquals("Bugs", getFormElement(Locator.name("type")));
        WebElement inputType = Locator.input("type").findElement(getDriver());
        assertFalse(inputType.getText() + " should be disabled.", inputType.isEnabled());

        assertEquals("Dev Area", getFormElement(Locator.name("area")));
        WebElement inputArea = Locator.input("type").findElement(getDriver());
        assertFalse(inputArea.getText() + " should be disabled.", inputArea.isEnabled());

        assertEquals("Prioritah", getFormElement(Locator.name("priority")));
        WebElement inputPri = Locator.input("type").findElement(getDriver());
        assertFalse(inputPri.getText() + " should be disabled.", inputPri.isEnabled());

        assertEquals("Contract Number", getFormElement(Locator.name("int1")));
        WebElement inputInt1 = Locator.input("type").findElement(getDriver());
        assertFalse(inputInt1.getText() + " should be disabled.", inputInt1.isEnabled());

        assertEquals("Development Sprint", getFormElement(Locator.name("string1")));
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
        assertEquals("Folder_B_Str2", getFormElement(Locator.name("string2")));
        WebElement inputString2 = Locator.input("string2").findElement(getDriver());
        assertTrue(inputString2.getText() + " is not enabled.", inputString2.isEnabled());

        //check if non-inherited options are modifiable
        //addKeyword("string2", "Folder_B_Str2", "B_4");
        //addLookupValues();
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

        assertEquals("milestone_A", getFormElement(Locator.name("milestone")));
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
        assertNotEquals(pathToB, getFormElement(Locator.name("inheritFromContainerSelect")));

        /***** End: Test for chaining - which is disallowed *****/

    }

    // NOTE: returning string here to avoid extra casting
    public String getIssueId()
    {
        String title = getDriver().getTitle();
        return title.substring(title.indexOf(' '), title.indexOf(':')).trim();
    }
}
