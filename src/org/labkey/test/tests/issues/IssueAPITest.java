package org.labkey.test.tests.issues;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.issues.IssueCommand;
import org.labkey.remoteapi.issues.IssueModel;
import org.labkey.remoteapi.issues.IssueResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.APIUserHelper;
import org.labkey.test.util.IssuesHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.PasswordUtil.getUsername;

@Category({InDevelopment.class})
public class IssueAPITest extends BaseWebDriverTest
{
    IssuesHelper _issuesHelper = new IssuesHelper(this);
    APIUserHelper _userHelper = new APIUserHelper(this);
    String ISSUES = "issues";
    Integer TEST_USER_ID;
    String TEST_USER_NAME;

    Integer TEST_BUDDY_ID;
    String TEST_BUDDY_NAME = "testbuddy@issues.test";

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
        TEST_USER_ID = _userHelper.getUserId(TEST_USER_NAME);
        _userHelper.createUser(TEST_BUDDY_NAME);
        TEST_BUDDY_ID = _userHelper.getUserId(TEST_BUDDY_NAME);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testInsertAnIssue() throws Exception
    {
        IssueModel testIssue = new IssueModel()
                .setTitle("Insert Issue Test Issue")
                .setComment("This is a test to see if we can just... insert an issue")
                .setAction(IssueModel.IssueAction.INSERT)
                .setAssignedTo(TEST_USER_ID)
                .setNotifyList(List.of(TEST_BUDDY_NAME))
                .setIssueDefName(ISSUES)
                .setPriority(3)
                .setType("Defect");

        IssueCommand cmd = new IssueCommand();
        cmd.setIssue(testIssue);
        IssueResponse response = cmd.execute(createDefaultConnection(), getProjectName());

        _issuesHelper.goToIssueList(getProjectName(), ISSUES);
        assertNotNull("expect our issue response to tell us an issue ID", response.getIssueId());
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
