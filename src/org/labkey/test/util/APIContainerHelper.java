/*
 * Copyright (c) 2012-2016 LabKey Corporation
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
        path = "";
        for (String container : splitPath)
        {
            path = path + "/" + EscapeUtil.encode(container);
        }

        _test.beginAt("/project" + path + "/" + EscapeUtil.encode(folderName) +  "/begin.view?");
    }

    public CreateContainerResponse doCreateContainer(String parentPath, @Nullable String name, String title, String folderType, boolean isWorkbook)
    {
        Connection connection = new Connection(_test.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        CreateContainerCommand command = new CreateContainerCommand((String)null);

        if (isWorkbook)
            command.setWorkbook(true);

        if (title != null)
            command.setTitle(title);

        if (name != null)
            command.setName(name);

        command.setFolderType(folderType);
        parentPath = (parentPath.startsWith("/") ? "" : "/") + parentPath;

        try
        {
            CreateContainerResponse response = command.execute(connection, parentPath);
            if (!isWorkbook)
            {
                assertEquals("Unexpected name for created container", name, response.getName());
                String path = String.join("/", parentPath, name).replace("//", "/");
                assertEquals("Unexpected path for created container", path, response.getPath());
            }
            assertEquals("Wrong folder type for " + response.getPath() + ". Defining module may be missing.",
                    folderType == null ? "None" : folderType, response.getProperty("folderType"));
            return response;
        }
        catch (CommandException | IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void doDeleteProject(String projectName, boolean failIfNotFound, int wait) throws TestTimeoutException
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
            if (e.getMessage().contains("Not Found"))
            {
                if (failIfNotFound)
                    fail("Container not found: " + path);
            }
            else
            {
                throw new RuntimeException("Failed to delete container", e);
            }
        }
        catch (SocketTimeoutException e)
        {
            throw new TestTimeoutException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to delete container", e);
        }
    }

    @Override
    public void moveFolder(@LoggedParam String projectName, @LoggedParam String folderName, @LoggedParam String newParent, final boolean createAlias) throws CommandException
    {
        Connection connection = new Connection(_test.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

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
            throw new RuntimeException(fail);
        }
    }
}
