/*
 * Copyright (c) 2008-2010 LabKey Corporation
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

package org.labkey.test.drt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.PasswordUtil;

/**
 * User: tamram
 * Date: May 15, 2006
 */
public class IssuesTest extends BaseSeleniumWebTest
{
    protected static final String PROJECT_NAME = "IssuesVerifyProject";
    private static final String ISSUE_TITLE_0 = "A very serious issue";
    private static final String ISSUE_TITLE_1 = "Even more serious issue";
    private static final String ISSUE_TITLE_2 = "A not so serious issue";
    private static final String USER1 = "user1@issues.test";
    private static final String USER2 = "user2@issues.test";
    private static final String USER3 = "user3@issues.test";
    private static final String EMAILRECORD_TABLE = "dataregion_EmailRecord";

    private static final String[] REQUIRED_FIELDS = {"Title", "AssignedTo", "Type", "Area", "Priority", "Milestone",
                "NotifyList", "String1", "Int1"};
    private static final String TEST_GROUP = "testers";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/issues";
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

        log("Record emails");
        addWebPart("Mail Record");
        uncheckCheckbox("emailRecordOn");
        checkCheckbox("emailRecordOn");
    }

    protected void doTestSteps()
    {
        initProject();

        clickLinkWithText("view open Issues");
        assertNavButtonPresent("New Issue");

//        pushLocation();
//        clickNavButton("Email Preferences");
//        checkCheckbox("emailPreference", "8");
//        clickNavButton("Update");
//        popLocation();

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

        // AddKeywordAction
        setFormElement(Locator.formElement("addMilestone", "keyword"), "2012");
        clickNavButton("Add Milestone");
        assertTextPresent("2012");
        setFormElement(Locator.formElement("addMilestone", "keyword"), "2013");
        clickNavButton("Add Milestone");
        assertTextPresent("2013");

        // UpdateRequiredFieldsAction
        //         <tr><td><input type="checkbox" name="requiredFields"  value="Milestone">Milestone</td></tr>
        checkCheckbox("requiredFields", "Milestone");
        clickNavButton("Update Required Fields");

        // SetCustomColumnConfigurationAction
        setText("int1", "MyInteger");
        clickNavButton("Update Custom Fields");

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
        setFormElement("title", ISSUE_TITLE_0);
        selectOptionByText("type", "UFO");
        selectOptionByText("area", "Area51");
        selectOptionByText("priority", "2");
        setFormElement("comment", "a bright flash of light");
        clickNavButton("Submit");

        // test validate
        assertTextPresent("Field AssignedTo cannot be blank");
        selectOptionByText("assignedTo", getDisplayName());
        clickNavButton("Submit");
        assertTextPresent("Field Milestone cannot be blank");
        selectOptionByText("milestone", "2012");
        clickNavButton("Submit");

        // find issueId - parse the text from first space to :
        String title = getLastPageTitle();
        title = title.substring(title.indexOf(' '), title.indexOf(':')).trim();
        int issueId = Integer.parseInt(title);

        // DetailsAction
        assertTextPresent("Issue " + issueId + ": " + ISSUE_TITLE_0);
        assertTextPresent("Milestone");

        // ListAction
        clickLinkWithText("return to grid");

        //Click the issue id based on the text issue title
        //String xpath = "//td/a[text() = '" + ISSUE_TITLE_0 + "']/../../td[2]/a";
        //clickAndWait(Locator.xpath(xpath));
        clickLinkWithText("" + issueId);

        // UpdateAction
        clickLinkWithText("update");
        setFormElement("comment", "don't believe the hype");
        clickNavButton("Submit");
        searchFor(PROJECT_NAME, "2012", 1, ISSUE_TITLE_0);

        // ResolveAction
        clickLinkWithText("resolve");
        clickNavButton("Submit");

        // ReopenAction
        clickLinkWithText("reopen");
        clickNavButton("Submit");

        // ResolveAction
        clickLinkWithText("resolve");
        clickNavButton("Submit");

        // CloseAction
        clickLinkWithText("close");
        clickNavButton("Submit");

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
        assertTextPresent("No email recorded.");

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

        clickLinkWithText(PROJECT_NAME);
        uncheckCheckbox("emailRecordOn");
        checkCheckbox("emailRecordOn");

        clickLinkWithText("Issues Summary");
        clickNavButton("New Issue");
        setFormElement("title", ISSUE_TITLE_2);
        selectOptionByText("assignedTo", USER1);
        selectOptionByText("priority", "4");
        selectOptionByText("milestone", "2012");
        setFormElement("notifyList", USER2);
        setFormElement("comment", "No big whup");
        clickNavButton("Submit");

        clickLinkWithText(PROJECT_NAME);
        assertElementPresent(Locator.xpath("//table[@id='dataregion_EmailRecord']//td[text() = '" + PasswordUtil.getUsername() + "' and position() = '1']"));
        assertElementPresent(Locator.xpath("//table[@id='dataregion_EmailRecord']//td[text() = '" + USER1 + "' and position() = '1']"));
        assertElementPresent(Locator.xpath("//table[@id='dataregion_EmailRecord']//td[text() = '" + USER2 + "' and position() = '1']"));
        assertTableCellContains(EMAILRECORD_TABLE, 3, 3, ISSUE_TITLE_2);

        impersonate(USER1);
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Issues Summary");
        clickLinkWithText(ISSUE_TITLE_2);
        clickLinkWithText("update");
        selectOptionByText("priority", "0");
        setFormElement("notifyList", USER3);
        setFormElement("comment", "Oh Noez!");
        clickNavButton("Submit");

        clickLinkWithText(PROJECT_NAME);
        assertElementPresent(Locator.xpath("//table[@id='dataregion_EmailRecord']//tr[position() <= '5']/td[text() = '" + PasswordUtil.getUsername() + "' and position() = '1']"));
        assertElementPresent(Locator.xpath("//table[@id='dataregion_EmailRecord']//tr[position() <= '5']/td[text() = '" + USER3 + "' and position() = '1']"));
        assertTableCellContains(EMAILRECORD_TABLE, 3, 3, ISSUE_TITLE_2);

        stopImpersonating();
    }

    private void entryTypeNameTest()
    {
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
        clickNavButton("Admin");
        setFormElement("int1", "Contract Number");
        setFormElement("string1", "Customer Name");
        clickNavButton("Update Custom Fields");

        //setWorkingForm("requiredFieldsForm");
        for (String field : REQUIRED_FIELDS)
            checkRequiredField(field, true);

        clickNavButton("Update Required Fields");
        clickNavButton("Back to Issues");
        clickNavButton("Admin");

        //setWorkingForm("requiredFieldsForm");
        for (String field : REQUIRED_FIELDS)
        {
            verifyFieldChecked(field);
            checkRequiredField(field, false);
        }
        clickNavButton("Update Required Fields");

        checkRequiredField("Title", true);
        clickNavButton("Update Required Fields");
        clickNavButton("Back to Issues");
        clickNavButton("New Issue");
        clickNavButton("Submit");

        assertTextPresent("Field Title cannot be blank.");
        clickNavButton("Return to Grid");
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
        setFilter("Issues", "Status", "<has any value>");
        clickCheckbox(".toggle");
        clickNavButton("View Details");
        assertTextPresent("a bright flash of light");
        assertTextPresent("don't believe the hype");
        clickLinkWithText("return to grid");
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
        clickNavButton("Submit");

        // assert both issues are present
        clickLinkWithText("return to grid");
        clearAllFilters("Issues", "IssueId");
        assertTextPresent(ISSUE_TITLE_0);
        assertTextPresent(ISSUE_TITLE_1);

        // Filter out all pri-1 bugs; assert newly created issue is filtered out
        setFilter("Issues", "Priority", "Does not Equal", "1");
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
        setFormElement("schemaName", "issues");
        submit();
        clickLinkWithText("Issues Queries");
        createNewQuery("issues");
        setFormElement("ff_newQueryName", "xxyzzy");
        clickNavButton("Create and Edit Source");
        clickNavButton("View Data");
        assertTextPresent(ISSUE_TITLE_0);
        assertTextPresent(ISSUE_TITLE_1);
        clickLinkWithText(PROJECT_NAME);
    }
}
