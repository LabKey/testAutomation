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

package org.labkey.test.tests.issues;

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
import org.labkey.test.pages.issues.ClosePage;
import org.labkey.test.pages.issues.DetailsPage;
import org.labkey.test.pages.issues.EmailPrefsPage;
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

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.PasswordUtil.getUsername;

@Category({DailyA.class, Data.class})
public class IssuesTest extends BaseWebDriverTest
{
    private static final String ISSUE_TITLE_0 = "A very serious issue";
    private static final String ISSUE_TITLE_1 = "Even more serious issue";
    private static final String USER1 = "user1_issuetest@issues.test";
    private static final String USER2 = "user2_issuetest@issues.test";
    private static final String USER3 = "user3_issuetest@issues.test";
    private final String NAME = displayNameFromEmail(getUsername());
    private final Map<String, String> ISSUE_0 = Maps.of("assignedTo", NAME, "title", ISSUE_TITLE_0, "priority", "2", "comment", "a bright flash of light");
    private final Map<String, String> ISSUE_1 = Maps.of("assignedTo", NAME, "title", ISSUE_TITLE_1, "priority", "1", "comment", "alien autopsy");

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
    private ApiPermissionsHelper _permissionsHelper = new ApiPermissionsHelper(this);

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

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteUsersIfPresent(USER1, USER2);
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        IssuesTest initTest = (IssuesTest)getCurrentTest();
        initTest.doInit();
    }

    public void doInit()
    {
        _containerHelper.createProject(getProjectName(), null);
        _permissionsHelper.createPermissionsGroup(TEST_GROUP);
        _permissionsHelper.assertPermissionSetting(TEST_GROUP, "No Permissions");
        _permissionsHelper.setPermissions(TEST_GROUP, "Editor");

        _issuesHelper.createNewIssuesList("issues", _containerHelper);

        // Add to group so user appears
        _userHelper.createUser(USER1);
        _userHelper.createUser(USER2);
        _userHelper.createUser(USER3);
        _permissionsHelper.addUserToProjGroup(getUsername(), getProjectName(), TEST_GROUP);
        _permissionsHelper.addUserToProjGroup(USER1, getProjectName(), TEST_GROUP);
        _permissionsHelper.addUserToProjGroup(USER3, getProjectName(), TEST_GROUP);

        // Create issues
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("Issue Summary"));
        _issuesHelper.addIssue(ISSUE_0);
        _issuesHelper.addIssue(ISSUE_1);
    }

    @Before
    public void returnToProject()
    {
        enableEmailRecorder();
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("Issue Summary"));
        DataRegionTable issuesTable = new DataRegionTable("issues-issues", getDriver());

        // clear region selection and filters
        issuesTable.uncheckAll();
        issuesTable.clearAllFilters("IssueId");

        // reset folder filter
        _extHelper.clickMenuButton(true, "Grid Views", "Folder Filter", "Current folder");
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
        assertElementPresent(Locator.tagWithName("form", "login"));
        signIn();
        recallLocation();                          // and logged in again
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
            test.clickAndWait(Locator.linkWithText("view data"));
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

        assertElementPresent(Locators.labkeyError.withText(errorMessage));

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
    }

    @Test
    public void emailTest()
    {
        Map<String, String> ISSUE = Maps.of("assignedTo", displayNameFromEmail(USER1), "title", "A not so serious issue", "priority", "4", "comment", "No big whup", "notifyList", USER2);

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
        _issuesHelper.addIssue(ISSUE)
                .clickReturnToGrid();

        // need to make change that will message current admin
        clickAndWait(Locator.linkWithText(ISSUE.get("title")));
        updateIssue();
        setFormElement(Locator.name("comment"), "Sup with this issue!");
        clickButton("Save");
        clickAndWait(Locator.linkWithText("return to grid"));

        //Issue 16238: From close issue screen: "Save" goes back to issue, "cancel" goes to issue list. This is the opposite of what I want
        log("verify cancelling returns to the same issue page");
        clickAndWait(Locator.linkWithText(ISSUE.get("title")));
        updateIssue();
        clickButton("Cancel");
        assertTitleContains(ISSUE.get("title"));

        goToModule("Dumbster");
        pushLocation();

        EmailRecordTable emailTable = new EmailRecordTable(this);
        EmailMessage message = emailTable.getMessage(ISSUE.get("title") + ",\" has been opened and assigned to " + displayNameFromEmail(USER1));

        // Presumed to get the first message
        List<String> recipients = emailTable.getColumnDataAsText("To");
        assertTrue("User did not receive issue notification", recipients.contains(getUsername()));
        assertTrue(USER1 + " did not receieve issue notification", recipients.contains(USER1));
        assertFalse(USER2 + " receieved issue notification without container read permission", recipients.contains(USER2));

        assertTrue("Issue Message does not contain title", message.getSubject().contains(ISSUE.get("title")));

        assertTextNotPresent("This line shouldn't appear");
    }

    private void updateIssue()
    {
        clickAndWait(Locator.linkWithText("update"));
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
                ISSUE_0.get("comment"),
                ISSUE_1.get("comment"));
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
        clickAndWait(Locator.linkWithText("Issue Summary"));
        // Set the container filter to include subfolders
        _extHelper.clickMenuButton(true, "Grid Views", "Folder Filter", "Current folder and subfolders");

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
        click(Locator.linkWithText("move"));

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

        DataRegionTable issuesTable = new DataRegionTable("issues-issues", getDriver());
        issuesTable.checkCheckboxByPrimaryKey(issueIdA);
        issuesTable.checkCheckboxByPrimaryKey(issueIdB);
        click(Locator.linkWithText("move"));

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

        _issuesHelper.addIssue(Maps.of("assignedTo", NAME, "title", "A is for Apple"));
        String issueIdA = getIssueId();

        _issuesHelper.addIssue(Maps.of("assignedTo", NAME, "title", "B is for Baking"));
        String issueIdB = getIssueId();

        _issuesHelper.addIssue(Maps.of("assignedTo", NAME, "title", "C is for Cat"));
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
        _userHelper.createUser(user);
        _permissionsHelper.createPermissionsGroup("Readers", user);
        _permissionsHelper.setPermissions("Readers", "Reader");

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
        clickAndWait(Locator.linkWithText("Issue Summary"));
        _issuesHelper.goToAdmin();
        _issuesHelper.setIssueAssignmentUser(displayNameFromEmail(deletedUser));
        clickButton("Save");

        // taking care of some clean-up while here for the test.
        _userHelper.deleteUsers(true, deletedUser, user);

        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText("Issue Summary"));
        clickButton("New Issue");
        // NPE
        //clickButton("Cancel");

        // TODO: extend test to check validate full user selection list based on group selection...
        // TODO: compare user dropdown list between admin and new issues page
    }

    @Test
    public void testAssignedToOnResolveAndClose() throws Exception
    {
        final String title = "assignmentTest";
        final String openTo = displayNameFromEmail(USER1);
        final String updateTo = displayNameFromEmail(USER3);
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
        String id = urlParameters.get("issueId");
        return id;
    }
}
