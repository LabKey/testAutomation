package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.admin.CreateProjectCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.list.ManageListsGrid;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PermissionsHelper;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.WebTestHelper.getRemoteApiConnection;

@Category(Daily.class)
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class ProjectCreatorUserTest extends BaseWebDriverTest
{
    private static final String PROJECT_CREATOR_USER = "project_creator@permission.test";
    private static final String READER = "reader@permission.test";
    private static final String PROJECT_NAME_PC = "FolderByProjectCreator";
    private static final String TEMPLATE_PROJECT = "Template project";
    private static final String TEMPLATE_SUBFOLDER = "Subfolder for template project";
    private static final String TEMPLATE_FOLDER_PERMISSION = "Data Management";

    private final ApiPermissionsHelper _permissionsHelper = new ApiPermissionsHelper(this);

    @BeforeClass
    public static void setup()
    {
        ProjectCreatorUserTest initTest = (ProjectCreatorUserTest) getCurrentTest();
        initTest.doSetup();
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return null;
    }

    private void doSetup()
    {
        _containerHelper.createProject(TEMPLATE_PROJECT, "Study");
        importStudyFromZip(TestFileUtils.getSampleData("studies/LabkeyDemoStudy.zip"));
        _containerHelper.createSubfolder(TEMPLATE_PROJECT, TEMPLATE_PROJECT, TEMPLATE_SUBFOLDER, "Collaboration", null, true);

        _userHelper.createUser(PROJECT_CREATOR_USER, true, true);
        _userHelper.createUser(READER, true, true);

        _permissionsHelper.addMemberToRole(PROJECT_CREATOR_USER, "Project Creator", PermissionsHelper.MemberType.user, "/");
        _permissionsHelper.addMemberToRole(PROJECT_CREATOR_USER, "Project Admin", PermissionsHelper.MemberType.user, TEMPLATE_PROJECT);

        goToProjectHome(TEMPLATE_PROJECT);
        _permissionsHelper.createProjectGroup(TEMPLATE_FOLDER_PERMISSION, TEMPLATE_PROJECT);
        _permissionsHelper.addUserToProjGroup(PROJECT_CREATOR_USER, TEMPLATE_PROJECT, TEMPLATE_FOLDER_PERMISSION);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(TEMPLATE_PROJECT, afterTest);
        _userHelper.deleteUsers(false, PROJECT_CREATOR_USER, READER);
    }

    @Before
    public void beforeTest()
    {
        _containerHelper.deleteProject(PROJECT_NAME_PC, false, WAIT_FOR_PAGE);
    }

    @Test
    public void testCreateProjectByProjectCreator() throws IOException
    {
        log("Project Creator creating the project with admin permission");
        goToHome();
        impersonate(PROJECT_CREATOR_USER);
        CreateProjectCommand command = new CreateProjectCommand()
            .setName(PROJECT_NAME_PC)
            .setAssignProjectAdmin(true)
            .setFolderType("Collaboration");
        createProject(command);
        stopImpersonating();

        log("Verifying the permissions");
        goToProjectHome(PROJECT_NAME_PC);
        navBar().goToPermissionsPage().assertPermissionSetting(PROJECT_CREATOR_USER, "Project Administrator");
        navBar().goToPermissionsPage().assertPermissionSetting(PROJECT_CREATOR_USER, "Folder Administrator");

        log("Cleanup : Deleting the project");
        _containerHelper.deleteProject(PROJECT_NAME_PC, false, WAIT_FOR_PAGE);

        log("Project Creator creating the project without admin permission");
        goToHome();
        impersonate(PROJECT_CREATOR_USER);
        command = new CreateProjectCommand()
            .setName(PROJECT_NAME_PC)
            .setAssignProjectAdmin(false)
            .setFolderType("Collaboration");
        createProject(command);
        stopImpersonating();

        log("Verifying the permissions");
        goToProjectHome(PROJECT_NAME_PC);
        navBar().goToPermissionsPage().assertNoPermission(PROJECT_CREATOR_USER, "No Permissions");
    }

    @Test
    public void testCreateProjectByReader() throws IOException
    {
        log("Verifying Reader creating the project fails");
        goToHome();
        impersonate(READER);
        CreateProjectCommand command = new CreateProjectCommand()
            .setName(PROJECT_NAME_PC)
            .setAssignProjectAdmin(false)
            .setFolderType("Collaboration");
        String response = createProject(command);
        stopImpersonating();

        assertEquals("Should not be able to create the project", "403 : Forbidden", response);
        assertFalse(projectMenu().projectLinkExists(PROJECT_NAME_PC));
    }

    @Test
    public void testCreateProjectByTemplate() throws IOException, CommandException
    {
        String containerId = ((APIContainerHelper) _containerHelper).getContainerId(TEMPLATE_PROJECT);
        goToHome();
        impersonate(PROJECT_CREATOR_USER);
        CreateProjectCommand command = new CreateProjectCommand()
            .setName(PROJECT_NAME_PC)
            .setAssignProjectAdmin(true)
            .setFolderType("Template")
            .setTemplateSourceId(containerId)
            .setTemplateIncludeSubfolders(true)
            .setTemplateWriterTypes("Lists");
        createProject(command);
        stopImpersonating();

        goToProjectHome(PROJECT_NAME_PC);
        assertTrue(projectMenu().projectLinkExists(PROJECT_NAME_PC));
        navBar().goToPermissionsPage().assertPermissionSetting(PROJECT_CREATOR_USER, "Project Administrator");
        navBar().goToPermissionsPage().assertPermissionSetting(PROJECT_CREATOR_USER, "Folder Administrator");

        ManageListsGrid listsGrid = goToManageLists().getGrid();
        assertEquals("Incorrect lists copied from template", Arrays.asList("Lab Machines", "Reagents", "Technicians"), listsGrid.getListNames());
    }

    /*
        Test coverage for Issue 45273: Importing groups during create from template can result in unauthorized exceptions
     */
    @Test
    public void testCreateProjectByTemplateWithSubfolderAndPermission() throws CommandException, IOException
    {
        String containerId = ((APIContainerHelper) _containerHelper).getContainerId(TEMPLATE_PROJECT);

        goToHome();
        impersonate(PROJECT_CREATOR_USER);
        CreateProjectCommand command = new CreateProjectCommand()
            .setName(PROJECT_NAME_PC)
            .setAssignProjectAdmin(true)
            .setFolderType("Template")
            .setTemplateSourceId(containerId)
            .setTemplateIncludeSubfolders(true)
            .setTemplateWriterTypes("Role assignments for users and groups", "Project-level groups and members");
        createProject(command);
        stopImpersonating();

        assertTrue(projectMenu().projectLinkExists(PROJECT_NAME_PC));
        WebElement projectTree = projectMenu().expandProjectFully(PROJECT_NAME_PC);
        assertNotNull("No link to subfolder: /" + TEMPLATE_PROJECT + "/" + TEMPLATE_SUBFOLDER, Locator.linkWithText(TEMPLATE_SUBFOLDER).findElementOrNull(projectTree));
        goToProjectHome(PROJECT_NAME_PC);
        goToFolderPermissions().isUserInGroup(PROJECT_CREATOR_USER, TEMPLATE_FOLDER_PERMISSION, PermissionsHelper.PrincipalType.USER);
    }

    private String createProject(CreateProjectCommand command) throws IOException
    {
        try
        {
            command.execute(getRemoteApiConnection(), "/");
        }
        catch (CommandException e)
        {
            return e.getMessage();
        }

        return "Success";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
