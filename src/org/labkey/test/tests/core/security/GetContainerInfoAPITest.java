package org.labkey.test.tests.core.security;

import org.apache.hc.core5.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PermissionsHelper;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests cross-container permission enforcement in CoreController.GetContainerInfoAction (Kanban #1924).
 *
 * The action accepts an optional {@code containerPath} parameter. Prior to the fix, a user who had
 * ReadPermission on the request container could supply any container path and receive information about
 * it — even containers they had no access to. The fix adds a check that the user also has ReadPermission
 * on the resolved container before returning any data.
 */
@Category({Daily.class})
public class GetContainerInfoAPITest extends BaseWebDriverTest
{
    private static final String READABLE_PROJECT = "GetContainerInfoAPITest Readable";
    private static final String RESTRICTED_PROJECT = "GetContainerInfoAPITest Restricted";
    private static final String READER_USER = "reader@getcontainerinfoapi.test";

    private final ApiPermissionsHelper _permissions = new ApiPermissionsHelper(this);

    public GetContainerInfoAPITest()
    {
        ((APIContainerHelper) _containerHelper).setNavigateToCreatedFolders(false);
    }

    @BeforeClass
    public static void setupProject()
    {
        GetContainerInfoAPITest init = getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(READABLE_PROJECT, "Collaboration");
        _containerHelper.createProject(RESTRICTED_PROJECT, "Collaboration");

        _userHelper.createUser(READER_USER);
        _userHelper.setInitialPassword(READER_USER);
        _permissions.addMemberToRole(READER_USER, "Reader", PermissionsHelper.MemberType.user, READABLE_PROJECT);
        // Intentionally not granting the user any role in RESTRICTED_PROJECT
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(READABLE_PROJECT, afterTest);
        _containerHelper.deleteProject(RESTRICTED_PROJECT, afterTest);
        _userHelper.deleteUsers(false, READER_USER);
    }

    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    // Kanban #1924
    @Test
    public void testGetContainerInfoAccessControl()
    {
        // Cross-container denial: user makes the request from READABLE_PROJECT (passing @RequiresPermission),
        // but containerPath resolves to RESTRICTED_PROJECT where the user has no ReadPermission. Expect 403.
        String restrictedUrl = WebTestHelper.buildURL("core", READABLE_PROJECT, "getContainerInfo",
                Map.of("containerPath", RESTRICTED_PROJECT, "newFolderType", "Collaboration"));
        int restrictedStatus = WebTestHelper.getHttpResponse(restrictedUrl, READER_USER, PasswordUtil.getPassword())
                .getResponseCode();
        assertEquals("Expected 403 when user lacks ReadPermission on the containerPath container",
                HttpStatus.SC_FORBIDDEN, restrictedStatus);

        // Same-container success: containerPath resolves to READABLE_PROJECT where the user is a Reader. Expect 200.
        String readableUrl = WebTestHelper.buildURL("core", READABLE_PROJECT, "getContainerInfo",
                Map.of("containerPath", READABLE_PROJECT, "newFolderType", "Collaboration"));
        int readableStatus = WebTestHelper.getHttpResponse(readableUrl, READER_USER, PasswordUtil.getPassword())
                .getResponseCode();
        assertEquals("Expected 200 when user has ReadPermission on the containerPath container",
                HttpStatus.SC_OK, readableStatus);
    }
}
