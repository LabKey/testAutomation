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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.EmailRecordTable;
import org.labkey.test.util.EmailRecordTable.EmailMessage;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.PasswordUtil;

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
    private static final String USER1 = "user1@issues.test";
    private static final String USER2 = "user2@issues.test";
    private static final String USER3 = "user3@issues.test";
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

    protected void doCleanup()
    {
        deleteUser(USER1);
        deleteUser(USER2);
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {/* */}
    }

    protected void initProject()
    {
        createProject(PROJECT_NAME);
        createPermissionsGroup(TEST_GROUP);
        assertPermissionSetting(TEST_GROUP, "No Permissions");
        setPermissions(TEST_GROUP, "Editor");
        clickNavButton("Save and Finish");

        enableModule(PROJECT_NAME, "Dumbster");

        clickLinkWithText(PROJECT_NAME);
        addWebPart("Issues Summary");
        addWebPart("Search");
        assertTextPresent("Open");

        createSubfolder(PROJECT_NAME, SUB_FOLDER_NAME, null);
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
        clickNavButton("Admin");

        // AddKeywordAction
        setFormElement(Locator.formElement("addArea", "keyword"), "Area51");
        clickNavButton("Add Area");
        assertTextPresent("Area51");
        setFormElement(Locator.formElement("addArea", "keyword"), "Fremont");
        clickNavButton("Add Area");
        assertTextPresent("Fremont");
        setFormElement(Locator.formElement("addArea", "keyword"), "Downtown");
        clickNavButton("Add Area");
        assertTextPresent("Downtown");

        // AddKeywordAction
        setFormElement(Locator.formElement("addType", "keyword"), "UFO");
        clickNavButton("Add Type");
        assertTextPresent("UFO");
        setFormElement(Locator.formElement("addType", "keyword"), "SPEC");
        clickNavButton("Add Type");
        assertTextPresent("SPEC");
        setFormElement(Locator.formElement("addType", "keyword"), "TODO");
        clickNavButton("Add Type");
        assertTextPresent("TODO");
        setFormElement(Locator.formElement("addType", "keyword"), "AAA");
        clickNavButton("Add Type");

        assertTextPresent("AAA");
        //SetKeywordDefaultAction
        clickLinkWithText("set");
        // check that AAA is bold and [clear] link is on that row
        assertElementContains(Locator.xpath("id('formTypes')/table/tbody/tr[1]/td[1]/b"), "AAA");
        assertElementContains(Locator.xpath("id('formTypes')/table/tbody/tr[1]/td[2]/a[2]"), "clear");
        //SetKeywordDefaultAction
        clickLinkWithText("clear");
        // check that AAA is not bold and [set] link is now on that row
        assertElementNotPresent(Locator.xpath("id('formTypes')/table/tbody/tr[1]/td[1]/b"));
        assertElementContains(Locator.xpath("id('formTypes')/table/tbody/tr[1]/td[2]/a[2]"), "set");
        clickLinkWithText("delete");
        assertTextNotPresent("AAA");

        // Check that non-integer priority results in an error message
        setFormElement(Locator.formElement("addPriority", "keyword"), "ABC");
        clickNavButton("Add Priority");
        assertTextPresent("Priority must be an integer");
        assertTextNotPresent("ABC");
        setFormElement(Locator.formElement("addPriority", "keyword"), "1.2");
        clickNavButton("Add Priority");
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
        clickNavButton("Update");

        // AddKeywordAction
        setFormElement(Locator.formElement("addMilestone", "keyword"), "2012");
        clickNavButton("Add Milestone");
        assertTextPresent("2012");
        setFormElement(Locator.formElement("addMilestone", "keyword"), "2013");
        clickNavButton("Add Milestone");
        assertTextPresent("2013");
        setFormElement(Locator.formElement("addMyFirstString", "keyword"), "North");
        clickNavButton("Add MyFirstString");
        assertTextPresent("North");
        setFormElement(Locator.formElement("addMyFirstString", "keyword"), "South");
        clickNavButton("Add MyFirstString");
        assertTextPresent("South");
        setFormElement(Locator.formElement("addMyFifthString", "keyword"), "Cadmium");
        clickNavButton("Add MyFifthString");
        assertTextPresent("Cadmium");
        setFormElement(Locator.formElement("addMyFifthString", "keyword"), "Polonium");
        clickNavButton("Add MyFifthString");
        assertTextPresent("Polonium");

        // UpdateRequiredFieldsAction
        checkCheckbox("requiredFields", "Milestone");
        checkCheckbox("requiredFields", "String4");
        checkCheckbox("requiredFields", "String5");
        clickNavButton("Update");

        // ListAction (empty)
        clickNavButton("Back to Issues");

        // InsertAction -- user isn't in any groups, so shouldn't appear in the assigned-to list yet
        clickNavButton("New Issue");
        String assignedToText = getText(Locator.xpath("//select[@name='assignedTo']"));
        assertEquals(assignedToText, "");

        // Add to group so user appears
        clickLinkWithText("IssuesVerifyProject");
        addUserToProjGroup(PasswordUtil.getUsername(), PROJECT_NAME, TEST_GROUP);
        clickLinkWithText("IssuesVerifyProject");
        clickLinkWithText("view open Issues");

        // InsertAction
        clickNavButton("New Issue");
        assignedToText = getText(Locator.xpath("//select[@name='assignedTo']"));
        assertEquals(assignedToText, getDisplayName());
        String customStringText = getText(Locator.xpath("//select[@name='string5']"));
        assertEquals(customStringText, "Cadmium Polonium");
        setFormElement("title", ISSUE_TITLE_0);
        selectOptionByText("type", "UFO");
        selectOptionByText("area", "Area51");
        selectOptionByText("priority", "2");
        setFormElement("comment", "a bright flash of light");
        clickNavButton("Save");

        // test validate
        assertTextPresent("Field AssignedTo cannot be blank");
        selectOptionByText("assignedTo", getDisplayName());
        clickNavButton("Save");
        assertTextPresent("Field Milestone cannot be blank");
        selectOptionByText("milestone", "2012");
        clickNavButton("Save");
        assertTextPresent("Field MyFourthString cannot be blank");
        setFormElement("string4", "http://www.issues.test");
        clickNavButton("Save");
        assertTextPresent("Field MyFifthString cannot be blank");
        selectOptionByText("string5", "Polonium");
        clickNavButton("Save");

        // find issueId - parse the text from first space to :
        String title = getLastPageTitle();
        title = title.substring(title.indexOf(' '), title.indexOf(':')).trim();
        int issueId = Integer.parseInt(title);

        // DetailsAction
        assertTextPresent("Issue " + issueId + ": " + ISSUE_TITLE_0);
        assertTextPresent("Milestone");
        assertTextPresent("MyInteger");
        assertTextPresent("MySecondInteger");
        assertTextPresent("MyFirstString");
        assertTextNotPresent("MySecondString");
        assertTextPresent("MyThirdString");
        assertTextPresent("MyFourthString");
        assertTextPresent("MyFifthString");
        assertLinkPresentWithText("http://www.issues.test");

        // ListAction
        clickLinkWithText("return to grid");

        //Click the issue id based on the text issue title
        clickLinkWithText("" + issueId);

        // UpdateAction
        clickLinkWithText("update");
        setFormElement("comment", "don't believe the hype");
        clickNavButton("Save");
        searchFor(PROJECT_NAME, "2012", 1, ISSUE_TITLE_0);

        // ResolveAction
        clickLinkWithText("resolve");
        clickNavButton("Save");

        // ReopenAction
        clickLinkWithText("reopen");
        clickNavButton("Save");

        // ResolveAction
        clickLinkWithText("resolve");
        clickNavButton("Save");

        // CloseAction
        clickLinkWithText("close");
        clickNavButton("Save");

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

        emailTest();//todo: move down
//        requiredFieldsTest();
//        viewSelectedDetailsTest();
//        entryTypeNameTest();

        // Test issues grid with issues in a sub-folder
        testSubFolderIssues();


        // UNDONE test these actions
        // CompleteUserAction
        // ExportTsvAction
        // PurgeAction
        // RssAction
    }

    private void emailTest()
    {
        log("Test notification emails");

        addUserToProjGroup(USER1, PROJECT_NAME, TEST_GROUP);
        createUser(USER2, "", false);

        clickLinkWithText(PROJECT_NAME);
        goToModule("Dumbster");
        assertTextPresent("No email recorded.");

        // CustomizeEmailAction 
        goToModule("Issues");
        clickNavButton("Admin");
        clickNavButton("Customize Email Template");
        String subject = getFormElement("emailSubject");
        setFormElement("emailMessage", TEST_EMAIL_TEMPLATE_BAD);
        clickNavButton("Save");
        assertTextPresent("Invalid template");
        setFormElement("emailMessage", TEST_EMAIL_TEMPLATE);
        clickNavButton("Save");
        assertTextNotPresent("Invalid template");
        assertFormElementEquals("emailSubject", subject); // regression check for issue #11389
        goToModule("Portal");

       // EmailPrefsAction
        clickLinkWithText("Issues Summary");
        clickNavButton("Email Preferences");
        checkCheckbox("emailPreference", "8"); // self enter/edit an issue
        clickNavButton("Update");

        impersonate(USER1);
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Issues Summary");
        clickNavButton("Email Preferences");
        uncheckCheckbox("emailPreference", "2"); // issue assigned to me is modified
        clickNavButton("Update");
        stopImpersonating();

        enableEmailRecorder();
        clickLinkWithText(PROJECT_NAME);

        clickLinkWithText("Issues Summary");
        clickNavButton("New Issue");
        setFormElement("title", ISSUE_TITLE_2);
        selectOptionByText("assignedTo", USER1);
        selectOptionByText("priority", "4");
        selectOptionByText("milestone", "2012");
        setFormElement("notifyList", USER2);
        setFormElement("comment", "No big whup");
        setFormElement("string4", "http://www.issues2.test");
        selectOptionByText("string5", "Cadmium");
        clickNavButton("Save");

        goToModule("Dumbster");
        pushLocation();

        EmailRecordTable emailTable = new EmailRecordTable(this);
        EmailMessage message = emailTable.getMessage(ISSUE_TITLE_2 + ",\" has been opened and assigned to " + USER1);

        // Presumed to get the first message
        assertTrue("User did not receive issue notification",      PasswordUtil.getUsername().equals(message.getTo()[0]));
        assertTrue(USER2 + " did not receieve issue notification", USER2.equals(emailTable.getDataAsText(1, "To")));
        assertTrue(USER1 + " did not receieve issue notification", USER1.equals(emailTable.getDataAsText(2, "To")));

        assertTrue("Issue Message does not contain title", message.getSubject().contains(ISSUE_TITLE_2));

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
        clickNavButton("Save");
        stopImpersonating();

        popLocation();

        emailTable = new EmailRecordTable(this);
        message = emailTable.getMessage(ISSUE_TITLE_2 + ",\" has been updated");

        assertTrue(USER3 + " did not receieve updated issue notification", USER3.equals(message.getTo()[0]));
        assertTrue("User did not receive updated issue notification",      PasswordUtil.getUsername().equals(emailTable.getDataAsText(1, "To")));
    }

    private void entryTypeNameTest()
    {
        goToModule("Issues");
        clickNavButton("Admin");
        setFormElement(Locator.formElement("entryTypeNames", "entrySingularName"), "Ticket");
        setFormElement(Locator.formElement("entryTypeNames", "entryPluralName"), "Tickets");
        clickNavButton("Update Entry Type Names");

        assertFormElementEquals("entrySingularName", "Ticket");
        assertFormElementEquals("entryPluralName", "Tickets");

        assertTextPresent("Tickets Admin Page");
        clickLinkWithText("Tickets List");

        assertTextPresent("Tickets List");
        assertTextNotPresent("Issues List");
        assertNavButtonPresent("New Ticket");
        assertNavButtonPresent("Jump to Ticket");
        assertTextPresent("Ticket ID");
        assertTextNotPresent("Issue ID");

        clickNavButton("Admin");
        setFormElement(Locator.formElement("entryTypeNames", "entrySingularName"), "Issue");
        setFormElement(Locator.formElement("entryTypeNames", "entryPluralName"), "Issues");
        clickNavButton("Update Entry Type Names");
    }

    private void requiredFieldsTest()
    {
        goToModule("Issues");
        clickNavButton("Admin");
        setFormElement("int1", "Contract Number");
        setFormElement("string1", "Customer Name");

        for (String field : REQUIRED_FIELDS)
            checkRequiredField(field, true);

        clickNavButton("Update");
        clickNavButton("Back to Issues");
        clickNavButton("Admin");

        //setWorkingForm("requiredFieldsForm");
        for (String field : REQUIRED_FIELDS)
        {
            verifyFieldChecked(field);
            checkRequiredField(field, false);
        }

        checkRequiredField("Title", true);
        clickNavButton("Update");
        clickNavButton("Back to Issues");
        clickNavButton("New Issue");
        clickNavButton("Save");

        assertTextPresent("Field Title cannot be blank.");
        clickNavButton("Cancel");
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

        assertFalse("Checkbox not set for element: " + fieldName, false);
    }

    private void viewSelectedDetailsTest()
    {
        setFilter("Issues", "Status", "Has Any Value");
        clickCheckbox(".toggle");
        clickNavButton("View Details");
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
        clickNavButton("Save");

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
        clickNavButton("Create and Edit Source");
        ExtHelper.clickExtTab(this, "Data");
        waitForText(ISSUE_TITLE_0, WAIT_FOR_JAVASCRIPT);
        waitForText(ISSUE_TITLE_1, WAIT_FOR_JAVASCRIPT);
        clickLinkWithText(PROJECT_NAME);
    }

    public void testSubFolderIssues()
    {
        log("Testing issues in sub-folders");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(SUB_FOLDER_NAME);

        clickNavButton("New Issue");
        setFormElement("title", ISSUE_TITLE_3);
        selectOptionByText("assignedTo", getDisplayName());
        setFormElement("comment", "We are in a sub-folder");
        clickNavButton("Save");

        clickLinkContainingText(PROJECT_NAME);
        clickLinkWithText("Issues Summary");
        // Set the container filter to include subfolders
        clickMenuButton("Views", "Folder Filter", "Current folder and subfolders");

        // Verify the URL of ISSUE_TITLE_0 goes to PROJECT_NAME
        String href = getAttribute(Locator.linkContainingText(ISSUE_TITLE_0), "href");
        assertTrue("Expected issue details URL to link to project container", href.contains("/issues/" + PROJECT_NAME + "/details.view"));

        // Verify the URL of ISSUE_TITLE_3 goes to PROJECT_NAME/SUB_FOLDER_NAME
        href = getAttribute(Locator.linkContainingText(ISSUE_TITLE_3), "href");
        assertTrue("Expected issue details URL to link to sub-folder container", href.contains("/issues/" + PROJECT_NAME + "/" + SUB_FOLDER_NAME + "/details.view"));
    }

}
