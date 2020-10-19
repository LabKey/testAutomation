package org.labkey.test.tests.announcements;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.announcements.CreateMessageThreadCommand;
import org.labkey.remoteapi.announcements.DeleteMessageThreadCommand;
import org.labkey.remoteapi.announcements.DeleteMessageThreadResponse;
import org.labkey.remoteapi.announcements.GetDiscussionsCommand;
import org.labkey.remoteapi.announcements.GetDiscussionsResponse;
import org.labkey.remoteapi.announcements.GetMessageThreadCommand;
import org.labkey.remoteapi.announcements.MessageThreadResponse;
import org.labkey.remoteapi.announcements.TestAnnouncementModel;
import org.labkey.remoteapi.announcements.UpdateMessageThreadCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.DailyC;
import org.labkey.test.util.WikiHelper;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({DailyC.class})
public class AnnouncementAPITest extends BaseWebDriverTest
{
    @BeforeClass
    public static void setupProject()
    {
        AnnouncementAPITest init = (AnnouncementAPITest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName());
    }

    @Before
    public void preTest() throws Exception
    {
        goToProjectHome();
    }

    @Test
    public void testGetDiscussion() throws Exception
    {
        _containerHelper.createSubfolder(getProjectName(), "discussionSubfolder");
        String containerPath = getProjectName() + "/discussionSubfolder";
        TestAnnouncementModel parentThread = createThread(new TestAnnouncementModel().setBody("parent"), containerPath)
                .getAnnouncementModel();

        // give it two responses
        MessageThreadResponse firstChildResponse = respondToThread(parentThread,
                new TestAnnouncementModel().setBody("firstChild"), containerPath);
        MessageThreadResponse secondChildResponse = respondToThread(parentThread,
                new TestAnnouncementModel().setBody("secondChild"), containerPath);

        GetDiscussionsCommand cmd = new GetDiscussionsCommand(parentThread.getDiscussionSrcIdentifier());
        GetDiscussionsResponse response = cmd.execute(createDefaultConnection(), containerPath);

        parentThread = getThread(parentThread, containerPath).getAnnouncementModel();
        TestAnnouncementModel secondChild = getThread(secondChildResponse.getAnnouncementModel(), containerPath)
                .getAnnouncementModel();

        assertThat("Expect a single response", response.getThreads().size(), is(1));
        assertThat("Expect the discussion request to return the parent thread",
                response.getThreads().get(0).getEntityId(), is(parentThread.getEntityId()));
    }

    @Test
    public void testCreateThread() throws Exception
    {
        TestAnnouncementModel preCreatedThread = new TestAnnouncementModel()
                .setTitle("FirstCreatedThread")
                .setBody("testBody")
                .setRendererType(WikiHelper.WikiRendererType.RADEOX);
        MessageThreadResponse response = createThread(preCreatedThread, getProjectName());

        assertThat("Expect success", response.getStatusCode(), is(200));
        TestAnnouncementModel created = response.getAnnouncementModel();
        assertThat("Expect title to match", created.getTitle(), is(preCreatedThread.getTitle()));
        assertThat("Expect body to match", created.getBody(), is(preCreatedThread.getBody()));

        // now confirm that the response thread can be found independently
        MessageThreadResponse confirm = getThread(response.getAnnouncementModel(), getProjectName());
        assertThat(confirm.getStatusCode(), is(200));
        assertThat(confirm.getAnnouncementModel().getBody(), is(response.getAnnouncementModel().getBody()));
        assertThat(confirm.getAnnouncementModel().getTitle(), is(response.getAnnouncementModel().getTitle()));
        assertThat(confirm.getAnnouncementModel().getRendererType(), is(response.getAnnouncementModel().getRendererType()));
    }

    @Test
    public void testCreateThreadFailsIfReplyIsFalse() throws Exception
    {
        // arrange
        TestAnnouncementModel preCreatedThread = new TestAnnouncementModel().setTitle("testRespondIfNotReply")
                .setBody("testBody");
        TestAnnouncementModel parentResponse = createThread(preCreatedThread, getProjectName()).getAnnouncementModel();

        TestAnnouncementModel respondingThread = new TestAnnouncementModel().setTitle("wilNeverBeCreated");

        //act
        // make sure if reply is false, the API refuses
        CreateMessageThreadCommand replyFalseCmd = new CreateMessageThreadCommand(respondingThread);
        replyFalseCmd.setReply(false);
        respondingThread.setParent(parentResponse.getEntityId());
        try
        {
            replyFalseCmd.execute(createDefaultConnection(), getProjectName());
            fail("expect command to refuse if reply is false");
        }catch(CommandException success)
        {
            assertThat("Expect the appropriate error", success.getMessage(),
                    is("Failed to create thread. Improper request for create as a parent was specified."));
        }
    }

    @Test
    public void testCreateThreadFailsIfNoParentIsSpecified() throws Exception
    {
        // arrange
        TestAnnouncementModel preCreatedThread = new TestAnnouncementModel().setTitle("testRespondIfNoParent")
                .setBody("testBody");
        TestAnnouncementModel parentResponse = createThread(preCreatedThread, getProjectName()).getAnnouncementModel();
        TestAnnouncementModel respondingThread = new TestAnnouncementModel().setTitle("wilNeverBeCreated");

        // now verify that if reply is true but no parent is specified, it refuses
        CreateMessageThreadCommand noParentSpecified = new CreateMessageThreadCommand(respondingThread);
        noParentSpecified.setReply(true);
        respondingThread.setParent(null);
        try
        {
            noParentSpecified.execute(createDefaultConnection(), getProjectName());
            fail("expect command to refuse if no parent is specified");
        }catch(CommandException success)
        {
            assertThat("Expect the appropriate error", success.getMessage(),
                is("Failed to reply to thread. Improper request for a reply as a parent was not specified."));
        }
    }

    @Test
    public void testCreateThreadFailsIfSpecifiedParentDoesNotExist() throws Exception
    {
        // arrange
        TestAnnouncementModel respondingThread = new TestAnnouncementModel().setTitle("responseToNonExistentThread");

        // now verify that if relpy is true but no parent is specified, it refuses
        CreateMessageThreadCommand noParentSpecified = new CreateMessageThreadCommand(respondingThread);
        noParentSpecified.setReply(true);
        respondingThread.setParent("totally-bogus-entity-id");
        try
        {
            noParentSpecified.execute(createDefaultConnection(), getProjectName());
            fail("expect command to fail if specified parent is invalid");
        }catch(CommandException success)
        {
            assertThat("Expect the appropriate error", success.getMessage(),
                    is("Failed to reply to thread. Unable to find parent thread \"totally-bogus-entity-id\"."));
        }
    }

    @Test
    public void testDeleteThreadByRowId() throws Exception
    {
        // arrange
        TestAnnouncementModel preThread = new TestAnnouncementModel().setBody("deleteMeByRowId");
        TestAnnouncementModel createdThread = createThread(preThread, getProjectName()).getAnnouncementModel();

        assertThat("expect response object to match input", createdThread.getBody(), is(preThread.getBody()));

        // now confirm that the expected thread exists
        MessageThreadResponse confirm = getThread(createdThread, getProjectName());
        assertThat(confirm.getStatusCode(), is(200));
        assertThat(confirm.getAnnouncementModel().getBody(), is(createdThread.getBody()));

        // act
        DeleteMessageThreadCommand delCmd = new DeleteMessageThreadCommand(confirm.getAnnouncementModel().getRowId());
        DeleteMessageThreadResponse delResponse = delCmd.execute(createDefaultConnection(), getProjectName());

        // assert
        assertThat(delResponse.getStatusCode(), is(200));

        // ensure it's no longer there
        try
        {
            getThread(createdThread, getProjectName());
            fail("expect not to find thread after it is deleted");
        }catch(CommandException success)
        {
            assertThat("Expect to be unable to find thread once it is deleted",
                    success.getMessage(), is("Unable to find thread in folder /AnnouncementAPITest Project"));
        }
    }

    @Test
    public void testDeleteThreadByEntityId() throws Exception
    {
        // arrange
        TestAnnouncementModel preThread = new TestAnnouncementModel().setBody("deleteMeByEntityId");
        TestAnnouncementModel createdThread = createThread(preThread, getProjectName()).getAnnouncementModel();

        assertThat("expect response object to match input", createdThread.getBody(), is(preThread.getBody()));

        // now confirm that the expected thread exists
        MessageThreadResponse confirm = getThread(createdThread, getProjectName());
        assertThat(confirm.getStatusCode(), is(200));
        assertThat(confirm.getAnnouncementModel().getBody(), is(createdThread.getBody()));

        // act
        DeleteMessageThreadCommand delCmd = new DeleteMessageThreadCommand(createdThread.getEntityId());
        DeleteMessageThreadResponse delResponse = delCmd.execute(createDefaultConnection(), getProjectName());

        // assert
        assertThat(delResponse.getStatusCode(), is(200));

        // ensure it's no longer there
        try
        {
            getThread(createdThread, getProjectName());
            fail("expect not to find thread after it is deleted");
        }catch(CommandException success)
        {
            assertThat("Expect to be unable to find thread once it is deleted",
                    success.getMessage(), is("Unable to find thread in folder /AnnouncementAPITest Project"));
        }
    }

    @Test
    public void testRespondToExistingThread() throws Exception
    {
        // arrange
        TestAnnouncementModel parentThread = new TestAnnouncementModel();
        parentThread.setTitle("Parent");
        MessageThreadResponse parentCreateResponse = createThread(parentThread, getProjectName());
        TestAnnouncementModel createdParent = parentCreateResponse.getAnnouncementModel();

        TestAnnouncementModel childThread = new TestAnnouncementModel()
                .setTitle("Child")
                .setBody("expected body")
                .setDiscussionSrcIdentifier("test discussion src identifier")
                .setRendererType(WikiHelper.WikiRendererType.MARKDOWN);
        childThread.setParent(createdParent.getEntityId());
        CreateMessageThreadCommand replyCmd = new CreateMessageThreadCommand(childThread);
        replyCmd.setReply(true);

        // act
        MessageThreadResponse childCreateResponse = replyCmd.execute(createDefaultConnection(), getProjectName());

        // assert
        assertThat(childCreateResponse.getStatusCode(), is(200));
        TestAnnouncementModel createdChild = childCreateResponse.getAnnouncementModel();
        assertThat(createdChild.getBody(), is("expected body"));
        assertThat(createdChild.getTitle(), is("Child"));
        assertThat(createdChild.getParent(), is(createdParent.getEntityId()));
        assertThat(childThread.getDiscussionSrcIdentifier(), is("test discussion src identifier"));
        assertThat(createdChild.getRendererType(), is(WikiHelper.WikiRendererType.MARKDOWN.toString()));

        TestAnnouncementModel originalParent = getThread(createdParent, getProjectName()).getAnnouncementModel();
        assertTrue("Expect parent row to be updated with added child",
                originalParent.getResponses().stream().anyMatch(a-> a.getEntityId().equals(createdChild.getEntityId())));
    }

    @Test
    public void testUpdateThread() throws Exception
    {
        // arrange
        TestAnnouncementModel toUpdate = new TestAnnouncementModel()
                .setTitle("old title")
                .setBody("old body")
                .setDiscussionSrcIdentifier("old discussionSrcIdentifier")
                .setRendererType(WikiHelper.WikiRendererType.MARKDOWN);
        TestAnnouncementModel created = createThread(toUpdate, getProjectName()).getAnnouncementModel();

        // act
        created.setTitle("new title")
                .setBody("new body")
                .setDiscussionSrcIdentifier("whole new discussionsrcIdentifier")
                .setRendererType(WikiHelper.WikiRendererType.HTML);
        UpdateMessageThreadCommand upddateCmd = new UpdateMessageThreadCommand(created);
        MessageThreadResponse updateResponse = upddateCmd.execute(createDefaultConnection(), getProjectName());

        // assert
        TestAnnouncementModel updated = updateResponse.getAnnouncementModel();
        assertThat("expect title to update", updated.getTitle(), is("new title"));
        assertThat("expect body to update", updated.getBody(), is("new body"));
        assertThat("don't expect discussionSrcIdentifier to update",
                updated.getDiscussionSrcIdentifier(), is("old discussionSrcIdentifier"));
        assertThat("expect renderer to update", updated.getRendererType(), is("HTML"));
    }

    @Test
    public void testApiPermissions() throws Exception
    {
        // arrange
        TestAnnouncementModel toCreate = new TestAnnouncementModel().setBody("forPermissionsTesting");
        TestAnnouncementModel created = createThread(toCreate, getProjectName()).getAnnouncementModel();

        goToProjectHome(getProjectName());
        impersonateRole("Reader");

        // act and assert
        // as a Reader, attempt to do basic things that require permissions and confirm

        // first, get an existing thread as a reader
        MessageThreadResponse createResponse = getThread(created, getProjectName());
        assertThat(createResponse.getStatusCode(), is(200));

        // create a thread
        try
        {
            createThread(new TestAnnouncementModel(), getProjectName());
            fail("Reader should not have permissions to create a thread");
        }catch (CommandException success)
        {
             assertThat(success.getMessage(), is("User does not have permission to perform this operation."));
        }

        // respond to a thread
        try
        {
            respondToThread(created, new TestAnnouncementModel().setTitle("fake"), getProjectName());
            fail("Reader should not have permission to respond to a thread via createThread.api");
        }catch (CommandException success)
        {
            assertThat(success.getMessage(), is("User does not have permission to perform this operation."));
        }

        // delete a thread
        try
        {
            DeleteMessageThreadCommand delCmd = new DeleteMessageThreadCommand(created.getEntityId());
            delCmd.execute(createDefaultConnection(), getProjectName());
            fail("Reader should not have permissions to delete via deleteThread.api");
        }catch (CommandException success)
        {
            assertThat(success.getMessage(), is("User does not have permission to perform this operation."));
        }

        // update a thread
        try
        {
            UpdateMessageThreadCommand updateCmd = new UpdateMessageThreadCommand(created);
            updateCmd.execute(createDefaultConnection(), getProjectName());
            fail("Reader should not have permission to update via updateThread.api");
        }catch (CommandException success)
        {
            assertThat(success.getMessage(), is("User does not have permission to perform this operation."));
        }

        stopImpersonatingHTTP();
    }

    private MessageThreadResponse createThread(TestAnnouncementModel thread, String containerPath) throws Exception
    {
        CreateMessageThreadCommand cmd = new CreateMessageThreadCommand(thread);
        return cmd.execute(createDefaultConnection(), containerPath);
    }

    private MessageThreadResponse respondToThread(TestAnnouncementModel parentThread,
                                                  TestAnnouncementModel respondingThread,
                                                  String containerPath) throws Exception
    {
        CreateMessageThreadCommand replyCmd = new CreateMessageThreadCommand(respondingThread);
        replyCmd.setReply(true);
        respondingThread.setParent(parentThread.getEntityId());
        return replyCmd.execute(createDefaultConnection(), containerPath);
    }

    private MessageThreadResponse getThread(TestAnnouncementModel thread, String containerPath) throws Exception
    {
        GetMessageThreadCommand cmd = new GetMessageThreadCommand(thread);
        return cmd.execute(createDefaultConnection(), containerPath);
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "AnnouncementAPITest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
