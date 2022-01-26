package org.labkey.test.tests.issues;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.issues.GetIssueCommand;
import org.labkey.remoteapi.issues.GetIssueResponse;
import org.labkey.remoteapi.issues.IssueCommand;
import org.labkey.remoteapi.issues.IssueModel;
import org.labkey.remoteapi.issues.IssueResponse;
import org.labkey.remoteapi.issues.IssueResponseModel;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.pages.issues.DetailsPage;
import org.labkey.test.util.APIUserHelper;
import org.labkey.test.util.IssuesHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.PasswordUtil.getUsername;

@Category({InDevelopment.class})
public class IssueAPITest extends BaseWebDriverTest
{
    IssuesHelper _issuesHelper = new IssuesHelper(this);
    APIUserHelper _userHelper = new APIUserHelper(this);
    static String ISSUES = "issues";
    static Integer TEST_USER_ID;
    static String TEST_USER_NAME;
    static String TEST_USER_DISPLAY_NAME;

    static Integer TEST_BUDDY_ID;
    static String TEST_BUDDY_NAME = "testbuddy@issues.test";
    static String TEST_BUDDY_DISPLAY_NAME;

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        IssueAPITest init = (IssueAPITest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        TEST_USER_NAME = getUsername();
        TEST_USER_ID = _userHelper.getUserId(TEST_USER_NAME);
        TEST_USER_DISPLAY_NAME = _userHelper.getDisplayNameForEmail(TEST_USER_NAME);
        _userHelper.createUser(TEST_BUDDY_NAME);
        TEST_BUDDY_ID = _userHelper.getUserId(TEST_BUDDY_NAME);
        TEST_BUDDY_DISPLAY_NAME = _userHelper.getDisplayNameForEmail(TEST_BUDDY_NAME);

        _containerHelper.createProject(getProjectName(), null);
        _issuesHelper.createNewIssuesList(ISSUES, getContainerHelper());
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testInsertAnIssue() throws Exception
    {
        String comment = "This is a test to see if we can just... insert an issue";
        String title = "Insert Issue Test Issue";
        IssueModel testIssue = basicIssueModel(title, comment);

        IssueCommand cmd = new IssueCommand();
        cmd.setIssue(testIssue);
        IssueResponse response = cmd.execute(createDefaultConnection(), getProjectName());
        assertNotNull("expect our issue response to tell us an issue ID", response.getIssueId());

        var issuePage = DetailsPage.beginAt(this, getProjectName(), response.getIssueId().toString());
        var issueComment = issuePage.getComments().get(0);
        assertEquals("expect that the comment was set", comment, issueComment.getComment());
        assertEquals("expect issue author to be correctly identified", TEST_USER_DISPLAY_NAME, issueComment.getUser());
    }

    @Test
    public void testUpdateAnIssue() throws Exception
    {
        String originalComment = "This is an issue comment that will be updated";
        String originalTitle = "Pre-Update Issue Test Issue";
        String comment = "This is the update comment";
        String title = "Updated issue test issue";
        Long updatedPri = 4L;
        IssueModel originalIssue = basicIssueModel(originalTitle, originalComment);
        IssueModel updateIssue = basicIssueModel(title, comment)
                .setAction(IssueModel.IssueAction.UPDATE)
                .setPriority(updatedPri);

        // insert the issue
        var issueId = doIssueAction(originalIssue);

        // update
        updateIssue.setIssueId(issueId);
        doIssueAction(updateIssue);

        // verify
        var updatedModel = getIssueResponse(issueId);
        var latestComment = updatedModel.getComments().get(1);
        assertEquals("expect updated title", title, updatedModel.getTitle());
        assertThat("expect current comment reponse to have updated", latestComment.getComment(), containsString(comment));
        assertEquals("expect priority to now be 4", updatedPri, updatedModel.getPriority());
    }

    @Test
    public void testResolveAnIssue() throws Exception
    {
        String originalComment = "This issue will be immediately resolved";
        String originalTitle = "Resolve Issue Test";
        String newComment = "This issue should now be resolved";
        String newTitle = "Resolved test issue";
        Long updatedPri = 4L;
        IssueModel originalIssue = basicIssueModel(originalTitle, originalComment);

        IssueModel resolveIssue = basicIssueModel(newTitle, newComment)
                .setAction(IssueModel.IssueAction.RESOLVE)
                .setResolution("Fixed")
                .setPriority(updatedPri);

        // create the issue
        var issueId = doIssueAction(originalIssue);

        // resolve the issue
        resolveIssue.setIssueId(issueId);
        doIssueAction(resolveIssue);

        // get the updated issue from the server
        var issueResponse = getIssueResponse(issueId);
        assertEquals("expect status to be resolved", "resolved", issueResponse.getStatus());
        assertEquals("expect resolution to be updated", "Fixed", issueResponse.resolution());
        assertEquals("expect a new title", newTitle, issueResponse.getTitle());
        assertNotNull(issueResponse.getResolved());
    }

    @Test
    public void testCloseAnIssue() throws Exception
    {
        String originalComment = "This issue will be immediately closed";
        String originalTitle = "Close Issue Test";
        String newComment = "This issue should now be closed";
        String newTitle = "Closed test issue";
        Long updatedPri = 4L;
        IssueModel originalIssue = basicIssueModel(originalTitle, originalComment);
        IssueModel closeIssue = basicIssueModel(newTitle, newComment)
                .setAction(IssueModel.IssueAction.CLOSE)
                .setPriority(updatedPri);

        // insert the issue
        var issueId = doIssueAction(originalIssue);

        // close the issue
        closeIssue.setIssueId(issueId);
        doIssueAction(closeIssue);

        // verify
        var updatedIssue = getIssueResponse(issueId);
        assertEquals("closed", updatedIssue.getStatus());
        assertEquals(newTitle, updatedIssue.getTitle());
    }

    //@Test
    public void testAssignAnIssue() throws Exception
    {
        String title = "Assign Test Issue";
        IssueModel originalIssue = basicIssueModel(title, "Gonna assign this");
        IssueModel updateIssue = basicIssueModel(null, "assigned now")
                .setAction(IssueModel.IssueAction.UPDATE)
                .setAssignedTo(TEST_BUDDY_ID);

        // create the issue
        var issueId = doIssueAction(originalIssue);

        // assign the issue
        updateIssue.setIssueId(issueId);
        doIssueAction(updateIssue);

        // get the updated result
        var updatedIssue = getIssueResponse(issueId);
        assertEquals("should be assigned to buddy", TEST_BUDDY_ID, updatedIssue.getAssignedTo());
        assertEquals("passing null title should not update", title, updatedIssue.getTitle());
    }

    @Test
    public void testReopenAnIssue() throws Exception
    {
        IssueModel originalIssue = basicIssueModel("Reopen test issue", "Gonna close and reopen this");
        IssueModel closeIssue = basicIssueModel(null, null)
                .setAction(IssueModel.IssueAction.CLOSE);
        IssueModel reopenIssue = basicIssueModel(null, null)
                .setAction(IssueModel.IssueAction.REOPEN);

        // insert
        var issueId = doIssueAction(originalIssue);

        // close
        closeIssue.setIssueId(issueId);
        doIssueAction(closeIssue);

        // verify
        var closedIssue = getIssueResponse(issueId);
        assertEquals("closed", closedIssue.getStatus());

        // reopen
        reopenIssue.setIssueId(issueId);
        doIssueAction(reopenIssue);

        // verify
        var updatedIssue = getIssueResponse(issueId);
        assertEquals("open", updatedIssue.getStatus());
    }

    private Long doIssueAction(IssueModel params) throws Exception
    {
        IssueCommand updateCmd = new IssueCommand();
        updateCmd.setIssue(params);
        var updateResponse = updateCmd.execute(createDefaultConnection(), getProjectName());
        return updateResponse.getIssueId();
    }

    private IssueResponseModel getIssueResponse(Long issueId) throws Exception
    {
        GetIssueCommand getCmd = new GetIssueCommand(issueId);
        GetIssueResponse getResponse = getCmd.execute(createDefaultConnection(), getProjectName());
        return getResponse.getIssueModel();
    }

    private IssueModel basicIssueModel(String title, String comment)
    {
        return new IssueModel()
                .setTitle(title)
                .setComment(comment)
                .setAction(IssueModel.IssueAction.INSERT)
                .setAssignedTo(TEST_USER_ID)
                .setNotify(TEST_BUDDY_NAME)
                .setIssueDefName(ISSUES)
                .setPriority(3L)
                .setType("Defect");
    }


    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "IssueAPITest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
