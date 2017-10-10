/*
 * Copyright (c) 2012-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.util;

import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.labkey.api.util.Path;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.security.CreateContainerCommand;
import org.labkey.remoteapi.security.CreateContainerResponse;
import org.labkey.remoteapi.security.DeleteContainerCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class APIContainerHelper extends AbstractContainerHelper
{
    public APIContainerHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    @Override
    public void doCreateProject(String projectName, String folderType)
    {
        doCreateFolder(projectName, "", folderType);
    }

    public final void createSubfolder(String parentPath, String folderName, String folderType)
    {
       doCreateFolder(folderName, parentPath, folderType);
    }

    public CreateContainerResponse createWorkbook(String parentPath, String title, String folderType)
    {
        _test.log("Creating workbook via API");
        return doCreateContainer(parentPath, null, title, folderType, true);
    }

    public void doCreateFolder(String folderName, String path, String folderType)
    {
        _test.log("Creating project via API with name: " + folderName);
        doCreateContainer(path, folderName, null, folderType, false);

        String[] splitPath = path.split("/");
        StringBuilder fullPath = new StringBuilder();
        for (String container : splitPath)
        {
            fullPath.append(container).append("/");
        }
        fullPath.append(folderName);

        _test.beginAt(WebTestHelper.buildURL("project", fullPath.toString(), "begin"));
    }

    public CreateContainerResponse doCreateContainer(String parentPath, @Nullable String name, String title, String folderType, boolean isWorkbook)
    {
        Connection connection = new Connection(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        CreateContainerCommand command = new CreateContainerCommand(name);

        if (isWorkbook)
            command.setWorkbook(true);

        if (title != null)
            command.setTitle(title);

        command.setFolderType(folderType);
        parentPath = (parentPath.startsWith("/") ? "" : "/") + parentPath;

        try
        {
            CreateContainerResponse response = command.execute(connection, parentPath);
            if (!isWorkbook)
            {
                String path = String.join("/", parentPath, name).replace("//", "/");
                assertEquals("Unexpected path for created container", path, response.getPath());
            }
            if (folderType == null || "custom".equals(folderType.toLowerCase()) || "none".equals(folderType))
                folderType = (isWorkbook ? "Workbook" : "None");
            assertEquals("Wrong folder type for " + response.getPath() + ". Defining module may be missing.", folderType, response.getProperty("folderType"));
            return response;
        }
        catch (CommandException | IOException e)
        {
            throw new RuntimeException("Failed to create container: " + parentPath + (name == null ? "" : "/" + name), e);
        }
    }

    @Override
    protected void doDeleteProject(String projectName, boolean failIfNotFound, int wait) throws TestTimeoutException
    {
        deleteContainer("/" + projectName, failIfNotFound, wait);
    }

    public void deleteWorkbook(String parent, int rowId, boolean failIfNotFound, int wait) throws TestTimeoutException
    {
        String path = parent + "/" + rowId;
        deleteContainer(path, failIfNotFound, wait);
    }

    public void deleteContainer(String path, boolean failIfNotFound, int wait) throws TestTimeoutException
    {
        DeleteContainerCommand dcc = new DeleteContainerCommand();
        dcc.setTimeout(wait);
        try
        {
            Connection defaultConnection = _test.createDefaultConnection(false);
            defaultConnection.setTimeout(wait);
            dcc.execute(defaultConnection, path);
        }
        catch (CommandException e)
        {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND || e.getMessage().contains("Not Found"))
            {
                if (failIfNotFound)
                    fail("Container not found: " + path);
            }
            else
            {
                throw new RuntimeException("Failed to delete container: " + path, e);
            }
        }
        catch (SocketTimeoutException e)
        {
            throw new TestTimeoutException("Timed out deleting container: " + path, e);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to delete container: " + path, e);
        }
    }

    @Override
    public void moveFolder(@LoggedParam String projectName, @LoggedParam String folderName, @LoggedParam String newParent, final boolean createAlias) throws CommandException
    {
        Connection connection = new Connection(WebTestHelper.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        if (!projectName.startsWith("/"))
            projectName = "/" + projectName;
        if (!newParent.startsWith("/"))
            newParent = "/" + newParent;
        final Path containerPath = new Path(projectName, folderName);
        final Path newParentPath = new Path(newParent);
        PostCommand command = new PostCommand("core", "moveContainer")
        {
            @Override
            public JSONObject getJsonObject()
            {
                JSONObject result = super.getJsonObject();
                if (result == null)
                {
                    result = new JSONObject();
                }
                result.put("container", containerPath.toString());
                result.put("parent", newParentPath.toString());
                result.put("addAlias", createAlias);
                setJsonObject(result);
                return result;
            }
        };

        try
        {
            command.execute(connection, null);
        }
        catch (IOException fail)
        {
            throw new RuntimeException("Failed to move '" + containerPath.toString() + "' to '" + newParentPath.toString() + "'", fail);
        }
    }
}
