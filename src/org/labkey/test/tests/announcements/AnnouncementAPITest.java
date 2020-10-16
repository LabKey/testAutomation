package org.labkey.test.tests.announcements;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.announcements.CreateMessageThreadCommand;
import org.labkey.remoteapi.announcements.DeleteMessageThreadCommand;
import org.labkey.remoteapi.announcements.DeleteMessageThreadResponse;
import org.labkey.remoteapi.announcements.GetDiscussionsCommand;
import org.labkey.remoteapi.announcements.GetDiscussionsResponse;
import org.labkey.remoteapi.announcements.GetMessageThreadCommand;
import org.labkey.remoteapi.announcements.MessageThreadResponse;
import org.labkey.remoteapi.announcements.TestAnnouncementModel;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.DailyC;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
        TestAnnouncementModel parentThread = createThread(new TestAnnouncementModel().setBody("parent"), getProjectName())
                .getAnnouncementModel();

        // give it two responses
        MessageThreadResponse firstChildResponse = respondToThread(parentThread, new TestAnnouncementModel().setBody("firstChild"));
        MessageThreadResponse secondChildResponse = respondToThread(parentThread, new TestAnnouncementModel().setBody("secondChild"));

        GetDiscussionsCommand cmd = new GetDiscussionsCommand(parentThread.getDiscussionSrcIdentifier());
        GetDiscussionsResponse response = cmd.execute(createDefaultConnection(), getProjectName());

        parentThread = getThread(parentThread, getProjectName()).getAnnouncementModel();
        TestAnnouncementModel secondChild = getThread(secondChildResponse.getAnnouncementModel(), getProjectName())
                .getAnnouncementModel();

        assertThat("Expect a single response", response.getThreads().size(), is(1));
        assertThat("Expect the discussion request to return the parent thread",
                response.getThreads().get(0).getEntityId(), is(parentThread.getEntityId()));
    }

    @Test
    public void testCreateThread() throws Exception
    {
        TestAnnouncementModel preCreatedThread = new TestAnnouncementModel().setTitle("FirstCreatedThread")
                .setBody("testBody");
        MessageThreadResponse response = createThread(preCreatedThread, getProjectName());

        assertThat("Expect success", response.getStatusCode(), is(200));
        TestAnnouncementModel created = response.getAnnouncementModel();
        assertThat("Expect title to match", created.getTitle(), is(preCreatedThread.getTitle()));
        assertThat("Expect body to match", created.getBody(), is(preCreatedThread.getBody()));

        // now confirm that the response thread can be found independently
        MessageThreadResponse confirm = getThread(response.getAnnouncementModel(), getProjectName());
        assertThat(confirm.getStatusCode(), is(200));
        assertThat(confirm.getAnnouncementModel().getBody(), is(response.getAnnouncementModel().getBody()));
    }

    @Test
    public void testDeleteThreadByRowId() throws Exception
    {
        TestAnnouncementModel preThread = new TestAnnouncementModel().setBody("deleteMeByRowId");
        TestAnnouncementModel createdThread = createThread(preThread, getProjectName()).getAnnouncementModel();

        assertThat("expect response object to match input", createdThread.getBody(), is(preThread.getBody()));

        // now confirm that the expected thread exists
        MessageThreadResponse confirm = getThread(createdThread, getProjectName());
        assertThat(confirm.getStatusCode(), is(200));
        assertThat(confirm.getAnnouncementModel().getBody(), is(createdThread.getBody()));

        // now delete
        DeleteMessageThreadCommand delCmd = new DeleteMessageThreadCommand(confirm.getAnnouncementModel().getRowId());
        DeleteMessageThreadResponse delResponse = delCmd.execute(createDefaultConnection(), getProjectName());

        assertThat(delResponse.getStatusCode(), is(200));
    }

    @Test
    public void testDeleteThreadByEntityId() throws Exception
    {
        TestAnnouncementModel preThread = new TestAnnouncementModel().setBody("deleteMeByEntityId");
        TestAnnouncementModel createdThread = createThread(preThread, getProjectName()).getAnnouncementModel();

        assertThat("expect response object to match input", createdThread.getBody(), is(preThread.getBody()));

        // now confirm that the expected thread exists
        MessageThreadResponse confirm = getThread(createdThread, getProjectName());
        assertThat(confirm.getStatusCode(), is(200));
        assertThat(confirm.getAnnouncementModel().getBody(), is(createdThread.getBody()));

        // now delete it
        DeleteMessageThreadCommand delCmd = new DeleteMessageThreadCommand(createdThread.getEntityId());
        DeleteMessageThreadResponse delResponse = delCmd.execute(createDefaultConnection(), getProjectName());

        assertThat(delResponse.getStatusCode(), is(200));
    }

    @Test
    public void testRespondToExistingThread() throws Exception
    {
        TestAnnouncementModel parentThread = new TestAnnouncementModel();
        parentThread.setTitle("Parent");
        MessageThreadResponse parentCreateResponse = createThread(parentThread, getProjectName());
        TestAnnouncementModel createdParent = parentCreateResponse.getAnnouncementModel();

        TestAnnouncementModel childThread = new TestAnnouncementModel();
        childThread.setTitle("Child");
        childThread.setParent(createdParent.getEntityId());
        CreateMessageThreadCommand replyCmd = new CreateMessageThreadCommand(childThread);
        replyCmd.setReply(true);
        MessageThreadResponse childCreateResponse = replyCmd.execute(createDefaultConnection(), getProjectName());

        assertThat(childCreateResponse.getStatusCode(), is(200));

        TestAnnouncementModel createdChild = childCreateResponse.getAnnouncementModel();
        assertThat(createdChild.getParent(), is(createdParent.getEntityId()));

        TestAnnouncementModel originalParent = getThread(createdParent, getProjectName()).getAnnouncementModel();
        assertTrue("Expect parent row to be updated with added child",
                originalParent.getResponses().stream().anyMatch(a-> a.getEntityId().equals(createdChild.getEntityId())));
    }

    private MessageThreadResponse createThread(TestAnnouncementModel thread, String containerPath) throws Exception
    {
        CreateMessageThreadCommand cmd = new CreateMessageThreadCommand(thread);
        return cmd.execute(createDefaultConnection(), containerPath);
    }

    private MessageThreadResponse respondToThread(TestAnnouncementModel parentThread, TestAnnouncementModel respondingThread) throws Exception
    {
        CreateMessageThreadCommand replyCmd = new CreateMessageThreadCommand(respondingThread);
        replyCmd.setReply(true);
        respondingThread.setParent(parentThread.getEntityId());
        return replyCmd.execute(createDefaultConnection(), getProjectName());
    }

    private MessageThreadResponse getThread(TestAnnouncementModel thread, String containerPath) throws Exception
    {
        GetMessageThreadCommand cmd = new GetMessageThreadCommand(thread);
        return cmd.execute(createDefaultConnection(), getProjectName());
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
