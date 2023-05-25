package org.labkey.test.tests;

import org.apache.hc.core5.http.HttpStatus;
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
import org.labkey.test.Locators;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.core.admin.ShowAuditLogPage;
import org.labkey.test.util.LogMethod;

import java.io.IOException;
import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class RenameFolderJavaClientApiTest extends BaseWebDriverTest
{
    private static final String DUPLICATE_FOLDER_NAME = "Duplicate folder name";

    @BeforeClass
    public static void doSetup()
    {
        RenameFolderJavaClientApiTest initTest = (RenameFolderJavaClientApiTest) getCurrentTest();
        initTest.setupProject();
    }

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
        Assert.assertEquals("Rename container api failed ", HttpStatus.SC_OK, response.getStatusCode());
        Assert.assertEquals("Incorrect new name", newContainerName, response.getName());
        Assert.assertEquals("Incorrect new title", newContainerName, response.getProperty("title"));

        log("New title only: Verify only title changes to " + newTitle);
        command = new RenameContainerCommand(null, newTitle, true);
        response = command.execute(cn, getProjectName());
        Assert.assertEquals("Rename container api failed ", HttpStatus.SC_OK, response.getStatusCode());
        Assert.assertEquals("Incorrect new name", newContainerName, response.getName());
        Assert.assertEquals("Incorrect new title", newTitle, response.getProperty("title"));

        log("New name and title: Verify both name and title update");
        command = new RenameContainerCommand(newContainerName + "1", newTitle + "1", true);
        response = command.execute(cn, getProjectName());
        Assert.assertEquals("Rename container api failed ", HttpStatus.SC_OK, response.getStatusCode());
        Assert.assertEquals("Incorrect new name", newContainerName + "1", response.getName());
        Assert.assertEquals("Incorrect new title", newTitle + "1", response.getProperty("title"));

        log("Required values missing: Verify error appears");
        command = new RenameContainerCommand(null, null, false);
        try
        {
            command.execute(cn, getProjectName());
            Assert.fail("Rename should have failed");
        }
        catch (CommandException e)
        {
            Assert.assertEquals("Blank names are not allowed.", HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
            Assert.assertEquals("Incorrect error message", "Please specify a name or a title.", e.getMessage());
        }

        log("Empty name: Verify error appears");
        command = new RenameContainerCommand("", null, true);
        try
        {
            command.execute(cn, getProjectName());
            Assert.fail("Rename should have failed");
        }
        catch (CommandException e)
        {
            Assert.assertEquals("Blank names are not allowed.", HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
            Assert.assertEquals("Incorrect error message", "Please specify a name or a title.", e.getMessage());
        }

        log(" Empty title: Verify error appears");
        command = new RenameContainerCommand(null, "", true);
        try
        {
            command.execute(cn, getProjectName());
            Assert.fail("Rename should have failed");
        }
        catch (CommandException e)
        {
            Assert.assertEquals("Blank names are not allowed.", HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
            Assert.assertEquals("Incorrect error message", "Please specify a name or a title.", e.getMessage());
        }

        log("Verify renaming folder to existing folder fails");
        command = new RenameContainerCommand(DUPLICATE_FOLDER_NAME, DUPLICATE_FOLDER_NAME, false);
        try
        {
            command.execute(cn, getProjectName());
            Assert.fail("Rename should have failed");
        }
        catch (CommandException e)
        {
            Assert.assertEquals("Folder should not be renamed to existing folder", HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
            Assert.assertEquals("Incorrect error message", "The server already has a project with this name.", e.getMessage());
        }

        log("AddAlias explicitly false: Verify works, and name and title update");
        goToProjectHome(newContainerName);
        command = new RenameContainerCommand(newContainerName + " NO ALIAS", newTitle + " NO ALIAS", false);
        response = command.execute(cn, getProjectName());
        Assert.assertEquals("Rename container api failed ", HttpStatus.SC_OK, response.getStatusCode());
        Assert.assertEquals("Incorrect new name", newContainerName + " NO ALIAS", response.getName());
        Assert.assertEquals("Incorrect new title", newTitle + " NO ALIAS", response.getProperty("title"));

        goToProjectHome(newContainerName + "1");
        Assert.assertEquals("Project should not be accessible with old name",
                "No such project: /" + newContainerName + "1",
                Locators.labkeyErrorHeading.findElement(getDriver()).getText());

        ShowAuditLogPage logPage = goToAdminConsole().clickAuditLog();
        logPage.selectView("Project and Folder events");
        Assert.assertEquals("Incorrect comment in audit log",
                newContainerName + " NO ALIAS was renamed from " +
                        newContainerName + "1 to " + newContainerName + " NO ALIAS",
                logPage.getLogTable().getDataAsText(0, "Comment"));
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), false);
        _containerHelper.deleteProject(DUPLICATE_FOLDER_NAME, false);
    }
}
