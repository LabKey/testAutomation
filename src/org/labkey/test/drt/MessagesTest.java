package org.labkey.test.drt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.io.File;

/**
 * User: tamram
 * Date: May 15, 2006
 */
public class MessagesTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "MessagesVerifyProject";
    private static final String EXPIRES1 = "2107-07-19";
    private static final String EXPIRES2 = "2108-07-19";
    private static final String MSG1_TITLE = "test message 1";
    private static final String MSG1_BODY_FIRST = "this is a test message";
    private static final String MSG1_BODY = "this is a test message to Banana";
    private static final String MSG2_TITLE = "test message 2";
    private static final String MSG3_TITLE = "test message 3";
    private static final String RESP1_TITLE = "test response 1";
    private static final String RESP1_BODY = "this is another test, thanks";
    private static final String RESP2_BODY = "third test, thanks";
    private static final String USER1 = "apple@a1b2c1.com";
    private static final String USER2 = "orange@a1b2c1.com";
    private static final String USER3 = "banana@a1b2c1.com";
    private static final String HTML_BODY = "1 <b>x</b>\n" +
            "<b>${labkey.webPart(partName='Query', title='My Proteins', schemaName='ms2', " +
            "queryName='Sequences', allowChooseQuery='true', allowChooseView='true')}</b>\n";
    private static final String HTML_BODY_WEBPART_TEST = "Best Gene Name";

    public String getAssociatedModuleDirectory()
    {
        return "announcements";
    }

    protected void permissionCheck(String permission, boolean readAbility)
    {
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Permissions");
        setPermissions("Users", permission);
        impersonate(USER1);
        clickLinkWithText(PROJECT_NAME);
        if (readAbility)
            assertTextPresent(MSG1_BODY);
        else
            assertTextNotPresent(MSG1_BODY);
        signOut();
        signIn();
    }

    protected void doCleanup()
    {
        deleteUser(USER1);
        deleteUser(USER2);
        deleteUser(USER3);
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void doTestSteps()
    {

        log("Open new project, add group, alter permissions");
        createProject(PROJECT_NAME, "Collaboration");
        createPermissionsGroup("testers1");
        assertPermissionSetting("Administrators", "Admin (all permissions)");
        assertPermissionSetting("testers1", "No Permissions");

        log("Add messages and search to project");
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Messages");
        addWebPart("Search");

        //bvt
        log("Check email preferences");
        clickLinkWithText("email preferences");
        checkCheckbox("emailPreference", "1", true);
        clickNavButton("Update");
        clickNavButton("Done");

        log("Check email admin works");
        clickLinkWithText("email admin");
        selectOptionByText("defaultEmailOption", "All conversations");
        clickNavButton("Set");
        clickNavButton("Bulk Edit");
        assertTextPresent("All conversations");
        assertFormElementEquals("emailOptionId", "1");
        clickLinkWithText("Messages");
        clickLinkWithText("email preferences");
        checkCheckbox("notificationType", "256", true);
        checkCheckbox("emailPreference", "0", true);
        clickNavButton("Update");
        clickNavButton("Done");
        clickLinkWithText("email admin");
        selectOptionByText("defaultEmailOption", "No email");
        clickNavButton("Set");

        log("Check message works in Wiki");
        clickLinkWithText("Messages");
        clickLinkWithText("new message");
        setFormElement("title", MSG1_TITLE);
        setFormElement("expires", EXPIRES1);
        setFormElement("body", "1 <b>x</b>");
        selectOptionByText("rendererType", "Wiki Page");
        assertTextPresent("Admin Broadcast");
        submit();
        assertTextPresent(MSG1_TITLE);
        assertTextPresent(EXPIRES1);
        assertTextPresent("<b>x</b>");
        clickNavButton("Delete Message");
        clickNavButton("Delete");

        log("Check that HTML message works");
        clickLinkWithText("new message");
        setFormElement("title", MSG1_TITLE);
        setFormElement("body", HTML_BODY);
        selectOptionByText("rendererType", "HTML");
        submit();
        assertTextPresent("1 x");
        assertTextPresent(HTML_BODY_WEBPART_TEST);
        clickNavButton("Delete Message");
        clickNavButton("Delete");
        //endbvt

        log("Check that Plain Text message works and is added everywhere");
        clickLinkWithText("new message");
        setFormElement("title", MSG1_TITLE);
        setFormElement("body", MSG1_BODY_FIRST);
        selectOptionByText("rendererType", "Plain Text");
        //test attachments too.
        if (isFileUploadAvailable())
        {
            clickLinkWithText("Attach a file", false);
            File file = new File(getLabKeyRoot() + "/common.properties");
            setFormElement("formFiles[0]", file);
        }
        else
            log("File upload skipped.");
        submit();
        if (isFileUploadAvailable())
            assertTextPresent("common.properties");
        assertTextPresent(MSG1_BODY_FIRST);
        clickLinkWithText("view list");
        assertTextPresent(MSG1_TITLE);
        clickLinkWithText("Messages");
        assertTextPresent(MSG1_TITLE);
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(MSG1_TITLE);

        log("Check that edit works");
        clickLinkWithText("edit");
        setFormElement("body", MSG1_BODY);
        assertTextPresent("Delete");
        submit();
        assertTextPresent(MSG1_BODY);

        log("Add response");
        clickNavButton("Post Response");
        setFormElement("title", RESP1_TITLE);
        setFormElement("expires", EXPIRES2);
        setFormElement("body", RESP1_BODY);
        submit();

        log("Make sure response was entered correctly");
        assertTextPresent(RESP1_TITLE);
        assertTextPresent(EXPIRES2);
        assertTextPresent(RESP1_BODY);

        log("Add second response, make sure it was entered and recognized");
        clickNavButton("Post Response");
        setFormElement("body", RESP2_BODY);
        submit();
        assertTextPresent(RESP2_BODY);
        clickLinkWithText("Messages");
        assertTextPresent("2 responses");

        log("Checks the search module on messages");
        clickLinkWithText(PROJECT_NAME);
        searchFor(PROJECT_NAME, "Banana", 1, RESP1_TITLE);
        //bvt
        log("Create fake user for permissions check");
        clickLinkWithText("Permissions");
        clickLink("managegroup/MessagesVerifyProject/Users");
        setFormElement("names", USER1);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");

        log("Check if permissions work without security");
        permissionCheck("Reader", true);
        permissionCheck("Editor", true);

        log("Check with security");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("customize");
        checkCheckbox("secure", 1, true);
        clickNavButton("Save");
        permissionCheck("Reader", false);
        permissionCheck("Editor", true);

        log("Check if the customized names work");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("customize");
        setFormElement("boardName", "Notes");
        setFormElement("conversationName", "Thread");
        clickNavButton("Save");
        assertTextPresent("Notes");
        assertTextPresent("thread");
        clickLinkWithText("customize");
        setFormElement("boardName", "Messages");
        setFormElement("conversationName", "Message");
        clickNavButton("Save");

        log("Check if sorting works");
        clickLinkWithText("new message");
        setFormElement("title", MSG2_TITLE);
        submit();
        clickLinkWithText("Messages");
        clickLinkWithText("view message or respond");
        assertTextPresent(MSG2_TITLE);
        clickLinkWithText("Messages");
        clickLinkWithText("view message or respond", 1);
        clickNavButton("Post Response");
        submit();
        clickLinkWithText("Messages");
        clickLinkWithText("customize");
        checkCheckbox("sortOrderIndex", 1, true);
        clickNavButton("Save");
        clickLinkWithText("view message or respond");
        assertTextPresent(MSG1_TITLE);

        log("Edit other customize options");
        clickLinkWithText("Messages");
        clickLinkWithText("customize");
        uncheckCheckbox("titleEditable");
        checkCheckbox("memberList");
        checkCheckbox("status");
        uncheckCheckbox("expires");
        checkCheckbox("assignedTo");
        uncheckCheckbox("formatPicker");
        selectOptionByText("defaultAssignedTo", USER1);
        clickNavButton("Save");

        log("Check if status and expires work");
        clickLinkWithText("new message");
        assertTextPresent(USER1);
        clickNavButton("Cancel");
        clickLinkWithText(MSG2_TITLE);
        clickNavButton("Post Response");
        selectOptionByText("status", "Closed");
        assertFormElementEquals("assignedTo", "");
        submit();
        assertTextPresent("Status: Closed");
        assertTextNotPresent("Expires:");
        impersonate(USER1);
        clickLinkWithText(PROJECT_NAME);
        assertTextNotPresent(MSG2_TITLE);
        signOut();
        signIn();
        clickLinkWithText(PROJECT_NAME);

        log("Test member list");
        clickLinkWithText("Permissions");
        setPermissions("Users", "Reader");
        clickNavButton("Update");
        clickLinkWithText("manage group", 1);
        setFormElement("names", USER2);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");
        clickLinkWithText("Permissions");
        clickLinkWithText("manage group", 0);
        setFormElement("names", USER3);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("new message");
        setFormElement("emailList", USER2);
        click(Locator.buttonWithImgSrc("Submit.button"));
        assertAlert("Title must not be blank");
        setFormElement("title", MSG3_TITLE);
        submit();
        assertTextPresent("This user doesn't have permission");
        setFormElement("emailList", USER1);
        setFormElement("assignedTo", USER3);
        submit();
        assertTextPresent("Members: "+USER1);
        assertTextNotPresent("Assigned To: "+USER3);
        impersonate(USER1);
        clickLinkWithText(PROJECT_NAME);
        assertTextPresent(MSG3_TITLE);
        signOut();
        signIn();
        impersonate(USER2);
        clickLinkWithText(PROJECT_NAME);
        assertTextNotPresent(MSG3_TITLE);
        signOut();
        signIn();
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("customize");
        checkCheckbox("secure", 0, true);
        clickNavButton("Save");
        clickLinkWithText(MSG3_TITLE);
        clickNavButton("Delete Message");
        clickNavButton("Delete");
        //endbvt
        log("Check delete response works and is recognized");
        clickLinkWithText("view message or respond", 1);
        clickLinkWithText("delete");
        clickNavButton("Delete");
        assertTextPresent(RESP2_BODY);
        clickLinkWithText("delete");
        clickNavButton("Delete");
        clickLinkWithText("Messages");
        assertTextPresent("1 response");
        clickLinkWithText("view message or respond", 1);
        clickLinkWithText("delete");
        clickNavButton("Delete");
        clickLinkWithText("Messages");
        assertTextNotPresent(RESP1_TITLE);
        clickLinkWithText(PROJECT_NAME);
        assertTextNotPresent(RESP1_TITLE);
        assertTextNotPresent(MSG2_TITLE);
        clickLinkWithText("view message or respond");

        log("Check delete message works fully");
        clickNavButton("Delete Message");
        clickNavButton("Delete");
        assertTextNotPresent(MSG1_TITLE);
        clickLinkWithText(PROJECT_NAME);
        assertTextNotPresent(MSG1_TITLE);
    }

}
