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

        Map<String, String> expectedChanges = new HashMap<>();
        expectedChanges.put("Title", title);
        expectedChanges.put("Assigned To", TEST_USER_DISPLAY_NAME);
        expectedChanges.put("Notify", TEST_BUDDY_DISPLAY_NAME);
        expectedChanges.put("Type", "Defect");
        expectedChanges.put("Priority", "3");

        IssueCommand cmd = new IssueCommand();
        cmd.setIssue(testIssue);
        IssueResponse response = cmd.execute(createDefaultConnection(), getProjectName());

        //var issueListPage = _issuesHelper.goToIssueList(getProjectName(), ISSUES);
        assertNotNull("expect our issue response to tell us an issue ID", response.getIssueId());

        var issuePage = DetailsPage.beginAt(this, getProjectName(), response.getIssueId().toString());
        var issueComment = issuePage.getComments().get(0);
        assertEquals("expect that the comment was set", comment, issueComment.getComment());
        assertEquals("expect issue author to be correctly identified", TEST_USER_DISPLAY_NAME, issueComment.getUser());

        // fieldChanges on IssueComment seems not to work at all, this was a hopeful attempt to use it
        // we should fix it, and we should also be able to validate these things over the API
//        assertEquals("expect title, assignment, notify, type, pri to be set",
//                expectedChanges, issueComment.getFieldChanges());
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

        IssueCommand cmd = new IssueCommand();
        cmd.setIssue(originalIssue);
        IssueResponse response = cmd.execute(createDefaultConnection(), getProjectName());
        var issueId = response.getIssueId();

        updateIssue.setIssueId(issueId);
        IssueCommand updateCmd = new IssueCommand();
        updateCmd.setIssue(updateIssue);
        var updateResponse = updateCmd.execute(createDefaultConnection(), getProjectName());

        // navigate to the page for easy visualization
        var issuePage = DetailsPage.beginAt(this, getProjectName(), response.getIssueId().toString());

        // get issue status from the server
        GetIssueCommand getCmd = new GetIssueCommand(issueId);
        GetIssueResponse getResponse = getCmd.execute(createDefaultConnection(), getProjectName());
        var updatedModel = getResponse.getIssueModel();
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

        IssueModel updateIssue = basicIssueModel(newTitle, newComment)
                .setAction(IssueModel.IssueAction.RESOLVE)
                .setResolution("Fixed")
                .setPriority(updatedPri);

        // create the issue
        IssueCommand cmd = new IssueCommand();
        cmd.setIssue(originalIssue);
        IssueResponse response = cmd.execute(createDefaultConnection(), getProjectName());
        var issueId = response.getIssueId();

        // resolve the issue
        updateIssue.setIssueId(issueId);
        IssueCommand updateCmd = new IssueCommand();
        updateCmd.setIssue(updateIssue);
        var updateResponse = updateCmd.execute(createDefaultConnection(), getProjectName());

        // get the updated issue from the server
        GetIssueCommand getCmd = new GetIssueCommand(issueId);
        GetIssueResponse getResponse = getCmd.execute(createDefaultConnection(), getProjectName());
        var updatedIssue = getResponse.getIssueModel();
        assertEquals("expect status to be resolved", "resolved", updatedIssue.getStatus());
        assertEquals("expect resolution to be updated", "Fixed", updatedIssue.resolution());
        assertEquals("expect a new title", newTitle, updatedIssue.getTitle());
        assertNotNull(updatedIssue.getResolved());
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
        IssueModel updateIssue = basicIssueModel(newTitle, newComment)
                .setAction(IssueModel.IssueAction.CLOSE)
                .setPriority(updatedPri);

        IssueCommand cmd = new IssueCommand();
        cmd.setIssue(originalIssue);
        IssueResponse response = cmd.execute(createDefaultConnection(), getProjectName());
        var issueId = response.getIssueId();

        updateIssue.setIssueId(issueId);
        IssueCommand updateCmd = new IssueCommand();
        updateCmd.setIssue(updateIssue);
        var updateResponse = updateCmd.execute(createDefaultConnection(), getProjectName());

        GetIssueCommand getCmd = new GetIssueCommand(issueId);
        GetIssueResponse getResponse = getCmd.execute(createDefaultConnection(), getProjectName());
        var updatedIssue = getResponse.getIssueModel();
        assertEquals("closed", updatedIssue.getStatus());
        assertEquals(newTitle, updatedIssue.getTitle());
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
