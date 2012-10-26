/*
 * Copyright (c) 2008-2012 LabKey Corporation
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
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.EmailRecordTable;
import org.labkey.test.util.EmailRecordTable.EmailMessage;
import org.labkey.test.util.PasswordUtil;
import java.util.List;

/**
 * User: tamram
 * Date: May 15, 2006
 */
public class IssuesTest extends BaseSeleniumWebTest
{
    protected static final String PROJECT_NAME = "IssuesVerifyProject";
    protected static final String SUB_FOLDER_NAME = "SubFolder";
    private static final String ISSUE_TITLE_0 = "A very serious issue";
    private static final String ISSUE_TITLE_1 = "Even more serious issue";
    private static final String ISSUE_TITLE_2 = "A not so serious issue";
    private static final String ISSUE_TITLE_3 = "A sub-folder issue";
    private static final String USER1 = "user1_issuetest@issues.test";
    private static final String USER2 = "user2_issuetest@issues.test";
    private static final String USER3 = "user3_issuetest@issues.test";
    private static final String EMAILRECORD_TABLE = "dataregion_EmailRecord";

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

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/issues";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected void doCleanup(boolean afterTest)
    {
        deleteUser(USER1);
        deleteUser(USER2);
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {/* */}
    }

    public void validateQueries()
    {
        // TODO: Fix broken query validation
    }

    protected void initProject()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        createPermissionsGroup(TEST_GROUP);
        assertPermissionSetting(TEST_GROUP, "No Permissions");
        setPermissions(TEST_GROUP, "Editor");
        clickButton("Save and Finish");

        enableModule(PROJECT_NAME, "Dumbster");

        clickLinkWithText(PROJECT_NAME);
        addWebPart("Issues Summary");
        addWebPart("Search");
        assertTextPresent("Open");

        _containerHelper.createSubfolder(PROJECT_NAME, SUB_FOLDER_NAME, null);
        addWebPart("Issues List");

        enableEmailRecorder();
    }

    protected void doTestSteps()
    {
        initProject();

        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("view open Issues");
        assertNavButtonPresent("New Issue");

        // quick security test
        // TODO push lots of locations as we go and move this test to end
        pushLocation();
        pushLocation();
        signOut();
        popLocation();                          // try open issues as guest
        assertNavButtonNotPresent("New Issue");
        assertFormPresent("login");
        signIn();
        popLocation();                          // and logged in again
        assertNavButtonPresent("New Issue");

        // AdminAction
        clickButton("Admin");

        // AddKeywordAction
        addKeywordsAndVerify("area", "Area", "Area51", "Fremont", "Downtown");
        addKeywordsAndVerify("type", "Type", "UFO", "SPEC", "TODO", "AAA");

        //SetKeywordDefaultAction
        clickLinkWithText("set");
        // check that AAA is bold and [clear] link is on that row
        assertElementContains(Locator.xpath("id('formtype')/table/tbody/tr[1]/td[1]/b"), "AAA");
        assertElementContains(Locator.xpath("id('formtype')/table/tbody/tr[1]/td[2]/a[2]"), "clear");
        //SetKeywordDefaultAction
        clickLinkWithText("clear");
        // check that AAA is not bold and [set] link is now on that row
        assertElementNotPresent(Locator.xpath("id('formtype')/table/tbody/tr[1]/td[1]/b"));
        assertElementContains(Locator.xpath("id('formtype')/table/tbody/tr[1]/td[2]/a[2]"), "set");
        clickLinkWithText("delete");
        assertTextNotPresent("AAA");

        // Check that non-integer priority results in an error message
        addKeyword("priority", "Priority", "ABC");
        assertTextPresent("Priority must be an integer");
        assertTextNotPresent("ABC");
        addKeyword("priority", "Priority", "1.2");
        assertTextPresent("Priority must be an integer");
        assertTextNotPresent("1.2");

        // SetCustomColumnConfigurationAction
        setText("int1", "MyInteger");
        setText("int2", "MySecondInteger");
        setText("string1", "MyFirstString");
        // Omit string2 to test using it in email template.
        setText("string3", "MyThirdString");
        setText("string4", "MyFourthString");
        setText("string5", "MyFifthString");
        checkCheckbox("pickListColumns", "string1");
        checkCheckbox("pickListColumns", "string5");
        clickButton("Update");

        // AddKeywordAction
        addKeywordsAndVerify("milestone", "Milestone", "2012", "2013");
        addKeywordsAndVerify("string1", "MyFirstString", "North", "South");
        addKeywordsAndVerify("string5", "MyFifthString", "Cadmium", "Polonium");

        // UpdateRequiredFieldsAction
        checkCheckbox("requiredFields", "Milestone");
        checkCheckbox("requiredFields", "String4");
        checkCheckbox("requiredFields", "String5");
        clickButton("Update");

        // ListAction (empty)
        clickButton("Back to Issues");

        // InsertAction -- user isn't in any groups, so shouldn't appear in the assigned-to list yet
        clickButton("New Issue");
        String assignedToText = getText(Locator.xpath("//select[@name='assignedTo']"));
        Assert.assertEquals(assignedToText, "");

        // Add to group so user appears
        clickLinkWithText("IssuesVerifyProject");
        addUserToProjGroup(PasswordUtil.getUsername(), PROJECT_NAME, TEST_GROUP);
        clickLinkWithText("IssuesVerifyProject");
        clickLinkWithText("view open Issues");

        // InsertAction
        clickButton("New Issue");
        assignedToText = getText(Locator.xpath("//select[@name='assignedTo']"));
        Assert.assertEquals(assignedToText, getDisplayName());
        String customStringText = getText(Locator.xpath("//select[@name='string5']"));
        Assert.assertEquals(customStringText, "Cadmium Polonium");
        setFormElement("title", ISSUE_TITLE_0);
        selectOptionByText("type", "UFO");
        selectOptionByText("area", "Area51");
        selectOptionByText("priority", "2");
        setFormElement("comment", "a bright flash of light");
        clickButton("Save");

        // test validate
        assertTextPresent("Field AssignedTo cannot be blank");
        selectOptionByText("assignedTo", getDisplayName());
        clickButton("Save");
        assertTextPresent("Field Milestone cannot be blank");
        selectOptionByText("milestone", "2012");
        clickButton("Save");
        assertTextPresent("Field MyFourthString cannot be blank");
        setFormElement("string4", "http://www.issues.test");
        clickButton("Save");
        assertTextPresent("Field MyFifthString cannot be blank");
        selectOptionByText("string5", "Polonium");
        clickButton("Save");

        // find issueId - parse the text from first space to :
        String title = getLastPageTitle();
        title = title.substring(title.indexOf(' '), title.indexOf(':')).trim();
        int issueId = Integer.parseInt(title);

        // DetailsAction
        assertTextPresent("Issue " + issueId + ": " + ISSUE_TITLE_0);
        assertTextPresent("Milestone", "MyInteger", "MySecondInteger", "MyFirstString", "MyThirdString", "MyFourthString", "MyFifthString");
        assertTextNotPresent("MySecondString");
        assertLinkPresentWithText("http://www.issues.test");

        // ListAction
        clickLinkWithText("return to grid");

        //Click the issue id based on the text issue title
        clickLinkWithText("" + issueId);

        // UpdateAction
        clickLinkWithText("update");
        setFormElement("comment", "don't believe the hype");
        clickButton("Save");
        searchFor(PROJECT_NAME, "2012", 1, ISSUE_TITLE_0);

        // ResolveAction
        clickLinkWithText("resolve");
        clickButton("Save");

        // ReopenAction
        clickLinkWithText("reopen");
        clickButton("Save");

        // ResolveAction
        clickLinkWithText("resolve");
        clickButton("Save");

        // CloseAction
        clickLinkWithText("close");
        clickButton("Save");

        // Test .lastFilter
        testLastFilter(issueId);

        // JumpToIssueAction
        setText("issueId", "" + issueId);
        submit(Locator.formWithName("jumpToIssue"));
        assertTextPresent(ISSUE_TITLE_0);
        assertTextNotPresent("Invalid");

        // SearchAction
        clickLinkWithText("return to grid");
        pushLocation();
        String index = getContextPath() + "/search/" + PROJECT_NAME + "/index.view?wait=1";
        log(index);
        beginAt(index, 5*defaultWaitForPage);
        popLocation();
        // UNDONE: test grid search box

        // SearchWebPart
        searchFor(PROJECT_NAME, "hype", 1, ISSUE_TITLE_0);
        // SearchWebPart
        searchFor(PROJECT_NAME, "2012", 1, ISSUE_TITLE_0);

        queryTest();

        // back to grid view
        clickLinkWithText("Issues Summary");

        requiredFieldsTest();
        viewSelectedDetailsTest();
        entryTypeNameTest();

        // Test issues grid with issues in a sub-folder
        subFolderIssuesTest();

        emailTest();//todo: move down

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
    private static void addKeyword(BaseSeleniumWebTest test, String fieldName, String caption, String value)
    {
        test.setFormElement(Locator.formElement("add" + fieldName, "keyword"), value);
        test.clickButton("Add " + caption);
    }

    // Add new keyword(s) to the given field and verify they were added without error.  Need to be on the issues admin page already.
    public static void addKeywordsAndVerify(BaseSeleniumWebTest test, String fieldName, String caption, String... values)
    {
        for (String value : values)
        {
            addKeyword(test, fieldName, caption, value);
            test.assertNoLabkeyErrors();
            test.assertTextPresent(value);
        }
    }

    private void emailTest()
    {
        log("Test notification emails");

        addUserToProjGroup(USER1, PROJECT_NAME, TEST_GROUP);
        createUser(USER2, null, false);

        clickLinkWithText(PROJECT_NAME);
        goToModule("Dumbster");
        assertTextPresent("No email recorded.");

        // CustomizeEmailAction 
        goToModule("Issues");
        clickButton("Admin");
        clickButton("Customize Email Template");
        String subject = getFormElement("emailSubject");
        setFormElement("emailMessage", TEST_EMAIL_TEMPLATE_BAD);
        clickButton("Save");
        assertTextPresent("Invalid template");
        setFormElement("emailMessage", TEST_EMAIL_TEMPLATE);
        clickButton("Save");
        assertTextNotPresent("Invalid template");
        assertFormElementEquals("emailSubject", subject); // regression check for issue #11389
        goToModule("Portal");

       // EmailPrefsAction
        clickLinkWithText("Issues Summary");
        clickButton("Email Preferences");
        checkCheckbox("emailPreference", "8"); // self enter/edit an issue
        clickButton("Update");

        impersonate(USER1);
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Issues Summary");
        clickButton("Email Preferences");
        uncheckCheckbox("emailPreference", "2"); // issue assigned to me is modified
        clickButton("Update");
        stopImpersonating();

        enableEmailRecorder();
        clickLinkWithText(PROJECT_NAME);

        clickLinkWithText("Issues Summary");
        clickButton("New Issue");
        setFormElement("title", ISSUE_TITLE_2);
        selectOptionByText("assignedTo", displayNameFromEmail(USER1));
        selectOptionByText("priority", "4");
        selectOptionByText("milestone", "2012");
        setFormElement("notifyList", USER2);
        setFormElement("comment", "No big whup");
        setFormElement("string4", "http://www.issues2.test");
        selectOptionByText("string5", "Cadmium");
        clickButton("Save");

        goToModule("Dumbster");
        pushLocation();

        EmailRecordTable emailTable = new EmailRecordTable(this);
        EmailMessage message = emailTable.getMessage(ISSUE_TITLE_2 + ",\" has been opened and assigned to " + displayNameFromEmail(USER1));

        // Presumed to get the first message
        List<String> recipients = emailTable.getColumnDataAsText("To");
        Assert.assertTrue("User did not receive issue notification",      recipients.contains(PasswordUtil.getUsername()));
        Assert.assertTrue(USER2 + " did not receieve issue notification", recipients.contains(USER2));
        Assert.assertTrue(USER1 + " did not receieve issue notification", recipients.contains(USER1));

        Assert.assertTrue("Issue Message does not contain title", message.getSubject().contains(ISSUE_TITLE_2));

        assertTextPresent("Customized template line: Cadmium", 3);
        assertTextNotPresent("This line shouldn't appear");

        impersonate(USER1);
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Issues Summary");
        clickLinkWithText(ISSUE_TITLE_2);
        clickLinkWithText("update");
        selectOptionByText("priority", "0");
        setFormElement("notifyList", USER3);
        setFormElement("comment", "Oh Noez!");
        clickButton("Save");
        stopImpersonating();

        popLocation();

        emailTable = new EmailRecordTable(this);
        message = emailTable.getMessage(ISSUE_TITLE_2 + ",\" has been updated");

        Assert.assertTrue(USER3 + " did not receieve updated issue notification" + message.getTo()[0],
                USER3.equals(emailTable.getDataAsText(0, "To")) || USER3.equals(emailTable.getDataAsText(1, "To")));
        Assert.assertTrue("User did not receive updated issue notification",
                PasswordUtil.getUsername().equals(emailTable.getDataAsText(0, "To")) || PasswordUtil.getUsername().equals(emailTable.getDataAsText(1, "To")));
    }

    private void entryTypeNameTest()
    {
        goToModule("Issues");
        clickButton("Admin");
        setFormElement(Locator.formElement("entryTypeNames", "entrySingularName"), "Ticket");
        setFormElement(Locator.formElement("entryTypeNames", "entryPluralName"), "Tickets");
        clickButton("Update");

        assertFormElementEquals("entrySingularName", "Ticket");
        assertFormElementEquals("entryPluralName", "Tickets");

        assertTextPresent("Tickets Admin Page");
        clickLinkWithText("Back to Tickets");

        assertTextPresent("Tickets List");
        assertTextNotPresent("Issues List");
        assertNavButtonPresent("New Ticket");
        assertNavButtonPresent("Jump to Ticket");
        assertTextPresent("Ticket ID");
        assertTextNotPresent("Issue ID");

        clickButton("Admin");
        setFormElement(Locator.formElement("entryTypeNames", "entrySingularName"), "Issue");
        setFormElement(Locator.formElement("entryTypeNames", "entryPluralName"), "Issues");
        clickButton("Update");
    }

    private void requiredFieldsTest()
    {
        goToModule("Issues");
        clickButton("Admin");
        setFormElement("int1", "Contract Number");
        setFormElement("string1", "Customer Name");

        for (String field : REQUIRED_FIELDS)
            checkRequiredField(field, true);

        clickButton("Update");
        clickButton("Back to Issues");
        clickButton("Admin");

        //setWorkingForm("requiredFieldsForm");
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
    }

    private void checkRequiredField(String name, boolean select)
    {
        Locator checkBoxLocator = Locator.checkboxByNameAndValue("requiredFields", name);

        if (select)
            checkCheckbox("requiredFields", name);
        else
        {
            if (isChecked(checkBoxLocator))
                click(checkBoxLocator);
        }
    }

    private void verifyFieldChecked(String fieldName)
    {
        if (isChecked(Locator.checkboxByNameAndValue("requiredFields", fieldName)))
            return;

        Assert.assertFalse("Checkbox not set for element: " + fieldName, false);
    }

    private void viewSelectedDetailsTest()
    {
        setFilter("Issues", "Status", "Has Any Value");
        clickCheckbox(".toggle");
        clickButton("View Details");
        assertTextPresent("a bright flash of light");
        assertTextPresent("don't believe the hype");
        clickLinkWithText("view grid");
    }

    public void testLastFilter(int issueId)
    {
        log("Testing .lastFilter");

        // insert a new issue
        clickLinkWithText("new issue");
        setFormElement("title", ISSUE_TITLE_1);
        selectOptionByText("type", "UFO");
        selectOptionByText("area", "Area51");
        selectOptionByText("priority", "1");
        setFormElement("comment", "alien autopsy");
        selectOptionByText("milestone", "2013");
        selectOptionByText("assignedTo", getDisplayName());
        setFormElement("string4", "http://www.issues2.test");
        selectOptionByText("string5", "Cadmium");
        clickButton("Save");

        // assert both issues are present
        clickLinkWithText("return to grid");
        clearAllFilters("Issues", "IssueId");
        assertTextPresent(ISSUE_TITLE_0);
        assertTextPresent(ISSUE_TITLE_1);

        // Filter out all pri-1 bugs; assert newly created issue is filtered out
        setFilter("Issues", "Priority", "Does Not Equal", "1");
        assertTextPresent(ISSUE_TITLE_0);
        assertTextNotPresent(ISSUE_TITLE_1);

        // view an issue
        clickLinkWithText(String.valueOf(issueId));

        // assert .lastFilter is applied
        clickLinkWithText("return to grid");
        assertTextPresent(ISSUE_TITLE_0);
        assertTextNotPresent(ISSUE_TITLE_1);

        clearAllFilters("Issues", "IssueId");
    }

    protected void queryTest()
    {
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Query");
        selenium.select("//select[@name='schemaName']", "issues");
//        setFormElement("schemaName", "issues");
        submit();
        clickLinkWithText("Issues Queries");
        createNewQuery("issues");
        setFormElement("ff_newQueryName", "xxyzzy");
        clickButton("Create and Edit Source");
        _extHelper.clickExtTab("Data");
        waitForText(ISSUE_TITLE_0, WAIT_FOR_JAVASCRIPT);
        waitForText(ISSUE_TITLE_1, WAIT_FOR_JAVASCRIPT);
        clickLinkWithText(PROJECT_NAME);
    }

    public void subFolderIssuesTest()
    {
        log("Testing issues in sub-folders");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(SUB_FOLDER_NAME);

        //Issue 15550: Better tests for view details, admin, and email preferences
        for(String button : new String[] {"Admin", "Email Preferences"})
        {
            Locator l = Locator.xpath("//span/a[span[text()='" + button + "']]");
            Assert.assertTrue(getAttribute(l,  "href").contains(PROJECT_NAME + "/" + SUB_FOLDER_NAME));

        }

        clickButton("New Issue");
        setFormElement("title", ISSUE_TITLE_3);
        selectOptionByText("assignedTo", getDisplayName());
        setFormElement("comment", "We are in a sub-folder");
        clickButton("Save");

        clickLinkContainingText(PROJECT_NAME);
        clickLinkWithText("Issues Summary");
        // Set the container filter to include subfolders
        clickMenuButton("Views", "Folder Filter", "Current folder and subfolders");

        // Verify the URL of ISSUE_TITLE_0 goes to PROJECT_NAME
        String href = getAttribute(Locator.linkContainingText(ISSUE_TITLE_0), "href");
        Assert.assertTrue("Expected issue details URL to link to project container", href.contains("/issues/" + PROJECT_NAME + "/details.view"));

        // Verify the URL of ISSUE_TITLE_3 goes to PROJECT_NAME/SUB_FOLDER_NAME
        href = getAttribute(Locator.linkContainingText(ISSUE_TITLE_3), "href");
        Assert.assertTrue("Expected issue details URL to link to sub-folder container", href.contains("/issues/" + PROJECT_NAME + "/" + SUB_FOLDER_NAME + "/details.view"));
    }

}
