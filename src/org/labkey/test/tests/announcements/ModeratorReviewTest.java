package org.labkey.test.tests.announcements;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.DailyC;
import org.labkey.test.pages.announcements.AdminPage;
import org.labkey.test.pages.announcements.InsertPage;
import org.labkey.test.pages.announcements.ModeratorReviewPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.PortalHelper;

import java.util.Collections;
import java.util.List;

/**
 * User: tgaluhn
 * Date: 4/30/2018
 */
@Category({DailyC.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class ModeratorReviewTest extends BaseWebDriverTest
{
    private final String SPAM_TITLE = "This is a spam message";
    private final String APPROVED_TITLE = "This is a real message";

    private final String SPAM_USER = "moderatorreviewspamauthor@messages.test";
    private final String APPROVED_USER = "moderatorreviewapprovedauthor@messages.test";
    private final String EDITOR_USER = "moderatorrevieweditor@messages.test";

    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.singletonList("announcements");
    }

    @Override
    protected String getProjectName()
    {
        return this.getClass().getSimpleName() + " Project";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @BeforeClass
    public static void setupProject()
    {
        ModeratorReviewTest init = (ModeratorReviewTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        new PortalHelper(this).addWebPart("Messages");
        _userHelper.createUser(APPROVED_USER);
        _userHelper.createUser(SPAM_USER);
        _userHelper.createUser(EDITOR_USER);
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.addMemberToRole(APPROVED_USER, "Author", PermissionsHelper.MemberType.user);
        apiPermissionsHelper.addMemberToRole(SPAM_USER, "Author", PermissionsHelper.MemberType.user);
        apiPermissionsHelper.addMemberToRole(EDITOR_USER, "Editor", PermissionsHelper.MemberType.user);
    }

    @Before
    public void preTest() throws Exception
    {
        log("Delete all existing messages in project");
        deleteAllRows(getProjectName(), "announcement", "Announcement");
    }

    @Test
    public void testInitialPost()
    {
        log("Verify 'Initial Post' setting only requires approval on first post");
        AdminPage.beginAt(this)
                .setModeratorReviewInitial()
                .save();

        log("Verify initial post requires approval");
        insertMessage(APPROVED_USER, APPROVED_TITLE, false);
        reviewMessage(APPROVED_TITLE, true);
        // Author was approved once, next post should be auto-approved
        log("Verify second post is auto-approved");
        insertMessage(APPROVED_USER, "This is also an approved message", true);
        log("Verify only Approved user is auto-approved, others still require approval");
        insertMessage(SPAM_USER, SPAM_TITLE, false);
        reviewMessage(SPAM_TITLE, false);
    }

    @Test
    public void testAll()
    {
        log("Verify 'All' setting requires approval for all posts");
        AdminPage.beginAt(this)
                .setModeratorReviewAll()
                .save();

        log("Verify initial post requires approval");
        insertMessage(APPROVED_USER, APPROVED_TITLE, false);
        reviewMessage(APPROVED_TITLE, true);
        // Author was approved once, but future post should still require approval
        log("Verify second post still requires approval");
        insertMessage(APPROVED_USER, SPAM_TITLE, false);
        reviewMessage(SPAM_TITLE, false);

        log("Verifying email sent to moderators (admins)");
        goToModule("Dumbster");
        assertTextPresent("requires moderator review");

        log("Verify posts from editors are auto-approved");
        insertMessage(EDITOR_USER, "This is an editor message", true);
    }

    @Test
    public void testFlipFromNone()
    {
        log("Verify an existing message board can be changed to require moderator review");
        AdminPage.beginAt(this)
                .setModeratorReviewNone()
                .save();
        insertMessage(SPAM_USER, SPAM_TITLE, true);
        // Oh no! Someone spammed my message board! Let's require moderator review.
        AdminPage.beginAt(this)
                .setModeratorReviewAll()
                .save();
        insertMessage(SPAM_USER, APPROVED_TITLE, false);
        reviewMessage(APPROVED_TITLE, true);
    }

    private void insertMessage(String user, String title, boolean expectAutoApproval)
    {
        goToProjectHome();
        impersonate(user);
        log("Insert message");
        InsertPage.beginAt(this)
                .setTitle(title)
                .submit();
        stopImpersonating();

        verifyMessage(title, expectAutoApproval);
    }

    private void reviewMessage(String title, boolean approve)
    {
        ModeratorReviewPage.beginAt(this)
                .review(title, approve);
        verifyMessage(title, approve);
    }

    private void verifyMessage(String title, boolean expect)
    {
        goToProjectHome();
        if (expect)
            assertTextPresent(title);
        else
            assertTextNotPresent(title);
    }
}
