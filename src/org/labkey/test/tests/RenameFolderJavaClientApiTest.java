package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.RenameContainerCommand;
import org.labkey.remoteapi.security.RenameContainerResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.core.admin.ShowAuditLogPage;
import org.labkey.test.pages.samplemanagement.admin.AuditLogPage;
import org.labkey.test.util.LogMethod;

import java.io.IOException;
import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class RenameFolderJavaClientApiTest extends BaseWebDriverTest
{
    private static final String DUPLICATE_FOLDER_NAME = "Duplicate folder name";

    @Override
    protected @Nullable String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @BeforeClass
    public static void doSetup()
    {
        RenameFolderJavaClientApiTest initTest = (RenameFolderJavaClientApiTest) getCurrentTest();
        initTest.setupProject();
    }

    @LogMethod
    private void setupProject()
    {
        _containerHelper.createProject(getProjectName());
        _containerHelper.createProject(DUPLICATE_FOLDER_NAME);
    }

    @Test
    public void testRenameFolder() throws IOException, CommandException
    {
        String newContainerName = "Renamed Name for " + getProjectName();
        String newTitle = "Renamed Title for " + getProjectName();

        goToProjectHome();
        Connection cn = WebTestHelper.getRemoteApiConnection();

        log("New name only: Verify name and title become " + newContainerName);
        RenameContainerCommand command = new RenameContainerCommand(newContainerName, null, true);
        RenameContainerResponse response = command.execute(cn, getProjectName());
        Assert.assertEquals("Rename container api failed ", 200, response.getStatusCode());
        Assert.assertEquals("Incorrect new name", newContainerName, response.getName());
        Assert.assertEquals("Incorrect new title", newContainerName, response.getProperty("title"));

        log("New title only: Verify only title changes to " + newTitle);
        command = new RenameContainerCommand(null, newTitle, true);
        response = command.execute(cn, getProjectName());
        Assert.assertEquals("Rename container api failed ", 200, response.getStatusCode());
        Assert.assertEquals("Incorrect new name", newContainerName, response.getName());
        Assert.assertEquals("Incorrect new title", newTitle, response.getProperty("title"));

        log("New name and title: Verify both name and title update");
        command = new RenameContainerCommand(newContainerName + "1", newTitle + "1", true);
        response = command.execute(cn, getProjectName());
        Assert.assertEquals("Rename container api failed ", 200, response.getStatusCode());
        Assert.assertEquals("Incorrect new name", newContainerName + "1", response.getName());
        Assert.assertEquals("Incorrect new title", newTitle + "1", response.getProperty("title"));

        log("Required values missing: Verify error appears");
        command = new RenameContainerCommand(null, null, false);
        try
        {
            command.execute(cn, getProjectName());
        }
        catch (CommandException e)
        {
            Assert.assertEquals("Blank names are not allowed.", 500, e.getStatusCode());
            Assert.assertEquals("Incorrect error message", "Please specify a name or a title.", e.getMessage());
        }

        log("Empty name: Verify error appears");
        command = new RenameContainerCommand("", null, true);
        try
        {
            command.execute(cn, getProjectName());
        }
        catch (CommandException e)
        {
            Assert.assertEquals("Blank names are not allowed.", 500, e.getStatusCode());
            Assert.assertEquals("Incorrect error message", "Please specify a name or a title.", e.getMessage());
        }

        log(" Empty title: Verify error appears");
        command = new RenameContainerCommand(null, "", true);
        try
        {
            command.execute(cn, getProjectName());
        }
        catch (CommandException e)
        {
            Assert.assertEquals("Blank names are not allowed.", 500, e.getStatusCode());
            Assert.assertEquals("Incorrect error message", "Please specify a name or a title.", e.getMessage());
        }

        log("Verify renaming folder to existing folder fails");
        command = new RenameContainerCommand(DUPLICATE_FOLDER_NAME, DUPLICATE_FOLDER_NAME, false);
        try
        {
            command.execute(cn, getProjectName());
        }
        catch (CommandException e)
        {
            Assert.assertEquals("Folder should not be renamed to existing folder", 500, e.getStatusCode());
            Assert.assertEquals("Incorrect error message", "The server already has a project with this name.", e.getMessage());
        }

        log("AddAlias explicitly false: Verify works, and name and title update");
        command = new RenameContainerCommand(newContainerName + " NO ALIAS", newTitle + " NO ALIAS", false);
        response = command.execute(cn, getProjectName());
        Assert.assertEquals("Rename container api failed ", 200, response.getStatusCode());
        Assert.assertEquals("Incorrect new name", newContainerName + " NO ALIAS", response.getName());
        Assert.assertEquals("Incorrect new title", newTitle + " NO ALIAS", response.getProperty("title"));

        goToProjectHome(newContainerName);
        Assert.assertEquals("","");

        ShowAuditLogPage logPage = goToAdminConsole().clickAuditLog();
        logPage.selectView("Project and Folder events");
        Assert.assertEquals("", "");
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), false);
        _containerHelper.deleteProject(DUPLICATE_FOLDER_NAME, false);
    }
}
