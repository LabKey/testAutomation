/*
 * Copyright (c) 2008-2017 LabKey Corporation
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
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.admin.PermissionsPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.Locator.NBSP;
import static org.labkey.test.Locator.name;

@Category({DailyA.class})
public class MessagesLongTest extends BaseWebDriverTest
{
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
    private static final String MSG4_TITLE = "test message 4";
    private static final String MSG4_BODY = "test message 4 - special characters: " + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
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
    private static final String TEMPLATE_TEXT = "***Please do not reply to this email notification. Replies to this email are routed to an unmonitored mailbox. Instead, please use the link below.***";
    private static final String USER = "message_user@messages.test";
    private static final String GROUP = "Message group";

    public static final String FILES_DEFAULT_COMBO = "Default setting for files:";
    public static final String MESSAGES_DEFAULT_COMBO = "Default setting for messages:";
    public static final String USERS_UPDATE_BUTTON = "Update user settings";
    public static final String NEW_SETTING_LABEL = "New setting:";
    public static final String POPUP_UPDATE_BUTTON = "Update settings for 1 user";
    public static final String FILES_MENU_ITEM = "For files";
    public static final String MESSAGES_MENU_ITEM = "For messages";

    private final PortalHelper _portalHelper = new PortalHelper(this);
    private String _messageUserId;

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("announcements");
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
        PermissionsPage permissionsPage = navBar().goToPermissionsPage()
            .removePermission("Users", "Reader")
            .removePermission("Users","Reader")
            .removePermission("Users","Author")
            .removePermission("Users","Editor")
            .setPermissions("Users", permission);
        permissionsPage.clickSaveAndFinish();
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
        deleteUsersIfPresent(USER1, USER2, USER3, RESPONDER, USER);
        _containerHelper.deleteProject(MessagesLongTest.PROJECT_NAME, afterTest);
        modifyTemplate(false);
        goToHome();
    }

    @Test
    public void testSteps()
    {
        log("Modify the default email template for message board notification.");
        // This is done to test Issue 23934: Allow customization of email template for message board notifications
        modifyTemplate(true);
        goToHome();

        log("Open new project, add group, alter permissions");
        _containerHelper.createProject(PROJECT_NAME, "Collaboration");
        navBar().goToPermissionsPage()
            .createPermissionsGroup("Administrators")
            .setPermissions("Administrators", "Project Administrator")
            .createPermissionsGroup("testers1")
            .assertPermissionSetting("testers1", "No Permissions")
            .clickSaveAndFinish();
        _containerHelper.enableModule(PROJECT_NAME, "Dumbster");

        enableEmailRecorder();
        basicMessageTests();
        schemaTest();

        doTestEmailPrefsMine();

        clickProject(PROJECT_NAME);
        log("Check email preferences");
        _portalHelper.clickWebpartMenuItem("Messages", true, "Email", "Preferences");
        checkCheckbox(Locator.radioButtonByName("emailPreference").index(2));
        clickButton("Update");
        clickButton("Done");

        log("Customize message board");
        _portalHelper.clickWebpartMenuItem("Messages", true, "Admin");
        checkCheckbox(Locator.checkboxByName("expires"));
        clickButton("Save");

        verifyAdmin();
        clickProject(PROJECT_NAME);

        log("Check message works in Wiki");
        _portalHelper.clickWebpartMenuItem("Messages", true, "New");
        selectOptionByText(Locator.name("rendererType"), "Wiki Page");
        setFormElement(Locator.name("title"), MSG1_TITLE);
        setFormElement(Locator.name("expires"), EXPIRES1);
        setFormElement(Locator.id("body"), "1 <b>first message testing</b>");
        clickButton("Submit", longWaitForPage);
        assertTextPresent(MSG1_TITLE);
        clickAndWait(Locator.linkWithText("view message or respond"));
        assertTextPresent(EXPIRES1, "<b>first message testing</b>");
        clickButton("Delete Message");
        clickButton("Delete");

        log("Create message using markdown");
        clickButton( "New");
        Select select = new Select(Locator.name("rendererType").findElement(getDriver()));
        assertTrue("default selection should be 'Markdown'", select.getFirstSelectedOption().getText().equals("Markdown"));
        assertElementPresent(Locator.tagWithClassContaining("li", "nav-item")
            .withChild(Locator.tagWithClass("a", "nav-link").withText("Source")));
        Locator previewPaneTab = Locator.tagWithClassContaining("li", "nav-item")
                .withChild(Locator.tagWithClass("a", "nav-link").withText("Preview"));
        assertElementPresent(previewPaneTab);
        setFormElement(Locator.name("title"), "Markdown is a thing now");
        setFormElement(Locator.id("body"), "# Holy Header, Batman!\n" +
                "**bold as bold can possibly be**\n" +
                "\n" +
                "```var foo = bar.fooValue;```\n" +
                "\n" +
                "## List of things I don't like \n" +
                "+ hair clogs\n" +
                "+ stinky feet\n" +
                "+ internet trolls");
        // now look at the preview pane
        previewPaneTab.findWhenNeeded(getDriver()).click();
        waitForElement(Locator.tagWithText("h2", "List of things I don't like"), 2000);
        assertElementPresent(Locator.tagWithText("li", "hair clogs"));
        assertElementPresent(Locator.tagWithText("li", "stinky feet"));
        assertElementPresent(Locator.tagWithText("li", "internet trolls"));
        clickButton("Submit", 0);
        Window confirmWindow = Window.Window(getDriver()).withTitle("Confirm message formatting").find();
        confirmWindow.clickButton("Yes");
        assertElementPresent(Locator.tagWithText("h1", "Holy Header, Batman!"));
        assertElementPresent(Locator.tagWithText("strong", "bold as bold can possibly be"));

        log("Check that message with unicode character works");
        clickButton("New");
        selectOptionByText(Locator.name("rendererType"), "Plain Text");
        setFormElement(Locator.name("title"), MSG4_TITLE);
        setFormElement(Locator.id("body"), MSG4_BODY);
        clickButton("Submit");
        assertTextPresent(MSG4_TITLE);
        assertTextPresent(MSG4_BODY);

        log("Check that HTML message works");
        clickButton("New");
        selectOptionByText(Locator.name("rendererType"), "HTML");
        setFormElement(Locator.name("title"), MSG1_TITLE);
        setFormElement(Locator.id("body"), HTML_BODY);
        clickButton("Submit");
        assertElementPresent(Locator.tag("div").withClass("message-text").withPredicate("starts-with(normalize-space(), '1 x')"));
        assertElementPresent(Locator.linkWithText(HTML_BODY_WEBPART_TEST));

        log("Check that edit works");
        clickAndWait(Locator.linkWithText("view message or respond"));
        clickAndWait(Locator.linkWithText("edit"));
        setFormElement(Locator.id("body"), MSG1_BODY);
        clickButton("Submit", longWaitForPage);
        assertTextPresent(MSG1_BODY);

        log("Add response");
        clickRespondButton();
        setFormElement(Locator.name("title"), RESP1_TITLE);
        setFormElement(Locator.name("expires"), EXPIRES2);
        setFormElement(Locator.id("body"), RESP1_BODY);
        clickButton("Submit", longWaitForPage);

        log("Make sure response was entered correctly");
        assertTextPresent(
                RESP1_TITLE,
                EXPIRES2,
                RESP1_BODY);

        log("Add second response with attachment, make sure it was entered and recognized");
        clickRespondButton();
        setFormElement(Locator.id("body"), RESP2_BODY);
        click(Locator.linkContainingText("Attach a file"));
        File attachmentFile = TestFileUtils.getSampleData("fileTypes/pdf_sample.pdf");
        setFormElement(Locator.name("formFiles[00]"), attachmentFile);
        clickButton("Submit", longWaitForPage);
        assertTextPresent(RESP2_BODY);
        clickAndWait(Locator.linkWithText("Messages"));
        assertElementPresent(Locator.id("table1").append(Locator.tag("td").withText("(2" + NBSP + "responses)")));

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
        _portalHelper.clickWebpartMenuItem("Messages", true, "Admin");
        checkCheckbox(Locator.radioButtonByName("secure").index(1));
        clickButton("Save");
        permissionCheck("Reader", false);
        permissionCheck("Editor", true);

        log("Check if the customized names work");
        clickProject(PROJECT_NAME);
        _portalHelper.clickWebpartMenuItem("Messages", true, "Admin");
        setFormElement(Locator.name("boardName"), "Notes");
        setFormElement(Locator.name("conversationName"), "Thread");
        clickButton("Save");
        assertTextPresent("Notes", "thread");
        _portalHelper.clickWebpartMenuItem("Notes", true, "Admin");
        setFormElement(Locator.name("boardName"), "Messages");
        setFormElement(Locator.name("conversationName"), "Message");
        clickButton("Save");

        log("Check if sorting works");
        _portalHelper.clickWebpartMenuItem("Messages", true, "New");
        setFormElement(Locator.name("title"), MSG2_TITLE);
        clickButton("Submit", longWaitForPage);
        clickAndWait(Locator.linkWithText("Messages"));
        clickAndWait(Locator.linkWithText("view message or respond"));
        assertTextPresent(MSG2_TITLE);
        clickAndWait(Locator.linkWithText("Messages"));
        clickAndWait(Locator.linkWithText("view message or respond").index(1));
        clickRespondButton();
        clickButton("Submit", longWaitForPage);
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
        selectOptionByText(Locator.name("defaultAssignedTo"), _userHelper.getDisplayNameForEmail(USER1));
        clickButton("Save");

        log("Check if status and expires work");
        clickButton("New");
        assertTextPresent(_userHelper.getDisplayNameForEmail(USER1));
        clickButton("Cancel");
        clickAndWait(Locator.linkWithText(MSG2_TITLE));
        clickRespondButton();
        selectOptionByText(Locator.name("status"), "Closed");
        assertEquals("", getFormElement(Locator.name("assignedTo")));
        clickButton("Submit", longWaitForPage);
        assertTextPresent("Status: Closed");
        assertTextNotPresent("Expires:");
        impersonate(USER1);
        clickProject(PROJECT_NAME);
        // We now show closed messages by default
        assertTextPresent(MSG2_TITLE);
        stopImpersonating();

        testMemberLists();

        clickProject(PROJECT_NAME);
        _portalHelper.clickWebpartMenuItem("Messages", true, "Admin");
        checkCheckbox(Locator.radioButtonByName("secure"));
        clickButton("Save");
        clickAndWait(Locator.linkWithText(MSG3_TITLE));
        clickButton("Delete Message");
        clickButton("Delete");

        log("Check delete response works and is recognized");
        clickAndWait(Locator.linkWithText("view message or respond").index(1));
        clickAndWait(Locator.linkWithText("delete"));
        clickButton("Delete");
        assertTextNotPresent(RESP1_BODY);
        clickAndWait(Locator.linkWithText("Messages"));
        assertElementPresent(Locator.id("table1").append(Locator.tag("td").withText("(2" + NBSP + "responses)")));
        clickProject(PROJECT_NAME);
        // We now show closed messages by default
        assertTextPresent(MSG2_TITLE);
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
        click(Locator.linkWithText(MSG1_TITLE));
        assertTextPresent(
                "1 <b>x</b>",
                "<a href=\"/labkey" + WebTestHelper.buildRelativeUrl("list", getProjectName(), "begin") + "?\" class=\"labkey-text-link\">manage lists</a>");
        click(Locator.linkWithText(MSG1_TITLE).index(1));
        assertTextPresent("first message testing");
        assertElementNotPresent(Locator.linkWithText(MSG3_TITLE));
        assertElementNotPresent(Locator.linkWithText(MSG2_TITLE));

        log("Check attachment linked in emailed message");
        click(Locator.linkWithText("RE: " + MSG1_TITLE));
        assertTextPresent(attachmentFile.getName());
        assertTextNotPresent(TEMPLATE_TEXT);
        assertElementPresent(Locator.linkWithText(attachmentFile.getName()));

        log("Validate that the Message Board Daily Digest is sent.");
        getDriver().navigate().to(WebTestHelper.getBaseURL() + "/announcements/home/sendDailyDigest.view?");
        goToModule("Dumbster");
        assertTextPresent("New posts to /" + PROJECT_NAME, 2);
        click(Locator.linkWithText("New posts to /" + PROJECT_NAME));
        assertTextPresent("The following new posts were made yesterday");
    }

    private void verifyAdmin()
    {
        log("Check email admin works");

        // Folder default settings
        final String fileDefaultExistingSetting = "No Email";
        final String fileDefaultNewSetting = "Daily digest";

        log("Check folder default settings");
        _portalHelper.clickWebpartMenuItem("Messages", true, "Email", "Administration");

        assertElementNotPresent(Locator.xpath("//a[text()='messages']"));

        _ext4Helper.selectComboBoxItem(FILES_DEFAULT_COMBO, fileDefaultNewSetting);
        clickButton("Update", 0);
        _ext4Helper.waitForMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        assertTextPresent(fileDefaultNewSetting);

        // User settings
        log("Check user settings");
        final String usersDataRegion = "Users";
        final String userSettingNew = "Daily digest of all conversations";
        final String messageColumn = "Message Settings";

        waitForElementToDisappear(Ext4Helper.Locators.window("Update complete"));
        new DataRegionTable(usersDataRegion, getDriver()).checkCheckboxByPrimaryKey(_messageUserId);
        shortWait().until(LabKeyExpectedConditions.elementIsEnabled(Locator.lkButton(USERS_UPDATE_BUTTON)));
        new BootstrapMenu(getDriver(), Locator.tagWithClassContaining("div","lk-menu-drop")
                .withDescendant(Locator.lkButton(USERS_UPDATE_BUTTON)).findElement(getDriver()))
                .clickSubMenu(false, "For messages");
        _ext4Helper.selectComboBoxItem(NEW_SETTING_LABEL, userSettingNew);
        clickButton(POPUP_UPDATE_BUTTON, 0);
        waitForText("Are you sure");
        clickButton("Yes");

        DataRegionTable dr = new DataRegionTable(usersDataRegion, getDriver());
        assertEquals(dr.getDataAsText(_messageUserId, messageColumn), userSettingNew);
     }

    private void testMemberLists()
    {
        clickProject(PROJECT_NAME);
        // USER1 is now a reader
        log("Test member list");
        navBar().goToPermissionsPage()
            .removePermission("Users", "Editor")
            .setPermissions("Users", "Reader")
            .clickSaveAndFinish();

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
        _portalHelper.clickWebpartMenuItem("Messages", true, "New");
        setFormElement(Locator.id(MEMBER_LIST), USER2);
        clickButtonContainingText("Submit", "Title must not be blank");
        clickButtonContainingText("OK", 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("title"), MSG3_TITLE);
        clickButton("Submit");
        assertTextPresent("This user doesn't have permission");
        setFormElement(Locator.id(MEMBER_LIST), USER1);
        selectOptionByText(Locator.name("assignedTo"), _userHelper.getDisplayNameForEmail(USER3));
        clickButton("Submit");
        clickAndWait(Locator.linkWithText("view message or respond"));
        verifyMemberList();
        assertElementPresent(Locator.tagWithName("div", "webpart").withDescendant(Locator.tag("td")
                .withText("Assigned" + NBSP + "To: " + _userHelper.getDisplayNameForEmail(USER3))));
        impersonate(USER1);
        clickProject(PROJECT_NAME);

        // Tests for changing email vs display name autocomplete / redisplay rules to follow site permissions
        // Also verify bug fixes during that development: member list stays tied to parent of thread, and isn't
        // wiped on a response.
        log("Verify member list email vs display name rules for reader");
        clickAndWait(Locator.linkWithText(MSG3_TITLE));
        // should be display name only
        assertTextNotPresent(USER3);
        assertTextPresent(_userHelper.getDisplayNameForEmail(USER3));
        stopImpersonating();

        log("Verify member list failed user lookup reports error");
        clickProject(PROJECT_NAME);
        impersonateRole("Editor");
        clickAndWait(Locator.linkWithText(MSG3_TITLE));
        clickRespondButton();
        // enter invalid username, ensure error appears
        setFormElement(Locator.id(MEMBER_LIST), NOT_A_USER);
        clickButtonContainingText("Submit", NOT_A_USER + ": Invalid");

        log("Verify member list autocomplete only shows display name, not email");
        setFormElement(Locator.id(MEMBER_LIST), _userHelper.getDisplayNameForEmail(RESPONDER));
        setFormElement(Locator.id("body"), "Another response again woo hoo");
        assertTextPresent(_userHelper.getDisplayNameForEmail(RESPONDER));
        assertTextNotPresent(RESPONDER);

        log("Verify redisplay is display name only, even if email entered in member list.");
        // Also tests persistence of member list changes.
        setFormElement(Locator.id(MEMBER_LIST),USER1);
        clickButton("Submit");
        assertTextPresent(_userHelper.getDisplayNameForEmail("Members: " + USER1));
        assertTextNotPresent(USER1);
        stopImpersonatingRole();
        log("Verify admin user still sees email address");
        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText(MSG3_TITLE));
        verifyMemberList();
    }

    private void verifyMemberList()
    {
        assertTextPresent("Members: "+ USER1 + " (" + _userHelper.getDisplayNameForEmail(USER1) +")");
    }


    private void clickRespondButton()
    {
        clickButton("Respond");
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

        _portalHelper.clickWebpartMenuItem("Messages", true, "Email", "Preferences");
        checkCheckbox(Locator.radioButtonByName("emailPreference").index(1));
        clickButton("Update");
        clickButton("Done");

        createNewMessage(_messageTitle, _messageBody);

        impersonate(RESPONDER);

        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText(_messageTitle));
        clickRespondButton();
        setFormElement(Locator.name("title"), _messageTitle + " response");
        setFormElement(Locator.id("body"), _messageBody + " response");
        clickButton("Submit");

        stopImpersonating();

        clickProject(PROJECT_NAME);
        goToModule("Dumbster");
        EmailRecordTable record = new EmailRecordTable(this);
        List<String> subject = record.getColumnDataAsText("Message");
        assertEquals("Message creator and responder should both receive notifications", "RE: "+_messageTitle, subject.get(0));
        assertEquals("Message creator and responder should both receive notifications", "RE: "+_messageTitle, subject.get(1));
        List<String> to = record.getColumnDataAsText("To");
        assertTrue("Incorrect message notifications.",
                to.get(0).equals(RESPONDER) && to.get(1).equals(PasswordUtil.getUsername()) ||
                to.get(1).equals(RESPONDER) && to.get(0).equals(PasswordUtil.getUsername()));

        assertElementPresent(Locator.linkWithText(_messageTitle));
        assertElementPresent(Locator.linkWithText("RE: "+_messageTitle));
        click(Locator.linkWithText("RE: " + _messageTitle).index(1));
    }

    private void createNewMessage(String title, String body)
    {
        clickButton("New");
        selectOptionByText(Locator.name("rendererType"), "HTML");
        setFormElement(Locator.name("title"), title);
        setFormElement(Locator.id("body"), body);
        clickButton("Submit");
    }

    private void basicMessageTests()
    {
        log("Add search to project");
        _portalHelper.addWebPart("Search");

        _messageUserId = _userHelper.createUser(USER).getUserId().toString();
        goToHome();
        goToProjectHome();
        navBar().goToPermissionsPage()
            .createPermissionsGroup(GROUP)
            .setPermissions(GROUP, "Editor")
            .addUserToProjGroup(USER, getProjectName(), GROUP);
        goToProjectHome();

        log("Check that Plain Text message works and is added everywhere");
        clickProject(PROJECT_NAME);
        clickButton("New");

        // Check defaults for uncustomized message board
        assertElementNotPresent(Locator.lkLabel("Status"));
        assertElementNotPresent(Locator.lkLabel("Assigned To"));
        assertElementNotPresent(Locator.lkLabel("Members"));
        assertElementNotPresent(Locator.lkLabel("Expires"));

        setFormElement(Locator.name("title"), MSG1_TITLE);
        setFormElement(Locator.id("body"), MSG1_BODY_FIRST);
        selectOptionByText(Locator.name("rendererType"), "Plain Text");

        log("test attachments too");
        click(Locator.linkContainingText("Attach a file"));
        File attachmentFile = TestFileUtils.getSampleData("fileTypes/docx_sample.docx");
        setFormElement(Locator.name("formFiles[00]"), attachmentFile);
        clickButton("Submit");
        assertTextPresent(attachmentFile.getName(), MSG1_BODY_FIRST);
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
        waitForTextToDisappear(attachmentFile.getName());
        assertTextNotPresent(attachmentFile.getName());
        clickButton("Submit");
        assertTextPresent(MSG1_BODY);

        log("verify a user can subscribe to a thread");
        impersonate(USER);
        goToProjectHome();
        clickAndWait(Locator.linkContainingText("view message"));
        Locator subscribeButton = Locator.tagWithText("span", "subscribe");
        assertElementPresent(subscribeButton);
        click(subscribeButton);
        clickAndWait(Locator.tagWithText("a", "thread"));
        clickAndWait(Locator.linkWithText("unsubscribe"));
        assertElementPresent(subscribeButton);

        click(subscribeButton);
        clickAndWait(Locator.tagWithText("a", "forum"));
        clickButton("Update");
        clickButton("Done");

        stopImpersonating();
        goToProjectHome();

        log("test add response");
        clickAndWait(Locator.linkWithText("view message or respond"));
        clickRespondButton();
        setFormElement(Locator.name("title"), RESP1_TITLE);
        setFormElement(Locator.id("body"), RESP1_BODY);
        clickButton("Submit");

        log("Make sure response was entered correctly");
        assertTextPresent(RESP1_TITLE,
                RESP1_BODY);

        log("test the search module on messages");
        clickProject(PROJECT_NAME);
        searchFor(PROJECT_NAME, "Banana", 1, MSG1_TITLE);

        log("test filtering of messages grid");
        clickAndWait(Locator.linkWithText("view list"));
        DataRegionTable region = new DataRegionTable("Announcements", getDriver());
        region.setFilter("Title", "Equals", "foo", WAIT_FOR_PAGE);

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

    private void modifyTemplate(boolean modify)
    {
        String emailTemplate;
        goToAdminConsole().clickEmailCustomization();
        selectOptionByText(Locator.css("select[id='templateClass']"), "Message board notification");
        if(modify)
        {
            emailTemplate = getFormElement(Locator.css("textarea[id='emailMessage']")).replace(TEMPLATE_TEXT, "");
            setFormElement(Locator.css("textarea[id='emailMessage']"), emailTemplate);
            clickButton("Save");
        }
        else
        {
            if (isButtonPresent("Reset to Default Template"))
            {
                clickButton("Reset to Default Template");
            }
        }
    }
}
