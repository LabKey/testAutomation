/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.tests.announcements;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.pages.announcements.AdminPage;
import org.labkey.test.pages.announcements.EmailPrefsPage;
import org.labkey.test.pages.announcements.InsertPage;
import org.labkey.test.pages.announcements.ModeratorReviewPage;
import org.labkey.test.pages.announcements.RespondPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.query.QueryUtils;

import java.util.Arrays;
import java.util.List;

/**
 * User: tgaluhn
 * Date: 4/30/2018
 */
@Category({Daily.class})
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
        return Arrays.asList("announcements");
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
        // Subscribe the admin user to be notified on all new messages
        EmailPrefsPage.beginAt(this)
                .setNotifyOnAll()
                .update();
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
        QueryUtils.selectAndDeleteAllRows(getProjectName(), "announcement", "Announcement");
        // Go to the project home page to start the test so that settings are changed in the correct folder even in
        // the case that a previous test failed.
        goToProjectHome();
    }

    @Test
    public void testInitialPost()
    {
        log("Verify 'Initial Post' setting only requires approval on first post");
        AdminPage.beginAt(this)
                .setModeratorReviewInitial()
                .save();

        log("Verify initial post requires approval");
        insertAndReviewMessage(APPROVED_USER, APPROVED_TITLE, true);
        // Author was approved once, next post should be auto-approved
        log("Verify second post is auto-approved");
        insertMessage(APPROVED_USER, "This is also an approved message", true);
        log("Verify only Approved user is auto-approved, others still require approval");
        insertAndReviewMessage(SPAM_USER, SPAM_TITLE, false);
    }

    @Test
    public void testNewThread()
    {
        log("Verify 'New Thread' setting requires approval on every new thread");
        AdminPage.beginAt(this)
                .setModeratorReviewNewThread()
                .save();

        log("Verify new thread requires approval");
        String title = insertAndReviewMessage(APPROVED_USER, APPROVED_TITLE, true);

        // Response should not require moderator approval
        log(String.format("Verify response to thread by %s does not require approval", APPROVED_USER));
        title = addResponse(APPROVED_USER, title, true);

        // Response by another author should not require approval even if this is their first message
        log(String.format("Verify response to thread by %s does not require approval", SPAM_USER));
        addResponse(SPAM_USER, title, true);

        // A new message thread should still require approval
        log("Verify new thread still requires approval");
        insertMessage(APPROVED_USER, SPAM_TITLE, false);

        log("Verify posts from editors are auto-approved");
        insertMessage(EDITOR_USER, "This is an editor message", true);
    }

    private String addResponse(String user, String title, boolean expectAutoApproved)
    {
        goToProjectHome();
        impersonate(user);
        log("Add response");
        log("Currently impersonating user:" + user);

        clickAndWait(Locator.linkWithText(title));
        clickButton("Respond");
        String response = "This is a response to " + title + " by user " + user;
        String responseTitle = title + " - " + user;
        new RespondPage(getDriver())
                .setTitle(responseTitle)
                .setBody(response)
                .submit();

        boolean responseAdded = isTextPresent(response);
        if (expectAutoApproved && !responseAdded)
        {
            checker().fatal().error(String.format("Expected response '%s' was not present on the thread page.", response));
        }
        else if (!expectAutoApproved && responseAdded)
        {
            checker().fatal().error(String.format("Response '%s' was present on the thread page. It should not be", response));
        }
        stopImpersonating();

        verifyNotification("RE: " + responseTitle, expectAutoApproved);

        return expectAutoApproved ? responseTitle : title; // New title if the response was posted successfully
    }

    @Test
    public void testAll()
    {
        log("Verify 'All' setting requires approval for all posts");
        AdminPage.beginAt(this)
                .setModeratorReviewAll()
                .save();

        log("Verify initial post requires approval");
        insertAndReviewMessage(APPROVED_USER, APPROVED_TITLE, true);
        // Author was approved once, but future post should still require approval
        log("Verify second post still requires approval");
        insertAndReviewMessage(APPROVED_USER, SPAM_TITLE, false);

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
        insertAndReviewMessage(SPAM_USER, APPROVED_TITLE, true);
    }

    private String insertMessage(String user, String title, boolean expectAutoApproval)
    {
        goToProjectHome();
        impersonate(user);
        log("Insert message");
        log("Currently impersonating user:" + user);
        // make title unique in test scope, to check email notifications later
        title = title + " " + System.currentTimeMillis();
        log("Inserting message with title " + title);
        InsertPage.beginAt(this)
                .setTitle(title)
                .setBody(title)
                .submit();
        log("Submission of message (supposedly) complete");
        stopImpersonating();
        if (expectAutoApproval)
        {
            ModeratorReviewPage.beginAt(this);
            assertTextNotPresent(title);
        }

        verifyMessage(title, expectAutoApproval);

        return title;
    }

    private String insertAndReviewMessage(String user, String title, boolean approve)
    {
        title = insertMessage(user, title, false);
        ModeratorReviewPage.beginAt(this)
                .review(title, approve);
        verifyMessage(title, approve);
        return title;
    }

    private void verifyMessage(String title, boolean expect)
    {
        goToProjectHome();
        boolean condition = false;
        int tries = 0;

        // Give the message a chance to show up, or be removed, from the home page.
        while(!condition && tries < 5)
        {
            if (expect)
                condition = isTextPresent(title);
            else
                condition = !isTextPresent(title);

            // If the expected condition has been met, then break out of the loop.
            if(condition)
                break;

            sleep(1_000);
            refresh();
            tries++;
        }

        if(!condition)
        {
            String msgFormat;
            if(expect)
                msgFormat = "Expected message with title '%s' was not present on home page.";
            else
                msgFormat = "Expected message with title '%s' was present on home page, it should not be.";

            checker().fatal().error(String.format(msgFormat, title));
        }
        else
        {
            verifyNotification(title, expect);
        }

    }

    private void verifyNotification(String title, boolean expect)
    {
        goToModule("Dumbster");
        EmailRecordTable.EmailMessage notification;
        boolean condition = false;
        int tries = 0;

        // Try five times for the expected condition to be met.
        while (!condition && tries < 5)
        {
            notification = new EmailRecordTable(this).getMessage(title);

            if(expect)
                condition = notification != null;
            else
                condition = notification == null;

            // If the expected condition has been met, then break out of the loop.
            if(condition)
                break;

            sleep(1_000);
            refresh();
            tries++;

        }

        if(!condition)
        {
            String msgFormat;
            if(expect)
                msgFormat = "Expected email notification for message '%s' was not present.";
            else
                msgFormat = "Email notification for message '%s' was present, it should not be.";

            checker().fatal().error(String.format(msgFormat, title));
        }

    }
}
