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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.labkey.test.util.PasswordUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyA.class})
public class MessagesLongTest extends BaseWebDriverTest
{
    // TODO: This test and/or MessagesLongTest are misnamed
    private static final String PROJECT_NAME = "MessagesVerifyProject";
    private static final String MSG1_TITLE = "test message 1";
    private static final String MSG1_BODY = "this is a test message to Banana";
    private static final String RESP1_TITLE = "test response 1";
    private static final String RESP1_BODY = "this is another test, thanks";
    private static final String EXPIRES1 = "2107-07-19";
    private static final String EXPIRES2 = "2108-07-19";
    private static final String MSG1_BODY_FIRST = "this is a test message";
    private static final String MSG2_TITLE = "test message 2";
    private static final String MSG3_TITLE = "test message 3";
    private static final String RESP2_BODY = "third test, thanks";
    private static final String USER1 = "messageslong_user1@messages.test";
    private static final String USER2 = "messageslong_user2@messages.test";
    private static final String USER3 = "messageslong_user3@messages.test";
    private static final String NOT_A_USER = "Squirrel";
    private static final String RESPONDER = "responder@messages.test";
    private static final String HTML_BODY = "1 <b>x</b>\n" +
            "<b>${labkey.webPart(partName='Lists')}</b>\n";
    private static final String HTML_BODY_WEBPART_TEST = "manage lists";
    private static final String MEMBER_LIST = "memberListInput";

    String user = "message_user@gmail.com";
    String group = "Message group";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/announcements";
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

    protected void permissionCheck(String permission, boolean readAbility)
    {
        clickProject(PROJECT_NAME);
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.removePermission("Users","Reader");
        _permissionsHelper.removePermission("Users","Author");
        _permissionsHelper.removePermission("Users","Editor");
        _permissionsHelper.setPermissions("Users", permission);
        _permissionsHelper.exitPermissionsUI();
        impersonate(USER1);
        clickProject(PROJECT_NAME);
        if (readAbility)
            assertTextPresent(MSG1_BODY);
        else
            assertTextNotPresent(MSG1_BODY);
        stopImpersonating();
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteUsers(afterTest, USER1, USER2, USER3, RESPONDER, user);
        deleteProject(MessagesLongTest.PROJECT_NAME, afterTest);
    }

    @Test
    public void testSteps()
    {
        log("Open new project, add group, alter permissions");
        _containerHelper.createProject(PROJECT_NAME, "Collaboration");
        _permissionsHelper.createPermissionsGroup("Administrators");
        _permissionsHelper.setPermissions("Administrators", "Project Administrator");
        _permissionsHelper.createPermissionsGroup("testers1");
        _permissionsHelper.assertPermissionSetting("testers1", "No Permissions");
        _permissionsHelper.exitPermissionsUI();
        enableModule(PROJECT_NAME, "Dumbster");

        enableEmailRecorder();
        basicMessageTests();
        schemaTest();
        
        doTestEmailPrefsMine();

        clickProject(PROJECT_NAME);
        log("Check email preferences");
        clickWebpartMenuItem("Messages", "Email", "Preferences");
        checkCheckbox(Locator.radioButtonByName("emailPreference").index(2));
        clickButton("Update");
        clickButton("Done");

        log("Customize message board");
        clickWebpartMenuItem("Messages", "Admin");
        checkCheckbox(Locator.checkboxByName("expires"));
        clickButton("Save");

        log("Check email admin works");
        clickWebpartMenuItem("Messages", "Email", "Administration");

        assertElementNotPresent(Locator.xpath("//a[text()='messages']"));
        click(Locator.lkButton("Update Settings"));
        shortWait().until(LabKeyExpectedConditions.dataRegionPanelIsExpanded(Locator.id("Users")));
        _extHelper.clickSideTab("messages");
        Locator.XPathLocator folderDefaultCombo = Locator.xpath("//input[@name='defaultEmailOption']/../../div");

        waitForElement(Locator.xpath("//input[@name='defaultEmailOption']"), WAIT_FOR_JAVASCRIPT);
        clickButton("Update Folder Default", 0);

        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        assertTextPresent("All conversations");

        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        clickProject(PROJECT_NAME);

        log("Check message works in Wiki");
        clickWebpartMenuItem("Messages", "New");
        setFormElement(Locator.name("title"), MSG1_TITLE);
        setFormElement(Locator.name("expires"), EXPIRES1);
        setFormElement(Locator.id("body"), "1 <b>first message testing</b>");
        selectOptionByText(Locator.name("rendererType"), "Wiki Page");
        clickButton("Submit");
        assertTextPresent(MSG1_TITLE);
        clickAndWait(Locator.linkWithText("view message or respond"));
        assertTextPresent(EXPIRES1);
        assertTextPresent("<b>first message testing</b>");
        clickButton("Delete Message");
        clickButton("Delete");

        log("Check that HTML message works");
        clickButton("New");
        setFormElement(Locator.name("title"), MSG1_TITLE);
        setFormElement(Locator.id("body"), HTML_BODY);
        selectOptionByText(Locator.name("rendererType"), "HTML");
        clickButton("Submit");
        assertElementPresent(Locator.tag("div").withClass("message-text").withPredicate("starts-with(normalize-space(), '1 x')"));
        assertElementPresent(Locator.linkWithText(HTML_BODY_WEBPART_TEST));

        log("Check that edit works");
        clickAndWait(Locator.linkWithText("view message or respond"));
        clickAndWait(Locator.linkWithText("edit"));
        setFormElement(Locator.id("body"), MSG1_BODY);
        clickButton("Submit");
        assertTextPresent(MSG1_BODY);

        log("Add response");
        clickButton("Respond");
        setFormElement(Locator.name("title"), RESP1_TITLE);
        setFormElement(Locator.name("expires"), EXPIRES2);
        setFormElement(Locator.id("body"), RESP1_BODY);
        clickButton("Submit");

        log("Make sure response was entered correctly");
        assertTextPresent(RESP1_TITLE);
        assertTextPresent(EXPIRES2);
        assertTextPresent(RESP1_BODY);

        log("Add second response, make sure it was entered and recognized");
        clickButton("Respond");
        setFormElement(Locator.id("body"), RESP2_BODY);
        clickButton("Submit");
        assertTextPresent(RESP2_BODY);
        clickAndWait(Locator.linkWithText("Messages"));
        assertElementPresent(Locator.css("#table1 td").withText(" (2 responses)")); // xpath doesn't work with nbsp

        log("Create fake user for permissions check");
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.clickManageGroup("Users");
        setFormElement(Locator.name("names"), USER1);
        uncheckCheckbox(Locator.checkboxByName("sendEmail"));
        clickButton("Update Group Membership");

        log("Check if permissions work without security");
        permissionCheck("Reader", true);
        permissionCheck("Editor", true);

        log("Check with security");
        clickProject(PROJECT_NAME);
        clickWebpartMenuItem("Messages", "Admin");
        checkCheckbox(Locator.radioButtonByName("secure").index(1));
        clickButton("Save");
        permissionCheck("Reader", false);
        permissionCheck("Editor", true);

        log("Check if the customized names work");
        clickProject(PROJECT_NAME);
        clickWebpartMenuItem("Messages", "Admin");
        setFormElement(Locator.name("boardName"), "Notes");
        setFormElement(Locator.name("conversationName"), "Thread");
        clickButton("Save");
        assertTextPresent("Notes");
        assertTextPresent("thread");
        clickWebpartMenuItem("Notes", "Admin");
        setFormElement(Locator.name("boardName"), "Messages");
        setFormElement(Locator.name("conversationName"), "Message");
        clickButton("Save");

        log("Check if sorting works");
        clickWebpartMenuItem("Messages", "New");
        setFormElement(Locator.name("title"), MSG2_TITLE);
        clickButton("Submit");
        clickAndWait(Locator.linkWithText("Messages"));
        clickAndWait(Locator.linkWithText("view message or respond"));
        assertTextPresent(MSG2_TITLE);
        clickAndWait(Locator.linkWithText("Messages"));
        clickAndWait(Locator.linkWithText("view message or respond", 1));
        clickButton("Respond");
        clickButton("Submit");
        clickAndWait(Locator.linkWithText("Messages"));
        clickAndWait(Locator.linkWithText("Admin"));
        checkCheckbox(Locator.radioButtonByName("sortOrderIndex").index(1));
        clickButton("Save");
        clickAndWait(Locator.linkWithText("view message or respond"));
        assertTextPresent(MSG1_TITLE);

        log("Edit other customize options");
        clickAndWait(Locator.linkWithText("Messages"));
        clickAndWait(Locator.linkWithText("Admin"));
        uncheckCheckbox(Locator.checkboxByName("titleEditable"));
        checkCheckbox(Locator.checkboxByName("memberList"));
        checkCheckbox(Locator.checkboxByName("status"));
        uncheckCheckbox(Locator.checkboxByName("expires"));
        checkCheckbox(Locator.checkboxByName("assignedTo"));
        uncheckCheckbox(Locator.checkboxByName("formatPicker"));
        selectOptionByText(Locator.name("defaultAssignedTo"), displayNameFromEmail(USER1));
        clickButton("Save");

        log("Check if status and expires work");
        clickButton("New");
        assertTextPresent(displayNameFromEmail(USER1));
        clickButton("Cancel");
        clickAndWait(Locator.linkWithText(MSG2_TITLE));
        clickButton("Respond");
        selectOptionByText(Locator.name("status"), "Closed");
        assertFormElementEquals("assignedTo", "");
        clickButton("Submit");
        assertTextPresent("Status: Closed");
        assertTextNotPresent("Expires:");
        impersonate(USER1);
        clickProject(PROJECT_NAME);
        assertTextNotPresent(MSG2_TITLE);
        stopImpersonating();

        testMemberLists();

        clickProject(PROJECT_NAME);
        clickWebpartMenuItem("Messages", "Admin");
        checkCheckbox(Locator.radioButtonByName("secure").index(0));
        clickButton("Save");
        clickAndWait(Locator.linkWithText(MSG3_TITLE));
        clickButton("Delete Message");
        clickButton("Delete");

        log("Check delete response works and is recognized");
        clickAndWait(Locator.linkWithText("view message or respond", 1));
        clickAndWait(Locator.linkWithText("delete"));
        clickButton("Delete");
        assertTextNotPresent(RESP1_BODY);
        clickAndWait(Locator.linkWithText("Messages"));
        assertElementPresent(Locator.css("#table1 td").withText(" (2 responses)")); // xpath doesn't work with nbsp
        clickProject(PROJECT_NAME);
        assertTextNotPresent(MSG2_TITLE);
        clickAndWait(Locator.linkWithText("view message or respond"));

        log("Check delete message works fully");
        clickButton("Delete Message");
        clickButton("Delete");
        assertTextNotPresent(MSG1_TITLE);
        clickProject(PROJECT_NAME);
        assertTextNotPresent(MSG1_TITLE);

        log("Check emailed messages");
        goToModule("Dumbster");
        assertTextPresent("RE: " + MSG1_TITLE, 6);
        click(Locator.linkWithText(MSG1_TITLE, 0));
        assertTextPresent("1 <b>x</b>");
        assertTextPresent("<a href=\"/labkey/list/MessagesVerifyProject/begin.view?\" class=\"labkey-text-link\">manage lists</a>");
        click(Locator.linkWithText(MSG1_TITLE, 1));
        assertTextPresent("first message testing");
        assertElementNotPresent(Locator.linkWithText(MSG3_TITLE));
        assertElementNotPresent(Locator.linkWithText(MSG2_TITLE));
    }

    private void testMemberLists()
    {
        clickProject(PROJECT_NAME);
        // USER1 is now a reader
        log("Test member list");
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.removePermission("Users", "Editor");
        _permissionsHelper.setPermissions("Users", "Reader");
        _permissionsHelper.exitPermissionsUI();

        // USER2 is a nobody
        goToSiteUsers();
        clickButton("Add Users");
        setFormElement(Locator.id("newUsers"), USER2);
        uncheckCheckbox(Locator.checkboxByName("sendMail"));
        clickButton("Add Users");
        clickProject(PROJECT_NAME);

        // USER3 is a Project Administrator
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.clickManageGroup("Administrators");
        setFormElement(Locator.name("names"), USER3);
        uncheckCheckbox(Locator.checkboxByName("sendEmail"));
        clickButton("Update Group Membership");

        clickProject(PROJECT_NAME);
        clickWebpartMenuItem("Messages", "New");
        setFormElement(Locator.id(MEMBER_LIST), USER2);
        clickButtonContainingText("Submit", "Title must not be blank");
        clickButtonContainingText("OK", 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("title"), MSG3_TITLE);
        clickButton("Submit");
        assertTextPresent("This user doesn't have permission");
        setFormElement(Locator.id(MEMBER_LIST), USER1);
        selectOptionByText(Locator.name("assignedTo"), displayNameFromEmail(USER3));
        clickButton("Submit");
        clickAndWait(Locator.linkWithText("view message or respond"));
        assertTextPresent("Members: "+USER1);
        assertElementPresent(Locator.css("#webpart_-1 td").withText("Assigned To: "+ displayNameFromEmail(USER3)));
        impersonate(USER1);
        clickProject(PROJECT_NAME);

        // Tests for changing email vs display name autocomplete / redisplay rules to follow site permissions
        // Also verify bug fixes during that development: member list stays tied to parent of thread, and isn't
        // wiped on a response.
        log("Verify member list email vs display name rules for reader");
        clickAndWait(Locator.linkWithText(MSG3_TITLE));
        // should be display name only
        assertTextNotPresent(USER3);
        assertTextPresent(displayNameFromEmail(USER3));
        stopImpersonating();

        log("Verify member list failed user lookup reports error");
        clickProject(PROJECT_NAME);
        impersonateRole("Editor");
        clickAndWait(Locator.linkWithText(MSG3_TITLE));
        clickButton("Respond");
        // enter invalid username, ensure error appears
        setFormElement(Locator.id(MEMBER_LIST), NOT_A_USER);
        clickButtonContainingText("Submit", NOT_A_USER + ": Invalid");

        log("Verify member list autocomplete only shows display name, not email");
        setFormElement(Locator.id(MEMBER_LIST), displayNameFromEmail(RESPONDER));
        setFormElement(Locator.id("body"), "Another response again woo hoo");
        assertTextPresent(displayNameFromEmail(RESPONDER));
        assertTextNotPresent(RESPONDER);

        log("Verify redisplay is display name only, even if email entered in member list.");
        // Also tests persistence of member list changes.
        setFormElement(Locator.id(MEMBER_LIST),USER1);
        clickButton("Submit");
        assertTextPresent(displayNameFromEmail("Members: " +USER1));
        assertTextNotPresent(USER1);
        stopImpersonatingRole();
        log("Verify admin user still sees email address");
        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText(MSG3_TITLE));
        assertTextPresent("Members: " + USER1);
    }

    //Expects an empty email record
    //Regression test: 14824: forum not sending email for message replies to users configured as "my conversations"
    private void doTestEmailPrefsMine()
    {
        String _messageTitle = "Mine Message";
        String _messageBody = "test";

        clickProject(PROJECT_NAME);
        createUserWithPermissions(RESPONDER, PROJECT_NAME, "Editor");
        clickButton("Save and Finish");

        clickWebpartMenuItem("Messages", "Email", "Preferences");
        checkCheckbox(Locator.radioButtonByName("emailPreference").index(1));
        clickButton("Update");
        clickButton("Done");

        createNewMessage(_messageTitle, _messageBody);

        impersonate(RESPONDER);

        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText(_messageTitle));
        clickButton("Respond");
        setFormElement(Locator.name("title"), _messageTitle + " response");
        setFormElement(Locator.id("body"), _messageBody + " response");
        clickButton("Submit");

        stopImpersonating();

        clickProject(PROJECT_NAME);
        goToModule("Dumbster");
        DataRegionTable record = new DataRegionTable("EmailRecord", this, false, false);
        List<String> subject = record.getColumnDataAsText("Message");
        assertEquals("Message creator and responder should both receive notifications", "RE: "+_messageTitle, subject.get(0));
        assertEquals("Message creator and responder should both receive notifications", "RE: "+_messageTitle, subject.get(1));
        List<String> to = record.getColumnDataAsText("To");
        assertTrue("Incorrect message notifications.",
                to.get(0).equals(RESPONDER) && to.get(1).equals(PasswordUtil.getUsername()) ||
                to.get(1).equals(RESPONDER) && to.get(0).equals(PasswordUtil.getUsername()));

        assertElementPresent(Locator.linkWithText(_messageTitle));
        assertElementPresent(Locator.linkWithText("RE: "+_messageTitle));
        click(Locator.linkWithText("RE: "+_messageTitle, 1));
    }

    private void createNewMessage(String title, String body)
    {
        clickButton("New");
        setFormElement(Locator.name("title"), title);
        setFormElement(Locator.id("body"), body);
        selectOptionByText(Locator.name("rendererType"), "HTML");
        clickButton("Submit");
    }

    private void basicMessageTests()
    {
        log("Add search to project");
        addWebPart("Search");

        createUser(user, null);
        goToHome();
        goToProjectHome();
        _permissionsHelper.createPermissionsGroup(group);
        _permissionsHelper.setPermissions(group, "Editor");
//        add
        _permissionsHelper.addUserToProjGroup(user, getProjectName(), group);
        goToProjectHome();

        log("Check that Plain Text message works and is added everywhere");
        clickProject(PROJECT_NAME);
        clickButton("New");

        // Check defaults for uncustomized message board
        assertTextNotPresent("Status");
        assertTextNotPresent("Assigned To");
        assertTextNotPresent("Members");
        assertTextNotPresent("Expires");

        setFormElement(Locator.name("title"), MSG1_TITLE);
        setFormElement(Locator.id("body"), MSG1_BODY_FIRST);
        selectOptionByText(Locator.name("rendererType"), "Plain Text");

        log("test attachments too");
        click(Locator.linkContainingText("Attach a file"));
        File file = new File(getLabKeyRoot() + "/common.properties");
        setFormElement(Locator.name("formFiles[00]"), file);
        clickButton("Submit");
        assertTextPresent("common.properties");
        assertTextPresent(MSG1_BODY_FIRST);
        clickAndWait(Locator.linkWithText("view message or respond"));
        clickAndWait(Locator.linkWithText("view list"));
        assertTextPresent(MSG1_TITLE);
        goToModule("Messages");
        clickAndWait(Locator.linkWithText(MSG1_TITLE));

        log("test edit messages");
        clickAndWait(Locator.linkWithText("edit"));
        setFormElement(Locator.id("body"), MSG1_BODY);
        assertTextPresent("remove");
        click(Locator.linkWithText("remove"));
        waitForText("This cannot be undone");
        clickButton("OK", 0);
        waitForTextToDisappear("common.properties");
        assertTextNotPresent("common.properties");
        clickButton("Submit");
        assertTextPresent(MSG1_BODY);


        log("verify a user can subscribe to a thread");
        impersonate(user);
        goToProjectHome();
        clickAndWait(Locator.linkContainingText("view message"));
        Locator subscribeButton = Locator.tagWithText("span", "subscribe");
        assertElementPresent(subscribeButton);
        click(subscribeButton);
        clickAndWait(Locator.tagWithText("span", "thread"));
        clickAndWait(Locator.linkWithText("unsubscribe"));
        assertElementPresent(subscribeButton);

        click(subscribeButton);
        clickAndWait(Locator.tagWithText("span", "forum"));
        clickButton("Update");
        clickButton("Done");


        stopImpersonating();
        goToProjectHome();

        log("test add response");
        clickAndWait(Locator.linkWithText("view message or respond"));
        clickButton("Respond");
        setFormElement(Locator.name("title"), RESP1_TITLE);
        setFormElement(Locator.id("body"), RESP1_BODY);
        clickButton("Submit");

        log("Make sure response was entered correctly");
        assertTextPresent(RESP1_TITLE);
        assertTextPresent(RESP1_BODY);

        log("test the search module on messages");
        clickProject(PROJECT_NAME);
        searchFor(PROJECT_NAME, "Banana", 1, MSG1_TITLE);

        log("test filtering of messages grid");
        clickAndWait(Locator.linkWithText("view list"));
        setFilterAndWait("Announcements", "Title", "Equals", "foo", WAIT_FOR_PAGE);

        assertTextNotPresent(RESP1_TITLE);
    }

    private void schemaTest()
    {

        SelectRowsCommand selectCmd = new SelectRowsCommand("announcement", "ForumSubscription");
        selectCmd.setMaxRows(-1);
        selectCmd.setContainerFilter(ContainerFilter.CurrentAndSubfolders);
        selectCmd.setColumns(Arrays.asList("*"));
        SelectRowsResponse selectResp = null;

        String[] queries = {"Announcement", "AnnouncementSubscription", "EmailFormat", "EmailOption", "ForumSubscription"};
        int[] counts = {2, 0, 2, 5, 1};

        for(int i = 0; i<queries.length; i++)
        {
            selectResp = executeSelectRowCommand("announcement", queries[i]);
            assertEquals("Count mismatch with query: " + queries[i], counts[i], selectResp.getRowCount().intValue());
        }

    }
}
