/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.security.CreateContainerCommand;
import org.labkey.remoteapi.security.DeleteContainerCommand;
import org.labkey.test.BaseSeleniumWebTest;

import java.io.IOException;

/**
 * User: jeckels
 * Date: Jul 20, 2012
 */
public class APIContainerHelper extends AbstractContainerHelper
{
    public APIContainerHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }

    @Override
    public void doCreateProject(String projectName, String folderType)
    {
        doCreateFolder(projectName, "/", folderType);
    }

    public final void createSubfolder(String parent, String folderName, String folderType)
    {
       doCreateFolder(folderName,  "/" + parent, folderType);
    }

    public void doCreateFolder(String folderName, String path, String folderType)
    {
        _test.log("Creating project with name via API " + folderName);
        Connection connection = new Connection(_test.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        CreateContainerCommand command = new CreateContainerCommand(folderName);
        command.setFolderType(folderType);
        try
        {
            command.execute(connection, path);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        _test.beginAt("/project" + path.replace("#", "%23") + "/" + folderName.replace("#", "%23") +  "/begin.view?");
    }

    @Override
    //wait is irrelevant for the API version
    public void deleteProject(String projectName, boolean failIfNotFound, int wait)
    {
        DeleteContainerCommand dcc = new DeleteContainerCommand();
        try
        {
            dcc.execute(_test.getDefaultConnection(), "/" + projectName);
        }
        catch (CommandException e)
        {
            if (e.getMessage().contains("Not Found"))
            {
                if (failIfNotFound)
                    Assert.fail("Project not found");
            }
            else
            {
                Assert.fail(e.getMessage());
            }
        }
        catch (IOException e)
        {
            Assert.fail(e.getMessage());
        }
    }
}
