package org.labkey.test.tests.issues;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.issues.GetIssueCommand;
import org.labkey.remoteapi.issues.GetIssueResponse;
import org.labkey.remoteapi.issues.IssuesCommand;
import org.labkey.remoteapi.issues.IssueModel;
import org.labkey.remoteapi.issues.IssueResponse;
import org.labkey.remoteapi.issues.IssueResponseModel;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.APIUserHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.IssuesHelper;
import org.labkey.test.util.PermissionsHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    static Long TEST_USER_ID;
    static String TEST_USER_NAME;
    static String TEST_USER_DISPLAY_NAME;

    static Long TEST_BUDDY_ID;
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
        _containerHelper.createProject(getProjectName(), null);
        _issuesHelper.createNewIssuesList(ISSUES, getContainerHelper());

        TEST_USER_NAME = getUsername();
        TEST_USER_ID = Long.valueOf(_userHelper.getUserId(TEST_USER_NAME));
        TEST_USER_DISPLAY_NAME = _userHelper.getDisplayNameForEmail(TEST_USER_NAME);
        _userHelper.createUser(TEST_BUDDY_NAME);
        TEST_BUDDY_ID = Long.valueOf(_userHelper.getUserId(TEST_BUDDY_NAME));
        TEST_BUDDY_DISPLAY_NAME = _userHelper.getDisplayNameForEmail(TEST_BUDDY_NAME);
        var permissionsHelper = new ApiPermissionsHelper(this);
        permissionsHelper.addMemberToRole(TEST_BUDDY_NAME, "Project Administrator",
                PermissionsHelper.MemberType.user, getProjectName());
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
        File testFile = TestFileUtils.writeTempFile("err.txt", "I found a bug");
        IssueModel testIssue = basicIssueModel(title, comment);
        testIssue.addAttachment(testFile);

        IssuesCommand cmd = new IssuesCommand();
        cmd.setIssues(List.of(testIssue));
        IssueResponse response = cmd.execute(createDefaultConnection(), getProjectName());
        assertNotNull("expect our issue response to tell us an issue ID", response.getIssueIds().get(0));

        var issueResponseModel = getIssueResponse(response.getIssueIds().get(0));
        var lastComment = issueResponseModel.getComments().get(0);
        var attachments = lastComment.getAttachments();
        assertThat("expect that the comment was set", lastComment.getComment(), containsString(comment));
        assertEquals("expect issue author to be correctly identified", TEST_USER_DISPLAY_NAME, lastComment.getCreatedBy());
        assertEquals("expect a single attachment", List.of(testFile.getName()), attachments);
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
                .setAction(IssueModel.IssueAction.update)
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
                .setAction(IssueModel.IssueAction.resolve)
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
        assertEquals("expect resolution to be updated", "Fixed", issueResponse.getResolution());
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
                .setAction(IssueModel.IssueAction.close)
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

    @Test
    public void testAssignAnIssue() throws Exception
    {
        String title = "Assign Test Issue";
        IssueModel originalIssue = basicIssueModel(title, "Gonna assign this");
        IssueModel updateIssue = basicIssueModel(null, "assigned now")
                .setAction(IssueModel.IssueAction.update)
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
                .setAction(IssueModel.IssueAction.close);
        IssueModel reopenIssue = basicIssueModel(null, null)
                .setAction(IssueModel.IssueAction.reopen);

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

    @Test
    public void testUpdateMultipleIssues() throws Exception
    {
        File firstFile = TestFileUtils.writeTempFile("firstErr.txt", "first error of 2");
        File secondFile = TestFileUtils.writeTempFile("secondErr.txt", "second error of 2");
        IssueModel firstIssue = basicIssueModel("first of three", "to test multiple issues at once")
                .addAttachment(firstFile);
        IssueModel secondIssue = basicIssueModel("second of three", "to test multiple issues at once")
                .addAttachment(secondFile);
        IssueModel thirdIssue = basicIssueModel("third of three", "to test multiple issues at once")
                .addAttachment(firstFile).addAttachment(secondFile);
        List<IssueModel> issues = new ArrayList<>();
        issues.add(firstIssue);
        issues.add(secondIssue);
        issues.add(thirdIssue);

        IssuesCommand insertCmd = new IssuesCommand();
        insertCmd.setIssues(issues);
        var insertResponse = insertCmd.execute(createDefaultConnection(), getProjectName());
        List<Long> issueIds =  insertResponse.getIssueIds();
        for (Long issueId : issueIds)
        {
            var issueresponseModel = getIssueResponse(issueId);
            switch (issueresponseModel.getTitle())
            {
                case "first of three":
                {
                    // verify initial values
                    assertEquals("expect the first attachment",
                            List.of(firstFile.getName()), issueresponseModel.getComments().get(0).getAttachments());
                    // set new ones for update
                    firstIssue.setIssueId(issueId)
                            .setAction(IssueModel.IssueAction.resolve)
                            .setResolution("fixed");
                    break;
                }
                case "second of three":
                {
                    // verify expected
                    assertEquals("expect the second attachment only",
                            List.of(secondFile.getName()), issueresponseModel.getComments().get(0).getAttachments());
                    // set for update
                    secondIssue.setIssueId(issueId)
                            .setAction(IssueModel.IssueAction.close)
                            .setComment("close comment");
                    break;
                }
                case "third of three":
                {
                    // verify expected
                    assertEquals("expect both attachments",
                            List.of(firstFile.getName(), secondFile.getName()), issueresponseModel.getComments().get(0).getAttachments());
                    // set for updated
                    thirdIssue.setIssueId(issueId)
                            .setComment("Assigning to buddy")
                            .setAssignedTo(TEST_BUDDY_ID);
                    break;
                }
            }
        }

        // now update the issues
        IssuesCommand updateCmd = new IssuesCommand();
        updateCmd.setIssues(issues);
        insertCmd.execute(createDefaultConnection(), getProjectName());
        for (Long issueId : issueIds)
        {
            var issueresponseModel = getIssueResponse(issueId);
            switch (issueresponseModel.getTitle())
            {
                case "first of three":
                {
                    assertEquals("expect new status", "resolved", issueresponseModel.getStatus());
                    assertEquals("expect fixed", "fixed", issueresponseModel.getResolution());
                    break;
                }
                case "second of three":
                {
                    assertEquals("expect new status", "closed", issueresponseModel.getStatus());
                    break;
                }
                case "third of three":
                {
                    assertEquals("expect assignment to buddy",
                            TEST_BUDDY_ID, issueresponseModel.getAssignedTo());
                    break;
                }
            }
        }
    }

    private Long doIssueAction(IssueModel params) throws Exception
    {
        IssuesCommand updateCmd = new IssuesCommand();
        updateCmd.setIssues(List.of(params));
        var updateResponse = updateCmd.execute(createDefaultConnection(), getProjectName());
        return updateResponse.getIssueIds().get(0);
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
                .setAction(IssueModel.IssueAction.insert)
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
