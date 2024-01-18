package org.labkey.test.tests.announcements;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.announcements.AnnouncementModel;
import org.labkey.remoteapi.announcements.CreateMessageThreadCommand;
import org.labkey.remoteapi.announcements.DeleteMessageThreadCommand;
import org.labkey.remoteapi.announcements.DeleteMessageThreadResponse;
import org.labkey.remoteapi.announcements.GetDiscussionsCommand;
import org.labkey.remoteapi.announcements.GetDiscussionsResponse;
import org.labkey.remoteapi.announcements.GetMessageThreadCommand;
import org.labkey.remoteapi.announcements.MessageThreadResponse;
import org.labkey.remoteapi.announcements.UpdateMessageThreadCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.WikiHelper;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({Daily.class})
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
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testGetDiscussions() throws Exception
    {
        // Arrange
        String folderName = "discussionSubfolder";
        String containerPath = getProjectName() + "/" + folderName;
        _containerHelper.createSubfolder(getProjectName(), folderName);

        AnnouncementModel parentThread = createThread(new AnnouncementModel().setTitle("parent"), containerPath);

        // Give it two responses
        respondToThread(parentThread, new AnnouncementModel().setTitle("firstChild"), containerPath);
        respondToThread(parentThread, new AnnouncementModel().setTitle("secondChild"), containerPath);

        // Act
        GetDiscussionsCommand cmd = new GetDiscussionsCommand(parentThread.getDiscussionSrcIdentifier());
        GetDiscussionsResponse response = cmd.execute(createDefaultConnection(), containerPath);

        parentThread = getThread(parentThread, containerPath).getAnnouncementModel();

        // Assert
        assertThat("Expect a single response", response.getThreads().size(), is(1));
        assertThat("Expect the discussion request to return the parent thread",
                response.getThreads().get(0).getEntityId(), is(parentThread.getEntityId()));
    }

    @Test
    public void testCreateThread() throws Exception
    {
        // Arrange
        AnnouncementModel preCreatedThread = new AnnouncementModel()
                .setTitle("FirstCreatedThread")
                .setBody("testBody")
                .setRendererType(WikiHelper.WikiRendererType.RADEOX);

        // Act
        MessageThreadResponse response = new CreateMessageThreadCommand(preCreatedThread)
                .execute(createDefaultConnection(), getProjectName());

        // Assert
        assertThat("Expect success", response.getStatusCode(), is(200));
        AnnouncementModel created = response.getAnnouncementModel();
        assertThat("Expect title to match", created.getTitle(), is(preCreatedThread.getTitle()));
        assertThat("Expect body to match", created.getBody(), is(preCreatedThread.getBody()));

        // Confirm that the response thread can be found independently
        MessageThreadResponse confirm = getThread(response.getAnnouncementModel());
        assertThat(confirm.getAnnouncementModel().getBody(), is(response.getAnnouncementModel().getBody()));
        assertThat(confirm.getAnnouncementModel().getTitle(), is(response.getAnnouncementModel().getTitle()));
        assertThat(confirm.getAnnouncementModel().getRendererType(), is(response.getAnnouncementModel().getRendererType()));
    }

    @Test
    public void testCreateThreadFailsIfReplyIsFalse() throws Exception
    {
        // Arrange
        AnnouncementModel preCreatedThread = new AnnouncementModel()
                .setTitle("testRespondIfNotReply")
                .setBody("testBody");
        AnnouncementModel parentThread = createThread(preCreatedThread);

        AnnouncementModel respondingThread = new AnnouncementModel().setTitle("wilNeverBeCreated");
        respondingThread.setParent(parentThread.getEntityId());

        // Act
        // Ensure if "reply" is false, then the API refuses
        CreateMessageThreadCommand replyFalseCmd = new CreateMessageThreadCommand(respondingThread);
        replyFalseCmd.setReply(false);

        try
        {
            replyFalseCmd.execute(createDefaultConnection(), getProjectName());
            fail("expect command to refuse if reply is false");
        }
        catch (CommandException success)
        {
            assertThat("Expect the appropriate error", success.getMessage(),
                    is("Failed to create thread. Improper request for create as a parent was specified."));
        }
    }

    @Test
    public void testCreateThreadFailsIfNoParentIsSpecified() throws Exception
    {
        // Arrange
        AnnouncementModel respondingThread = new AnnouncementModel().setTitle("wilNeverBeCreated");
        respondingThread.setParent(null);

        // Ensure if "reply" is true but no "parent" is specified, then the API refuses
        CreateMessageThreadCommand noParentSpecified = new CreateMessageThreadCommand(respondingThread);
        noParentSpecified.setReply(true);

        try
        {
            noParentSpecified.execute(createDefaultConnection(), getProjectName());
            fail("expect command to refuse if no parent is specified");
        }
        catch (CommandException success)
        {
            assertThat("Expect the appropriate error", success.getMessage(),
                is("Failed to reply to thread. Improper request for a reply as a parent was not specified."));
        }
    }

    @Test
    public void testCreateThreadFailsIfSpecifiedParentDoesNotExist() throws Exception
    {
        // Arrange
        AnnouncementModel respondingThread = new AnnouncementModel().setTitle("responseToNonExistentThread");
        respondingThread.setParent("totally-bogus-entity-id");

        // Verify that if "reply" is true but "parent" is not specified, then it fails.
        CreateMessageThreadCommand noParentSpecified = new CreateMessageThreadCommand(respondingThread);
        noParentSpecified.setReply(true);

        try
        {
            noParentSpecified.execute(createDefaultConnection(), getProjectName());
            fail("expect command to fail if specified parent is invalid");
        }
        catch (CommandException success)
        {
            assertThat("Expect the appropriate error", success.getMessage(),
                    is("Failed to reply to thread. Unable to find parent thread \"totally-bogus-entity-id\"."));
        }
    }

    @Test
    public void testDeleteThreadByRowId() throws Exception
    {
        // Arrange
        AnnouncementModel createdThread = createThread(new AnnouncementModel().setTitle("deleteMeByRowId"));

        // now confirm that the expected thread exists
        MessageThreadResponse confirm = getThread(createdThread);
        assertThat(confirm.getStatusCode(), is(200));
        assertThat(confirm.getAnnouncementModel().getTitle(), is(createdThread.getTitle()));

        // Act
        DeleteMessageThreadCommand delCmd = new DeleteMessageThreadCommand(confirm.getAnnouncementModel().getRowId());
        DeleteMessageThreadResponse delResponse = delCmd.execute(createDefaultConnection(), getProjectName());

        // Assert
        assertThat(delResponse.getStatusCode(), is(200));

        try
        {
            getThread(createdThread);
            fail("expect not to find thread after it is deleted");
        }
        catch (CommandException success)
        {
            assertThat("Expect to be unable to find thread once it is deleted",
                    success.getMessage(), is("Unable to find thread in folder /" + getProjectName()));
        }
    }

    @Test
    public void testDeleteThreadByEntityId() throws Exception
    {
        // Arrange
        AnnouncementModel createdThread = createThread(new AnnouncementModel().setTitle("Delete By EntityId"));

        // Confirm that the expected thread exists
        MessageThreadResponse confirm = getThread(createdThread);
        assertThat(confirm.getStatusCode(), is(200));
        assertThat(confirm.getAnnouncementModel().getTitle(), is(createdThread.getTitle()));

        // Act
        DeleteMessageThreadCommand delCmd = new DeleteMessageThreadCommand(createdThread.getEntityId());
        DeleteMessageThreadResponse delResponse = delCmd.execute(createDefaultConnection(), getProjectName());

        // Assert
        assertThat(delResponse.getStatusCode(), is(200));

        try
        {
            getThread(createdThread);
            fail("expect not to find thread after it is deleted");
        }
        catch (CommandException success)
        {
            assertThat("Expect to be unable to find thread once it is deleted",
                    success.getMessage(), is("Unable to find thread in folder /" + getProjectName()));
        }
    }

    @Test
    public void testRespondToExistingThread() throws Exception
    {
        // Arrange
        AnnouncementModel createdParent = createThread(new AnnouncementModel().setTitle("Parent"));

        AnnouncementModel childThread = new AnnouncementModel()
                .setTitle("Child")
                .setBody("expected body")
                .setDiscussionSrcIdentifier("test discussion src identifier")
                .setRendererType(WikiHelper.WikiRendererType.MARKDOWN);
        childThread.setParent(createdParent.getEntityId());
        CreateMessageThreadCommand replyCmd = new CreateMessageThreadCommand(childThread);
        replyCmd.setReply(true);

        // Act
        MessageThreadResponse childCreateResponse = replyCmd.execute(createDefaultConnection(), getProjectName());

        // Assert
        assertThat("Expect success", childCreateResponse.getStatusCode(), is(200));
        AnnouncementModel createdChild = childCreateResponse.getAnnouncementModel();
        assertThat(createdChild.getBody(), is("expected body"));
        assertThat(createdChild.getTitle(), is("Child"));
        assertThat(createdChild.getParent(), is(createdParent.getEntityId()));
        assertThat(childThread.getDiscussionSrcIdentifier(), is("test discussion src identifier"));
        assertThat(createdChild.getRendererType(), is(WikiHelper.WikiRendererType.MARKDOWN.toString()));

        AnnouncementModel originalParent = getThread(createdParent).getAnnouncementModel();
        assertTrue("Expect parent row to be updated with added child",
                originalParent.getResponses().stream().anyMatch(a-> a.getEntityId().equals(createdChild.getEntityId())));
    }

    @Test
    public void testUpdateThread() throws Exception
    {
        // Arrange
        AnnouncementModel toUpdate = new AnnouncementModel()
                .setTitle("old title")
                .setBody("old body")
                .setDiscussionSrcIdentifier("old discussionSrcIdentifier")
                .setRendererType(WikiHelper.WikiRendererType.MARKDOWN);

        AnnouncementModel created = createThread(toUpdate)
                .setTitle("new title")
                .setBody("new body")
                .setDiscussionSrcIdentifier("whole new discussionSrcIdentifier")
                .setRendererType(WikiHelper.WikiRendererType.HTML);

        // Act
        MessageThreadResponse response = new UpdateMessageThreadCommand(created)
                .execute(createDefaultConnection(), getProjectName());

        // Assert
        assertThat("Expect success", response.getStatusCode(), is(200));
        AnnouncementModel updated = response.getAnnouncementModel();
        assertThat("expect title to update", updated.getTitle(), is("new title"));
        assertThat("expect body to update", updated.getBody(), is("new body"));
        assertThat("don't expect discussionSrcIdentifier to update",
                updated.getDiscussionSrcIdentifier(), is("old discussionSrcIdentifier"));
        assertThat("expect renderer to update", updated.getRendererType(), is("HTML"));
    }

    @Test
    public void testApiPermissions() throws Exception
    {
        // Arrange
        AnnouncementModel preThread = new AnnouncementModel()
                .setTitle("Permissions Testing")
                .setBody("forPermissionsTesting");
        AnnouncementModel created = createThread(preThread);

        goToProjectHome(getProjectName());
        impersonateRole("Reader");

        // Act and assert
        // as a Reader, attempt to do basic things that require permissions and confirm

        // first, get an existing thread as a reader
        MessageThreadResponse createResponse = getThread(created);
        assertThat(createResponse.getStatusCode(), is(200));

        // create a thread
        try
        {
            createThread(new AnnouncementModel());
            fail("Reader should not have permissions to create a thread");
        }
        catch (CommandException success)
        {
             assertThat(success.getMessage(), is("User does not have permission to perform this operation."));
        }

        // respond to a thread
        try
        {
            respondToThread(created, new AnnouncementModel().setTitle("fake"), getProjectName());
            fail("Reader should not have permission to respond to a thread via createThread.api");
        }
        catch (CommandException success)
        {
            assertThat(success.getMessage(), is("User does not have permission to perform this operation."));
        }

        // delete a thread
        try
        {
            DeleteMessageThreadCommand delCmd = new DeleteMessageThreadCommand(created.getEntityId());
            delCmd.execute(createDefaultConnection(), getProjectName());
            fail("Reader should not have permissions to delete via deleteThread.api");
        }
        catch (CommandException success)
        {
            assertThat(success.getMessage(), is("User does not have permission to perform this operation."));
        }

        // update a thread
        try
        {
            UpdateMessageThreadCommand updateCmd = new UpdateMessageThreadCommand(created);
            updateCmd.execute(createDefaultConnection(), getProjectName());
            fail("Reader should not have permission to update via updateThread.api");
        }
        catch (CommandException success)
        {
            assertThat(success.getMessage(), is("User does not have permission to perform this operation."));
        }

        stopImpersonatingHTTP();
    }

    private AnnouncementModel createThread(AnnouncementModel thread) throws Exception
    {
        return createThread(thread, getProjectName());
    }

    private AnnouncementModel createThread(AnnouncementModel thread, String containerPath) throws Exception
    {
        return new CreateMessageThreadCommand(thread)
                .execute(createDefaultConnection(), containerPath)
                .getAnnouncementModel();
    }

    private MessageThreadResponse getThread(AnnouncementModel thread) throws Exception
    {
        return getThread(thread, getProjectName());
    }

    private MessageThreadResponse getThread(AnnouncementModel thread, String containerPath) throws Exception
    {
        return new GetMessageThreadCommand(thread).execute(createDefaultConnection(), containerPath);
    }

    private MessageThreadResponse respondToThread(AnnouncementModel parentThread,
                                                  AnnouncementModel respondingThread,
                                                  String containerPath) throws Exception
    {
        CreateMessageThreadCommand replyCmd = new CreateMessageThreadCommand(respondingThread);
        replyCmd.setReply(true);
        respondingThread.setParent(parentThread.getEntityId());
        return replyCmd.execute(createDefaultConnection(), containerPath);
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
        return Arrays.asList("announcements");
    }
}
