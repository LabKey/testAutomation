package org.labkey.test.tests.core.security;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PermissionsHelper.MemberType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@Category({Daily.class})
public class GetReadableContainersAPITest extends BaseWebDriverTest
{
    private static final String PROJECT_PREFIX = "GetReadableContainersAPITest";
    private static final String READABLE_PROJECT = PROJECT_PREFIX + " Readable";
    private static final String UNREADABLE_PROJECT = PROJECT_PREFIX + " Unreadable";
    private static final String USER = "reader@containersapi.test";

    private static final List<ContainerInfo> containerInfos = new ArrayList<>();

    private final ApiPermissionsHelper _permissions = new ApiPermissionsHelper(this);

    public GetReadableContainersAPITest()
    {
        ((APIContainerHelper)_containerHelper).setNavigateToCreatedFolders(false);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(READABLE_PROJECT, afterTest);
        _containerHelper.deleteProject(UNREADABLE_PROJECT, afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        GetReadableContainersAPITest init = (GetReadableContainersAPITest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _userHelper.createUser(USER);
        createContainer(true);
        createContainer(true, true);
        createContainer(true, true, true);
        createContainer(true, true, false);
        createContainer(true, false);
        createContainer(true, false, true);
        createContainer(true, false, false);
        createContainer(false);
        createContainer(false, true);
        createContainer(false, true, true);
        createContainer(false, true, false);
        createContainer(false, false);
        createContainer(false, false, true);
        createContainer(false, false, false);
    }

    @Test
    public void testIgnoreDepthWithoutFlag()
    {
        impersonate(USER);
        Set<String> expectedFolders = getExpectedFolders(READABLE_PROJECT, 2, false);
        Set<String> readableContainers = getReadableContainers(null, 10, READABLE_PROJECT);
        
        assertEquals("Depth parameter should be ignored without 'includeSubfolders' flag", expectedFolders, readableContainers);
    }

    @Test
    public void testUnreadableStartingContainer()
    {
        impersonate(USER);
        Set<String> expectedFolders = getExpectedFolders(UNREADABLE_PROJECT, 10, false);
        Set<String> readableContainers = getReadableContainers(true, 10, UNREADABLE_PROJECT);

        assertEquals("Should list readable subfolders of unreadable starting container", expectedFolders, readableContainers);
    }

    @Test
    public void testUnlimitedDepth()
    {
        impersonate(USER);
        Set<String> expectedFolders = getExpectedFolders("", 10, false);
        Set<String> readableContainers = getReadableContainers(true, -1, "");

        assertEquals("Depth '-1' should be treated as unlimited'", expectedFolders, readableContainers);
    }

    @Test
    public void testCustomDepth()
    {
        impersonate(USER);
        Set<String> expectedFolders = getExpectedFolders("", 2, false);
        Set<String> readableContainers = getReadableContainers(true, 2, "");

        assertEquals("Custom listing depth should be respected", expectedFolders, readableContainers);
    }

    @Test
    public void testCurrentContainer()
    {
        impersonate(USER);
        goToProjectHome(READABLE_PROJECT);

        Set<String> expectedFolders = getExpectedFolders(READABLE_PROJECT, 3, false);
        Set<String> readableContainers = getReadableContainers(true, 2, null);

        assertEquals("Should use current container as default starting container", expectedFolders, readableContainers);
    }

    @Test
    public void testStartingContainerById() throws Exception
    {
        String containerId = ((APIContainerHelper) _containerHelper).getContainerId(UNREADABLE_PROJECT);

        impersonate(USER);
        Set<String> expectedFolders = getExpectedFolders(UNREADABLE_PROJECT, 3, false);
        Set<String> readableContainers = getReadableContainers(true, 2, containerId);

        assertEquals("Should be able to specify starting container by ID", expectedFolders, readableContainers);
    }

    @Test
    public void testSiteAdminSeesAll()
    {
        Set<String> expectedFolders = getExpectedFolders("", 10, true);
        Set<String> readableContainers = getReadableContainers(true, -1, "");

        assertEquals("Site admin should see all containers", expectedFolders, readableContainers);
    }

    private Set<String> getExpectedFolders(String container, int absoluteDepth, boolean includeUnreadable)
    {
        return containerInfos.stream().filter(
                info ->
                        (info.isReadable() || includeUnreadable) &&
                        info.getAbsoluteDepth() <= absoluteDepth &&
                        info.getPath().startsWith(container))
                .map(containerInfo -> "/" + containerInfo.getPath())
                .collect(Collectors.toSet());
    }

    private void createContainer(boolean projectReadable, boolean... foldersReadable)
    {
        final String path = getPathToFolder(projectReadable, foldersReadable);
        final boolean readable = foldersReadable.length > 0 ? foldersReadable[foldersReadable.length - 1] : projectReadable;
        _containerHelper.ensureContainer(path);

        if (readable)
        {
            _permissions.addMemberToRole(USER, "Reader", MemberType.user, path);
        }
        else
        {
            _permissions.removeUserRoleAssignment(USER, "Reader", path);
        }

        containerInfos.add(new ContainerInfo(path, readable, foldersReadable.length + 1));
    }

    private String getPathToFolder(boolean projectReadable, boolean... foldersReadable)
    {
        StringBuilder path = new StringBuilder();
        path.append(projectReadable ? READABLE_PROJECT : UNREADABLE_PROJECT);
        for (int i = 0; i < foldersReadable.length; i++)
        {
            boolean readable = foldersReadable[i];
            path.append("/");
            path.append(readable ? "Readable" : "Unreadable");
            path.append(i + 1);
        }
        return path.toString();
    }

    private Set<String> getReadableContainers(Boolean includeSubfolders, Integer depth, Object container)
    {
        List<String> response = executeGetReadableContainers(includeSubfolders, depth, container, List.class);
        return response.stream().filter(path -> path.startsWith("/" + PROJECT_PREFIX)).collect(Collectors.toSet());
    }

    private List<Map<String, String>> getReadableContainersErrors(Boolean includeSubfolders, Integer depth, Object container)
    {
        Map response = executeGetReadableContainers(includeSubfolders, depth, container, Map.class);
        if (response.get("errors") != null && response.get("errors") instanceof List)
        {
            return (List<Map<String, String>>) response.get("errors");
        }
        else
        {
            throw new RuntimeException("Did not find errors in response: \n" + response.toString());
        }
    }

    private <T> T executeGetReadableContainers(Boolean includeSubfolders, Integer depth, Object container, Class<T> expectedResultType)
    {
        Map<String, Object> config = new HashMap<>();
        if (includeSubfolders != null)
            config.put("includeSubfolders", includeSubfolders);
        if (depth != null)
            config.put("depth", depth);
        if (container != null)
            config.put("container", "/" + container);

        String script = "var config = arguments[0];\n" +
                "config.success = callback;\n" +
                "config.failure = callback;\n" +
                "LABKEY.Security.getReadableContainers(config);";

        return executeAsyncScript(script, expectedResultType, config);
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }

    private static class ContainerInfo
    {
        private final String _path;
        private final boolean _readable;
        private final int _absoluteDepth;

        ContainerInfo(String path, boolean readable, int absoluteDepth)
        {
            _path = path;
            _readable = readable;
            _absoluteDepth = absoluteDepth;
        }

        public String getPath()
        {
            return _path;
        }

        public boolean isReadable()
        {
            return _readable;
        }

        public int getAbsoluteDepth()
        {
            return _absoluteDepth;
        }
    }
}
